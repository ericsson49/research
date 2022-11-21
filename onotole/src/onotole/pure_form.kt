package onotole

import onotole.lib_defs.Additional
import onotole.lib_defs.BLS
import onotole.lib_defs.PyLib
import onotole.lib_defs.SSZLib
import onotole.typelib.TLTClass
import java.nio.file.Files
import java.nio.file.Paths

data class PurityDescriptor(val procedure: Boolean, val impureArgs: List<Int>, val pureName: String)


class PureFormConvertor(val f: FunctionDef, val purityDescriptors: Map<String, PurityDescriptor>, val aliasInfo: Map<String, List<Pair<String,TExpr>>>) {
  constructor(f: FunctionDef, purityDescriptors: Map<String, PurityDescriptor>): this(f, purityDescriptors, emptyMap())
  val roots = reverse(aliasInfo.mapValues { it.value.map { it.first } })

  val purityDescriptor get() = purityDescriptors[f.name]!!

  fun newVar(): String = "tmp"

  fun transformLVal(lv: TExpr, rv: TExpr): List<Stmt> {
    val (baseNames, stmts) = transformLVal0(lv, rv)

    if (lv is Name) {
      return stmts
    } else {

      val rs = baseNames.flatMap { roots[it] ?: emptyList() }
      val rootUpdates = rs.flatMap { root ->
        val (b, e) = aliasInfo[root]!!.filter { it.first in baseNames }[0]
        val (_, bb) = transformLVal0(e, mkName(b))
        bb
      }
      val aliasUpdates = rs.flatMap { root ->
        aliasInfo[root]!!.filter { it.first !in baseNames }
      }.map { Assign(mkName(it.first, true), it.second) }

      return stmts.plus(rootUpdates).plus(aliasUpdates)
    }
  }
  fun transformLVal0(lv: TExpr, rv: TExpr): Pair<List<String>, List<Stmt>> {
    return when(lv) {
      is Name -> listOf(lv.id) to listOf(Assign(lv, rv))
      is Attribute -> {
        val nv = Call(Attribute(lv.value, "updated", ExprContext.Load), emptyList(), listOf(Keyword(lv.attr, rv)))
        transformLVal0(lv.value, nv)
      }
      is Subscript -> {
        when(lv.slice) {
          is Index -> {
            val idx = lv.slice.value
            val nv = Call(Attribute(lv.value, "updated_at", ExprContext.Load), listOf(idx, rv), emptyList())
            transformLVal0(lv.value, nv)
          }
          is Slice -> {
            val n = NameConstant(null)
            val l = lv.slice.lower ?: n
            val u = lv.slice.upper ?: n
            val s = lv.slice.step ?: n
            val nv = Call(Attribute(lv.value, "updated_slice", ExprContext.Load), listOf(l, u, s, rv), emptyList())
            transformLVal0(lv.value, nv)
          }
          else -> TODO()
        }
      }
      is Tuple -> {
        val newNames = lv.elts.map { if (it is Name) it.id else newVar() }
        val res = listOf(Assign(Tuple(newNames.map { Name(it, ExprContext.Store) }, ExprContext.Store), rv))
        val baseNames = mutableSetOf<String>()
        val assigns = newNames.zip(lv.elts).flatMap {
          if (it.second !is Name) {
            val (bNames, r) = transformLVal0(it.second, Name(it.first, ExprContext.Load))
            baseNames.addAll(bNames)
            r
          } else
            emptyList()
        }
       baseNames.toList() to res.plus(assigns)
      }
      else -> fail()
    }
  }

  fun getPurityDescr(f: identifier): PurityDescriptor = purityDescriptors[f] ?: PurityDescriptor(false, emptyList(), f)

