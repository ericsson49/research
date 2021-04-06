import antlr.PyASTParserLexer
import antlr.PyASTParserParser
import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.times
import com.github.h0tk3y.betterParse.combinators.unaryMinus
import com.github.h0tk3y.betterParse.combinators.use
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

sealed class Item
data class CNumber(val value: Int): Item()
data class CBigNumber(val value: BigInteger): Item()
data class Ident(val name: String): Item()
data class Literal(val lit: String): Item()
data class FCall(val name: String, val params: List<FParam>): Item()

sealed class FParam {
  abstract val value: Item
}
data class NamedParam(val name: Ident, override val value: Item): FParam()
data class VParam(override val value: Item): FParam()

typealias PM = Map<String, Item>

fun fail(e: String? = null): Nothing = throw IllegalArgumentException(e)
fun paramsToMap(params: List<FParam>) = params.map {
  when(it) {
    is NamedParam -> it.name.name to it.value
    is VParam -> fail()
  }
}.toMap()

fun toFCall(v: Item?, name: String? = null): FCall = when(v) {
  is FCall -> {
    if (name != null && v.name != name)
      fail(v.name + " vs " + name)
    else
      v
  }
  else -> fail(v.toString())
}

fun <T> toList(v: Item?, mapper: (Item) -> T): List<T> {
  val f = toFCall(v, "listOf")
  return f.params.map { mapper(it.value) }
}

fun toInt(v: Item?): Int {
  return when(v) {
    is CNumber -> v.value
    else -> fail()
  }
}

fun toNum(v: Item?): Number {
  return when(v) {
    is CNumber -> v.value
    is CBigNumber -> v.value
    else -> fail()
  }
}

fun toStr(v: Item?): String {
  return when(v) {
    is Literal -> v.lit
    else -> fail(v.toString())
  }
}

fun toUnaryop(v: Item?): EUnaryOp {
  val f = toFCall(v)
  return EUnaryOp.valueOf(f.name)
}
fun toOperator(v: Item?): EBinOp {
  val f = toFCall(v)
  return EBinOp.valueOf(f.name)
}

fun toCmpop(v: Item?): ECmpOp {
  val f = toFCall(v)
  return ECmpOp.valueOf(f.name)
}
fun toCmpops(v: Item?) = toList(v, ::toCmpop)

fun toBoolop(v: Item?): EBoolOp {
  val f = toFCall(v)
  return EBoolOp.valueOf(f.name)
}

fun toComprehension(v: Item?): Comprehension {
  val f = toFCall(v, "comprehension")
  val pm = paramsToMap(f.params)
  return Comprehension(target = pm.toExpr("target"), iter = pm.toExpr("iter"), ifs = pm.toExprs("ifs"), is_async = toInt(pm["is_async"]))
}
fun toComprehensions(v: Item?) = toList(v, ::toComprehension)

fun toSlice(v: Item?): TSlice {
  val f = toFCall(v)
  val pm = paramsToMap(f.params)
  return when(f.name) {
    "Slice" -> Slice(lower = toExprOpt(pm["lower"]), upper = toExprOpt(pm["upper"]), step = toExprOpt(pm["step"]))
    "Index" -> Index(value = toExpr(pm["value"]))
    else -> fail(f.name)
  }
}
fun toCtx(v: Item?): ExprContext {
  val f = toFCall(v)
  return ExprContext.valueOf(f.name)
}

fun toIdentifierOpt(v: Item?): identifier? = when(v) {
  is Ident -> if (v.name == "None") null else fail(v.toString())
  else -> toIdentifier(v)
}

fun toIdentifier(v: Item?): identifier = when (v) {
  is Literal -> v.lit.substring(1,v.lit.length-1)
  else -> fail(v.toString())
}
fun toIdentifiers(v: Item?): List<identifier> = toList(v, ::toIdentifier)

fun toConstant(v: Item?): Constant = when(v) {
  is Ident -> {
    if (v.name == "True")
      NameConstant(true)
    else if (v.name == "False")
      NameConstant(false)
    else if (v.name == "None")
      NameConstant(null)
    else if (v.name == "Ellipsis")
      NameConstant("...")
    else
      fail(v.toString())
  }
  else -> fail(v.toString())
}

