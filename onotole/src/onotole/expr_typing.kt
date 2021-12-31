package onotole

import antlr.TypeExprLexer
import antlr.TypeExprParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.nio.file.Files
import java.nio.file.Paths

sealed class TypeConstraint
interface InvocationResolver {
  fun resolve(args: List<RTType>, kwdArgs: List<Pair<String, RTType>>): Pair<RTType, List<TypeConstraint>>
}

data class FuncSig(val args: List<Triple<String,RTType,TExpr?>>)
class SimpleInvocationResolver(val sigs: List<FuncSig>) {
  fun match(sig: FuncSig, noPosArgs: Int, kwdArgs: List<String>) {
    if (noPosArgs > sig.args.size) fail()
    val rest = sig.args.subList(noPosArgs, sig.args.size)
    if (kwdArgs.minus(rest.map { it.first }).isNotEmpty()) fail()
    val defaults = rest.filter { it.first !in kwdArgs }
    if (defaults.count { it.third == null } != 0) fail()
  }
}
/*
fun inferExprTypes(cfg: CFGraphImpl): Map<TExpr, Sort> {
  val bottom = TPyNothing
  fun join(a: RTType, b: RTType): RTType = TODO()
  fun join(ts: Collection<RTType>): RTType = ts.fold(bottom, ::join)
  val exprAnno = IdentityHashMap<TExpr, RTType>()
  val typeVars = mutableMapOf<String, RTType>()
  val varTypes = mutableMapOf<String, RTType>()
  fun freshName(): String = TODO()
  fun freshTypeVar() = TypeVar(freshName())

  fun resolveCall(receiverType: Sort, args: List<TExpr>, kwdArgs: List<Pair<String,TExpr>> = emptyList()): RTType {
    if (receiverType == TPyNothing) {
      return TPyNothing
    }
    when(receiverType) {
      is SpecialFuncRef -> {}
      is NamedFuncRef -> {}
      is Clazz -> {}
      else -> TODO()
    }
  }

  fun calcType(e: TExpr): RTType = when(e) {
    is Constant -> TODO()
    is Name -> varTypes.getOrDefault(e.id, bottom)
    is Call -> {
      if (isParamCall(e)) {
        TODO()
      } else if (isPhiCall(e)) {
        join(e.args.map(::calcType))
      } else if (e.func is Name) {
        if (e.func.id in TypeResolver.boolOps) {
          TPyBool
        } else if (e.func.id in TypeResolver.cmpOps) {
          TPyBool
        } else if (e.func.id == "<${EUnaryOp.Not.name}>") {
          TPyBool
        } else if (e.func.id in TypeResolver.binOps) {
          val a = calcType(e.args[0])
          val b = calcType(e.args[1])
          if (a == TPyNothing || b == TPyNothing)
            TPyNothing
          else {
            if (isSubType(b, a)) {
              TODO()
            } else {
              TODO()
            }
          }
        } else {
          TODO()
        }
      } else if (e.func is Attribute) {
        TODO()
      } else {
        bottom
      }
    }
    is Lambda -> {
      exprAnno.getOrPut(e) { FunType(e.args.args.map { freshTypeVar() }, freshTypeVar()) }
    }
    is PyList -> {
      exprAnno.getOrPut(e) { TPyList(freshTypeVar()) }
    }
    is PySet -> {
      exprAnno.getOrPut(e) { TPySet(freshTypeVar()) }
    }
    is PyDict -> {
      exprAnno.getOrPut(e) { TPyDict(freshTypeVar(), freshTypeVar()) }
    }
    else -> TODO()
  }

}
*/

/*sealed class TV
data class TVar(val name: String): TV()
data class TAtom(val name: String, val args: List<TV>): TV()

data class TConstr(val a: TV, val b: TV)

fun mergeTVs(a: TAtom, b: TAtom): Pair<TAtom, Collection<TConstr>> = TODO()
fun cpa() {
  val constraints = mutableListOf<TConstr>()
  val typing = mutableMapOf<String, TAtom>()
  do {
    var updated = false
    constraints.forEach { c ->
      val r = when (c.a) {
        is TVar -> typing[c.a.name]!!
        is TAtom -> c.a
      }
      val cs = when (c.b) {
        is TVar -> {
          val (m, cs) = mergeTVs(typing[c.b.name]!!, r)
          if (m != typing[c.b.name]) {
            typing[c.b.name] = m
            updated = true
          }
          cs
        }
        is TAtom -> {
          val (m, cs) = mergeTVs(r, c.b)
          if (m != c.b) fail()
          cs
        }
      }
      cs.forEach { c ->
        if (c !in constraints) {
          constraints.add(c)
          updated = true
        }
      }
    }
  } while (updated)
}
 */

