package onotole.type_inference

import onotole.AnnAssign
import onotole.ArgRef
import onotole.Assign
import onotole.Attribute
import onotole.Bytes
import onotole.CTV
import onotole.Call
import onotole.ClassVal
import onotole.ClassValParam
import onotole.Comprehension
import onotole.ConstExpr
import onotole.Constant
import onotole.DefaultRef
import onotole.DictComp
import onotole.ExTypeVar
import onotole.ExprTransformer
import onotole.For
import onotole.FreshNames
import onotole.FuncInst
import onotole.FunctionDef
import onotole.GeneratorExp
import onotole.If
import onotole.Index
import onotole.Keyword
import onotole.KeywordRef
import onotole.Lambda
import onotole.Let
import onotole.ListComp
import onotole.Name
import onotole.NameConstant
import onotole.Num
import onotole.PositionalRef
import onotole.PyDict
import onotole.PyList
import onotole.SetComp
import onotole.Slice
import onotole.Starred
import onotole.Stmt
import onotole.Str
import onotole.Subscript
import onotole.TExpr
import onotole.Tuple
import onotole.TypeResolver
import onotole.While
import onotole.asCTVal
import onotole.asClassVal
import onotole.fail
import onotole.matchSig
import onotole.mkCall
import onotole.mkName
import onotole.rewrite.ExprTransformRule
import onotole.rewrite.combineRules
import onotole.typelib.TLSig
import onotole.typelib.TLTCallable
import onotole.typelib.TLTClass
import onotole.typelib.TLTConst
import onotole.typelib.TLTVar
import onotole.typelib.TLType

fun replaceTypeVars(t: TLType, tvars: Map<String,TLType>): TLType = when(t) {
  is TLTConst -> t
  is TLTVar -> tvars[t.name]!!
  is TLTClass -> t.copy(params = t.params.map { replaceTypeVars(it, tvars) })
  is TLTCallable -> t.copy(args = t.args.map { replaceTypeVars(it, tvars) }, ret = replaceTypeVars(t.ret, tvars))
}

fun ClassVal.toTLTClass(): TLTClass = TLTClass(this.name,
    this.tParams.map { it.asClassVal().toTLTClass() }.plus(this.eParams.map { TLTConst(it) }))


