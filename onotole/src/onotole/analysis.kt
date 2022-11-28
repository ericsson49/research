package onotole

import onotole.lib_defs.FunDecl
import onotole.type_inference.FAtom
import onotole.type_inference.FTerm
import onotole.type_inference.TypingContext
import onotole.type_inference.getAncestorByClassName
import onotole.type_inference.resolveAttributeGet
import onotole.type_inference.tryCheckST
import onotole.type_inference.tryJoin
import onotole.util.toClassVal
import onotole.util.toFAtom
import onotole.util.toTExpr
import java.util.*

fun canBeCoercedTo(a: RTType, b: RTType): Boolean = when {
  isSubType_new(b, NamedType("ssz.boolean")) && isSubType_new(a, TPyBool) -> true
  b == TPyBool && a.typeInfo.coercibleToBool -> true
  b is NamedType && (b.name == "ssz.List" || b.name == "ssz.Vector") && isSubType_new(a, TPyIterable(b.tParams[0])) -> true
  isSubType_new(b, NamedType("ssz.uint")) && isSubType_new(a, TPyInt) -> true
  isSubType_new(b , TPyBytes) && isSubType_new(a, TPyBytes) -> true
  else -> false
}
fun canAssignOrCoerceTo(a: RTType, b: RTType) = isSubType_new(a, b) || canBeCoercedTo(a, b)

fun isSubType_new(a: RTType, b: RTType): Boolean {
  return SortTypeCalculator.isSubType(a, b)
}

class FuncCollection(funcs: List<FunSignature> = emptyList()) {
  val funcs = funcs.toMutableList()

  fun addFunc(args: List<FArg>, retType: RTType) {
    funcs.add(FunSignature(args, retType))
  }
  fun matches(ctx: NameResolver<Sort>, fp: List<FArg>, args: List<RTType>, kwdArgs: List<Pair<String,RTType>>): List<ArgRef>? {
    val slots = Array<ArgRef?>(fp.size) { null }
    for (i in args.indices) {
      if (i >= fp.size)
        return null
      else {
        val fpt = fp[i].type
        val fpt2 = if (fpt is TypeVar && ctx.contains(fpt.name))
          ctx.resolve(fpt.name)!!.asType()
        else
          fpt
        if (fpt2 is TypeVar)
          unifEx(fpt2, args[i])
        else if (!canAssignOrCoerceTo(args[i], fpt2))
          return null
        else
          slots[i] = PositionalRef(i)
      }
    }
    val consumedKwds = mutableSetOf<String>()
    for(i in kwdArgs.indices) {
      val (name, arg) = kwdArgs[i]
      val idx = fp.indexOfFirst { it.name == name }
      if (slots[idx] != null)
        return null
      if (!canAssignOrCoerceTo(arg, fp[idx].type))
        return null
      slots[idx] = KeywordRef(i)
      consumedKwds.add(name)
    }
    val offset = fp.indexOfFirst { it.default != null }
    val res = mutableListOf<ArgRef>()
    for(i in slots.indices) {
      val slot = slots[i]
      if (slot == null) {
        if (fp[i].default == null)
          return null
        res.add(DefaultRef(i - offset))
      } else {
        res.add(slot)
      }
    }
    if (kwdArgs.toMap().keys.subtract(consumedKwds).isNotEmpty())
      return null
    return res
  }
  fun resolve(ctx: NameResolver<Sort>, args: List<RTType>, kwdArgs: List<Pair<String,RTType>> = emptyList()): RTType? =
      resolveFP(ctx, args, kwdArgs)?.first?.retType

  fun resolveFP(ctx: NameResolver<Sort>, args: List<RTType>, kwdArgs: List<Pair<String,RTType>> = emptyList()): Pair<FunSignature,List<ArgRef>>? {
    for(f in funcs) {
      val match = matches(ctx, f.args, args, kwdArgs)
      if (match != null)
        return Pair(f,match)
    }
    return null
  }
}

object TypeResolver {
  val pylibAliases = mutableMapOf<String,String>()
  val consts = mutableMapOf<String, RTType>()
  val packages = mutableMapOf<String, Map<String, Sort>>()
  val metaclasses = mutableMapOf<String, MetaClass>()
  val nameToType = mutableMapOf<String, Clazz>()
  val funcSigs = mutableMapOf<String, FuncCollection>()
  val funcResolvers = mutableMapOf<String, (NameResolver<Sort>, List<RTType>, List<Pair<String, RTType>>) -> RTType>()
  val typeInfos = mutableMapOf<Sort, TypeInfo>()

  val packageStack = mutableListOf<Pair<String,MutableMap<String,String>>>("" to mutableMapOf())
  val aliases get() = packageStack[packageStack.size-1].second
  fun makeAliases(pkg: String, names: List<String>) {
    names.forEach {
      aliases[it] = "$pkg.$it"
    }
  }
  fun importFromPackage(name: String) {
    makeAliases(name, packages[name]!!.map { it.key })
  }

  fun pushPkg(pkg: String) {
    packageStack.add(pkg to mutableMapOf())
  }
  fun popPkg() = if (packageStack.isEmpty()) fail() else packageStack.removeAt(packageStack.size-1)

  val topLevelResolver: NameResolver<Sort>
    get() = object : NameResolver<Sort> {
      override val keys: Set<String>
        get() = consts.keys.union(funcResolvers.keys).union(nameToType.keys).union(funcSigs.keys)
                .union(metaclasses.keys).union(packages.keys).union(delayedDefinitions.keys)
                .union(aliases.keys).union(specialFuncNames)

      override fun resolve(name: identifier) = resolveNameTyp(name)
    }

  val delayedDefinitions = mutableMapOf<String,()->Unit>()

  fun registerDelayed(n: String, def: ()->Unit) {
    if (n in delayedDefinitions)
      fail("Already delayed")
    delayedDefinitions[n] = def
  }

  fun isConstant(name: String) = name in consts
  fun isFunction(name: String) = resolveNameTyp(name) is NamedFuncRef

  fun getFullName(name: String) = aliases[name] ?: name

