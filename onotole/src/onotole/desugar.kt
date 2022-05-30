package onotole

import onotole.rewrite.RuleSetTransformer
import java.nio.file.Files
import java.nio.file.Paths

fun isLeaf(e: TExpr) = e is NameConstant || e is Num || e is Bytes || e is Str || e is Name

fun splitComp(c: Comprehension, freshNames: FreshNames): Pair<List<Assign>, Comprehension> {
  val (its, itv) = splitExpression(c.iter, freshNames)
  return its to c.copy(iter = itv)
}

fun splitExpression(e: TExpr, freshNames: FreshNames): Pair<List<Assign>,TExpr> {
  return when {
    isLeaf(e) -> emptyList<Assign>() to e
    e is BinOp -> {
      val (sl, l) = splitExpression(e.left, freshNames)
      val (sr, r) = splitExpression(e.right, freshNames)
      sl.plus(sr) to e.copy(left = l, right = r)
    }
    e is Compare -> {
      val (s, l) = splitExpression(e.left, freshNames)
      val (ss, cc) = e.comparators.map { splitExpression(it, freshNames) }.unzip()
      s.plus(ss.flatten()) to e.copy(left = l, comparators = cc)
    }
    e is BoolOp -> {
      val (vs, v) = e.values.map { splitExpression(it, freshNames) }.unzip()
      vs.flatten() to e.copy(values = v)
    }
    e is UnaryOp -> {
      val (s, r) = splitExpression(e.operand, freshNames)
      s to e.copy(operand = r)
    }
    e is Attribute -> {
      val (s, r) = splitExpression(e.value, freshNames)
      s to e.copy(value = r)
    }
    e is Subscript -> {
      val (vs, v) = splitExpression(e.value, freshNames)
      when(e.slice) {
        is Index -> {
          val (iss, iv) = splitExpression(e.slice.value, freshNames)
          vs.plus(iss) to e.copy(value = v, slice = e.slice.copy(value = iv))
        }
        is Slice -> {
          fun split(e: TExpr?): Pair<List<Assign>,TExpr?> = if (e == null)
            emptyList<Assign>() to e
          else
            splitExpression(e, freshNames)
          val (ls, lv) = split(e.slice.lower)
          val (us, uv) = split(e.slice.upper)
          val (ss, sv) = split(e.slice.step)
          ls.plus(us).plus(ss) to e.copy(value = v, slice = e.slice.copy(lower = lv, upper = uv, step = sv))
        }
        else -> TODO()
      }
    }
    e is Call -> {
      val (fs, f) = splitExpression(e.func, freshNames)
      val (`as`, aa) = e.args.map { splitExpression(it, freshNames) }.unzip()
      val (ks, kk) = e.keywords.map { splitExpression(it.value, freshNames) }.unzip()
      val kwds = e.keywords.zip(kk).map { it.first.copy(value = it.second) }
      fs.plus(`as`.flatten()).plus(ks.flatten()) to e.copy(func = f, args = aa, keywords = kwds)
    }
    e is IfExp -> {
      val (ts, te) = splitExpression(e.test, freshNames)
      val (bs, be) = splitExpression(e.body, freshNames)
      val (es, ee) = splitExpression(e.orelse, freshNames)
      ts.plus(bs).plus(es) to e.copy(test = te, body = be, orelse = ee)
    }
    e is Tuple -> {
      val (es, ev) = e.elts.map { splitExpression(it, freshNames) }.unzip()
      es.flatten() to e.copy(elts = ev)
    }
    e is PyList -> {
      val (es, ev) = e.elts.map { splitExpression(it, freshNames) }.unzip()
      es.flatten() to e.copy(elts = ev)
    }
    e is ListComp -> {
      if (e.generators.size != 1) TODO()
      val (cs, cv) = splitComp(e.generators[0], freshNames)
      cs to e.copy(generators = listOf(cv))
    }
    e is Lambda -> emptyList<Assign>() to e
    e is Starred -> {
      val (ss, sv) = splitExpression(e.value, freshNames)
      ss to e.copy(value = sv)
    }
    else -> TODO("$e")
  }
}

