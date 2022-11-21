package onotole

import onotole.dafny.DafnyExprGen

class DafnyGen(/*currPkg: String, importedPkgs: Set<String>, */exprTyper: ExprTyper): BaseGen(/*currPkg, importedPkgs, */exprTyper) {
  var _exprGen: DafnyExprGen? = null
  val exprGen: DafnyExprGen
    get() = _exprGen
      ?: fail()

  override fun genExpr(e: TExpr, typer: ExprTyper): RExpr {
    return exprGen.genExpr(e, typer)
  }

  override val parenthesesAroundLambda = true

  override fun genName(n: String): RExpr {
    return super.genName(when {
      n.startsWith("phase0.") -> n.substring("phase0.".length)
      n.startsWith("ssz.") -> n.substring("ssz.".length)
      n.startsWith("pylib.") -> n.substring("pylib.".length)
      else -> n
    })
  }

  fun genNum(n: Num): String {
    return when(n.n) {
      //is BigInteger -> "\"" + n.n.toString() + "\".toBigInteger()"
      is Int -> n.n.toString()
      else -> fail("not supported yet")
    }
  }

  fun genNameConstant(e: NameConstant): RExpr = if (e.value == null)
    atomic("()") else atomic(e.value.toString())

  override fun applyMapFilter(e: RExpr, a: String, l: String): RExpr {
    val n = if (a == "map") "map_" else a
    return RInfix("", atomic(n), RSeq("(", ")", ",", listOf(atomic(l), e)))
  }

  override fun genLet(e: Let, typer: ExprTyper): RExpr {
    var ctx = typer
    val bindings = e.bindings.map {
      val va = genVarAssignment(false, mkName(it.arg!!), null, it.value, ctx)
      ctx = ctx.updated(listOf(it.arg to ctx[it.value]))
      va
    }
    return RPExpr(MIN_PRIO, bindings.joinToString(" ") + " " + render(genExpr(e.value, ctx)))
  }

  override fun genLambda(args: List<String>, preBody: List<String>, body: RExpr): String {
    //val args = args.mapIndexed { i, a -> if (a == "_") "_$i" else a }
    val begin = "(" + args.joinToString(", ") + ") => "
    return "(" + (if (preBody.isEmpty())
      begin + render(body)
    else
      begin + preBody.joinToString(" ") + " ${render(body)}") + ")"
  }

  override fun genLambdaArg(a: LVInfo): String = when(a) {
    is LVName -> a.name
    else -> fail("unsupported $a")
  }

  override fun genIfExpr(t: RExpr, b: RExpr, e: RExpr): RExpr = atomic("if (${render(t)}) then ${render(b)} else ${render(e)}")

  override fun genCmpOp(a: RExpr, op: ECmpOp, b: RExpr): RExpr {
    return if (op == ECmpOp.Is && render(b) == "null")
      RInfix("==", a, b)
    else if (op == ECmpOp.IsNot && render(b) == "null")
      RInfix("!=", a, b )
    else if (op == ECmpOp.In)
      genAttrCall(b, "contains", listOf(render(a)))
    else if (op == ECmpOp.NotIn)
      genUnaryOp(genAttrCall(b, "contains", listOf(render(a))), EUnaryOp.Not)
    else
      RInfix(genCmpop(op), a, b)
  }

  override fun genIndexSubscript(e: LVInfo, typer: ExprTyper): RExpr = toLoad(e, typer)

  override fun genBinOp(a: RExpr, o: EBinOp, b: RExpr): RExpr {
    val op = genOperator(o)
    return if (op[0].isLetter()) {
      atomic("$op(${render(a)}, ${render(b)})")
    } else {
      RInfix(op, a, b)
    }
  }

  override fun genBoolOp(exprs: List<RExpr>, op: EBoolOp): RExpr = RInfix(genTBoolop(op), exprs)