  fun resolveNameTyp(name: String): Sort? {
    if (name in pylibAliases)
      return resolveNameTyp(pylibAliases[name]!!)
    if (name in aliases)
      return resolveNameTyp(aliases[name]!!)
    val res = if (name in specialFuncNames) {
      SpecialFuncRef(name)
    } else if (name in metaclasses) {
      metaclasses[name]
    } else if (name in nameToType) {
      nameToType[name]
    } else if (name in consts) {
      consts[name]
    } else if (name in funcResolvers) {
      NamedFuncRef(name)
    } else if (name in funcSigs) {
      NamedFuncRef(name)
    } else if (name in packages) {
      PackageRef(name)
    } else {
      null
    }
    if (res != null && name in delayedDefinitions) {
      delayedDefinitions.remove(name)
    }
    return if (res == null && name in delayedDefinitions) {
      delayedDefinitions[name]!!()
      delayedDefinitions.remove(name)
      resolveNameTyp(name)
    } else {
      res
    }
  }

  fun registerPackage(name: String, attrs: Map<String, Sort>) {
    packages[name] = attrs
  }

  fun registerFunc(fd: FunDecl) {
    registerFunc(fd.name, fd.args, fd.retType)
  }

  fun registerFunc(name: identifier, args: List<FArg>, retType: RTType) {
    funcSigs.getOrPut(name) { FuncCollection() }.addFunc(args, retType)
    register(FuncRefTI(name))
  }

  fun registerTopLevelAssign(name: String, type: RTType) {
    consts[name] = type
  }

  val topLevelTyper: ExprTyper get() = ExprTypes(topLevelResolver, IdentityHashMap())

  fun registerFuncResolver(funcRef: String, resolver: (List<RTType>) -> RTType) {
    funcResolvers[funcRef] = { _, args, kwdArgs ->
      if (kwdArgs.isNotEmpty()) fail()
      resolver(args)}
    register(FuncRefTI(funcRef))
  }

  fun registerFuncResolver(funcRef: String, resolver: (List<RTType>, List<Pair<String,RTType>>) -> RTType) {
    funcResolvers[funcRef] = { _, arg, kwdArgs ->
      resolver(arg, kwdArgs)
    }
    register(FuncRefTI(funcRef))
  }

  @JvmName("registerFuncResolver1")
  fun registerFuncResolver(funcRef: String, resolver: (NameResolver<Sort>, List<RTType>) -> RTType) {
    funcResolvers[funcRef] = { ctx, arg, _ ->
      resolver(ctx, arg)
    }
    register(FuncRefTI(funcRef))
  }

  fun register(ti: TypeInfo) {
    val type = ti.type
    if (type is NamedType && type.tParams.isNotEmpty())
      fail("Parameterized NamedType $type shouldn't be registered directly")
    if (type is NamedType && type.tParams.isEmpty()) {
      if (type.name in nameToType)
        fail("Already registered ${type.name}")
      if (type != TPyObject && type != TPyNothing) {
        (ti.base?.type as? NamedType)?.name
                ?: fail("No superclass for ${type.name}")
      }
      nameToType[type.name] = Clazz(type.name)
    }
    if (type is MetaClass) {
      metaclasses[type.name] = MetaClass(type.name)
    }
    typeInfos[type] = ti
  }

  val typeInfoCache = mutableMapOf<Sort, TypeInfo>()
  fun getTypeInfo(type: Sort): TypeInfo = typeInfoCache.getOrPut(type) { _getTypeInfo(type) }
  fun _getTypeInfo(type: Sort): TypeInfo {
    return when {
      type is NamedType && type.tParams.isNotEmpty() -> {
        val metaClass = resolveNameTyp(type.name)
        if (metaClass == null || metaClass !is MetaClass)
          fail("Unsupported class ${type.name}")
        metaClass.instantiate(type.tParams.map {(it as NamedType).`class` })
      }
      type is NamedType && type.tParams.isEmpty() && type.name in metaclasses -> {
        metaclasses[type.name]!!.instantiate(emptyList())
      }
      type is PartiallyAppliedFuncRef -> {
        FuncRefTI(type.func, type.params)
      }
      type is FunType -> AnonymousFunTI(type)
      else -> {
        if (type !in typeInfos)
          fail("unsupported type $type")
        typeInfos[type]!!
      }
    }
  }

  //val specialFuncs = setOf("list", "dict")
  val binOps = EBinOp.values().map { it.name }
  val boolOps = EBoolOp.values().map { it.name }
  val cmpOps = ECmpOp.values().map { it.name }
  val unaryOp = EUnaryOp.values().map { it.name }
  val specialFuncNames = binOps.plus(boolOps).plus(cmpOps).plus(unaryOp).map { "<$it>" }.toSet()

  fun resolveSpecialCallType(name: String, args: List<RTType>): RTType {
    val op = name.substring(1,name.length-1)
    return when(op) {
      "list" -> resolveListType(args)
      "dict" -> resolveDictType(args)
      in binOps -> if (args.size == 2) resolveBinopType(args[0], EBinOp.valueOf(op), args[1])!! else fail()
      in boolOps -> resolveBoolOp(EBoolOp.valueOf(op), args)
      in unaryOp -> if (args.size == 1) resolveUnaryOp(EUnaryOp.valueOf(op), args[0]) else fail()
      in cmpOps -> if (args.size == 2) resolveCompOpType(args[0], ECmpOp.valueOf(op), args[1]) else fail()
      else -> fail("Unknown op $name")
    }
  }

  fun resolveListType(args: List<RTType>) =
          if (args.isEmpty()) TPyList(NamedType("?")) else TPyList(getCommonSuperType(args))

  fun resolveDictType(args: List<RTType>): RTType {
    val (keyTypes, valTypes) = args.map {
      if (it is NamedType && it.name == "Tuple" && it.tParams.size == 2) {
        it.tParams[0] to it.tParams[1]
      } else fail("parameter should be Tuple(Key,Value)")
    }.unzip()

    val keyType = if (keyTypes.isEmpty()) NamedType("?")
    else getCommonSuperType(keyTypes)
    val valType = if (valTypes.isEmpty()) NamedType("?")
    else getCommonSuperType(valTypes)

    return TPyDict(keyType, valType)
  }

