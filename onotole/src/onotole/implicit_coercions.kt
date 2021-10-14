package onotole


class ExpectedTypeCalc(val retType: RTType): ExprVisitor<MutableMap<TExpr,RTType>>() {
  override fun visitStmt(s: Stmt, ctx: MutableMap<TExpr, RTType>) {
    when(s) {
      is Assert -> {
        ctx[s.test] = TPyObject
        if (s.msg != null)
          ctx[s.msg] = TPyStr
      }
      is Return -> {
        if (s.value != null)
          ctx[s.value] = retType
        else if (retType != TPyNone) fail()
      }
      is AnnAssign -> {
        if (s.value != null)
          ctx[s.value] = parseType(s.annotation)
      }
      is If -> {
        ctx[s.test] = TPyBool
      }
      is While -> {
        ctx[s.test] = TPyBool
      }
      else -> {}
    }
    super.visitStmt(s, ctx)
  }
  override fun visitExpr(e: TExpr, ctx: MutableMap<TExpr, RTType>) {
    when(e) {
      is Call -> {

      }
      else -> {}
    }
  }

}