  fun checkExpr(e: TExpr) = true
  fun transformExpr(e: TExpr, tgt: TExpr?): Pair<List<TExpr>, TExpr> {
    when {
      e is Call -> {
        when(e.func) {
          is Name -> {
            val purirityInfo = getPurityDescr(e.func.id)
            val args = e.args
            val tgtArgs = purirityInfo.impureArgs.map { args[it] }
            tgtArgs.forEach {
              when(it) {
                is Name -> {}
                is Subscript -> {}
                is Attribute -> {}
                else -> TODO("not supported yet")
              }
            }
            if (tgt != null && purirityInfo.procedure) fail()
            val resExpr = if (tgt == null)
              if (!purirityInfo.procedure) listOf(Name("_", ExprContext.Store)) else emptyList()
            else
              listOf(tgt)

            return tgtArgs.plus(resExpr) to e.copy(func = Name(purirityInfo.pureName, ExprContext.Load))
          }
          is Subscript -> {
            return (if (tgt == null) emptyList() else listOf(tgt)) to e
          }
          is Attribute -> {
            val purirityInfo = getPurityDescr("attr_" + e.func.attr)
            if (purirityInfo.impureArgs.isNotEmpty()) {
              val args = listOf(e.func.value).plus(e.args)
              val tgtArgs = purirityInfo.impureArgs.map { args[it] }
              if (!purirityInfo.procedure) TODO()
              val pureName = if (purirityInfo.pureName.startsWith("attr_"))
                purirityInfo.pureName.substring("attr_".length)
              else purirityInfo.pureName
              return tgtArgs to e.copy(func = e.func.copy(attr = pureName))
            } else {
              val resExpr = if (tgt == null)
                //if (!purirityInfo.procedure) listOf(Name("_", ExprContext.Store)) else emptyList()
                TODO()
              else
                listOf(tgt)
              return resExpr to e
            }
          }
          is CTV -> when(e.func.v) {
            is FuncInst -> {
              val sig = e.func.v.sig
              val purirityInfo = getPurityDescr(e.func.v.name)
              val args = e.args
              val tgtArgs = purirityInfo.impureArgs.map { args[it] }
              val tgtTypes = purirityInfo.impureArgs.map { sig.args[it].second }
              tgtArgs.forEach {
                when (it) {
                  is Name -> {}
                  is Subscript -> {}
                  is Attribute -> {}
                  else -> TODO("not supported yet")
                }
              }
              if (tgt != null && purirityInfo.procedure) fail()
              val (resExpr, resType) = if (tgt == null)
                if (!purirityInfo.procedure)
                  listOf(Name("_", ExprContext.Store)) to listOf(sig.ret)
                else
                  emptyList<TExpr>() to emptyList()
              else
                listOf(tgt) to listOf(sig.ret)

              val resOutTypes = tgtTypes.plus(resType)
              val newRet = when {
                resOutTypes.isEmpty() -> TODO()
                resOutTypes.size == 1 -> resOutTypes.first()
                else -> TLTClass("pylib.Tuple", resOutTypes)
              }
              val newSig = sig.copy(ret = newRet)
              return tgtArgs.plus(resExpr) to e.copy(func = CTV(e.func.v.copy(sig = newSig)))
            }
            else -> TODO()
          }
          else -> TODO()
        }
      }
      else -> {
        checkExpr(e)
        return (if (tgt == null) emptyList() else listOf(tgt)) to e
      }
    }
  }

  inner class PureFormStmtTransformer(): SimpleStmtTransformer() {
    override fun doTransform(s: Stmt): List<Stmt>? {
      return transformStmt(s)
    }
  }

  fun transformStmt(s: Stmt): List<Stmt>? {
    return when(s) {
      is Assign -> {
        val (tgts, expr) = transformExpr(s.value, s.target)
        when {
          tgts.isEmpty() ->
            TODO() //listOf(Expr(expr))
          tgts.size == 1 -> transformLVal(tgts[0], expr)
          else -> transformLVal(Tuple(tgts, ExprContext.Store), expr)
        }
      }
      is AugAssign -> {
        transformStmt(Assign(s.target, BinOp(s.target, s.op, s.value)))
      }
      is Expr -> {
        val (tgts, expr) = transformExpr(s.value, null)
        when {
          tgts.isEmpty() -> listOf(Expr(expr))
          tgts.size == 1 -> transformLVal(tgts[0], expr)
          else -> transformLVal(Tuple(tgts, ExprContext.Store), expr)
        }
      }
      is Return -> {
        val pd = purityDescriptor
        if (pd.impureArgs.isEmpty()) {
          null
        } else {
          val impArgs: List<TExpr> = pd.impureArgs.map {  mkName(f.args.args[it].arg) }
          if (pd.procedure && isProcedureRetVal(s.value))
            fail()
          val retVals = if (!pd.procedure) impArgs.plus(s.value!!) else impArgs
          val retVal = when {
            retVals.isEmpty() -> null
            retVals.size == 1 -> retVals[0]
            else -> Tuple(retVals, ExprContext.Load)
          }
          listOf(Return(retVal))
        }
      }
      else -> null
    }
  }

