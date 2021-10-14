package onotole

import java.math.BigInteger

class KotlinGen(currPkg: String, importedPkgs: Set<String>): BaseGen(currPkg, importedPkgs) {
  override val destructLValTuples = false
  
  fun namedTypeToStr(n: String): String {
    val (pkg, name) = splitClassName(n)

    val name2 = if (pkg in importedPkgs) name else n

    return when(name2) {
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
      else -> name2
    }
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
        name + "<" + t.tParams.map { typeToStr(it) }.joinToString(", ") + ">"
    }
    else -> TODO()
  }

  override fun applyMapFilter(e: RExpr, a: String, l: String): RExpr = genAttrBase(e, wrapLambda(a, l))

  override val parenthesesAroundLambda = False

  override fun genNameConstant(e: NameConstant): RExpr = atomic(e.value.toString())

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

  override fun genLambda(args: List<String>, preBody: List<String>, body: RExpr): String
      = "{ " + args.joinToString(", ") + " -> " + render(body) + " }"
  override fun genIfExpr(t: RExpr, b: RExpr, e: RExpr): RExpr = atomic("if (${render(t)}) ${render(b)} else ${render(e)}")
  override fun genCmpOp(a: RExpr, op: ECmpOp, b: RExpr): RExpr {
    return if (op == ECmpOp.Is && render(b) == "null")
      RInfix("==", a, b)
    else if (op == ECmpOp.IsNot && render(b) == "null")
      RInfix("!=", a, b)
    else
      RInfix(genCmpop(op), a, b)
  }
  override fun genBoolOp(exprs: List<RExpr>, op: EBoolOp): RExpr = RInfix(genTBoolop(op), exprs)
  override fun genBinOp(l: RExpr, o: EBinOp, r: RExpr): RExpr = when(o) {
    EBinOp.Pow -> mkAttrCall(l, "pow", r)
    else -> RInfix(genOperator(o), l, r)
  }
  override fun genUnaryOp(a: RExpr, op: EUnaryOp) = RPrefix(genUnaryop(op), a)
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
  override fun genIndexSubscript(e: LVInfo, typ: RTType): RExpr {
    val resolveNull = (typ is NamedType && isGenType(typ) && (typ.name == "Dict" || typ.name == "SSZDict"))
    val (ex, t, ind) = when(e) {
      is LVIndex -> Triple(e.e, e.t, e.index)
      is LVSlice -> Triple(e.e, e.t, RInfix("until", e.start ?: atomic("0"), e.upper ?: atomic("-1")))
      else -> fail("not supported")
    }
    return atomic("${render(toLoad(ex, TPyObject))}[${render(ind)}]" + (if (resolveNull) "!!" else ""))
  }

  fun toLoad(lhs: LVInfo, t: RTType): RExpr = when(lhs) {
    is LVName -> atomic(lhs.name)
    is LVAttr -> genAttrLoad(toLoad(lhs.e, TPyObject), lhs.attr, false)
    is LVIndex -> genIndexSubscript(lhs,t)
    is LVSlice -> genIndexSubscript(lhs,t)
    is LVTuple -> atomic("(" + lhs.elts.map { render(toLoad(it, TPyObject)) }.joinToString(", ") + ")")
  }

  fun mkCall(e: RExpr, args: List<RExpr>) = RInfix("", e, RSeq("(", ")", ",", args))
  fun mkCall(name: String, args: List<RExpr>) = mkCall(RLit(name), args)
  fun mkCall(name: String, vararg args: RExpr) = mkCall(name, args.toList())
  fun mkAttrCall(r: RExpr, a: String, args: List<RExpr>) = mkCall(genAttrBase(r, a), args)
  fun mkAttrCall(r: RExpr, a: String, vararg args: RExpr) = mkAttrCall(r, a, args.toList())

  override fun genPyList(elts: List<RExpr>) = mkCall("PyList", elts)
  override fun genPyDict(elts: List<Pair<RExpr,RExpr>>): RExpr = mkCall("PyDict", elts.map { genTuple(listOf(it.first, it.second)) })
  override fun genTuple(elts: List<RExpr>): RExpr {
    val tupleName = if (elts.size == 0)
      "emptyList"
    else if (elts.size == 2)
      "Pair"
    else if (elts.size == 3)
      "Triple"
    else
      fail("too many tuple elements")
    return mkCall(tupleName, elts)
  }

  override fun genFunHandle(e: TExpr, type: Sort, fh: FunSignature, argRefs: List<ArgRef>, args: List<RExpr>, kwdArgs: List<Pair<String, RExpr>>, exprTypes: ExprTypes): Pair<RExpr, List<String>> {
    val resArgs = args.map { render(it) }.plus(kwdArgs.map { it.first + " = " + render(it.second) })
    val fh = if (type is Clazz) {
      RLit(typeToStr(type.toInstance()))
    } else if (e is Attribute) {
      genAttrBase(genExpr(e.value, exprTypes), e.attr)
    } else {
      genExpr(e, exprTypes)
    }
    return fh to resArgs
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
      render(genIndexSubscript(lv, lv.t)) + " = " + value
    }
    is LVSlice -> {
      render(genIndexSubscript(lv, lv.t)) + " = " + value
    }
    is LVAttr -> {
      render(toLoad(lv, TPyObject)) + " = " + value
    }
    is LVTuple -> {
      "val (" + lv.elts.map { genVarAssignment(null, it, null, null) }.joinToString(",") + ") = " + value
    }
  }


  override fun genAttrLoad(e: RExpr, a: identifier, isStatic: Boolean) = genAttrBase(e, a)
  override fun genAugAssignment(lhs: LVInfo, op: EBinOp, rhs: RExpr): String
          = genVarAssignment(
          null, lhs, null, render(genBinOp(toLoad(lhs, TPyObject), op, rhs)))
  override fun genExprStmt(e: String): String = e
  override fun genAssertStmt(e: String, m: String?): String = "assert(" + e + (m?.let { ", " + it } ?: "") + ")"
  override fun genForHead(t: LVInfo, i: String): String = "for (${render(toLoad(t, TPyObject))} in $i) {"
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

  override fun genValueClass(name: String, base: TExpr) {
    val base = genNativeType(base)
    println("typealias $name = $base")
    println("fun $name(x: $base): $name = x")
    val default = getDefaultValueForBase(base)
    println("fun $name() = $name($default)")
  }

  override fun genClsField(name: String, typ: String, init: String?): String {
    return "  var ${name}: $typ" + (init?.let { " = $it" } ?: "")
  }

  override fun genContainerClass(name: String, base: TExpr, fields: List<Triple<String, String, String?>>) {
    println("open class $name(")
    val declaredFields = fields.map { genClsField(it.first, it.second, it.third) }
    fun getBaseFields(nt: NamedType): List<Pair<String,NamedType>> {
      val ti = nt.typeInfo as DataTInfo
      val attrs = ti.attrs
          .filter { it.second is NamedType }
          .map { it.first to it.second as NamedType }
      return attrs.plus(ti.baseType?.let { getBaseFields(it as NamedType) } ?: emptyList())
    }

    val baseFields = getBaseFields(parseType(base) as NamedType)
    val baseStrFields = baseFields
        .map {
          val fTyp = typeToStr(it.second)
          val init = getDefaultValueForBase(fTyp) ?: "$fTyp()"
          "  " + it.first + ": " + fTyp + " = " + init
        }

    println((declaredFields + baseStrFields).joinToString(",\n"))
    if (baseStrFields.isEmpty()) {
      println(")")
    } else {
      println("): ${genNativeType(base)}(" + baseFields.map { it.first + " = " + it.first }.joinToString(", ") + ")")
    }
  }

  override fun genToplevel(n: String, e: TExpr) {
    println("val " + n + " = " + render(genExpr(e, TypeResolver.topLevelTyper)))
  }
}