fun toExprOpt(v: Item?): TExpr? {
  when(v) {
    is Ident -> if (v.name == "None") return null else fail(v.toString())
    is FCall -> return toExpr(v)
    else -> fail(v.toString())
  }
}

fun toExpr(v: Item?): TExpr {
  when(v) {
    is FCall -> {
      val pm = paramsToMap(v.params)
      return when(v.name) {
        "Name" -> Name(toIdentifier(pm["id"]), toCtx(pm["ctx"]))
        "Subscript" -> Subscript(value = toExpr(pm["value"]), slice = toSlice(pm["slice"]), ctx = toCtx(pm["ctx"]))
        "Tuple" -> Tuple(elts = pm.toExprs("elts"), ctx = toCtx(pm["ctx"]))
        "UnaryOp" -> UnaryOp(op = toUnaryop(pm["op"]), operand = pm.toExpr("operand"))
        "BinOp" -> BinOp(left = pm.toExpr("left"), op = toOperator(pm["op"]), right=pm.toExpr("right"))
        "BoolOp" -> BoolOp(op = toBoolop(pm["op"]), values = pm.toExprs("values"))
        "Compare" -> Compare(left = pm.toExpr("left"), ops = toCmpops(pm["ops"]), comparators = pm.toExprs("comparators"))
        "IfExp" -> IfExp(test = pm.toExpr("test"), body = pm.toExpr("body"), orelse = pm.toExpr("orelse"))
        "ListComp" -> ListComp(elt = pm.toExpr("elt"), generators = toComprehensions(pm["generators"]))
        "DictComp" -> DictComp(key = pm.toExpr("key"), value = pm.toExpr("value"), generators = toComprehensions(pm["generators"]))
        "GeneratorExp" -> GeneratorExp(elt = pm.toExpr("elt"), generators = toComprehensions(pm["generators"]))
        "Num" -> Num(toNum(pm["n"]))
        "NameConstant" -> toConstant(pm["value"])
        "Str" -> Str(toStr(pm["s"]))
        "List" -> PyList(elts = pm.toExprs("elts"), ctx = toCtx(pm["ctx"]))
        "Dict" -> PyDict(keys = pm.toExprs("keys"), values = pm.toExprs("values"))
        "Bytes" -> Bytes(toStr(pm["s"]))
        "Call" -> Call(func = pm.toExpr("func"), args = pm.toExprs("args"), keywords = toKeywords(pm["keywords"]))
        "Attribute" -> Attribute(value = pm.toExpr("value"), attr = toIdentifier(pm["attr"]), ctx = toCtx(pm["ctx"]))
        "Lambda" -> Lambda(args = toArguments(pm["args"]), body = pm.toExpr("body"))
        "Starred" -> Starred(value = pm.toExpr("value"), ctx = toCtx(pm["ctx"]))
        "Constant" -> {
          val constValue = pm["value"]
          when(constValue) {
            is Ident -> toConstant(constValue)
            is Literal -> {
              val s = toStr(constValue)
              if (s.startsWith("b")) Bytes(s.substring(1)) else Str(s)
            }
            is CNumber -> Num(toNum(constValue))
            is CBigNumber -> Num(toNum(constValue))
            else -> fail(constValue.toString())
          }
        }
        "Ellipsis" -> NameConstant("...")
        "JoinedStr" -> JoinedStr(values = pm.toExprs("values"))
        "FormattedValue" -> FormattedValue(value = pm.toExpr("value"), format_spec = pm.toExprOpt("format_spec"))
        else -> fail(v.name)
      }
    }
    else -> fail(v.toString())
  }
}
fun toExprs(v: Item?) = toList(v, ::toExpr)

fun toKeyword(v: Item?): Keyword {
  val f = toFCall(v, "keyword")
  val pm = paramsToMap(f.params)
  return Keyword(toIdentifierOpt(pm["arg"]), toExpr(pm["value"]))
}
fun toKeywords(v: Item?) = toList(v, ::toKeyword)

fun toArg(v: Item?): Arg {
  val f = toFCall(v, "arg")
  val pm = paramsToMap(f.params)
  return Arg(arg = toIdentifier(pm["arg"]), annotation = pm.toExprOpt("annotation"))
}
fun toArgOpt(v: Item?): Arg? {
  return when(v) {
    is FCall -> toArg(v)
    is Ident -> if (v.name == "None") null else fail(v.name)
    else -> fail(v.toString())
  }
}
fun toArgs(v: Item?) = toList(v, ::toArg)

