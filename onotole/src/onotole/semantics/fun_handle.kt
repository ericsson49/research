package onotole.semantics

import onotole.ClassVal
import onotole.Constant
import onotole.FreshNames
import onotole.TExpr
import onotole.fail
import onotole.identifier

sealed class FunHandle
data class OpHandle(val op: String): FunHandle()
data class CtorHandle(val `class`: ClassVal): FunHandle()
data class GetterHandle(val `class`: ClassVal, val attr: identifier): FunHandle()

interface RCtx<Ctx> {
  val ctx: Ctx
  fun freshName(pf: String): String
  fun emit(res: TExpr? = null, ctx: Ctx? = null): Nothing
}

/*
fun <C> applyRule(e: TExpr, ctx: C, r: RCtx<C>.(TExpr) -> Unit): Pair<TExpr, C> {
  val fn = FreshNames()
  var resE: TExpr? = null
  var resC: C? = null
  val rctx = object : RCtx<C> {
    override val ctx = ctx
    override fun freshName(pf: String) = fn.fresh(pf)
    override fun emit(res: TExpr?, ctx: C?): Nothing {
      if (res != null)
        resE = res
      if (ctx != null)
        resC = ctx
      fail()
    }
  }
  return (resE ?: e) to (resC ?: ctx)
}*/
