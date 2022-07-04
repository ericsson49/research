package onotole.rewrite

import onotole.ExprTransformer
import onotole.FreshNames
import onotole.SimpleStmtTransformer
import onotole.Stmt
import onotole.TExpr

typealias StmtTransformRule1 = (Stmt) -> List<Stmt>?
typealias StmtTransformRule = (FreshNames, Stmt) -> List<Stmt>?
fun interface ExprTransformRule<Ctx> {
  fun invoke(e: TExpr, ctx: Ctx): TExpr?
}

fun <Ctx> mkExprTransformRule(f: (TExpr, Ctx) -> TExpr?): ExprTransformRule<Ctx> = ExprTransformRule(f)

fun <Ctx> combineRules(vararg rs: ExprTransformRule<Ctx>): ExprTransformRule<Ctx> = mkExprTransformRule { e, ctx ->
  val res = rs.fold(e) { curr, rule -> rule.invoke(curr, ctx) ?: curr }
  if (res == e) null else res
}

class RuleSetTransformer(val rules: Collection<StmtTransformRule>): SimpleStmtTransformer() {
  constructor(rule: StmtTransformRule): this(listOf(rule))
  val tmpVars = FreshNames()
  override fun doTransform(s: Stmt): List<Stmt>? {
    rules.forEach { t ->
      val res = t.invoke(tmpVars, s)
      if (res != null) {
        return transform(res)
      }
    }
    return null
  }
}

class RuleSetTransformer1(val rules: Collection<StmtTransformRule1>): SimpleStmtTransformer() {
  constructor(rule: StmtTransformRule1): this(listOf(rule))
  override fun doTransform(s: Stmt): List<Stmt>? {
    rules.forEach { t ->
      val res = t.invoke(s)
      if (res != null) {
        return transform(res)
      }
    }
    return null
  }
}

abstract class RuleSetExprTransformer<Ctx>(val rules: Collection<ExprTransformRule<Ctx>>): ExprTransformer<Ctx>() {
  override fun transform(e: TExpr, ctx: Ctx, store: Boolean): TExpr {
    rules.forEach { rule ->
      val res = rule.invoke(e, ctx)
      if (res != null && res != e)
        return transform(res, ctx)
    }
    return e
  }
}
