package onotole

import onotole.typelib.TLSig
import onotole.typelib.TLTClass
import onotole.util.mergeExprTypers
import onotole.util.toClassVal
import onotole.util.toFAtom

class VarTypesAnalysis(val f: FunctionDef, typer: ExprTyper): ForwardAnalysis<ExprTyper>() {
  override val bottom = typer
  override fun merge(a: ExprTyper, b: ExprTyper): ExprTyper {
    return mergeExprTypers(a, b)
  }

  override fun procAssign(t: TExpr, v: TExpr, p: Stmt, ctx: ExprTyper): ExprTyper {
    val exprType = if (p is AnnAssign) {
      parseType(ctx, p.annotation)
    } else {
      ctx[v].asType()
    }
    return ctx.updated(when(t) {
      is Name -> listOf(t.id to exprType)
      is Subscript, is Attribute -> emptyList<Pair<String,RTType>>()
      is Tuple -> TODO()
      else -> fail()
    })
  }

  fun analyze() {
    val ctx = bottom.updated(getFunArgs(f).map { it.first.arg to parseType(bottom, it.first.annotation!!) })
    analyze(f.body, ctx)
  }
}

class MethodToFuncTransformer(val f: FunctionDef, val typer: ExprTyper) {
  val lva: LiveVarAnalysis
  init {
    lva = LiveVarAnalysis()
    lva.analyze(f.body, emptySet())
  }

  fun transform(): TExpr {
    val (comment, body) = if (f.body[0] is Expr && (f.body[0] as Expr).value is Str) {
      (f.body[0] as Expr).value as Str to f.body.subList(1, f.body.size)
    } else {
      null to f.body
    }

    val outcomeType = (f.returns!! as CTV).v as ClassVal
    val throwsExcn = outcomeType.name == "<Outcome>"
    val returnType = if (throwsExcn) outcomeType.tParams[0].asClassVal() else outcomeType
    val procedure = returnType.name == "pylib.None" || returnType.name == "None"

    val expr = transform(body, getFunArgs(f).map { it.first.arg }.toSet()) {
      if (procedure)
        if (throwsExcn) mkResult(NameConstant(null), TypeResolver.topLevelTyper) else NameConstant(null)
      else
        null
    }!!
    return expr
  }

  fun transform(c: List<Stmt>, av: Set<String>, cont: () -> TExpr?): TExpr? {
    return if (c.isNotEmpty()) {
      transform(c[0], av) {
        val nav = getNewDefs(c[0], av) ?: emptySet()
        transform(c.subList(1,c.size), nav.union(av), cont)
      }
    } else cont()
  }

  fun transform(s: Stmt, av: Set<String>, cont: () -> TExpr?): TExpr? {
    return when(s) {
      is Assert -> transform(Expr(assertToExpr(s)), av, cont)
      is Expr -> transform(Assign(mkName("_", true), s.value), av, cont)
      is Assign -> {
        val t = s.target as Name
        val b = Keyword(t.id, s.value)
        val nextExpr = cont() ?: fail()
        val (bindings, next) = if (nextExpr is Let) {
          listOf(b).plus(nextExpr.bindings) to nextExpr.value
        } else listOf(b) to nextExpr
        Let(bindings = bindings, next)
      }
      is AnnAssign -> transform(mkAssign(s.target, s.value!!), av, cont)
      is Return -> {
        (s.value ?: NameConstant(null))
      }
      is If -> {
        val k = cont()
        val bDefs = getNewDefs(s.body, av)
        val eDefs = getNewDefs(s.orelse, av)
        when {
          bDefs == null && eDefs == null -> {
            // returns in both branches
            if (k != null)
              TODO() // not reachable
            val body = transform(s.body, av) { null } ?: fail()
            val orelse = transform(s.orelse, av) { null } ?: fail()
            IfExp(s.test, body, orelse)
          }

          bDefs == null && eDefs != null -> {
            if (k == null)
              TODO()
            val body = transform(s.body, av) { null } ?: fail()
            val orelse = transform(s.orelse, av) { k } ?: fail()
            IfExp(s.test, body, orelse)
          }

          eDefs == null && bDefs != null -> {
            if (k == null)
              TODO()
            val body = transform(s.body, av) { k } ?: fail()
            val orelse = transform(s.orelse, av) { null } ?: fail()
            IfExp(s.test, body, orelse)
          }

          else -> {
            if (k == null)
              TODO()
            val defs = getNewDefs(s, av)!!
            val next = k
            val lvs = lva.after[s]!!
            val liveDefs = defs.intersect(lvs)
            val (tgtVar, retExpr) = if (liveDefs.isEmpty())
              Pair("_", mkResult(NameConstant(null), TypeResolver.topLevelTyper))
            else if (liveDefs.size == 1)
              Pair(liveDefs.first(), mkName(liveDefs.first()))
            else
              TODO()
            val body = transform(s.body, av) { retExpr } ?: fail()
            val orelse = transform(s.orelse, av) { retExpr } ?: fail()
            Let(listOf(Keyword(tgtVar, IfExp(s.test, body, orelse))), next)
          }
        }
      }
      is While -> {
        when {
          s.test == NameConstant(true) -> {
            val nextE = cont()
            if (nextE != null) fail()
            val defs = getNewDefs(s.body, av)!!
            val whileLvs = calcLiveVars(s, emptySet()).intersect(defs)
            if (whileLvs.size != 1) TODO()
            val fixFuncType = ClassVal("pylib.Callable", listOf(
                typer[mkName(whileLvs.first())].asType().toFAtom().toClassVal(),
                typer[mkName(whileLvs.first())].asType().toFAtom().toClassVal(),
            ))
            val lamArgs = Arguments(args = listOf(Arg("fix", CTV(fixFuncType))).plus(whileLvs.map { Arg(it, CTV(typer[mkName(it)].asType().toFAtom().toClassVal())) }))
            val body = s.body.plus(Return(mkCall(mkName("fix"), listOf(mkName(whileLvs.first())))))
            val lamBody = transform(body, av, cont)!!
            val lam = Lambda(lamArgs, lamBody)
            val sig = TLSig(emptyList(), listOf("lam" to TLTClass("pylib.object", emptyList())), TLTClass("pylib.object", listOf()))
            val loopCall = mkCall(CTV(FuncInst("loop", sig)), listOf(mkName(whileLvs.first()), lam))
            loopCall
          }
          s.test == NameConstant(false) -> TODO()
          isCollectionIterationLoop(s) -> {
            val nextE = cont() ?: fail()
            transformCollectionIterWhile(s, av, nextE)
          }
          else -> {
            TODO()
          }
        }
      }
      else -> TODO()
    }
  }