  fun resolveBinopType(x: RTType, op: EBinOp, y: RTType): RTType? {
    return if (isSubType(x, NamedType("ssz.uint")) && isSubType(y, TPyInt)) {
      if (op in setOf(EBinOp.Add, EBinOp.Sub, EBinOp.Mult, EBinOp.FloorDiv, EBinOp.Mod,
                      EBinOp.BitAnd, EBinOp.BitOr, EBinOp.BitXor, EBinOp.LShift, EBinOp.RShift))
        x
      else if (op in setOf(EBinOp.Pow))
        TPyInt
      else
        null
    } else if (isSubType(x, TPyInt) && isSubType(y, TPyInt)) {
      if (op in setOf(EBinOp.Add, EBinOp.Sub, EBinOp.Mult, EBinOp.FloorDiv, EBinOp.Mod,
              EBinOp.BitAnd, EBinOp.BitOr, EBinOp.BitXor))
        TPyInt
      else if (op in setOf(EBinOp.Pow, EBinOp.LShift, EBinOp.RShift))
        TPyInt
      else
        null
    } else if (isSubType(x, NamedType("phase0.BLSPubkey")) && isSubType(y, NamedType("phase0.BLSPubkey"))) {
      if (op == EBinOp.Add)
        x
      else
        null
    } else if (isSubType(x, TPyBytes)) {
      if (isSubType(y, TPyBytes) && op == EBinOp.Add) {
        TPyBytes
      } else if (isSubType(y, TPyInt) && op == EBinOp.Mult) {
        x
      } else {
        null
      }
    } else if (x is NamedType && isGenType(x) && (x.name == "PyList" || x.name == "ssz.List")) {
      if (y is NamedType && isGenType(y) && y.name == x.name && op == EBinOp.Add) {
        x
      } else if (isSubType(y, TPyInt) && op == EBinOp.Mult) {
        x
      } else {
        null
      }
    } else {
      null
    }
  }

  fun resolveBoolOp(op: EBoolOp, args: List<RTType>): RTType {
    args.forEach {
      if (!canAssignOrCoerceTo(it, TPyBool))
        fail("cannot coerce $it to bool")
    }
    //if (e.values.any { !getExprType(it, ctx).typeInfo!!.coercableToBool })
    //  fail("cannot coerce to bool")
    return TPyBool
  }

  fun resolveCompOpType(l: RTType, op: ECmpOp, r: RTType): RTType {
    val lti = l.typeInfo!!
    val rti = r.typeInfo!!
    val compatible = when (op) {
      ECmpOp.Eq, ECmpOp.NotEq -> {
        true
      }
      ECmpOp.Is, ECmpOp.IsNot -> {
        isSubType(l, r) || isSubType(r, l) || l == TPyNone || r == TPyNone
      }
      ECmpOp.Gt, ECmpOp.GtE, ECmpOp.Lt, ECmpOp.LtE -> {
        isSubType(l, r) && rti.comparable || isSubType(r, l) && lti.comparable
      }
      ECmpOp.In, ECmpOp.NotIn -> {
        isSubType(r, TPyCollection(l))
      }
    }
    if (!compatible) fail("incompatible args: $l $op $r")
    return TPyBool
  }

  fun resolveUnaryOp(op: EUnaryOp, type: RTType): RTType =
          if (isSubType(type, TPyInt) && (op == EUnaryOp.UAdd || op == EUnaryOp.USub)) {
            TPyInt
          } else if (canAssignOrCoerceTo(type, TPyBool) && op == EUnaryOp.Not) {
            TPyBool
          } else {
            TODO()
          }

  fun resolveSubscriptType(type: Sort, indices: List<Sort>): Sort {
    return when (type) {
      is NamedType -> {
        when {
          isGenType(type) -> getMapLikeValueElemTyp(type)
          isSimpleType(type) ->
            if (type.name in listOf("Bytes32", "bytes"))
              TPyInt
            else
              getMapLikeValueElemTyp(type)
          else -> fail()
        }
      }
      is MetaClass -> {
        (type.instantiate(indices).type as NamedType).`class`
      }
      else -> TODO()
    }
  }

  fun resolveSubscriptType(typ: Sort, lower: Sort?, upper: Sort?, step: Sort?): NamedType {
    val args = listOf(typ)
        .plus(lower?.let { listOf(it) } ?: emptyList())
        .plus(upper?.let { listOf(it) } ?: emptyList())
        .plus(step?.let { listOf(it) } ?: emptyList())
    if (TPyNothing in args)
      return TPyNothing
    return when {
      typ is RTType && isSubType(typ, TPyBytes) -> TPyBytes
      typ is RTType /*&& isGenType(typ)*/ -> TPySequence(getSeqElemTyp(typ))
      else -> TODO()
    }
  }

  fun resolveAttrType(typ: Sort, attr: String): Sort {
    return when (typ) {
      is Clazz -> {
        val attrType = typ.toInstance().typeInfo.getAttr(attr)?.type
        if (attrType == null || attrType !is NamedFuncRef || !attrType.name.startsWith(typ.name + "."))
          fail()
        attrType
      }
      is NamedType -> {
        when {
          typ == TPyNothing -> TPyNothing
          isSimpleType(typ) -> typ.typeInfo.getAttr(attr)?.type
                  ?: fail("cannot resolve attribute `$attr`")
          isGenType(typ) -> {
            if (typ.name == "Optional") {
              return resolveAttrType(typ.tParams[0], attr)
            } else {
              val ancestors = getNamedAncestors(typ)
              for (clsName in ancestors.keys) {
                val t = ancestors[clsName]!!
                val ti = t.typeInfo
                val attrType = ti.getAttr(attr)?.type
                if (attrType != null && attrType is NamedFuncRef) {
                  return PartiallyAppliedFuncRef(attrType, listOf(typ.asType()))
                }
              }
              fail(typ.toString())
            }
          }
          else -> fail()
        }
      }
      is PackageRef -> packages[typ.name]?.get(attr)
              ?: fail("Unknown ${typ.name}.$attr")
      else -> fail("Attributes are not supported for $typ")
    }
  }

