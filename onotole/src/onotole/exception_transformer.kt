package onotole

class ExceptionTransformer {
  val tmpVars = FreshNames()
  fun throwsException(e: TExpr): Boolean = TODO()
  fun transform(e: TExpr) = when(e) {
    is Call -> {
      if (e.args.plus(e.keywords.map { it.value }).any()) {
        fun transf(ls: List<TExpr>, pos: Int, acc: List<Pair<String, TExpr>>): TExpr {
          val tmpVar = tmpVars.fresh("tmp_exc")
          val newAcc = acc.plus(tmpVar to ls[pos])
          val expr = if (throwsException(ls[pos])) {
            val let = Let(newAcc.map { Keyword(it.first, it.second) },
                IfExp(Compare(mkName(tmpVar), listOf(ECmpOp.Is), listOf(mkName("<Fail>"))),
                    mkCall("Fail", listOf()),
                    transf(ls, pos+1, emptyList()))
            )
            let
          } else {
            transf(ls, pos+1, newAcc)
          }
          return expr
        }
        transf(e.args.plus(e.keywords.map { it.value }), 0, emptyList())
      } else e
    }
    else -> TODO()
  }
}