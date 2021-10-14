package onotole

class DafnyGen(currPkg: String, importedPkgs: Set<String>): BaseGen(currPkg, importedPkgs) {
  override val parenthesesAroundLambda = true

  override fun genNum(n: Num): String {
    return when(n.n) {
      //is BigInteger -> "\"" + n.n.toString() + "\".toBigInteger()"
      is Int -> n.n.toString()
      else -> fail("not supported yet")
    }
  }

  override fun genNameConstant(e: NameConstant): RExpr = atomic(e.value.toString())

  override fun applyMapFilter(e: RExpr, a: String, l: String): RExpr {
    val n = if (a == "map") "map_" else a
    return RInfix("", atomic(n), RSeq("(", ")", ",", listOf(atomic(l), e)))
  }

  override fun genLambda(args: List<String>, preBody: List<String>, body: RExpr): String {
    //val args = args.mapIndexed { i, a -> if (a == "_") "_$i" else a }
    val begin = "(" + args.joinToString(", ") + ") => "
    return if (preBody.isEmpty())
      begin + render(body)
    else
      begin + preBody.joinToString(" ") + " ${render(body)}"
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
    else
      RInfix(genCmpop(op), a, b)
  }

  override fun genIndexSubscript(e: LVInfo, typ: RTType): RExpr = toLoad(e)

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

  override fun genPyDict(elts: List<Pair<RExpr, RExpr>>): RExpr = atomic("PyDict(" + elts.map { "${render(it.first)} to ${render(it.second)}" }.joinToString(", ") + ")")

  override fun genPyList(elts: List<RExpr>): RExpr = atomic("PyList(" + elts.joinToString(", ") { render(it) } + ")")

  override fun genTuple(elts: List<RExpr>): RExpr {
    val tupleName = if (elts.size == 0)
      "Unit"
    else if (elts.size == 2)
      "Pair"
    else if (elts.size == 3)
      "Triple"
    else
      fail("too many tuple elements")
    return atomic(tupleName + "(" + elts.joinToString(", ") { render(it) } + ")")
  }

  override fun genAttrLoad(e: RExpr, a: identifier, isStatic: Boolean): RExpr = atomic("${render(e)}.$a")

  fun toLoad(lhs: LVInfo): RExpr = when(lhs) {
    is LVName -> atomic(lhs.name)
    is LVAttr -> genAttrLoad(toLoad(lhs.e), lhs.attr, false)
    is LVIndex -> atomic(render(toLoad(lhs.e)) + "[" + render(lhs.index) + "]")
    is LVSlice -> atomic(render(toLoad(lhs.e)) + "[" + lhs.start?.let { render(it) } + ".." + lhs.upper?.let { render(it) } + "]")
    else -> TODO("$lhs")
    /*is LVTuple -> fail("not supported $lhs")*/
  }

  override fun genFunHandle(e: TExpr, type: Sort, fh: FunSignature, argRefs: List<ArgRef>, args: List<RExpr>, kwdArgs: List<Pair<String, RExpr>>, exprTypes: ExprTypes): Pair<RExpr, List<String>> {
    val resArgs = argRefs.map { ar ->
      render(when (ar) {
        is PositionalRef -> args[ar.idx]
        is KeywordRef -> kwdArgs[ar.idx].second
        is DefaultRef -> genExpr(fh.defaults[ar.idx], exprTypes)
      })
    }
    return if (type is Clazz) {
      val t = type.toInstance()
      val fh = RLit(typeToStr(t) + "_")
      val resArgs = if (isSubType(t, TPyBytes) && resArgs.isNotEmpty()) {
        if (resArgs.size > 1) fail()
        val resArg = if (resArgs[0].startsWith("\""))
          resArgs[0].substring(1, resArgs[0].length-1)
        else resArgs[0]
        listOf(resArg)
      } else resArgs
      fh to resArgs
    } else if (e is Attribute) {
      genAttrBase(genExpr(e.value, exprTypes), e.attr) to resArgs
    } else {
      genExpr(e, exprTypes) to resArgs
    }
  }


