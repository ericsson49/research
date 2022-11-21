package onotole

import java.util.*

class ExprRenamer2(val inRenames: Map<String, TExpr>, val outRenames: Map<String, String>) {
  fun renameExprs(c: List<TExpr>): List<TExpr> = c.map(::renameExpr)
  fun renameName_Store(v: String) = outRenames[v] ?: v
  fun renameName(v: String, ctx: ExprContext): TExpr = if (ctx == ExprContext.Store)
    mkName(renameName_Store(v), true) else inRenames[v] ?: mkName(v)
  fun renameExprOpt(e: TExpr?): TExpr? = e?.let(::renameExpr)
  fun renameKwd(k: Keyword): Keyword = k.copy(value = renameExpr(k.value))
  fun renameSlice(s: TSlice) = when(s) {
    is Slice -> s.copy(lower = renameExprOpt(s.lower), upper = renameExprOpt(s.upper), step = renameExprOpt(s.step))
    is Index -> s.copy(value = renameExpr(s.value))
    else -> fail("not supported $s")
  }
  fun renameArgs(a: Arguments) = a.copy(defaults = renameExprs(a.defaults), /*kw_defaults = renameExprs(a.kw_defaults)*/)

  fun renameInFuncBody(names: List<identifier>, body: TExpr): TExpr {
    return ExprRenamer2(inRenames.minus(names), emptyMap()).renameExpr(body)
  }

  fun gatherIdentifiers(args: Arguments): List<identifier> {
    return args.args.map { it.arg }
        .plus(args.kwarg?.let { listOf(it.arg) } ?: emptyList())
        .plus(args.kwonlyargs.map { it.arg })
        .plus(args.posonlyargs.map { it.arg })
        .plus(args.vararg?.let { listOf(it.arg) } ?: emptyList())
  }

  fun renameLambda(l: Lambda): Lambda {
    return l.copy(args = renameArgs(l.args), body = renameInFuncBody(gatherIdentifiers(l.args), l.body))
  }
  fun renameLet(l: Let): Let {
    return l.copy(bindings = l.bindings.map(::renameKwd), value = renameInFuncBody(l.bindings.map { it.arg!! }, l.value))
  }
  fun renameComprehension(er: ExprRenamer2, c: Comprehension): Pair<ExprRenamer2, Comprehension> {
    val names = getVarNamesInStoreCtx(c.target)
    val newRenamer = ExprRenamer2(er.inRenames.minus(names), emptyMap())
    val newComp = c.copy(
        target = c.target,
        iter = er.renameExpr(c.iter),
        ifs = newRenamer.renameExprs(c.ifs))
    return Pair(newRenamer, newComp)
  }
  fun renameComprehensions(c: List<Comprehension>): Pair<ExprRenamer2,List<Comprehension>> {
    fun f(acc: Pair<ExprRenamer2,List<Comprehension>>, c: Comprehension): Pair<ExprRenamer2,List<Comprehension>> {
      val (renamer, comp) = renameComprehension(acc.first, c)
      return Pair(renamer, acc.second.plus(comp))
    }
    return c.fold(Pair(this, emptyList()), ::f)
  }

