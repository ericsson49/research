package onotole

import onotole.lib_defs.Additional
import onotole.lib_defs.BLS
import onotole.lib_defs.PyLib
import onotole.lib_defs.SSZLib
import onotole.rewrite.ExprTransformRule
import onotole.rewrite.combineRules
import onotole.rewrite.mkExprTransformRule
import onotole.type_inference.CallSiteTransormer
import onotole.type_inference.FAtom
import onotole.type_inference.TypeVarReplacer
import onotole.type_inference.TypingContext
import onotole.type_inference.inferConstTypes
import onotole.type_inference.inferTypes
import onotole.type_inference.inferTypes2
import onotole.type_inference.replaceTypeVars
import onotole.type_inference.toFAtom
import onotole.type_inference.transformCallSites
import onotole.typelib.*


sealed class CTVal
sealed interface CTLocal
sealed interface ClassValParam
fun ClassValParam.asClassVal() = when(this) { is ClassVal -> this; else -> TODO() }
fun ClassValParam.asTypeVar() = when(this) { is ExTypeVar -> this; else -> TODO() }
fun ClassValParam.asCTVal(): CTVal = when(this) { is ExTypeVar -> this; is ClassVal -> this }
object CTNothing : CTVal()
data class ExTypeVar(val v: String) : CTVal(), ClassValParam
data class ConstExpr(val e: TExpr) : CTVal(), CTLocal
data class PkgVal(val pkg: String) : CTVal()
data class ClassTemplate(val clsTempl: TLClassHead) : CTVal()
data class ClassVal(val name: String, val tParams: List<ClassValParam> = emptyList(), val eParams: List<ConstExpr> = emptyList()) : CTVal(), CTLocal, ClassValParam
data class ClassField(val cls: String, val field: String) : CTVal()
data class FuncTempl(val func: TLFuncDecl) : CTVal()
data class FuncInst(val name: String, val sig: TLSig) : CTVal()

val aliasResolveRule: ExprTransformRule<TestResolver> = mkExprTransformRule { e, ctx ->
  if (e is Name) {
    val resolvedName = ctx.resolveAlias(e.id) ?: e.id
    if (resolvedName != e.id) e.copy(id = resolvedName) else null
  } else null
}

val nameResolveRule: ExprTransformRule<TestResolver> = mkExprTransformRule { e, ctx ->
  if (e is Name) ctx.getVal(e.id) else null
}

val staticNameResolveRule: ExprTransformRule<TestResolver> = mkExprTransformRule { e, _ ->
  if (e is Attribute && e.value is CTV) {
    when (val v = e.value.v) {
      is PkgVal -> Name(v.pkg + "." + e.attr, e.ctx)
      is ClassVal -> Name(v.name + "#" + e.attr, e.ctx)
      is ClassTemplate -> TODO()
      else -> null
    }
  } else null
}

fun matchSig(s: TLSig, noPosArgs: Int, kwds: List<String>): Boolean {
  if (noPosArgs > s.args.size)
    return false
  val kwdArgs = s.args.subList(noPosArgs, s.args.size).map { it.first }
  val unknownArgs = kwds.minus(kwdArgs.toSet())
  if (unknownArgs.isNotEmpty())
    return false
  val argsWithDefaults = s.args.subList(s.args.size - s.defaults.size, s.args.size).map { it.first }
  val unassignedArgs = kwdArgs.minus(kwds.plus(argsWithDefaults).toSet())
  if (unassignedArgs.isNotEmpty())
    return false
  return true
}