fun getDefaultValue(cls: TLTClass): TExpr {
  val f = cls.toFAtom(emptyMap()) as FAtom
  return when {
    f == FAtom("pylib.int") -> Num(0)
    f == FAtom("pylib.str") -> Str("")
    f == FAtom("pylib.bytes") -> Bytes("")
    f == FAtom("pylib.bool") -> NameConstant(false)
    f == FAtom("pylib.None") -> NameConstant(null)
    f.ps.isEmpty() -> CTV(ConstExpr(mkName(f.n + "_default")))
    f.n == "ssz.List" -> {
      val tParams = f.ps.map { it.toClassVal().toTLTClass() }
      mkCall(CTV(FuncInst("pylib.list", TLSig(tParams, emptyList(), TLTClass("pylib.PyList", tParams)))), listOf())
    }
    f.n == "ssz.Vector" -> {
      val tParams = f.ps.map { it.toClassVal().toTLTClass() }
      mkCall(CTV(FuncInst("pylib.list", TLSig(tParams, emptyList(), TLTClass("pylib.PyList", tParams)))), listOf())
    }
    f.n == "pylib.Dict" -> {
      val tParams = f.ps.map { it.toClassVal().toTLTClass() }
      mkCall(CTV(FuncInst("pylib.dict", TLSig(tParams, emptyList(), cls))), emptyList())
    }
    else -> TODO()
  }
}
context (TypingCtx)
fun getCtorSignatures(cls: ClassVal): List<TLSig> {
  val tltClass = cls.toTLTClass()
  val f = tltClass.toFAtom(emptyMap()) as FAtom
  val clsDecl = TypingContext.classes[f.n]!!
  return when {
    isST(f, FAtom("pylib.bytes")) -> {
      listOf(TLSig(emptyList(), emptyList(), tltClass),
          TLSig(emptyList(), listOf("_0" to TLTClass("pylib.str", emptyList())), tltClass),
          TLSig(emptyList(), listOf("_0" to TLTClass("pylib.Sequence", listOf(TLTClass("pylib.int", emptyList())))), tltClass)
      )
    }
    isST(f, FAtom("pylib.int")) -> {
      listOf(TLSig(emptyList(), emptyList(), tltClass),
          TLSig(emptyList(), listOf("_0" to TLTClass("pylib.str", emptyList())), tltClass),
          TLSig(emptyList(), listOf("_0" to TLTClass("pylib.int", emptyList())), tltClass)
      )
    }
    clsDecl.userDefined -> {
      val fields = clsDecl.attrs.filterValues { it is TLTClass }.mapValues { it.value as TLTClass }.toList()
      if (fields.size != clsDecl.attrs.size) TODO()
      if (fields.isNotEmpty()) {
        val defaults = fields.map { getDefaultValue(it.second) }
        listOf(TLSig(emptyList(), fields, tltClass, defaults))
      } else TODO()
    }
    getAncestorByClassName(f, "pylib.Sequence") != null -> {
      val base = getAncestorByClassName(f, "pylib.Sequence")!!
      listOf(TLSig(emptyList(), listOf(), tltClass),
          TLSig(emptyList(), listOf("_0" to base.toClassVal().toTLTClass()), tltClass))
    }
    else -> TODO()
  }
}
context (TypingCtx)
fun mkTypeVarRepalcer_Expr(typeVars: Map<String, ClassVal>): ExprTransformRule<Unit> {
  val typeVarReplacerRule2 = ExprTransformRule<Unit> { e, _ ->
    if (e is CTV && e.v is FuncInst) {
      if (e.v.sig.tParams.isNotEmpty()) {
        val remap = e.v.sig.tParams.filterIsInstance<TLTVar>().associate {
          it.name to (typeVars[it.name]?.toTLTClass() ?: fail())
        }
        val tparams = e.v.sig.tParams.map { replaceTypeVars(it, remap) }
        val args = e.v.sig.args.map { it.copy(second = replaceTypeVars(it.second, remap)) }
        val ret = replaceTypeVars(e.v.sig.ret, remap)
        e.copy(v = e.v.copy(sig = e.v.sig.copy(tParams = tparams, args = args, ret = ret)))
      } else null
    } else null
  }
  val typeVarReplacerRule3 = ExprTransformRule<Unit> { e, _ ->
    if (e is Lambda) {
      fun replace(e: TExpr): TExpr = when (e) {
        is CTV -> {
          if (e.v is ExTypeVar) {
            typeVars[e.v.v]?.let { CTV(it) } ?: e
          } else e
        }
        else -> e
      }

      val newArgs = e.args.args.map { it.copy(annotation = replace(it.annotation!!)) }
      val newRet = e.returns?.let { replace(it) }
      e.copy(args = e.args.copy(args = newArgs), returns = newRet)
    } else null
  }
  fun replaceTypeVars(e: ClassValParam): ClassValParam = when(e) {
    is ExTypeVar -> typeVars[e.v] ?: e
    is ClassVal -> e.copy(tParams = e.tParams.map { replaceTypeVars(it) })
  }
  val tvrGenRule = ExprTransformRule<Unit> { e, _ ->
    fun processComprehension(c: Comprehension): Comprehension {
      val anno = c.targetAnno!!
      val newAnno = if (anno is CTV && anno.v is ClassValParam)
        CTV(replaceTypeVars(anno.v).asCTVal())
      else fail()
      return c.copy(targetAnno = newAnno)
    }
    fun processComprehensions(cs: List<Comprehension>) = cs.map(::processComprehension)
    when (e) {
      is GeneratorExp -> e.copy(generators = processComprehensions(e.generators))
      is ListComp, is SetComp, is DictComp -> TODO()
      else -> null
    }
  }
  val listRule = ExprTransformRule<Unit> { e, _ ->
    if (e is PyList) {
      e.copy(valueAnno = replaceTypeVars(e.valueAnno!!))
    } else null
  }
  val dictRule = ExprTransformRule<Unit> { e, _ ->
    if (e is PyDict) {
      e.copy(keyAnno = replaceTypeVars(e.keyAnno!!), valueAnno = replaceTypeVars(e.valueAnno!!))
    } else null
  }
  return combineRules(typeVarReplacerRule2, typeVarReplacerRule3, tvrGenRule, listRule, dictRule)
}


