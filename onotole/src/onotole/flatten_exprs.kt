package onotole

import onotole.lib_defs.Additional
import onotole.lib_defs.BLS
import onotole.lib_defs.PyLib
import onotole.lib_defs.SSZLib
import onotole.rewrite.RuleSetTransformer
import onotole.rewrite.StmtTransformRule
import java.nio.file.Files
import java.nio.file.Paths

fun isNameOrConst(e: TExpr?) = e == null || e is Name || e is Constant
fun introduceVar(tmpVars: FreshNames, e: TExpr): Pair<List<Stmt>, TExpr> {
  val tmp = tmpVars.fresh("tmp")
  return listOf(Assign(mkName(tmp, true), e)) to mkName(tmp)
}

fun assertSimplifier(tmpVars: FreshNames, s: Stmt): List<Stmt>? {
  return if (s is Assert && (!isNameOrConst(s.test) || s.msg != null && !isNameOrConst(s.msg))) {
    val (testStmts, testExpr) = if (!isNameOrConst(s.test)) introduceVar(tmpVars, s.test) else emptyList<Stmt>() to s.test
    val (msgStmts, msgExpr) = if (s.msg != null && !isNameOrConst(s.msg)) introduceVar(tmpVars, s.msg) else emptyList<Stmt>() to s.msg
    testStmts.plus(msgStmts).plus(s.copy(testExpr, msgExpr))
  } else null
}
fun returnSimplifier(tmpVars: FreshNames, s: Stmt): List<Stmt>? {
  return if (s is Return && s.value != null && !isNameOrConst(s.value)) {
    val tmp = tmpVars.fresh("tmp_ret")
    listOf(Assign(mkName(tmp, true), s.value), s.copy(value = mkName(tmp)))
  } else null
}
fun whileSimplifier(tmpVars: FreshNames, s: Stmt): List<Stmt>? {
  return if (s is While && s.test !is NameConstant) {
    val (tmpName, testStmts) = if (s.test !is Name) {
      val tmp = tmpVars.fresh("tmp_w")
      tmp to listOf(Assign(mkName(tmp, true), s.test))
    } else s.test.id to emptyList()
    listOf(While(test = NameConstant(true),
        body = testStmts.plus(If(mkName(tmpName), s.body, listOf(Break)))))
  } else null
}
fun ifSimplifier(tmpVars: FreshNames, s: Stmt): List<Stmt>? {
  return if (s is If && s.test !is NameConstant && s.test !is Name) {
    val tmp = tmpVars.fresh("tmp_if")
    listOf(Assign(mkName(tmp, true), s.test), s.copy(test = mkName(tmp)))
  } else null
}
inline fun <reified T: TExpr> on(e: TExpr): T? = if (e is T) e else null

fun tule(e: TExpr) {
  on<Subscript>(e)?.ctx
}

fun assignSimplifier(tmpVars: FreshNames, s: Stmt): List<Stmt>? {
  fun isSimpleTarget(tgt: TExpr): Boolean = when(tgt) {
    is Name -> true
    is Attribute -> tgt.value is Name
    is Subscript -> tgt.value is Name && when(tgt.slice) {
      is Index -> isNameOrConst(tgt.slice.value)
      is Slice -> isNameOrConst(tgt.slice.lower) && isNameOrConst(tgt.slice.upper) && isNameOrConst(tgt.slice.step)
      else -> TODO()
    }
    is Tuple -> tgt.elts.all(::isSimpleTarget)
    else -> fail("assign to $tgt is not supported")
  }
  fun replaceIfComplex(complex: Boolean, e: TExpr) = if (complex) introduceVar(tmpVars, e) else emptyList<Stmt>() to e
  fun replaceComplexExprOpt(e: TExpr?) = if (e != null)
    replaceIfComplex(!isNameOrConst(e), e)
  else emptyList<Stmt>() to e
  fun process(tgt: TExpr): Pair<List<Stmt>, TExpr> = when(tgt) {
    is Attribute -> {
      val (stmts, expr) = introduceVar(tmpVars, tgt.value)
      stmts to tgt.copy(value = expr)
    }
    is Subscript -> {
      val (valStmt, valExpr) = replaceIfComplex(tgt.value !is Name, tgt.value)
      val (sliceStmts, sliceExpr) = when(tgt.slice) {
        is Index -> {
          val (stmts, expr) = replaceIfComplex(!isNameOrConst(tgt.slice.value), tgt.slice.value)
          stmts to tgt.slice.copy(value = expr)
        }
        is Slice -> {
          val (lowerStmts, lowerExpr) = replaceComplexExprOpt(tgt.slice.lower)
          val (upperStmts, upperExpr) = replaceComplexExprOpt(tgt.slice.upper)
          val (stepStmts, stepExpr) = replaceComplexExprOpt(tgt.slice.step)
          lowerStmts.plus(upperStmts).plus(stepStmts) to tgt.slice.copy(lowerExpr, upperExpr, stepExpr)
        }
        else -> TODO()
      }
      valStmt.plus(sliceStmts) to tgt.copy(value = valExpr, slice = sliceExpr)
    }
    is Tuple -> {
      val (stmts, exprs) = tgt.elts.map { process(it) }.unzip()
      stmts.flatten() to tgt.copy(elts = exprs)
    }
    else -> fail("assign to $tgt is not supported")
  }
  return if (s is Assign && !isSimpleTarget(s.target)) {
    val (stmts, expr) = process(s.target)
    stmts.plus(s.copy(target = expr))
  } else if (s is AnnAssign && !isSimpleTarget(s.target)) {
    val (stmts, expr) = process(s.target)
    stmts.plus(s.copy(target = expr))
  } else if (s is AugAssign && !isSimpleTarget(s.target)) {
    val (stmts, expr) = process(s.target)
    stmts.plus(s.copy(target = expr))
  } else null
}

