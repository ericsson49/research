package onotole

import java.io.PrintStream
import java.math.BigInteger
import java.nio.file.Path
import java.nio.file.Paths

//class JavaGen(val packageName: String, val rootPath: String, currPkg: String, importedPkgs: Set<String>): BaseGen(currPkg, importedPkgs) {
//  val basePath = Paths.get(rootPath, *packageName.split(".").toTypedArray())
//  override val destructLValTuples = True
//
//  fun mapClassModuleName(mod: String): String = when(mod) {
//    "" -> "beacon_java.pylib"
//    "bls" -> "beacon_java.deps"
//    "ssz" -> "beacon_java.ssz"
//    else -> "beacon_java." + mod + ".data"
//  }
//
//  fun nameToFullString(n: String): String {
//    val (pkg, name) = splitClassName(remapName(n))
//    return mapClassModuleName(pkg) + "." + name
//  }
//
//  fun remapName(name: String) = when(name) {
//    "object" -> "Object"
//    "str" -> "String"
//    "int" -> "pyint"
//    "bytes" -> "pybytes"
//    "bool" -> "pybool"
//    "None" -> "void"
//    "Dict" -> "PyDict"
//    "ssz.List" -> "ssz.SSZList"
//    "ssz.Vector" -> "ssz.SSZVector"
//    "ssz.Bitlist" -> "ssz.SSZBitlist"
//    "ssz.Bitvector" -> "ssz.SSZBitvector"
//    "ssz.ByteList" -> "ssz.SSZByteList"
//    "ssz.ByteVector" -> "ssz.SSZByteVector"
//    "ssz.boolean" -> "ssz.SSZBoolean"
//    "ssz.bit" -> "ssz.SSZBit"
//    else -> name
//  }
//
//  fun nameToNativeStr(n: String): String {
//    val (pkg, name) = splitClassName(remapName(n))
//    return if (pkg == "" || n in importedNames) name else nameToFullString(n)
//  }
//
//  override fun typeToStr(t: RTType): String = when(t) {
//    is NamedType -> {
//      val name = if (t.name == "Tuple")
//        when(t.tParams.size) {
//          2 -> "Pair"
//          3 -> "Triple"
//          else -> fail("not implemented $t")
//        }
//      else
//        nameToNativeStr(t.name)
//      if (t.tParams.isEmpty())
//        name
//      else
//        name + "<" + t.tParams.map { typeToStr(it) }.joinToString(",") + ">"
//    }
//    else -> TODO()
//  }
//
//  override val parenthesesAroundLambda = True
//  override fun genNameConstant(e: NameConstant): RExpr = when(e.value) {
//    is Boolean -> atomic("pybool.create(" + e.value + ")")
//    else -> atomic(e.value.toString())
//  }
//  override fun genNum(n: Num): String {
//    return when(n.n) {
//      is BigInteger -> "pyint.create(\"${n.n}\")"
//      is Int -> "pyint.create(${n.n}L)"
//      else -> fail("not supported yet")
//    }
//  }
//
//  override fun applyMapFilter(e: RExpr, a: String, l: String): RExpr = genAttrBase(e, wrapLambda(a, l))
//
//  override fun genLambdaArg(a: LVInfo): String = when(a) {
//    is LVName -> a.name
//    else -> fail("unsupported $a")
//  }
//
//  override fun genLambda(args: List<String>, preBody: List<String>, body: RExpr): String {
//    val args = args.mapIndexed { i, a -> if (a == "_") "_$i" else a }
//    val begin = "(" + args.joinToString(", ") + ") -> "
//    return if (preBody.isEmpty())
//      begin + render(body)
//    else
//      begin + "{ " + preBody.joinToString(" ")  + " return ${render(body)}; }"
//  }
//
//  private fun toJavaBool(e: String): String = when {
//    e == "pybool.create(true)" -> "true"
//    e == "pybool.create(false)" -> "false"
//    e.endsWith(" == null") -> e
//    e.endsWith(" != null") -> e
//    else -> "$e.v()"
//  }
//
//  override fun genIfExpr(t: RExpr, b: RExpr, e: RExpr) = RPExpr(MIN_PRIO,toJavaBool(render(t)) + " ? " + render(b) + " : " + render(e))
//
//  fun mkCall(name: String, args: List<RExpr>) = genFunCall(RLit(name), args.map { render(it) })
//  override fun genCmpOp(a: RExpr, o: ECmpOp, b: RExpr): RExpr {
//    return when (o) {
//      ECmpOp.Eq -> mkCall("eq", listOf(a, b))
//      ECmpOp.NotEq -> genUnaryOp(genCmpOp(a, ECmpOp.Eq, b), EUnaryOp.Not)
//      ECmpOp.Lt -> mkCall("less", listOf(a, b))
//      ECmpOp.LtE -> mkCall("lessOrEqual", listOf(a, b))
//      ECmpOp.Gt -> mkCall("greater", listOf(a, b))
//      ECmpOp.GtE -> mkCall("greaterOrEqual", listOf(a, b))
//      ECmpOp.Is -> RInfix("==", a, b)
//      ECmpOp.IsNot -> RInfix("!=", a, b)
//      ECmpOp.In -> mkCall("contains", listOf(b, a))
//      ECmpOp.NotIn -> genUnaryOp(genCmpOp(a, ECmpOp.In, b), EUnaryOp.Not)
//    }
//  }
//
//  override fun genIndexSubscript(e: LVInfo, typ: RTType) = toLoad(e)
//
//  override fun genBoolOp(exprs: List<RExpr>, op: EBoolOp): RExpr {
//    return when {
//      exprs.isEmpty() -> fail()
//      exprs.size == 1 -> exprs[0]
//      else -> {
//        val funcName = when(op) {
//          EBoolOp.And -> "and"
//          EBoolOp.Or -> "or"
//        }
//        mkCall(funcName, exprs)
//      }
//    }
//  }
//  override fun genBinOp(l: RExpr, o: EBinOp, r: RExpr): RExpr = mkCall(genOperator(o), listOf(l, r))
//  override fun genUnaryOp(a: RExpr, op: EUnaryOp) = when (op) {
//    //EUnaryOp.Invert -> "~"
//    EUnaryOp.Not -> mkCall("not", listOf(a))
//    //EUnaryOp.UAdd -> "+"
//    EUnaryOp.USub -> mkCall("uminus", listOf(a))
//    else -> fail("not implemented $op")
//  }
//
//
//  override fun genOperator(o: EBinOp): String {
//    return when (o) {
//      EBinOp.Add -> "plus"
//      EBinOp.Sub -> "minus"
//      EBinOp.Mult -> "multiply"
//      EBinOp.MatMult -> fail("@ is not supported")
//      EBinOp.Div -> fail("/ is not supported")
//      EBinOp.Mod -> "modulo"
//      EBinOp.Pow -> "pow"
//      EBinOp.LShift -> "leftShift"
//      EBinOp.RShift -> "rightShift"
//      EBinOp.BitOr -> "bitOr"
//      EBinOp.BitXor -> "bitXor"
//      EBinOp.BitAnd -> "bitAnd"
//      EBinOp.FloorDiv -> "divide"
//    }
//  }
//
//  override fun genPyList(elts: List<RExpr>): RExpr {
//    return mkCall("PyList.of", elts)
//  }
//  override fun genPyDict(elts: List<Pair<RExpr, RExpr>>): RExpr {
//    return mkCall("PyDict.of", elts.map { genTuple(listOf(it.first, it.second)) })
//  }
//
//  override fun genTuple(elts: List<RExpr>): RExpr {
//    return when(elts.size) {
//      0 -> mkCall("new PyList<>", emptyList())
//      2 -> mkCall("new Pair<>", elts)
//      3 -> mkCall("new Triple<>", elts)
//      else -> fail("Tuple${elts.size} not yet supported")
//    }
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
//      RLit("new " + typeToStr(type.toInstance()))
//    } else if (e is Attribute) {
//      genAttrBase(genExpr(e.value, exprTypes), e.attr)
//    } else {
//      genExpr(e, exprTypes)
//    }
//    return funExpr to resArgs
//  }
//
//  override fun genReturn(v: String?, t: RTType) = "return" + (v?.let {" " + it} ?: "") + ";"
//
//  override fun genVarAssignment(isVar: Boolean?, name: LVInfo, typ: String?, value: TExpr?): String = genExprStmt(when(name) {
//    is LVName -> {
//      val typV = typ ?: "var"
//      val initV = value?.let { " = $it" } ?: ""
//      if (isVar == null) "${name.name}$initV" else "$typV ${name.name}$initV"
//    }
//    is LVAttr -> {
//      val nm = name.attr[0].toUpperCase() + name.attr.substring(1)
//      render(genFunCall(genAttrBase(toLoad(name.e), "set$nm"), listOf(value!!)))
//    }
//    is LVIndex -> render(genFunCall(genAttrBase(toLoad(name.e), "set"), listOf(render(name.index), value!!)))
//    is LVSlice -> {
//      val args = listOf(
//          name.start?.let { render(it) } ?: "null",
//          name.upper?.let { render(it) } ?: "null")
//      if (name.step != null) TODO()
//      render(genFunCall(genAttrBase(toLoad(name.e), "setSlice"), args.plus(value!!)))
//    }
//    is LVTuple -> fail("not supported $name")
//  })
//
//  override fun genAttrLoad(e: RExpr, attr: identifier, isStatic: Boolean): RExpr {
//    return if (isStatic) {
//      genAttrBase(e, attr)
//    } else {
//      val nm = attr[0].toUpperCase() + attr.substring(1)
//      genFunCall(genAttrBase(e, "get$nm"), emptyList())
//    }
//  }
//
//
//  fun toLoad(lhs: LVInfo): RExpr = when(lhs) {
//    is LVName -> atomic(lhs.name)
//    is LVAttr -> genAttrLoad(toLoad(lhs.e), lhs.attr, false)
//    is LVIndex -> genFunCall(genAttrBase(toLoad(lhs.e),"get"), listOf(render(lhs.index)))
//    is LVSlice -> {
//      val args = mutableListOf(
//          lhs.start?.let { render(it) } ?: "null",
//          lhs.upper?.let { render(it) } ?: "null")
//      if (lhs.step != null)
//        args.add(render(lhs.step))
//
//      genFunCall(genAttrBase(toLoad(lhs.e), "getSlice"), args)
//    }
//    is LVTuple -> fail("not supported $lhs")
//  }
//
//  override fun genAugAssignment(lhs: TExpr, op: EBinOp, rhs: RExpr) = genVarAssignment(
//      null, lhs, null, render(genBinOp(toLoad(lhs), op, rhs)))
//
//  override fun genExprStmt(e: String) = "$e;"
//
//  override fun genAssertStmt(e: String, m: String?) = "pyassert(" + e + (m?.let { ", " + it } ?: "") + ");"
//
//  override fun genForHead(t: LVInfo, i: String) = when(t) {
//    is LVName -> "for (var ${t.name}: $i) {"
//    else -> fail("not supported $t")
//  }
//
//  override fun genWhileHead(t: String): String = "while (${toJavaBool(t)}) {"
//
//  override fun genIfHead(t: String): String = "if (${toJavaBool(t)}) {"
//
//  override fun genComment(comment: String) {
//    val pw = currModule!!.pw
//    pw.print("  /*")
//    pw.print(comment.split("\\n").joinToString("\n") { if (it.isNotBlank()) "  $it" else it })
//    pw.println("  */")
//  }
//
//  override fun genFunBegin(n: String, args: List<Pair<Arg, String?>>, typ: String): String {
//    fun genArg(a: Arg, defaultNull: Boolean): String {
//      return (a.annotation?.let {
//        val typ = genNativeType(it)
//        (if (defaultNull) genOptionalType(typ) else typ)
//      } ?: "") + " " + a.arg
//    }
//    val argsStr = args.map {
//      genArg(it.first, it.second == "null")
//    }.joinToString(", ")
//    return "public static $typ " + n + "(" + argsStr + ") {"
//  }
//
//  override fun genToplevel(n: String, e: TExpr) {
//    val exprTypes = TypeResolver.topLevelTyper
//    val type = exprTypes[e].asType()
//    val typeStr = typeToStr(type)
//    val value = render(genExpr(e, exprTypes))
//    currModule!!.pw.println("  $typeStr $n = $value;")
//  }
//
//  override fun getDefaultValueForBase(base: String): String? {
//    return when(base) {
//      "boolean" -> "false"
//      "uint8" -> "uint8.ZERO"
//      "uint64" -> "uint64.ZERO"
//      else -> "new ${base}()"
//    }
//  }
//
//  override fun genOptionalType(typ: String) = typ
//
//  override fun genValueClass(name: String, base: TExpr) {
//    val base = genNativeType(base)
//    val pw = currModule!!.pw
//    pw.println("public class $name extends $base {")
//    TypeResolver.funcSigs[currPkg + "." + name]!!.funcs.forEach {
//      val args = it.args
//      if (args.isEmpty()) {
//        val default = getDefaultValueForBase(base)
//        pw.println("  public $name() { super($default); }")
//      } else {
//        val type = typeToStr(args[0].type)
//        pw.println("  public $name($type value) { super(value); }")
//      }
//    }
//    pw.println("}")
//  }
//
//  override fun genClsField(name: String, typ: String, init: String?): String {
//    return "  public $typ $name = ${name}_default;" //+ (init?.let { " = $it" } ?: "") + ";"
//  }
//
//  override fun genContainerClass(name: String, base: TExpr, fields: List<Triple<String, String, String?>>) {
//    val declaredFields = fields.map { genClsField(it.first, it.second, it.third) }
//    val pw = currModule!!.pw
//
//    pw.println("@Data @NoArgsConstructor @AllArgsConstructor")
//    pw.println("public class $name extends ${genNativeType(base)} {")
//    fields.forEach { (name, type, init) ->
//      pw.println("  public static $type ${name}_default" + (init?.let { " = $it" } ?: "") + ";")
//    }
//    pw.println(declaredFields.joinToString("\n"))
//    pw.println("  public $name copy() { return this; }")
//
//    fun getBaseFields(nt: NamedType): List<Pair<String,NamedType>> {
//      val ti = nt.typeInfo as DataTInfo
//      val attrs = ti.attrs
//          .filter { it.second is NamedType }
//          .map { it.first to it.second as NamedType }
//      return (ti.baseType?.let { getBaseFields(it as NamedType) } ?: emptyList()).plus(attrs)
//    }
//
//    val baseFields = getBaseFields(parseType(exprTyper, base) as NamedType)
//    val baseStrFields = baseFields.map { typeToStr(it.second) + " " + it.first }
//
//    if (baseStrFields.isNotEmpty()) {
//      val declaredStrFields = fields.map { it.second + " " + it.first }
//      val baseFieldNames = baseFields.map { it.first }
//      val declaredFieldNames = fields.map { it.first }
//      pw.println("  public $name(" + (baseStrFields + declaredStrFields).joinToString(", ") + ") {")
//      pw.println("    super(" + baseFieldNames.joinToString(", ") + ");")
//      declaredFieldNames.forEach {
//        pw.println("    this.$it = $it;")
//      }
//      pw.println("  }")
//    }
//
//    pw.println("}")
//  }
//
//  override fun toModules(defs: Collection<TopLevelDef>): List<Pair<String, Collection<TopLevelDef>>> {
//    val modules =  super.toModules(defs).toMap()
//    val constants = modules["constants"]!!
//    val classes = modules["classes"]!!
//    val methods = modules["methods"]!!
//    val clsModules = classes.map { "class:" + it.name to listOf(it) }
//    return listOf("constants" to constants).plus("methods" to methods).plus(clsModules)
//  }
//
//  var currModule: JavaModuleRef? = null
//  val importedNames = mutableSetOf<String>()
//
//  inner class JavaModuleRef(val name: String, pw: PrintStream): ModuleRef(pw) {
//    override fun finish() {
//      if (name == "constants" || name == "methods") {
//        pw.println("}")
//        pw.close()
//      } else if (name.startsWith("class:")) {
//        pw.close()
//      }
//    }
//    override fun genTopLevel(name: String, value: TExpr) {
//      this@JavaGen.genToplevel(name, value)
//    }
//    override fun genClass(cls: ClassDef) {
//      this@JavaGen.genClass(cls)
//    }
//    override fun genFunc(func: FunctionDef) {
//      this@JavaGen.genFunc(func).forEach {
//        pw.println("  $it")
//      }
//      pw.println()
//    }
//  }
//
//  fun Path.printStream(): PrintStream = PrintStream(this.toFile().outputStream().buffered(), true)
//  override fun beginModule(name: String, defs: Collection<TopLevelDef>): ModuleRef {
//    val localNames = defs.map { it.name }.toSet()
//    val names = defs.flatMap { gatherNames(it) }
//        .filter { it !in localNames }
//        .filter { TypeResolver.getFullName(it) != it }
//        .filter { "." !in it }
//        .toSet().sorted()
//
//    val currWriter: PrintStream
//    if (name.startsWith("class:")) {
//      val name = name.substring("class:".length)
//      currWriter = basePath.resolve("data").resolve("$name.java").toAbsolutePath().printStream()
//      val pw = currWriter!!
//
//      importedNames.clear()
//      pw.println("package $packageName.data;")
//      pw.println()
//      printImports(pw, names, true)
//    } else if (name == "methods") {
//      importedNames.clear()
//      currWriter = basePath.resolve("Spec.java").toAbsolutePath().printStream()
//      val pw = currWriter!!
//      pw.println("package $packageName;")
//      pw.println()
//      printImports(pw, names)
//      pw.println()
//      pw.println("public class Spec {")
//    } else if (name == "constants") {
//      importedNames.clear()
//      currWriter = basePath.resolve("Constants.java").toAbsolutePath().printStream()
//      val pw = currWriter!!
//      pw.println("package $packageName;")
//      pw.println()
//      printImports(pw, names)
//      pw.println()
//      pw.println("public interface Constants {")
//    } else TODO()
//    currModule = JavaModuleRef(name, currWriter!!)
//    return currModule!!
//  }
//
//  private fun printImports(pw: PrintStream, names: List<String>, lombok: Boolean = false) {
//    if (lombok)
//      pw.println("import lombok.*;")
//    pw.println("import beacon_java.pylib.*;")
//    names.forEach {
//      val fullName = TypeResolver.getFullName(it)
//      importedNames.add(fullName)
//      if (TypeResolver.isConstant(fullName)) {
//        val (mod, name) = splitClassName(fullName)
//        pw.println("import static beacon_java.$mod.Constants.${name};")
//      } else if (TypeResolver.isFunction(fullName)) {
//        val (mod, name) = splitClassName(fullName)
//        pw.println("import static beacon_java.$mod.Spec.${name};")
//      } else
//        pw.println("import ${nameToFullString(fullName)};")
//    }
//    pw.println()
//  }
//}