interface VTypeInfo {
  val attrs: Map<String, VTypeInfo>
  val invocationResolver: String?
}

/*val int_from_bytes_info = MethVTInfo(listOf(FuncSig(listOf(Triple("a", TPyBytes, null)))))
val int_ctor_info = MethVTInfo(listOf(
    FuncSig(emptyList()),
    FuncSig(listOf(Triple("n", TPyInt, null))),
    FuncSig(listOf(Triple("s", TPyStr, null)))
))
val int_info = ClassVTInfo(null, emptyList(), listOf(
    "from_bytes" to int_from_bytes_info,
    "__new__" to int_ctor_info
))
val int_instance_info = object : VTypeInfo {
  override val attrs: Map<String, VTypeInfo> = mapOf(
      "__add__" to TODO(),
      "__radd__" to TODO()
  )
  override val invocationResolver: String? = null
}

val global = object : VTypeInfo {
  override val attrs: Map<String,VTypeInfo> = mapOf(
      "int" to TODO()
  )
  override val invocationResolver: String? = null
}
*/

data class PkgVTInfo(val elems: List<Pair<String, VTypeInfo>>): VTypeInfo {
  override val attrs = elems.toMap()
  override val invocationResolver: String? = null
}

data class ClassTemplateVTInfo(val name: String): VTypeInfo {
  override val attrs = mapOf("__index__" to TODO())
  override val invocationResolver: String? = null
}

data class ClassVTInfo(val template: ClassTemplateVTInfo?, val typeParams: List<Any>, val _attrs: List<Pair<String,VTypeInfo>>): VTypeInfo {
  override val attrs = _attrs.toMap()
  override val invocationResolver: String? = null
}

data class MethVTInfo(val sigs: List<FuncSig>): VTypeInfo {
  override val attrs: Map<String, VTypeInfo> = emptyMap()
  override val invocationResolver: String?
    get() = TODO("Not yet implemented")
}

sealed class VType
sealed interface VTModule
data class VTQuote(val expr: TExpr): VType()
data class VTFun(val args: List<VType>, val res: VType): VType()
data class VTPkg(val name: String): VType(), VTModule
data class VTClassTemplate(val name: String, val noTParams: Int, val noQParams: Int): VType()
data class VTClass(val name: String, val typeParams: List<VType>, val litParams: List<VTQuote>): VType(), VTModule
data class VTInstance(val cls: VTClass): VType()
data class VTInstanceField(val value: TExpr, val attr: String): VType()
data class VTMethTemplate(val name: String, val noTParams: Int, val noLParams: Int, val sigs: List<FuncSig>): VType()
data class VTMeth(val name: String, val typeParams: List<VType>, val litParams: List<VTQuote>, val sigs: List<FuncSig>): VType()
sealed interface VTAttrKind
data class VTField(val type: VType): VTAttrKind
data class VTFunc(val coll: List<Any>): VTAttrKind
data class VTAttr(val mod: VTModule, val attr: String, val kind: VTAttrKind): VType()


fun classTemplateIndexResolver(template: VTClassTemplate, indices: List<VType>): VType {
  val numOf = template.noTParams
  val numOfLit = template.noQParams
  val typeParams = indices.subList(0, numOf)
  val quotedParams = indices.subList(numOf, numOf + numOfLit).map { it as VTQuote }
  return VTClass(template.name, typeParams, quotedParams)
}

interface TResolver {
  fun resolveAttr(a: String): VType
  fun resolveIndex(indices: List<VType>): VType
  fun resolveSlice(l: VType, u: VType, s: VType): VType
  fun resolveCall(args: List<VType>, kwdArgs: List<Pair<String,VType>>): VType
}

interface Evaluator<C> {
  fun resolvePkgAttr(p: VTPkg, a: identifier): VType = TODO()

  fun resolveTemplateIndex(template: VTClassTemplate, indices: List<VTQuote>): VType = TODO()