  fun resolveReturnType(ctx: NameResolver<Sort>, callable: Sort, argTypes: List<RTType>, kwdArgs: List<Pair<String, RTType>>): Pair<FunSignature,List<ArgRef>> {
    if (TPyNothing in listOf(callable).plus(argTypes).plus(kwdArgs.map { it.second })) {
      val retType = TPyNothing
      val sig = FunSignature(argTypes.mapIndexed { i,t -> FArg("_$i", t) }, retType)
      return Pair(sig, List(argTypes.size) { i -> PositionalRef(i) })
    }
    return when (callable) {
      is SpecialFuncRef -> {
        if (kwdArgs.isNotEmpty()) TODO()
        val retType = resolveSpecialCallType(callable.name, argTypes)
        val sig = FunSignature(argTypes.mapIndexed { i,t -> FArg("_$i", t) }, retType)
        Pair(sig, List(argTypes.size) { i -> PositionalRef(i) })
      }
      is NamedFuncRef -> {
        val name = callable.name
        if (name in funcSigs) {
          val sig = funcSigs[name]!!
          sig.resolveFP(ctx, argTypes, kwdArgs)
                  ?: fail("no function found for $name(${(argTypes+kwdArgs).joinToString(", ")})")
        } else {
          val retType = funcResolvers[name]?.invoke(ctx, argTypes, kwdArgs) ?: fail("not found $name")
          val sig = FunSignature(argTypes.mapIndexed { i,t -> FArg("_$i", t) }
                  .plus(kwdArgs.map { (a,t) -> FArg(a, t) }), retType
          )
          val argRefs = List(argTypes.size) { i -> PositionalRef(i) }
                  .plus(List(kwdArgs.size) { i -> KeywordRef(i) })
          Pair(sig, argRefs)
        }
      }
      is PartiallyAppliedFuncRef -> {
        val (fh, argRefs) = callable.func.resolveReturnType(ctx, callable.params + argTypes, kwdArgs)
        val newRefs = argRefs.subList(callable.params.size, argRefs.size).map {
          if (it is PositionalRef) it.copy(idx = it.idx - callable.params.size) else it
        }
        fh to newRefs
      }
      is Clazz -> {
        funcSigs[callable.name]?.resolveFP(ctx, argTypes, kwdArgs)// ?: fail("no function found for ${callable.name}($argTypes,$kwdArgs)")
            ?: (FunSignature(argTypes.mapIndexed { i, a -> FArg("_$i", a) }, callable.toInstance()) to argTypes.indices.map { PositionalRef(it) })
      }
      is FunType -> {
        if (kwdArgs.isNotEmpty())
          fail("keyword args are not supported")
        val sig = FunSignature(callable.argTypes.mapIndexed { i,t -> FArg("_$i", t) }, callable.retType)
        val coll = FuncCollection(listOf(sig))
        coll.resolveFP(ctx, argTypes, kwdArgs)
            ?: fail()
      }
      else -> TODO()
    }
  }
}

val Sort.typeInfo get() = TypeResolver.getTypeInfo(this)
fun Sort.resolveAttrType(attr: String) = TypeResolver.resolveAttrType(this, attr)
fun Sort.resolveReturnType(ctx: NameResolver<Sort>, argTypes: List<RTType>, kwdArgs: List<Pair<String,RTType>>): Pair<FunSignature,List<ArgRef>> {
  val recType = if (this is NamedType) this.`class` else this
  return TypeResolver.resolveReturnType(ctx, recType, argTypes, kwdArgs)
}
fun MetaClass.instantiate(tps: List<Sort>): TypeInfo {
  val ti = typeInfo as MetaClassTInfo
  val nTParams = ti.nTParams ?: tps.size
  if (tps.size < nTParams || tps.size > nTParams + ti.nEParams)
    fail("Wrong amount of types")
  val tParams = tps.subList(0, nTParams).map { if (it is Clazz) it.toInstance() else if (it is NamedType) it else TODO() }
  if (tParams.size != nTParams)
    fail("wrong type parameters $tps")
  return DataTInfo(ti.name, tParams, baseType = ti.baseClassF(tParams), attrs = ti._attrs)
}

fun parseSort(typer: ExprTyper, t: TExpr): Sort {
  return if (t == NameConstant(null)) TPyNone
  else typer[t]
}
fun parseType(typer: ExprTyper, t: String): RTType = parseType(typer, Name(t, ExprContext.Load))
fun parseType(typer: ExprTyper, t: TExpr): RTType {
  return when(val res = parseSort(typer, t)) {
    is NamedType -> res
    is FunType -> res
    is Clazz -> res.toInstance()
    is MetaClass -> res.toInstance()
    else -> TODO()
  }
}

fun getFArg(typer: ExprTyper, a: Arg, default: TExpr?) = FArg(a.arg, parseType(typer, a.annotation!!), default)

fun getFArgs(typer: ExprTyper, f: FunctionDef): List<FArg> {
  return getFunArgs(f).map { getFArg(typer, it.first, it.second) }
}

fun getFunArgs(f: FunctionDef): List<Pair<Arg, TExpr?>> {
  val args = f.args
  if (args.posonlyargs.isNotEmpty())
    fail("posonlyargs is not yet supported")
  if (args.kwonlyargs.isNotEmpty())
    fail("kwonlyargs is not yet supported")
  if (args.kw_defaults.isNotEmpty())
    fail("kw_defaults is not yet supported")
  if (args.vararg != null)
    fail("vararg is not yet supported")
  if (args.kwarg != null)
    fail("kwarg is not yet supported")

  val defaults = List(args.args.size - args.defaults.size) { null }.plus(args.defaults)
  val zip = args.args.zip(defaults)
  return zip
}

fun Clazz.toInstance() = NamedType(this.name, this.tParams)
fun MetaClass.toInstance(): NamedType {
  val res = instantiate(emptyList()).type
  if (res !is NamedType)
    fail()
  return res
}
val NamedType.`class` get() = Clazz(this.name, this.tParams)

