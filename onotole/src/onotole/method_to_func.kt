package onotole

import onotole.exceptions.ExcnChecker
import onotole.type_inference.classValToTLType
import onotole.typelib.TLSig
import onotole.typelib.TLTCallable
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

class MethodToFuncTransformer(val f: FunctionDef, val excnChecker: ExcnChecker, val typer: ExprTyper) {
  val lva: LiveVarAnalysis
  val throwsExcn = f.returns != null && isOutcome(f.returns)
  val procedure = isProcedure(f)
  val varTypes = inferVarTypes(typer, f)
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

    val expr = transform(body, getFunArgs(f).map { it.first.arg }.toSet()) {
      if (procedure) {
        val retVal = NameConstant(null)
        if (throwsExcn) mkResult(retVal, TypeResolver.topLevelTyper) else retVal
      } else
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
        val t = extractTargetNames(s.target)
        val b = LetBinder(t, s.value)
        val nextExpr = cont() ?: fail()
        val (bindings, next) = if (nextExpr is Let) {
          listOf(b).plus(nextExpr.bindings) to nextExpr.value
        } else listOf(b) to nextExpr
        Let(bindings = bindings, next)
      }
      is AnnAssign -> transform(mkAssign(s.target, s.value!!), av, cont)
      is Return -> {
        val r = (s.value ?: NameConstant(null))
        r
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
            val liveDefs = defs.intersect(lvs).toList()
            val retVal = if (liveDefs.isEmpty())
              NameConstant(null)
            else
              namesToTExpr(liveDefs, false)
            val ctx = typer.updated(varTypes.varTypingsAfter[s]!!)
            val body = transform(s.body, av) { retVal } ?: fail()
            val orelse = transform(s.orelse, av) { retVal } ?: fail()
            val be = excnChecker.canThrowExcn(body, ctx)
            val ee = excnChecker.canThrowExcn(orelse, ctx)
            val (body_, orelse_) = if (be || ee) {
              val outcomeVal = mkResult(retVal, ctx)
              val body_ = transform(s.body, av) { outcomeVal } ?: fail()
              val orelse_ = transform(s.orelse, av) { outcomeVal } ?: fail()
              body_ to orelse_
            } else body to orelse
            val outVars = if (liveDefs.isEmpty()) listOf("_") else liveDefs
            val ifExp = IfExp(s.test, body_, orelse_)
            val letBody = if (be || ee) mkCheck(ifExp, ctx) else ifExp
            Let(listOf(LetBinder(outVars, letBody)), next)
          }
        }
      }
      is While -> {
        when {
          s.test == NameConstant(true) -> {
            val nextE = cont()
            if (nextE != null) fail()
            val defs = getNewDefs(s.body, av)!!
            val whileLvs_ = calcLiveVars(s, emptySet()).intersect(defs).toList()
            val bodyResVal = namesToTExpr(whileLvs_, false)

            val ctx = typer.updated(varTypes.varTypingsAfter[s]!!)
            val bodyRetVal = mkCall(mkName("fix"), listOf(bodyResVal))
            val lamBody = transform(s.body.plus(Return(bodyRetVal)), av, cont)!!
            val be = excnChecker.canThrowExcn(lamBody, ctx)

            val inType = ctx[bodyResVal].asType().toFAtom().toClassVal()
            val outType = if (be) ctx[mkResult(bodyResVal, ctx)].asType().toFAtom().toClassVal() else inType
            val fixFuncType = ClassVal("pylib.Callable", listOf(inType, outType))
            val stateArgName = whileLvs_.joinToString("_")
            val lamArgs = Arguments(args = listOf(Arg("fix", CTV(fixFuncType)), Arg(stateArgName, CTV(inType))))
            val lam = Lambda(lamArgs, lamBody)
            val inT = classValToTLType(inType)
            val retT = classValToTLType(outType)
            val lamSig = TLSig(emptyList(), listOf("fix" to TLTCallable(listOf(inT), retT), stateArgName to inT), retT)
            val sig = TLSig(emptyList(),
                listOf("init" to inT, "body_fun" to TLTCallable(lamSig.args.map { it.second }, lamSig.ret)),
                lamSig.ret)
            val loopCall = mkCall(CTV(FuncInst("loop" + (if (be) "_f" else ""), sig)), listOf(mkName(stateArgName), lam))
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
      is Assign -> extractTargetNames(s.target).toSet()
      is AnnAssign -> extractTargetNames(s.target).toSet()
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
    val ctx = typer.updated(varTypes.varTypingsAfter[s]!!)

    val outVars = defs.intersect(lvs).toList()

    val bodyRetVal = namesToTExpr(outVars, false)

    val bodyExpr_ = transform(body, av) { bodyRetVal }!!
    val be = excnChecker.canThrowExcn(bodyExpr_, ctx)
    val (bodyExpr, bodyOutVal) = if (be) {
      val bodyOutcome = mkResult(bodyRetVal, ctx)
      transform(body, av) { bodyOutcome }!! to bodyOutcome
    } else bodyExpr_ to bodyRetVal

    val stateArgName = outVars.joinToString("_")
    val stateArgType = ctx[bodyRetVal].asType().toFAtom().toClassVal()
    val lamArgs = Arguments(args = listOf(Arg(stateArgName, CTV(stateArgType)), Arg(tgt.id, CTV(tgtType.toFAtom().toClassVal()))))
    val lamBody = if (outVars.size == 1)
      bodyExpr
    else
      Let(listOf(LetBinder(outVars, mkName(stateArgName))), bodyExpr)
    val lam = Lambda(lamArgs, lamBody)

    val lamSig = TLSig(emptyList(),
        listOf(stateArgName to classValToTLType(stateArgType), tgt.id to classValToTLType(tgtType.toFAtom().toClassVal())),
        classValToTLType(ctx[bodyOutVal].asType().toFAtom().toClassVal()))

    val sig = TLSig(emptyList(), listOf("lam" to TLTCallable(lamSig.args.map { it.second }, lamSig.ret)), lamSig.ret)
    val loopCall = mkCall(CTV(FuncInst("seq_loop" + (if (be) "_f" else ""), sig)), listOf(coll, bodyRetVal, lam))
    val letBody = if (be) mkCheck(loopCall, ctx) else loopCall
    return Let(listOf(LetBinder(outVars, letBody)), nextE)
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