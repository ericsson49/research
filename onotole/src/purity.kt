package onotole

import java.nio.file.Files
import java.nio.file.Paths

data class SemiLat<T>(val bottom: T, val merge: (T, T) -> T) {
  fun merge(c: Iterable<T>): T = c.fold(bottom, merge)
}

sealed class DFValue<N,T>
data class DFConst<N,T>(val v: T): DFValue<N,T>()
abstract class DFFunc<N,T>(): DFValue<N,T>() {
  abstract operator fun invoke(get: (N)->T): T
}


val lat = SemiLat(emptySet<String>()) { a, b -> a.union(b) }


data class FP(val mod: List<Int>, val res: List<Int> = emptyList())

typealias FR = Set<String>
typealias VARS = Map<String,FR>
data class GenKill<A,B>(val gen: A, val kill: B)

class FPA(val funcs: Map<String,FP>) {
  fun extractExpr(e: TExpr?): TExpr = e ?: NameConstant(null)
  fun extractExprs(e: TSlice): List<TExpr> = when(e) {
    is Index -> listOf(e.value)
    is Slice -> listOf(extractExpr(e.lower), extractExpr(e.upper), extractExpr(e.step))
    else -> fail()
  }
  fun extractLoadsFromLVal(e: TExpr): Set<TExpr> = when(e) {
    is Name -> emptySet()
    is Attribute -> extractLoadsFromLVal(e.value)
    is Subscript -> extractExprs(e.slice).plus(e.value).toSet()
    is Tuple -> e.elts.flatMap(::extractLoadsFromLVal).toSet()
    else -> TODO()
  }

  fun mergeVars(a: VARS, b: VARS): VARS = a.toList().plus(b.toList())
          .groupBy { it.first }
          .mapValues { lat.merge(it.value.map { it.second }) }

  fun procCall(f: FP, args: List<TExpr>, vs: VARS): Pair<FR,FR> {
    val (fs,ps) = args.map { procExpr(it, vs) }.unzip()
    val ps2 = f.mod.map { fs[it] }
    val pur = lat.merge(ps.plus(ps2))
    val fr = lat.merge(f.res.map { fs[it] })
    return fr to pur
  }

  val dependencies = mutableSetOf<String>()
  val pureFR = FP(emptyList())
  fun getFP(n: String): FP {
    dependencies.add(n)
    return funcs[n] ?: pureFR
  }
  fun procExpr(e: TExpr, vs: VARS): Pair<FR,FR> = when(e) {
    is Constant -> emptySet<String>() to emptySet()
    is Name -> Pair(vs[e.id] ?: emptySet(), emptySet())
    is BinOp -> procCall(pureFR, listOf(e.left, e.right), vs)
    is BoolOp -> procCall(pureFR, e.values, vs)
    is Compare -> procCall(pureFR, listOf(e.left).plus(e.comparators), vs)
    is UnaryOp -> procCall(pureFR, listOf(e.operand), vs)
    is Call -> {
      val (fp,args) = when(e.func) {
        is Name -> getFP(e.func.id) to emptyList()
        is Attribute -> getFP("_." + e.func.attr) to listOf(e.func.value)
        is Subscript -> pureFR to emptyList()
        else -> TODO()
      }
      procCall(fp, args.plus(e.args).plus(e.keywords.map { it.value }), vs)
    }
    is Attribute -> {
      procExpr(e.value, vs)
    }
    is Subscript -> {
      val (f,p) = procExpr(e.value, vs)
      val p2 = lat.merge(extractExprs(e.slice).map { procExpr(it, vs).second })
      f to lat.merge(p,p2)
    }
    is IfExp -> {
      val (ft,pt) = procExpr(e.test, vs)
      val (fb,pb) = procExpr(e.body, vs)
      val (fe,pe) = procExpr(e.orelse, vs)
      lat.merge(fb,fe) to lat.merge(listOf(pt,pb,pe))
    }
    is Tuple -> procCall(FP(emptyList(), e.elts.indices.toList()), e.elts, vs)
    is PyList -> procCall(FP(emptyList(), e.elts.indices.toList()), e.elts, vs)
    is PyDict -> procCall(FP(emptyList(), e.keys.indices.toList()), e.values, vs)
    is Lambda -> emptySet<String>() to emptySet()
    is GeneratorExp -> emptySet<String>() to emptySet()
    is ListComp -> emptySet<String>() to emptySet()
    is DictComp -> emptySet<String>() to emptySet()
    is Starred -> procExpr(e.value, vs)

    else -> TODO()
  }
  fun procStmts(c: Iterable<Stmt>, vs: VARS, rp: FR): Pair<VARS,FR> {
    var (cvs,crp) = vs to rp
    for(s in c) {
      val r = procStmt(s, cvs, crp)
      cvs = r.first
      crp = r.second
    }
    return cvs to crp
  }
  fun procStmt(s: Stmt, vs: VARS, rp: FR): Pair<VARS,FR> {
    return when(s) {
      is Assign -> {
        val target = s.target
        val p = lat.merge(extractLoadsFromLVal(target).map { procExpr(it, vs).second })
        val (f2, p2) = procExpr(s.value, vs)
        val (varsUpd, p3: FR) = when(target) {
          is Name -> setOf(target.id to f2) to emptySet()
          is Attribute -> emptySet<Pair<String,FR>>() to procExpr(target.value, vs).first
          is Subscript -> emptySet<Pair<String,FR>>() to procExpr(target.value, vs).first
          is Tuple -> target.elts.map { (it as Name).id to f2 }.toSet() to emptySet()
          else -> TODO()
        }
        vs.plus(varsUpd) to lat.merge(listOf(rp, p, p2, p3))
      }
      is AugAssign -> {
        procStmt(Assign(s.target,BinOp(s.target,s.op,s.value)), vs, rp)
      }
      is AnnAssign -> procStmt(Assign(s.target,s.value ?: NameConstant(null)), vs, rp)
      is Assert -> {
        vs to lat.merge(rp, lat.merge(listOf(s.test, extractExpr(s.msg)).map { procExpr(it, vs).second }))
      }
      is Expr -> {
        vs to procExpr(s.value, vs).second
      }
      is If -> {
        val (_, pt) = procExpr(s.test, vs)
        val p2 = lat.merge(rp, pt)
        val (vsb, pb) = procStmts(s.body, vs, p2)
        val (vse, pe) = procStmts(s.orelse, vs, p2)
        mergeVars(vsb, vse) to lat.merge(listOf(pb, pe))
      }
      is While -> {
        val (_, pt) = procExpr(s.test, vs)
        val p2 = lat.merge(rp, pt)
        val (vsb, pb) = procStmts(s.body, vs, p2)
        mergeVars(vsb, vs) to lat.merge(listOf(rp, pb))
      }
      is For -> {
        val iter = Assign(s.target, Call(Name("next", ExprContext.Load), listOf(s.iter), listOf()))
        val body = listOf(iter).plus(s.body)
        val loop = While(NameConstant("True"), body)
        procStmt(loop, vs, rp)
      }
      is Return -> vs to lat.merge(rp, procExpr(extractExpr(s.value), vs).second)
      is Continue -> vs to rp
      is Break -> vs to rp
      is Pass -> vs to rp
      else -> TODO()
    }
  }
}

