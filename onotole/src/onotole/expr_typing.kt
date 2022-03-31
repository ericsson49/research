package onotole

import onotole.type_inference.FAtom
import onotole.type_inference.TypeVarReplacer
import onotole.type_inference.TypingContext
import onotole.type_inference.inferConstTypes
import onotole.type_inference.inferTypes
import onotole.type_inference.inferTypes2
import onotole.type_inference.toFAtom
import onotole.typelib.*

sealed class TypeConstraint
interface InvocationResolver {
  fun resolve(args: List<RTType>, kwdArgs: List<Pair<String, RTType>>): Pair<RTType, List<TypeConstraint>>
}

data class FuncSig(val args: List<Triple<String, RTType, TExpr?>>)
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

data class PkgVTInfo(val elems: List<Pair<String, VTypeInfo>>) : VTypeInfo {
  override val attrs = elems.toMap()
  override val invocationResolver: String? = null
}

data class ClassTemplateVTInfo(val name: String) : VTypeInfo {
  override val attrs = mapOf("__index__" to TODO())
  override val invocationResolver: String? = null
}

data class ClassVTInfo(val template: ClassTemplateVTInfo?, val typeParams: List<Any>, val _attrs: List<Pair<String, VTypeInfo>>) : VTypeInfo {
  override val attrs = _attrs.toMap()
  override val invocationResolver: String? = null
}

data class MethVTInfo(val sigs: List<FuncSig>) : VTypeInfo {
  override val attrs: Map<String, VTypeInfo> = emptyMap()
  override val invocationResolver: String?
    get() = TODO("Not yet implemented")
}