fun Sort.asType(): RTType = when(this) {
  is RTType -> this
  else -> fail("")
}

interface ExprTyper {
  val ctx: NameResolver<Sort>
  operator fun contains(n: String): Boolean = n in ctx
  operator fun get(e: TExpr): Sort
  fun updated(ctx: NameResolver<Sort>): ExprTyper
  fun updated(vars: Collection<Pair<String, Sort>>): ExprTyper = updated(vars.toMap())
  fun updated(vars: Map<String, Sort>): ExprTyper = updated(ctx.copy(vars))
  fun forLambda(lam: Lambda): ExprTyper {
    return this.updated(lam.args.args.map { it.arg to parseType(this, it.annotation!!) })
  }
  fun forLet(let: Let): ExprTyper {
    return let.bindings.fold(this) { typer, k ->
      typer.updated(matchNamesAndTypes(k.names, typer.get(k.value).asType()))
    }
  }
}

interface TypeCalculator<T> {
  val numT: T
  val strT: T
  val bytesT: T
  val boolT: T
  val noneT: T

  fun isSubType(a: T, b: T): Boolean
  fun join(a: T, b: T): T
  fun join(a: Collection<T>): T = a.reduce(::join)
  fun asClass(v: T, className: String): Pair<T,List<T>>?
  fun asClassF(v: T, className: String): Pair<T,List<T>> = asClass(v, className)
      ?: fail("cannot cast $v as $className")
  fun mkClass(name: String, tParams: List<T>): T
  fun parseType(e: TExpr): T
  fun mkListType(t: T): T = mkClass("pylib.PyList", listOf(t))
  fun mkSetType(t: T): T = mkClass("pylib.Set", listOf(t))
  fun mkDictType(k: T, v: T): T = mkClass("pylib.Dict", listOf(k, v))
  fun mkTupleType(elts: List<T>): T = mkClass("pylib.Tuple", elts)
  fun mkFunType(args: List<T>, retType: T): T
  fun resolveAttrGet(t: T, attr: identifier): T
  fun resolveAttrCall(t: T, attr: identifier): Pair<T, List<T>>
  context (NameResolver<T>)
  fun resolveCall(t: T, args: List<T>, keywords: List<Pair<String,T>>): T
  fun resolveIndexGet(t: T, indices: List<T>): T
  fun resolveSliceGet(t: T, l: T?, u: T?, s: T?): T {
    return if (isSubType(t, bytesT))
      bytesT
    else {
      asClassF(t, "pylib.Sequence").first
    }
  }
  fun getConstType(e: Constant): T = when(e) {
    is Num -> numT
    is Str -> strT
    is Bytes -> bytesT
    is NameConstant -> when(e.value) {
      true, false -> boolT
      null -> noneT
      else -> fail("unsupported $e")
    }
  }