sealed class PurityRes
object Impure: PurityRes()
data class PurityDeps(val deps: Set<String> = emptySet()): PurityRes()
val Pure = PurityDeps()

fun main() {
  val ignoredFuncs = setOf("cache_this", "hash", "ceillog2", "floorlog2")
  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_altair_dev.txt")
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  val defs = parsed.map { toStmt(it) }
  val fDefs = defs.filterIsInstance<FunctionDef>().filter { it.name !in ignoredFuncs }

  val pure = setOf("len", "range", "map", "filter", "sorted", "set",
          "zip", "max", "min", "sum", "any", "all",
          "_.from_bytes",
          "_.count", "_.index", "_.union", "_.intersect",
          "uint8", "uint32", "uint64", "Bytes32",
          "_.copy", "copy",
          "hash", "hash_tree_root", "uint_to_bytes",
          "_.Aggregate", "_.Sign", "_.FastAggregateVerify", "_.Verify"
  )
  val transferring = mapOf("enumerate" to FP(emptyList(), listOf(0)), "next" to FP(emptyList(), listOf(0)))
  val impure = mapOf("_.append" to FP(listOf(0)))
  val cache = pure.map { it to FP(emptyList()) }.toMap().toMutableMap()
  cache.putAll(impure)
  cache.putAll(transferring)
  cache.putAll(defs.filterIsInstance<ClassDef>().map { it.name to FP(emptyList()) })

  var prev = emptyMap<String,FP>()
  while (true) {
    val res = fDefs.map { f ->
      val f = transformForOps(f)
      val vs = f.args.args.mapIndexed { i, a -> a.arg to setOf("#$i") }.toMap()
      val (a, b) = FPA(cache).procStmts(f.body, vs, emptySet())
      f.name to FP(b.map { it.substring(1).toInt() }.sorted())
    }.toMap()
    if (res == prev)
      break
    prev = res
    cache.putAll(res)
  }
  fun detectPurity(n: String): Boolean? {
    return when {
      n.startsWith("is_") || n.startsWith("get_") -> true
      n.startsWith("compute_") || n.startsWith("verify_") -> true
      n.startsWith("process_") -> false
      n.startsWith("on_") -> false
      n == "slash_validator" || n == "increase_balance" || n == "decrease_balance" -> false
      else -> null
    }
  }

  val fmap = fDefs.map { it.name to it }.toMap()
  prev.forEach { (n, fp) ->
    val purityAnno = detectPurity(n)
    if (purityAnno == null || purityAnno != fp.mod.isEmpty())
      println(n + "     " + (if (fp.mod.isEmpty()) "@Pure" else "@Impure" + fp.mod) )
    if (fp.mod.isNotEmpty() && fmap[n]!!.returns != NameConstant(null))
      println("  " + pyPrintType(fmap[n]!!.returns!!))
  }

}