  override fun genUnaryOp(a: RExpr, op: EUnaryOp): RExpr = RPrefix(genUnaryop(op), a)

  override fun genOperator(o: EBinOp): String {
    return when (o) {
      EBinOp.Add -> "+"
      EBinOp.Sub -> "-"
      EBinOp.Mult -> "*"
      EBinOp.MatMult -> fail("@ is not supported")
      EBinOp.Div -> fail("/ is not supported")
      EBinOp.Mod -> "%"
      EBinOp.Pow -> "pow"
      EBinOp.LShift -> "shl"
      EBinOp.RShift -> "shr"
      EBinOp.BitOr -> "or"
      EBinOp.BitXor -> "xor"
      EBinOp.BitAnd -> "and"
      EBinOp.FloorDiv -> "/"
    }
  }

  fun genPyDict(elts: List<Pair<RExpr, RExpr>>): RExpr = atomic("Dict_new(map[" + elts.map { "${render(it.first)} := ${render(it.second)}" }.joinToString(", ") + "])")

  fun genPyList(elts: List<RExpr>): RExpr = atomic("PyList_new([" + elts.joinToString(", ") { render(it) } + "])")

  fun genTuple(elts: List<RExpr>): RExpr {
    return atomic("(" + elts.joinToString(", ") { render(it) } + ")")
  }

  override fun genAttrLoad(e: RExpr, a: identifier, valType: RTType, exprType: RTType): RExpr {
    val res = atomic("${render(e)}.$a")
    return when {
      valType is NamedType && a in (dafnyFreshAttrs[valType.name] ?: emptySet()) -> {
        if (exprType !is NamedType) TODO()
        when(exprType.name) {
          "ssz.List" -> genFunCall(atomic("PyList_new"), listOf(render(res)))
          else -> TODO()
        }
      }
      else -> res
    }
  }

  fun toLoad(lhs: TExpr): TExpr = when(lhs) {
    is Name -> lhs.copy(ctx = ExprContext.Load)
    is Attribute -> lhs.copy(ctx = ExprContext.Load)
    is Subscript -> lhs.copy(ctx = ExprContext.Load)
    else -> TODO("$lhs")
    /*is LVTuple -> fail("not supported $lhs")*/
  }

  fun LVInfo.toTExpr(): TExpr = when(this) {
    is LVName -> mkName(this.name)
    is LVAttr -> mkAttribute(this.e.toTExpr(), this.attr)
    else -> TODO()
  }
  fun toLoad(lhs: LVInfo, typer: ExprTyper): RExpr = when(lhs) {
    is LVName -> atomic(lhs.name)
    is LVAttr -> genAttrLoad(toLoad(lhs.e, typer), lhs.attr, typer[lhs.e.toTExpr()].asType(), typer[lhs.toTExpr()].asType())
    is LVIndex -> atomic(render(toLoad(lhs.e, typer)) + ".get(" + render(lhs.index) + ")")
    is LVSlice -> atomic(render(toLoad(lhs.e, typer)) + "[" + lhs.start?.let { render(it) } + ".." + lhs.upper?.let { render(it) } + "]")
    else -> TODO("$lhs")
    /*is LVTuple -> fail("not supported $lhs")*/
  }