  fun calcLiveVars(s: Stmt, out: Set<String>): Set<String> {
    return when(s) {
      is Expr -> liveVarAnalysis(s.value).union(out)
      is Assign -> out.minus(getVarNamesInStoreCtx(s.target)).plus(liveVarAnalysis(s.value)).plus(getVarNamesInLoadCtx(s.target))
      is If -> liveVarAnalysis(s.test).union(calcLiveVars(s.body, out)).union(calcLiveVars(s.orelse, out))
      is While -> calcLiveVars(s, out)
      is Return -> s.value?.let { liveVarAnalysis(it) } ?: setOf()
      else -> TODO()
    }
  }

  fun calcLiveVars(ss: List<Stmt>, out: Set<String>) = ss.foldRight(out) { st, acc -> calcLiveVars(st, acc) }
  fun calcLiveVars(s: While, out: Set<String>): Set<String> {
    var curr = out
    do {
      val prev = curr
      curr = calcLiveVars(s.body, curr).union(out).union(liveVarAnalysis(s.test))
    } while (curr != prev)
    return curr
  }

  fun getNewDefs(c: List<Stmt>, av: Set<String>): Set<String>? {
    val acc = mutableSetOf<String>()
    c.forEach { s ->
      val res = getNewDefs(s, av.union(acc))
      if (res == null)
        return null
      acc.addAll(res)
    }
    return acc
  }
  fun getNewDefs(s: Stmt, av: Set<String>): Set<String>? {
    return when(s) {
      is Assert, is Expr -> emptySet()
      is Assign -> {
        setOf((s.target as Name).id)
      }
      is AnnAssign -> setOf((s.target as Name).id)
      is If -> {
        val bDefs = getNewDefs(s.body, av)
        val eDefs = getNewDefs(s.orelse, av)
        when {
          bDefs != null && eDefs != null -> bDefs.intersect(eDefs.union(av)).union(eDefs.intersect(bDefs.union(av)))
          bDefs != null && eDefs == null -> bDefs
          bDefs == null && eDefs != null -> eDefs
          else -> null
        }
      }
      is Return -> null
      is While -> getNewDefs(s.body, av)
      else -> TODO()
    }
  }

  fun isCollectionIterationLoop(s: While): Boolean {
    if (!isCall(s.test, "pylib.has_next"))
      return false
    val firstStmt = s.body[0]
    return if (firstStmt is Assign && isCall(firstStmt.value, "pylib.next"))
      true
    else firstStmt is AnnAssign && isCall(firstStmt.value!!, "pylib.next")
  }
  fun transformCollectionIterWhile(s: While, av: Set<String>, nextE: TExpr): TExpr {
    val body = s.body.subList(1, s.body.size)
    val (tgt, tgtType) = when (val fst = s.body[0]) {
      is Assign -> (fst.target as Name) to typer[fst.value].asType()
      is AnnAssign -> (fst.target as Name) to parseType(typer, fst.annotation).asType()
      else -> TODO()
    }
    val coll = when (val fst = s.body[0]) {
      is Assign -> (fst.value as Call).args[0]
      is AnnAssign -> (fst.value as Call).args[0]
      else -> TODO()
    }

    val defs = getNewDefs(body, av)!!
    val lvs = lva.after[s]!!

    val outVars = defs.intersect(lvs)
    if (outVars.size != 1) TODO()

    val bodyExpr = transform(body, av) { mkName(outVars.first()) }!!

    val lamArgs = Arguments(args = outVars.map { Arg(it, CTV(typer[mkName(it)].asType().toFAtom().toClassVal())) }
        .plus(Arg(tgt.id, CTV(tgtType.toFAtom().toClassVal()))))
    val lam = Lambda(lamArgs, bodyExpr)

    val sig = TLSig(emptyList(), listOf("lam" to TLTClass("pylib.object", emptyList())), TLTClass("pylib.object", listOf()))
    val loopCall = mkCall(CTV(FuncInst("seq_loop", sig)), listOf(coll, mkName(outVars.first()), lam))
    return Let(listOf(Keyword(outVars.first(), loopCall)), nextE)
  }

  fun findReturn(b: List<Stmt>): Return? {
    if (b.isEmpty())
      return null
    return when(val st = b[0]) {
      is Assign, is AnnAssign, is AugAssign, is Assert, is Expr -> findReturn(b.subList(1, b.size))
      is While -> {
        if (findReturn(st.body) != null)
          fail()
        else
          findReturn(b.subList(1, b.size))
      }
      is If -> {
        val br = findReturn(st.body)
        val er = findReturn(st.orelse)
        val nr = findReturn(b.subList(1, b.size))
        when {
          br != null && er == null && nr == null -> br
          er != null && br == null && nr == null -> er
          nr != null && br == null && er == null -> nr
          else -> null
        }
      }
      is Return -> st
      else -> TODO()
    }
  }
}