  fun resolveClassAttr(cls: VTClass, a: identifier): VType = TODO()
  fun resolveCtor(cls: VTClass): VTMeth = VTMeth(cls.name + ":<ctor>", emptyList(), emptyList(), emptyList())

  fun resolveInstanceAttr(o: VTInstance, a: identifier): VType = TODO()
  fun resolveInstanceIndex(o: VTInstance, indices: List<VType>): VType = TODO()
  fun resolveInstanceSlice(o: VTInstance, lower: VType, upper: VType, step: VType): VType = TODO()
  fun resolveInstanceCall(o: VTInstance): VType = TODO()

  fun getLiteralType(e: TExpr): VTQuote
  fun getType(e: TExpr): VType
  fun eval(c: C, e: TExpr) {
    when(e) {
      is Attribute -> {
        when(val vt = getType(e.value)) {
          is VTPkg -> resolvePkgAttr(vt, e.attr)
          is VTClass -> resolveClassAttr(vt, e.attr)
          is VTInstance -> VTInstanceField(e.value, e.attr)
          else -> TODO()
        }
      }
      is Subscript -> {
        val vt = getType(e.value)
        when(e.slice) {
          is Index -> {
            val elts = if (e.slice.value is Tuple) e.slice.value.elts else listOf(e.slice.value)
            when(vt) {
              is VTClassTemplate -> resolveTemplateIndex(vt, elts.map(::getLiteralType))
              is VTInstance -> resolveInstanceIndex(vt, elts.map(::getType))
              else -> TODO()
            }
          }
          is Slice -> {
            val lower = getType(e.slice.lower ?: NameConstant(null))
            val upper = getType(e.slice.upper ?: NameConstant(null))
            val step = getType(e.slice.step ?: NameConstant(null))
            when(vt) {
              is VTInstance -> resolveInstanceSlice(vt, lower, upper, step)
              else -> TODO()
            }
          }
        }
      }
      is Call -> {
        val funcType = getType(e.func)
        val receiverType = if (funcType is VTClass) resolveCtor(funcType) else funcType
        when(receiverType) {
          is VTMeth -> {
            val coll: FuncCollection = TODO()
            // reorder args
          }
          is VTInstanceField -> {
            val thisType = getType(receiverType.value)
            val handle = resolveInstanceAttr(thisType as VTInstance, receiverType.attr)
            if (e.keywords.isNotEmpty()) fail()
            val args = listOf(receiverType.value).plus(e.args)
          }
          is VTFun -> {

          }
        }
      }
    }
  }
}

sealed class LCVal()
data class LCStrLit(val lit: String): LCVal()
data class LCTempl(val name: String): LCVal()
data class LCInst(val template: LCTempl, val params: List<LCVal> = emptyList()): LCVal()


sealed class LExpr
data class LExtVal(val name: String, val params: List<LExpr> = emptyList()): LExpr()
data class LQuote(val expr: TExpr): LExpr()
data class LLit(val lit: String): LExpr()
data class LVar(val name: String): LExpr()

sealed class LHandle
data class LOp(val name: String, val typeParams: List<LExpr> = emptyList()): LHandle()
data class LValHandle(val name: LCVal): LHandle()
data class LApp(val func: LHandle, val args: List<LExpr>): LExpr()

fun compileTimeEval(ctx: Map<String, VType>, e: TExpr): LExpr {
  fun getVType(e: TExpr): VType = when(e) {
    is Name -> ctx[e.id]!!
    is Subscript -> {
      val vt = getVType(e.value)
      if (vt is VTClassTemplate) {
        val idx = (e.slice as Index).value
        val idx2 = (if (idx is Tuple) idx.elts else listOf(idx)).map { VTQuote(it) }
        VTClass(vt.name, idx2.subList(0, vt.noTParams), idx2.subList(vt.noTParams, vt.noTParams+vt.noQParams))
      } else {
        TODO()
      }
    }
    else -> TODO()
  }
  return when(e) {
    is Name -> {
      val vt = getVType(e)
      if (vt is VTClass) {
        LExtVal(vt.name)
      } else
       TODO()
    }
    is Subscript -> {
      val vt = getVType(e.value)
      if (vt is VTClassTemplate) {
        val vtClass = getVType(e) as VTClass
        val tParams = vtClass.typeParams.map { compileTimeEval(ctx, (it as VTQuote).expr) }
        val eParams = vtClass.litParams.map { LQuote(it.expr) }
        LExtVal(vtClass.name, tParams.plus(eParams))
      } else
        TODO()
    }
    is Call -> {
      val recType = getVType(e.func)
      if (recType is VTClass) {
        val clsHandle = compileTimeEval(ctx, e.func) as LExtVal
        val funHandle = LOp(clsHandle.name + "#<ctor>", clsHandle.params)
        val args = e.args.plus(e.keywords.map { it.value }).map { LQuote(it) }
        LApp(funHandle, args)
      } else {
        TODO()
      }
    }
    else -> TODO("$e")
  }
}

