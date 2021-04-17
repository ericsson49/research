import java.math.BigInteger

class KotlinGen: BaseGen() {
  override val destructLValTuples = false

  fun namedTypeToStr(n: String): String = when(n) {
    "object" -> "Any"
    "str" -> "String"
    "int" -> "pyint"
    "bytes" -> "pybytes"
    "bool" -> "pybool"
    "List" -> "SSZList"
    "Vector" -> "SSZVector"
    "Dict" -> "PyDict"
    "Bitlist" -> "SSZBitlist"
    "Bitvector" -> "SSZBitvector"
    "ByteList" -> "SSZByteList"
    "ByteVector" -> "SSZByteVector"
    "boolean" -> "SSZBoolean"
    "None" -> "Unit"
    else -> n
  }

  override fun typeToStr(t: RTType): String = when(t) {
    is NamedType -> {
      val name = if (t.name == "Tuple")
        when(t.tParams.size) {
          2 -> "Pair"
          3 -> "Triple"
          else -> fail("not implemented $t")
        }
      else
        namedTypeToStr(t.name)
      if (t.tParams.isEmpty())
        name
      else
        name + "<" + t.tParams.map { typeToStr(it) }.joinToString(",") + ">"
    }
    else -> TODO()
  }

  override val parenthesesAroundLambda = False

  override fun genNameConstant(e: NameConstant): PExpr = atomic(e.value.toString())

  override fun genNum(n: Num): String {
    return when(n.n) {
      is BigInteger -> "\"" + n.n.toString() + "\".toBigInteger()"
      is Int -> n.n.toString() + "uL"
      else -> fail("not supported yet")
    }
  }

  override fun genLambdaArg(a: LVInfo): String = when(a) {
    is LVName -> a.name
    is LVTuple -> "(" + a.elts.map(::genLambdaArg).joinToString(", ") + ")"
    else -> fail("unsupported $a")
  }