  override fun genFunHandle(e: TExpr, type: Sort, fh: FunSignature, argRefs: List<ArgRef>, args: List<RExpr>, kwdArgs: List<Pair<String, RExpr>>, typer: ExprTyper): Pair<RExpr, List<String>> {
    val resArgs = argRefs.map { ar ->
      render(when (ar) {
        is PositionalRef -> args[ar.idx]
        is KeywordRef -> kwdArgs[ar.idx].second
        is DefaultRef -> genExpr(fh.defaults[ar.idx], typer)
      })
    }
    return if (type is Clazz) {
      val t = type.toInstance()
      val fh = RLit(typeToStr(t.copy(name = t.name + "_new")))
      val resArgs = if (isSubType_new(t, TPyBytes) && resArgs.isNotEmpty()) {
        if (resArgs.size > 1) fail()
        val resArg = if (resArgs[0].startsWith("\""))
          resArgs[0].substring(1, resArgs[0].length-1)
        else resArgs[0]
        listOf(resArg)
      } else resArgs
      fh to resArgs
    } else if (e is Attribute) {
      genAttrBase(genExpr(e.value, typer), e.attr) to resArgs
    } else if (e is CTV && e.v is FuncInst) {
      if (e.v.name == "pylib.copy") {
        if (resArgs.size != 1) fail()
        genAttrBase(atomic(resArgs[0]), "copy") to emptyList()
      } else {
        val funcName = when {
          e.v.name == "<assert>" -> "pyassert"
          e.v.name == "<check>" -> fail()
          e.v.name == "<Result>::new" -> "Result"
          e.v.name == "pylib.list" -> "pylist"
          e.v.name == "pylib.set" -> "pyset"
          e.v.name =="pylib.dict" -> if (resArgs.isEmpty()) "Dict_new" else "pydict"
          else -> {
            if (e.v.name.endsWith("::new")) {
              val className = e.v.name.substring(0, e.v.name.length - "::new".length)
              val cn = (genName(className) as RPExpr).expr
              when {
                dafnyClassKinds[className] == DafnyClassKind.Class ->
                  "new $cn.Init"
                dafnyClassKinds[className] == DafnyClassKind.Primitive ->
                  "${cn}_new"
                else -> cn
              }
            } else {
              (genName(e.v.name) as RPExpr).expr
            }
          }
        }
        RLit(funcName) to resArgs
      }
    } else {
      genExpr(e, typer) to resArgs
    }
  }

  override fun genCall(e: Call, typer: ExprTyper, type: Sort): RInfix {
    val res = super.genCall(e, typer, type)
    return if (e.func is Attribute && e.args.isEmpty() && e.keywords.isEmpty()) {
      val valType = typer[e.func.value].asType()
      val exprType = (type as FunType).retType
      when {
        valType is NamedType && (e.func.attr + "()") in (dafnyFreshAttrs[valType.name] ?: emptySet()) -> {
          wrapFreshValue(exprType, res)
        }
        else -> res
      }
    } else if (e.func is CTV && e.func.v is FuncInst && findFuncDescr(e.func.v.name)?.memoryModel == MemoryModel.FRESH) {
      val exprType = (type as FunType).retType
      wrapFreshValue(exprType, res)
    } else res
  }

  private fun wrapFreshValue(exprType: RTType, res: RInfix): RInfix {
    if (exprType !is NamedType) TODO()
    return when (exprType.name) {
      "ssz.List" -> genFunCall(atomic("PyList_new"), listOf(render(res)))
      "pylib.Set" -> genFunCall(atomic("Set_new"), listOf(render(res)))
      "pylib.Sequence" -> genFunCall(atomic("Sequence_new"), listOf(render(res)))
      else -> TODO()
    }
  }

  override fun genReturn(v: String?, t: RTType): String {
    return "return" + (v?.let { " $it" } ?: "") + ";"
  }

  fun genCheckedVarAssignment(isVar: Boolean?, vn: String, value: RExpr): String {
    return (if (isVar != null) "var " else "") + "$vn :- " + render(value) + ";"
  }

