package onotole.type_inference

import onotole.AnnAssign
import onotole.ArgRef
import onotole.Assign
import onotole.Bytes
import onotole.CTV
import onotole.Call
import onotole.ClassVal
import onotole.ConstExpr
import onotole.DefaultRef
import onotole.ExTypeVar
import onotole.ExprTransformer
import onotole.FreshNames
import onotole.FuncInst
import onotole.Keyword
import onotole.KeywordRef
import onotole.Lambda
import onotole.Let
import onotole.Name
import onotole.NameConstant
import onotole.Num
import onotole.PositionalRef
import onotole.Stmt
import onotole.Str
import onotole.TExpr
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
    this.tParams.map { it.toTLTClass() }.plus(this.eParams.map { TLTConst(it) }))


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
      listOf(TLSig(emptyList(), listOf("_0" to base.toClassVal().toTLTClass()), tltClass))
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
  val tvrRule4 = ExprTransformRule<Unit> { e, _ ->
    if (e is Call && e.func is CTV && e.func.v is ClassVal) {
      val tvs = typeVars
      val sigs = getCtorSignatures(e.func.v)
      val kwdNames = e.keywords.map { it.arg!! }
      val sigs2 = sigs.filter { matchSig(it, e.args.size, kwdNames) }
      fun getType(e: TExpr) = when(e) {
        is Name -> typeVars["T" + e.id]!!
        is Str -> ClassVal("pylib.str")
        is Bytes -> ClassVal("pylib.bytes")
        is Num -> ClassVal("pylib.int")
        is NameConstant -> when (e.value) {
          null -> ClassVal("pylib.None")
          is Boolean -> ClassVal("pylib.bool")
          else -> TODO()
        }
        else -> TODO()
      }
      fun checkSigTypes(t: TLSig): Boolean {
        val aligned = alignArgs(t, e.args, e.keywords)
        aligned.forEach { ar ->
          when(ar) {
            is PositionalRef -> {
              val paramType = t.args[ar.idx].second as TLTClass
              val argType = getType(e.args[ar.idx])
              if (!canConvertTo(argType.toTLTClass().toFAtom(emptyMap()) as FAtom, paramType.toFAtom(emptyMap()) as FAtom))
                return false
            }
            is KeywordRef -> {
              val paramType = t.args.find { it.first == e.keywords[ar.idx].arg!! }!!.second
              val argType = getType(e.keywords[ar.idx].value)
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
      null
    } else null
  }
  return combineRules(typeVarReplacerRule2, typeVarReplacerRule3, tvrRule4)
}

class TypeVarReplacer(val typeVars: Map<String,ClassVal>): ExprTransformer<Unit>() {
  private val callRule = mkTtttttRule(FreshNames())
  private val exprRule = with(TypingContext) { mkTypeVarRepalcer_Expr(typeVars) }
  override fun merge(a: Unit, b: Unit) {}

  override fun procStmt(s: Stmt, ctx: Unit): Pair<Stmt, Unit> {
    return when(s) {
      is Assign -> {
        if (s.target is Name) {
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
    val r = exprRule.invoke(e, ctx) ?: defaultTransform(e, ctx, store)
    return callRule.invoke(r, ctx) ?: r
  }
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
fun mkTtttttRule(freshNames: FreshNames) = ExprTransformRule<Unit> { e, _ ->
  if (e is Call) {
    when {
      e.func is CTV && e.func.v is FuncInst -> {
        val sig = e.func.v.sig
        val args = alignArgs(sig, e.args, e.keywords)
        val ordering = args.map { ar ->
          when(ar) {
            null -> fail()
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
          TODO()
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