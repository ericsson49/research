import java.nio.file.Files
import java.nio.file.Paths

class Instr(val lNames: Set<String>, val rNames: Set<String>)

fun Instr.toString(cfg: CFG<Instr>): String {
  val label = cfg.labeling[this]!!
  if (cfg.reverse[this] == null)
    println()
  return when {
    cfg.transitions[this]!!.isEmpty() -> "$label: <enter>"
    (cfg.reverse[this] ?: emptyList()).isEmpty() -> "$label: <exit>"
    this.lNames.isNotEmpty() -> "$label: " + this.lNames + " = " + this.rNames
    else -> "$label: " + this.rNames
  }
}

fun <T> printCFG(cfg: CFG<Instr>, anno: Map<Point,T>) {
  var nextLabels: List<String>? = null
  cfg.transitions.toList().reversed().forEach {
    val i = it.first
    val label = cfg.labeling[i]!!
    if (nextLabels != null && nextLabels != listOf(label))
      println("  branches: " + nextLabels)
    println("---")
    println("in: " + anno[Pair(i, "in")])
    println(i.toString(cfg))
    println("out: " + anno[Pair(i, "out")])
    nextLabels = it.second.map { cfg.labeling[it]!! }
  }
  if (nextLabels != null)
    println("  branches: " + nextLabels)
}
class SimpleProcessor: NodeProcessor<Instr> {
  override fun mkInstr(targets: Set<identifier>, refs: Set<String>): Instr {
    return Instr(targets, refs)
  }
}
typealias Point = Pair<Instr,String>

fun makeLiveVarProblem(cfg: CFG<Instr>): DFAProblem<Point,VarSet> {
  fun genKillMaker(n: Instr) = Pair(n.rNames, n.lNames)
  return makeSetDFAProblem(cfg, forward = false, must = false, init = ::emptySet, genKillMaker = ::genKillMaker)
}

data class Val(val valName: String, val binder: String, val instLabel: String) {
  override fun toString() = valName
}
fun makeReachingDefsProblem(cfg: CFG<Instr>): DFAProblem<Point,Set<Val>> {
  val instrToDefsMap = calcValueDefs(cfg)
  val defs = instrToDefsMap.values.flatten()
  val nameToDefsMap = defs.groupBy { it.binder }.mapValues { it.value.toSet() }
  fun genKillMaker(n: Instr): Pair<Set<Val>,Set<Val>> {
    val gen = instrToDefsMap[n]!!
    val kill = gen.flatMap { vl -> nameToDefsMap[vl.binder]!!.minus(vl) }.toSet()
    return Pair(gen, kill)
  }
  return makeSetDFAProblem(cfg, true, false, ::emptySet, ::genKillMaker)
}

