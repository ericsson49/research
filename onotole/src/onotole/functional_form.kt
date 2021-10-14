package onotole

fun <T> ensureSingle(c: Collection<T>): T {
  return if (c.size != 1) fail("should be single")
  else c.first()
}

fun getRegionExits(graph: Graph<CPoint>, region: Collection<CPoint>): List<CPoint> {
  return region.flatMap { graph.transitions[it]!!.filter { it !in region } }
}

fun freshVar(g: CFGraphImpl): String {
  val prefix = "tmp_td_"
  val tmpVars = getLocalVars(g).filter { it.startsWith(prefix) }.toSet()
  var num = tmpVars.size
  while ("$prefix$num" in tmpVars) {
    num++
  }
  return "$prefix$num"
}

fun getLocalVars(cfg: CFGraphImpl) = cfg.blocks.values.flatMap { it.stmts.flatMap { it.lNames } }.toSet()

fun extractMethod(
    cfg: CFGraphImpl, cfgAnalyses: ICFGAnalyses<CPoint>, method: List<CPoint>, typing: Map<String, RTType>,
    name: String, recursive: Boolean): Pair<CFGraphImpl, CFGraphImpl> {
  val entry = ensureSingle(cfg.transitions.keys.minus(cfg.reverse.keys))
  val methExit = ensureSingle(getRegionExits(cfg, method))
  val methEntry = ensureSingle(getRegionExits(cfg, cfg.blocks.keys.minus(method)))

  fun lvs(p: CPoint, before: Boolean) =
      cfgAnalyses.liveVars[p to (if (before) "in" else "out")]!!.filter { !it.startsWith("<") }.toSet()

  val before = lvs(methEntry, true).toList()
  val after = if (recursive) before else lvs(methExit, true)

  val stmts = addCallSite(cfg, name, before, after)
  val callSite = BasicBlock(stmts, Branch(null, listOf(methExit)))

  val newGraphNodes = cfg.blocks.keys.minus(method).plus(methEntry)
  val newGraphBlocks = newGraphNodes.map { l ->
    l to (if (l == methEntry) callSite else cfg.blocks[l]!!)
  }
  val newLoops = cfg.loops.filter { it.head !in method }
  val newIfs = cfg.ifs.filter { it.head.first !in method }
  val newGraph = CFGraphImpl(newGraphBlocks, newLoops, newIfs)

  val methNodes = listOf(entry).plus(method).plus(methExit)
  val methProlog = BasicBlock(
      before.mapIndexed { i, v -> StmtInstr(VarLVal(v, typing[v]!!), mkCall("<Parameter>", listOf(Num(i)))) },
      Branch(null, listOf(methEntry)))
  val methRet = BasicBlock(listOf(StmtInstr(VarLVal("<return>"), Tuple(after.map { mkName(it) }, ExprContext.Load))), Branch(null, emptyList()))
  val methBlocks = methNodes.map { l ->
    l to (when(l) {
      entry -> methProlog
      methExit -> methRet
      else -> cfg.blocks[l]!!
    })
  }
  val methLoops = cfg.loops.filter { it.head in method }
  val methIfs = cfg.ifs.filter { it.head.first in method }
  val methGraph = CFGraphImpl(methBlocks, methLoops, methIfs)
  return newGraph to methGraph
}

private fun addCallSite(g: CFGraphImpl, name: String, args: Collection<String>, res: Collection<String>): List<StmtInstr> {
  val tmp = freshVar(g)
  return listOf(StmtInstr(VarLVal(tmp), mkCall(name, args.map { mkName(it) })))
      .plus(res.mapIndexed { i, v -> StmtInstr(VarLVal(v), mkSubscript(tmp, mkIndex(i))) })
}

