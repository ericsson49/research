package onotole

import onotole.util.deconstruct

val dafnyFreshAttrs = mapOf(
    "phase0.IndexedAttestation" to setOf("attesting_indices"),
    "phase0.BeaconState" to setOf("validators"),
    "pylib.Dict" to setOf("keys()")
)

enum class DafnyClassKind {
  Primitive, Datatype, Class
}

val dafnyClassKinds = mapOf(
    "pylib.PyList" to DafnyClassKind.Class,
    "pylib.Set" to DafnyClassKind.Class,
    "pylib.Dict" to DafnyClassKind.Class,
    "phase0.Store" to DafnyClassKind.Class,
    "ssz.List" to DafnyClassKind.Class,

    "phase0.LatestMessage" to DafnyClassKind.Datatype,
    "phase0.Checkpoint" to DafnyClassKind.Datatype,
    "phase0.BeaconState" to DafnyClassKind.Datatype,

    "ssz.uint64" to DafnyClassKind.Primitive,
    "ssz.Bytes32" to DafnyClassKind.Primitive,
    "phase0.Epoch" to DafnyClassKind.Primitive,
    "phase0.Slot" to DafnyClassKind.Primitive,
    "phase0.Root" to DafnyClassKind.Primitive,
    "phase0.Gwei" to DafnyClassKind.Primitive
)
enum class SideEffect {
  Pure, Fresh, Mutable
}
val dafnyFuncs_pure = setOf(
    "pylib.len", "pylib.sum", "pylib.any", "pylib.max", "pylib.has_next",
    "pylib.int#from_bytes",

    "ssz.hash_tree_root",

)
val dafnyFuncs_fresh = setOf(
    "pylib.sorted", "pylib.set", "pylib.list", "pylib.dict", "pylib.copy", "pylib.zip", "pylib.iter",
    "attr_copy", "attr_intersection",
    "attr_keys",
)
val dafnyFuncs_local = setOf(
    "pylib.next"
)
val dafnyFuncs_impure = setOf(
    "attr_append", "attr_add", "attr_remove"
)

fun isDafnyClassKind(e: TExpr, typer: ExprTyper, kind: DafnyClassKind): Boolean {
  return isDafnyClassKind(e, typer, setOf(kind))
}
fun isDafnyClassKind(e: TExpr, typer: ExprTyper, kinds: Set<DafnyClassKind>): Boolean {
  val t = typer[e]
  return when (t) {
    is NamedType -> {
      val k = dafnyClassKinds[t.name]
      k in kinds
    }
    is FunType -> false
    else -> TODO()
  }
}

class SideEffectDetector(val freshAttrs: Map<String, Set<String>>) {
  fun processNode(e: TExpr, typer: ExprTyper): Boolean {
    return when(e) {
      is CTV -> false
      is Constant, is Name -> false
      is BinOp, is BoolOp, is Compare, is UnaryOp -> false
      is IfExp -> false
      is Attribute -> if (e.ctx == ExprContext.Load) {
        val t = typer.get(e.value)
        if (t !is NamedType) TODO()
        e.attr in (freshAttrs[t.name] ?: emptySet())
      } else TODO()
      is Subscript -> if (e.ctx == ExprContext.Load) false else TODO()
      is Call -> when(e.func) {
        is Name ->
          if (e.func.id in TypeResolver.specialFuncNames) false
          else
            TODO()
        is Attribute ->
          if ("attr_${e.func.attr}" in dafnyFuncs_pure) false
          else if ("attr_${e.func.attr}" in dafnyFuncs_impure.plus(dafnyFuncs_fresh)) true
          else
            TODO()
        is CTV -> when(e.func.v) {
          is ClassVal -> {
            when(dafnyClassKinds[e.func.v.name]) {
              DafnyClassKind.Primitive, DafnyClassKind.Datatype -> false
              DafnyClassKind.Class -> true
              null -> TODO()
            }
          }
          is FuncInst ->
            if (e.func.v.name == "map") {
              if (e.args[0] !is Lambda) TODO()
              val lam = (e.args[0] as Lambda)
              val lamTyper = typer.forLambda(lam)
              processNode(lam.body, lamTyper)
            } else if (e.func.v.name == "<check>" || e.func.v.name == "<assert>") false
            else if (e.func.v.name.endsWith("::new")) {
              val clsName = e.func.v.name.substring(0, e.func.v.name.length - "::new".length)
              when(dafnyClassKinds[clsName]) {
                DafnyClassKind.Primitive, DafnyClassKind.Datatype -> false
                DafnyClassKind.Class -> true
                null -> TODO()
              }
            } else if (e.func.v.name in dafnyFuncs_pure)
              false
            else if (e.func.v.name in dafnyFuncs_fresh.plus(dafnyFuncs_impure + dafnyFuncs_local))
              true
            else if (e.func.v.name.startsWith("phase0.") && e.func.v.name.substring("phase0.".length) in fcFuncsDescr) {
              val memoryModel = fcFuncsDescr[e.func.v.name.substring("phase0.".length)]!!.memoryModel
              memoryModel == MemoryModel.MUT_HEAP || memoryModel == MemoryModel.FRESH
            } else TODO()
          else -> TODO()
        }
        else -> TODO()
      }
      is GeneratorExp -> {
        if (e.generators.size != 1) TODO()
        val c = e.generators[0]
        val exprs = listOf(c.iter) + c.ifs + listOf(e.elt)
        exprs.any { processNode(it, typer) }
      }
      is PyDict, is PyList, is PySet ->
        true
      is Lambda -> false
      else -> TODO()
    }
  }
  fun process(e: TExpr, typer: ExprTyper): Boolean {
    val r = processNode(e, typer)
    return r || deconstruct(e).first.any { process(it, typer) }
  }
}

fun checkSideEffects(e: TExpr, typer: ExprTyper, recursive: Boolean = false): Boolean {
  val d = SideEffectDetector(dafnyFreshAttrs)
  return if (recursive)
    d.process(e, typer)
  else
    d.processNode(e, typer)
}