  fun parseType(cp: ClassValParam): T = when(cp) {
    is ClassVal -> parseType(cp.toTExpr())
    is ExTypeVar -> TODO()
  }
  context (NameResolver<T>)
  fun getType(e: TExpr): T {
    return when(e) {
      is CTV -> {
        when (e.v) {
          is ClassVal -> parseType(e.v)
          is FuncInst ->
            mkFunType(e.v.sig.args.map { parseType(it.second.toFAtom(emptyMap()).toClassVal()) },
              parseType(e.v.sig.ret.toFAtom(emptyMap()).toClassVal()))
          is ConstExpr -> getType(e.v.e)
          else -> TODO()
        }
      }
      is Constant -> getConstType(e)
      is Attribute -> resolveAttrGet(getType(e.value), e.attr)
      is Subscript -> {
        val typ = getType(e.value)
        val tuple = asClass(typ, "pylib.Tuple")
        if (tuple != null) {
          if (e.slice is Index) {
            if (e.slice.value is Num) {
              tuple.second[e.slice.value.n.toInt()]
            } else TODO()
          } else  TODO()
        } else {
          when (e.slice) {
            is Index -> {
              val indices = when (e.slice.value) {
                is Tuple -> e.slice.value.elts
                else -> listOf(e.slice.value)
              }.map { getType(it) }
              resolveIndexGet(typ, indices)
            }
            is Slice -> resolveSliceGet(typ,
                e.slice.lower?.let { getType(it) },
                e.slice.upper?.let { getType(it) },
                e.slice.step?.let { getType(it) })
            is ExtSlice -> fail("ExtSlice is not supported")
          }
        }
      }
      is BoolOp, is Compare -> boolT
      is BinOp -> {
        val aType = getType(e.left)
        val bType = getType(e.right)
        if (aType != bType && isSubType(bType, aType))
          resolveAttrCall(bType, "__r" + e.op + "__").first
        else
          resolveAttrCall(aType, "__" + e.op + "__").first
      }
      is UnaryOp -> when(e.op) {
        EUnaryOp.USub -> {
          val valType = getType(e.operand)
          if (isSubType(valType, numT))
            valType
          else TODO()
        }
        EUnaryOp.Not -> {
          val valType = getType(e.operand)
          if (isSubType(valType, boolT) || isSubType(valType, numT)) {
            boolT
          } else TODO()
        }
        EUnaryOp.Invert, EUnaryOp.UAdd -> TODO()
      }
      is Call -> {
        when {
          e.func is Name && e.func.id in TypeResolver.specialFuncNames -> {
            val op = e.func.id.substring(1, e.func.id.length-1)
            if (e.keywords.isNotEmpty()) fail("$op shouldn't have keyword arguments")
            when (op) {
              in TypeResolver.cmpOps, in TypeResolver.cmpOps -> boolT
              in TypeResolver.binOps -> {
                if (e.args.size != 2) fail("binOp $op should have two args")
                getType(BinOp(e.args[0], EBinOp.valueOf(op), e.args[1]))
              }
              in TypeResolver.unaryOp -> {
                if (e.args.size != 1) fail("unaryOp $op should have one argument")
                getType(UnaryOp(EUnaryOp.valueOf(op), e.args[0]))
              }
              else -> fail("unsupported $op")
            }
          }
          e.func is CTV && e.func.v is ClassVal -> {
            parseType(e.func) // todo: convert to clazz instantiation
          }
          e.func is CTV && e.func.v is FuncInst -> {
            val t = getType(e.func)
            val (_,tParams) = asClassF(t, "pylib.Callable")
            // resolveCall(t, e.args.map { getType(it) }, e.keywords.map { it.arg!! to getType(it.value) })
            tParams[tParams.size-1]
          }
          e.func is Attribute -> {
            resolveAttrCall(getType(e.func.value), e.func.attr).first
          }
          e.func is Lambda -> {
            TODO()
          }
          e.func is Name && resolve(e.func.id) != null -> {
            val t = getType(e.func)
            val (_,tParams) = asClassF(t, "pylib.Callable")
            tParams[tParams.size-1]
          }
          else -> TODO()
        }
      }
      is IfExp -> join(getType(e.body), getType(e.orelse))
      is Starred -> getType(e.value)
      is PyList -> {
        val elemType = if (e.valueAnno != null) parseType(e.valueAnno)
        else join(e.elts.map { getType(it) })
        mkListType(elemType)
      }
      is PySet -> {
        val elemType = if (e.valueAnno != null) parseType(e.valueAnno)
        else join(e.elts.map { getType(it) })
        mkSetType(elemType)
      }
      is PyDict -> {
        val keyType = if (e.keyAnno != null) parseType(e.keyAnno)
        else join(e.keys.map { getType(it) })
        val valueType = if (e.valueAnno != null) parseType(e.valueAnno)
        else join(e.values.map { getType(it) })
        mkDictType(keyType, valueType)
      }
      is Tuple -> mkTupleType(e.elts.map { getType(it) })
      is Let -> {
        val newResolver = e.bindings.fold(this@NameResolver) { resolver, k ->
          val t = with(resolver) {
            getType(k.value)
          }
          resolver.copy(matchVarsAndTypes(namesToTExpr(k.names, true), t))
        }
        with(newResolver) { getType(e.value) }
      }
      is GeneratorExp -> {
        if (e.generators.size != 1) {
          fail("unsupported")
        } else {
          val gen = e.generators[0]
          val elemType = asClassF(getType(gen.iter),"pylib.Iterable").second[0]
          val newResolver = this@NameResolver.copy(matchVarsAndTypes(gen.target, elemType))
          val elType = with (newResolver) { getType(e.elt) }
          asClassF(mkListType(elType), "pylib.Sequence").first
        }
      }
      is ListComp -> {
        val elemType = asClassF(getType(GeneratorExp(e.elt, e.generators)), "pylib.Iterable").second[0]
        mkListType(elemType)
      }
      is Lambda -> {
        if (!(e.args.posonlyargs.isEmpty() && e.args.defaults.isEmpty() && e.args.kw_defaults.isEmpty() && e.args.kwonlyargs.isEmpty() && e.args.kwarg == null && e.args.vararg == null)) {
          fail("")
        }
        if (e.args.args.any { it.annotation == null })
          fail()
        val lamArgTypes = e.args.args.map { parseType(it.annotation!!)}
        val newResolver = this@NameResolver.copy(e.args.args.zip(lamArgTypes).map { (arg, typ) -> arg.arg to typ })
        val retType = with(newResolver) { getType(e.body) }
        mkFunType(lamArgTypes, retType)
      }
      is Name -> if (e.ctx == ExprContext.Store)
        TODO()
      else {
        resolve(e.id)
            ?: fail()
      }
      else -> TODO()
    }
  }
  fun matchVarsAndTypes(e: TExpr, t: T): Collection<Pair<String,T>> {
    return when(e) {
      is Name -> listOf(Pair(e.id, t))
      is Tuple -> {
        if (e.elts.size == 1)
          matchVarsAndTypes(e.elts[0], t)
        else {
          val (_, params) = asClassF(t, "pylib.Tuple")
          if (params.size == e.elts.size) {
            e.elts.zip(params).flatMap { matchVarsAndTypes(it.first, it.second) }
          } else {
            fail("target $e doesn't match elem type $t")
          }
        }
      }
      else -> fail("unsupported $e")
    }
  }
}

object SortTypeCalculator : TypeCalculator<Sort> {
  val typingCtx = TypingContext
  override val boolT = toRTType(FAtom("pylib.bool"))
  override val numT = toRTType(FAtom("pylib.int"))
  override val strT = toRTType(FAtom("pylib.str"))
  override val bytesT = toRTType(FAtom("pylib.bytes"))
  override val noneT = toRTType(FAtom("pylib.None"))

  override fun parseType(e: TExpr): Sort {
    return parseType(TypeResolver.topLevelTyper, e)
  }

  override fun parseType(cp: ClassValParam): Sort {
    return if (cp is ClassVal) {
      toRTType(cp)
    } else super.parseType(cp)
  }
  override fun join(a: Sort, b: Sort): Sort {
    val a_ = a.toFAtom()
    val b_ = b.toFAtom()
    with(typingCtx) {
      val (a2, b2) = tryJoin(a_, b_)
      if (a2 != b2) fail()
      return toRTType(a2)
    }
  }

  override fun isSubType(a: Sort, b: Sort): Boolean {
    val a_ = a.toFAtom()
    val b_ = b.toFAtom()
    with(typingCtx) {
      return tryCheckST(a_, b_)!!
    }
  }

  override fun asClass(v: Sort, className: String): Pair<Sort, List<Sort>>? {
    if (v !is RTType)
      return null
    val res = with(typingCtx) {
      getAncestorByClassName(v.toFAtom(), className)
    }
    return res?.let { toRTType(it) to it.ps.map { toRTType(it) } }
  }

  override fun mkClass(name: String, tParams: List<Sort>): Sort {
    return NamedType(name, tParams.map { it.asType() })
  }

  override fun mkFunType(args: List<Sort>, retType: Sort): Sort {
    return FunType(args.map { it.asType() }, retType.asType())
  }
  override fun resolveAttrGet(t: Sort, attr: identifier): Sort {
    with(typingCtx) {
      return toRTType(resolveAttributeGet(t.toFAtom(), attr))
    }
  }