fun transformLoopsToFuncs(f: FunctionDef): List<FunctionDef> {
  val cfg_ = convertToCFG(desugar(f))
  val ssa_ = convertToSSA(cfg_)
  val (cfg, renames) = destructSSA(ssa_, getFuncAnalyses(cfg_).cfgAnalyses.dom)

  //val f2 = convertToAndOutOfSSA(desugar(f))
  //val cfg = convertToCFG(f2)
  val analyses = getFuncAnalyses(cfg)
  val dom = analyses.cfgAnalyses.dom
  val domTree = dom.domTree

  val typing = _inferTypes_CFG(cfg)

  val trans = cfg.transitions.flatMap { (k, v) -> v.map { k to it } }

  fun getLoopNodes(cfg: CFGraphImpl, dom: DominanceAnalysis<CPoint>, head: CPoint): List<CPoint> {
    val queue = mutableListOf(head)
    val loopNodes = mutableListOf<CPoint>()
    while (queue.isNotEmpty()) {
      val curr = queue.removeAt(0)
      loopNodes.add(curr)
      val next = cfg.reverse[curr]!!.filter { head in dom.domsOf[it]!! }
      queue.addAll(next.minus(loopNodes))
    }
    return loopNodes
  }

  fun getExitEdges(cfg: CFGraphImpl, loop: Collection<CPoint>): List<Pair<CPoint, CPoint>> {
    return loop.flatMap { n -> cfg.transitions[n]!!.filter { it !in loop }.map { n to it } }
  }

  val backEdges = trans.filter { it.second in dom.domsOf[it.first]!! }
  val loopHeads = backEdges.map { it.second }

  if (loopHeads.toSet() != cfg.loops.map { it.head }.toSet()) fail()

  //loopHeads.forEach { head ->
  //  val loopNodes = getLoopNodes(head)
  //  val exitEdges = getExitEdges(loopNodes)
  //}

  val loopTree = buildLoopTree(loopHeads.map { it to getLoopNodes(cfg, dom, it) }.toMap())
  fun topoSort(ns: Collection<CPoint>): List<CPoint> {
    val children = ns.flatMap { n -> loopTree[n] ?: emptyList() }
    return (if (children.isEmpty()) emptyList() else topoSort(children)).plus(ns)
  }

  val sorted = topoSort(loopHeads.minus(loopTree.values.flatten()))

  var currCfg = cfg
  val loopFuncs = mutableListOf<FunctionDef>()
  val tmpVars = mutableSetOf<String>()
  sorted.forEachIndexed { i, head ->
    val currDom = DominanceAnalysis(currCfg)
    val loopNodes = getLoopNodes(currCfg, currDom, head)
    val exitEdges = getExitEdges(currCfg, loopNodes)
    val exits = exitEdges.map { it.second }
    if (exits.size > 1 || exits[0] != cfg.loops.find { it.head == head }!!.exit)
      TODO()

    val loopMethName = f.name + "\$${i + 1}"
    fun lvs(p: CPoint, before: Boolean) =
        analyses.cfgAnalyses.liveVars[p to (if (before) "in" else "out")]!!.filter { !it.startsWith("<") }.toSet()

    val argNames = lvs(head, true).toList()
    val argTypes = argNames.map { typing[it]!! }
    val args = argNames.zip(argTypes).map { Arg(it.first, it.second.toTExpr()) }
    val retType = if (args.size == 1) argTypes[0] else TPyTuple(argTypes)
    val (g1, g2) = extractMethod(currCfg, analyses.cfgAnalyses, loopNodes, typing, loopMethName, true)
    val loopCfg = loopToRecursion(loopMethName, argNames, g2)

    val loopFD = reconstructFuncDef(
        FunctionDef(loopMethName,
            args = Arguments(args = args),
            returns = retType.toTExpr()
        ), loopCfg)
    tmpVars.addAll(getLocalVars(loopCfg).minus(getLocalVars(g2)))
    val loopFDResugared = resugarTupleDestructs(tmpVars, loopFD)
    loopFuncs.add(loopFDResugared)
    currCfg = g1
  }
  val resultResugared = resugarTupleDestructs(getLocalVars(currCfg).minus(getLocalVars(cfg)), reconstructFuncDef(f, currCfg))
  return listOf(resultResugared).plus(loopFuncs)
}

