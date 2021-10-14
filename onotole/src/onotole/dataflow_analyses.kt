package onotole

import onotole.lib_defs.BLS
import onotole.lib_defs.PyLib
import onotole.lib_defs.SSZLib
import java.nio.file.Files
import java.nio.file.Paths

interface IInstr {
  val lNames: Set<String>
  val rNames: Set<String>
}
class Instr(override val lNames: Set<String>, override val rNames: Set<String>): IInstr

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

fun <L, N: IInstr> makeLiveVarProblem(cfg: CFGraph<L, N>): DFAProblem<Pair<L,String>,VarSet> {
  fun genKillMaker(n: IInstr) = Pair(n.rNames, n.lNames)
  fun genKillMaker(l: L) = genKillMaker(cfg.get(l))
  return makeSetDFAProblem(cfg, forward = false, must = false, init = ::emptySet, genKillMaker = ::genKillMaker)
}

data class Val(val valName: String, val binder: String) {
  override fun toString() = valName
}
fun <L, N: IInstr> makeReachingDefsProblem(cfg: CFGraph<L, N>): DFAProblem<Pair<L,String>,Set<Val>> {
  val instrToDefsMap = calcValueDefs(cfg)
  val defs = instrToDefsMap.values.flatten()
  val nameToDefsMap = defs.groupBy { it.binder }.mapValues { it.value.toSet() }
  fun genKillMaker(l: L): Pair<Set<Val>,Set<Val>> {
    val gen = instrToDefsMap[l]!!
    val kill = gen.flatMap { vl -> nameToDefsMap[vl.binder]!!.minus(vl) }.toSet()
    return Pair(gen, kill)
  }
  return makeSetDFAProblem(cfg, true, false, ::emptySet, ::genKillMaker)
}

fun <L, N: IInstr> calcValueDefs(cfg: CFGraph<L, N>): Map<L, Set<Val>> {
  val valuesAcc = mutableMapOf<String, Int>()
  fun newName(s: String): String {
    val i = valuesAcc.getOrDefault(s, -1) + 1
    valuesAcc[s] = i
    return if (i == 0) s else s + "_" + i
  }
  return cfg.transitions.keys.toList().reversed().map { l ->
    val i = cfg.get(l)
    l to i.lNames.map { Val(newName(it), it) }.toSet()
  }.toMap()
}

