package onotole.exceptions

import onotole.AnnAssign
import onotole.Assert
import onotole.Assign
import onotole.Attribute
import onotole.AugAssign
import onotole.BinOp
import onotole.BoolOp
import onotole.Break
import onotole.Call
import onotole.Compare
import onotole.Comprehension
import onotole.Continue
import onotole.DictComp
import onotole.Expr
import onotole.ExprTyper
import onotole.ExprVisitor
import onotole.For
import onotole.GeneratorExp
import onotole.If
import onotole.IfExp
import onotole.Index
import onotole.Lambda
import onotole.ListComp
import onotole.Name
import onotole.Pass
import onotole.PyDict
import onotole.PyList
import onotole.PySet
import onotole.Return
import onotole.SetComp
import onotole.Slice
import onotole.Starred
import onotole.Stmt
import onotole.Subscript
import onotole.TExpr
import onotole.Tuple
import onotole.UnaryOp
import onotole.While
import onotole.fail
import onotole.flatten
import onotole.parseType

abstract class TypedExprVisitor {
  fun procStmts(c: List<Stmt>, ctx: ExprTyper) = c.fold(ctx) { ctx, s -> procStmt(s, ctx) }
  open fun procStmt(s: Stmt, ctx: ExprTyper): ExprTyper {
    return when(s) {
      is Expr -> {
        procExpr(s.value, ctx)
        ctx
      }
      is Assert -> {
        procExprs(flatten(s.test, s.msg), ctx)
        ctx
      }
      is Assign -> {
        procExprs(listOf(s.target, s.value), ctx)
        when(s.target) {
          is Name -> ctx.updated(listOf(s.target.id to ctx[s.value]))
          is Subscript -> ctx
          is Attribute -> ctx
          else -> TODO()
        }
      }
      is AnnAssign -> {
        procExprs(flatten(s.target, s.value), ctx)
        ctx.updated(listOf((s.target as Name).id to parseType(ctx, s.annotation)))
      }
      is AugAssign -> {
        procExprs(listOf(s.target, s.value), ctx)
        ctx
      }
      is If -> {
        procExpr(s.test, ctx)
        procStmts(s.body, ctx)
        procStmts(s.orelse, ctx)
      }
      is While -> {
        procExpr(s.test, ctx)
        procStmts(s.body, ctx)
      }
      is For -> {
        procExprs(listOf(s.target, s.iter), ctx)
        procStmts(s.body, ctx)
        ctx
      }
      is Return -> {
        procExprs(flatten(s.value), ctx)
        ctx
      }
      is Pass, is Break, is Continue -> ctx
      else -> TODO()
    }
  }
  abstract fun visitExpr(e: TExpr, ctx: ExprTyper)
  fun procExprs(c: List<TExpr>, ctx: ExprTyper) = c.forEach { procExpr(it, ctx) }
  open fun procComprehension(c: Comprehension, ctx: ExprTyper) {
    procExpr(c.iter, ctx)
    procExprs(c.ifs, ctx)
  }
  open fun procExpr(e: TExpr, ctx: ExprTyper) {
    visitExpr(e, ctx)
    when(e) {
      is BinOp -> procExprs(listOf(e.left, e.right), ctx)
      is Compare -> procExprs(listOf(e.left) + e.comparators, ctx)
      is BoolOp -> procExprs(e.values, ctx)
      is UnaryOp -> procExpr(e.operand, ctx)
      is Call -> procExprs(listOf(e.func) + e.args + e.keywords.map { it.value }, ctx)
      is Attribute -> procExpr(e.value, ctx)
      is Subscript -> when(e.slice) {
        is Index -> procExprs(listOf(e.value, e.slice.value), ctx)
        is Slice -> procExprs(flatten(e.value, e.slice.lower, e.slice.upper, e.slice.step), ctx)
        else -> fail()
      }
      is Tuple -> procExprs(e.elts, ctx)
      is PyList -> procExprs(e.elts, ctx)
      is PySet -> procExprs(e.elts, ctx)
      is PyDict -> procExprs(e.keys + e.values, ctx)
      is IfExp -> procExprs(listOf(e.test, e.body, e.orelse), ctx)
      is Lambda -> procExpr(e.body, ctx.forLambda(e))
      is GeneratorExp -> {
        procExpr(e.elt, ctx)
        e.generators.forEach { procComprehension(it, ctx) }
      }
      is ListComp -> {
        procExpr(e.elt, ctx)
        e.generators.forEach { procComprehension(it, ctx) }
      }
      is SetComp -> {
        procExpr(e.elt, ctx)
        e.generators.forEach { procComprehension(it, ctx) }
      }
      is DictComp -> {
        procExprs(listOf(e.key, e.value), ctx)
        e.generators.forEach { procComprehension(it, ctx) }
      }
      is Starred -> procExpr(e.value, ctx)
      else -> {}
    }
  }
}
