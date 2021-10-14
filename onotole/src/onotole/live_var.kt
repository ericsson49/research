package onotole

abstract class ForwardAnalysis<T> {
  val before = StmtAnnoMap<T>()
  val after = StmtAnnoMap<T>()

  abstract fun procAssign(t: TExpr, v: TExpr, p: Stmt, ins: T): T
  abstract fun merge(a: T, b: T): T
  abstract val bottom: T

  fun freshVar(): Name = TODO()

  fun procStmt(s: Stmt, ins: T): T = when(s) {
    is Assign -> procAssign(s.target, s.value, s, ins)
    is AnnAssign -> s.value?.let { procAssign(s.target, it, s, ins) } ?: ins
    is AugAssign -> procAssign(s.target, BinOp(s.target, s.op, s.value), s, ins)
    is If -> {
      val tmp: Name = freshVar()
      val testOut = procAssign(tmp, s.test, s, ins)
      val bodyOut = analyze(s.body, testOut)
      val orelseOut = analyze(s.orelse, testOut)
      merge(bodyOut, orelseOut)
    }
    is While -> {
      val tmp: Name = freshVar()
      val testIn = merge(ins, after[s.body[s.body.size-1]] ?: bottom)
      val testOut = procAssign(tmp, s.test, s, testIn)
      val bodyOut = analyze(s.body, testOut)
      testOut
    }
    else -> TODO()
  }
  private fun analyzeStmt(s: Stmt, ins: T): T {
    before[s] = ins
    val res = procStmt(s, ins)
    after[s] = res
    return res
  }
  fun analyze(c: List<Stmt>, ins: T): T = c.fold(ins, { acc, s -> analyzeStmt(s, acc)})
}

abstract class BackwardAnalysis<T> {
  val before = StmtAnnoMap<T>()
  val after = StmtAnnoMap<T>()
  abstract fun procStmt(s: Stmt, out: T): T
  private fun analyzeStmt(s: Stmt, out: T): T {
    after[s] = out
    val res = procStmt(s, out)
    before[s] = res
    return res
  }
  fun analyze(c: List<Stmt>, out: T): T = c.foldRight(out, { s, acc -> analyzeStmt(s, acc)})
}

fun liveVarAnalysis(c: Collection<TExpr>): Set<String> = c.flatMap { liveVarAnalysis(it) }.toSet()
fun liveVarAnalysis(e: TExpr): Set<String> {
  return when (e) {
    is Lambda -> liveVarAnalysis(e.body).minus(e.args.args.map { it.arg })
    is IfExp -> liveVarAnalysis(listOf(e.test, e.body, e.orelse))
    is GeneratorExp -> {
      if (e.generators.size != 1)
        fail("not yet implemented")
      val gen = e.generators[0]
      val out1 = liveVarAnalysis(e.elt).union(liveVarAnalysis(gen.ifs))
      val kill = getVarNamesInStoreCtx(gen.target)
      out1.minus(kill).union(liveVarAnalysis(gen.iter))
    }
    is Call -> liveVarAnalysis(listOf(e.func).plus(e.args).plus(e.keywords.map { it.value }))
    is Constant -> emptySet()
    is Attribute -> liveVarAnalysis(e.value)
    is Subscript -> {
      fun gatherExprs(s: TSlice) = when (s) {
        is Slice -> flatten(s.lower, s.upper, s.step)
        is Index -> listOf(s.value)
        else -> fail("unsupported $s")
      }
      liveVarAnalysis(listOf(e.value).plus(gatherExprs(e.slice)))
    }
    is Starred -> liveVarAnalysis(e.value)
    is Name -> setOf(e.id)
    is Tuple -> liveVarAnalysis(e.elts)
    else -> fail("unsupported $e")
  }
}

typealias VarSet = Set<String>

fun liveVarAnalysis(f: FunctionDef): Pair<StmtAnnoMap<VarSet>,StmtAnnoMap<VarSet>> {
  val analysis = LiveVarAnalysis()
  analysis.analyze(f.body, emptySet())
  return Pair(analysis.before, analysis.after)
}

class LiveVarAnalysis: BackwardAnalysis<VarSet>() {
  override fun procStmt(s: Stmt, out: VarSet): VarSet = when (s) {
    is Return -> s.value?.let(::liveVarAnalysis) ?: emptySet()
    is Assign -> {
      val kill = getVarNamesInStoreCtx(s.target)
      out.minus(kill).plus(liveVarAnalysis(s.value)).plus(getVarNamesInLoadCtx(s.target))
    }
    is AnnAssign -> {
      val kill = getVarNamesInStoreCtx(s.target)
      out.minus(kill).plus(s.value?.let(::liveVarAnalysis) ?: emptySet())
          .plus(getVarNamesInLoadCtx(s.target)).plus(extractNamesFromTypeExpr(s.annotation))
    }
    is AugAssign -> {
      out.plus(getVarNamesInStoreCtx(s.target)).plus(liveVarAnalysis(s.value))
          .plus(getVarNamesInLoadCtx(s.target))
    }
    is For -> {
      val bodyIn = analyze(s.body, out)
      val bodyKill = getVarNamesInStoreCtx(s.target)
      bodyIn.minus(bodyKill).union(out).union(liveVarAnalysis(s.iter))
    }
    is While -> {
      analyze(s.body, out).union(out).union(liveVarAnalysis(s.test))
    }
    is If -> analyze(s.body, out).union(analyze(s.orelse, out)).union(liveVarAnalysis(s.test))
    is Try -> {
      val finallyIn = analyze(s.finalbody, out)
      val orelseIn = analyze(s.orelse, finallyIn)
      val handlerIns = s.handlers.map {
        analyze(it.body, finallyIn).minus(it.name?.let(::setOf) ?: emptySet())
      }
      val blockOut = orelseIn.union(handlerIns.flatten())
      analyze(s.body, blockOut)
    }
    is Assert -> liveVarAnalysis(s.test).union(s.msg?.let(::liveVarAnalysis) ?: emptySet()).union(out)
    is Expr -> liveVarAnalysis(s.value).union(out)
    is Pass -> out
    is Break -> out
    is Continue -> out
    else -> fail("Not implemented yet $s")
  }

}