fun callsSimplifier(tmpVars: FreshNames, s: Stmt): List<Stmt>? {
  fun isSimpleFuncRef(e: TExpr) = isNameOrConst(e) || e is Attribute || e is CTV
  fun isComplexExpr(e: TExpr): Boolean = when(e) {
    is CTV, is Constant, is Name, is Lambda, is Let -> false
    is Attribute -> !isNameOrConst(e.value)
    is Subscript -> !isNameOrConst(e.value) || when(e.slice) {
      is Index -> !isNameOrConst(e.slice.value)
      is Slice -> !listOf(e.slice.lower, e.slice.upper, e.slice.step).all(::isNameOrConst)
      else -> TODO()
    }
    is Call -> !isSimpleFuncRef(e.func) || !e.args.plus(e.keywords.map { it.value }).all(::isNameOrConst)
    is IfExp -> !listOf(e.test, e.body, e.orelse).all(::isNameOrConst)
    is GeneratorExp -> !e.generators.map { it.iter }.all(::isNameOrConst)
    is Tuple -> !e.elts.all(::isNameOrConst)
    is PyList -> !e.elts.all(::isNameOrConst)
    is PySet -> !e.elts.all(::isNameOrConst)
    is PyDict -> !e.keys.plus(e.values).all(::isNameOrConst)
    is Starred -> !isNameOrConst(e.value)
    else -> TODO()
  }

  fun simplifyExpr(e: TExpr): Pair<List<Stmt>,TExpr> = when(e) {
    is Constant, is Name -> fail()
    is Attribute -> {
      val (stmts, expr) = introduceVar(tmpVars, e.value)
      stmts to e.copy(value = expr)
    }
    is Subscript -> {
      val (valStmts, valExpr) = if (!isNameOrConst(e.value)) introduceVar(tmpVars, e.value) else emptyList<Stmt>() to e.value
      val (sliceStmts, sliceExpr) = when(e.slice) {
        is Index -> if (!isNameOrConst(e.slice.value)) {
          val (stmts, expr) = introduceVar(tmpVars, e.slice.value)
          stmts to e.slice.copy(expr)
        } else emptyList<Stmt>() to e.slice
        is Slice -> {
          val (lowerStmts, lowerExpr) =
              if (e.slice.lower != null && !isNameOrConst(e.slice.lower)) introduceVar(tmpVars, e.slice.lower)
              else emptyList<Stmt>() to e.slice.lower
          lowerStmts to lowerExpr
          val (upperStmts, upperExpr) =
              if (e.slice.upper != null && !isNameOrConst(e.slice.upper)) introduceVar(tmpVars, e.slice.upper)
              else emptyList<Stmt>() to e.slice.upper
          lowerStmts to lowerExpr
          val (stepStmts, stepExpr) =
              if (e.slice.step != null && !isNameOrConst(e.slice.step)) introduceVar(tmpVars, e.slice.step)
              else emptyList<Stmt>() to e.slice.step
          lowerStmts.plus(upperStmts).plus(stepStmts) to e.slice.copy(lowerExpr, upperExpr, stepExpr)
        }
        else -> TODO()
      }
      valStmts.plus(sliceStmts) to e.copy(value = valExpr, slice = sliceExpr)
    }
    is Call -> {
      val (funcStmts, funcExpr) = if (!isSimpleFuncRef(e.func)) introduceVar(tmpVars, e.func) else emptyList<Stmt>() to e.func
      val (argStmts, argExprs) = e.args.map {
        if (!isNameOrConst(it)) introduceVar(tmpVars, it) else emptyList<Stmt>() to it
      }.unzip()
      val (kwdStmts, kwdExprs) = e.keywords.map {
        val (stmts, kwdExpr) =
            if (!isNameOrConst(it.value)) introduceVar(tmpVars, it.value)
            else emptyList<Stmt>() to it.value
        stmts to it.copy(value = kwdExpr)
      }.unzip()
      funcStmts.plus(argStmts.flatten()).plus(kwdStmts.flatten()) to e.copy(funcExpr, argExprs, kwdExprs)
    }
    is IfExp -> {
      val (testStmts, testExpr) = if (!isNameOrConst(e.test)) introduceVar(tmpVars, e.test) else emptyList<Stmt>() to e.test
      val (bodyStmts, bodyExpr) = if (!isNameOrConst(e.body)) introduceVar(tmpVars, e.body) else emptyList<Stmt>() to e.body
      val (orelseStmts, orleseExpr) = if (!isNameOrConst(e.orelse)) introduceVar(tmpVars, e.orelse) else emptyList<Stmt>() to e.orelse
      testStmts.plus(bodyStmts).plus(orelseStmts) to e.copy(testExpr, bodyExpr, orleseExpr)
    }
    is GeneratorExp -> {
      val (stmts, generators) = e.generators.map {
        val (stmts, expr) = if (!isNameOrConst(it.iter)) introduceVar(tmpVars, it.iter)
        else emptyList<Stmt>() to it.iter
        stmts to it.copy(iter = expr)
      }.unzip()
      stmts.flatten() to e.copy(generators = generators)
    }
    is Tuple -> {
      val (stmts, exprs) = e.elts.map {
        if (!isNameOrConst(it)) introduceVar(tmpVars, it) else emptyList<Stmt>() to it
      }.unzip()
      stmts.flatten() to e.copy(elts = exprs)
    }
    is PyList -> {
      val (stmts, exprs) = e.elts.map {
        if (!isNameOrConst(it)) introduceVar(tmpVars, it) else emptyList<Stmt>() to it
      }.unzip()
      stmts.flatten() to e.copy(elts = exprs)
    }
    is PySet -> {
      val (stmts, exprs) = e.elts.map {
        if (!isNameOrConst(it)) introduceVar(tmpVars, it) else emptyList<Stmt>() to it
      }.unzip()
      stmts.flatten() to e.copy(elts = exprs)
    }
    is PyDict -> {
      val (keyStmts, keyExprs) = e.keys.map {
        if (!isNameOrConst(it)) introduceVar(tmpVars, it) else emptyList<Stmt>() to it
      }.unzip()
      val (valueStmts, valueExprs) = e.values.map {
        if (!isNameOrConst(it)) introduceVar(tmpVars, it) else emptyList<Stmt>() to it
      }.unzip()
      keyStmts.plus(valueStmts).flatten() to e.copy(keys = keyExprs, values = valueExprs)
    }
    is Starred -> {
      val (stmts, expr) = introduceVar(tmpVars, e.value)
      stmts to e.copy(value = expr)
    }
    else -> TODO()
  }

  return when {
    s is Expr && isComplexExpr(s.value) -> {
      val (stmts, expr) = simplifyExpr(s.value)
      stmts.plus(s.copy(expr))
    }
    s is Assign && isComplexExpr(s.value) -> {
      val (stmts, expr) = simplifyExpr(s.value)
      stmts.plus(s.copy(value = expr))
    }
    s is AnnAssign && s.value != null && isComplexExpr(s.value) -> {
      val (stmts, expr) = simplifyExpr(s.value)
      stmts.plus(s.copy(value = expr))
    }
    s is AugAssign && !isNameOrConst(s.value) -> {
      val (stmts, expr) = introduceVar(tmpVars, s.value)
      stmts.plus(s.copy(value = expr))
    }
    else -> null
  }
}