val VType.resolver: TResolver get() = TODO()
val VType.isLiteral: Boolean get() = TODO()

fun getVType(ctx: Map<String,VType>, e: TExpr): VType = when(e) {
  is Constant -> VTQuote(e)
  is Name -> ctx[e.id] ?: fail()
  is Attribute -> {
    val vType = getVType(ctx, e.value)
    if (vType.isLiteral) VTQuote(e) else vType.resolver.resolveAttr(e.attr)
  }
  is Subscript -> {
    val vType = getVType(ctx, e.value)
    when(e.slice) {
      is Index -> {
        val indices = if (e.slice.value is Tuple) e.slice.value.elts else listOf(e.slice.value)
        val idxTypes = indices.map { getVType(ctx, it) }
        if (vType.isLiteral && idxTypes.all { it.isLiteral} ) VTQuote(e)
        else vType.resolver.resolveIndex(idxTypes)
      }
      is Slice -> {
        val lowerType = e.slice.lower?.let { getVType(ctx, it) } ?: VTQuote(NameConstant(null))
        val upperType = e.slice.upper?.let { getVType(ctx, it) } ?: VTQuote(NameConstant(null))
        val stepType = e.slice.step?.let { getVType(ctx, it) } ?: VTQuote(NameConstant(null))
        if (listOf(vType, lowerType, upperType, stepType).all { it.isLiteral })
          VTQuote(e)
        else
          vType.resolver.resolveSlice(lowerType, upperType, stepType)
      }
      else -> TODO()
    }
  }
  is Call -> {
    val recType = getVType(ctx, e.func)
    val argTypes = e.args.map { getVType(ctx, it) }
    val kwdTypes = e.keywords.map { getVType(ctx, it.value) }
    if (listOf(recType).plus(argTypes).plus(kwdTypes).all { it.isLiteral })
      VTQuote(e)
    else
      recType.resolver.resolveCall(argTypes, e.keywords.map { it.arg!! }.zip(kwdTypes))
  }
  is Lambda -> {
    fun convert(a: Arg): Pair<String, VType> = a.arg to (a.annotation?.let { getVType(ctx, it) } ?: VTQuote(NameConstant(null)))
    val args: Map<String,VType> = e.args.args.map { convert(it) }.toMap()
    val newCtx = ctx.plus(args)
    val resType = getVType(newCtx, e.body)
    VTFun(args.map { it.value }, resType)
  }
  else -> TODO()
}

interface VTResolver {
  fun resolve(n: String): VType?
  fun resolvePkgAttr(p: VTPkg, a: identifier): VTAttr = TODO()
  fun resolveStaticClassAttr(c: VTClass, a: identifier): VTAttr = TODO()
  fun resolveFuncCall(fn: VTAttr, args: List<VType>, kwdArgs: List<Pair<String, VType>>): Triple<VType, List<ArgRef>, List<TExpr>> = TODO()
}