fun mkCompileTimeCalcRule(freshNames: FreshNames): ExprTransformRule<TestResolver> {
  val subscriptCase: ExprTransformRule<TestResolver> = mkExprTransformRule { e, _ ->
    if (e is Subscript) {
      val value = e.value
      if (value is CTV && value.v is ClassTemplate) {
        val index = if (e.slice is Index) e.slice.value else fail()
        val indices = if (index is Tuple) index.elts else listOf(index)
        val clsTempl = value.v.clsTempl
        val (tparams, eparams) = if (clsTempl.name == "pylib.Tuple") {
          indices to emptyList()
        } else {
          if (indices.size != clsTempl.noTParams + clsTempl.noEParams)
            fail()
          val tparams = indices.subList(0, clsTempl.noTParams)
          val eparams = indices.subList(clsTempl.noTParams, clsTempl.noTParams + clsTempl.noEParams)
          tparams to eparams
        }
        if (!tparams.all { it is CTV && it.v is ClassVal }) fail()
        val eparams3 = eparams.map { if (it is CTV && it.v is ConstExpr) it else CTV(ConstExpr(it)) }
        val tparams2 = tparams.map { (it as CTV).v as ClassVal }
        val eparams2 = eparams3.map { it.v as ConstExpr }
        CTV(ClassVal(clsTempl.name, tparams2, eparams2))
      } else null
    } else null
  }
  val callCase: ExprTransformRule<TestResolver> = mkExprTransformRule { e, _ ->
    if (e is Call) {
      val func = e.func
      fun isConstExprOrClassVal(e: TExpr) = e is CTV && (e.v is ConstExpr || e.v is ClassVal)
      if (func !is CTV && func !is Attribute && !(func is Name && func.id in TypeResolver.specialFuncNames))
        fail()
      if (func is Name && func.id in TypeResolver.specialFuncNames && e.args.all(::isConstExprOrClassVal)) {
        if (e.keywords.isNotEmpty()) fail()
        CTV(ConstExpr(e))
      } else if (func is CTV) {
        if (func.v is FuncTempl) {
          val sigs = func.v.func.sigs
          val sig = sigs.find { matchSig(it, e.args.size, e.keywords.map { it.arg!! }) }
              ?: fail()
          val tvRemap = sig.tParams.filterIsInstance<TLTVar>().associate { it.name to freshNames.fresh("?$it") }
          e.copy(func = CTV(FuncInst(func.v.func.name, renameTVars(sig, tvRemap))))
        } else null
      } else null
    } else null
  }
  val listCase: ExprTransformRule<TestResolver> = mkExprTransformRule { e, _ ->
    if (e is PyList) {
      val tvv = TLTVar(freshNames.fresh("?T"))
      //val sig = TLSig(listOf(tvv), e.elts.indices.map { "_$it" to tvv }, TLTClass("pylib.PyList", listOf(tvv)))
      //mkCall(CTV(FuncInst("pylib.PyList", sig)), e.elts)
      e.copy(valueAnno = ExTypeVar(tvv.name))
    } else null
  }
  val dictCase: ExprTransformRule<TestResolver> = mkExprTransformRule { e, _ ->
    if (e is PyDict) {
      if (e.keyAnno != null || e.valueAnno != null) TODO()
      val ktv = TLTVar(freshNames.fresh("?K"))
      val vtv = TLTVar(freshNames.fresh("?V"))
      /*val items = Tuple(elts = e.keys.zip(e.values).map { (k,v) ->
        Tuple(elts = listOf(k, v), ctx = ExprContext.Load)
      }, ctx = ExprContext.Load)
      val argTypes = listOf(
          "items" to TLTClass("pylib.Sequence", listOf(TLTClass("pylib.Tuple", listOf(ktv, vtv)))))
      val resType = TLTClass("pylib.Dict", listOf(ktv, vtv))
      mkCall(CTV(FuncInst("pylib.Dict", TLSig(listOf(ktv, vtv), argTypes, resType))), listOf(items))*/
      e.copy(keyAnno = ExTypeVar(ktv.name), valueAnno = ExTypeVar(vtv.name))
    } else null
  }
  val lambdaCase: ExprTransformRule<TestResolver> = mkExprTransformRule { e, _ ->
    if (e is Lambda) {
      e.copy(
          args = e.args.copy(args = e.args.args.map {
            if (it.annotation != null) it
            else it.copy(annotation = CTV(ExTypeVar(freshNames.fresh("?L"))))
          }),
          returns = e.returns ?: CTV(ExTypeVar(freshNames.fresh("?R"))),
      )
    } else null
  }
  fun convertGenerator(c: Comprehension): Comprehension {
    return c.copy(targetAnno = when (c.target) {
      is Name -> CTV(ExTypeVar(freshNames.fresh("?G")))
      is Tuple -> CTV(ClassVal("pylib.Tuple", c.target.elts.map { ExTypeVar(freshNames.fresh("?G")) }))
      else -> fail()
    })
  }
  fun convertGenerators(cs: Collection<Comprehension>) = cs.map(::convertGenerator)
  val generatorCase: ExprTransformRule<TestResolver> = mkExprTransformRule { e, _ ->
    when(e) {
      is GeneratorExp -> e.copy(generators = convertGenerators(e.generators))
      is ListComp, is SetComp, is DictComp -> TODO()
      else -> null
    }
  }
  return combineRules(nameResolveRule, staticNameResolveRule, subscriptCase, callCase, listCase, dictCase, lambdaCase, generatorCase)
}