  fun isProc(f: FunctionDef) = f.returns == null || f.returns is NameConstant && f.returns._value == null
  fun transformFunction(): FunctionDef {
    if (f.name in purityDescriptors) {
      val pd = purityDescriptor
      val newName = pd.pureName
      val updatedArgs = pd.impureArgs.map { f.args.args[it].annotation!! }
      val ret = if (!isProc(f))
        listOf(f.returns!!)
      else
        emptyList()
      val retVals = updatedArgs.plus(ret)
      val newRetType = when {
        retVals.isEmpty() -> null
        retVals.size == 1 -> retVals[0]
        else -> CTV(ClassVal("pylib.Tuple", retVals.map { (it as CTV).v as ClassVal }))
      }
      val retStmt = if (pd.procedure && f.body.isNotEmpty() && f.body[f.body.size-1] !is Return)
        when {
          pd.impureArgs.isEmpty() -> null
          pd.impureArgs.size == 1 -> Return(mkName(f.args.args[pd.impureArgs[0]].arg))
          else -> Return(Tuple(pd.impureArgs.map { mkName(f.args.args[it].arg) }, ExprContext.Load))
        }
      else null
      val st = PureFormStmtTransformer()
      val newBody1 = f.body.flatMap { st.transform(it) }
      val newBody = if (retStmt != null) newBody1.plus(retStmt) else newBody1
      return f.copy(name = newName, returns = newRetType, body = newBody)
    } else {
      return f
    }
  }
}

val pure_funcs = listOf(
    "hash_tree_root",
    // phase0
    "integer_squareroot",
    "xor",
    "bytes_to_uint64",
    "is_active_validator",
    "is_eligible_for_activation_queue",
    "is_eligible_for_activation",
    "is_slashable_validator",
    "is_slashable_attestation_data",
    "is_valid_indexed_attestation",
    "is_valid_merkle_branch",
    "compute_shuffled_index",
    "compute_proposer_index",
    "compute_committee",
    "compute_epoch_at_slot",
    "compute_start_slot_at_epoch",
    "compute_activation_exit_epoch",
    "compute_fork_data_root",
    "compute_fork_digest",
    "compute_domain",
    "compute_signing_root",
    "get_current_epoch",
    "get_previous_epoch",
    "get_block_root",
    "get_block_root_at_slot",
    "get_randao_mix",
    "get_active_validator_indices",
    "get_validator_churn_limit",
    "get_seed",
    "get_committee_count_per_slot",
    "get_beacon_committee",
    "get_beacon_proposer_index",
    "get_total_balance",
    "get_total_active_balance",
    "get_domain",
    "get_indexed_attestation",
    "get_attesting_indices",
    "is_valid_genesis_state",
    "verify_block_signature",
    "get_matching_source_attestations",
    "get_matching_target_attestations",
    "get_matching_head_attestations",
    "get_unslashed_attesting_indices",
    "get_attesting_balance",
    "get_base_reward",
    "get_proposer_reward",
    "get_finality_delay",
    "is_in_inactivity_leak",
    "get_eligible_validator_indices",
    "get_attestation_component_deltas",
    "get_source_deltas",
    "get_target_deltas",
    "get_head_deltas",
    "get_inclusion_delay_deltas",
    "get_inactivity_penalty_deltas",
    "get_attestation_deltas",
    "get_validator_from_deposit",
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
    "validate_on_attestation",
    "check_if_validator_active",
    "get_committee_assignment",
    "is_proposer",
    "get_epoch_signature",
    "compute_time_at_slot",
    "voting_period_start_time",
    "is_candidate_block",
    "get_eth1_vote",
    "compute_new_state_root",
    "get_block_signature",
    "get_attestation_signature",
    "compute_subnet_for_attestation",
    "get_slot_signature",
    "is_aggregator",
    "get_aggregate_signature",
    "get_aggregate_and_proof",
    "get_aggregate_and_proof_signature",
    "compute_weak_subjectivity_period",
    "is_within_weak_subjectivity_period",
    "initialize_beacon_state_from_eth1"
)

