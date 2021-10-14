package onotole

// expressions should be pure in:
// - if test expressions
// - while test expression
// - for iter expression
// - in an assignment to a field
// - in an assignment to an index/slice
// - lambda expression
// - comprehensions
// - object and container constructors
// - non-toplevel expressions in statements and assigns to a variable
// allowed forms:
// - Expr(Call(impureFunc(<pureArgs>)))
// - Expr(Call(Attribute(<pure>, <impure attr>, <pure args>)))
// - Assign(Name(), Call(impureFunc(<pureArgs>)))
// - Assign(Name(), Call(Attribute(<pure>, <impure attr>, <pure args>)))

class PurityConstraintChecker(val impureFuncs: Set<String>): ExprVisitor<Unit>() {
  fun checkPure(e: TExpr, ctx: Unit): Unit = procExpr(e, ctx)
  fun checkPure(es: List<TExpr>, ctx: Unit): Unit = es.forEach { checkPure(it, ctx) }
  fun checkTopLevel(e: TExpr, ctx: Unit): Unit = when(e) {
    is Call -> checkPure(e.args + e.keywords.map { it.value }, ctx)
    else -> checkPure(e, ctx)
  }

  override fun visitStmt(s: Stmt, ctx: Unit) = when(s) {
    is Expr -> checkTopLevel(s.value, ctx)
    is Assign -> when(s.target) {
      is Name -> checkTopLevel(s.value, ctx)
      else -> super.visitStmt(s, ctx)
    }
    else -> super.visitStmt(s, ctx)
  }

  override fun visitExpr(e: TExpr, ctx: Unit) {
    if (e is Call) {
      if (e.func is Name) {
        if (e.func.id in impureFuncs) fail("$e")
      } else if (e.func is Attribute) {
        if ("attr_" + e.func.attr in impureFuncs) fail("$e")
      }
    }
  }

}

fun checkPurityConstraints(f: FunctionDef, impureFuncs: Set<String>) {
  PurityConstraintChecker(impureFuncs).procStmts(f.body, Unit)
}