fun desugarStmts(f: FunctionDef): FunctionDef {
  return RuleSetTransformer(listOf(
      ::forLoopsDestructor,
      ::whileSimplifier,
      ::ifSimplifier,
      ::returnSimplifier,
      ::assertSimplifier,
      ::assignSimplifier,
      ::callsSimplifier
  )).transform(f)
}

fun main() {
  PyLib.init()
  SSZLib.init()
  BLS.init()
  val specVersion = "phase0"
  Additional.init(specVersion)
  val tlDefs = loadSpecDefs(specVersion)
  PhaseInfo.getPkgDeps(specVersion).forEach {
    TypeResolver.importFromPackage(it)
  }
  TypeResolver.importFromPackage(specVersion)
  val phase = specVersion
  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_${phase}_dev.txt")
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  val defs = parsed.map { toStmt(it) }

  val fDefs = defs.filterIsInstance<FunctionDef>()
  fDefs.forEach { registerFuncInfo(phase, it) }

  val excFuncs = phase0_exc.plus(pylib_exc)
  fDefs.map { it.name }.forEach { n ->
    val fdi = getFuncDefInfo(phase, n)
    println("-----")
    val transformed = desugarStmts(fdi.destructedWithCopies)
    pyPrintFunc(transformed)
    println("-")
    val fdi2 = FuncDefInfo(transformed)
    val typer = TypeResolver.topLevelTyper.updated(fdi2.varTypes)
    val noExc = DeExceptionizer(transformed.name, typer, excFuncs).transform(transformed)
    pyPrintFunc(noExc)
  }
}
