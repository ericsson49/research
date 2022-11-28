package onotole.util

import onotole.AnnAssign
import onotole.Arg
import onotole.Arguments
import onotole.Assert
import onotole.Assign
import onotole.Attribute
import onotole.AugAssign
import onotole.BinOp
import onotole.BoolOp
import onotole.CTV
import onotole.Call
import onotole.Compare
import onotole.Constant
import onotole.Expr
import onotole.ExprContext
import onotole.ExprTyper
import onotole.GeneratorExp
import onotole.If
import onotole.IfExp
import onotole.Index
import onotole.Keyword
import onotole.Lambda
import onotole.Let
import onotole.ListComp
import onotole.Name
import onotole.PyDict
import onotole.PyList
import onotole.Return
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
import onotole.mkCall


fun interface TExprAssembler<T: TExpr> {
  fun assemble(args: List<TExpr>): T
}

inline fun <reified T: TExpr> deconstruct(e: T): Pair<List<TExpr>, TExprAssembler<T>> = when(e) {
  is Name, is Constant, is CTV, is Lambda, is Let, is GeneratorExp -> {
    emptyList<TExpr>() to TExprAssembler { e }
  }
  is Attribute -> listOf(e.value) to TExprAssembler { args -> e.copy(value = args[0]) as T }
  is Subscript -> when(e.slice) {
    is Index -> listOf(e.value, e.slice.value) to TExprAssembler { e.copy(value = it[0], slice = e.slice.copy(value = it[1])) as T }
    is Slice -> flatten(e.value, e.slice.lower, e.slice.upper, e.slice.step) to TExprAssembler {
      var idx = 1
      val lower = if (e.slice.lower != null) it[idx++] else null
      val upper = if (e.slice.upper != null) it[idx++] else null
      val step = if (e.slice.step != null) it[idx++] else null
      e.copy(value = it[0], slice = e.slice.copy(lower, upper, step)) as T
    }
    else -> fail()
  }
  is Call -> listOf(e.func).plus(e.args).plus(e.keywords.map { it.value }) to TExprAssembler {
    val func = it[0]
    val args = it.subList(1, e.args.size+1)
    val kwds = e.keywords.map { it.arg }.zip(it.subList(1+e.args.size, it.size)).map { Keyword(it.first, it.second) }
    e.copy(func = func, args = args, keywords = kwds) as T
  }
  is PyList -> e.elts to TExprAssembler { e.copy(elts = it) as T }
  is PyDict -> e.keys.plus(e.values) to TExprAssembler {
    e.copy(keys = it.subList(0, e.keys.size), values = it.subList(e.keys.size, it.size)) as T
  }
  is Tuple -> e.elts to TExprAssembler { e.copy(elts = it) as T }
  is IfExp -> listOf(e.test, e.body, e.orelse) to TExprAssembler {
    e.copy(test = it[0], body = it[1], orelse = it[2]) as T
  }
  is Starred -> listOf(e.value) to TExprAssembler { e.copy(value = it[0]) as T }
  else -> TODO()
}


inline fun <reified T: TExpr> deconstructS(e: T): Pair<List<TExpr>, TExprAssembler<T>> = when(e) {
  is CTV, is Constant, is Name, is Lambda -> {
    emptyList<T>() to TExprAssembler { e }
  }
  is Attribute -> listOf(e.value) to TExprAssembler { args -> e.copy(value = args[0]) as T }
  is Subscript -> when(e.slice) {
    is Index -> listOf(e.value, e.slice.value) to TExprAssembler { e.copy(value = it[0], slice = e.slice.copy(value = it[1])) as T }
    is Slice -> flatten(e.value, e.slice.lower, e.slice.upper, e.slice.step) to TExprAssembler {
      var idx = 1
      val lower = if (e.slice.lower != null) it[idx++] else null
      val upper = if (e.slice.upper != null) it[idx++] else null
      val step = if (e.slice.step != null) it[idx++] else null
      e.copy(value = it[0], slice = e.slice.copy(lower, upper, step)) as T
    }
    else -> fail()
  }
  is BinOp -> listOf(e.left, e.right) to TExprAssembler { e.copy(left = it[0], right = it[1]) as T }
  is UnaryOp -> listOf(e.operand) to TExprAssembler { e.copy(operand = it[0]) as T }
  is BoolOp -> e.values to TExprAssembler { e.copy(values = it) as T }
  is Compare -> listOf(e.left).plus(e.comparators) to TExprAssembler { e.copy(left = it[0], comparators = it.subList(1, it.size)) as T }
  is Call -> listOf(e.func).plus(e.args).plus(e.keywords.map { it.value }) to TExprAssembler {
    val func = it[0]
    val args = it.subList(1, e.args.size+1)
    val kwds = e.keywords.map { it.arg }.zip(it.subList(1+e.args.size, it.size)).map { Keyword(it.first, it.second) }
    e.copy(func = func, args = args, keywords = kwds) as T
  }
  is PyList -> e.elts to TExprAssembler { e.copy(elts = it) as T }
  is PyDict -> e.keys.plus(e.values) to TExprAssembler {
    e.copy(keys = it.subList(0, e.keys.size), values = it.subList(e.keys.size, it.size)) as T
  }
  is Tuple -> e.elts to TExprAssembler { e.copy(elts = it) as T }
  is IfExp -> listOf(e.test) to TExprAssembler {
    e.copy(test = it[0]) as T
  }
  is Starred -> listOf(e.value) to TExprAssembler { e.copy(value = it[0]) as T }
  is ListComp -> listOf(GeneratorExp(elt = e.elt, generators = e.generators)) to TExprAssembler {
    val ge = it[0] as GeneratorExp
    e.copy(elt = ge.elt, generators = ge.generators) as T
  }
  else -> TODO()
}

fun getExprs(s: Stmt, ctx: ExprContext? = null): List<TExpr> {
  val res = when (s) {
    is Expr -> listOf(s.value)
    is Assert -> flatten(s.msg, s.test)
    is Assign -> listOf(s.target, s.value)
    is AnnAssign -> flatten(s.target, s.value)
    is AugAssign -> TODO()
    is If -> listOf(s.test) + getExprs(s.body) + getExprs(s.orelse)
    is While -> listOf(s.test) + getExprs(s.body)
    is Return -> flatten(s.value)
    else -> TODO()
  }
  fun getCtx(e: TExpr) =
      when(e) {
        is Name -> e.ctx
        is Attribute -> e.ctx
        is Subscript -> e.ctx
        is Tuple -> e.ctx
        else -> ExprContext.Load
      }
  return if (ctx == null)
    res
  else
    res.filter { getCtx(it) == ctx }
}

fun getExprs(c: Collection<Stmt>): List<TExpr> = c.flatMap { getExprs(it, ExprContext.Load) }

fun mergeExprTypers(a: ExprTyper, b: ExprTyper): ExprTyper {
  val aKeys = a.ctx.keys
  val bKeys = b.ctx.keys
  val merged = aKeys.union(bKeys)
  val common = aKeys.intersect(bKeys)
  //if (common.find { a.ctx[it] != b.ctx[it]} != null)
  //  TODO()
  return a.updated(merged.minus(aKeys).map { it to b.ctx[it]!! })
}
