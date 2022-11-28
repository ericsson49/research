package onotole

import onotole.exceptions.ExcnChecker
import onotole.typelib.TLSig
import onotole.typelib.TLTCallable
import onotole.typelib.TLTClass
import onotole.util.TExprAssembler
import onotole.util.deconstruct
import onotole.util.toClassVal
import onotole.util.toFAtom
import onotole.util.toTLTClass


fun assertToExpr(s: Assert): TExpr {
  if (s.msg != null) TODO()
  val tBool = TLTClass("pylib.bool", emptyList())
  val tNone = TLTClass("pylib.None", emptyList())
  val assertCtv = CTV(FuncInst("<assert>", TLSig(emptyList(), listOf("test" to tBool), tNone)))
  return mkCall(assertCtv, listOf(s.test))
}

fun mkCheck(e: TExpr, ctx: ExprTyper): TExpr {
  val t = (ctx[e].asType() as NamedType).toTLTClass()
  val retT = if (t.name == "<Outcome>") t.params[0] else t
  val sig = TLSig(emptyList(), listOf("a" to t), retT)
  val checkCTV = FuncInst("<check>", sig)
  return mkCall(CTV(checkCTV), listOf(e))
}

fun isExceptionCheck(value: TExpr) = value is Call && value.func is CTV && value.func.v is FuncInst && value.func.v.name == "<check>"

fun mkResult(e: TExpr, typer: ExprTyper): TExpr {
  val t = (typer[e].asType() as NamedType).toTLTClass()
  return mkResult(e, t)
}

fun mkResult(e: TExpr, t: TLTClass): TExpr {
  return if (t.name != "<Outcome>") {
    val t2 = TLTClass("<Outcome>", listOf(t))
    val ctv = CTV(FuncInst("<Result>::new", TLSig(emptyList(), listOf("a" to t), t2)))
    mkCall(ctv, listOf(e))
  } else {
    e
  }
}

interface EffectDetector {
  fun hasEffect(e: TExpr, typer: ExprTyper, recursive: Boolean): Boolean
}
class MethodInExprProcessor(val tmpVars: FreshNames, val effectDetector: EffectDetector, val typer: ExprTyper) {
  fun <T: TExpr> _procExpr(p: Pair<Collection<TExpr>, TExprAssembler<T>>, outerVar: Boolean): Pair<TExpr, List<Assign>> {
    val (args, builder) = p
    val (newArgs, assgns_) = args.map { procExpr(it) }.unzip()
    val assgns = assgns_.flatten()
    val newTyper = assgns.fold(typer) { t, a -> t.updated(mapOf((a.target as Name).id to t[a.value]))}
    val newE = builder.assemble(newArgs)
    return if (effectDetector.hasEffect(newE, newTyper, false)) {
      if (outerVar) {
        newE to assgns
      } else {
        val v = mkName(tmpVars.fresh("ttmp"))
        v to assgns + listOf(mkAssign(v.copy(ctx = ExprContext.Store), newE))
      }
    } else {
      newE to assgns
    }
  }
  fun procExpr(e: TExpr, outerVar: Boolean = false): Pair<TExpr, List<Assign>> {
    return when(e) {
      is Constant, is Name, is CTV -> e to emptyList()
      is Attribute, is Subscript, is Call, is PyList, is PyDict, is Tuple ->
        _procExpr(deconstruct(e), outerVar)
      is GeneratorExp -> {
        if (e.generators.size != 1) TODO()
        val g = e.generators[0]
        val (newIter, assgns) = procExpr(g.iter)
        e.copy(generators = listOf(g.copy(iter = newIter))) to assgns
      }
      is IfExp -> e to emptyList()
      is Lambda -> e to emptyList()
      else -> TODO()
    }
  }
}

fun findLambdaArgument(sig: TLSig): Int? {
  val args = sig.args.map { it.second }
  val callables = args.filterIsInstance<TLTCallable>()
  if (callables.size > 1) TODO()
  return if (callables.isEmpty())
    null
  else
    args.indexOf(callables[0])
}