open class BaseNameTransformer(val transformRule: ExprTransformRule<TestResolver>) : ExprTransformer<TestResolver>() {
  fun findNames(e: TExpr): List<String> = when (e) {
    is Name -> listOf(e.id)
    is Attribute -> emptyList()
    is Subscript -> emptyList()
    is Tuple -> e.elts.flatMap { findNames(it) }
    else -> fail()
  }

  override fun transform(e: TExpr, ctx: TestResolver, store: Boolean): TExpr {
    val r = defaultTransform(e, ctx, store)
    return transformRule.invoke(r, ctx) ?: r
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

fun mkPhaseMod(phase: String): TLModLoader {
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

fun main() {
  PyLib.init()
  SSZLib.init()
  BLS.init()
  val specVersion = "phase0"
  Additional.init(specVersion)
  val tlDefs = loadSpecDefs(specVersion)
  PhaseInfo.getPkgDeps(specVersion).forEach {
    TypeResolver.importFromPackage(it)
  }
  TypeResolver.importFromPackage(specVersion)



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
  val gen = DafnyGen(specVersion, setOf("bls", "ssz", specVersion))
  p0Module.definitions.filterIsInstance<TLFuncDef>().forEach {
    val f = convertToAndOutOfSSA(it.func)
    pyPrintFunc(f)
    val types = inferTypes2(f, ssa = true)
    val res = replaceTypeVars(types, f)
    val res2 = transformCallSites(types, res)
    //pyPrintFunc(res2)
    gen.genFunc(res2).forEach(::println)
    println("------------")
  }

  val altairModule = TopLevelScope.resolveModule("altair")
  val constTypes_alt = inferConstTypes(altairModule.constantDefs.map { it.name to it.value.const.e })
  TypingContext.initConstants(constTypes_alt)
  altairModule.definitions.filterIsInstance<TLFuncDef>().forEach {
    val f = desugarStmts(convertToAndOutOfSSA(it.func))
    val types = inferTypes2(f, ssa = true)
    val res = replaceTypeVars(types, f)
    //val res2 = transformCallSites(types, res)
    //pyPrintFunc(res2)
    //gen.genFunc(res2).forEach(::println)
    //println("------------")
  }

  val bellatrixModule = TopLevelScope.resolveModule("bellatrix")
  val btxconst_decls = bellatrixModule.constantDecls.map { it.name to it.type.toFAtom(emptyMap()) as FAtom }.toMap()
  TypingContext.initConstants(btxconst_decls)
  val constTypes_btx = inferConstTypes(bellatrixModule.constantDefs.map { it.name to it.value.const.e })
  TypingContext.initConstants(constTypes_btx)
  bellatrixModule.definitions.filterIsInstance<TLFuncDef>().forEach {
    val f = desugarStmts(convertToAndOutOfSSA(it.func))
    val types = inferTypes2(f, ssa = true)
    val res = replaceTypeVars(types, f)
    val res2 = transformCallSites(types, res)
    //pyPrintFunc(res2)
    //gen.genFunc(res2).forEach(::println)
    //println("------------")
  }
}