fun <N> makeDominatorsProblem(cfg: Graph<N>): DFAProblem<Pair<N,String>,Set<N>> {
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

fun <N> calcIDom(doms: Map<N,Set<N>>, cfg: Graph<N>): Map<N,N> {
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

fun <N> calcDomFrontier(cfg: Graph<N>, idom: Map<N,N>): Map<N,Set<N>> {
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

fun <T: IInstr> insertPhiNodes(cfg: Graph<T>, phiNodes: Map<T,Set<String>>): Pair<CFG<Instr>, Map<T,Instr>> {
  fun copy(i: IInstr): Instr = Instr(i.lNames, i.rNames)
  val transitions = mutableMapOf<Instr, List<Instr>>()
  val dupInstrs = cfg.transitions.keys.map { it to copy(it) }.toMap()
  val phiInstrs = phiNodes.mapValues { Instr(it.value, it.value) }
  phiInstrs.forEach { (i, phiI) ->
    transitions[phiI] = listOf(dupInstrs[i]!!)
  }
  cfg.transitions.forEach { (n, succ) ->
    transitions[dupInstrs[n]!!] = succ.map { phiInstrs[it] ?: dupInstrs[it]!! }
  }
  return CFG(transitions) to dupInstrs
}

fun <N> calcDom(cfg: Graph<N>)
    = solve(makeDominatorsProblem(cfg))
    .filterKeys { it.second == "out" }
    .mapKeys { it.key.first }

class DominanceAnalysis<N>(cfg: Graph<N>) {
  val domsOf: Map<N,Set<N>> = calcDom(cfg)
  val immDomOf: Map<N,N> = calcIDom(domsOf, cfg)
  val domTree = reverse(immDomOf.mapValues { setOf(it.value) })
  val domFrontier: Map<N, Set<N>> = calcDomFrontier(cfg, immDomOf)
  val domFrontierPlus = domFrontier.mapValues { transClosure(it.value) { n -> domFrontier[n]!! } }
}

interface ICFGAnalyses<T> {
  val dom: DominanceAnalysis<T>
  val liveVars: Map<Pair<T,String>,VarSet>
  val reachingDefs: Map<Pair<T,String>,Set<Val>>
}

class CFGAnalyses<L,N: IInstr>(cfg: CFGraph<L,N>): ICFGAnalyses<L> {
  override val dom: DominanceAnalysis<L> = DominanceAnalysis(cfg)
  override val liveVars: Map<Pair<L,String>,VarSet> = solve(makeLiveVarProblem(cfg))
  override val reachingDefs: Map<Pair<L,String>,Set<Val>> = solve(makeReachingDefsProblem(cfg))
}

val getFuncAnalyses = MemoizingFunc<CFGraphImpl, FuncAnalyses<CPoint, BasicBlock>> { cfg ->
  FuncAnalyses(cfg, emptyMap(), emptyMap()) }

class FuncAnalyses<L, N: IInstr>(
    val cfg: CFGraph<L, N>,
    val cfgStmtMap: Map<Stmt,Pair<L,L>>,
    val edgeLabeling: Map<Pair<L,L>,EdgeSource>) {
  val cfgAnalyses: CFGAnalyses<L, N> = CFGAnalyses(cfg)
}

fun <T: IInstr> makeFuncAnalyses(f: FunctionDef, proc: NodeProcessor<T>): FuncAnalyses<T, T> {
  val (cfg, stmtMap, edgeLabeling) = CFGBuilder.makeCFG(f, proc)
  return FuncAnalyses(cfg, stmtMap, edgeLabeling)
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
    target = Name(id = p.second, ctx = ExprContext.Store),
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
    KotlinGen("", emptySet()).genFunc(newF).forEach { println(it) }
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

fun convertToAndOutOfSSA(it: FunctionDef) = convertToAndOutOfSSA(it, SimpleProcessor())

fun <T: IInstr> convertToAndOutOfSSA(it: FunctionDef, proc: NodeProcessor<T>): FunctionDef {
  val (ssaF, phiFuncs) = convertToSSA(it, proc)
  return destructSSA(ssaF, phiFuncs)
}

fun destructSSA(ssaF: FunctionDef, phiFuncs: List<PhiFunc>): FunctionDef {
  return ssaF.copy(body = insertCopies(ssaF.body, phiFuncs.toCopies()))
}

fun <T: IInstr> convertToSSA(it: FunctionDef, proc: NodeProcessor<T>): Pair<FunctionDef, List<PhiFunc>> {
  val analyses = makeFuncAnalyses(it, proc)

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

val convertToSSA = MemoizingFunc(::makeSSAFromCFG)

private fun makeSSAFromCFG(cfg: CFGraphImpl): CFGraphImpl {
  val analyses = getFuncAnalyses(cfg) //FuncAnalyses(cfg, emptyMap(), emptyMap())
  return makeSSAFromCFG(cfg, analyses)
}

fun makeSSAFromCFG(cfg: CFGraphImpl, analyses: FuncAnalyses<CPoint, BasicBlock>): CFGraphImpl {
  val phiFuncs = calcPhiNodes2(analyses)

  val domsOf = analyses.cfgAnalyses.dom.domsOf
  val domTree = analyses.cfgAnalyses.dom.domTree
  val start = cfg.transitions.keys.minus(cfg.reverse.keys).toList()[0]

  fun preorder(node: CPoint): List<CPoint> {
    return listOf(node).plus((domTree[node] ?: emptyList()).flatMap { preorder(it) })
  }
  val varVersions = mutableMapOf<String,Int>()
  val reachingDef = mutableMapOf<String,String?>()
  val definitions = mutableMapOf<String,Pair<CPoint,Int>>()
  fun defDominatesInstr(v: String, i: Pair<CPoint,Int>): Boolean {
    val d = definitions[v]!!
    return if (d.first == i.first) d.second <= i.second
    else d.first in domsOf[i.first]!!
  }
  fun updateReachingDef(v: String, i: Pair<CPoint,Int>) {
    var r = reachingDef[v]
    while (!(r == null || defDominatesInstr(r, i))) {
      r = reachingDef[r]
    }
    reachingDef[v] = r
  }
  val phiVarVersions = mutableMapOf<CPoint,Map<String,String>>()
  val phiVarInVersions = mutableMapOf<CPoint,MutableMap<CPoint,MutableMap<String,String>>>()
  val newBlocks = preorder(start).map { l ->
    val bb = cfg.get(l)
    fun step1(v: String, offset: Int): String {
      updateReachingDef(v, l to offset)
      val res = reachingDef[v] ?: "<undef>"
      return if (res.endsWith("_0")) v else res
    }
    fun step2(v: String, offset: Int): String {
      updateReachingDef(v, l to offset)
      val newVersion = varVersions.getOrPut(v) { -1 } + 1
      varVersions[v] = newVersion
      val newV = v + "_" + newVersion
      definitions[newV] = l to offset
      reachingDef[newV] = reachingDef[v]
      reachingDef[v] = newV
      return if (newVersion == 0) v else newV
    }
    if (l in phiFuncs) {
      phiVarVersions[l] = phiFuncs[l]!!.map { v -> v to step2(v, -1) }.toMap()
    }
    val newStmts = bb.stmts.mapIndexed { i,s ->
      val inRenames = s.rNames.intersect(varVersions.keys).map { v -> v to step1(v, i) }.toMap()
      val outRenames = s.lNames.map { v -> v to step2(v, i) }.toMap()
      val renamer = ExprRenamer(inRenames, outRenames)

      val newLVal = when (s.lval) {
        is EmptyLVal -> s.lval
        is VarLVal -> VarLVal(renamer.renameName(s.lval.v, ExprContext.Store), s.lval.t)
        is FieldLVal -> FieldLVal(renamer.renameExpr(s.lval.r), s.lval.f)
        is SubscriptLVal -> SubscriptLVal(renamer.renameExpr(s.lval.r), renamer.renameSlice(s.lval.i))
      }
      val newRVal = renamer.renameExpr(s.rval)
      StmtInstr(newLVal, newRVal)
    }
    val newBranch = if (bb.branch.discrVar == null) bb.branch
    else {
      val nn = step1(bb.branch.discrVar, bb.stmts.size)
      Branch(nn, bb.branch.next)
    }

    cfg.transitions[l]!!.intersect(phiFuncs.keys).forEach { succ ->
      phiFuncs[succ]!!.forEach { v ->
        updateReachingDef(v, l to bb.stmts.size)
        val newV = reachingDef[v] ?: "<undef>"
        phiVarInVersions.getOrPut(succ) { mutableMapOf() }.getOrPut(l) { mutableMapOf() } [v] =
            if (newV.endsWith("_0")) v else newV
      }
    }

    l to BasicBlock(newStmts, newBranch)
  }
  val newBlocksWithPhis = newBlocks.map { (l, bb) ->
    val newBB = if (l in phiFuncs) {
      val phis = phiVarVersions[l]!!.map { (v, vv) ->
        val preds = cfg.reverse[l]!!
        val newInVS = preds.map { p -> mkName(phiVarInVersions[l]!![p]!![v]!!) }
        StmtInstr(VarLVal(vv), mkCall("<Phi>", newInVS))
      }
      BasicBlock(phis.plus(bb.stmts), bb.branch)
    } else bb
    l to newBB
  }
  val newLoops = cfg.loops
  val newIfs = cfg.ifs.map {
    if (it.test in phiFuncs) {
      val offset = phiFuncs[it.test]!!.size
      IfInfo2(it.head.first to (it.head.second + offset), it.test, it.body, it.orelse, it.exit)
    } else it
  }
  return CFGraphImpl(newBlocksWithPhis, newLoops, newIfs)
}

fun <T: IInstr> calcSSARenames(f: FunctionDef, analyses: FuncAnalyses<T, T>): SSAInfo {
  val cfg = analyses.cfg
  val phiNodes = calcPhiNodes2(analyses)
  val (cfgWithPhis, oldToNewInstrMap) = insertPhiNodes(cfg, phiNodes)
  val phiNodes2 = cfgWithPhis.transitions.keys.minus(oldToNewInstrMap.values)

  val phiAnalyses = CFGAnalyses(cfgWithPhis)
  val reachDefsWithPhis = phiAnalyses.reachingDefs

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

fun <L, N : IInstr> calcPhiNodes2(analyses: FuncAnalyses<L, N>): Map<L, Set<String>> {
  val cfg = analyses.cfg
  val liveVars = analyses.cfgAnalyses.liveVars
  val nameToStmtsMap = reverse(cfg.transitions.keys.map { it to cfg.get(it).lNames }.toMap())

  /*val domFr = analyses.cfgAnalyses.dom.domFrontier
    val res = nameToStmtsMap.mapValues { (name, instrs) ->
    val queue = instrs.toMutableList()
    val res = mutableSetOf<T>()
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
  val finalRes = reverse(res).mapValues { it.value.toSet() }*/

  val domFrPlus = analyses.cfgAnalyses.dom.domFrontierPlus
  val minPhiFuncs = nameToStmtsMap
      .mapValues { it.value.flatMap { n -> domFrPlus[n]!! }.toSet() }
  // prune phi funcs
  return reverse(minPhiFuncs).mapValues { liveVars[it.key to "in"]!!.intersect(it.value) }
      .filterValues { it.isNotEmpty() }
}