class ExprExceptionProcessor(val tmpVars: FreshNames, val excnChecker: ExcnChecker, val typer: ExprTyper) {
  fun <T: TExpr> procExpr(p: Pair<Collection<TExpr>, TExprAssembler<T>>, outerVar: Boolean): Pair<TExpr, List<Assign>> {
    val (args, builder) = p
    val (newArgs, assgns_) = args.map { procExpr(it) }.unzip()
    val assgns = assgns_.flatten()
    val newE = builder.assemble(newArgs)
    return if (excnChecker.canThrowExcn(newE, typer)) {
      val newTyper = assgns.fold(typer) { ctx, s -> ctx.updated(listOf((s.target as Name).id to ctx[s.value].asType())) }
      val resE = mkCheck(newE, newTyper)
      if (outerVar) {
        resE to assgns
      } else {
        val v = mkName(tmpVars.fresh("tmp"))
        v to assgns + listOf(mkAssign(v.copy(ctx = ExprContext.Store), resE))
      }
    } else {
      newE to assgns
    }
  }

  fun procExpr(e: TExpr, outerVar: Boolean = false): Pair<TExpr, List<Assign>> {
    return when(e) {
      is Constant, is Name, is CTV -> e to emptyList()
      is Attribute, is Subscript, is PyList, is PyDict, is Tuple -> procExpr(deconstruct(e), outerVar)
      is Call -> {
        if (e.func is CTV && e.func.v is FuncInst && findLambdaArgument(e.func.v.sig) != null) {
          val lamArgNo = findLambdaArgument(e.func.v.sig)!!
          val (lam, assgns) = procExpr(e.args[lamArgNo] as Lambda)
          if (assgns.isNotEmpty()) fail()
          if ((((lam as Lambda).returns!! as CTV).v as ClassVal).name == "<Outcome>") {
            val currSig = e.func.v.sig
            val currLamSig = currSig.args[lamArgNo].second as TLTCallable
            val newLamSig = currLamSig.copy(ret = TLTClass("<Outcome>", listOf(currLamSig.ret)))
            fun <T> List<T>.updated(i: Int, v: T): List<T> = this.subList(0, i) + listOf(v) + this.subList(i + 1, this.size)
            val newFunc = e.func.v.copy(
                name = e.func.v.name + "_f",
                sig = currSig.copy(
                    args = currSig.args.updated(lamArgNo, currSig.args[lamArgNo].copy(second = newLamSig)),
                    ret = TLTClass("<Outcome>", listOf(currSig.ret))))
            val newF = e.copy(func = CTV(newFunc), args = e.args.updated(lamArgNo, lam))
            procExpr(deconstruct(newF), outerVar)
          } else procExpr(deconstruct(e), outerVar)
        } else procExpr(deconstruct(e), outerVar)
      }
      is IfExp -> {
        val (testE, testAssgns) = procExpr(e.test)
        val (bodyE, bodyAssgns) = procExpr(e.body)
        val (orelseE, orelseAssgns) = procExpr(e.orelse)
        val bodyRes = if (bodyAssgns.isEmpty()) {
          //if (bodyE != e.body) TODO()
          bodyE
        } else {
          Let(bodyAssgns.map { LetBinder(extractTargetNames(it.target), it.value) }, bodyE)
        }
        val orelseRes = if (orelseAssgns.isEmpty()) {
          //if (orelseE != e.orelse) TODO()
          orelseE
        } else {
          Let(orelseAssgns.map { LetBinder(extractTargetNames(it.target), it.value) }, orelseE)
        }
        val tExcn = excnChecker.canThrowExcn(testE, typer)
        val bExcn = excnChecker.canThrowExcn(bodyRes, typer)
        val eExcn = excnChecker.canThrowExcn(orelseRes, typer)
        val bodyRes2 = if (bExcn != eExcn) {
          val typer = orelseAssgns.fold(typer) { ctx, s -> ctx.updated(listOf((s.target as Name).id to ctx[s.value])) }
          val bodyE2 = if (excnChecker.canThrowExcn(bodyE, typer))
            bodyE
          else
            mkResult(bodyE, typer)
          if (bodyAssgns.isEmpty()) {
            bodyE2
          } else {
            Let(bodyAssgns.map { LetBinder(extractTargetNames(it.target), it.value) }, bodyE2)
          }
        } else bodyRes
        val orelseRes2 = if (bExcn != eExcn) {
          val typer = orelseAssgns.fold(typer) { ctx, s -> ctx.updated(listOf((s.target as Name).id to ctx[s.value])) }
          val orelseE2 = if (excnChecker.canThrowExcn(orelseE, typer))
            orelseE
          else
            mkResult(orelseE, typer)
          if (orelseAssgns.isEmpty()) {
            orelseE2
          } else {
            Let(orelseAssgns.map { LetBinder(extractTargetNames(it.target), it.value) }, orelseE2)
          }
        } else orelseRes
        IfExp(testE, bodyRes2, orelseRes2) to testAssgns
      }
      is GeneratorExp -> {
        if (e.generators.size != 1) TODO()
        val g = e.generators[0]
        val v = (g.target as Name).id
        val currIter = g.iter
        val argType = (parseType(typer, g.targetAnno!!).asType() as NamedType).toTLTClass()
        val lamArgs = Arguments(args = listOf(Arg(arg = v, annotation = g.targetAnno)))
        val filterLamSig = TLTCallable(listOf(argType), TLTClass("pylib.bool", emptyList()))
        val iterClass = typer[currIter].asType() as NamedType
        val collClass = when (iterClass.name) {
          "pylib.PyList" -> "pylib.PyList"
          "pylib.Set" -> "pylib.Set"
          else -> "pylib.Sequence"
        }
        val filterSig = TLSig(emptyList(),
            listOf("lam" to filterLamSig, "coll" to TLTClass(collClass, listOf(argType))),
            TLTClass(collClass, listOf(argType)))
        val filterFI = FuncInst("pylib.filter", filterSig)
        val filters = g.ifs.fold(currIter) { a, b ->
          mkCall(CTV(filterFI), listOf(Lambda(args = lamArgs, body = b, returns = CTV(ClassVal("pylib.bool"))), a))
        }
        val newCtx = typer.updated(lamArgs.args.map { it.arg to parseType(typer, it.annotation!!) })
        val retType = newCtx[e.elt].asType().toFAtom().toClassVal()
        val mapLamSig = TLTCallable(listOf(argType), retType.toTLTClass())
        val mapSig = TLSig(listOf(),
            listOf("lam" to mapLamSig, "coll" to TLTClass(collClass, listOf(argType))),
            TLTClass(collClass, listOf(retType.toTLTClass()))
        )
        val mapFI = FuncInst("pylib.map", mapSig)
        val mapF = mkCall(CTV(mapFI), listOf(Lambda(args = lamArgs, body = e.elt, returns = CTV(retType)), filters))
        val resF = if (e.elt == mkName(v)) filters else mapF
        procExpr(resF, outerVar)
      }
      is Lambda -> {
        val newCtx = typer.forLambda(e)
        val bodyExcn = excnChecker.canThrowExcn(e.body, newCtx, recursive = true)
        val retClass = (e.returns!! as CTV).v as ClassVal
        val body = if (bodyExcn && retClass.name != "<Outcome>") {
          val ep = ExprExceptionProcessor(tmpVars, excnChecker, newCtx)
          val (body, bodyAssigns) = ep.procExpr(e.body)
          val newCtx2 = bodyAssigns.fold(newCtx) { ctx, s -> ctx.updated(listOf((s.target as Name).id to ctx[s.value])) }
          Let(bodyAssigns.map { LetBinder(extractTargetNames(it.target), it.value) }, mkResult(body, newCtx2))
        } else e.body
        val newRetType = if (bodyExcn && retClass.name != "<Outcome>") {
          CTV(ClassVal("<Outcome>", listOf(retClass)))
        } else e.returns
        e.copy(body = body, returns = newRetType) to emptyList()
      }
      else -> TODO()
    }
  }
}