sealed class VType
sealed interface VTModule
data class VTQuote(val expr: TExpr) : VType()
data class VTFun(val args: List<VType>, val res: VType) : VType()
data class VTPkg(val name: String) : VType(), VTModule
data class VTClassTemplate(val name: String, val noTParams: Int, val noQParams: Int) : VType()
data class VTClass(val name: String, val typeParams: List<VType>, val litParams: List<VTQuote>) : VType(), VTModule
data class VTInstance(val cls: VTClass) : VType()
data class VTInstanceField(val value: TExpr, val attr: String) : VType()
data class VTMethTemplate(val name: String, val noTParams: Int, val noLParams: Int, val sigs: List<FuncSig>) : VType()
data class VTMeth(val name: String, val typeParams: List<VType>, val litParams: List<VTQuote>, val sigs: List<FuncSig>) : VType()
sealed interface VTAttrKind
data class VTField(val type: VType) : VTAttrKind
data class VTFunc(val coll: List<Any>) : VTAttrKind
data class VTAttr(val mod: VTModule, val attr: String, val kind: VTAttrKind) : VType()


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
  fun resolveCall(args: List<VType>, kwdArgs: List<Pair<String, VType>>): VType
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
    when (e) {
      is Attribute -> {
        when (val vt = getType(e.value)) {
          is VTPkg -> resolvePkgAttr(vt, e.attr)
          is VTClass -> resolveClassAttr(vt, e.attr)
          is VTInstance -> VTInstanceField(e.value, e.attr)
          else -> TODO()
        }
      }
      is Subscript -> {
        val vt = getType(e.value)
        when (e.slice) {
          is Index -> {
            val elts = if (e.slice.value is Tuple) e.slice.value.elts else listOf(e.slice.value)
            when (vt) {
              is VTClassTemplate -> resolveTemplateIndex(vt, elts.map(::getLiteralType))
              is VTInstance -> resolveInstanceIndex(vt, elts.map(::getType))
              else -> TODO()
            }
          }
          is Slice -> {
            val lower = getType(e.slice.lower ?: NameConstant(null))
            val upper = getType(e.slice.upper ?: NameConstant(null))
            val step = getType(e.slice.step ?: NameConstant(null))
            when (vt) {
              is VTInstance -> resolveInstanceSlice(vt, lower, upper, step)
              else -> TODO()
            }
          }
        }
      }
      is Call -> {
        val funcType = getType(e.func)
        val receiverType = if (funcType is VTClass) resolveCtor(funcType) else funcType
        when (receiverType) {
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
data class LCStrLit(val lit: String) : LCVal()
data class LCTempl(val name: String) : LCVal()
data class LCInst(val template: LCTempl, val params: List<LCVal> = emptyList()) : LCVal()


sealed class LExpr
data class LExtVal(val name: String, val params: List<LExpr> = emptyList()) : LExpr()
data class LQuote(val expr: TExpr) : LExpr()
data class LLit(val lit: String) : LExpr()
data class LVar(val name: String) : LExpr()

sealed class LHandle
data class LOp(val name: String, val typeParams: List<LExpr> = emptyList()) : LHandle()
data class LValHandle(val name: LCVal) : LHandle()
data class LApp(val func: LHandle, val args: List<LExpr>) : LExpr()

fun compileTimeEval(ctx: Map<String, VType>, e: TExpr): LExpr {
  fun getVType(e: TExpr): VType = when (e) {
    is Name -> ctx[e.id]!!
    is Subscript -> {
      val vt = getVType(e.value)
      if (vt is VTClassTemplate) {
        val idx = (e.slice as Index).value
        val idx2 = (if (idx is Tuple) idx.elts else listOf(idx)).map { VTQuote(it) }
        VTClass(vt.name, idx2.subList(0, vt.noTParams), idx2.subList(vt.noTParams, vt.noTParams + vt.noQParams))
      } else {
        TODO()
      }
    }
    else -> TODO()
  }
  return when (e) {
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

fun getVType(ctx: Map<String, VType>, e: TExpr): VType = when (e) {
  is Constant -> VTQuote(e)
  is Name -> ctx[e.id] ?: fail()
  is Attribute -> {
    val vType = getVType(ctx, e.value)
    if (vType.isLiteral) VTQuote(e) else vType.resolver.resolveAttr(e.attr)
  }
  is Subscript -> {
    val vType = getVType(ctx, e.value)
    when (e.slice) {
      is Index -> {
        val indices = if (e.slice.value is Tuple) e.slice.value.elts else listOf(e.slice.value)
        val idxTypes = indices.map { getVType(ctx, it) }
        if (vType.isLiteral && idxTypes.all { it.isLiteral }) VTQuote(e)
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
    fun convert(a: Arg): Pair<String, VType> = a.arg to (a.annotation?.let { getVType(ctx, it) }
        ?: VTQuote(NameConstant(null)))

    val args: Map<String, VType> = e.args.args.map { convert(it) }.toMap()
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

fun getVType(ctx: VTResolver, e: TExpr): VType = when (e) {
  is Constant -> VTQuote(e)
  is Name -> ctx.resolve(e.id) ?: VTQuote(e)
  is Attribute -> {
    val vt = getVType(ctx, e.value)
    when (vt) {
      is VTPkg -> ctx.resolvePkgAttr(vt, e.attr)
      is VTClass -> ctx.resolveStaticClassAttr(vt, e.attr)
      is VTQuote -> VTQuote(e.copy(value = vt.expr))
      else -> fail()
    }
  }
  is Subscript -> {
    val vt = getVType(ctx, e.value)
    when (vt) {
      is VTQuote -> VTQuote(e.copy(value = vt.expr))
      is VTClassTemplate -> {
        val idx = (e.slice as Index).value
        val idx2 = (if (idx is Tuple) idx.elts else listOf(idx))
        val tParams = idx2.subList(0, vt.noTParams).map { getVType(ctx, it) }
        val lParams = idx2.subList(vt.noTParams, vt.noTParams + vt.noQParams).map { VTQuote(it) }
        VTClass(vt.name, tParams, lParams)
      }
      else -> fail()
    }
  }
  is Call -> {
    val recType = getVType(ctx, e.func)
    when (recType) {
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

class CTResolver(val ctx: VTResolver) : SimpleStmtTransformer() {
  val tmpVars = FreshNames()
  fun resolve(e: TExpr): TExpr {
    fun getVType(e: TExpr) = getVType(ctx, e)
    return when (e) {
      is Str -> e
      is Name -> {
        val vt = ctx.resolve(e.id)
        if (vt != null) CTValue(vt) else e
      }
      is Attribute -> {
        val value = resolve(e.value)
        if (value is CTValue) {
          when (val vt = value.vtype) {
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
              val lParams = idx2.subList(vt.noTParams, vt.noTParams + vt.noQParams).map { VTQuote(it) }
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
        when (recType) {
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
              when (it) {
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

  override fun doTransform(s: Stmt): List<Stmt>? = when (s) {
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
  return when (e) {
    is Name -> {
      val vt = getVType(e)
      if (vt is VTQuote) vt.expr else CTValue(vt)
    }
    is Attribute -> {
      val valueType = getVType(e.value)
      when (valueType) {
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
      when (vt) {
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
      when (recType) {
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
            when (it) {
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

class LocalVTResolver(val localVars: Map<String, VType>, val parent: VTResolver) : VTResolver {
  override fun resolve(n: String): VType? {
    return localVars[n] ?: parent.resolve(n)
  }
}

class GlobalVTResolver(val currPkgName: String, val pkgs: Map<String, VTResolver>) : VTResolver {
  override fun resolve(n: String): VType? {
    return if (n in pkgs) {
      VTPkg(n)
    } else {
      pkgs[currPkgName]!!.resolve(n)
    }
  }
}

class PkgVTResolver(val pkg: String, val attrs: Map<String, VTAttr>) : VTResolver {
  override fun resolve(n: String): VType? {
    return attrs[n]
  }
}

class ClassVTResolver(val name: String, val attrs: Map<String, VTAttr>) : VTResolver {
  override fun resolve(n: String): VType? {
    return if (n == "<ctor>") {
      VTAttr(VTClass(name, emptyList(), emptyList()), n, VTFunc(emptyList()))
    } else attrs[n]
  }
}

sealed class CTVal
sealed interface CTLocal
object CTNothing : CTVal()
data class ExTypeVar(val v: String) : CTVal()
data class ConstExpr(val e: TExpr) : CTVal(), CTLocal
data class PkgVal(val pkg: String) : CTVal()
data class ClassTemplate(val clsTempl: TLClassHead) : CTVal()
data class ClassVal(val name: String, val tParams: List<ClassVal> = emptyList(), val eParams: List<ConstExpr> = emptyList()) : CTVal(), CTLocal
data class ClassField(val cls: String, val field: String) : CTVal()
data class FuncTempl(val func: TLFuncDecl) : CTVal()
data class FuncInst(val name: String, val sig: TLSig) : CTVal()

class AliasResolver(val resolveAlias: (String) -> String?) : ExprTransformer<Unit>() {
  override fun merge(a: Unit, b: Unit) = a
  override fun transform(e: TExpr, ctx: Unit, store: Boolean): TExpr {
    if (e is Name) {
      val resolvedName = resolveAlias(e.id) ?: e.id
      if (resolvedName != e.id)
        return e.copy(id = resolvedName)
    }
    return defaultTransform(e, ctx, store)
  }
}

class CompileTimeCalc() : BaseNameTransformer() {
  val freshNames = FreshNames()
  override fun transform(e: TExpr, ctx: TestResolver, store: Boolean): TExpr {
    return when (e) {
      is CTV -> e
      is Constant -> CTV(ConstExpr(e))
      is Name -> ctx.getVal(e.id, store)
      is Attribute -> {
        val v = transform(e.value, ctx, false)
        if (v is CTV) {
          when (v.v) {
            is PkgVal -> transform(Name(v.v.pkg + "." + e.attr, e.ctx), ctx, e.ctx == ExprContext.Store)
            is ClassVal -> Name(v.v.name + "#" + e.attr, e.ctx)
            else -> TODO()
          }
        } else e.copy(value = v)
      }
      is Subscript -> {
        val value = transform(e.value, ctx, store)
        if (value is CTV && value.v is ClassTemplate) {
          val index = if (e.slice is Index) e.slice.value else fail()
          val indices = if (index is Tuple) index.elts else listOf(index)
          val clsTempl = value.v.clsTempl
          val indices2 = indices.map { transform(it, ctx, false) }
          val (tparams, eparams) = if (clsTempl.name == "pylib.Tuple") {
            indices2 to emptyList()
          } else {
            if (indices.size != clsTempl.noTParams + clsTempl.noEParams)
              fail()
            val tparams = indices2.subList(0, clsTempl.noTParams)
            val eparams = indices2.subList(clsTempl.noTParams, clsTempl.noTParams + clsTempl.noEParams)
            tparams to eparams
          }
          if (!tparams.all { it is CTV && it.v is ClassVal }) fail()
          if (!eparams.all { it is CTV && it.v is ConstExpr })
            fail()
          val tparams2 = tparams.map { (it as CTV).v as ClassVal }
          val eparams2 = eparams.map { (it as CTV).v as ConstExpr }
          CTV(ClassVal(clsTempl.name, tparams2, eparams2))
        } else {
          val slice = when (e.slice) {
            is Index -> e.slice.copy(value = transform(e.slice.value, ctx, false))
            is Slice -> {
              val lower = e.slice.lower?.let { transform(it, ctx, false) }
              val upper = e.slice.upper?.let { transform(it, ctx, false) }
              val step = e.slice.step?.let { transform(it, ctx, false) }
              e.slice.copy(lower, upper, step)
            }
            else -> fail()
          }
          e.copy(value = value, slice = slice)
        }
      }
      is UnaryOp -> {
        val operand = transform(e.operand, ctx, false)
        if (operand is CTV) {
          CTV(ConstExpr(e.copy(operand = (operand.v as ConstExpr).e)))
        } else {
          e.copy(operand = if (operand is CTV) (operand.v as ConstExpr).e else operand)
        }
      }
      is BinOp -> {
        val l = transform(e.left, ctx, false)
        val r = transform(e.right, ctx, false)
        if (l is CTV && r is CTV) {
          val l2 = (l.v as ConstExpr).e
          val r2 = (r.v as ConstExpr).e
          CTV(ConstExpr(e.copy(left = l2, right = r2)))
        } else {
          val l2 = if (l is CTV) (l.v as ConstExpr).e else l
          val r2 = if (r is CTV) (r.v as ConstExpr).e else r
          e.copy(left = l2, right = r2)
        }
      }
      is BoolOp -> {
        val values = e.values.map { transform(it, ctx, false) }
        if (values.all { it is CTV }) {
          CTV(ConstExpr(e.copy(values = values.map { ((it as CTV).v as ConstExpr).e })))
        } else {
          e.copy(values = values.map { if (it is CTV) (it.v as ConstExpr).e else it })
        }
      }
      is Compare -> {
        val l = transform(e.left, ctx, false)
        val comps = e.comparators.map { transform(it, ctx, false) }
        if (l is CTV && comps.all { it is CTV }) {
          val l2 = (l.v as ConstExpr).e
          val comps2 = comps.map { ((it as CTV).v as ConstExpr).e }
          CTV(ConstExpr(e.copy(left = l2, comparators = comps2)))
        } else {
          val l2 = if (l is CTV) (l.v as ConstExpr).e else l
          val comps2 = comps.map { if (it is CTV) (it.v as ConstExpr).e else it }
          e.copy(left = l2, comparators = comps2)
        }
      }
      is Call -> {
        val func = transform(e.func, ctx, false)
        val args = e.args.map { transform(it, ctx, false) }
        val kwds = e.keywords.map { it.copy(value = transform(it.value, ctx, false)) }

        fun isConstExprOrClassVal(e: TExpr) = e is CTV && (e.v is ConstExpr || e.v is ClassVal)
        if (func !is CTV && func !is Attribute && !(func is Name && func.id in TypeResolver.specialFuncNames))
          fail()
        if (func is Name && func.id in TypeResolver.specialFuncNames && args.all(::isConstExprOrClassVal)) {
          if (kwds.isNotEmpty()) fail()
          CTV(ConstExpr(e))
        } else {
          val func2 = if (func is CTV && func.v is FuncTempl) {
            val sigs = func.v.func.sigs
            fun matchSig(s: TLSig, noPosArgs: Int, kwds: List<String>): Boolean {
              if (noPosArgs > s.args.size)
                return false
              val kwdArgs = s.args.subList(noPosArgs, s.args.size).map { it.first }
              return kwds.minus(kwdArgs).isEmpty()
            }

            val sig = sigs.find { matchSig(it, e.args.size, e.keywords.map { it.arg!! }) }
                ?: fail()
            val tvRemap = sig.tParams.filterIsInstance<TLTVar>().map { it.name to freshNames.fresh("?$it") }.toMap()
            CTV(FuncInst(func.v.func.name, renameTVars(sig, tvRemap)))
          } else func
          val res = e.copy(func = func2, args = args, keywords = kwds)
//          if (func2 is CTV && func2.v is ClassVal && args.all(::isConstExpr) && kwds.all { isConstExpr(it.value) }) {
//            CTV(ConstExpr(res))
//          } else {
//            res
//          }
          val pureFuncs = setOf("ssz.floorlog2", "ssz.get_generalized_index")
          if (func2 is CTV && func2.v is FuncInst && func2.v.name in pureFuncs
              && args.all(::isConstExprOrClassVal) && kwds.all { isConstExprOrClassVal(it.value) }
          ) {
            CTV(ConstExpr(res))
          } else res
        }
      }
      is PyList -> {
        val tv = freshNames.fresh("?T")
        val tvv = TLTVar(tv)
        val args = e.elts.map { transform(it, ctx) }
        val sig = TLSig(listOf(tvv), args.indices.map { "_$it" to tvv }, TLTClass("pylib.PyList", listOf(tvv)))
        mkCall(CTV(FuncInst("pylib.PyList", sig)), args)
      }
      is PyDict -> {
        val ktvv = freshNames.fresh("?K")
        val ktv = TLTVar(ktvv)
        val vtvv = freshNames.fresh("?V")
        val vtv = TLTVar(vtvv)
        val keys = Tuple(elts = e.keys.map { transform(it, ctx) }, ctx = ExprContext.Load)
        val values = Tuple(elts = e.values.map { transform(it, ctx) }, ctx = ExprContext.Load)
        val argTypes = listOf(
            "keys" to TLTClass("pylib.Sequence", listOf(ktv)),
            "values" to TLTClass("pylib.Sequence", listOf(vtv)))
        val resType = TLTClass("pylib.Dict", listOf(ktv, vtv))
        Call(CTV(FuncInst("pylib.Dict", TLSig(listOf(ktv, vtv), argTypes, resType))), args = listOf(keys, values), keywords = emptyList())
      }
      is Lambda -> {
        e.copy(
            args = e.args.copy(args = e.args.args.map {
              if (it.annotation != null) it
              else it.copy(annotation = CTV(ExTypeVar(freshNames.fresh("?L"))))
            }),
            returns = e.returns ?: CTV(ExTypeVar(freshNames.fresh("?R"))),
            body = transform(e.body, ctx.updated(e.args.args.map { it.arg }))
        )
      }
      else -> defaultTransform(e, ctx, store)
    }
  }
}

class StaticNameResolver() : BaseNameTransformer() {
  override fun transform(e: TExpr, ctx: TestResolver, store: Boolean): TExpr {
    return if (e is Attribute) {
      if (e.value is Name) {
        val id = e.value.id
        val vl = ctx.getVal(id, store)
        if (vl is CTV) {
          when (vl.v) {
            is PkgVal -> Name(vl.v.pkg + "." + e.attr, e.ctx)
            is ClassVal -> Name(vl.v.name + "#" + e.attr, e.ctx)
            is ClassTemplate -> TODO()
            else -> e
          }
        } else e
      } else e
    } else defaultTransform(e, ctx, store)
  }
}

abstract class BaseNameTransformer() : ExprTransformer<TestResolver>() {
  fun findNames(e: TExpr): List<String> = when (e) {
    is Name -> listOf(e.id)
    is Attribute -> emptyList()
    is Subscript -> emptyList()
    is Tuple -> e.elts.flatMap { findNames(it) }
    else -> fail()
  }

  fun processComprehensions(gens: Collection<Comprehension>, ctx: TestResolver): Pair<List<Comprehension>, TestResolver> {
    var currCtx = ctx
    val newGens = mutableListOf<Comprehension>()
    gens.forEach { c ->
      val newIter = transform(c.iter, currCtx)
      currCtx = currCtx.updated(findNames(c.target))
      val newIfs = transform(c.ifs, currCtx)
      newGens.add(c.copy(iter = newIter, ifs = newIfs))
    }
    return newGens to currCtx
  }

  override fun defaultTransform(e: TExpr, ctx: TestResolver, store: Boolean): TExpr {
    return if (e is GeneratorExp) {
      val (gens, newCtx) = processComprehensions(e.generators, ctx)
      super.defaultTransform(e.copy(generators = gens), newCtx, store)
    } else if (e is ListComp) {
      val (gens, newCtx) = processComprehensions(e.generators, ctx)
      super.defaultTransform(e.copy(generators = gens), newCtx, store)
    } else if (e is SetComp || e is DictComp) {
      TODO()
    } else if (e is Lambda) {
      super.defaultTransform(e, ctx.updated(e.args.args.map { it.arg }), store)
    } else super.defaultTransform(e, ctx, store)
  }

  override fun procStmt(s: Stmt, ctx: TestResolver): Pair<Stmt, TestResolver> {
    return when (s) {
      is Assign -> super.procStmt(s, ctx.updated(findNames(s.target)))
      is AnnAssign -> super.procStmt(s, ctx.updated(findNames(s.target)))
      is AugAssign -> super.procStmt(s, ctx.updated(findNames(s.target)))
      is While -> s.copy(test = transform(s.test, ctx), body = procStmts(s.body, ctx).first) to ctx
      is For -> s.copy(
          iter = transform(s.iter, ctx),
          body = procStmts(s.body, ctx.updated(findNames(s.target))).first) to ctx
      else -> super.procStmt(s, ctx)
    }
  }

  override fun merge(a: TestResolver, b: TestResolver): TestResolver {
    if (a.globals !== b.globals) fail()
    return TestResolver(a.globals, a.locals.union(b.locals))
  }
}


val classParseCassInfo = mapOf(
    "List" to TLClassHead("ssz.List", listOf("T", "sz")),
    "ssz.List" to TLClassHead("ssz.List", listOf("T", "sz")),
    "Vector" to TLClassHead("ssz.Vector", listOf("T", "sz")),
    "ssz.Vector" to TLClassHead("ssz.Vector", listOf("T", "sz")),
    "Bitlist" to TLClassHead("ssz.Bitlist", listOf("n")),
    "ssz.Bitlist" to TLClassHead("ssz.Bitlist", listOf("n")),
    "Bitvector" to TLClassHead("ssz.Bitvector", listOf("n")),
    "ssz.Bitvector" to TLClassHead("ssz.Bitvector", listOf("n")),
    "ByteList" to TLClassHead("ssz.ByteList", listOf("n")),
    "ssz.ByteList" to TLClassHead("ssz.ByteList", listOf("n")),
    "ByteVector" to TLClassHead("ssz.ByteVector", listOf("n")),
    "ssz.ByteVector" to TLClassHead("ssz.ByteVector", listOf("n")),
)

fun mkPhaseMod(phase: String): TLModLoader {
  //val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_${phase}_dev.txt")
  //val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  //val defs = parsed.map { toStmt(it) }
  //val modDefs = defsToModDefs(defs)

  val (_, defs) = PhaseInfo.getPhaseDefs(phase, false)

  val modDefs = defs.map { when(it) {
    is ConstTLDef -> TLConstDef(it.name, TLTConst(ConstExpr((desugarExprs(it.const) as Assign).value)))
    is ClassTLDef -> TLClassDef(desugarExprs(it.clazz))
    is FuncTLDef -> TLFuncDef(simplifyFunc(it.func))
  } }

  val phaseDeps = mapOf(
      "phase0" to listOf("ssz", "bls"),
      "altair" to listOf("ssz", "bls", "phase0"),
      "bellatrix" to listOf("ssz", "bls", "phase0", "altair")
  )
  val externs = mapOf(
      "phase0" to listOf(
          parseFuncDecl("get_eth1_data(Eth1Block)->Eth1Data")
      ),
      "bellatrix" to listOf(
          TLConstDecl("EXECUTION_ENGINE", TLTClass("ExecutionEngine", emptyList())),
          parseClassDescr("ExecutionEngine <: object",
              "get_payload" to "(PayloadId)->ExecutionPayload",
              "notify_new_payload" to "(ExecutionPayload)->bool",
              "notify_forkchoice_updated" to "(Hash32,Hash32,Optional[PayloadAttributes])->Optional[PayloadId]"
          ),
          parseFuncDecl("get_pow_block(Hash32)->PowBlock")
      )
  )

  return mkModule(phase, listOf("pylib").plus(phaseDeps[phase]!!), modDefs, externs[phase] ?: emptyList())
}

fun simplifyFunc(f: FunctionDef) = desugarExprs(destructForLoops(destructTupleAssign(transformForEnumerate(transformForOps(f)))))
fun defsToModDefs(defs: Collection<Stmt>): List<TLModDef> {
  return defs.map {
    when (it) {
      is Assign -> TLConstDef((it.target as Name).id, TLTConst(ConstExpr(it.value)))
      is ClassDef -> TLClassDef(it)
      is FunctionDef -> TLFuncDef(simplifyFunc(it))
      else -> TODO()
    }
  }
}

fun main() {
  val ph0Mod = mkPhaseMod("phase0")
  val altairMod = mkPhaseMod("altair")
  val bellatrixMod = mkPhaseMod("bellatrix")
  val modLoaders = listOf(pylib, ssz, bls, ph0Mod, altairMod, bellatrixMod)
  modLoaders.forEach(TopLevelScope::registerModule)
  val modules = modLoaders.map { TopLevelScope.resolveModule(it.name) }
  TypingContext.registerModules(modules)

  val p0Module = TopLevelScope.resolveModule("phase0")
  val constTypes_p0 = inferConstTypes(p0Module.constantDefs.map { it.name to it.value.const.e })
  TypingContext.initConstants(constTypes_p0)
  p0Module.definitions.filterIsInstance<TLFuncDef>().forEach {
    val types = inferTypes2(it.func)
    val res = TypeVarReplacer(types).procStmts(it.func.body, Unit).first
    pyPrintFunc(it.func.copy(body = res))
    println("------------")
  }

  val altairModule = TopLevelScope.resolveModule("altair")
  val constTypes_alt = inferConstTypes(altairModule.constantDefs.map { it.name to it.value.const.e })
  TypingContext.initConstants(constTypes_alt)
  altairModule.definitions.filterIsInstance<TLFuncDef>().forEach {
    val types = inferTypes2(it.func)
    val res = TypeVarReplacer(types).procStmts(it.func.body, Unit).first
    pyPrintFunc(it.func.copy(body = res))
    println("------------")
  }

  val bellatrixModule = TopLevelScope.resolveModule("bellatrix")
  val btxconst_decls = bellatrixModule.constantDecls.map { it.name to it.type.toFAtom(emptyMap()) as FAtom }.toMap()
  TypingContext.initConstants(btxconst_decls)
  val constTypes_btx = inferConstTypes(bellatrixModule.constantDefs.map { it.name to it.value.const.e })
  TypingContext.initConstants(constTypes_btx)
  bellatrixModule.definitions.filterIsInstance<TLFuncDef>().forEach {
    val types = inferTypes2(it.func)
    val res = TypeVarReplacer(types).procStmts(it.func.body, Unit).first
    pyPrintFunc(it.func.copy(body = res))
    println("------------")
  }

///  val names = pkgs.map { it to PKG }
//      .plus(consts.map { it.name to CONST })
//      .plus(classes.map { it.name to CLS(it.head) })
//      .plus(funcs.map { it.name to FUNC(it) }).toMap()

  //val transformer = ModuleTransformer(names)

  //val ttt = vDefs.map { ("phase0." + (it.target as Name).id) to transformer.transform(it.value, transformer.globalCtx) }
  //TypingContext.initConstants(ctt)

//  fDefs.forEach {
//    val fd = desugarExprs(destructForLoops(destructTupleAssign(transformForEnumerate(transformForOps(it)))))
//    pyPrintFunc(fd)
//    println()
//
//    println()
//    val fd2 = transformer.transform(fd)
//    pyPrintFunc(fd2)
//    inferTypes(fd2)
//    println("------")
//  }
}