fun toArguments(v: Item?): Arguments {
  val f = toFCall(v, "arguments")
  val pm = paramsToMap(f.params)
  return Arguments(
      posonlyargs = pm["posonlyargs"]?.let(::toArgs)?:listOf(), args = toArgs(pm["args"]),
      vararg = toArgOpt(pm["vararg"]), kwonlyargs = toArgs(pm["kwonlyargs"]),
      kw_defaults = pm.toExprs("kw_defaults"), kwarg = toArgOpt(pm["kwarg"]),
      defaults = pm.toExprs("defaults")
  )
}

fun PM.toExpr(k: String) = toExpr(this[k])
fun PM.toExprs(k: String) = toExprs(this[k])
fun PM.toExprOpt(k: String) = toExprOpt(this[k])
fun PM.toStmt(k: String) = toStmt(this[k])
fun PM.toStmts(k: String) = toStmts(this[k])

fun toStmt(v: Item?): Stmt {
  val f = toFCall(v)
  val pm = paramsToMap(f.params)
  return when (f.name) {
    "ClassDef" -> {
      ClassDef(name = toIdentifier(pm["name"]), bases = toExprs(pm["bases"]),
          keywords = toKeywords(pm["keywords"]), body = toStmts(pm["body"]), decorator_list = toExprs(pm["decorator_list"]))
    }
    "FunctionDef" -> {
      FunctionDef(name = toIdentifier(pm["name"]), args = toArguments(pm["args"]),
          decorator_list = pm.toExprs("decorator_list"), body = pm.toStmts("body"),
          returns = pm.toExprOpt("returns"))
    }
    "Assign" -> Assign(targets = pm.toExprs("targets"), value = pm.toExpr("value"))
    "AnnAssign" -> {
      val target = toExpr(pm["target"])
      val annotation = toExpr(pm["annotation"])
      val value = toExprOpt(pm["value"])
      AnnAssign(target = target, annotation = annotation, value = value, simple = toInt(pm["simple"]))
    }
    "AugAssign" -> AugAssign(target = pm.toExpr("target"), op = toOperator(pm["op"]), value = pm.toExpr("value"))
    "Expr" -> Expr(value = pm.toExpr("value"))
    "While" -> While(test = pm.toExpr("test"), body = pm.toStmts("body"), orelse = pm.toStmts("orelse"))
    "For" -> For(target = pm.toExpr("target"), iter = pm.toExpr("iter"), body = pm.toStmts("body"), orelse = pm.toStmts("orelse"))
    "If" -> If(test = pm.toExpr("test"), body = pm.toStmts("body"), orelse = pm.toStmts("orelse"))
    "Assert" -> Assert(test = pm.toExpr("test"), msg = pm.toExprOpt("msg"))
    "Pass" -> Pass()
    "Return" -> Return(value = pm.toExprOpt("value"))
    "Continue" -> Continue()
    "Break" -> Break()
    "Try" -> Try(body = pm.toStmts("body"), handlers = toExceptHandlers(pm["handlers"]), orelse = pm.toStmts("orelse"), finalbody = pm.toStmts("finalbody"))
    "Nonlocal" -> Nonlocal(names = toIdentifiers(pm["names"]))
    "Raise" -> Raise(exc = pm.toExprOpt("exc"), cause = pm.toExprOpt("cause"))
    else -> fail(f.name)
  }
}
fun toStmts(v: Item?) = toList(v, ::toStmt)

fun toExceptHandler(v: Item?): ExceptHandler {
  val f = toFCall(v, "ExceptHandler")
  val pm = paramsToMap(f.params)
  return ExceptHandler(typ = pm.toExpr("type"), name = toIdentifierOpt(pm["name"]), body = pm.toStmts("body"))
}
fun toExceptHandlers(v: Item?) = toList(v, ::toExceptHandler)

object ItemsParser : Grammar<Item>() {
  val NUM by token("\\d+")
  val WORD by token("[A-Z_a-z]+(?!\")")
  val COMMA by token(",\\s+")
  val LPAR by token("\\(")
  val RPAR by token("\\)")
  val EQ by token("=")
  val STRINGLIT by token("\".*?\"")
  val B_STRINGLIT by token("b\".*?\"")