fun calcValueDefs(cfg: CFG<Instr>): Map<Instr, Set<Val>> {
  val valuesAcc = mutableMapOf<String, Int>()
  fun newName(s: String): String {
    val i = valuesAcc.getOrDefault(s, -1) + 1
    valuesAcc[s] = i
    return s + "_" + i
  }
  return cfg.transitions.keys.toList().reversed().map { i ->
    i to i.lNames.map { Val(newName(it), it, cfg.labeling[i]!!) }.toSet()
  }.toMap()
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

fun <N> calcIDom(doms: Map<N,Set<N>>, cfg: CFG<N>): Map<N,N> {
  val sdoms = doms.mapValues { it.value.minus(it.key) }
  val res = mutableMapOf<N,N>()
  cfg.transitions.keys.forEach { n ->
    val sdomN = sdoms[n]!!
    val r = sdomN.fold(sdomN, { acc,m -> acc.minus(sdoms[m]!!)}).toList()
    if (r.size == 1)
      res[n] = r[0]
  }
  return res.toMap()
}

fun <N> calcDomFrontier(cfg: CFG<N>, idom: Map<N,N>): Map<N,Set<N>> {
  val nodes = cfg.transitions.keys
  val res = nodes.map { b -> b to mutableSetOf<N>() }.toMap().toMutableMap()
  nodes.forEach { b ->
    val preds = cfg.reverse[b] ?: emptyList()
    if (preds.size >= 2) {
      preds.forEach { p ->
        var runner = p
        while (runner != idom[b]) {
          res[runner]!!.add(b)
          runner = idom[runner]!!
        }
      }
    }
  }
  return res.mapValues { it.value.toSet() }.toMap()
}

fun insertPhiNodes(cfg: CFG<Instr>, phiNodes: Map<Instr,Set<String>>): Pair<CFG<Instr>, Map<Instr,Instr>> {
  fun copy(i: Instr) = Instr(i.lNames, i.rNames)
  val res = CFG<Instr>()
  val dupInstrs = cfg.transitions.keys.map { it to copy(it) }.toMap()
  val phiInstrs = phiNodes.mapValues { Instr(it.value, it.value) }
  phiInstrs.forEach { (i, phiI) ->
    res.addInstr(phiI, dupInstrs[i]!!)
  }
  cfg.transitions.forEach { (n, succ) ->
    res.addInstr(dupInstrs[n]!!, succ.map { phiInstrs[it] ?: dupInstrs[it]!! })
  }
  res.complete()
  return res to dupInstrs
}

fun <N> calcDom(cfg: CFG<N>)
    = solve(makeDominatorsProblem(cfg))
    .filterKeys { it.second == "out" }
    .mapKeys { it.key.first }

class DominanceAnalysis<N>(cfg: CFG<N>) {
  val domsOf: Map<N,Set<N>> = calcDom(cfg)
  val immDomOf: Map<N,N> = calcIDom(domsOf, cfg)
  val domFrontier: Map<N, Set<N>> = calcDomFrontier(cfg, immDomOf)
}

class CFGAnalyses(cfg: CFG<Instr>) {
  val dom: DominanceAnalysis<Instr> = DominanceAnalysis(cfg)
  val liveVars: Map<Point,VarSet> = solve(makeLiveVarProblem(cfg))
  val reachingDefs: Map<Point,Set<Val>> = solve(makeReachingDefsProblem(cfg))
}

class FuncAnalyses(f: FunctionDef) {
  val cfg: CFG<Instr>
  val cfgStmtMap: Map<Stmt,Pair<Instr,Instr>>
  val edgeLabeling: Map<Pair<Instr,Instr>,EdgeSource>
  val cfgAnalyses: CFGAnalyses
  init {
    val (_cfg, _stmtMap, _edgeLabeling) = CFGBuilder.makeCFG(f, SimpleProcessor())
    cfg = _cfg
    cfgStmtMap = _stmtMap
    edgeLabeling = _edgeLabeling
    cfgAnalyses = CFGAnalyses(cfg)
  }
}

object CopiesComparator: Comparator<Pair<String,String>> {
  override fun compare(o1: Pair<String, String>?, o2: Pair<String, String>?): Int {
    val (a1, b1) = o1!!
    val (a2, b2) = o2!!
    return if (b1 == a2 && a1 == b2)
      fail("curcular dependency $o1 $o2")
    else if (a1 == b2)
      1
    else if (a2 == b1)
      -1
    else
      0
  }
}
fun makeCopyStmt(p: Pair<String,String>) = Assign(
    targets = listOf(Name(id = p.second, ctx = ExprContext.Store)),
    value = Name(id = p.first, ctx = ExprContext.Load))
fun makeCopyStmts(s: List<Pair<String,String>>): List<Assign> {
  val sortedWith = s.sortedWith(CopiesComparator)
  return sortedWith.map(::makeCopyStmt)
}

fun insertCopies(s: Stmt, copies: Map<EdgeSource, List<Pair<String,String>>>): Stmt {
  return when(s) {
    is If -> s.copy(body = insertCopies(s.body, copies), orelse = insertCopies(s.orelse, copies))
    is While -> s.copy(body = insertCopies(s.body, copies))
    is For -> s.copy(body = insertCopies(s.body, copies))
    else -> s
  }
}

fun insertCopies(c: List<Stmt>, copies: Map<EdgeSource, List<Pair<String,String>>>): List<Stmt> {
  val res = mutableListOf<Stmt>()
  c.forEachIndexed { i, s ->
    res.addAll(copies[BlockIndex(c, i)]?.let { makeCopyStmts(it) } ?: emptyList<Assign>())
    res.add(insertCopies(s, copies))
  }
  res.addAll(copies[BlockIndex(c, c.size)]?.let { makeCopyStmts(it) } ?: emptyList<Assign>())
  return res.toList()
}

fun renameFuncArg(a: Arg, renames: Map<String,String>) = a.copy(arg = renames[a.arg] ?: a.arg)
fun renameFuncArguments(a: Arguments, renames: Map<String,String>) = a.copy(
    posonlyargs = a.posonlyargs.map { renameFuncArg(it, renames) },
    args = a.args.map { renameFuncArg(it, renames) },
    vararg = a.vararg?.let { renameFuncArg(it, renames) },
    kwonlyargs = a.kwonlyargs.map { renameFuncArg(it, renames) },
    kw_defaults = a.kw_defaults.map { renameVars(it, renames) },
    kwarg = a.kwarg?.let { renameFuncArg(it, renames) }
)

fun main() {
  PyLib.init()
  SSZLib.init()
  BLS.init()

  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_test.txt")
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  val funcs = parsed.map { toStmt(it) as FunctionDef }

  for (it in funcs) {
    if (it.name != "test13")
      continue
    println(it.name)
    val newF = convertToAndOutOfSSA(it)
    KotlinGen().genFunc(newF).forEach { println(it) }
    /*println()
    println("SSA")
    val ssa = CFG<Instr>()
    val instrMap = cfgWithPhis.transitions.keys.map { n ->
      n to if (n in phiRenames) {
        val inValues = reachDefsWithPhis[n to "in"]!!.groupBy { it.binder }
        val phiVals = phiRenames[n]!!
        Instr(phiVals.values.map { it.valName }.toSet(),
            phiVals.keys.flatMap { inValues[it]!!.map { it.valName } }.toSet())
      } else {
        //val inRenames = inValues.mapValues { it.value[0] }
        //val outRenames = reachDefsWithPhis[n to "out"]!!.groupBy { it.binder }.mapValues { it.value[0] }
        val (inRenames, outRenames) = nonPhiRenames[n]!!
        Instr(
            n.lNames.map { outRenames[it]!!.valName }.toSet(),
            n.rNames.map { inRenames[it]?.valName ?: it }.toSet())
      }
    }.toMap()
    cfgWithPhis.transitions.forEach { (n, succ) ->
      ssa.addInstr(instrMap[n]!!, succ.map { instrMap[it]!! })
    }
    ssa.complete()

    printCFG(ssa, emptyMap<Point,Unit>())*/

    println()
  }

}

fun convertToAndOutOfSSA(it: FunctionDef): FunctionDef {
  val (ssaF, phiFuncs) = convertToSSA(it)
  return destructSSA(ssaF, phiFuncs)
}

fun destructSSA(ssaF: FunctionDef, phiFuncs: List<PhiFunc>): FunctionDef {
  return ssaF.copy(body = insertCopies(ssaF.body, phiFuncs.toCopies()))
}

fun convertToSSA(it: FunctionDef): Pair<FunctionDef, List<PhiFunc>> {
  val analyses = FuncAnalyses(it)

  val ssaInfo = calcSSARenames(it, analyses)
  val enterRenames = ssaInfo.enter
  val before = ssaInfo.before
  val after = ssaInfo.after

  val renamer = StmtRenamer(before, after)
  val ssaF = it.copy(args = renameFuncArguments(it.args, enterRenames),
          body = renamer.renameStmts(it.body))

  fun remapEdgeSource(e: EdgeSource): EdgeSource = when (e) {
    is BlockIndex -> e.copy(block = renamer.blockRemap[e.block]!!)
    //is LoopTest -> e.copy(loop = renamer.stmtRemap[e.loop]!!)
    is Undef -> e
  }

  val phiFuncs = ssaInfo.phiFuncs.map {
    it.copy(vars = it.vars.map { (v, e) -> v to remapEdgeSource(e) }) }
  return Pair(ssaF, phiFuncs)
}

typealias RENAMES = Map<String,String>
typealias COPY = Pair<String,String>
class SSAInfo(
    val enter: RENAMES,
    val before: StmtAnnoMap<RENAMES>,
    val after: StmtAnnoMap<RENAMES>,
    val phiFuncs: List<PhiFunc>
)

data class PhiFunc(val phiVar: String, val vars: List<Pair<String,EdgeSource>>)

fun List<PhiFunc>.toCopies(): Map<EdgeSource,List<Pair<String,String>>> {
  return this.flatMap { it.vars.map { (v,e) -> e to (v to it.phiVar) } }
          .groupBy { it.first }.mapValues { it.value.map { it.second } }
}

fun calcSSARenames(f: FunctionDef, analyses: FuncAnalyses): SSAInfo {
  val cfg = analyses.cfg
  val phiNodes = calcPhiNodes2(analyses)
  val (cfgWithPhis, oldToNewInstrMap) = insertPhiNodes(cfg, phiNodes)
  val phiNodes2 = cfgWithPhis.transitions.keys.minus(oldToNewInstrMap.values)

  val phiAnalyses = CFGAnalyses(cfgWithPhis)
  val reachDefsWithPhis = phiAnalyses.reachingDefs

  /*val newRDomFr = reverse(phiAnalyses.dom.domFrontier)
  val liveVarsWithPhis = phiAnalyses.liveVars
  val _phiRenames = newRDomFr.keys.map { n ->
    val preds = cfgWithPhis.reverse[n]!!
    val rdefs = reachDefsWithPhis[n to "in"]!!
    val live = liveVarsWithPhis[n to "in"]!!
    val reaching = rdefs.map { it.binder }.toSet()
    val shouldBeAvailableVars = live.intersect(reaching)
    val nameToValsMap = shouldBeAvailableVars.map { it to mutableSetOf<Val>() }.toMap().toMutableMap()
    preds.forEach { i ->
      val vals = reachDefsWithPhis[i to "out"]!!.groupBy { it.binder }
      shouldBeAvailableVars.forEach { v ->
        val vs = vals[v] ?: fail("$v has no reaching definition at $i")
        nameToValsMap[v]!!.addAll(vs)
      }
    }
    val phiVars = n.lNames
    n to reachDefsWithPhis[n to "out"]!!.groupBy { it.binder }.filterKeys { it in phiVars }.mapValues { it.value[0] }
  }.toMap()*/

  fun valsMap(vals: Collection<Val>, phiVars: Set<String>)
      = vals.filter { it.binder in phiVars }
      .groupBy { it.binder }
      .mapValues { if (it.value.size != 1) fail() else it.value[0].valName }

  val phiFuncs = phiNodes.flatMap { (n, vs) ->
    val preds = cfg.reverse[n]!!
    preds.flatMap { i ->
      val edgeSource = analyses.edgeLabeling[i to n]!!
      val pre = valsMap(reachDefsWithPhis[oldToNewInstrMap[i]!! to "out"]!!, vs)
      val phi = valsMap(reachDefsWithPhis[oldToNewInstrMap[n]!! to "in"]!!, vs)

      vs.map { phi[it]!! to (pre[it]!! to edgeSource) }
    }
  }.groupBy { it.first }.mapValues { it.value.map { it.second } }.map { (k,v) -> PhiFunc(k,v) }

  val nonPhiRenames: Map<Instr, Pair<Map<String, Val>, Map<String, Val>>> = cfgWithPhis.transitions.keys.flatMap { n ->
    if (n !in phiNodes2) {
      val inValues = reachDefsWithPhis[n to "in"]!!.groupBy { it.binder }
      val inRenames = inValues.mapValues { it.value[0] }
      val outRenames = reachDefsWithPhis[n to "out"]!!.groupBy { it.binder }.mapValues { it.value[0] }
      listOf(n to Pair(inRenames, outRenames))
    } else {
      emptyList()
    }
  }.toMap()

  val before = StmtAnnoMap<Map<String,String>>()
  val after = StmtAnnoMap<Map<String,String>>()
  analyses.cfgStmtMap.forEach { (s, p) ->
    val firstI = oldToNewInstrMap[p.first]!!
    val lastI = oldToNewInstrMap[p.second]!!
    before[s] = nonPhiRenames[firstI]!!.first.mapValues { it.value.valName }
    if (s is Assign || s is AnnAssign || s is AugAssign) {
      after[s] = nonPhiRenames[firstI]!!.second.mapValues { it.value.valName }
    } else {
      after[s] = nonPhiRenames[lastI]!!.second.mapValues { it.value.valName }
    }
  }

  val funcInitI = cfgWithPhis.transitions[cfgWithPhis.first]!![0]
  val initialRenames = reachDefsWithPhis[funcInitI to "out"]!!
      .groupBy { it.binder }.mapValues { it.value[0].valName }
  return SSAInfo(initialRenames, before, after, phiFuncs)
}

fun calcPhiNodes2(analyses: FuncAnalyses): Map<Instr, Set<String>> {
  val cfg = analyses.cfg
  val domFr = analyses.cfgAnalyses.dom.domFrontier
  val liveVars = analyses.cfgAnalyses.liveVars
  val instrToDefsMap = calcValueDefs(cfg)
  val instrToNamesMap = instrToDefsMap.mapValues { it.value.map { it.binder } }
  val nameToStmtsMap = reverse(instrToNamesMap)

  val res = nameToStmtsMap.mapValues { (name, instrs) ->
    val queue = instrs.toMutableList()
    val res = mutableSetOf<Instr>()
    var i = 0
    while (i < queue.size) {
      val instr = queue[i]
      i += 1
      val nodes = domFr.getOrDefault(instr, emptySet())
      if (nodes.isNotEmpty()) {
        for(n in nodes) {
          if (name in liveVars[n to "in"]!!)
            res.add(n)
        }
        queue.addAll(nodes.minus(queue.subList(0,i)))
      }
    }
    res.toSet()
  }
  return reverse(res).mapValues { it.value.toSet() }
}

fun calcPhiNodes(analyses: FuncAnalyses): Map<Instr, Set<String>> {
  val cfg = analyses.cfg
  val liveVars = analyses.cfgAnalyses.liveVars
  val reachingDefs = analyses.cfgAnalyses.reachingDefs
  val doms = analyses.cfgAnalyses.dom.domsOf
  val rdomFr = reverse(analyses.cfgAnalyses.dom.domFrontier)

  val phiNodes = rdomFr.keys.map { n ->
    val preds = cfg.reverse[n]!!
    if (preds.any { n in doms[it]!! }) {
      //println("cycle")
    } else {
      //println("non cycle")
    }

    val rdefs = reachingDefs[n to "in"]!!
    val live = liveVars[n to "in"]!!
    val reaching = rdefs.map { it.binder }.toSet()
    val shouldBeAvailableVars = live.intersect(reaching)
    val nameToValsMap = shouldBeAvailableVars.map { it to mutableSetOf<Val>() }.toMap().toMutableMap()
    preds.forEach { i ->
      val vals = reachingDefs[i to "out"]!!.groupBy { it.binder }
      shouldBeAvailableVars.forEach { v ->
        val vs = vals[v] ?: fail("$v has no reaching definition at $i")
        nameToValsMap[v]!!.addAll(vs)
      }
    }
    n to nameToValsMap.filter { it.value.size > 1 }.keys
  }.toMap()
  return phiNodes
}