fun buildLoopTree(loops: Map<CPoint,Collection<CPoint>>): Map<CPoint, List<CPoint>> {
  val enclosingLoopsOf = mutableMapOf<CPoint, MutableSet<CPoint>>()
  loops.forEach { (h, b) ->
    b.intersect(loops.keys).forEach {
      if (it != h)
        enclosingLoopsOf.getOrPut(it) { mutableSetOf() }.add(h)
    }
  }
  val parentLoop = mutableMapOf<CPoint, CPoint>()
  enclosingLoopsOf.forEach {
    val parentAncestors = it.value.map { (enclosingLoopsOf[it] ?: mutableSetOf()).toSet() }.reduce { a,b -> a.union(b) }
    val parent = it.value.minus(parentAncestors)
    if (parent.isNotEmpty()) {
      parentLoop[it.key] = ensureSingle(parent)
    }
  }
  val a1 = parentLoop.map { it.key to listOf(it.value) }
  val a2 = loops.keys.minus(parentLoop.keys).map<CPoint, Pair<CPoint, List<CPoint>>> { it to emptyList() }
  return reverse(a1
      .plus(a2).toMap())
}

fun loopToRecursion(f: String, args: List<String>, cfg: CFGraphImpl): CFGraphImpl {
  val entry = cfg.entry
  val head = ensureSingle(cfg.blocks[entry]!!.branch.next)
  val exit = ensureSingle(cfg.transitions.values.flatten().minus(cfg.reverse.values.flatten()))
  val backEdgeOrigins = cfg.reverse[head]!!.filter { it != entry }.toSet()
  val replaces = backEdgeOrigins.flatMap { n ->
    val b = cfg.get(n)
    val callSite = addCallSite(cfg, f, args, args)
    if (b.branch.next.size == 1) {
      listOf(n to BasicBlock(b.stmts.plus(callSite), b.branch.copy(next = listOf(exit))))
    } else {
      TODO()
    }
  }
  val newBlocks = cfg.blocks.plus(replaces).toList()
  val whileInfo = cfg.loops.find { it.head == head }!!
  return CFGraphImpl(newBlocks, cfg.loops.minus(whileInfo), cfg.ifs.plus(
      IfInfo2(whileInfo.entry, head, whileInfo.body, whileInfo.exit, whileInfo.exit)))
}

fun resugarTupleDestructs(tmpVars: Set<String>, f: FunctionDef): FunctionDef {
  fun applyTransform(stmts: List<Stmt>, transform: (List<Stmt>) -> List<Stmt>): List<Stmt> {
    return transform(stmts).flatMap {
      when (it) {
        is If -> listOf(it.copy(body = applyTransform(it.body, transform), orelse = applyTransform(it.orelse, transform)))
        is While -> listOf(it.copy(body = applyTransform(it.body, transform)))
        is For -> listOf(it.copy(body = applyTransform(it.body, transform)))
        else -> listOf(it)
      }
    }
  }
  fun transform(stmts: List<Stmt>): List<Stmt> {
    val tmpAssigns = stmts.filter { it is Assign && it.target is Name && it.target.id in tmpVars }
    var resStmts = stmts

    tmpAssigns.forEach { s ->
      val a = s as Assign
      val tmpVar = (a.target as Name).id
      val idx = resStmts.indexOf(s)
      if (idx == -1) fail()
      val assigns = resStmts.subList(idx + 1, resStmts.size).takeWhile {
        it is Assign && it.target is Name && it.value is Subscript && it.value.value == mkName(tmpVar)
            && it.value.slice is Index && it.value.slice.value is Num
      }
      val vars = assigns.map {
        val assign = it as Assign
        val v = (assign.target as Name).id
        val i = (((assign.value as Subscript).slice as Index).value as Num).n as Int
        v to i
      }.toMap()
      if (vars.values.toSet() != (0 until vars.size).toSet()) fail()
      val varNames = vars.keys.sortedBy { vars[it]!! }
      val newAssign = Assign(Tuple(varNames.map { mkName(it, true) }, ExprContext.Store), a.value)
      resStmts = resStmts.subList(0, idx).plus(newAssign).plus(resStmts.subList(idx + 1 + assigns.size, resStmts.size))
    }

    return resStmts
  }
  return f.copy(body = applyTransform(f.body, ::transform))
}