  fun renameExpr(e: TExpr): TExpr {
    return when (e) {
      is Constant -> e
      is Name -> renameName(e.id, e.ctx)
      is Attribute -> e.copy(value = renameExpr(e.value))
      is Subscript -> e.copy(value = renameExpr(e.value), slice = renameSlice(e.slice))
      is IfExp -> e.copy(test = renameExpr(e.test), body = renameExpr(e.body), orelse = renameExpr(e.orelse))
      is GeneratorExp -> {
        val (renamer, comps) = renameComprehensions(e.generators)
        e.copy(elt = renamer.renameExpr(e.elt), generators = comps)
      }
      is Tuple -> e.copy(elts = renameExprs(e.elts))
      is PyList -> e.copy(elts = renameExprs(e.elts))
      is PySet -> e.copy(elts = renameExprs(e.elts))
      is PyDict -> e.copy(keys = renameExprs(e.keys), values = renameExprs(e.values))
      is Starred -> e.copy(value = renameExpr(e.value))
      is Call -> e.copy(func = renameExpr(e.func), args = renameExprs(e.args), keywords = e.keywords.map(::renameKwd))
      is Lambda -> renameLambda(e)
      is Let -> renameLet(e)
      is CTV -> e
      else -> fail("Not supported $e")
    }
  }
}
class ExprRenamer(val inRenames: Map<String, String>, val outRenames: Map<String, String>) {
  fun renameExprs(c: List<TExpr>): List<TExpr> = c.map(::renameExpr)
  fun renameName(v: String, ctx: ExprContext) = (if (ctx == ExprContext.Store) outRenames else inRenames)[v] ?: v
  fun renameExprOpt(e: TExpr?): TExpr? = e?.let(::renameExpr)
  fun renameKwd(k: Keyword): Keyword = k.copy(value = renameExpr(k.value))
  fun renameSlice(s: TSlice) = when(s) {
    is Slice -> s.copy(lower = renameExprOpt(s.lower), upper = renameExprOpt(s.upper), step = renameExprOpt(s.step))
    is Index -> s.copy(value = renameExpr(s.value))
    else -> fail("not supported $s")
  }
  fun renameArgs(a: Arguments) = a.copy(defaults = renameExprs(a.defaults), /*kw_defaults = renameExprs(a.kw_defaults)*/)

  fun renameInFuncBody(names: List<identifier>, body: TExpr): TExpr {
    return ExprRenamer(inRenames.minus(names), emptyMap()).renameExpr(body)
  }

  fun gatherIdentifiers(args: Arguments): List<identifier> {
    return args.args.map { it.arg }
        .plus(args.kwarg?.let { listOf(it.arg) } ?: emptyList())
        .plus(args.kwonlyargs.map { it.arg })
        .plus(args.posonlyargs.map { it.arg })
        .plus(args.vararg?.let { listOf(it.arg) } ?: emptyList())
  }

  fun renameLambda(l: Lambda): Lambda {
    return l.copy(args = renameArgs(l.args), body = renameInFuncBody(gatherIdentifiers(l.args), l.body))
  }
  fun renameLet(l: Let): Let {
    return l.copy(bindings = l.bindings.map(::renameKwd), value = renameInFuncBody(l.bindings.map { it.arg!! }, l.value))
  }
  fun renameComprehension(er: ExprRenamer, c: Comprehension): Pair<ExprRenamer, Comprehension> {
    val names = getVarNamesInStoreCtx(c.target)
    val newRenamer = ExprRenamer(er.inRenames.minus(names), emptyMap())
    val newComp = c.copy(
        target = c.target,
        iter = er.renameExpr(c.iter),
        ifs = newRenamer.renameExprs(c.ifs))
    return Pair(newRenamer, newComp)
  }
  fun renameComprehensions(c: List<Comprehension>): Pair<ExprRenamer,List<Comprehension>> {
    fun f(acc: Pair<ExprRenamer,List<Comprehension>>, c: Comprehension): Pair<ExprRenamer,List<Comprehension>> {
      val (renamer, comp) = renameComprehension(acc.first, c)
      return Pair(renamer, acc.second.plus(comp))
    }
    return c.fold(Pair(this, emptyList()), ::f)
  }