  override fun genVarAssignment(isVar: Boolean?, lv: TExpr, typ: String?, value: TExpr?, typer: ExprTyper): String {
    if (value != null && isExceptionCheck(value)) {
      (((value as Call).func as CTV).v as FuncInst)
      if (lv !is Name) fail()
      if (value.args.size != 1 || value.keywords.isNotEmpty()) fail()
      //if (isVar == null || isVar == true) fail()
      return genCheckedVarAssignment(isVar, lv.id, genExpr(value.args[0], typer))
    }

    val value = value?.let { render(genExpr(it, typer)) }
    return when {
      lv is Name -> {
        val type = if (typ == null) "" else ": $typ"
        val initV = value?.let { " := $it" } ?: ""
        if (isVar == null) "${lv.id}$type$initV;" else "var ${lv.id}$type$initV;"
      }
      lv is Attribute -> {
        render(genExpr(toLoad(lv.value), typer)) + "." + lv.attr + " := " + value + ";"
        //genVarAssignment(null, lv.e, null, render(toLoad(lv.e)) + ".(" + lv.attr + " := " + value + ")")
      }
      lv is Subscript && lv.slice is Index -> {
        render(genExpr(toLoad(lv.value), typer)) + ".set_value(" + render(genExpr(lv.slice.value, typer)) + ", " + value + ");"
        // //genVarAssignment(null, lv.e,null, render(toLoad(lv.e)) + "[" + render(lv.index) + " := " + value + "]")
      }
      lv is Subscript && lv.slice is Slice -> {
        TODO()
        //genVarAssignment(null,lv.e, null, render(toLoad(lv.e)) + "[" + lv.start?.let { render(it) } + ".." + lv.upper?.let { render(it) } + " := " + value + "]")
      }
      else -> TODO("$lv")
/*
        is LVTuple -> fail("not supported $lv")
*/
    }
  }

  override fun genAugAssignment(lhs: TExpr, op: EBinOp, rhs: TExpr, typer: ExprTyper): String = genVarAssignment(
      null, lhs, null, BinOp(lhs, op, rhs), typer)

  override fun genExprStmt(e: String): String = e + ";"

  override fun genAssertStmt(e: String, m: String?): String = ":- pyassert(" + e + (m?.let { ", " + it } ?: "") + ");"

  override fun genForHead(target: TExpr, iter: TExpr, typer: ExprTyper): String {
    TODO()
//    val i = freshName("i")
//    val coll = freshName("coll")
//    val itInit = genVarAssignment(false, LVName(i), null, Num(0), typer)
//    val collInit = genVarAssignment(false, LVName(coll), null, mkName(i), typer)
//    val variant = "  decreases |$coll| - i"
//    val whileHead = genWhileHead("$i < |$coll|", variant)
//
//    val iter = genVarAssignment(false, t, null, mkSubscript(mkName(coll), Index(mkName(i))), typer)
//    return "$itInit\n$collInit\n$whileHead\n$iter"
  }

  override fun genWhileHead(t: String): String = genWhileHead(t, null)
  fun genWhileHead(t: String, variant: String? = null): String {
    if (variant == null) {
      return "while $t {"
    } else {
      return "while $t\n$variant\n{"
    }
  }

  override fun genIfHead(t: String): String = "if ${t} {"

  fun toShortName(tn: String): String {
    val idx = tn.lastIndexOf(".")
    return if (idx == -1) tn else tn.substring(idx + 1)
  }
  override fun typeToStr(t: RTType): String = when {
    t is NamedType && (t.name == "None" || t.name == "pylib.None") -> "()"
    t is NamedType && t.name == "<Outcome>" -> typeToStr(t.copy(name = "Outcome"))
    t is NamedType && t.name == "Tuple" -> t.tParams.map { typeToStr(it) }.joinToString(", ", "(", ")")
    t is NamedType -> {
      val clsName = toShortName(t.name)
      if (t.tParams.isEmpty()) clsName else clsName + "<" + t.tParams.map { typeToStr(it) }.joinToString(",") + ">"
    }
    else -> TODO()
  }

  override val destructLValTuples = true

  override fun genComment(comment: String) {
    print("/*")
    print(comment.split("\\n").joinToString("\n"))
    println("*/")
  }