fun convertComprehension(mapBody: TExpr, c: Comprehension, last: Boolean): TExpr {
  fun targetToArgs(t: TExpr): List<Arg> = when(t) {
    is Name -> listOf(Arg(t.id))
    is Tuple -> t.elts.flatMap { if (it is Name) targetToArgs(it) else fail() }
    else -> fail()
  }
  val args = Arguments(args = targetToArgs(c.target))
  val filtered = c.ifs.fold(c.iter) { iter, filterBody ->
    mkCall("filter", listOf(Lambda(args, filterBody), iter))
  }
  return mkCall(if (last) "map" else "flat_map", listOf(Lambda(args, mapBody), filtered))
}
fun convertComprehensions(elt: TExpr, cs: List<Comprehension>): TExpr {
  return cs.foldRight(elt to true) { c, p ->
    val (body, last) = p
    convertComprehension(body, c, last) to false
  }.first
}

class FreshNames {
  val tmpVars = mutableSetOf<String>()
  fun fresh(prefix: String = "tmp"): String {
    val n = prefix + "_" + tmpVars.size
    tmpVars.add(n)
    return n
  }
}


abstract class ExprTransformer<Ctx> {
  fun defaultTransformComprehension(c: Comprehension, ctx: Ctx): Comprehension =
          c.copy(target = transform(c.target, ctx, true), iter = transform(c.iter, ctx), ifs = transform(c.ifs, ctx))
  open fun defaultTransform(e: TExpr, ctx: Ctx, store: Boolean = false): TExpr = when(e) {
    is Constant -> e
    is Name -> e
    is BinOp -> e.copy(left = transform(e.left, ctx), right = transform(e.right, ctx))
    is BoolOp -> e.copy(values = transform(e.values, ctx))
    is Compare -> e.copy(left = transform(e.left, ctx), comparators = transform(e.comparators, ctx))
    is UnaryOp -> e.copy(operand = transform(e.operand, ctx))
    is Attribute -> e.copy(value = transform(e.value, ctx))
    is Subscript -> {
      val newValue = transform(e.value, ctx)
      val newSlice = when(e.slice) {
        is Index -> e.slice.copy(value = transform(e.slice.value, ctx))
        is Slice -> e.slice.copy(
                lower = e.slice.lower?.let { transform(it, ctx) },
                upper = e.slice.upper?.let { transform(it, ctx) },
                step = e.slice.step?.let { transform(it, ctx) },
        )
        else -> fail()
      }
      e.copy(value = newValue, slice = newSlice)
    }
    is IfExp -> e.copy(test = transform(e.test, ctx), body = transform(e.body, ctx), orelse = transform(e.orelse, ctx))
    is Tuple -> e.copy(elts = transform(e.elts, ctx))
    is PyList -> e.copy(elts = transform(e.elts, ctx))
    is PySet -> e.copy(elts = transform(e.elts, ctx))
    is PyDict -> e.copy(keys = transform(e.keys, ctx), values = transform(e.values, ctx))
    is Call -> e.copy(func = transform(e.func, ctx), args = transform(e.args, ctx),
            keywords = e.keywords.map { it.copy(value = transform(it.value, ctx)) })
    is Lambda -> e.copy(body = transform(e.body, ctx))
    is GeneratorExp -> e.copy(elt = transform(e.elt, ctx), generators = e.generators.map { defaultTransformComprehension(it, ctx) })
    is ListComp -> e.copy(elt = transform(e.elt, ctx), generators = e.generators.map { defaultTransformComprehension(it, ctx) })
    is SetComp -> e.copy(elt = transform(e.elt, ctx), generators = e.generators.map { defaultTransformComprehension(it, ctx) })
    is DictComp -> e.copy(key = transform(e.key, ctx), value = transform(e.value, ctx),
            generators = e.generators.map { defaultTransformComprehension(it, ctx) })
    is Starred -> e.copy(value = transform(e.value, ctx))
    is CTV -> when (e.v) {
      is ConstExpr -> CTV(ConstExpr(transform(e.v.e, ctx)))
      is ClassVal -> e
      is ClassTemplate -> e
      is FuncInst -> e
      is FuncTempl -> e
      else -> TODO()
    }
    is Let -> {
      val (newAssgnmts, newCtx) = procStmts(e.bindings.map { Assign(mkName(it.arg!!, true), it.value) }, ctx)
      val newBindings = newAssgnmts.map {
        when(it) {
          is Assign -> Keyword((it.target as Name).id, it.value)
          is AnnAssign -> Keyword((it.target as Name).id, it.value!!)
          else -> TODO()
        }
      }
      e.copy(bindings = newBindings, value = transform(e.value, newCtx))
    }
    else -> TODO("$e")
  }
  abstract fun transform(e: TExpr, ctx: Ctx, store: Boolean = false): TExpr
  fun transform(c: List<TExpr>, ctx: Ctx): List<TExpr> = c.map { transform(it, ctx) }
  fun procStmts(c: List<Stmt>, ctx: Ctx): Pair<List<Stmt>, Ctx> {
    var currCtx = ctx
    val res = mutableListOf<Stmt>()
    c.forEach { s ->
      val (ns, nc) = procStmt(s, currCtx)
      currCtx = nc
      res.add(ns)
    }
    return res to currCtx
  }
  fun convertLValToLoad(e: TExpr): TExpr = when(e) {
    is Name -> e.copy(ctx = ExprContext.Load)
    is Subscript -> e.copy(ctx = ExprContext.Load)
    is Attribute -> e.copy(ctx = ExprContext.Load)
    is Tuple -> e.copy(ctx = ExprContext.Load, elts = e.elts.map(::convertLValToLoad))
    else -> fail()
  }
  abstract fun merge(a: Ctx, b: Ctx): Ctx
  open fun procStmt(s: Stmt, ctx: Ctx): Pair<Stmt, Ctx> {
    return when(s) {
      is Expr -> s.copy(value = transform(s.value, ctx)) to ctx
      is Assert -> s.copy(test = transform(s.test, ctx), msg = s.msg?.let { transform(it, ctx) }) to ctx
      is Assign -> s.copy(target = transform(s.target, ctx, true), value = transform(s.value, ctx)) to ctx
      is AnnAssign -> s.copy(target = transform(s.target, ctx, true),
              annotation = transform(s.annotation, ctx),
              value = s.value?.let { transform(it, ctx) }) to ctx
      is AugAssign -> procStmt(Assign(target = s.target, value = BinOp(convertLValToLoad(s.target), s.op, s.value)), ctx)
      is If -> {
        val test = transform(s.test, ctx)
        val (body, bodyCtx) = procStmts(s.body, ctx)
        val (orelse, orelseCtx) = procStmts(s.orelse, ctx)
        s.copy(test = test, body = body, orelse = orelse) to merge(bodyCtx, orelseCtx)
      }
      is While -> {
        val test = transform(s.test, ctx)
        val (body, bodyCtx) = procStmts(s.body, ctx)
        s.copy(test = test, body = body) to merge(ctx, bodyCtx)
      }
      is For -> {
        val target = transform(s.target, ctx, true)
        val iter = transform(s.iter, ctx)
        val (body, bodyCtx) = procStmts(s.body, ctx)
        s.copy(target = target, iter = iter, body = body) to merge(ctx, bodyCtx)
      }
      is Return -> (s.value?.let { s.copy(value = transform(it, ctx)) } ?: s) to ctx
      is Pass -> s to ctx
      is Continue -> s to ctx
      is Break -> s to ctx
      else -> TODO("$s")
    }
  }
}