  override fun resolveAttrCall(t: Sort, attr: identifier): Pair<Sort, List<Sort>> {
    val attrHandle = resolveAttrGet(t, attr.toLowerCase()) as FunType
    return attrHandle.retType to attrHandle.argTypes
  }

  context (NameResolver<Sort>)
  override fun resolveCall(t: Sort, args: List<Sort>, keywords: List<Pair<String, Sort>>): Sort {
    val funcType = if (t is NamedType) t.`class` else t
    return TypeResolver.resolveReturnType(this@NameResolver, funcType,
        args.map { it.asType() }, keywords.map { it.first to it.second.asType() }).first.retType
  }

  override fun resolveIndexGet(t: Sort, indices: List<Sort>): Sort {
    return if (t is RTType)
      resolveAttrCall(t, "__getitem__").first
    else
      TypeResolver.resolveSubscriptType(t, indices)
  }
}

fun toRTType(a: FTerm): RTType = toRTType(a.toClassVal())
fun toRTType(c: ClassVal): RTType {
  if (c.name == "Callable" || c.name == "pylib.Callable")
    return FunType(
        c.tParams.subList(0, c.tParams.size-1).map { toRTType(it.asClassVal()) },
        toRTType(c.tParams[c.tParams.size-1].asClassVal())
    )
  return NamedType(c.name, c.tParams.map { toRTType(it.asClassVal()) })
}

private class ExprTypes(override val ctx: NameResolver<Sort>, private val cache: IdentityHashMap<TExpr, Sort>): ExprTyper {
  override fun updated(ctx: NameResolver<Sort>) = ExprTypes(ctx, cache)
  override fun updated(vars: Map<String, Sort>) = updated(ctx.copy(vars))
  override fun updated(vars: Collection<Pair<String, Sort>>) = updated(vars.toMap())
  override fun get(e: TExpr): Sort = cache.getOrPut(e) { getExprType(e) }

  val typeCalc = SortTypeCalculator
  private fun getExprType(e: TExpr): Sort {
    with(ctx) {
      val res = when (e) {
        is Constant -> typeCalc.getType(e)
        is Attribute -> typeCalc.getType(e)
        is Subscript -> typeCalc.getType(e)
        is BoolOp, is Starred, is IfExp -> typeCalc.getType(e)
        is Call -> typeCalc.getType(e)
        is Tuple, is PyList, is PySet, is PyDict -> typeCalc.getType(e)
        is Let -> typeCalc.getType(e)
        is GeneratorExp, is ListComp -> typeCalc.getType(e)
        is Lambda -> typeCalc.getType(e)
        is CTV -> typeCalc.getType(e)
        is Name ->
          if (e.ctx == ExprContext.Store)
            fail()
          else if (e.id.endsWith("_default"))
            NamedType(e.id.substring(0, e.id.length - "_default".length))
          else ctx.resolve(e.id)
              ?: (if (e.id.startsWith("ssz."))
                ctx.resolve(e.id.substring("ssz.".length))
              else null)
              ?: fail(e.toString())
        else -> fail(e.toString())
      }
      return res
    }
  }
}

fun <T> toList(t: T?) = t?.let(::listOf)?: emptyList()

fun getVarNamesInStoreCtx(e: TExpr): Collection<String> = when(e) {
  is Name -> listOf(e.id)
  is Tuple -> e.elts.flatMap(::getVarNamesInStoreCtx).toSet()
  is Subscript -> listOf() //listOf(e.value).plus(getShallowExprs(e.slice)).flatMap(::getVarDefs).toSet()
  is Attribute -> listOf() //getVarDefs(e.value)
  else -> fail("unsupported $e")
}

fun getVarNamesInLoadCtx(e: TExpr): Collection<String> = when(e) {
  is Name -> listOf()
  is Tuple -> e.elts.flatMap(::getVarNamesInLoadCtx).toSet()
  is Subscript -> liveVarAnalysis(e)
  is Attribute -> liveVarAnalysis(e)
  else -> fail("unsupported $e")
}

fun extractTargetNames(lval: TExpr): List<String> = when(lval) {
  is Name -> listOf(lval.id)
  is Tuple -> lval.elts.map { (it as Name).id }
  else -> TODO()
}

fun namesToTExpr(tgts: List<String>, store: Boolean): TExpr = when {
  tgts.size == 1 -> mkName(tgts[0], store)
  else -> Tuple(tgts.map { mkName(it, store) }, if (store) ExprContext.Store else ExprContext.Load)
}

fun matchNamesAndTypes(names: List<String>, t: RTType): Collection<Pair<String,RTType>> {
  return when {
    names.size == 1 -> listOf(names[0] to t)
    else -> {
      val res = matchVarsAndTypes(Tuple(names.map { mkName(it, true) }, ExprContext.Store), t)
      res.map { it.first.id to it.second }
    }
  }
}
fun matchVarsAndTypes(e: TExpr, t: RTType): Collection<Pair<Name,RTType>> {
  return when(e) {
    is Name -> listOf(Pair(e, t))
    is Tuple -> {
      val gt = asGenType(t)
      if (gt.name == "Tuple" || gt.name == "pylib.Tuple"|| gt.tParams.size != e.elts.size) {
        e.elts.zip(gt.tParams).flatMap{ matchVarsAndTypes(it.first, it.second) }
      } else {
        fail("unsupported")
      }
    }
    else -> fail("unsupported $e")
  }
}

fun <S,A,B> foldWithState(initial: S, events: Iterable<A>, transition: (S,A)->Pair<S,B>): List<B> {
  var curr = initial
  val acc = mutableListOf<B>()
  for (e in events) {
    val (next, outcome) = transition(curr, e)
    acc.add(outcome)
    curr = next
  }
  return acc
}


typealias SST = Pair<String, RTType>
typealias CTX = Map<String,Sort>

data class Analyses(
    val varTypings: StmtAnnoMap<NameResolver<Sort>>,
    val varTypingsAfter: StmtAnnoMap<NameResolver<Sort>>,
    val funcArgs: List<FArg>) {
  fun deepCopy(): Analyses {
    return Analyses(varTypings.clone() as StmtAnnoMap<NameResolver<Sort>>,
        varTypingsAfter.clone() as StmtAnnoMap<NameResolver<Sort>>,
        funcArgs)
  }
}