class MethodProcessor(val tmpVars: FreshNames, val effectDetector: EffectDetector) {
  fun transformNode(s: Stmt, typer: ExprTyper): List<Stmt> {
    val exprProc = MethodInExprProcessor(tmpVars, effectDetector, typer)
    return when(s) {
      is Expr -> {
        val (e, assgns) = exprProc.procExpr(s.value, true)
        if (e is Name)
          assgns
        else
          assgns.plus(Expr(e))
      }
      is Assert -> {
        if (s.msg != null) TODO()
        transformNode(Expr(assertToExpr(s)), typer)
      }
      is Assign -> {
        val (e, assgns) = exprProc.procExpr(s.value, s.target is Name)
        if (effectDetector.hasEffect(e, typer, false) && s.target !is Name) {
          TODO()
        }
        when(s.target) {
          is Name -> {
            assgns.plus(s.copy(value = e))
          }
          is Attribute -> {
            val (tgtVal, tgtAssgns) = exprProc.procExpr(s.target.value)
            if (effectDetector.hasEffect(tgtVal, typer, false)) {
              TODO()
            } else {
              assgns.plus(tgtAssgns).plus(s.copy(target = s.target.copy(value = tgtVal), value = e))
            }
          }
          is Subscript -> {
            val (tgtVal, tgtAssgns) = exprProc.procExpr(s.target.value)
            if (effectDetector.hasEffect(tgtVal, typer, false)) {
              TODO()
            } else {
              assgns.plus(tgtAssgns).plus(s.copy(target = s.target.copy(value = tgtVal), value = e))
            }
          }
          else -> TODO()
        }
      }
      is AnnAssign -> {
        val (e, assgns) = exprProc.procExpr(s.value!!, true)
        assgns.plus(s.copy(value = e))
      }
      is If -> {
        val (testE, testAssgns) = exprProc.procExpr(s.test)
        if (testAssgns.isEmpty() && testE == s.test) {
          null
        } else {
          if (effectDetector.hasEffect(testE, typer, false)) {
            val newV = mkName(tmpVars.fresh("tmp"))
            val assgn = mkAssign(newV.copy(ctx = ExprContext.Store), mkCheck(testE, typer))
            testAssgns.plus(assgn).plus(s.copy(test = newV))
          } else {
            testAssgns.plus(s.copy(test = testE))
          }
        }
      }
      is While -> {
        val (testE, testAssgns) = exprProc.procExpr(s.test)
        if (testAssgns.isNotEmpty() || testE != s.test) TODO()
        null
      }
      is Return -> {
        if (s.value != null) {
          val (retE, assgns) = exprProc.procExpr(s.value)
          when {
            assgns.isEmpty() && retE == s.value -> null
            assgns.isNotEmpty() && retE == s.value -> TODO()
            else -> assgns.plus(s.copy(value = retE))
          }
        } else null
      }
//      is Return -> {
//        val value = s.value ?: NameConstant(null) //Tuple(emptyList(), ExprContext.Load)
//        if (effectDetector.hasEffect(value, typer)) {
//          val newValue = TODO("fixFuncInstType(value)")
//          if (newValue != value)
//            listOf(s.copy(value = newValue))
//          else
//            null
//        } else
//          listOf(s.copy(value = mkResult(value, typer)))
//      }
      else -> null
    } ?: listOf(s)
  }
  fun transform(c: List<Stmt>, typer: ExprTyper): Pair<List<Stmt>, ExprTyper> {
    val res = mutableListOf<Stmt>()
    var foundNew = false
    var currTyper = typer
    for(s in c) {
      val (newS, newTyper) = transform(s, currTyper)
      if (newS.size != 1 || newS[0] != s) {
        foundNew = true
      }
      res.addAll(newS)
      currTyper = newTyper
    }
    return (if (foundNew) res else c) to currTyper
  }
  fun transform(s: Stmt, ctx: ExprTyper): Pair<List<Stmt>, ExprTyper> {
    return when(s) {
      is Assign -> {
        val newCtx = when(s.target) {
          is Name -> ctx.updated(listOf(s.target.id to ctx[s.value]))
          is Subscript -> ctx
          is Attribute -> ctx
          else -> TODO()
        }
        transformNode(s, ctx) to newCtx
      }
      is AnnAssign -> {
        val newCtx = ctx.updated(listOf((s.target as Name).id to parseType(ctx, s.annotation)))
        transformNode(s, ctx) to newCtx
      }
      is If -> {
        val (body, bodyCtx) = transform(s.body, ctx)
        val (orelse, orelseCtx) = transform(s.orelse, ctx)
        val res = transformNode(s.copy(body = body, orelse = orelse), ctx)
        res to bodyCtx
      }
      is While -> {
        val (body, bodyCtx) = transform(s.body, ctx)
        val res = transformNode(s.copy(body = body), ctx)
        res to ctx
      }
      is For -> {
        val (body, bodyCtx) = transform(s.body, ctx)
        val res = transformNode(s.copy(body = body), ctx)
        res to ctx
      }
      else -> transformNode(s, ctx) to ctx
    }
  }
  fun transformFunc(f: FunctionDef, ctx: ExprTyper): FunctionDef {
    return f.copy(body = transform(f.body, ctx).first)
  }
}

