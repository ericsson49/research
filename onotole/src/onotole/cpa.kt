package onotole

import onotole.lib_defs.Additional
import onotole.lib_defs.BLS
import onotole.lib_defs.PyLib
import onotole.lib_defs.SSZLib
import java.nio.file.Files
import java.nio.file.Paths

interface DataflowAnalysis<N,D> {
  val bottom: D
  fun process(n: N, d: D, env: (N) -> D): D
  fun join(a: D, b: D): D
  fun solve(nodes: Collection<N>): Map<N, D> {
    val res = mutableMapOf<N, D>()
    fun getValue(n: N) = res.getOrPut(n) { bottom }
    do {
      var updated = false
      nodes.forEach { currNode ->
        val currValue = getValue(currNode)
        val newValue = join(currValue, process(currNode, currValue, ::getValue))
        if (currValue != newValue) {
          res[currNode] = newValue
          updated = true
        }
      }
    } while (updated)
    return res
  }
}
abstract class SimpleInterprocAnalysis<D>(val funcs: Collection<FunctionDef>): StaticGraphDataFlow<String, D> {
  val funcMap = funcs.map { it.name to it }.toMap()

  abstract fun getInitial(n: String): D
  val resCache = mutableMapOf<String, D>()
  fun getValue(n: String) = resCache.getOrPut(n) { getInitial(n) }

  val deps = funcs.map { it.name to findDeps(it) }.toMap()
  override fun predecessors(n: String): Collection<String> = deps[n] ?: emptyList()

  override fun process(n: String, d: D, env: (String) -> D): D {
    return predecessors(n).fold(getValue(n)) { a,dep -> join(a, env(dep)) }
  }
}

typealias SACont<N,D> = (N, D, env: (N) -> D) -> SAResult<N,D>
data class SAResult<N,D>(val r: D, val deps: Collection<N>)

interface StaticGraphDataFlow<N,D> {
  val bottom: D
  fun join(a: D, b: D): D
  fun predecessors(n: N): Collection<N>
  fun process(n: N, d: D, env: (N) -> D): D
  fun solve(init: Collection<N>): Map<N, D> {
    val revDeps = mutableMapOf<N, MutableSet<N>>()
    val nodes = mutableSetOf<N>()
    fun walk(n: N) {
      if (n !in nodes) {
        nodes.add(n)
        predecessors(n).forEach {
          revDeps.getOrPut(it) { mutableSetOf() }.add(n)
          walk(it)
        }
      }
    }
    init.forEach(::walk)

    val curr = mutableMapOf<N, D>()
    fun getVal(n: N) = curr[n] ?: bottom

    val workList = nodes.toMutableSet()
    while (workList.isNotEmpty()) {
      val n = workList.first()
      workList.remove(n)
      val currV = getVal(n)
      val res = process(n, currV, ::getVal)
      val newV = join(res, currV)
      if (newV != currV) {
        curr[n] = newV
        workList.addAll(revDeps[n] ?: emptyList())
      }
    }

    return curr
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
  val descrs = phase0PDs()

  val funcs = fDefs.map { fd ->
    val fi = getFuncDefInfo(phase, fd.name)
    val fd2 = fi.fd
    val cfg = fi.ssa
    fd2 to cfg
  }
  val t1 = System.currentTimeMillis()
  val r1 = detectPurity(funcs)
  val t2 = System.currentTimeMillis()
  val r2 = ImpurityAnalysis(funcs).solve(funcs.map { it.first.name })
  val t3 = System.currentTimeMillis()
  funcs.map { it.first.name }.forEach { n ->
    val res1 = r1[n]?.impureArgs?.toSet() ?: emptySet()
    val res2 = r2[n] ?: emptySet()
    val res3 = descrs[n]?.impureArgs?.toSet() ?: emptySet()
    if (res1 != res2 || res2 != res3) {
      println(n)
      println("  $res1 vs $res2 vs $res3")
    }

  }
  println()
  println(" " + (t2-t1) + " " + (t3-t2))
}