  override fun genFunBegin(n: String, args_: List<Pair<Arg, String?>>, typ: String, f: FunctionDef, typer: ExprTyper): String {
    val nonLocalVars = args_.map { it.first.arg }.filter {
      isDafnyClassKind(mkName(it), typer, setOf(DafnyClassKind.Class, DafnyClassKind.Datatype))
    }.toSet()
    _exprGen = DafnyExprGen(::genNativeType, true, nonLocalVars)
    val args = args_.joinToString(", ") { genArg(it.first) }
    val name = if (f.name.contains(".")) {
      if (f.name.startsWith("phase0.")) f.name.substring("phase0.".length)
      else TODO()
    } else f.name

    val retType = (f.returns!! as CTV).v as ClassVal

    val pfDescr = fcFuncsDescr[name]!!
    val framing = pfDescr.framing?.let {
      val kind = if (pfDescr.function) "reads" else "modifies"
      listOf("$kind $it")
    } ?: emptyList()
    val preconditions = pfDescr.precondition?.let { listOf("requires $it") } ?: emptyList()
    val midSection = if (pfDescr.function) {
      val funcVersion = MethodToFuncTransformer(f, typer).transform()
      val expr = render(exprGen.with(insideMethod = false).genExpr(funcVersion, typer))
      listOf("{", "  $expr", "} by method {")
    } else if (retType.name == "<Outcome>" && retType.tParams[0] is ClassVal && retType.tParams[0].asClassVal().name == "pylib.None") {
      listOf("{", "  ret_ := Result(());")
    } else if (retType.name == "pylib.None") {
      listOf("{", "  ret_ := ();")
    } else listOf("{")
    val methHead = genDafnyFunction(n, args, typ).plus(framing + preconditions + midSection)
    return methHead.joinToString("\n")
  }

  override fun genToplevel(n: String, e: TExpr) {
    println("const $n := " + render(genExpr(e, TypeResolver.topLevelTyper)) + ";")
  }

  override fun getDefaultValueForBase(base: String): String? = run {
    when(base) {
      "boolean" -> "false"
      "uint8" -> "0"
      "uint64" -> "0"
      else -> initializers[base] ?: "${base}()"
    }
  }

  override fun genOptionalType(typ: String): String {
    TODO("Not yet implemented")
  }

  override fun genClsField(name: String, typ: String, init: String?): String {
    return "  var ${name}: $typ;"
  }

  val initializers = mutableMapOf<String,String>()
  override fun genValueClass(name: String, base: TExpr) {
    val baseType = parseType(exprTyper, base)
    val baseName = typeToStr(baseType)
    println("type $name = $baseName")
    println("function method ${name}_(x: $baseName): $name { x }")
    println("const ${name}_default := ${name}_(0);")
    initializers[name] = "${name}_default"
  }

  override fun genContainerClass(name: String, base: TExpr, fields: List<Triple<String, String, String?>>) {
    println("class $name {")
    println("  constructor() {")
    fields.forEach { (name, type, init) ->
      println("    $name := $init;")
    }
    println("  }")
    println(fields.joinToString("\n") {
      genClsField(it.first, it.second, it.third!!)
    })
    println("}")
    initializers[name] = "new $name()"
  }

  context(FuncGenCtx) override fun genStmt(s: Stmt): List<String> {
    val res = super.genStmt(s)
    when {
      s is VarDeclaration && s.value != null -> {
        if (s.target.size != 1) TODO()
        val ctx = analyses.varTypings[s]!!
        val exprTypes = exprTyper.updated(ctx)
        if (exprGen.canReferToClass(s.value, exprTypes)) {
          _exprGen = exprGen.with(nonlocalVars = exprGen.nonlocalVars.union(s.target.map { it.id }))
        }
      }
      s is Assign && s.target is Name -> {
        val ctx = analyses.varTypings[s]!!
        val exprTypes = exprTyper.updated(ctx)
        if (exprGen.canReferToClass(s.value, exprTypes))
          TODO()
      }
      s is AnnAssign ->
        TODO()
      else -> {}
    }
    return res
  }

}