fun getVType(ctx: VTResolver, e: TExpr): VType = when(e) {
  is Constant -> VTQuote(e)
  is Name -> ctx.resolve(e.id) ?: VTQuote(e)
  is Attribute -> {
    val vt = getVType(ctx, e.value)
    when(vt) {
      is VTPkg -> ctx.resolvePkgAttr(vt, e.attr)
      is VTClass -> ctx.resolveStaticClassAttr(vt, e.attr)
      is VTQuote -> VTQuote(e.copy(value = vt.expr))
      else -> fail()
    }
  }
  is Subscript -> {
    val vt = getVType(ctx, e.value)
    when(vt) {
      is VTQuote -> VTQuote(e.copy(value = vt.expr))
      is VTClassTemplate -> {
        val idx = (e.slice as Index).value
        val idx2 = (if (idx is Tuple) idx.elts else listOf(idx))
        val tParams = idx2.subList(0, vt.noTParams).map { getVType(ctx, it) }
        val lParams = idx2.subList(vt.noTParams, vt.noTParams+vt.noQParams).map { VTQuote(it) }
        VTClass(vt.name, tParams, lParams)
      }
      else -> fail()
    }
  }
  is Call -> {
    val recType = getVType(ctx, e.func)
    when(recType) {
      is VTPkg -> fail()
      is VTClassTemplate -> fail()
      is VTClass -> {
        // to func
        //Call(CTValue(VTAttr(recType, "<ctor>")), args = e.args, keywords = e.keywords)
        TODO()
      }
      else -> TODO()
    }

  }
  else -> TODO()
}

class CTResolver(val ctx: VTResolver): SimpleStmtTransformer() {
  val tmpVars = FreshNames()
  fun resolve(e: TExpr): TExpr {
    fun getVType(e: TExpr) = getVType(ctx, e)
    return when(e) {
      is Str -> e
      is Name -> {
        val vt = ctx.resolve(e.id)
        if (vt != null) CTValue(vt) else e
      }
      is Attribute -> {
        val value = resolve(e.value)
        if (value is CTValue) {
          when(val vt = value.vtype) {
            is VTPkg -> CTValue(ctx.resolvePkgAttr(vt, e.attr))
            is VTClass -> CTValue(ctx.resolveStaticClassAttr(vt, e.attr))
            else -> TODO()
          }
        } else {
          e.copy(value = value)
        }
      }
      is Subscript -> {
        val value = resolve(e.value)
        if (value is CTValue) {
          when (val vt = value.vtype) {
            is VTPkg -> fail()
            is VTClass -> fail()
            is VTClassTemplate -> {
              val idx = (e.slice as Index).value
              val idx2 = (if (idx is Tuple) idx.elts else listOf(idx))
              val tParams = idx2.subList(0, vt.noTParams).map { getVType(ctx, it) }
              val lParams = idx2.subList(vt.noTParams, vt.noTParams+vt.noQParams).map { VTQuote(it) }
              CTValue(VTClass(vt.name, tParams, lParams))
            }
            else -> TODO()
          }
        } else {
          e.copy(value = value)
        }
      }
      is Call -> {
        val recType = getVType(e.func)
        when(recType) {
          is VTPkg -> fail()
          is VTClassTemplate -> fail()
          is VTClass -> {
            val f = VTAttr(recType, "<ctor>", VTFunc(emptyList()))
            val (resType, argRefs, defaultArgs) = ctx.resolveFuncCall(f,
                e.args.map { getVType(it) }, e.keywords.map { it.arg!! to getVType(it.value) })
            val argDefs = e.args.map {
              val tmp = tmpVars.fresh("tmp_arg_")
              tmp to it
            }
            val kwdDefs = e.keywords.map {
              val tmp = tmpVars.fresh("tmp_kwd_${it.arg!!}")
              tmp to it.value
            }
            argRefs.map {
              when(it) {
                is PositionalRef -> mkName(argDefs[it.idx].first)
                is KeywordRef -> mkName(kwdDefs[it.idx].first)
                is DefaultRef -> defaultArgs[it.idx]
              }
            }
            val args = argDefs.plus(kwdDefs)
            Let(args.map { Keyword(it.first, it.second) }, Call(CTValue(f), args.map { it.second }, emptyList()))
          }
          else -> TODO()
        }
      }
      else -> TODO()
    }

  }

  override fun doTransform(s: Stmt): List<Stmt>? = when(s) {
    is Expr -> listOf(s.copy(value = resolve(s.value)))
    is Assign -> {
      listOf(s.copy(value = resolve(s.value)))
    }
    else -> TODO("$s")
  }
}

