package onotole

fun isPhiCall(e: TExpr): Boolean = e is Call && e.func == Name("<Phi>", ExprContext.Load)

fun destructSSA(ssa: CFGraphImpl, dom: DominanceAnalysis<CPoint>): Pair<CFGraphImpl,ExprRenamer> {
  val phiStmts = ssa.blocks.values.flatMap { it.stmts }.filter { isPhiCall(it.rval) }
      .map { (it.lval as VarLVal).v to (it.rval as Call).args.map { (it as Name).id } }
  val varToDef = ssa.blocks.flatMap { (l,bb) ->
    bb.stmts.mapIndexed { i,s ->
      if (s.lval is VarLVal) listOf(s.lval.v to (l to i))
      else emptyList()
    }.flatten()
  }.toMap()
  val phiWebs = mutableMapOf<String,Set<String>>()
  phiStmts.forEach { (v, vs) ->
    val vars = vs.plus(v).toSet()
    val newWeb = vars.flatMap { phiWebs.getOrElse(it) { setOf(it) } }.toSet()
    vars.forEach {
      phiWebs[it] = newWeb
    }
  }
  val phiRenames = phiWebs.mapValues { it.value.sortedBy { it.length }.first() }
  phiRenames.values.toSet().forEach { v ->
    val webVars = phiWebs[v]!!
    val commonDominators = webVars.map { dom.domsOf[varToDef[it]!!.first]!! }.reduce { a,b -> a.intersect(b) }
    val cdoms = commonDominators.toMutableSet()
    commonDominators.forEach {
      val immDom = dom.immDomOf[it]
      if (immDom != null && immDom in cdoms)
        cdoms.remove(immDom)
    }
    if (cdoms.size != 1)
      fail()
    cdoms.first()
  }

  val renamer = ExprRenamer(phiRenames, phiRenames)
  val newBlocks = ssa.blocks.mapValues {
    val bb = it.value
    val newStmts = mutableListOf<StmtInstr>()
    bb.stmts.forEach { s ->
      if (!isPhiCall(s.rval)) {
        val newRval = renamer.renameExpr(s.rval)
        val newLVal = when (s.lval) {
          is EmptyLVal -> s.lval
          is VarLVal -> VarLVal(renamer.renameName(s.lval.v, ExprContext.Store), s.lval.t)
          is FieldLVal -> FieldLVal(renamer.renameExpr(s.lval.r), s.lval.f)
          is SubscriptLVal -> SubscriptLVal(renamer.renameExpr(s.lval.r), renamer.renameSlice(s.lval.i))
        }
        newStmts.add(StmtInstr(newLVal, newRval))
      }
    }
    BasicBlock(newStmts, bb.branch.discrVar?.let { Branch(renamer.renameName(it, ExprContext.Load), bb.branch.next) } ?: bb.branch)
  }
  val offsets = ssa.blocks.mapValues {
    it.value.stmts.filter { isPhiCall(it.rval) }.size
  }
  val newLoops = ssa.loops
  val newIfs = ssa.ifs.map {
    val offset = offsets[it.test]!!
    if (offset > 0) {
      IfInfo2(it.head.first to (it.head.second - offset), it.test, it.body, it.orelse, it.exit)
    } else {
      it
    }
  }
  return CFGraphImpl(newBlocks.toList(), newLoops, newIfs) to renamer
}