  override fun genReturn(v: String?, t: RTType): String {
    return "return" + (v?.let {" " + it} ?: "") + ";"
  }

  override fun genVarAssignment(isVar: Boolean?, lv: LVInfo, typ: String?, value: String?): String = when(lv) {
    is LVName -> {
      val type = if (typ == null) "" else ": $typ"
      val initV = value?.let { " := $it" } ?: ""
      if (isVar == null) "${lv.name}$type$initV;" else "var ${lv.name}$type$initV;"
    }
    is LVIndex -> {
      genVarAssignment(null, lv.e,null, render(toLoad(lv.e)) + "[" + render(lv.index) + " := " + value + "]")
    }
    is LVAttr -> {
      genVarAssignment(null, lv.e, null, render(toLoad(lv.e)) + ".(" + lv.attr + " := " + value + ")")
    }
    is LVSlice -> {
      genVarAssignment(null,lv.e, null, render(toLoad(lv.e)) + "[" + lv.start?.let { render(it) } + ".." + lv.upper?.let { render(it) } + " := " + value + "]")
    }
    else -> TODO("$lv")
/*
        is LVTuple -> fail("not supported $lv")
*/
  }

  override fun genAugAssignment(lhs: LVInfo, op: EBinOp, rhs: RExpr): String = genVarAssignment(
      null, lhs, null, render(genBinOp(toLoad(lhs), op, rhs)))

  override fun genExprStmt(e: String): String = e + ";"

  override fun genAssertStmt(e: String, m: String?): String = "assert(" + e + (m?.let { ", " + it } ?: "") + ");"

  override fun genForHead(t: LVInfo, i: String): String {
    val i = freshName("i")
    val coll = freshName("coll")
    val itInit = genVarAssignment(false, LVName(i), null, "0")
    val collInit = genVarAssignment(false, LVName(coll), null, i)
    val variant = "  decreases |$coll| - i"
    val whileHead = genWhileHead("$i < |$coll|", variant)
    val iter = genVarAssignment(false, t, null,
        render(genIndexSubscript(LVIndex(LVName(coll), TPyObject, atomic(i)), TPyObject)))
    return "$itInit\n$collInit\n$whileHead\n$iter"
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
    t is NamedType && t.name == "Tuple" -> t.tParams.map { typeToStr(it) }.joinToString(", ", "(", ")")
    t is NamedType -> if (t.tParams.isEmpty()) toShortName(t.name) else t.name + "<" + t.tParams.map { typeToStr(it) }.joinToString(",") + ">"
    else -> TODO()
  }

  override val destructLValTuples = true

  override fun genComment(comment: String) {
    print("/*")
    print(comment.split("\\n").joinToString("\n"))
    println("*/")
  }

  override fun genFunBegin(n: String, args: List<Pair<Arg, String?>>, typ: String): String {
    val args = args.map { genArg(it.first) }.joinToString(", ")
    return "function method $n($args): $typ {"
  }

  override fun genToplevel(n: String, e: TExpr) {
    println("const $n := " + render(genExpr(e, TypeResolver.topLevelTyper)) + ";")
  }

  override fun getDefaultValueForBase(base: String): String? = run {
    when(base) {
      "boolean" -> "false"
      "uint8" -> "0"
      "uint64" -> ""
      else -> "${base}()"
    }
  }

  override fun genOptionalType(typ: String): String {
    TODO("Not yet implemented")
  }

  override fun genClsField(name: String, typ: String, init: String?): String {
    return "  ${name}: $typ"
  }

  override fun genValueClass(name: String, base: TExpr) {
    val baseType = parseType(exprTypes, base)
    val baseName = typeToStr(baseType)
    println("type $name = $baseName")
    println("function method ${name}_(x: $baseName): $name { x }")
  }

  override fun genContainerClass(name: String, base: TExpr, fields: List<Triple<String, String, String?>>) {
    println("datatype $name = $name(")
    println(fields.joinToString(",\n") {
      genClsField(it.first, it.second, it.third)
    })
    println(")")
  }

}