  val litp = (B_STRINGLIT or STRINGLIT) use { Literal(text)}
  val nump = NUM use {
    try {
      CNumber(text.toInt())
    } catch (e: java.lang.NumberFormatException) {
      CBigNumber(text.toBigInteger())
    }
  }
  val ident = WORD use { Ident(text) }

  val namedParam: Parser<FParam> = ident * -EQ * parser {value} map { NamedParam(it.t1, it.t2)}
  val fParam: Parser<FParam> = parser {value} map {VParam(it)}
  val params = separatedTerms(namedParam or fParam , COMMA, true)
  val fcall by ident and -LPAR * params * -RPAR map { FCall(it.t1.name, it.t2) }

  val value: Parser<Item> = fcall or ident or nump or litp

  override val rootParser by fcall
}

object ItemsParser2 {
  fun parseValue(value: PyASTParserParser.ValueContext): Item {
    return when {
      value.NB_STRING_LIT() != null -> Literal(value.NB_STRING_LIT().text)
      value.BSTRING_LIT() != null -> Literal(value.BSTRING_LIT().text)
      value.WORD() != null -> Ident(value.WORD().text)
      value.NUM() != null -> {
        try {
          CNumber(value.NUM().text.toInt())
        } catch(e: NumberFormatException) {
          CBigNumber(value.NUM().text.toBigInteger())
        }
      }
      else -> parseFCall(value.funcCall())
    }
  }

  fun parseParam(param: PyASTParserParser.ParamContext): FParam {
    return if (param.value() != null) {
      VParam(parseValue(param.value()))
    } else {
      NamedParam(Ident(param.namedParam().WORD().text), parseValue(param.namedParam().value()))
    }
  }

  fun parseParamList(paramList: PyASTParserParser.ParamListContext?): List<FParam> {
    return paramList?.param()?.map { parseParam(it) } ?: listOf()
  }

  fun parseFCall(fcall: PyASTParserParser.FuncCallContext): FCall {
    val paramList = fcall.paramList()
    val params = parseParamList(paramList)
    return FCall(fcall.WORD().text, params)
  }

  fun parseToEnd(it: String): Item {
    val str = CharStreams.fromString(it)
    val lexer = PyASTParserLexer(str)
    val ts = CommonTokenStream(lexer)
    val parser = PyASTParserParser(ts)
    return parseFCall(parser.funcCall())
  }
}

sealed class TopLevelDef(val name: String)
data class ConstTLDef(val const: Assign): TopLevelDef((const.targets[0] as Name).id)
data class ClassTLDef(val clazz: ClassDef): TopLevelDef(clazz.name)
data class FuncTLDef(val func: FunctionDef): TopLevelDef(func.name)

fun parseSpecFile(path: Path): Collection<TopLevelDef> {
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  return parsed.map {
    val stmt = toStmt(it)
    when (stmt) {
      is Assign -> ConstTLDef(stmt)
      is ClassDef -> ClassTLDef(stmt)
      is FunctionDef -> FuncTLDef(stmt)
      else -> fail(stmt.toString())
    }
  }
}

fun filterOutDefinitions(defs: Collection<TopLevelDef>, specPhase: String): List<TopLevelDef> {
  val ignoredTopLevels_ = setOf("SSZVariableName", "GeneralizedIndex", "SSZObject", "_hash",
          "_compute_shuffled_index", "compute_shuffled_index", "_get_total_active_balance", "get_total_active_balance",
          "_get_base_reward", "get_base_reward", "_get_committee_count_at_slot", "get_committee_count_at_slot",
          "_get_active_validator_indices", "get_active_validator_indices", "_get_beacon_committee", "get_beacon_committee",
          "_get_matching_target_attestations", "get_matching_target_attestations", "_get_matching_head_attestations", "get_matching_head_attestations",
          "_get_attesting_indices", "get_attesting_indices", "_get_start_shard", "get_start_shard",
          "_get_committee_count_per_slot", "get_committee_count_per_slot"
  )
  val ignoredFuncs_all = setOf("cache_this", "hash", "ceillog2", "floorlog2") // setOf("init_SSZ_types", "apply_constants_preset")
  val ignoredFuncs_altair = if (specPhase == "altair") setOf(
          "get_matching_source_attestations", "get_matching_target_attestations", "get_matching_head_attestations",
          "get_source_deltas", "get_target_deltas", "get_head_deltas",
          "get_inclusion_delay_deltas", "get_attestation_deltas",
          "process_participation_record_updates"
  ) else emptySet()
  val ignoredFuncs = ignoredFuncs_all.union(ignoredFuncs_altair)

  return defs.filter { def ->
    when(def) {
      is ConstTLDef -> def.name !in ignoredTopLevels_
      is FuncTLDef -> def.name !in ignoredFuncs
      else -> true
    }
  }
}