class ExprToCalls(): ExprTransformer<Unit>() {
  val tmpVars = FreshNames()
  fun mkCall(name: String, args: List<TExpr>) = Call(Name(name, ExprContext.Load), args, emptyList())
  fun transform2(body: TExpr, cs: List<Comprehension>): TExpr {
    if (cs.size != 1) fail()
    val c = cs[0]
    fun processTarget(t: TExpr): List<Arg> = when(t) {
      is Name -> listOf(Arg(t.id))
      is Tuple -> t.elts.flatMap(::processTarget)
      else -> fail()
    }
    val lamArgs = Arguments(args = processTarget(c.target))
    val coll = c.ifs.fold(c.iter) { c, ifE ->
      val filterL = Lambda(args = lamArgs, body = ifE)
      mkCall("<filter>", listOf(filterL, c))
    }
    val mapL = Lambda(args = lamArgs, body = body)
    return transform(mkCall("<map>", listOf(mapL, coll)), Unit)
  }
  override fun transform(e: TExpr, ctx: Unit, store: Boolean): TExpr {
    return when (e) {
      is BinOp -> mkCall("<${e.op}>", transform(listOf(e.left, e.right), ctx))
      is BoolOp -> {
        val redOp: (TExpr, TExpr) -> TExpr = when (e.op) {
          EBoolOp.And -> { a, b -> IfExp(a, b, NameConstant(false)) }
          EBoolOp.Or -> { a, b -> IfExp(a, NameConstant(true), b) }
        }
        //mkCall("<${e.op}>", transform(e.values))
        transform(e.values, ctx).reduceRight(redOp)
      }
      is Compare -> {
        fun convert(l: TExpr, rest: List<Pair<ECmpOp, TExpr>>): TExpr {
          if (rest.isEmpty()) {
            TODO()
          } else {
            val (op, r) = rest[0]
            val needTmp1 = l !is Constant && l !is Name
            val needTmp2 = rest.size > 1 && r !is Constant && r !is Name

            val (bs1, v1) = if (needTmp1 && needTmp2) {
              val tmp = tmpVars.fresh("tmp_c")
              listOf(tmp to transform(l, ctx)) to mkName(tmp)
            } else emptyList<Pair<String, TExpr>>() to l
            val (bs2, v2) = if (needTmp2) {
              val tmp = tmpVars.fresh("tmp_c")
              listOf(tmp to transform(r, ctx)) to mkName(tmp)
            } else emptyList<Pair<String, TExpr>>() to r
            val cmp = transform(mkCall("<$op>", listOf(v1, v2)), ctx)
            val res = if (rest.size > 1) {
              val next = convert(v2, rest.subList(1, rest.size))
              IfExp(cmp, next, NameConstant(false))
            } else cmp
            val bs = bs1.plus(bs2)
            return if (bs.isNotEmpty())
              Let(bs.map { Keyword(it.first, it.second) }, res)
            else res
          }
        }
        convert(e.left, e.ops.zip(e.comparators))


        /*val exprs = listOf(e.left).plus(e.comparators.subList(0, e.comparators.size-1))
                .zip(e.ops).zip(e.comparators).map { (l_o, r) ->
          val (l, o) = l_o
          mkCall("<$o>", listOf(l, r))
        }
        if (exprs.size == 1) transform(exprs[0])
        else transform(BoolOp(op = EBoolOp.And, values = exprs))*/
      }
      is UnaryOp -> mkCall("<${e.op}>", listOf(transform(e.operand, ctx)))
      //is PyList -> mkCall("<list>", transform(e.elts))
      //is PySet -> mkCall("<set>", transform(e.elts))
      //is PyDict -> mkCall("<dict>", e.keys.zip(e.values).map {
      //  Tuple(elts = listOf(it.first, it.second), ctx = if (store) ExprContext.Store else ExprContext.Load) })
      is GeneratorExp -> e.copy(elt = transform(e.elt, ctx), generators = e.generators.map {
        it.copy(target = transform(it.target, ctx, true), iter = transform(it.iter, ctx), ifs = transform(it.ifs, ctx))
      })
      is ListComp -> mkCall("list", listOf(transform(GeneratorExp(e.elt, e.generators), ctx)))
      is SetComp -> mkCall("set", listOf(transform(GeneratorExp(e.elt, e.generators), ctx)))
      is DictComp -> mkCall("dict", listOf(transform(GeneratorExp(
          Tuple(elts = listOf(e.key, e.value), ctx = ExprContext.Load), e.generators
      ), ctx)))
      else -> defaultTransform(e, ctx, store)
    }
  }