class DeExceptionizer(val excnChecker: ExcnChecker, val funcInfo: Map<String, PFDescr>) {
  val tmpVars = FreshNames()

  fun transformNode(s: Stmt, typer: ExprTyper): List<Stmt> {
    val excnProcessor = ExprExceptionProcessor(tmpVars, excnChecker, typer)
    return when(s) {
      is Expr -> {
        val (e, assgns) = excnProcessor.procExpr(s.value, true)
          if (e is Name)
            assgns
          else
            assgns.plus(Expr(e))
      }
      is Assert -> {
        if (s.msg != null) TODO()
        transformNode(Expr(assertToExpr(s)), typer)
      }
      is Assign -> {
        val (e, assgns) = excnProcessor.procExpr(s.value, s.target is Name)
        if (excnChecker.canThrowExcn(e, typer) && s.target !is Name) {
          TODO()
        }
        when(s.target) {
          is Name -> {
            assgns.plus(s.copy(value = e))
          }
          is Attribute -> {
            val (tgtVal, tgtAssgns) = excnProcessor.procExpr(s.target.value)
            if (excnChecker.canThrowExcn(tgtVal, typer)) {
              TODO()
            } else {
              assgns.plus(tgtAssgns).plus(s.copy(target = s.target.copy(value = tgtVal), value = e))
            }
          }
          is Subscript -> {
            val (tgtVal, tgtAssgns) = excnProcessor.procExpr(s.target.value)
            if (excnChecker.canThrowExcn(tgtVal, typer)) {
              TODO()
            } else {
              assgns.plus(tgtAssgns).plus(s.copy(target = s.target.copy(value = tgtVal), value = e))
            }
          }
          is Tuple -> {
            assgns.plus(s.copy(value = e))
          }
          else -> TODO()
        }
      }
      is AnnAssign -> {
        val (e, assgns) = excnProcessor.procExpr(s.value!!, true)
        if (!isExceptionCheck(e) && excnChecker.canThrowExcn(e, typer)) {
          TODO()
        } else {
          assgns.plus(s.copy(value = e))
        }
      }
      is If -> {
        val (testE, testAssgns) = excnProcessor.procExpr(s.test)
        if (testAssgns.isEmpty() && testE == s.test) {
          null
        } else {
          if (excnChecker.canThrowExcn(testE, typer)) {
            val newV = mkName(tmpVars.fresh("tmp"))
            val assgn = mkAssign(newV.copy(ctx = ExprContext.Store), mkCheck(testE, typer))
            testAssgns.plus(assgn).plus(s.copy(test = newV))
          } else {
            testAssgns.plus(s.copy(test = testE))
          }
        }
      }
      is While -> {
        val (testE, testAssgns) = excnProcessor.procExpr(s.test)
        if (testAssgns.isNotEmpty() || testE != s.test) TODO()
        null
      }
      is Return -> {
        val value = s.value ?: NameConstant(null) //Tuple(emptyList(), ExprContext.Load)
        val (retE, assgns) = excnProcessor.procExpr(value, outerVar = true)
        if (excnChecker.canThrowExcn(retE, typer)) {
          val newValue = fixFuncInstType(value)
          if (newValue != value)
            assgns.plus(listOf(s.copy(value = newValue)))
          else {
            if (assgns.isNotEmpty()) TODO()
            null
          }
        } else
          assgns.plus(listOf(s.copy(value = mkResult(value, typer))))
      }
      else -> null
    } ?: listOf(s)
  }