fun phase0PDs(): Map<String,PurityDescriptor> {
  fun mkPure(fn: String) = PurityDescriptor(false, emptyList(), fn)
  val pureFuncPDs = pure_funcs.map { it to mkPure(it) }

  return mapOf(
      "increase_balance" to PurityDescriptor(true, listOf(0), "increase_balance_pure"),
      "decrease_balance" to PurityDescriptor(true, listOf(0), "decrease_balance_pure"),
      "initiate_validator_exit" to PurityDescriptor(true, listOf(0), "initiate_validator_exit_pure"),
      "slash_validator" to PurityDescriptor(true, listOf(0), "slash_validator_pure"),
      "state_transition" to PurityDescriptor(true, listOf(0), "state_transition_pure"),
      "process_slots" to PurityDescriptor(true, listOf(0), "process_slots_pure"),
      "process_slot" to PurityDescriptor(true, listOf(0), "process_slot_pure"),
      "process_epoch" to PurityDescriptor(true, listOf(0), "process_epoch_pure"),
      "process_justification_and_finalization" to PurityDescriptor(true, listOf(0), "process_justification_and_finalization_pure"),
      "process_rewards_and_penalties" to PurityDescriptor(true, listOf(0), "process_rewards_and_penalties_pure"),
      "process_registry_updates" to PurityDescriptor(true, listOf(0), "process_registry_updates_pure"),
      "process_slashings" to PurityDescriptor(true, listOf(0), "process_slashings_pure"),
      "process_eth1_data_reset" to PurityDescriptor(true, listOf(0), "process_eth1_data_reset_pure"),
      "process_effective_balance_updates" to PurityDescriptor(true, listOf(0), "process_effective_balance_updates_pure"),
      "process_slashings_reset" to PurityDescriptor(true, listOf(0), "process_slashings_reset_pure"),
      "process_randao_mixes_reset" to PurityDescriptor(true, listOf(0), "process_randao_mixes_reset_pure"),
      "process_historical_roots_update" to PurityDescriptor(true, listOf(0), "process_historical_roots_update_pure"),
      "process_participation_record_updates" to PurityDescriptor(true, listOf(0), "process_participation_record_updates_pure"),
      "process_block" to PurityDescriptor(true, listOf(0), "process_block_pure"),
      "process_block_header" to PurityDescriptor(true, listOf(0), "process_block_header_pure"),
      "process_randao" to PurityDescriptor(true, listOf(0), "process_randao_pure"),
      "process_eth1_data" to PurityDescriptor(true, listOf(0), "process_eth1_data_pure"),
      "process_operations" to PurityDescriptor(true, listOf(0), "process_operations_pure"),
      "process_proposer_slashing" to PurityDescriptor(true, listOf(0), "process_proposer_slashing_pure"),
      "process_attester_slashing" to PurityDescriptor(true, listOf(0), "process_attester_slashing_pure"),
      "process_attestation" to PurityDescriptor(true, listOf(0), "process_attestation_pure"),
      "process_deposit" to PurityDescriptor(true, listOf(0), "process_deposit_pure"),
      "process_voluntary_exit" to PurityDescriptor(true, listOf(0), "process_voluntary_exit_pure"),
      "weigh_justification_and_finalization" to PurityDescriptor(true, listOf(0), "weigh_justification_and_finalization_pure"),
      "store_target_checkpoint_state" to PurityDescriptor(true, listOf(0), "store_target_checkpoint_state_pure"),
      "update_latest_messages" to PurityDescriptor(true, listOf(0), "update_latest_messages_pure"),
      "on_tick" to PurityDescriptor(true, listOf(0), "on_tick_pure"),
      "on_block" to PurityDescriptor(true, listOf(0), "on_block_pure"),
      "on_attestation" to PurityDescriptor(true, listOf(0), "on_attestation_pure"),
      "on_attester_slashing" to PurityDescriptor(true, listOf(0), "on_attester_slashing_pure"),
      "filter_block_tree" to PurityDescriptor(false, listOf(2), "filter_block_tree_pure"),


      "attr_append" to PurityDescriptor(true, listOf(0), "attr_append_pure")
  )//.plus(pureFuncPDs)
}