  override fun merge(a: Unit, b: Unit) { }
}

class LambdaToFuncs(val outerName: String, val localVars: Set<String>): ExprTransformer<Unit>() {
  val freshNames = FreshNames()
  val localFuncs = mutableListOf<FunctionDef>()
  override fun transform(e: TExpr, ctx: Unit, store: Boolean): TExpr = when(e) {
    is Lambda -> {
      val name = freshNames.fresh("lambda_" + outerName)
      val liveVars = liveVarAnalysis(e).intersect(localVars).toList()
      val funcArgs = e.args.copy(args = liveVars.map { Arg(it) }.plus(e.args.args))
      localFuncs.add(FunctionDef(name = name, args = funcArgs, body = listOf(Return(transform(e.body, ctx)))))
      if (liveVars.isEmpty()) {
        Name(name, ExprContext.Load)
      } else {
        e.copy(body = Call(Name(name, ExprContext.Load),
                liveVars.plus(e.args.args.map { it.arg }).map { Name(id = it, ExprContext.Load) },
                emptyList()
        ))
      }
    }
    else -> defaultTransform(e, ctx, store)
  }

  override fun merge(a: Unit, b: Unit) {}
}

fun mkCtx(store: Boolean) = if (store) ExprContext.Store else ExprContext.Load
fun mkName(v: String, store: Boolean = false) = Name(v, mkCtx(store))
fun mkTuple(vararg elts: TExpr) = Tuple(elts.toList(), ExprContext.Load)
fun mkIndex(i: Int) = Index(Num(i))
fun mkIndex(vararg indices: TExpr) = Index(if (indices.size == 1) indices[0] else Tuple(indices.toList(), ExprContext.Load))
fun mkAttribute(v: String, a: identifier, store: Boolean = false) = mkAttribute(mkName(v), a, store)
fun mkAttribute(e: TExpr, a: identifier, store: Boolean = false) = Attribute(e, a, mkCtx(store))
fun mkSubscript(v: String, s: TSlice, store: Boolean = false) = mkSubscript(mkName(v), s, store)
fun mkSubscript(e: TExpr, s: TSlice, store: Boolean = false) = Subscript(e, s, mkCtx(store))
fun mkAssign(t: TExpr, e: TExpr) = Assign(t, e)

