import java.math.BigInteger
import java.nio.file.Paths

class JavaGen(val packageName: String, val rootPath: String): BaseGen() {
  val basePath = Paths.get(rootPath, *packageName.split(".").toTypedArray())
  override val destructLValTuples = True

  fun namedTypeToStr(n: String): String = when(n) {
    "object" -> "Object"
    "str" -> "String"
    "int" -> "pyint"
    "bytes" -> "pybytes"
    "bool" -> "pybool"
    "Unit" -> "void"
    "List" -> "SSZList"
    "Vector" -> "SSZVector"
    "Dict" -> "PyDict"
    "Bitlist" -> "SSZBitlist"
    "Bitvector" -> "SSZBitvector"
    "ByteList" -> "SSZByteList"
    "ByteVector" -> "SSZByteVector"
    "boolean" -> "SSZBoolean"
    "bit" -> "SSZBit"
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

  override val parenthesesAroundLambda = True
  override fun genNameConstant(e: NameConstant): PExpr = when(e.value) {
    is Boolean -> atomic("pybool.create(" + e.value + ")")
    else -> atomic(e.value.toString())
  }
  override fun genNum(n: Num): String {
    return when(n.n) {
      is BigInteger -> "pyint.create(\"${n.n}\")"
      is Int -> "pyint.create(${n.n}L)"
      else -> fail("not supported yet")
    }
  }

  override fun genLambdaArg(a: LVInfo): String = when(a) {
    is LVName -> a.name
    else -> fail("unsupported $a")
  }

  override fun genLambda(args: List<String>, preBody: List<String>, body: String): String {
    val args = args.mapIndexed { i, a -> if (a == "_") "_$i" else a }
    val begin = "(" + args.joinToString(", ") + ") -> "
    return if (preBody.isEmpty())
      begin + body
    else
      begin + "{ " + preBody.joinToString(" ")  + " return $body; }"
  }

  private fun toJavaBool(e: String): String = when {
    e == "pybool.create(true)" -> "true"
    e == "pybool.create(false)" -> "false"
    e.endsWith(" == null") -> e
    e.endsWith(" != null") -> e
    else -> "$e.v()"
  }

  override fun genIfExpr(t: PExpr, b: PExpr, e: PExpr) = atomic(toJavaBool(t.expr) + " ? " + b.expr + " : " + e.expr)

  override fun genCmpOp(a: PExpr, o: ECmpOp, b: PExpr): PExpr {
    return when (o) {
      ECmpOp.Eq -> atomic("eq(${a.expr}, ${b.expr})")
      ECmpOp.NotEq -> genUnaryOp(genCmpOp(a, ECmpOp.Eq, b), EUnaryOp.Not)
      ECmpOp.Lt -> atomic("less(${a.expr}, ${b.expr})")
      ECmpOp.LtE -> atomic("lessOrEqual(${a.expr}, ${b.expr})")
      ECmpOp.Gt -> atomic("greater(${a.expr}, ${b.expr})")
      ECmpOp.GtE -> atomic("greaterOrEqual(${a.expr}, ${b.expr})")
      ECmpOp.Is -> format_binop_expr(a, b, "==")
      ECmpOp.IsNot -> format_binop_expr(a, b, "!=")
      ECmpOp.In -> atomic("contains(${b.expr}, ${a.expr})")
      ECmpOp.NotIn -> genUnaryOp(genCmpOp(a, ECmpOp.In, b), EUnaryOp.Not)
    }
  }

  override fun genIndexSubscript(e: LVInfo, typ: RTType) = toLoad(e)

  override fun genBoolOp(exprs: List<PExpr>, op: EBoolOp): PExpr {
    return when {
      exprs.isEmpty() -> fail()
      exprs.size == 1 -> exprs[0]
      else -> {
        val funcName = when(op) {
          EBoolOp.And -> "and"
          EBoolOp.Or -> "or"
        }
        atomic(funcName + "(" + exprs.joinToString(", ") { it.expr } + ")")
      }
    }
  }
  override fun genBinOp(l: PExpr, o: EBinOp, r: PExpr): PExpr = atomic("${genOperator(o)}(${l}, $r)")
  override fun genUnaryOp(a: PExpr, op: EUnaryOp) = when (op) {
    //EUnaryOp.Invert -> "~"
    EUnaryOp.Not -> atomic("not(" + a.expr + ")")
    //EUnaryOp.UAdd -> "+"
    EUnaryOp.USub -> atomic("uminus(" + a.expr + ")")
    else -> fail("not implemented $op")
  }


  override fun genOperator(o: EBinOp): String {
    return when (o) {
      EBinOp.Add -> "plus"
      EBinOp.Sub -> "minus"
      EBinOp.Mult -> "multiply"
      EBinOp.MatMult -> fail("@ is not supported")
      EBinOp.Div -> fail("/ is not supported")
      EBinOp.Mod -> "modulo"
      EBinOp.Pow -> "power"
      EBinOp.LShift -> "leftShift"
      EBinOp.RShift -> "rightShift"
      EBinOp.BitOr -> "bitOr"
      EBinOp.BitXor -> "bitXor"
      EBinOp.BitAnd -> "bitAnd"
      EBinOp.FloorDiv -> "divide"
    }
  }

  override fun genPyList(elts: List<String>): PExpr {
    return atomic("PyList.of(" + elts.joinToString(", ") + ")")
  }
  override fun genPyDict(elts: List<Pair<String, String>>): String {
    return "PyDict.of(" + elts.joinToString(", ") { "new Pair<>(" + it.first + ", " + it.second + ")" } + ")"
  }

  override fun genTuple(elts: List<String>): PExpr {
    return atomic(when(elts.size) {
      0 -> "new PyList<>()"
      2 -> "new Pair<>(" + elts.joinToString(", ") + ")"
      3 -> "new Triple<>(" + elts.joinToString(", ") + ")"
      else -> fail("Tuple${elts.size} not yet supported")
    })
  }

  override fun genFunHandle(e: TExpr, type: Sort, fh: FunSignature, argRefs: List<ArgRef>, args: List<String>, kwdArgs: List<Pair<String,String>>, exprTypes: ExprTypes): Pair<String, List<String>> {
    val resArgs = argRefs.map { ar ->
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

  override fun genReturn(v: String?, t: RTType) = "return" + (v?.let {" " + it} ?: "") + ";"

  override fun genVarAssignment(isVar: Boolean?, name: LVInfo, typ: String?, value: String?): String = when(name) {
    is LVName -> {
      val typV = typ ?: "var"
      val initV = value?.let { " = $it" } ?: ""
      if (isVar == null) "${name.name}$initV;" else "$typV ${name.name}$initV;"
    }
    is LVAttr -> {
      val nm = name.attr[0].toUpperCase() + name.attr.substring(1)
      toLoad(name.e).expr + ".set" + nm + "(" + value + ");"
    }
    is LVIndex -> toLoad(name.e).expr + ".set(" + name.index.expr + ", " + value + ");"
    is LVSlice -> toLoad(name.e).expr + ".setSlice(" + name.start?.expr + ", " + name.upper?.expr + ", " + value + ");"
    is LVTuple -> fail("not supported $name")
  }

  override fun genAttrLoad(e: PExpr, attr: identifier): PExpr {
    val nm = attr.toUpperCase() + attr.substring(1)
    return atomic(e.expr + ".get" + nm + "()")
  }


  fun toLoad(lhs: LVInfo): PExpr = when(lhs) {
    is LVName -> atomic(lhs.name)
    is LVAttr -> genAttrLoad(toLoad(lhs.e), lhs.attr)
    is LVIndex -> atomic(toLoad(lhs.e).expr + ".get(" + lhs.index + ")")
    is LVSlice -> atomic(toLoad(lhs.e).expr + ".getSlice(" + lhs.start?.expr + ", " + lhs.upper?.expr + ")")
    is LVTuple -> fail("not supported $lhs")
  }

  override fun genAugAssignment(lhs: LVInfo, op: EBinOp, rhs: PExpr) = genVarAssignment(
      null, lhs, null, genBinOp(toLoad(lhs), op, rhs).expr)

  override fun genExprStmt(e: String) = "$e;"

  override fun genAssertStmt(e: String, m: String?) = "pyassert(" + e + (m?.let { ", " + it } ?: "") + ");"

  override fun genForHead(t: LVInfo, i: String) = when(t) {
    is LVName -> "for (var ${t.name}: $i) {"
    else -> fail("not supported $t")
  }

  override fun genWhileHead(t: String): String = "while (${toJavaBool(t)}) {"

  override fun genIfHead(t: String): String = "if (${toJavaBool(t)}) {"

  override fun genComment(comment: String) {
    print("/*")
    print(comment.split("\\n").joinToString("\n"))
    println("*/")
  }

  override fun genFunBegin(n: String, args: List<Pair<Arg, String?>>, typ: String): String {
    fun genArg(a: Arg, defaultNull: Boolean): String {
      return (a.annotation?.let {
        val typ = genNativeType(it)
        (if (defaultNull) genOptionalType(typ) else typ)
      } ?: "") + " " + a.arg
    }
    val argsStr = args.map {
      genArg(it.first, it.second == "null")
    }.joinToString(", ")
    return "public static $typ " + n + "(" + argsStr + ") {"
  }

  override fun genToplevel(n: String, e: TExpr): String {
    val exprTypes = TypeResolver.topLevelTyper
    val type = exprTypes[e].asType()
    val typeStr = typeToStr(type)
    val value = genExpr(e, exprTypes).expr
    return "public static $typeStr $n = $value;"
  }

  override fun getDefaultValueForBase(base: String): String? {
    return when(base) {
      "boolean" -> "false"
      "uint8" -> "uint8.ZERO"
      "uint64" -> "uint64.ZERO"
      else -> "new ${base}()"
    }
  }

  override fun genOptionalType(typ: String) = typ

  override fun genValueClass(name: String, base: String) {
    val pw = basePath.resolve("data").resolve("$name.java").toAbsolutePath().toFile().printWriter()

    pw.use { pw ->
      pw.println("package $packageName.data;")
      pw.println()
      pw.println("import beacon_java.pylib.*;")
      pw.println("import beacon_java.ssz.*;")
      pw.println()
      pw.println("public class $name extends $base {")
      TypeResolver.funcSigs[name]!!.funcs.forEach {
        val args = it.args
        if (args.isEmpty()) {
          val default = getDefaultValueForBase(base)
          pw.println("  public $name() { super($default); }")
        } else {
          val type = typeToStr(args[0].type)
          pw.println("  public $name($type value) { super(value); }")
        }
      }
      pw.println("}")
    }
  }

  override fun genClsField(name: String, typ: String, init: String?): String {
    return "  public $typ $name = ${name}_default;" //+ (init?.let { " = $it" } ?: "") + ";"
  }

  override fun genContainerClass(name: String, fields: List<Triple<String, String, String?>>) {
    val pw = basePath.resolve("data").resolve("$name.java").toAbsolutePath().toFile().printWriter()
    pw.use { pw ->
      pw.println("package $packageName.data;")
      pw.println()
      pw.println("import beacon_java.pylib.*;")
      pw.println("import beacon_java.ssz.*;")
      pw.println("import lombok.*;")
      pw.println()
      pw.println("@Data @NoArgsConstructor @AllArgsConstructor")
      pw.println("public class $name {")
      fields.forEach { (name, type, init) ->
        pw.println("  public static $type ${name}_default" + (init?.let { " = $it" } ?: "") + ";")
      }
      pw.println(fields.joinToString("\n") {
        genClsField(it.first, it.second, it.third)
      })
      pw.println("  public $name copy() { return this; }")
      pw.println("}")
    }
  }

}