  override fun genLambda(args: List<String>, preBody: List<String>, body: String): String
      = "{ " + args.joinToString(", ") + " -> " + body + " }"
  override fun genIfExpr(t: PExpr, b: PExpr, e: PExpr): PExpr = atomic("if (${t.expr}) ${b.expr} else ${e.expr}")
  override fun genCmpOp(a: PExpr, op: ECmpOp, b: PExpr): PExpr {
    return if (op == ECmpOp.Is && b.expr == "null")
      format_binop_expr(a, b, "==")
    else if (op == ECmpOp.IsNot && b.expr == "null")
      format_binop_expr(a, b, "!=")
    else
      format_binop_expr(a, b, genCmpop(op))
  }
  override fun genBoolOp(exprs: List<PExpr>, op: EBoolOp): PExpr = format_commutative_op_expr(exprs, genTBoolop(op))
  override fun genBinOp(l: PExpr, o: EBinOp, r: PExpr): PExpr = when(o) {
    EBinOp.Pow -> atomic("${l}.pow($r)")
    else -> format_binop_expr(l, r, genOperator(o))
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
      EBinOp.LShift -> "shl"
      EBinOp.RShift -> "shr"
      EBinOp.BitOr -> "or"
      EBinOp.BitXor -> "xor"
      EBinOp.BitAnd -> "and"
      EBinOp.FloorDiv -> "/"
    }
  }
  override fun genIndexSubscript(e: LVInfo, typ: RTType): PExpr {
    val resolveNull = (typ is NamedType && isGenType(typ) && (typ.name == "Dict" || typ.name == "SSZDict"))
    val (ex, t, ind) = when(e) {
      is LVIndex -> Triple(e.e, e.t, e.index)
      is LVSlice -> Triple(e.e, e.t, format_binop_expr(e.start ?: atomic("0"), e.upper ?: atomic("-1"), "until"))
      else -> fail("not supported")
    }
    return atomic("${toLoad(ex, TPyObject).expr}[${ind.expr}]" + (if (resolveNull) "!!" else ""))
  }

  fun toLoad(lhs: LVInfo, t: RTType): PExpr = when(lhs) {
    is LVName -> atomic(lhs.name)
    is LVAttr -> genAttrLoad(toLoad(lhs.e, TPyObject), lhs.attr)
    is LVIndex -> genIndexSubscript(lhs,t)
    is LVSlice -> genIndexSubscript(lhs,t)
    is LVTuple -> atomic("(" + lhs.elts.map { toLoad(it, TPyObject).expr }.joinToString(", ") + ")")
  }

  override fun genPyList(elts: List<String>) = atomic("PyList(" + elts.joinToString(", ") + ")")
  override fun genPyDict(elts: List<Pair<String,String>>): String = "PyDict(" + elts.map { "${it.first} to ${it.second}" }.joinToString(", ") + ")"
  override fun genTuple(elts: List<String>): PExpr {
    val tupleName = if (elts.size == 0)
      "emptyList"
    else if (elts.size == 2)
      "Pair"
    else if (elts.size == 3)
      "Triple"
    else
      fail("too many tuple elements")
    return atomic(tupleName + "(" + elts.joinToString(", ") + ")")
  }

  override fun genFunHandle(e: TExpr, type: Sort, fh: FunSignature, argRefs: List<ArgRef>, args: List<String>, kwdArgs: List<Pair<String, String>>, exprTypes: ExprTypes): Pair<String, List<String>> {
    val resArgs = args.plus(kwdArgs.map { it.first + "=" + it.second })
    return if (type is Clazz) {
      Pair(typeToStr(type.toInstance()), resArgs)
    } else if (e is Attribute) {
      Pair(genAttrBase(genExpr(e.value, exprTypes), e.attr).expr, resArgs)
    } else {
      Pair(genExpr(e, exprTypes).expr, resArgs)
    }
  }

  override fun genReturn(v: String?, t: RTType) = "return" + (v?.let {" " + it} ?: "")

  override fun genVarAssignment(isVar: Boolean?, lv: LVInfo, typ: String?, value: String?): String = when(lv) {
    is LVName -> {
      val v = if (isVar != null) {
        (if (isVar) "var" else "val") + " "
      } else ""
      val typV = typ?.let { ": $it" } ?: ""
      val initV = value?.let { " = $it" } ?: ""
      v + lv.name + typV + initV
    }
    is LVIndex -> {
      genIndexSubscript(lv, lv.t).expr + " = " + value
    }
    is LVSlice -> {
      genIndexSubscript(lv, lv.t).expr + " = " + value
    }
    is LVAttr -> {
      toLoad(lv, TPyObject).expr + " = " + value
    }
    is LVTuple -> {
      "val (" + lv.elts.map { genVarAssignment(null, it, null, null) }.joinToString(",") + ") = " + value
    }
  }


  override fun genAttrLoad(e: PExpr, a: identifier) = genAttrBase(e, a)
  override fun genAugAssignment(lhs: LVInfo, op: EBinOp, rhs: PExpr): String
          = genVarAssignment(
          null, lhs, null, genBinOp(toLoad(lhs, TPyObject), op, rhs).expr)
  override fun genExprStmt(e: String): String = e
  override fun genAssertStmt(e: String, m: String?): String = "assert(" + e + (m?.let { ", " + it } ?: "") + ")"
  override fun genForHead(t: LVInfo, i: String): String = "for (${toLoad(t, TPyObject).expr} in $i) {"
  override fun genWhileHead(t: String): String = "while ($t) {"
  override fun genIfHead(t: String): String = "if ($t) {"


  override fun genOptionalType(typ: String): String = "$typ?"

  override fun genFunBegin(n: String, args: List<Pair<Arg,String?>>, typ: String): String {
    fun genArg(a: Arg, defaultNull: Boolean): String {
      return a.arg + (a.annotation?.let {
        val typ = genNativeType(it)
        ": " + (if (defaultNull) genOptionalType(typ) else typ)
      } ?: "")
    }
    val argsStr = args.map {
      genArg(it.first, it.second == "null") + (it.second?.let { " = $it" } ?: "")
    }.joinToString(", ")
    return "fun " + n + "(" + argsStr + "): " + typ + " {"
  }

  override fun genComment(comment: String) {
    print("/*")
    print(comment.split("\\n").joinToString("\n"))
    println("*/")
  }

  override fun getDefaultValueForBase(base: String): String? = run {
    when(base) {
      "boolean" -> "false"
      "uint8" -> "0u.toUByte()"
      "uint64" -> "0uL"
      else -> "${base}()"
    }
  }

  override fun genValueClass(name: String, base: String) {
    println("typealias $name = $base")
    println("fun $name(x: $base): $name = x")
    val default = getDefaultValueForBase(base)
    println("fun $name() = $name($default)")
  }

  override fun genClsField(name: String, typ: String, init: String?): String {
    return "  var ${name}: $typ" + (init?.let { " = $it" } ?: "")
  }

  override fun genContainerClass(name: String, fields: List<Triple<String, String, String?>>) {
    println("data class $name(")
    println(fields.joinToString(",\n") {
      genClsField(it.first, it.second, it.third)
    })
    println(")")
  }

  override fun genToplevel(n: String, e: TExpr): String {
    return "val " + n + " = " + genExpr(e, TypeResolver.topLevelTyper).expr
  }
}