fun mkCall(f: String, args: List<TExpr>) = onotole.mkCall(mkName(f), args)
fun mkCall(e: TExpr, args: List<TExpr>) = Call(e, args = args, keywords = emptyList())

class Desugar {
  val destrLVal = true
  val destrWhile = true
  val destrFor = true
  val destrExpr = true

  val freshNames = FreshNames()
  fun fresh(): String = freshNames.fresh()


  fun destructLVal(a: Assign): List<Assign> {
    return when (val t = a.target) {
      is Name -> return listOf(a)
      is Attribute -> {
        if (t.value is Name)
          listOf(a)
        else {
          val v = fresh()
          listOf(mkAssign(mkName(v, true), t.value),
                  mkAssign(mkAttribute(v, t.attr, true), a.value))
        }
      }
      is Subscript -> {
        if (t.value is Name)
          listOf(a)
        else {
          val v = fresh()
          listOf(mkAssign(mkName(v, true), t.value),
                  mkAssign(mkSubscript(v, t.slice, true), a.value)
          )
        }
      }
      is Tuple -> {
        if (a.value is Tuple) {
          if (t.elts.size != a.value.elts.size) fail("Incompatible tuples")
          t.elts.zip(a.value.elts).flatMap { destructLVal(mkAssign(it.first, it.second)) }
        } else {
          val v = fresh()
          listOf(mkAssign(mkName(v, true), a.value))
                  .plus(t.elts.mapIndexed { i,e -> destructLVal(mkAssign(e, mkSubscript(v, mkIndex(i)))) }.flatten())
        }
      }
      else -> fail()
    }
  }

  fun procExpr(e: TExpr): Pair<List<Assign>,TExpr> = if (destrExpr)
    splitExpression(e, freshNames)
  else
    emptyList<Assign>() to e

