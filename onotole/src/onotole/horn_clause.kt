package onotole

fun main() {
  val f = FunctionDef("test",
      Arguments(args = listOf(Arg("k", mkName("int")))),
      returns = NameConstant(null),
      body = listOf(
          Assign(mkName("r", true), Num(0)),
          Assign(mkName("i", true), Num(0)),
          While(Compare(mkName("i"), listOf(ECmpOp.Lt), listOf(mkName("k"))), listOf(
              AugAssign(mkName("r", true), EBinOp.Add, Num(2)),
              AugAssign(mkName("i", true), EBinOp.Add, Num(1))
          )),
          Assert(Compare(mkName("r"), listOf(ECmpOp.Eq), listOf(BinOp(Num(2), EBinOp.Mult, mkName("k")))))
      )
  )
  val f2 = convertToAndOutOfSSA(desugarExprs(f))
  pyPrintFunc(f2)
  val cfg = convertToCFG(f2)
  printCFG(cfg)
  val analyses = FuncAnalyses(cfg, emptyMap(), emptyMap())

  fun rds(p: CPoint, before: Boolean) =
      analyses.cfgAnalyses.reachingDefs[p to (if (before) "in" else "out")]!!
  fun lvs(p: CPoint, before: Boolean) =
      analyses.cfgAnalyses.liveVars[p to (if (before) "in" else "out")]!!.filter { !it.startsWith("<") }.toSet()

  fun mkAtom(l: CPoint, ps: Collection<String>) = "p$l" + ps.joinToString(",", "(", ")")
  cfg.blocks.forEach { l, b ->
    val before = lvs(l, true)
    val after = lvs(l, false)
    val bodyClauses = b.stmts.map { it.toString() }

    val clauses = listOf(mkAtom(l, before)).plus(bodyClauses)
    val nexts = cfg.transitions[l]!!
    if (nexts.isNotEmpty()) {
      nexts.forEachIndexed { i, next ->
        val conditions = if (b.branch.next.size == 2)
          listOf(if (i == 0) "${b.branch.discrVar!!} is True" else if (i == 1) "${b.branch.discrVar!!} is not True" else fail())
        else emptyList()
        println("  ${mkAtom(next, after)} <- ${clauses.plus(conditions).joinToString(", ")}")
      }
    } else {
      println("  false <- ${clauses.joinToString(", ")}")
    }
  }
}