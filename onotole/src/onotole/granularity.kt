package onotole

import onotole.lib_defs.Additional
import onotole.lib_defs.BLS
import onotole.lib_defs.PyLib
import onotole.lib_defs.SSZLib
import java.nio.file.Files
import java.nio.file.Paths

fun analyzeGranularity(f: FunctionDef, cfg: CFGraphImpl): Pair<Map<String, Set<String>>, Set<String>> {
  val varTypes = inferTypes_FD.invoke(f)
  val exprTypes = TypeResolver.topLevelTyper.updated(varTypes)
  fun getType(e: TExpr): RTType = exprTypes[e].asType()

  val localRes = mutableMapOf<String, MutableSet<String>>()
  val deps = mutableSetOf<String>()
  val visitor = object : ExprVisitor<MutableSet<TExpr>>() {
    override fun procComprehension(c: Comprehension, ctx: MutableSet<TExpr>) {
      TODO()
    }
    override fun procExpr(e: TExpr, ctx: MutableSet<TExpr>) {
      if (e !is Lambda && e !is GeneratorExp && e !is ListComp && e !is SetComp && e !is DictComp)
        super.procExpr(e, ctx)
    }
    override fun visitExpr(e: TExpr, ctx: MutableSet<TExpr>) {
      ctx.add(e)
    }
  }
  val exprs = mutableSetOf<TExpr>()
  val topLevelExprs = cfg.blocks.flatMap { it.value.stmts }.flatMap { s ->
    when(s.lval) {
      is VarLVal, is EmptyLVal -> emptyList()
      is FieldLVal -> listOf(s.lval.r)
      is SubscriptLVal -> listOf(s.lval.r).plus(when(s.lval.i) {
        is Index -> listOf(s.lval.i.value)
        is Slice -> listOfNotNull(s.lval.i.lower, s.lval.i.upper, s.lval.i.step)
        else -> TODO()
      })
    }.plus(s.rval)
  }
  topLevelExprs.forEach { e ->
    visitor.procExpr(e, exprs)
  }
  exprs.forEach { e ->
    when(e) {
      is Attribute -> {
        if (!(e.value is Name && e.value.id in listOf("int", "bls"))) {
          val t = getType(e.value)
          if (t is NamedType)
            localRes.getOrPut(t.name) { mutableSetOf() }.add(e.attr)
        }
      }
      is Subscript -> {
        if (!(e.value is Name && e.value.id in listOf("List"))) {
          val t = getType(e.value) as NamedType
          localRes.getOrPut(t.name) { mutableSetOf() }.add("<content>")
        }
      }
      is Call -> {
        if (e.func is Name) {
          deps.add(e.func.id)
        } else if (e.func is Attribute) {
          if (!(e.func.value is Name && e.func.value.id in listOf("int", "bls"))) {
          }
        } else if (e.func is Subscript) {
          if (!(e.func.value is Name && e.func.value.id in listOf("List"))) {
            TODO()
          }
        } else
          TODO()
      }
      is Name, is Constant, is IfExp, is Let, is PyList, is PyDict, is Tuple, is Starred -> {}
      else -> TODO()
    }
  }
  return localRes to deps
}

class GranularityAnalysis(_funcs: List<Pair<FunctionDef,CFGraphImpl>>): StaticGraphDataFlow<String, Map<String, Set<String>>> {
  override val bottom = emptyMap<String, Set<String>>()
  override fun join(a: Map<String, Set<String>>, b: Map<String, Set<String>>): Map<String, Set<String>> {
    return a.keys.union(b.keys).map { k -> k to (a[k] ?: emptySet()).union(b[k] ?: emptySet()) }.toMap()
  }
  val results = _funcs.map { it.first.name to analyzeGranularity(it.first, it.second) }.toMap()

  override fun predecessors(n: String): Collection<String> = results[n]?.second ?: emptyList()

  override fun process(n: String, d: Map<String, Set<String>>, env: (String) -> Map<String, Set<String>>): Map<String, Set<String>> {
    val localRes = results[n]?.first ?: emptyMap()
    return predecessors(n).fold(localRes) { a, b -> join(a, env(b)) }
  }
}

fun main() {
  PyLib.init()
  SSZLib.init()
  BLS.init()
  val specVersion = "phase0"
  Additional.init(specVersion)
  val tlDefs = loadSpecDefs(specVersion)
  PhaseInfo.getPkgDeps(specVersion).forEach {
    TypeResolver.importFromPackage(it)
  }
  TypeResolver.importFromPackage(specVersion)

  val phase = specVersion
  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_${phase}_dev.txt")
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  val defs = parsed.map { toStmt(it) }

  val fDefs = defs.filterIsInstance<FunctionDef>()
  fDefs.forEach { registerFuncInfo(phase, it) }

  val funcs = fDefs.map { fd ->
    val fi = getFuncDefInfo(phase, fd.name)
    val fd2 = fi.fd
    val cfg = fi.ssa
    fd2 to cfg
  }
  //funcs.forEach { pyPrintFunc(it.first) }

  val funcNames = funcs.map { it.first.name }
  val analysis = GranularityAnalysis(funcs)
  val r = analysis.solve(funcNames).filter { it.key in funcNames }
  r.forEach { (n, f) ->
    println(n)
    println(f)
    println("----")
  }
}