  fun procLVal(e: TExpr): Pair<List<Assign>,TExpr> = if (destrExpr) {
    when(e) {
      is Name -> emptyList<Assign>() to e
      is Attribute -> {
        val (s, v) = procExpr(e.value)
        s to e.copy(value = v)
      }
      is Subscript -> {
        val (vs, v) = procExpr(e.value)
        val (ss, r) = when(e.slice) {
          is Index -> {
            val (`is`, iv) = procExpr(e.slice.value)
            `is` to e.slice.copy(value = iv)
          }
          is Slice -> {
            fun split(s: TExpr?): Pair<List<Assign>,TExpr?> = if (s == null)
              emptyList<Assign>() to e
            else
              procExpr(s)
            val (ls, lv) = split(e.slice.lower)
            val (us, uv) = split(e.slice.upper)
            val (ss, sv) = split(e.slice.step)
            ls.plus(us).plus(ss) to e.slice.copy(lower = lv, upper = uv, step = sv)
          }
          else -> fail()
        }
        vs.plus(ss) to e.copy(value = v, slice = r)
      }
      is Tuple -> {
        val (es, ev) = e.elts.map { procLVal(it) }.unzip()
        es.flatten() to e.copy(elts = ev)
      }
      else -> fail()
    }
  } else emptyList<Assign>() to e

  fun procStmts(s: Iterable<Stmt>): List<Stmt> = s.flatMap(::procStmt)
  fun procStmt(s: Stmt): List<Stmt> = when(s) {
    is Expr -> {
      val (es, ev) = procExpr(s.value)
      es.plus(s.copy(value = ev))
    }
    is Assign -> {
      val (ls, lv) = procLVal(s.target)
      val (rs, rv) = procExpr(s.value)
      ls.plus(rs).plus(s.copy(target = lv, value = rv))
    }
    is If -> listOf(s.copy(body = procStmts(s.body), orelse = procStmts(s.orelse)))
    is While -> {
      val newBody = procStmts(s.body)
      val newWhile = if (destrWhile) {
        val (ts, v) = splitExpression(s.test, freshNames)
        if (ts.isNotEmpty())
          While(
                  NameConstant(true),
                  body = ts.plus(If(v, newBody, listOf(Break)))
          )
        else
          s.copy(test = v, body = newBody)
      } else
        s.copy(body = newBody)
      listOf(newWhile)
    }
    is For -> {
      if (destrFor) {
        val coll = freshNames.fresh()
        listOf(mkAssign(mkName(coll,true), s.iter)).plus(
            procStmt(While(
                Call(mkName("has_next"), listOf(mkName(coll)), emptyList()),
                listOf(
                        mkAssign(s.target, Call(mkName("next"), listOf(mkName(coll)), emptyList()))
                ).plus(s.body)
        )))
      } else
        listOf(s.copy(body = procStmts(s.body)))
    }
    else -> listOf(s)
  }
  fun procFunc(f: FunctionDef) = f.copy(body = procStmts(f.body))
}

abstract class StmtVisitor<Ctx> {
  abstract fun visitStmt(s: Stmt, ctx: Ctx)
  fun procStmts(c: List<Stmt>, ctx: Ctx) = c.map { procStmt(it, ctx) }
  fun procStmt(s: Stmt, ctx: Ctx) {
    visitStmt(s, ctx)
    when(s) {
      is If -> {
        procStmts(s.body, ctx)
        procStmts(s.orelse, ctx)
      }
      is While -> {
        procStmts(s.body, ctx)
      }
      is For -> {
        procStmts(s.body, ctx)
      }
      else -> {}
    }
  }
}