fun replaceTypeVars(typeVars: Map<String,ClassVal>, f: FunctionDef): FunctionDef {
  fun getTargets(t: TExpr): List<String> = when(t) {
    is Name -> listOf(t.id)
    is Tuple -> t.elts.flatMap(::getTargets)
    else -> emptyList()
  }
  fun getTargets(s: Stmt): List<String> = when(s) {
    is Assign -> getTargets(s.target)
    is AnnAssign -> getTargets(s.target)
    is If -> s.body.flatMap(::getTargets).plus(s.orelse.flatMap(::getTargets))
    is While -> s.body.flatMap(::getTargets)
    is For -> s.body.flatMap(::getTargets)
    else -> emptyList()
  }

  val phiTargets = f.body.flatMap(::getTargets).groupBy { it }.mapValues { it.value.size }.filterValues { it >= 2 }.keys

  return f.copy(body = TypeVarReplacer(typeVars, phiTargets).procStmts(f.body, Unit).first)
}
class TypeVarReplacer(val typeVars: Map<String,ClassVal>, val phiTargets: Set<String>): ExprTransformer<Unit>() {
  private val exprRule = with(TypingContext) { mkTypeVarRepalcer_Expr(typeVars) }
  override fun merge(a: Unit, b: Unit) {}

  override fun procStmt(s: Stmt, ctx: Unit): Pair<Stmt, Unit> {
    return when(s) {
      is Assign -> {
        if (s.target is Name && s.target.id !in phiTargets) {
          val t = typeVars["T" + s.target.id]
          if (t != null) {
            super.procStmt(AnnAssign(s.target, CTV(t), s.value), ctx)
          } else
            super.procStmt(s, ctx)
        } else super.procStmt(s, ctx)
      }
      else -> super.procStmt(s, ctx)
    }
  }

  override fun transform(e: TExpr, ctx: Unit, store: Boolean): TExpr {
    val r = exprRule.invoke(e, ctx) ?: e
    return defaultTransform(r, ctx, store)
  }
}

fun transformCallSites(typeVars: Map<String,ClassVal>, f: FunctionDef): FunctionDef {
  return f.copy(body = CallSiteTransormer().procStmts(f.body, typeVars).first)
}
class CallSiteTransormer(): ExprTransformer<Map<String,ClassVal>>() {
  private val callRule = mkCallArgsTransformRule(FreshNames())
  override fun merge(a: Map<String,ClassVal>, b: Map<String,ClassVal>): Map<String,ClassVal> {
    return if (a == b) a else TODO()
  }

  override fun transform(e: TExpr, ctx: Map<String, ClassVal>, store: Boolean): TExpr {
    val ctorRule = with(TypingContext) { mkCtorArgsTransformRule(ctx) }
    val r1 = ctorRule.invoke(e, Unit) ?: e
    val r2 = callRule.invoke(r1, Unit) ?: r1
    return if (r2 == e)
      defaultTransform(e, ctx, store)
    else
      r2
  }
  override fun defaultTransform(e: TExpr, ctx: Map<String,ClassVal>, store: Boolean): TExpr {
    return if (e is GeneratorExp) {
      val (gens, newCtx) = processComprehensions(e.generators, ctx)
      super.defaultTransform(e.copy(generators = gens), newCtx, store)
    } else if (e is ListComp) {
      val (gens, newCtx) = processComprehensions(e.generators, ctx)
      super.defaultTransform(e.copy(generators = gens), newCtx, store)
    } else if (e is SetComp || e is DictComp) {
      TODO()
    } else if (e is Lambda) {
      val newCtx = ctx.plus(e.args.args.map { ("T" + it.arg) to ((it.annotation!! as CTV).v as ClassVal) })
      super.defaultTransform(e, newCtx, store)
    } else super.defaultTransform(e, ctx, store)
  }
  fun processComprehensions(gens: Collection<Comprehension>, ctx: Map<String,ClassVal>): Pair<List<Comprehension>, Map<String,ClassVal>> {
    var currCtx = ctx
    val newGens = mutableListOf<Comprehension>()
    gens.forEach { c ->
      val newIter = transform(c.iter, currCtx)
      currCtx = currCtx.plus(extractAnnoTypes(c))
      val newIfs = transform(c.ifs, currCtx)
      newGens.add(c.copy(iter = newIter, ifs = newIfs))
    }
    return newGens to currCtx
  }
}

