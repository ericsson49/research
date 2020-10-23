import java.nio.file.Files
import java.nio.file.Paths

sealed class Instr(val cfg: CFG<Instr>) {
  override fun toString() = cfg.labeling[this]!!
}
class BranchI(cfg: CFG<Instr>, val rNames: Set<String>): Instr(cfg)
class AssgnI(cfg: CFG<Instr>, val lNames: Set<String>, val rNames: Set<String>): Instr(cfg)
class EnterI(cfg: CFG<Instr>): Instr(cfg)
class ExitI(cfg: CFG<Instr>): Instr(cfg)

fun <T> printCFG(cfg: CFG<Instr>, anno: Map<Point,T>) {
  var nextLabels: List<String>? = null
  cfg.transitions.toList().reversed().forEach {
    val i = it.first
    val label = cfg.labeling[i]!!
    if (nextLabels != null && nextLabels != listOf(label))
      println("  branches: " + nextLabels)
    println("---")
    println("in: " + anno[Pair(i, "in")])
    when(i) {
      is EnterI -> println("$label: <enter>")
      is ExitI -> println("$label: <exit>")
      is AssgnI -> println("$label: " + i.lNames + " = " + i.rNames)
      is BranchI -> println("$label: " + i.rNames)
    }
    println("out: " + anno[Pair(i, "out")])
    nextLabels = it.second.map { cfg.labeling[it]!! }
  }
  if (nextLabels != null)
    println("  branches: " + nextLabels)
}
class SimpleProcessor: NodeProcessor<Instr> {
  override fun enterInstr(cfg: CFG<Instr>): Instr {
    return EnterI(cfg)
  }
  override fun assignInstr(cfg: CFG<Instr>, targets: Set<identifier>, refs: Set<String>): Instr {
    return AssgnI(cfg, targets, refs)
  }
  override fun branchInstr(cfg: CFG<Instr>, refs: Set<String>): Instr {
    return BranchI(cfg, refs)
  }
  override fun exitInstr(cfg: CFG<Instr>): Instr {
    return ExitI(cfg)
  }
}
typealias Point = Pair<Instr,String>

fun makeLiveVarProblem(cfg: CFG<Instr>): DFAProblem<Point,VarSet> {
  fun genKillMaker(n: Instr) = when(n) {
    is AssgnI -> Pair(n.rNames, n.lNames)
    is BranchI -> Pair(n.rNames, emptySet())
    else -> Pair(emptySet(), emptySet())
  }
  return makeSetDFAProblem(cfg, forward = false, must = false, init = ::emptySet, genKillMaker = ::genKillMaker)
}

data class Val(val valName: String, val binder: String, val instLabel: String) {
  override fun toString() = valName
}
fun makeReachingDefsProblem(cfg: CFG<Instr>): DFAProblem<Point,Set<Val>> {
  val valuesAcc = mutableMapOf<String,Int>()
  fun newName(s: String): String {
    val i = valuesAcc.getOrDefault(s, -1) + 1
    valuesAcc[s] = i
    return s + "_" + i
  }
  val instrToDefsMap = cfg.transitions.keys.toList().reversed().map { i ->
    val vals = if (i is AssgnI) {
      i.lNames.map { Val(newName(it), it, cfg.labeling[i]!!) }.toSet()
    } else {
      emptySet()
    }
    i to vals
  }.toMap()
  val defs = instrToDefsMap.values.flatten()
  val nameToDefsMap = defs.groupBy { it.binder }.mapValues { it.value.toSet() }
  fun genKillMaker(n: Instr): Pair<Set<Val>,Set<Val>> = when(n) {
    is AssgnI -> {
      val gen = instrToDefsMap[n]!!
      val kill = gen.flatMap { vl -> nameToDefsMap[vl.binder]!!.minus(vl) }.toSet()
      Pair(gen, kill)
    }
    else -> Pair(emptySet(), emptySet())
  }
  return makeSetDFAProblem(cfg, true, false, ::emptySet, ::genKillMaker)
}

fun <N> makeDominatorsProblem(cfg: CFG<N>): DFAProblem<Pair<N,String>,Set<N>> {
  val nodes = cfg.transitions.keys.toSet()
  val starting = nodes.minus(cfg.reverse.keys)
  if (starting.size != 1) fail("Expecting that there will be only one starting node")

  fun genKillMaker(n: N): Pair<Set<N>,Set<N>> {
    val gen = setOf(n)
    val kill = if (n in starting) nodes else emptySet()
    return Pair(gen, kill)
  }
  return makeSetDFAProblem(cfg, true, true, {nodes}, ::genKillMaker)
}

fun <N> calcIDom(cfg: CFG<N>, domsRes: Map<Pair<N,String>,Set<N>>): Map<N,N> {
  val doms = domsRes.filterKeys { it.second == "out" }.mapKeys { it.key.first }
  val sdoms = doms.mapValues { it.value.minus(it.key) }
  val res = mutableMapOf<N,N>()
  cfg.transitions.keys.forEach { n ->
    val sdomN = sdoms[n]!!
    val r = sdomN.fold(sdomN, { acc,m -> acc.minus(sdoms[m]!!)}).toList()
    if (r.size == 1)
      res[n] = r[0]
  }
  return res
}

fun <N> calcDomFrontier(cfg: CFG<N>, idom: Map<N,N>): Map<N,Set<N>> {
  val nodes = cfg.transitions.keys
  var res = nodes.map { b -> b to mutableSetOf<N>() }.toMap().toMutableMap()
  nodes.forEach { b ->
    val preds = cfg.reverse[b] ?: emptyList()
    if (preds.size >= 2) {
      preds.forEach { p ->
        var runner = p
        while (runner != idom[p]) {
          res[runner]!!.add(b)
          runner = idom[p]!!
        }
      }
    }
  }
  return res.mapValues { it.value.toSet() }.toMap()
}

fun main() {
  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_test.txt")
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  val funcs = parsed.map { toStmt(it) as FunctionDef }

  funcs.forEach {
    val cfg = CFGBuilder.makeCFG(it, SimpleProcessor())
    println(it.name)
    val lvp = makeLiveVarProblem(cfg)
    val rdp = makeReachingDefsProblem(cfg)
    val domsP = makeDominatorsProblem(cfg)
    val domsRes = solve(domsP)
    val idom = calcIDom(cfg, domsRes)
    val domFr = calcDomFrontier(cfg, idom)

    printCFG(cfg, domFr.mapKeys { it.key to "out" })
    println()
  }

}