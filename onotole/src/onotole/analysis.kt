package onotole

import onotole.lib_defs.FunDecl
import onotole.lib_defs.PyLib
import java.util.*

fun canBeCoercedTo(a: RTType, b: RTType): Boolean = when {
  isSubType(b, NamedType("ssz.boolean")) && isSubType(a, TPyBool) -> true
  b == TPyBool && a.typeInfo.coercibleToBool -> true
  b is NamedType && (b.name == "ssz.List" || b.name == "ssz.Vector") && isSubType(a, TPyIterable(b.tParams[0])) -> true
  isSubType(b, NamedType("ssz.uint")) && isSubType(a, TPyInt) -> true
  isSubType(b , TPyBytes) && isSubType(a, TPyBytes) -> true
  else -> false
}
fun canAssignOrCoerceTo(a: RTType, b: RTType) = isSubType(a, b) || canBeCoercedTo(a, b)

class FuncCollection(funcs: List<FunSignature> = emptyList()) {
  val funcs = funcs.toMutableList()

  fun addFunc(args: List<FArg>, retType: RTType) {
    funcs.add(FunSignature(args, retType))
  }
  fun matches(fp: List<FArg>, args: List<RTType>, kwdArgs: List<Pair<String,RTType>>): List<ArgRef>? {
    val slots = Array<ArgRef?>(fp.size) { null }
    for (i in args.indices) {
      if (i >= fp.size)
        return null
      else {
        val fpt = fp[i].type
        if (fpt is TypeVar)
          unifEx(fpt, args[i])
        else if (!canAssignOrCoerceTo(args[i], fpt))
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
  fun resolve(args: List<RTType>, kwdArgs: List<Pair<String,RTType>> = emptyList()): RTType? =
      resolveFP(args, kwdArgs)?.first?.retType

  fun resolveFP(args: List<RTType>, kwdArgs: List<Pair<String,RTType>> = emptyList()): Pair<FunSignature,List<ArgRef>>? {
    for(f in funcs) {
      val match = matches(f.args, args, kwdArgs)
      if (match != null)
        return Pair(f,match)
    }
    return null
  }
}

object TypeResolver {
  val consts = mutableMapOf<String, RTType>()
  val packages = mutableMapOf<String, Map<String, Sort>>()
  val metaclasses = mutableMapOf<String, MetaClass>()
  val nameToType = mutableMapOf<String, Clazz>()
  val funcSigs = mutableMapOf<String, FuncCollection>()
  val funcResolvers = mutableMapOf<String, (List<RTType>, List<Pair<String, RTType>>) -> RTType>()
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

  val topLevelTyper get() = ExprTypes(topLevelResolver, IdentityHashMap())

  fun registerFuncResolver(funcRef: String, resolver: (List<RTType>) -> RTType) {
    funcResolvers[funcRef] = { args, kwdArgs ->
      if (kwdArgs.isNotEmpty()) fail()
      resolver(args)}
    register(FuncRefTI(funcRef))
  }

  fun registerFuncResolver(funcRef: String, resolver: (List<RTType>, List<Pair<String,RTType>>) -> RTType) {
    funcResolvers[funcRef] = resolver
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
        metaClass.instantiate(type.tParams.map { (it as NamedType).clazz })
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

  val specialFuncs = setOf("list", "dict")
  val binOps = EBinOp.values().map { it.name }
  val boolOps = EBoolOp.values().map { it.name }
  val cmpOps = ECmpOp.values().map { it.name }
  val unaryOp = EUnaryOp.values().map { it.name }
  val specialFuncNames = specialFuncs.plus(binOps).plus(boolOps).plus(cmpOps).plus(unaryOp).map { "<$it>" }.toSet()

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
        (type.instantiate(indices).type as NamedType).clazz
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

  fun resolveReturnType(callable: Sort, argTypes: List<RTType>, kwdArgs: List<Pair<String, RTType>>): Pair<FunSignature,List<ArgRef>> {
    if (TPyNothing in listOf(callable).plus(argTypes).plus(kwdArgs.map { it.second })) {
      val retType = TPyNothing
      val sig = FunSignature(argTypes.mapIndexed { i,t -> FArg("_$i", t) }, retType)
      return Pair(sig, argTypes.mapIndexed { i,_ -> PositionalRef(i) })
    }
    return when (callable) {
      is SpecialFuncRef -> {
        if (kwdArgs.isNotEmpty()) TODO()
        val retType = resolveSpecialCallType(callable.name, argTypes)
        val sig = FunSignature(argTypes.mapIndexed { i,t -> FArg("_$i", t) }, retType)
        Pair(sig, argTypes.mapIndexed { i,_ -> PositionalRef(i) })
      }
      is NamedFuncRef -> {
        val name = callable.name
        if (name in funcSigs) {
          val sig = funcSigs[name]!!
          sig.resolveFP(argTypes, kwdArgs)
                  ?: fail("no function found for $name(${(argTypes+kwdArgs).joinToString(", ")})")
        } else {
          val retType = funcResolvers[name]?.invoke(argTypes, kwdArgs) ?: fail("not found $name")
          val sig = FunSignature(argTypes.mapIndexed { i,t -> FArg("_$i", t) }
                  .plus(kwdArgs.map { (a,t) -> FArg(a, t) }), retType
          )
          val argRefs = argTypes.mapIndexed { i,_ -> PositionalRef(i) }
                  .plus(kwdArgs.mapIndexed { i,_ -> KeywordRef(i) })
          Pair(sig, argRefs)
        }
      }
      is PartiallyAppliedFuncRef -> {
        val (fh, argRefs) = callable.func.resolveReturnType(callable.params + argTypes, kwdArgs)
        val newRefs = argRefs.subList(callable.params.size, argRefs.size).map {
          if (it is PositionalRef) it.copy(idx = it.idx - callable.params.size) else it
        }
        fh to newRefs
      }
      is Clazz -> {
        funcSigs[callable.name]?.resolveFP(argTypes, kwdArgs)// ?: fail("no function found for ${callable.name}($argTypes,$kwdArgs)")
                ?: FunSignature(argTypes.mapIndexed { i,a -> FArg("_$i", a)}, callable.toInstance()) to argTypes.indices.map { PositionalRef(it) }
      }
      is FunType -> {
        if (kwdArgs.isNotEmpty()) fail("keyword args are not supported")
        val sig = FunSignature(callable.argTypes.mapIndexed { i,t -> FArg("_$i", t) }, callable.retType)
        val coll = FuncCollection(listOf(sig))
        coll.resolveFP(argTypes, kwdArgs)!!
      }
      else -> TODO()
    }
  }
}

val Sort.typeInfo get() = TypeResolver.getTypeInfo(this)
fun Sort.resolveAttrType(attr: String) = TypeResolver.resolveAttrType(this, attr)
fun Sort.resolveReturnType(argTypes: List<RTType>, kwdArgs: List<Pair<String,RTType>>)
    = TypeResolver.resolveReturnType(this, argTypes, kwdArgs)
fun MetaClass.instantiate(tps: List<Sort>): TypeInfo {
  val ti = typeInfo as MetaClassTInfo
  val nTParams = ti.nTParams ?: tps.size
  if (tps.size < nTParams || tps.size > nTParams + ti.nEParams)
    fail("Wrong amount of types")
  val tParams = tps.subList(0, nTParams).filterIsInstance<Clazz>().map { it.toInstance() }
  if (tParams.size != nTParams)
    fail("wrong type parameters $tps")
  return DataTInfo(ti.name, tParams, baseType = ti.baseClassF(tParams), attrs = ti._attrs)
}

fun parseType(typer: ExprTypes, t: String): RTType = parseType(typer, Name(t, ExprContext.Load))
fun parseType(typer: ExprTypes, t: TExpr): RTType {
  return if (t == NameConstant(null))
    TPyNone
  else {
    val res = typer[t]
    if (res is Clazz) {
      res.toInstance()
    } else if (res is MetaClass) {
      res.toInstance()
    } else
      TODO()
  }
}

fun getFArg(typer: ExprTypes, a: Arg, default: TExpr?) = FArg(a.arg, parseType(typer, a.annotation!!), default)

fun getFArgs(typer: ExprTypes, f: FunctionDef): List<FArg> {
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
val NamedType.clazz get() = Clazz(this.name, this.tParams)

fun Sort.asType(): RTType = when(this) {
  is RTType -> this
  else -> fail("")
}

class ExprTypes(val ctx: NameResolver<Sort>, val cache: IdentityHashMap<TExpr, Sort>) {
  fun new(newCtx: NameResolver<Sort>) = ExprTypes(newCtx, cache)
  operator fun get(e: TExpr): Sort = cache.getOrPut(e) { getExprType(e) }
  fun getExprType(e: TExpr) = getExprType(e, ctx)
  fun getExprType(e: TExpr, ctx: NameResolver<Sort>): Sort {
    val res = when (e) {
      is Num -> TPyInt
      is Str -> TPyStr
      is Bytes -> TPyBytes
      is Name ->
        ctx.resolve(e.id)
          ?: fail(e.toString())
      is NameConstant -> when (e.value) {
        true -> TPyBool
        false -> TPyBool
        null -> TPyNone
        else -> fail("unsupported $e")
      }
      is Attribute -> getExprType(e.value, ctx).resolveAttrType(e.attr)
      is GeneratorExp -> {
        if (e.generators.size != 1) {
          fail("unsupported")
        } else {
          val gen = e.generators[0]
          val vtPairs = matchVarsAndTypes(gen.target, getIterableElemType(getExprType(gen.iter, ctx).asType()))
          val newCtx = ctx.copy(vtPairs.map { it.first.id to it.second }.toMap())
          TPySequence(getExprType(e.elt, newCtx).asType())
        }
      }
      is Subscript -> {
        val typ = getExprType(e.value, ctx)
        when (e.slice) {
          is Index -> {
            val indices = when (e.slice.value) {
              is Tuple -> e.slice.value.elts
              else -> listOf(e.slice.value)
            }.map { getExprType(it, ctx) }
            TypeResolver.resolveSubscriptType(typ, indices)
          }
          is Slice -> TypeResolver.resolveSubscriptType(typ,
              e.slice.lower?.let { getExprType(it, ctx) },
              e.slice.upper?.let { getExprType(it, ctx) },
              e.slice.step?.let { getExprType(it, ctx) }
          )
          is ExtSlice -> fail("ExtSlice is not supported")
        }
      }
      is Call -> {
        val receiverType = getExprType(e.func, ctx)
        fun tryResolve(ctx: NameResolver<Sort>): RTType = run {
          receiverType.resolveReturnType(e.args.map { getExprType(it, ctx).asType() },
                  e.keywords.map { it.arg!! to getExprType(it.value, ctx).asType() }).first.retType
        }
        try {
          tryResolve(ctx)
        } catch (e: UnificationException) {
          val newCtx = ctx.copy(e.unif.map { it.first.name to it.second }.toMap())
          tryResolve(newCtx)
        }
      }
      is IfExp -> getCommonSuperType(getExprType(e.body, ctx).asType(), getExprType(e.orelse, ctx).asType())
      is Lambda -> {
        if (!(e.args.posonlyargs.isEmpty() && e.args.defaults.isEmpty() && e.args.kw_defaults.isEmpty() && e.args.kwonlyargs.isEmpty() && e.args.kwarg == null && e.args.vararg == null)) {
          fail("")
        }
        var unknownArgTypes = 0
        val lamArgTypes = e.args.args.map {
          if (it.annotation == null) {
            val tName = "?${unknownArgTypes}"
            val sort = ctx.resolve(tName)
            if (sort != null) {
              sort.asType()
            } else {
              unknownArgTypes++
              TypeVar(tName)
            }
          } else parseType(this, it.annotation)
        }

        if (unknownArgTypes == 0) {
          val newCtx = ctx.copy(e.args.args.zip(lamArgTypes).map { (arg, typ) -> arg.arg to typ }.toMap())
          FunType(lamArgTypes, getExprType(e.body, newCtx).asType())
        } else {
          FunType(lamArgTypes, TypeVar("?${unknownArgTypes}"))
        }
      }
      is Starred -> getExprType(e.value, ctx).asType()
      is Tuple -> TPyTuple(e.elts.map { getExprType(it, ctx).asType() })
      else -> fail(e.toString())
    }
    cache[e] = res
    return res
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

fun matchVarsAndTypes(e: TExpr, t: RTType): Collection<Pair<Name,RTType>> {
  return when(e) {
    is Name -> listOf(Pair(e, t))
    is Tuple -> {
      val gt = asGenType(t)
      if (gt.name == "Tuple" || gt.tParams.size != e.elts.size) {
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



data class Tuple4<A,B,C,D>(val first: A, val second: B, val third: C, val fourth: D)

typealias SST = Pair<String, RTType>
typealias CTX = Map<String,Sort>

data class Analyses(
    val varTypings: StmtAnnoMap<NameResolver<Sort>>,
    val varTypingsAfter: StmtAnnoMap<NameResolver<Sort>>,
    val funcArgs: List<FArg>)

fun inferenceVarTypes(f: FunctionDef): Analyses {
  val args = getFArgs(TypeResolver.topLevelTyper, f)
  val stmts = f.body.filterIndexed { i, s -> !(i == 0 && s is Expr && s.value is Str) }
  val globalCtx = TypeResolver.topLevelResolver.copy(args.map { it.name to it.type }.toMap())

  fun gatherUpdates(ctx: NameResolver<Sort>, e: TExpr, t: RTType): List<Pair<String,Sort>> {
    val exprTypes = ExprTypes(ctx, IdentityHashMap())
    fun getExprType(e: TExpr) = exprTypes[e]
    return when (e) {
        is Name -> {
          if (e.id !in ctx || e.id in globalCtx && ctx[e.id] == globalCtx[e.id]) {
            listOf(e.id to t)
          } else {
            val type = ctx[e.id]!!.asType()
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
        val ft = getExprType(e.value).resolveAttrType(e.attr).asType()
        if (!canAssignOrCoerceTo(t, ft))
          fail("types do not match")
        emptyList()
      }
      is Tuple -> {
        val etPairs = matchVarsAndTypes(e, t)
        etPairs.flatMap { gatherUpdates(ctx, it.first, it.second) }
      }
      else -> fail(e.toString())
    }
  }

  val typingBefore = StmtAnnoMap<NameResolver<Sort>>()
  val typingAfter = StmtAnnoMap<NameResolver<Sort>>()

  fun getVarUpds(exprTypes: ExprTypes, s: Stmt): Pair<ExprTypes, List<SST>> {
    val varTyping: NameResolver<Sort> = exprTypes.ctx
    typingBefore[s] = varTyping

    fun getExprType(e: TExpr) = exprTypes[e]

    fun procAssgnTarget(exprTypes: ExprTypes, e: TExpr, st: Stmt): List<SST> = run {
      when (e) {
        is Name -> listOf(Pair(e.id, exprTypes[e].asType()))
        is Tuple -> e.elts.flatMap { procAssgnTarget(exprTypes, it, st) }
        is Subscript -> emptyList()
        is Attribute -> emptyList()
        else -> fail(e.toString())
      }
    }

    val res = when (s) {
      is Assign -> {
        val e = s.target
        val newCtx = varTyping.copy(gatherUpdates(varTyping, e, getExprType(s.value).asType()))
        Pair(newCtx, procAssgnTarget(exprTypes.new(newCtx), e, s))
      }
      is AugAssign -> {
        gatherUpdates(varTyping, s.target, getExprType(BinOp(s.target, s.op, s.value)).asType())
        Pair(varTyping, procAssgnTarget(exprTypes, s.target, s))
      }
      is AnnAssign -> {
        val newCtx = varTyping.copy(gatherUpdates(varTyping, s.target, parseType(exprTypes, s.annotation)))
        Pair(newCtx, procAssgnTarget(exprTypes.new(newCtx), s.target, s))
      }
      is If -> {
        val bodyUpdates = foldWithState(exprTypes, s.body, ::getVarUpds).flatten()
        val elseUpdates = foldWithState(exprTypes, s.orelse, ::getVarUpds).flatten()
        val bups = bodyUpdates.map { it.first to it.second }.toMap()
        val eups = elseUpdates.map { it.first to it.second }.toMap()

        val vups = mutableMapOf<String,Sort>()
        val upds = mutableListOf<Pair<String,Sort>>()
        for(k in bups.keys.union(eups.keys)) {
          val bu = bups[k]
          val eu = eups[k]
          if (bu != null && eu != null && bu != eu) {
            fail("ggg")
          }
          if (bu != null) {
            vups[k] = bu
            upds.addAll(gatherUpdates(varTyping, Name(k, ExprContext.Load), bu))
          }
          if (eu != null) {
            vups[k] = eu
            upds.addAll(gatherUpdates(varTyping, Name(k, ExprContext.Load), eu))
          }
        }

        Pair(varTyping.copy(upds), bodyUpdates.plus(elseUpdates))
      }
      is While -> {
        val updates = foldWithState(exprTypes, s.body, ::getVarUpds)
        Pair(varTyping, updates.flatten())
      }
      is For -> {
        val iterTyp = getExprType(s.iter).asType()
        val bodyCtx = varTyping.copy(gatherUpdates(varTyping, s.target, getIterableElemType(iterTyp)))
        val updates = foldWithState(exprTypes.new(bodyCtx), s.body, ::getVarUpds).flatten()
        Pair(varTyping, updates)
      }
      is Try -> {
        val updates = foldWithState(exprTypes, s.body, ::getVarUpds).flatten()
        val handlersUpdtes = s.handlers.flatMap { foldWithState(exprTypes, it.body, ::getVarUpds).flatten() }

        val elseUpdates = foldWithState(exprTypes, s.orelse, ::getVarUpds).flatten()
        val finalUpdates = foldWithState(exprTypes, s.finalbody, ::getVarUpds).flatten()

        Pair(varTyping, updates.plus(handlersUpdtes).plus(elseUpdates).plus(finalUpdates))
      }
      is Return -> Pair(varTyping, emptyList())
      is Assert -> Pair(varTyping, emptyList())
      is Expr -> Pair(varTyping, emptyList())
      is Pass -> Pair(varTyping, emptyList())
      is Continue -> Pair(varTyping, emptyList())
      is Break -> Pair(varTyping, emptyList())
      else -> fail(s.toString())
    }
    typingAfter[s] = res.first
    return Pair(exprTypes.new(res.first), res.second)
  }

  val exprTypes = ExprTypes(globalCtx, IdentityHashMap())
  foldWithState(exprTypes, stmts, ::getVarUpds).flatten()

  return Analyses(typingBefore, typingAfter, args)
}