fun extractAnnoTypes(c: Comprehension): List<Pair<String, ClassVal>> {
  val genArgs = when (c.target) {
    is Name -> listOf(c.target.id)
    is Tuple -> c.target.elts.map { (it as Name).id }
    else -> fail()
  }.map { "T$it" }
  val anno = c.targetAnno
  if (anno == null || anno !is CTV || anno.v !is ClassVal) fail()
  val argTypes = if (genArgs.size == 1) {
    if (anno.v.name == "pylib.Tuple") fail()
    listOf(anno.v)
  } else {
    if (anno.v.name != "pylib.Tuple") fail()
    anno.v.tParams.map { it.asClassVal() }
  }
  return genArgs.zip(argTypes)
}
fun alignArgs(sig: TLSig, args: List<TExpr>, keywords: List<Keyword>): List<ArgRef> {
  val res = arrayOfNulls<ArgRef>(sig.args.size)
  sig.defaults.indices.forEach { i ->
    res[i + (sig.args.size - sig.defaults.size)] = DefaultRef(i)
  }
  args.indices.forEach { i ->
    res[i] = PositionalRef(i)
  }
  fun findIndex(n: String): Int? {
    sig.args.forEachIndexed { i, (a, _) ->
      if (a == n)
        return i
    }
    return null
  }
  keywords.forEachIndexed { i, k ->
    val idx = findIndex(k.arg!!) ?: fail()
    if (res[idx] != null && res[idx] !is DefaultRef) fail()
    res[idx] = KeywordRef(i)
  }
  return res.map { it!! }
}

context (TypingCtx)
fun mkCtorArgsTransformRule(typeVars: Map<String, ClassVal>) = ExprTransformRule<Unit> { e, _ ->
  if (e is Call && e.func is CTV && e.func.v is ClassVal) {
    val tvs = typeVars
    val sigs = getCtorSignatures(e.func.v)
    val kwdNames = e.keywords.map { it.arg!! }
    val sigs2 = sigs.filter { matchSig(it, e.args.size, kwdNames) }

    fun ClassVal.toFAtom() = this.toTLTClass().toFAtom(emptyMap()) as FAtom

    fun getType(e: TExpr, typeVars: Map<String, ClassVal>): ClassVal = when(e) {
      is Name -> typeVars["T" + e.id]
          ?: TypingContext.constTypes[e.id]?.toClassVal()
          ?: fail()
      is Str -> ClassVal("pylib.str")
      is Bytes -> ClassVal("pylib.bytes")
      is Num -> ClassVal("pylib.int")
      is NameConstant -> when (e.value) {
        null -> ClassVal("pylib.None")
        is Boolean -> ClassVal("pylib.bool")
        else -> TODO()
      }
      is CTV -> {
        when(e.v) {
          is ConstExpr -> getType(e.v.e, typeVars)
          else -> TODO()
        }
      }
      is Attribute -> {
        resolveAttributeGet(getType(e.value, typeVars).toFAtom(), e.attr).toClassVal()
      }
      is Subscript -> {
        val valType = getType(e.value, typeVars).toFAtom()
        when(e.slice) {
          is Index -> {
            resolveIndexGet(valType, getType(e.slice.value, typeVars).toFAtom()).toClassVal()
          }
          is Slice -> {
            resolveSliceGet(valType, null, null, null).toClassVal()
          }
          else -> TODO()
        }
      }
      is Call -> {
        when {
          e.func is CTV && e.func.v is FuncInst -> e.func.v.sig.ret.toFAtom(emptyMap()).toClassVal()
          e.func is CTV && e.func.v is ClassVal -> e.func.v
          e.func is Attribute -> {
            resolveAttributeCall(getType(e.func.value, typeVars).toFAtom(), e.func.attr).first.toClassVal()
          }
          e.func is Name && e.func.id in TypeResolver.specialFuncNames -> {
            val op = e.func.id.substring(1, e.func.id.length-1)
            if (e.keywords.isNotEmpty()) fail()
            val args = e.args.map { getType(it, typeVars).toFAtom() }
            val res = resolveSpecialOpType(op, args)
            res.toClassVal()
          }
          else -> TODO()
        }
      }
      is GeneratorExp -> {
        if (e.generators.size != 1) TODO()
        val newCtx = typeVars.plus(extractAnnoTypes(e.generators[0]))
        ClassVal("pylib.Sequence", listOf(getType(e.elt, newCtx)))
      }
      is PyList -> ClassVal("pylib.PyList", listOf(e.valueAnno!!.asClassVal()))
      is PyDict -> ClassVal("pylib.Dict", listOf(e.keyAnno!!.asClassVal(), e.valueAnno!!.asClassVal()))
      is Starred -> {
        ClassVal("pylib.Sequence", listOf(getType(e.value, typeVars)))
      }
      else -> TODO()
    }
    fun checkSigTypes(t: TLSig): Boolean {
      val aligned = alignArgs(t, e.args, e.keywords)
      aligned.forEach { ar ->
        when(ar) {
          is PositionalRef -> {
            val paramType = t.args[ar.idx].second as TLTClass
            val argType = getType(e.args[ar.idx], typeVars)
            if (!canConvertTo(argType.toTLTClass().toFAtom(emptyMap()) as FAtom, paramType.toFAtom(emptyMap()) as FAtom))
              return false
          }
          is KeywordRef -> {
            val paramType = t.args.find { it.first == e.keywords[ar.idx].arg!! }!!.second
            val argType = getType(e.keywords[ar.idx].value, typeVars)
            if (!canConvertTo(argType.toTLTClass().toFAtom(emptyMap()) as FAtom, paramType.toFAtom(emptyMap()) as FAtom))
              return false
          }
          else -> {}
        }
      }
      return true
    }
    val sigs3 = sigs2.filter { checkSigTypes(it) }
    if (sigs3.isEmpty()) fail()
    if (sigs3.size > 1) fail()
    e.copy(func = CTV(FuncInst(e.func.v.name + "::new", sigs3.first())))
  } else null
}

