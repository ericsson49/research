class RustGen: BaseGen() {
  override val destructLValTuples = false

  override fun typeToStr(t: RTType): String = when(t) {
    is NamedType -> if (t.tParams.isEmpty()) t.name else t.name + "<" + t.tParams.map { typeToStr(it) }.joinToString(",") + ">"
    else -> TODO()
  }

  override val parenthesesAroundLambda = True
  override fun genNameConstant(e: NameConstant): PExpr = atomic(e.value.toString())
  override fun genNum(n: Num): String {
    return when(n.n) {
      is Int -> n.n.toString()
      else -> fail("not supported yet")
    }
  }

  override fun genLambdaArg(a: LVInfo): String {
    TODO("Not yet implemented")
  }
  override fun genLambda(args: List<String>, preBody: List<String>, body: String): String
      = "|" + args.joinToString(", ") + "| { " + body + " }"
  override fun genIfExpr(t: PExpr, b: PExpr, e: PExpr): PExpr = PExpr(MIN_PRIO, "if $t { $b } else { $e }")
  override fun genCmpOp(a: PExpr, op: ECmpOp, b: PExpr): PExpr {
    return atomic(if (op == ECmpOp.Is && b.expr == "null")
      "($a == null)"
    else if (op == ECmpOp.IsNot && b.expr == "null")
      "($a != null)"
    else if (op == ECmpOp.In)
      "$b.contains($a)"
    else if (op == ECmpOp.NotIn)
      "!($b.contains($a))"
    else
      "(" + a + " " + genCmpop(op) + " " + b + ")")
  }
  override fun genBoolOp(exprs: List<PExpr>, op: EBoolOp): PExpr = format_commutative_op_expr(exprs, genTBoolop(op))
  override fun genBinOp(l: PExpr, o: EBinOp, r: PExpr): PExpr = when(o) {
    EBinOp.Pow -> atomic("${l}.pow($r)")
    else -> atomic("$l ${genOperator(o)} $r")
  }
  override fun genUnaryOp(a: PExpr, op: EUnaryOp) = format_unop_expr(a, genUnaryop(op))
  override fun genOperator(o: EBinOp): String {
    return when (o) {
      EBinOp.Add -> "+"
      EBinOp.Sub -> "-"
      EBinOp.Mult -> "*"
      EBinOp.MatMult -> fail("@ is not supported")
      EBinOp.Div -> fail("/ is not supported")
      EBinOp.Mod -> "%"
      EBinOp.Pow -> "pow"
      EBinOp.LShift -> ">>"
      EBinOp.RShift -> "<<"
      EBinOp.BitOr -> "|"
      EBinOp.BitXor -> "^"
      EBinOp.BitAnd -> "&"
      EBinOp.FloorDiv -> "/"
    }
  }
  override fun genIndexSubscript(e: LVInfo, typ: RTType): PExpr =  atomic("$e[??]")
  override fun genPyList(elts: List<String>) = atomic("py_list![" + elts.joinToString(", ") + "]")
  override fun genPyDict(elts: List<Pair<String,String>>): String = "py_dict![" + elts.map { "(${it.first}, ${it.second})" }.joinToString(", ") + "]"
  override fun genTuple(elts: List<String>): PExpr = atomic("(" + elts.joinToString(", ") + ")")

  override fun genFunHandle(e: TExpr, type: Sort, fh: FunSignature, argRefs: List<ArgRef>, args: List<String>, kwdArgs: List<Pair<String,String>>, exprTypes: ExprTypes): Pair<String, List<String>> {
    var resArgs = argRefs.map { ar ->
      when(ar) {
        is PositionalRef -> args[ar.idx]
        is KeywordRef -> kwdArgs[ar.idx].second
        is DefaultRef -> genExpr(fh.defaults[ar.idx], exprTypes).expr
      }
    }
    return if (type is Clazz) {
      Pair("new " + typeToStr(type.toInstance()), resArgs)
    } else if (e is Attribute) {
      Pair(genAttrBase(genExpr(e.value, exprTypes), e.attr).expr, resArgs)
    } else {
      Pair(genExpr(e, exprTypes).expr, resArgs)
    }
  }



  override fun genReturn(v: String?, t: RTType) = "return" + (v?.let { " $it" } ?: "") + ";"
  override fun genAttrLoad(e: PExpr, a: identifier) = genAttrBase(e, a)
  override fun genVarAssignment(isVar: Boolean?, name: LVInfo, typ: String?, value: String?): String {
    val v = if (isVar != null) {
      (if (isVar) "let mut" else "let") + " "
    } else ""
    val typV = typ?.let { ": $it" } ?: ""
    val initV = value?.let { " = $it" }
    return "$v$name$typV$initV;"
  }
  override fun genAugAssignment(lhs: LVInfo, op: EBinOp, rhs: PExpr): String = "$lhs $op= $rhs;"
  override fun genExprStmt(e: String): String = "$e;"
  override fun genAssertStmt(e: String, m: String?): String = "assert!(" + e + (m?.let { ", $it" } ?: "") + ");";
  override fun genForHead(t: LVInfo, i: String): String = "for $t in $i {"
  override fun genWhileHead(t: String): String = "while $t {"
  override fun genIfHead(t: String): String = "if $t {"


  override fun genOptionalType(typ: String): String = "Option<$typ>"


  override fun genFunBegin(n: String, args: List<Pair<Arg, String?>>, typ: String): String {
    val argsStr = args.map {
      val arg = it.first
      val default = it.second
      val typS = if (arg.annotation != null) {
        val typ = genNativeType(arg.annotation)
        ": " + if (default != null) { genOptionalType(typ) } else { typ }
      } else {
        ""
      }
      arg.arg + typS
    }.joinToString(", ")
    return "fn $n($argsStr) -> $typ {"
  }

  override fun genComment(comment: String) {
    for (c in comment.split("\\n")) {
      val s = c.strip() ?: ""
      if (!s.isBlank())
        println("/// $s")
    }
  }

  override fun getDefaultValueForBase(base: String): String? = run {
    when(base) {
      "boolean" -> "false"
      "uint8" -> "0u8"
      "uint64" -> "0u64"
      else -> "${base}_default()"
    }
  }

  override fun genValueClass(name: String, base: String) {
    println("type $name = $base;")
    println("fn $name(x: $base) -> $name { x }")
    val default = getDefaultValueForBase(base)
    println("fn ${name}_default() -> $name { $name($default) }")
  }

  override fun genClsField(name: String, typ: String, init: String?): String {
    return "  ${name}: $typ"
  }

  override fun genContainerClass(name: String, fields: List<Triple<String, String, String?>>) {
    println("struct $name {")
    println(fields.joinToString(",\n") {
      genClsField(it.first, it.second, it.third)
    })
    println("}")
  }

  override fun genToplevel(n: String, e: TExpr): String {
    val typ = TypeResolver.topLevelTyper[e].asType()
    val default = getDefaultValueForBase(typ.toString())//genExpr(e, emptyMap())
    return "static $n: $typ = $default;"
  }
}