  fun getPFDescr(n: String): PFDescr? {
    val shortName = if (n.startsWith("phase0.")) n.substring("phase0.".length) else n
    return funcInfo[shortName]
  }
  fun fixFuncInstType(e: TExpr): TExpr {
    if (e is Call && e.func is CTV && e.func.v is FuncInst && getPFDescr(e.func.v.name) != null) {
      val descr = getPFDescr(e.func.v.name)!!
      if (descr.exception) {
        val newSig = e.func.v.sig.copy(ret = TLTClass("<Outcome>", listOf(e.func.v.sig.ret)))
        val copy = e.copy(func = e.func.copy(v = e.func.v.copy(sig = newSig)))
        return copy
      }
    }
    return e
  }

  fun transform(c: List<Stmt>, typer: ExprTyper): Pair<List<Stmt>, ExprTyper> {
    val res = mutableListOf<Stmt>()
    var foundNew = false
    var currTyper = typer
    for(s in c) {
      val (newS, newTyper) = transform(s, currTyper)
      if (newS.size != 1 || newS[0] != s) {
        foundNew = true
      }
      res.addAll(newS)
      currTyper = newTyper
    }
    return (if (foundNew) res else c) to currTyper
  }
  fun transform(s: Stmt, ctx: ExprTyper): Pair<List<Stmt>, ExprTyper> {
    return when(s) {
      is Assign -> {
        val newCtx = when(s.target) {
          is Name -> ctx.updated(listOf(s.target.id to ctx[s.value]))
          is Subscript -> ctx
          is Attribute -> ctx
          is Tuple -> ctx.updated(matchNamesAndTypes(s.target.elts.map { (it as Name).id }, ctx[s.value].asType()))
          else -> TODO()
        }
        transformNode(s, ctx) to newCtx
      }
      is AnnAssign -> {
        val newCtx = ctx.updated(listOf((s.target as Name).id to parseType(ctx, s.annotation)))
        transformNode(s, ctx) to newCtx
      }
      is If -> {
        val (body, bodyCtx) = transform(s.body, ctx)
        val (orelse, orelseCtx) = transform(s.orelse, ctx)
        val res = transformNode(s.copy(body = body, orelse = orelse), ctx)
        res to bodyCtx
      }
      is While -> {
        val (body, bodyCtx) = transform(s.body, ctx)
        val res = transformNode(s.copy(body = body), ctx)
        res to ctx
      }
      is For -> {
        val (body, bodyCtx) = transform(s.body, ctx)
        val res = transformNode(s.copy(body = body), ctx)
        res to ctx
      }
      else -> transformNode(s, ctx) to ctx
    }
  }
  fun transformFunc(f: FunctionDef, ctx: ExprTyper): FunctionDef {
    val retType = (f.returns!! as CTV).v as ClassVal
    if (retType.name == "<Outcome>") TODO()
    val outType = ClassVal("<Outcome>", listOf(retType))
    return f.copy(body = transform(f.body, ctx).first, returns = CTV(outType))
  }
}