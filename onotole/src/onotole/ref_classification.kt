package onotole

private fun getBaseName(e: TExpr): String = when(e) {
  is Name -> e.id
  is Attribute -> getBaseName(e.value)
  is Subscript -> getBaseName(e.value)
  else -> fail()
}

fun findMutableRefs(f: FunctionDef, cfg: CFGraphImpl, pds: Map<String,PurityDescriptor>): Map<String, List<Pair<String,TExpr>>> {
  if (f.name in pds) {
    val pd = pds[f.name]!!
    val funArgs = getFunArgs(f)
    val mutableArgs = pd.impureArgs.map { funArgs[it].first.arg }.toSet()

    val varTypes = inferTypes_FD(f)
    val exprTypes = TypeResolver.topLevelTyper.updated(varTypes)
    fun getType(e: TExpr): RTType = exprTypes[e].asType()
    fun isValueType(e: TExpr): Boolean {
      val t = getType(e).asType()
      return canAssignOrCoerceTo(t, TPyInt) || canAssignOrCoerceTo(t, TPyBytes)
    }

    val stmts = cfg.blocks.flatMap { it.value.stmts }
    val aliases = mutableMapOf<String,String>()
    fun resolveMutAlias(n: String): String? {
      return if (n in mutableArgs) n else if (n in aliases) aliases[n] else null
    }
    do {
      var updated = false
      fun updateAliasing(v: String, root: String) {
        if (v in aliases) {
          if (aliases[v] != root) fail()
        } else {
          aliases[v] = root
          updated = true
        }
      }
      stmts.forEach { s ->
        if (s.lval is VarLVal) {
          when (s.rval) {
            is Name, is Attribute, is Subscript -> {
              val baseName = getBaseName(s.rval)
              val name = resolveMutAlias(baseName)
              if (name != null && !isValueType(s.rval)) {
                updateAliasing(s.lval.v, name)
              }
            }
            is Call -> if (s.rval.func is Name && s.rval.func.id == "<Phi>") {
              val baseNames = s.rval.args.flatMap { resolveMutAlias((it as Name).id)?.let { setOf(it) } ?: emptySet() }.toSet().toList()
              if (baseNames.size >= 2)
                fail()
              if (baseNames.size == 1)
                updateAliasing(s.lval.v, baseNames[0])
            }
            is NameConstant -> {}
            else ->
              TODO()
          }
        }
      }
    } while (updated)

    val simpleStmts = stmts.filter { it.lval is VarLVal }.map { it.lval as VarLVal to it.rval }
    val varDefs = simpleStmts.map { it.first.v to it.second }.toMap()
    val res = aliases.map { (v, root) ->
      fun process(e: TExpr, resolver: (String) -> TExpr): TExpr = when (e) {
        is Name -> resolver.invoke(e.id)
        is Attribute -> e.copy(value = process(e.value, resolver))
        is Subscript -> e.copy(value = process(e.value, resolver))
        else -> fail()
      }

      fun expandName(v: String): TExpr {
        if (v == root)
          return mkName(v)
        val def = varDefs[v]!!
        if (def is Call && def.func is Name && def.func.id == "<Phi>") fail()
        return process(def, ::expandName)
      }

      root to (v to expandName(v))
    }.groupBy { it.first }.mapValues { it.value.map { it.second } }
    return res
  } else {
    return emptyMap()
  }
}

fun findModifiedParams(f: FunctionDef, cfg: CFGraphImpl, pds: Map<String,PurityDescriptor>): List<Int> {
  val stmts = cfg.blocks.flatMap { it.value.stmts }
  val simpleStmts = stmts.filter { it.lval is VarLVal }.map { it.lval as VarLVal to it.rval }
  val varDefs = simpleStmts.map { it.first.v to it.second }.toMap()
  val funcArgs = getFunArgs(f).mapIndexed { i, a -> a.first.arg to i }.toMap()

  val modifiedParams = mutableSetOf<Int>()

  val duTargets = getDUTargets(cfg, pds)
  duTargets.forEach {
    var curr = it
    while(true) {
      val baseName = getBaseName(curr)
      if (baseName in funcArgs) {
        modifiedParams.add(funcArgs[baseName]!!)
        break
      }
      when(val def = varDefs[baseName]!!) {
        is Name, is Attribute, is Subscript -> {
          curr = def
        }
        is Call -> {
          if (def.func is Name) {
            if (def.func.id in setOf("list", "<Mult>", "BeaconState", "SyncAggregate", "copy", "<dict>")) {
              //"<local>"
              break
            }  else
              fail("<local> ?? " + def)
          } else if (def.func is Attribute) {
            if (def.func.attr in "copy")
              break
            else fail()
          } else
            fail()
        }
        else -> fail()
      }
    }
  }
  return modifiedParams.sorted()
}

fun getDUTargets(cfg: CFGraphImpl, pds: Map<String, PurityDescriptor>): List<TExpr> {
  val stmts = cfg.blocks.flatMap { it.value.stmts }
  fun getDUTargets(s: StmtInstr): List<TExpr> = when(s.lval) {
    is FieldLVal -> listOf(s.lval.r)
    is SubscriptLVal -> listOf(s.lval.r)
    is VarLVal, EmptyLVal -> when(s.rval) {
      is Call -> when(s.rval.func) {
        is Name -> {
          val ids = pds[s.rval.func.id]?.let { it.impureArgs } ?: emptyList()
          val args = s.rval.args
          ids.map { args[it] }
        }
        is Attribute -> {
          val ids = pds["attr_" + s.rval.func.attr]?.let { it.impureArgs } ?: emptyList()
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

fun detectPurity(funcs: List<Pair<FunctionDef,CFGraphImpl>>): Map<String,PurityDescriptor> {
  val initial = mapOf("attr_append" to PurityDescriptor(true, listOf(0), "attr_append_pure"))
  val res = initial.toMutableMap()
  do {
    var updated = false
    funcs.forEach { (f, cfg) ->
      val modParams = findModifiedParams(f, cfg, res)
      if (modParams.isNotEmpty()) {
        val pd = PurityDescriptor(f.returns == null || f.returns == NameConstant(null), modParams, f.name + "_pure")
        if (f.name !in res || res[f.name] != pd) {
          res[f.name] = pd
          updated = true
        }
      }
    }
  } while (updated)
  return res
}