fun compileTimeResolver(ctx: VTResolver, e: TExpr): TExpr {
  fun getVType(e: TExpr): VType = getVType(ctx, e)
  val tmpVars = FreshNames()
  return when(e) {
    is Name -> {
      val vt = getVType(e)
      if (vt is VTQuote) vt.expr else CTValue(vt)
    }
    is Attribute -> {
      val valueType = getVType(e.value)
      when(valueType) {
        is VTQuote -> e.copy(value = valueType.expr)
        is VTPkg -> {
          val vt = getVType(e)
          if (vt is VTQuote) CTValue(VTAttr(valueType, e.attr, VTFunc(emptyList()))) else CTValue(vt)
        }
        else -> TODO()
      }
    }
    is Subscript -> {
      val vt = getVType(e.value)
      when(vt) {
        is VTPkg -> fail()
        is VTClass -> fail()
        is VTClassTemplate -> {
          CTValue(getVType(e))
        }
        else -> TODO()
      }
    }
    is Call -> {
      val recType = getVType(e.func)
      when(recType) {
        is VTPkg -> fail()
        is VTClassTemplate -> fail()
        is VTClass -> {
          val f = VTAttr(recType, "<ctor>", VTFunc(emptyList()))
          val (resType, argRefs, defaultArgs) = ctx.resolveFuncCall(f,
              e.args.map { getVType(it) }, e.keywords.map { it.arg!! to getVType(it.value) })
          val argDefs = e.args.map {
            val tmp = tmpVars.fresh("tmp_arg_")
            tmp to it
          }
          val kwdDefs = e.keywords.map {
            val tmp = tmpVars.fresh("tmp_kwd_${it.arg!!}")
            tmp to it.value
          }
          argRefs.map {
            when(it) {
              is PositionalRef -> mkName(argDefs[it.idx].first)
              is KeywordRef -> mkName(kwdDefs[it.idx].first)
              is DefaultRef -> defaultArgs[it.idx]
            }
          }
          val args = argDefs.plus(kwdDefs)
          Let(args.map { Keyword(it.first, it.second) }, Call(CTValue(f), args.map { it.second }, emptyList()))
        }
        else -> TODO()
      }
    }
    else -> TODO()
  }
}

class LocalVTResolver(val localVars: Map<String, VType>, val parent: VTResolver): VTResolver {
  override fun resolve(n: String): VType? {
    return localVars[n] ?: parent.resolve(n)
  }
}
class GlobalVTResolver(val currPkgName: String, val pkgs: Map<String, VTResolver>): VTResolver {
  override fun resolve(n: String): VType? {
    return if (n in pkgs) {
      VTPkg(n)
    } else {
      pkgs[currPkgName]!!.resolve(n)
    }
  }
}

class PkgVTResolver(val pkg: String, val attrs: Map<String, VTAttr>): VTResolver {
  override fun resolve(n: String): VType? {
    return attrs[n]
  }
}

class ClassVTResolver(val name: String, val attrs: Map<String, VTAttr>): VTResolver {
  override fun resolve(n: String): VType? {
    return if (n == "<ctor>") {
      VTAttr(VTClass(name, emptyList(), emptyList()), n, VTFunc(emptyList()))
    } else attrs[n]
  }
}
fun main() {
  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_phase0_dev.txt")
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  val defs = parsed.map { toStmt(it) }
  val vDefs = defs.filterIsInstance<Assign>()
  val cDefs = defs.filterIsInstance<ClassDef>()
  val fDefs = defs.filterIsInstance<FunctionDef>()

  val phase0Pkg = VTPkg("phase0")
  fun typeToVType(e: TExpr): VType = TODO()

  val tlAttrs = vDefs.map {
    val v = (it.target as Name).id
    VTAttr(phase0Pkg, v, VTField(VTQuote(it.value)))
  }

  fDefs.forEach {
    val fd = destructForLoops(destructTupleAssign(transformForEnumerate(transformForOps(it))))
    pyPrintFunc(fd)
    println()
    val vtr = object : VTResolver {
      override fun resolve(n: String): VType? {
        TODO("Not yet implemented")
      }
      override fun resolvePkgAttr(p: VTPkg, a: identifier): VTAttr = TODO()

    }
    CTResolver(vtr).transform(fd)
  }
}

fun _main() {
  val tt = "max(a: Seq[A], key: (A)->B) -> A, B <: Comp[B]"
  val str = CharStreams.fromString(tt)
  val lexer = TypeExprLexer(str)
  val ts = CommonTokenStream(lexer)
  val parser = TypeExprParser(ts)
  val r = parser.funDecl()
  println()
}