private fun loadSpecDefs(specVersion: String, prefix: String = ""): List<TopLevelDef> {
  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_${specVersion}_dev.txt")
  val defs = filterOutDefinitions(parseSpecFile(path), specVersion)

  for (d in defs) {
    val processor: () -> Unit = {
      when (d) {
        is ConstTLDef -> processTopLevel(d.const)
        is ClassTLDef -> processClass(d.clazz)
        is FuncTLDef -> TypeResolver.registerFunc(d.func)
      }
    }
    TypeResolver.registerDelayed(d.name, processor)
  }
  return defs
}

fun main(args: Array<String>) {
  PyLib.init()
  SSZLib.init()
  BLS.init()

  val specVersion = "phase0"
  val tlDefs = loadSpecDefs(specVersion)

  for (phase in listOf("phase0", "altair")) {
    val defs = loadSpecDefs(specVersion)
    TypeResolver.register(PackageTI(phase,
            defs.map { phase + ":" + it.name to TypeResolver.resolveNameTyp(it.name)!! }))
  }

  val gen = KotlinGen()
  for (d in tlDefs.filterIsInstance<ConstTLDef>()) {
    if (d.name.toList().all { it.toUpperCase() == it })
      println(gen.genToplevel(d.name, d.const.value))
  }
  for (d in tlDefs.filterIsInstance<ClassTLDef>()) {
    gen.genClass(d.clazz)
  }
  for (d in tlDefs.filterIsInstance<FuncTLDef>()) {
    val newF = convertToAndOutOfSSA(transformForOps(d.func))
    gen.genFunc(newF).forEach(::println)
    println()
  }
}

private fun processClass(c: ClassDef) {
  if (c.name in TypeResolver.nameToType)
    return
  if (c.body.size == 1 && c.body[0] is Pass) {
    // register constructor as a func
    if (c.bases.size > 1)
      fail(c.name)
    else {
      val retTyp = NamedType(c.name)

      val argType = parseType(c.bases[0])
      val args = listOf(FArg("value", argType))
      TypeResolver.registerFunc(c.name, args, retTyp)
      TypeResolver.registerFunc(c.name, emptyList(), retTyp)
      if (isSubType(argType, TPyInt)) {
        val typ = TPyInt
        TypeResolver.registerFunc(c.name, listOf(FArg("value", typ)), retTyp)
      }
      if (isSubType(argType, TPyBytes)) {
        val strTyp = TPyStr
        TypeResolver.registerFunc(c.name, listOf(FArg("value", strTyp)), retTyp)
      }
      TypeResolver.register(DataTInfo(c.name, baseType = argType))
    }
  } else {
    if (c.bases.size > 1) fail(c.name)
    val argType = if (c.bases.isEmpty()) TPyObject else parseType(c.bases[0])

    val retTyp = NamedType(c.name)
    val fTypes = c.body.map {
      val annAssign = it as AnnAssign
      val fName = (annAssign.target as Name).id
      val fTyp = parseType(annAssign.annotation)
      fName to fTyp
    }
    val copyFuncName = "${c.name}_copy"

    val fargs = fTypes.map { FArg(it.first, it.second, Name("${c.name}.${it.first}_default", ExprContext.Load)) }
    TypeResolver.registerFunc(c.name, fargs, retTyp)
    TypeResolver.registerFunc(copyFuncName, emptyList(), retTyp)
    TypeResolver.register(FuncRefTI(copyFuncName))
    val attrs2 = fTypes.map { it.first to it.second }.plus("copy" to FuncRefTI(copyFuncName).type)
    TypeResolver.register(DataTInfo(c.name, baseType = argType, attrs = attrs2))
  }
}

private fun processTopLevel(a: Assign) {
  if (a.targets.size != 1) fail(a.targets.toString())
  TypeResolver.registerTopLevelAssign(
          (a.targets[0] as Name).id,
          TypeResolver.topLevelTyper[a.value].asType())
}