fun renameVarsInCFG(cfg: CFGraphImpl, renamer: ExprRenamer2): CFGraphImpl {
  val blocks = cfg.blocks.mapValues { (l, b) ->
    val newStmts = b.stmts.map {
      val lval = when(it.lval) {
        is EmptyLVal -> it.lval
        is VarLVal -> VarLVal(renamer.renameName_Store(it.lval.v), it.lval.t)
        is FieldLVal -> FieldLVal(renamer.renameExpr(it.lval.r), it.lval.f)
        is SubscriptLVal -> SubscriptLVal(renamer.renameExpr(it.lval.r), renamer.renameSlice(it.lval.i))
      }
      StmtInstr(lval, renamer.renameExpr(it.rval))
    }
    val newBranch = b.branch.discrVar?.let { Branch((renamer.renameName(it, ExprContext.Load) as Name).id, b.branch.next) } ?: b.branch
    BasicBlock(newStmts, newBranch)
  }
  return CFGraphImpl(blocks.toList(), cfg.loops, cfg.ifs)
}

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




  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_phase0_dev.txt")
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  val defs = parsed.map { toStmt(it) }

  val fDefs = defs.filterIsInstance<FunctionDef>()
  val descrs = phase0PDs()

  for(fd in fDefs) {
    if (fd.name in pure_funcs)
      continue
    println()
    val fd = transformForOps(fd)
    pyPrintFunc(fd)
    println()
    val tf = purify2(fd, descrs, TypeResolver.topLevelTyper)
    pyPrintFunc(tf)
    println()
  }
}

fun purify1(f: FunctionDef, descrs: Map<String, PurityDescriptor>, typer: ExprTyper): FunctionDef {
  val fd2 = desugarExprs(transformForEnumerate(f))
  val cfg = convertToCFG(fd2)
  val analyses = getFuncAnalyses(cfg)
  val ssa = convertToSSA(cfg)
  val mutRefs = findMutableAliases(fd2, ssa, descrs[fd2.name], typer)
  val (cfg2, renamer) = destructSSA(ssa, analyses.cfgAnalyses.dom)
  val fd3 = reconstructFuncDef(fd2, cfg2)
  val mutRefs2 = mutRefs.mapValues { it.value.map { it.copy(second = renamer.renameExpr(it.second)) } }
  if (mutRefs != mutRefs2) TODO()
  return PureFormConvertor(fd3, descrs, mutRefs).transformFunction()
}
fun purify2(f: FunctionDef, descrs: Map<String, PurityDescriptor>, typer: ExprTyper): FunctionDef {
  val fd2 = desugarExprs(transformForEnumerate(f))
  val cfg = convertToCFG(fd2)
  val analyses = getFuncAnalyses(cfg)
  val ssa = convertToSSA(cfg)
  val aliases = findMutableAliases(fd2, ssa, descrs[fd2.name], typer)
  val (cfg2, renamer) = destructSSA(ssa, analyses.cfgAnalyses.dom)
  val mutRefs2 = aliases.mapValues { it.value.map { it.copy(second = renamer.renameExpr(it.second)) } }
  if (aliases != mutRefs2) TODO()
  val inlines = aliases.flatMap { it.value }.toMap()
  // check inlines
  fun checkAccessPath(e: TExpr): Boolean = when(e) {
    is Name -> true
    is Attribute -> checkAccessPath(e.value)
    is Subscript -> {
      checkAccessPath(e.value) && when(val s = e.slice) {
        is Index -> isNameOrConst(s.value)
        is Slice -> listOf(s.lower, s.upper, s.step).all(::isNameOrConst)
        else -> TODO()
      }
    }
    else -> TODO()
  }
  if (!inlines.values.all(::checkAccessPath))
    println()


  val cfg3 = renameVarsInCFG(cfg2, ExprRenamer2(inlines, emptyMap()))
  // drop alias definitions
  val blocks = cfg3.blocks.mapValues {
    val newStmts = it.value.stmts.filterNot { s -> s.lval is VarLVal && s.lval.v in inlines }
    BasicBlock(newStmts, it.value.branch)
  }
  val updatedBlockLabels = cfg3.blocks.keys.filter { cfg3.blocks[it]!!.stmts.size != blocks[it]!!.stmts.size }
  val newIfs = cfg3.ifs.map {
    if (it.head.first in updatedBlockLabels) {
      val ifStmt = cfg3.blocks[it.head.first]!!.stmts[it.head.second]
      val newIfIdx = blocks[it.head.first]!!.stmts.indexOf(ifStmt)
      if (newIfIdx == -1) fail()
      IfInfo2(it.head.first to newIfIdx, it.test, it.body, it.orelse, it.exit)
    } else it
  }
  val cfg4 = CFGraphImpl(blocks.toList(), cfg3.loops, newIfs)
  val fd4 = reconstructFuncDef(fd2, cfg4)
  return PureFormConvertor(fd4, descrs).transformFunction()
}