abstract class ExprVisitor<Ctx>: StmtVisitor<Ctx>() {
  abstract fun visitExpr(e: TExpr, ctx: Ctx)
  fun procExprs(c: List<TExpr>, ctx: Ctx) = c.forEach { procExpr(it, ctx) }
  open fun procComprehension(c: Comprehension, ctx: Ctx) {
    procExpr(c.iter, ctx)
    procExprs(c.ifs, ctx)
  }
  open fun procExpr(e: TExpr, ctx: Ctx) {
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
      is Lambda -> procExpr(e.body, ctx)
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
  override fun visitStmt(s: Stmt, ctx: Ctx) {
    when(s) {
      is Expr -> procExpr(s.value, ctx)
      is Assert -> procExprs(flatten(s.test, s.msg), ctx)
      is Assign -> procExprs(listOf(s.target, s.value), ctx)
      is AnnAssign -> procExprs(flatten(s.target, s.value), ctx)
      is AugAssign -> procExprs(listOf(s.target, s.value), ctx)
      is If -> procExpr(s.test, ctx)
      is While -> procExpr(s.test, ctx)
      is For -> procExprs(listOf(s.target, s.iter), ctx)
      is Return -> procExprs(flatten(s.value), ctx)
      is Pass -> Unit
      is Break -> Unit
      is Continue -> Unit
      else -> fail()
    }
  }
}

class GatherLocalVars: StmtVisitor<MutableSet<String>>() {
  override fun visitStmt(s: Stmt, ctx: MutableSet<String>) {
    when(s) {
      is Assign -> ctx.addAll(getVarNamesInStoreCtx(s.target))
      is AnnAssign -> ctx.addAll(getVarNamesInStoreCtx(s.target))
      is AugAssign -> ctx.addAll(getVarNamesInStoreCtx(s.target))
      is For -> ctx.addAll(getVarNamesInStoreCtx(s.target))
      else -> {}
    }
  }
}

val FunctionDef.allArgs get() = args.posonlyargs + args.args + args.kwonlyargs + flatten(args.vararg, args.kwarg)
fun gatherLocalVars(f: FunctionDef): Set<String> {
  val res = mutableSetOf<String>()
  GatherLocalVars().procStmts(f.body, res)
  val argParamNames = f.allArgs.map { it.arg }
  return res.plus(argParamNames)
}

fun desugarExprs(s: Stmt): Stmt = ExprToCalls().procStmt(s, Unit).first
fun desugarExprs(f: FunctionDef): FunctionDef {
  val body = ExprToCalls().procStmts(f.body, Unit).first
  //val localVars = gatherLocalVars(f)
  //val lambdaToFuncs = LambdaToFuncs(f.name, localVars)
  //return lambdaToFuncs.localFuncs.plus(f.copy(body = lambdaToFuncs.procStmts(body)))
  return f.copy(body = body)
  //return Desugar().procFunc(f)
}
fun desugarExprs(c: ClassDef): ClassDef = c.copy(body = c.body.map(::desugarExprs))


fun tupleAssignDestructor(tmpVars: FreshNames, s: Stmt): List<Stmt>? {
  return if (s is Assign && s.target is Tuple) {
    val tmpName = tmpVars.fresh("td")
    listOf(mkAssign(mkName(tmpName, true), s.value)).plus(
        s.target.elts.mapIndexed { i,e -> mkAssign(e, mkSubscript(tmpName, mkIndex(i))) }
    )
  } else null
}

fun destructTupleAssign(f: FunctionDef): FunctionDef = RuleSetTransformer(::tupleAssignDestructor).transform(f)

fun forLoopsDestructor(tmpVars: FreshNames, s: Stmt): List<Stmt>? {
   return if (s is For) {
    val tmp = tmpVars.fresh("tmp_for")
    listOf(
        mkAssign(mkName(tmp, true), mkCall("iter", listOf(s.iter))),
        While(mkCall("has_next", listOf(mkName(tmp))), listOf(mkAssign(s.target, mkCall("next", listOf(mkName(tmp))))).plus(s.body))
    )
  } else null
}

fun destructForLoops(f: FunctionDef): FunctionDef = RuleSetTransformer(::forLoopsDestructor).transform(f)

fun main() {
  val ignoredFuncs = setOf("cache_this", "hash", "ceillog2", "floorlog2")
  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_phase0_dev.txt")
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  val defs = parsed.map { toStmt(it) }
  val fDefs = defs.filterIsInstance<FunctionDef>().filter { it.name !in ignoredFuncs }
  fDefs.forEach {
    pyPrintFunc(destructForLoops(destructTupleAssign(transformForEnumerate(transformForOps(it)))))
    println()
  }
}