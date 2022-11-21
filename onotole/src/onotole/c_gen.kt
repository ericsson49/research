package onotole

import java.math.BigInteger

//class CGen(currPkg: String, importedPkgs: Set<String>): BaseGen(currPkg, importedPkgs) {
//  override fun applyMapFilter(e: RExpr, a: String, l: String): RExpr {
//    TODO("Not yet implemented")
//  }
//
//  override val parenthesesAroundLambda: Boolean
//    get() = TODO("Not yet implemented")
//
//  override fun genNum(n: Num): String {
//    return when(n.n) {
//      is BigInteger -> "mk_pyint(\"${n.n}\")"
//      is Int -> "mk_pyint(${n.n}L)"
//      else -> fail("not supported yet")
//    }
//  }
//
//  override fun genNameConstant(e: NameConstant): RExpr {
//    TODO("Not yet implemented")
//  }
//
//  override fun genLambda(args: List<String>, preBody: List<String>, body: RExpr): String {
//    TODO("Not yet implemented")
//  }
//
//  override fun genLambdaArg(a: LVInfo): String {
//    TODO("Not yet implemented")
//  }
//
//  override fun genIfExpr(t: RExpr, b: RExpr, e: RExpr): RExpr {
//    TODO("Not yet implemented")
//  }
//
//  override fun genCmpop(o: ECmpOp): String {
//    return when (o) {
//      ECmpOp.Eq -> "=="
//      ECmpOp.NotEq -> "!="
//      ECmpOp.Lt -> "<"
//      ECmpOp.LtE -> "<="
//      ECmpOp.Gt -> ">"
//      ECmpOp.GtE -> ">="
//      ECmpOp.Is -> TODO()
//      ECmpOp.IsNot -> TODO()
//      ECmpOp.In -> TODO()
//      ECmpOp.NotIn -> TODO()
//    }
//  }
//
//  override fun genCmpOp(a: RExpr, op: ECmpOp, b: RExpr): RExpr {
//    return RInfix(genCmpop(op), a, b)
//  }
//
//  override fun genIndexSubscript(e: LVInfo, typ: RTType): RExpr {
//    TODO("Not yet implemented")
//  }
//
//  override fun genOperator(o: EBinOp): String {
//    return when (o) {
//      EBinOp.Add -> "+"
//      EBinOp.Sub -> "-"
//      EBinOp.Mult -> "*"
//      EBinOp.MatMult -> fail("@ is not supported")
//      EBinOp.Div -> fail("/ is not supported")
//      EBinOp.Mod -> "%"
//      EBinOp.Pow -> TODO() //"pow"
//      EBinOp.LShift -> "<<"
//      EBinOp.RShift -> ">>"
//      EBinOp.BitOr -> "|"
//      EBinOp.BitXor -> "^"
//      EBinOp.BitAnd -> "&"
//      EBinOp.FloorDiv -> "/"
//    }
//  }
//
//  override fun genBinOp(a: RExpr, op: EBinOp, b: RExpr): RExpr = when(op) {
//    EBinOp.Pow -> genFunCall(RLit("pow"), listOf(a, b).map { render(it) })
//    else -> RInfix(genOperator(op), a, b)
//  }
//
//  override fun genBoolOp(exprs: List<RExpr>, op: EBoolOp): RExpr {
//    TODO("Not yet implemented")
//  }
//
//  override fun genUnaryOp(a: RExpr, op: EUnaryOp): RExpr {
//    TODO("Not yet implemented")
//  }
//
//  override fun genPyDict(elts: List<Pair<RExpr, RExpr>>): RExpr {
//    TODO("Not yet implemented")
//  }
//
//  override fun genPyList(elts: List<RExpr>): RExpr {
//    TODO("Not yet implemented")
//  }
//
//  override fun genTuple(elts: List<RExpr>): RExpr {
//    TODO("Not yet implemented")
//  }
//
//  override fun genAttrLoad(e: RExpr, a: identifier, isStatic: Boolean): RExpr {
//    TODO("Not yet implemented")
//  }
//
//  override fun genFunHandle(e: TExpr, type: Sort, fh: FunSignature, argRefs: List<ArgRef>, args: List<RExpr>, kwdArgs: List<Pair<String,RExpr>>, exprTypes: ExprTyper): Pair<RExpr, List<String>> {
//    val resArgs = argRefs.map { ar ->
//      render(when (ar) {
//        is PositionalRef -> args[ar.idx]
//        is KeywordRef -> kwdArgs[ar.idx].second
//        is DefaultRef -> genExpr(fh.defaults[ar.idx], exprTypes)
//      })
//    }
//    val funExpr = if (type is Clazz) {
//      RLit("mk_" + typeToStr(type.toInstance()))
//    } else if (e is Attribute) {
//      genAttrBase(genExpr(e.value, exprTypes), e.attr)
//    } else {
//      genExpr(e, exprTypes)
//    }
//    return funExpr to resArgs
//  }
//
//  override fun genReturn(v: String?, t: RTType): String {
//    return "return" + (v?.let { " $it" } ?: "") + ";"
//  }
//
//  override fun genVarAssignment(isVar: Boolean?, lv: LVInfo, typ: String?, value: TExpr?): String = when(lv) {
//    is LVName -> {
//      val v = if (isVar != null)
//        typ ?: "undef"
//      else ""
//      val initV = value?.let { " = $it" } ?: ""
//      v + " " + lv.name + initV + ";"
//    }
//    else -> TODO()
//  }
//
//  override fun genAugAssignment(lhs: TExpr, op: EBinOp, rhs: RExpr): String {
//    TODO("Not yet implemented")
//  }
//
//  override fun genExprStmt(e: String): String {
//    TODO("Not yet implemented")
//  }
//
//  override fun genAssertStmt(e: String, m: String?): String {
//    TODO("Not yet implemented")
//  }
//
//  override fun genForHead(t: LVInfo, i: String): String {
//    TODO("Not yet implemented")
//  }
//
//  override fun genWhileHead(t: String): String {
//    return "while($t) {"
//  }
//
//  override fun genIfHead(t: String): String {
//    TODO("Not yet implemented")
//  }
//
//  override fun typeToStr(t: RTType): String  = when(t) {
//    is NamedType -> {
//      val parts = t.name.split(".")
//      val name = if (parts.size == 2 && (parts[0] == "phase0" || parts[0] == "ssz")) parts[1] else t.name
//      if (t.tParams.isEmpty())
//        name
//      else
//        listOf(name).plus(t.tParams.map { typeToStr(it) }).joinToString("_")
//    }
//    else -> TODO()
//  }
//
//  override val destructLValTuples = true
//
//  override fun genComment(comment: String) {
//    println("  /*")
//    println(comment.split("\\n").joinToString("\n") { if (it.isNotBlank()) "  $it" else it })
//    println("  */")
//  }
//
//  override fun genFunBegin(n: String, args: List<Pair<Arg, String?>>, typ: String): String {
//    val argStrings = args.joinToString(", ") { genNativeType(it.first.annotation!!) + " " + it.first.arg }
//    return "$typ $n($argStrings) {"
//  }
//
//  override fun genToplevel(n: String, e: TExpr) {
//    val exprTypes = TypeResolver.topLevelTyper
//    val type = exprTypes[e].asType()
//    val typeStr = typeToStr(type)
//    val value = render(genExpr(e, exprTypes))
//    println("const $typeStr $n = $value;")
//  }
//
//  override fun getDefaultValueForBase(base: String): String? {
//    return null
//  }
//
//  override fun genOptionalType(typ: String): String {
//    TODO("Not yet implemented")
//  }
//
//  override fun genClsField(name: String, typ: String, init: String?): String {
//    return "  $typ $name;"
//  }
//
//  override fun genValueClass(name: String, base: TExpr) {
//    println("#define $name ${genNativeType(base)}")
//  }
//
//  override fun genContainerClass(name: String, base: TExpr, fields: List<Triple<String, String, String?>>) {
//    val declaredFields = fields.map { genClsField(it.first, it.second, it.third) }
//    println("typedef struct {")
//    declaredFields.forEach {
//      println(it)
//    }
//    println("} $name;")
//  }
//
//}