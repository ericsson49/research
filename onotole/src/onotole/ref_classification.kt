package onotole

private fun getBaseName(e: TExpr): String? = when(e) {
  is Name -> e.id
  is Attribute -> getBaseName(e.value)
  is Subscript -> getBaseName(e.value)
  else -> null
}

fun findMutableAliases(f: FunctionDef, cfg: CFGraphImpl, pd: PurityDescriptor?): Map<String, List<Pair<String,TExpr>>> {
  val impureArgs = pd?.impureArgs ?: emptyList()
  val aliases = findAliases(f, cfg)
  val funArgs = getFunArgs(f)
  val mutableArgNames = impureArgs.map { funArgs[it].first.arg }.toSet()
  return aliases.filterKeys { it in mutableArgNames }
}

fun findAliases(f: FunctionDef, cfg: CFGraphImpl): Map<String, List<Pair<String,TExpr>>> {
  val argTypes = getFArgs(TypeResolver.topLevelTyper, f).map { it.type }
  val varTypes = inferTypes_FD.invoke(f)
  val exprTypes = TypeResolver.topLevelTyper.updated(varTypes)
  fun getType(e: TExpr): RTType = exprTypes[e].asType()
  fun isValueType(t: RTType): Boolean {
    return t == TPyNone || (t is NamedType && t.name == "Tuple") || canAssignOrCoerceTo(t, TPyInt) || canAssignOrCoerceTo(t, TPyBytes)
  }
  fun isValueType(e: TExpr): Boolean {
    val t = if (isParamCall(e)) {
      argTypes[((e as Call).args[0] as Num).n.toInt()]
    } else if (isPhiCall(e)) {
      getCommonSuperType((e as Call).args.map(::getType))
    } else if (e is PyDict) {
      // @todo fix this workaround
      TPyDict(TPyObject, TPyObject)
    } else getType(e).asType()
    return isValueType(t)
  }
  val simpleStmts = cfg.allStmts.filter { it.lval is VarLVal }
  val varDefs = simpleStmts.map { (it.lval as VarLVal).v to it.rval }.toMap()
  val res = mutableMapOf<String, TExpr>()
  fun resolve(v: String, def: TExpr): TExpr {
    if (v in res) {
      return res[v]!!
    }
    return when(def) {
      is Call -> {
        if (isParamCall(def)) {
          val resolved = mkName(v)
          res[v] = resolved
          resolved
        } else if (isPhiCall(def)) {
          TODO()
        } else {
          // should be local
          val resolved = mkName(v)
          res[v] = resolved
          resolved
        }
      }
      is Name, is Attribute, is Subscript -> {
        val baseName = getBaseName(def)!!
        val baseExpr = resolve(baseName, varDefs[baseName]!!)
        val resolved = ExprRenamer2(mapOf(baseName to baseExpr), emptyMap()).renameExpr(def)
        res[v] = resolved
        resolved
      }
      else -> TODO()
    }
  }
  simpleStmts.filter { !isValueType(it.rval) }.forEach { s ->
    resolve((s.lval as VarLVal).v, s.rval)
  }

  return res.toList().filter { mkName(it.first) != it.second }.groupBy { getBaseName(it.second)!! }
}

sealed class ExprKindLattice
object MixedExprKind: ExprKindLattice()
object ValueExprKind: ExprKindLattice()
object BadExprKind: ExprKindLattice()
object LocalExprKind: ExprKindLattice()
data class ParamAliasExprKind(val param: Int): ExprKindLattice()
object UnknownExprKind: ExprKindLattice()

fun exprKindJoin(a: ExprKindLattice, b: ExprKindLattice) = when {
  a == b -> a
  a is UnknownExprKind -> b
  b is UnknownExprKind -> a
  a is MixedExprKind || b is MixedExprKind -> MixedExprKind
  else -> MixedExprKind
}