fun inferVarTypes(outerTyper: ExprTyper, f: FunctionDef): Analyses {
  val args = getFArgs(outerTyper, f)
  val stmts = f.body.filterIndexed { i, s -> !(i == 0 && s is Expr && s.value is Str) }

  fun gatherUpdates(typer: ExprTyper, e: TExpr, t: RTType): List<Pair<String, Sort>> {
    fun getExprType(e: TExpr) = typer[e]
    return when (e) {
      is Name -> {
        if (e.id !in typer || e.id in outerTyper && typer.ctx.resolve(e.id) == outerTyper.ctx.resolve(e.id)) {
          listOf(e.id to t)
        } else {
          val type = typer.ctx.resolve(e.id)!!.asType()
          if (!isSubType(t, type)) {
            fail("type mismatch")
          }
          emptyList()
        }
      }
      is Subscript -> {
        val rt = getExprType(e.value)
        when (e.slice) {
          is Index -> {
            val it = getExprType(e.slice.value)
            //println("TODO check")
            emptyList()
          }
          is Slice -> {
            val lower = e.slice.lower?.let { getExprType(it) }
            val upper = e.slice.upper?.let { getExprType(it) }
            val step = e.slice.step?.let { getExprType(it) }
            //println("TODO check")
            emptyList()
          }
          else -> TODO()
        }
      }
      is Attribute -> {
        val ft = getExprType(e).asType()
        if (!canAssignOrCoerceTo(t, ft))
          fail("types do not match")
        emptyList()
      }
      is Tuple -> {
        val etPairs = matchVarsAndTypes(e, t)
        etPairs.flatMap { gatherUpdates(typer, it.first, it.second) }
      }
      else -> fail(e.toString())
    }
  }

  val typingBefore = StmtAnnoMap<NameResolver<Sort>>()
  val typingAfter = StmtAnnoMap<NameResolver<Sort>>()

  fun getVarUpds(typer: ExprTyper, s: Stmt): Pair<ExprTyper, List<SST>> {
    typingBefore[s] = typer.ctx

    fun getExprType(e: TExpr) = typer[e]

    fun procAssgnTarget(exprTyper: ExprTyper, e: TExpr, st: Stmt): List<SST> = run {
      when (e) {
        is Name -> listOf(Pair(e.id, exprTyper.ctx.resolve(e.id)!!.asType()))
        is Tuple -> e.elts.flatMap { procAssgnTarget(exprTyper, it, st) }
        is Subscript -> emptyList()
        is Attribute -> emptyList()
        else -> fail(e.toString())
      }
    }

    val res = when (s) {
      is Assign -> {
        val e = s.target
        val newTyper = typer.updated(gatherUpdates(typer, e, getExprType(s.value).asType()))
        Pair(newTyper, procAssgnTarget(newTyper, e, s))
      }
      is AugAssign -> {
        gatherUpdates(typer, s.target, getExprType(BinOp(s.target, s.op, s.value)).asType())
        Pair(typer, procAssgnTarget(typer, s.target, s))
      }
      is AnnAssign -> {
        val newTyper = typer.updated(gatherUpdates(typer, s.target, parseType(typer, s.annotation)))
        Pair(newTyper, procAssgnTarget(newTyper, s.target, s))
      }
      is If -> {
        val bodyUpdates = foldWithState(typer, s.body, ::getVarUpds).flatten()
        val elseUpdates = foldWithState(typer, s.orelse, ::getVarUpds).flatten()
        val bups = bodyUpdates.map { it.first to it.second }.toMap()
        val eups = elseUpdates.map { it.first to it.second }.toMap()

        val vups = mutableMapOf<String,Sort>()
        val upds = mutableListOf<Pair<String,Sort>>()
        for(k in bups.keys.union(eups.keys)) {
          val bu = bups[k]
          val eu = eups[k]
          if (bu != null && eu != null) {
            val u = getCommonSuperType(bu, eu)
            vups[k] = u
            upds.addAll(gatherUpdates(typer, Name(k, ExprContext.Load), u))
          } else if (bu != null) {
            vups[k] = bu
            upds.addAll(gatherUpdates(typer, Name(k, ExprContext.Load), bu))
          } else if (eu != null) {
            vups[k] = eu
            upds.addAll(gatherUpdates(typer, Name(k, ExprContext.Load), eu))
          }
        }

        Pair(typer.updated(upds), bodyUpdates.plus(elseUpdates))
      }
      is While -> {
        val updates = foldWithState(typer, s.body, ::getVarUpds)
        Pair(typer, updates.flatten())
      }
      is For -> {
        val iterTyp = getExprType(s.iter).asType()
        val newVars = gatherUpdates(typer, s.target, getIterableElemType(iterTyp))
        val updates = foldWithState(typer.updated(newVars), s.body, ::getVarUpds).flatten()
        Pair(typer, updates)
      }
      is Try -> {
        val updates = foldWithState(typer, s.body, ::getVarUpds).flatten()
        val handlersUpdtes = s.handlers.flatMap { foldWithState(typer, it.body, ::getVarUpds).flatten() }

        val elseUpdates = foldWithState(typer, s.orelse, ::getVarUpds).flatten()
        val finalUpdates = foldWithState(typer, s.finalbody, ::getVarUpds).flatten()

        Pair(typer, updates.plus(handlersUpdtes).plus(elseUpdates).plus(finalUpdates))
      }
      is Return -> Pair(typer, emptyList())
      is Assert -> Pair(typer, emptyList())
      is Expr -> Pair(typer, emptyList())
      is Pass -> Pair(typer, emptyList())
      is Continue -> Pair(typer, emptyList())
      is Break -> Pair(typer, emptyList())
      else -> fail(s.toString())
    }
    typingAfter[s] = res.first.ctx
    return Pair(res.first, res.second)
  }

  foldWithState(outerTyper.updated(args.map { it.name to it.type }), stmts, ::getVarUpds).flatten()

  return Analyses(typingBefore, typingAfter, args)
}