fun mkCallArgsTransformRule(freshNames: FreshNames) = ExprTransformRule<Unit> { e, _ ->
  if (e is Call) {
    when {
      e.func is CTV && e.func.v is FuncInst -> {
        val sig = e.func.v.sig
        val args = alignArgs(sig, e.args, e.keywords)
        val ordering = args.map { ar ->
          val argVal = when(ar) {
            is PositionalRef -> e.args[ar.idx]
            is KeywordRef -> e.keywords[ar.idx].value
            is DefaultRef -> sig.defaults[ar.idx]
          }
          if (argVal is Name || argVal is Constant)
            -1
          else when(ar) {
            is PositionalRef -> ar.idx
            is KeywordRef -> ar.idx + e.args.size
            is DefaultRef -> -1
          }
        }.filter { it >= 0 }
        if (ordering.sorted() != ordering) {
          val posBindings = List(e.args.size) { i ->
            val n = freshNames.fresh("pa$i")
            Keyword(n, e.args[i])
          }
          val kwdBindings = List(e.keywords.size) { i ->
            val n = freshNames.fresh("ka${e.keywords[i].arg}")
            Keyword(n, e.keywords[i].value)
          }
          val bindings = posBindings.plus(kwdBindings)
          val args2 = args.map {
            when(it) {
              is PositionalRef -> mkName(posBindings[it.idx].arg!!)
              is KeywordRef -> mkName(kwdBindings[it.idx].arg!!)
              is DefaultRef -> sig.defaults[it.idx]
            }
          }
          Let(bindings = bindings, e.copy(args = args2, keywords = emptyList()))
        } else {
          val args2 = args.map { ar ->
            when(ar) {
              is PositionalRef -> e.args[ar.idx]
              is KeywordRef -> e.keywords[ar.idx].value
              is DefaultRef -> sig.defaults[ar.idx]
            }
          }
          e.copy(args = args2, keywords = emptyList())
        }
      }
      else -> null
    }
  } else null
}