package onotole

class DeExceptionizer(val fname: String, val typer: ExprTyper, val excFuncs: Map<String, Boolean>): SimpleStmtTransformer() {
  val tmpVars = FreshNames()
  fun throwsException(n: String): Boolean = excFuncs[n] ?: true

  fun wrapWithExcHandler(e: TExpr): Pair<List<Stmt>, TExpr> {
    val tmpVar = tmpVars.fresh("tmp_ex")
    return listOf(
        Assign(mkName(tmpVar, true), e),
        If(Compare(mkName(tmpVar), listOf(ECmpOp.Is), listOf(mkName("<Fail>"))),
            listOf(Return(mkCall("<Fail>", listOf())))),
    ) to Attribute(mkName(tmpVar), "result", ExprContext.Load)
  }
  fun transformRVal(e: TExpr): Pair<List<Stmt>, TExpr>? = when(e) {
    is Call -> {
      if (e.func is Name && throwsException(e.func.id)) {
        wrapWithExcHandler(e)
      } else null
    }
    is Subscript -> if (canThrowException(typer, e)) wrapWithExcHandler(e) else null
    else -> null
  }

  override fun doTransform(s: Stmt): List<Stmt>? = when(s) {
    is Assign -> {
      transformRVal(s.value)?.let { it.first.plus(s.copy(value = it.second)) }
    }
    is AnnAssign -> {
      s.value?.let { transformRVal(it) }?.let { it.first.plus(s.copy(value = it.second)) }
    }
    is AugAssign -> TODO()
    is Assert -> {
      val res = transformRVal(mkCall("<assert>", listOf(s.test).plus(s.msg?.let { listOf(it) } ?: emptyList())))
      res?.first
    }
    is Return -> if (excFuncs[fname] == true) {
      val retVal = s.value ?: NameConstant(null)
      listOf(s.copy(mkCall("<Result>", listOf(retVal))))
    } else null
    else -> null
  }

  override fun transform(f: FunctionDef): FunctionDef {
    val res = super.transform(f)
    return if (excFuncs[f.name] == true) {
      res.copy(name = f.name + "_exc", returns = res.returns?.let { Subscript(mkName("<Outcome>"), Index(it), ExprContext.Load) })
    } else res
  }
}