  fun renameExpr(e: TExpr): TExpr {
    return when (e) {
      is Constant -> e
      is Name -> e.copy(id = renameName(e.id, e.ctx))
      is UnaryOp -> e.copy(operand = renameExpr(e.operand))
      is BinOp -> e.copy(left = renameExpr(e.left), right = renameExpr(e.right))
      is BoolOp -> e.copy(values = renameExprs(e.values))
      is Compare -> e.copy(left = renameExpr(e.left), comparators = renameExprs(e.comparators))
      is Attribute -> e.copy(value = renameExpr(e.value))
      is Subscript -> e.copy(value = renameExpr(e.value), slice = renameSlice(e.slice))
      is IfExp -> e.copy(test = renameExpr(e.test), body = renameExpr(e.body), orelse = renameExpr(e.orelse))
      is GeneratorExp -> {
        val (renamer, comps) = renameComprehensions(e.generators)
        e.copy(elt = renamer.renameExpr(e.elt), generators = comps)
      }
      is ListComp -> {
        val (renamer, comps) = renameComprehensions(e.generators)
        e.copy(elt = renamer.renameExpr(e.elt), generators = comps)
      }
      is Tuple -> e.copy(elts = renameExprs(e.elts))
      is PyList -> e.copy(elts = renameExprs(e.elts))
      is PySet -> e.copy(elts = renameExprs(e.elts))
      is PyDict -> e.copy(keys = renameExprs(e.keys), values = renameExprs(e.values))
      is Starred -> e.copy(value = renameExpr(e.value))
      is Call -> e.copy(func = renameExpr(e.func), args = renameExprs(e.args), keywords = e.keywords.map(::renameKwd))
      is Lambda -> renameLambda(e)
      is Let -> renameLet(e)
      is CTV -> if (e.v is ConstExpr) CTV(ConstExpr(renameExpr(e.v.e))) else e
      else -> fail("Not supported $e")
    }
  }
}

class StmtRenamer(val inRenames: StmtAnnoMap<Map<String,String>>, val outRenames: StmtAnnoMap<Map<String, String>>) {
  val stmtRemap = IdentityHashMap<Stmt,Stmt>()
  val blockRemap = IdentityHashMap<List<Stmt>,List<Stmt>>()

  fun renameStmts(c: List<Stmt>): List<Stmt> {
    val res = c.map(::renameStmt)
    blockRemap[c] = res
    return res
  }

  fun renameStmt(s: Stmt): Stmt {
    val exprRenamer = ExprRenamer(inRenames[s]!!, outRenames[s]!!)
    val renameExpr = exprRenamer::renameExpr
    val renameExprOpt = exprRenamer::renameExprOpt
    val renameExprs = exprRenamer::renameExprs
    val res = when (s) {
      is Assign -> s.copy(target = renameExpr(s.target), value = renameExpr(s.value))
      is AnnAssign -> s.copy(target = renameExpr(s.target), value = renameExprOpt(s.value))
      is AugAssign -> {
        val vars = getVarNamesInStoreCtx(s.target)
        if (vars.any { (exprRenamer.inRenames[it] ?: it) != (exprRenamer.outRenames[it] ?: it) }) {
          // convert to simple assignment
          fun convertToLoadCtx(e: TExpr): TExpr = when(e) {
            is Name -> e.copy(id = e.id, ctx = ExprContext.Load)
            is Tuple -> e.copy(elts = e.elts.map(::convertToLoadCtx), ctx = ExprContext.Load)
            is Subscript -> e
            is Attribute -> e
            else -> fail("unsupported $e")
          }
          Assign(
              target = renameExpr(s.target),
              value = renameExpr(BinOp(
                  left = convertToLoadCtx(s.target),
                  op = s.op,
                  right = s.value
              ))
          )
        } else {
          s.copy(target = renameExpr(s.target), value = renameExpr(s.value))
        }
      }
      is If -> s.copy(test = renameExpr(s.test), body = renameStmts(s.body), orelse = renameStmts(s.orelse))
      is While -> s.copy(test = renameExpr(s.test), body = renameStmts(s.body))
      is For -> {
        val targetRenamer = if (s.body.isNotEmpty()) {
          ExprRenamer(emptyMap(), inRenames[s.body[0]]!!)
        } else {
          exprRenamer
        }
        s.copy(target = targetRenamer.renameExpr(s.target), iter = renameExpr(s.iter), body = renameStmts(s.body))
      }
      is Return -> s.copy(value = renameExprOpt(s.value))
      is Assert -> s.copy(test = renameExpr(s.test), msg = renameExprOpt(s.msg))
      is Expr -> s.copy(value = renameExpr(s.value))
      is Pass -> s
      is Continue -> s
      is Break -> s
      else -> fail("Not supported $s")
    }
    stmtRemap[s] = res
    return res
  }
}
