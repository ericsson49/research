package onotole

import onotole.dafny.ComprehensionTransformer
import onotole.dafny.DafnyExprGen
import onotole.exceptions.ExceptionAnalysis
import onotole.exceptions.SimpleExcnChecker
import onotole.lib_defs.phaseModuleDefs
import onotole.rewrite.ExprTransformRule
import onotole.rewrite.combineRules
import onotole.rewrite.mkExprTransformRule
import onotole.type_inference.FAtom
import onotole.type_inference.TypingContext
import onotole.type_inference.TypingCtx
import onotole.type_inference.alignArgs
import onotole.type_inference.canConvertTo
import onotole.type_inference.inferConstTypes
import onotole.type_inference.inferTypes2
import onotole.type_inference.replaceTypeVars
import onotole.type_inference.transformCallSites
import onotole.typelib.*
import onotole.util.toClassVal
import onotole.util.toFAtom
import onotole.util.toTExpr


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

context (TypingCtx)
fun checkSigTypes(t: TLSig, argTypes: List<RTType>, kwdTypes: List<Pair<String,RTType>>): Boolean {
  val aligned = alignArgs(t, argTypes.size, kwdTypes.map { it.first })
  aligned.forEach { ar ->
    when(ar) {
      is PositionalRef -> {
        val paramType = t.args[ar.idx].second as TLTClass
        val argType = argTypes[ar.idx]
        if (!canConvertTo(argType.toFAtom(), paramType.toFAtom(emptyMap()) as FAtom))
          return false
      }
      is KeywordRef -> {
        val paramType = t.args.find { it.first == kwdTypes[ar.idx].first }!!.second
        val argType = kwdTypes[ar.idx].second
        if (!canConvertTo(argType.toFAtom(), paramType.toFAtom(emptyMap()) as FAtom))
          return false
      }
      else -> {}
    }
  }
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
      e.copy(valueAnno = ExTypeVar(tvv.name))
    } else null
  }
  val dictCase: ExprTransformRule<TestResolver> = mkExprTransformRule { e, _ ->
    if (e is PyDict) {
      if (e.keyAnno != null || e.valueAnno != null) TODO()
      val ktv = TLTVar(freshNames.fresh("?K"))
      val vtv = TLTVar(freshNames.fresh("?V"))
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

fun mkPhaseModule(phase: String): TLModLoader {
  val (_, defs) = PhaseInfo.getPhaseDefs(phase, false)

  val modDefs = defs.map { when(it) {
    is ConstTLDef -> TLConstDef(it.name, TLTConst(ConstExpr((desugarExprs(it.const) as Assign).value)))
    is ClassTLDef -> TLClassDef(desugarExprs(it.clazz))
    is FuncTLDef -> TLFuncDef(simplifyFunc(it.func))
  } }

  val moduleDef = phaseModuleDefs.find { it.name == phase }!!
  return mkModule(phase, moduleDef.deps, modDefs, moduleDef.extern)
}

fun simplifyFunc(f: FunctionDef) = desugarExprs(destructForLoops(destructTupleAssign(transformForEnumerate(transformForOps(f)))))

fun main() {
  val specVersion = "phase0"

  val modLoaders = listOf(pylib, ssz, bls, mkPhaseModule("phase0")/*, mkPhaseModule("altair"), mkPhaseModule("bellatrix")*/)
  modLoaders.forEach(TopLevelScope::registerModule)
  TypingContext.registerModules(modLoaders.map { TopLevelScope.resolveModule(it.name) })

  val p0Module = TopLevelScope.resolveModule("phase0")
  val constTypes_p0 = inferConstTypes(p0Module.constantDefs.map { it.name to it.value.const.e })
  TypingContext.initConstants(constTypes_p0)
  val aa = TypingContext.classes.filterValues { (it.head.noTParams + it.head.noEParams) == 0 }
  val simpleClasses = aa.mapValues { NamedType(it.value.head.name) }
  val tlTyper1 = TypeResolver.topLevelTyper.updated(simpleClasses)
  val constantsSort = constTypes_p0.mapValues { parseType(tlTyper1, it.value.toClassVal().toTExpr()) }
  val tlTyper = tlTyper1.updated(constantsSort)
  val gen = DafnyGen(tlTyper)

  val forkChoiceFuncs = setOf(
      "compute_start_slot_at_epoch",
      "compute_epoch_at_slot",
      // fork choice methods
      "is_previous_epoch_justified",
      "get_forkchoice_store",
      "get_slots_since_genesis",
      "get_current_slot",
      "compute_slots_since_epoch_start",
      "get_ancestor",
      "get_latest_attesting_balance",
      "filter_block_tree",
      "get_filtered_block_tree",
      "get_head",
      "should_update_justified_checkpoint",
      "update_checkpoints",
      "pull_up_tip",
      "on_tick_per_slot",
      "validate_target_epoch_against_current_time",
      "validate_on_attestation",
      "store_target_checkpoint_state",
      "update_latest_messages",
      "on_tick",
      "on_block",
      "on_attestation",
      "on_attester_slashing"
  )
  val p0ds = phase0PDs()
      .mapKeys { "phase0." + it.key }
      .mapValues { it.value.copy(pureName = "phase0." + it.value.pureName) }
      .plus("attr_append" to PurityDescriptor(true, listOf(0), "attr_append_pure"))
      .plus("attr_add" to PurityDescriptor(true, listOf(0), "attr_add_pure"))
      .plus("phase0.get_head" to PurityDescriptor(false, listOf(0), "phase0.get_head_pure"))
      .plus("<Result>::new" to PurityDescriptor(false, emptyList(), "<Result>::new"))

  val p0Funcs = p0Module.definitions.filterIsInstance<TLFuncDef>()
  val transformedAndTypes = p0Funcs.map {
    //val f_prev = convertToAndOutOfSSA(it.func)
    val comprehensionTransformer = ComprehensionTransformer(tlTyper)
    val shortName = it.name.substring("phase0.".length)
    val f_ = if (shortName in forkChoiceFuncs && !fcFuncsDescr[shortName]!!.function)
      comprehensionTransformer.transform(it.func)
    else
      it.func
    //pyPrintFunc(f_)
    val f = convertToAndOutOfSSA2(f_)
    //pyPrintFunc(f)
    val varTypes = inferTypes2(f, ssa = true)
    val res = replaceTypeVars(varTypes, f)
    val res2 = transformCallSites(tlTyper, res)
    //pyPrintFunc(res2)
    res2 to varTypes.filterKeys { it.startsWith("T") }.mapKeys { it.key.substring(1) }
  }

  val forkChoice = transformedAndTypes.filter {
    val shortName = it.first.name.substring("phase0.".length)
    shortName in forkChoiceFuncs
  }
  val depsExcn = mapOf(
      "phase0.get_current_epoch" to false,
      "phase0.get_active_validator_indices" to false,
      "phase0.get_total_active_balance" to false,
      "phase0.get_indexed_attestation" to false,
      "phase0.is_valid_indexed_attestation" to false,
      "phase0.is_slashable_attestation_data" to false,
      "phase0.process_slots" to true,
      "phase0.state_transition" to true,
      "phase0.process_justification_and_finalization" to true
  )

  val excnAnalysis = ExceptionAnalysis("phase0", forkChoice.unzip().first, SimpleExcnChecker(depsExcn), tlTyper)
  val funcsExcn = excnAnalysis.solve()

  val excnChecker = SimpleExcnChecker(depsExcn).updated(funcsExcn.toList())


  transformedAndTypes.forEach { (res2, varTypes) ->
    val shortName = res2.name.substring("phase0.".length)
    if (shortName in forkChoiceFuncs) {
      val typer = tlTyper.updated(res2.args.args.map { it.arg to parseType(tlTyper, it.annotation!!) })
      val excnStmtProcessor = DeExceptionizer(excnChecker, fcFuncsDescr)
      val methProcessor = MethodProcessor(FreshNames(), object: EffectDetector {
        override fun hasEffect(e: TExpr, typer: ExprTyper, recursive: Boolean): Boolean {
          return checkSideEffects(e, typer, recursive)
        }
      })
      val res2_ = if (fcFuncsDescr[shortName]!!.exception)
        methProcessor.transformFunc(res2, typer)
      else res2
      //pyPrintFunc(res_)
      val res3 = if (fcFuncsDescr[shortName]!!.exception)
        excnStmtProcessor.transformFunc(res2_, typer)
      else res2_


      //val res3p = purify2(res4, descrs)
      //pyPrintFunc(res3p)
      gen.genFunc(res3).forEach(::println)
      println()

      val pf = purify2(res3, p0ds, tlTyper)
      val funcTyper = tlTyper.updated(varTypes.mapValues { toRTType(it.value) })
      val funcPure = MethodToFuncTransformer(pf, funcTyper).transform()
      val nonPureMeths = p0ds.mapValues { it.value.pureName }
      val ttt = DafnyExprGen(gen::genNativeType, false, emptySet(), nonPureMeths)
      val rrr = ttt.genExpr(funcPure, typer)
      val args = pf.args.args.joinToString(", ") { gen.genArg(it) }
      val shortPFName = pf.name.substring("phase0.".length)
      println("function method ${shortPFName}($args): ${gen.genNativeType(pf.returns!!)} {")
      println("  " + gen.render(rrr))
      println("}")
      println()
      println()
      //println("------------")
    }
  }


//  val altairModule = TopLevelScope.resolveModule("altair")
//  val constTypes_alt = inferConstTypes(altairModule.constantDefs.map { it.name to it.value.const.e })
//  TypingContext.initConstants(constTypes_alt)
//  altairModule.definitions.filterIsInstance<TLFuncDef>().forEach {
//    val f = desugarStmts(convertToAndOutOfSSA(it.func))
//    val types = inferTypes2(f, ssa = true)
//    val res = replaceTypeVars(types, f)
//    //val res2 = transformCallSites(types, res)
//    //pyPrintFunc(res2)
//    //gen.genFunc(res2).forEach(::println)
//    //println("------------")
//  }
//
//  val bellatrixModule = TopLevelScope.resolveModule("bellatrix")
//  val btxconst_decls = bellatrixModule.constantDecls.map { it.name to it.type.toFAtom(emptyMap()) as FAtom }.toMap()
//  TypingContext.initConstants(btxconst_decls)
//  val constTypes_btx = inferConstTypes(bellatrixModule.constantDefs.map { it.name to it.value.const.e })
//  TypingContext.initConstants(constTypes_btx)
//  bellatrixModule.definitions.filterIsInstance<TLFuncDef>().forEach {
//    val f = desugarStmts(convertToAndOutOfSSA(it.func))
//    val types = inferTypes2(f, ssa = true)
//    val res = replaceTypeVars(types, f)
//    val res2 = transformCallSites(types, res)
//    //pyPrintFunc(res2)
//    //gen.genFunc(res2).forEach(::println)
//    //println("------------")
//  }
}