fun inferVariableMutationKinds(f: FunctionDef, cfg: CFGraphImpl): Map<String, ExprKindLattice> {
  val simpleStmts = cfg.allStmts.filter { it.lval is VarLVal }.map { it.lval as VarLVal to it.rval }
  val varDefs = simpleStmts.map { it.first.v to it.second }.toMap()
  val funcArgs = getFunArgs(f).map { a -> a.first.arg }.mapIndexed { i, a -> a to i }.toMap()
  val argTypes = getFArgs(TypeResolver.topLevelTyper, f).map { it.type }

  val varTypes = inferTypes_FD.invoke(f)
  val exprTypes = TypeResolver.topLevelTyper.updated(varTypes)
  fun getType(e: TExpr): RTType = exprTypes[e].asType()
  fun isValueType(t: RTType): Boolean {
    return t == TPyNone || (t is NamedType && t.name == "Tuple") || canAssignOrCoerceTo(t, TPyInt) || canAssignOrCoerceTo(t, TPyBytes)
  }
  fun isValueType(e: TExpr): Boolean {
    val t = if (isParamCall(e)) {
      argTypes[((e as Call).args[0] as Num).n.toInt()]
    } else if (isPhiCall(e)) {
      getCommonSuperType((e as Call).args.map(::getType))
    } else if (e is PyDict) {
      // @todo fix this workaround
      TPyDict(TPyObject, TPyObject)
    } else getType(e).asType()
    return isValueType(t)
  }

  val knownLocalResults = setOf("list", "<Mult>", "BeaconState", "SyncAggregate", "copy", "<dict>", "attr_copy")
  val varKinds = mutableMapOf<String, ExprKindLattice>()
  do {
    var updated = false
    varDefs.forEach { (v, def) ->
      val currValue = varKinds.getOrPut(v) { UnknownExprKind }
      val newValue = if (isValueType(def)) {
        ValueExprKind
      } else if (isParamCall(def)) {
        ParamAliasExprKind(funcArgs[v]!!)
      } else if (def is Name || def is Attribute || def is Subscript) {
        val baseName = getBaseName(def)
        if (baseName != null && baseName in varDefs)
          varKinds.getOrPut(baseName) { UnknownExprKind }
        else
          TODO()
      } else if (def is Call) {
        when (def.func) {
          is Name -> if (def.func.id in knownLocalResults) LocalExprKind else BadExprKind
          is Attribute -> if (def.func.attr in knownLocalResults) LocalExprKind else BadExprKind
          is Subscript -> BadExprKind
          else -> fail("not supported")
        }
      } else if (def is PyDict) {
        LocalExprKind
      } else {
        BadExprKind
      }
      val joinedValue = exprKindJoin(currValue, newValue)
      if (currValue != joinedValue) {
        varKinds[v] = joinedValue
        updated = true
      }
    }
  } while (updated)

  return varKinds
}

fun findModifiedParams(f: FunctionDef, cfg: CFGraphImpl, pds: (String) -> Set<Int>?): Set<Int> {
  val varKinds = inferVariableMutationKinds(f, cfg)
  val duTargets = getDUTargets(cfg, pds)
  return duTargets.flatMap { getModifiedParams(varKinds, it) }.toSet()
}

fun getDUTargets(cfg: CFGraphImpl, pds: (String) -> Set<Int>?): List<TExpr> {
  val stmts = cfg.allStmts
  fun getDUTargets(s: StmtInstr): List<TExpr> = when(s.lval) {
    is FieldLVal -> listOf(s.lval.r)
    is SubscriptLVal -> listOf(s.lval.r)
    is VarLVal, EmptyLVal -> when(s.rval) {
      is Call -> when(s.rval.func) {
        is Name -> {
          val ids = pds.invoke(s.rval.func.id) ?: emptySet()
          val args = s.rval.args
          ids.map { args[it] }
        }
        is Attribute -> {
          val ids = pds.invoke("attr_" + s.rval.func.attr) ?: emptySet()
          val args = listOf(s.rval.func.value).plus(s.rval.args)
          ids.map { args[it] }
        }
        else -> emptyList()
      }
      else -> emptyList()
    }
  }
  return stmts.flatMap { getDUTargets(it) }
}

sealed class FFunc2<A,B>
data class ConstFFunc2<A,B>(val c: B): FFunc2<A,B>()
data class RealFFunc2<A,B>(val f: (A) -> B): FFunc2<A,B>()

fun getModifiedParams(varKinds: Map<String, ExprKindLattice>, e: TExpr): Set<Int> {
  val kind = varKinds[getBaseName(e)]!!
  return if (kind is ParamAliasExprKind)
    setOf(kind.param)
  else if (kind == LocalExprKind) emptySet()
  else fail()
}

fun findModifiedParams2(f: FunctionDef, cfg: CFGraphImpl): List<Pair<String?, FFunc2<Set<Int>, Set<Int>>>> {
  val varKinds = inferVariableMutationKinds(f, cfg)
  return getDUTargets2(cfg, varKinds)
}

