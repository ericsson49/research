
fun liveVarAnalysis(c: Collection<TExpr>): Set<String> = c.flatMap { liveVarAnalysis(it) }.toSet()
fun liveVarAnalysis(e: TExpr): Set<String> {
  return when (e) {
    is BoolOp -> liveVarAnalysis(e.values)
    is BinOp -> liveVarAnalysis(listOf(e.left, e.right))
    is UnaryOp -> liveVarAnalysis(e.operand)
    is Lambda -> liveVarAnalysis(e.body).minus(e.args.args.map { it.arg })
    is IfExp -> liveVarAnalysis(listOf(e.test, e.body, e.orelse))
    is PyDict -> liveVarAnalysis(e.keys.plus(e.values))
    is ListComp -> {
      if (e.generators.size != 1)
        fail("not yet implemented")
      val gen = e.generators[0]
      val out1 = liveVarAnalysis(e.elt).union(liveVarAnalysis(gen.ifs))
      val kill = getVarNamesInStoreCtx(gen.target)
      out1.minus(kill).union(liveVarAnalysis(gen.iter))
    }
    is DictComp -> {
      if (e.generators.size != 1)
        fail("not yet implemented")
      val gen = e.generators[0]
      val out1 = liveVarAnalysis(e.key).union(liveVarAnalysis(e.value)).union(liveVarAnalysis(gen.ifs))
      val kill = getVarNamesInStoreCtx(gen.target)
      out1.minus(kill).union(liveVarAnalysis(gen.iter))
    }
    is GeneratorExp -> {
      if (e.generators.size != 1)
        fail("not yet implemented")
      val gen = e.generators[0]
      val out1 = liveVarAnalysis(e.elt).union(liveVarAnalysis(gen.ifs))
      val kill = getVarNamesInStoreCtx(gen.target)
      out1.minus(kill).union(liveVarAnalysis(gen.iter))
    }
    is Compare -> liveVarAnalysis(listOf(e.left).plus(e.comparators))
    is Call -> liveVarAnalysis(listOf(e.func).plus(e.args).plus(e.keywords.map { it.value }))
    is Constant -> emptySet()
    is Attribute -> liveVarAnalysis(e.value)
    is Subscript -> {
      fun gatherExprs(s: TSlice) = when (s) {
        is Slice -> toList(s.lower).plus(toList(s.upper)).plus(toList(s.step))
        is Index -> listOf(s.value)
        else -> fail("unsupported $s")
      }
      liveVarAnalysis(listOf(e.value).plus(gatherExprs(e.slice)))
    }
    is Starred -> liveVarAnalysis(e.value)
    is Name -> setOf(e.id)
    is PyList -> liveVarAnalysis(e.elts)
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
      if (s.targets.size != 1)
        fail("unsupported")
      val kill = getVarNamesInStoreCtx(s.targets[0])
      out.minus(kill).plus(liveVarAnalysis(s.value))
    }
    is AnnAssign -> {
      val kill = getVarNamesInStoreCtx(s.target)
      out.minus(kill).plus(s.value?.let(::liveVarAnalysis) ?: emptySet())
    }
    is AugAssign -> {
      out.plus(getVarNamesInStoreCtx(s.target)).plus(liveVarAnalysis(s.value))
    }
    is For -> {
      if (s.orelse.size > 0)
        fail("not yet implemented")
      val bodyIn = analyze(s.body, out)
      val bodyKill = getVarNamesInStoreCtx(s.target)
      bodyIn.minus(bodyKill).union(out).union(liveVarAnalysis(s.iter))
    }
    is While -> {
      if (s.orelse.size > 0)
        fail("not yet implemented")
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
    is Assert -> liveVarAnalysis(s.test).union(s.msg?.let(::liveVarAnalysis) ?: emptySet())
    is Expr -> liveVarAnalysis(s.value).union(out)
    is Pass -> out
    is Break -> out
    is Continue -> out
    else -> fail("Not implemented yet $s")
  }

}