fun getDUTargets2(cfg: CFGraphImpl, varKinds: Map<String, ExprKindLattice>): List<Pair<String?,FFunc2<Set<Int>, Set<Int>>>> {
  val stmts = cfg.allStmts
  fun getDUTargets(s: StmtInstr): List<Pair<String?,FFunc2<Set<Int>, Set<Int>>>> = when(s.lval) {
    is FieldLVal -> listOf(null to ConstFFunc2(getModifiedParams(varKinds, s.lval.r)))
    is SubscriptLVal -> listOf(null to ConstFFunc2(getModifiedParams(varKinds, s.lval.r)))
    is VarLVal, EmptyLVal -> when(s.rval) {
      is Call -> when(s.rval.func) {
        is Name -> {
          listOf(s.rval.func.id to RealFFunc2 { ids ->
            val args = s.rval.args
            ids.flatMap { getModifiedParams(varKinds, args[it]) }.toSet()
          })
        }
        is Attribute -> {
          listOf("attr_" + s.rval.func.attr to RealFFunc2 { ids ->
            val args = listOf(s.rval.func.value).plus(s.rval.args)
            ids.flatMap { getModifiedParams(varKinds, args[it]) }.toSet()
          })
        }
        else -> emptyList()
      }
      else -> emptyList()
    }
  }
  return stmts.flatMap { getDUTargets(it) }
}

fun detectPurity(funcs: List<Pair<FunctionDef,CFGraphImpl>>): Map<String,PurityDescriptor> {
  val initial = mapOf("attr_append" to PurityDescriptor(true, listOf(0), "attr_append_pure"))
  val res = initial.toMutableMap()
  do {
    var updated = false
    funcs.forEach { (f, cfg) ->
      val modParams = findModifiedParams(f, cfg) { n -> res[n]?.impureArgs?.toSet() }
      if (modParams.isNotEmpty()) {
        val pd = PurityDescriptor(f.returns == null || f.returns == NameConstant(null), modParams.sorted(), f.name + "_pure")
        if (f.name !in res || res[f.name] != pd) {
          res[f.name] = pd
          updated = true
        }
      }
    }
  } while (updated)
  return res
}

class ImpurityInterprocAnalysis(funcs: List<Pair<FunctionDef,CFGraphImpl>>): DataflowAnalysis<String,Set<Int>> {
  val libDescrs = mapOf("attr_append" to PurityDescriptor(true, listOf(0), "attr_append_pure"))
  val flowFuncs = funcs.map { it.first.name to findModifiedParams2(it.first, it.second) }.toMap()
  override val bottom = emptySet<Int>()
  override fun join(a: Set<Int>, b: Set<Int>) = a.union(b)
  override fun process(n: String, d: Set<Int>, getValue: (String) -> Set<Int>): Set<Int> {
    fun getDescriptor(id: identifier) = if (id in libDescrs) libDescrs[id]?.impureArgs?.toSet() else getValue(id)
    val depResults = flowFuncs[n]!!.map { (fn, ff) ->
      if (fn == null) {
        (ff as ConstFFunc2<Set<Int>, Set<Int>>).c
      } else {
        (ff as RealFFunc2<Set<Int>, Set<Int>>).f(getDescriptor(fn) ?: emptySet())
      }
    }
    return depResults.fold(bottom, ::join)
  }
}

class ImpurityAnalysis(funcs: List<Pair<FunctionDef,CFGraphImpl>>): StaticGraphDataFlow<String, Set<Int>> {
  override val bottom = emptySet<Int>()
  override fun join(a: Set<Int>, b: Set<Int>) = a.union(b)
  val libDescrs = mapOf("attr_append" to PurityDescriptor(true, listOf(0), "attr_append_pure"))
  val flowFuncs = funcs.map { it.first.name to findModifiedParams2(it.first, it.second) }.toMap()
  val deps = flowFuncs.mapValues { it.value.mapNotNull { it.first } }
  val localRes = flowFuncs.mapValues {
    it.value.filter { it.first == null }
        .map { (it.second as ConstFFunc2<Set<Int>, Set<Int>>).c }
        .fold(bottom, ::join)
  }

  override fun predecessors(n: String) = deps[n] ?: emptyList()

  override fun process(n: String, d: Set<Int>, env: (String) -> Set<Int>): Set<Int> {
    fun getDescriptor(id: identifier) = if (id in libDescrs) libDescrs[id]?.impureArgs?.toSet() else env(id)
    return (flowFuncs[n] ?: emptyList()).filter { it.first != null }.map {
      val fn = it.first!!
      val ff = it.second as RealFFunc2<Set<Int>, Set<Int>>
      ff.f(getDescriptor(fn) ?: emptySet())
    }.fold(localRes[n] ?: bottom, ::join)
  }
}