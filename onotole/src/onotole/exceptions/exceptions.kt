package onotole.exceptions

import onotole.Assert
import onotole.Attribute
import onotole.BinOp
import onotole.BoolOp
import onotole.CTV
import onotole.Call
import onotole.ClassVal
import onotole.Compare
import onotole.Constant
import onotole.EBinOp
import onotole.EBoolOp
import onotole.ECmpOp
import onotole.EUnaryOp
import onotole.ExprContext
import onotole.ExprTyper
import onotole.FuncInst
import onotole.FunctionDef
import onotole.GeneratorExp
import onotole.IfExp
import onotole.Index
import onotole.Lambda
import onotole.Let
import onotole.Name
import onotole.NamedType
import onotole.Num
import onotole.PyDict
import onotole.PyList
import onotole.PySet
import onotole.Slice
import onotole.Sort
import onotole.Stmt
import onotole.Subscript
import onotole.TExpr
import onotole.Tuple
import onotole.TypeResolver
import onotole.UnaryOp
import onotole.fail
import onotole.findLambdaArgument
import onotole.parseType
import onotole.type_inference.extractAnnoTypes
import onotole.util.deconstruct
import onotole.util.toTExpr

interface ExcnChecker {
  fun canThrowExcnSimple(e: TExpr, typer: ExprTyper): Boolean
  fun canThrowExcn(e: TExpr, typer: ExprTyper, recursive: Boolean = false): Boolean {
    return if (!recursive)
      canThrowExcnSimple(e, typer)
    else {
      val (subExprs, _) = deconstruct(e)
      subExprs.any { canThrowExcn(it, typer, true) } || canThrowExcnSimple(e, typer)
    }
  }
  fun updated(funcs: Collection<Pair<String, Boolean>>): ExcnChecker
}
class SimpleExcnChecker(val knownFuncs: Map<String,Boolean>): ExcnChecker {
  val noExcFuncs = setOf(
      "pylib.copy", "pylib.dict", "pylib.list", "pylib.set", "pylib.sum", "pylib.len", "pylib.any", "pylib.all",
      "pylib.iter", "pylib.has_next", "pylib.next",
      "ssz.hash_tree_root",
  )

  override fun updated(funcs: Collection<Pair<String, Boolean>>): ExcnChecker {
    val res = knownFuncs.toMutableMap()
    for((f,v) in funcs) {
      if (f !in res) {
        res[f] = v
      } else {
        res[f] = res[f]!! || v
      }
    }
    return SimpleExcnChecker(res)
  }

  override fun canThrowExcnSimple(e: TExpr, typer: ExprTyper): Boolean = when(e) {
    is Constant, is Name, is CTV, is Attribute, is PyDict, is PyList, is PySet -> false
    is BoolOp, is Compare -> false
    is BinOp -> canThrowExcn(e)
    is UnaryOp -> canThrowExcn(e)
    is Subscript -> canThrowExcn(e, typer)
    is Call -> canThrowExcn(e, typer)
    is IfExp -> canThrowExcn(e.test, typer) || canThrowExcn(e.body, typer) || canThrowExcn(e.orelse, typer)
    is GeneratorExp -> {
      if (e.generators.size != 1) TODO()
      val g = e.generators[0]
      val iterExcn = canThrowExcn(g.iter, typer)
      val tps = extractAnnoTypes(g, typer)
      val newTyper = typer.updated(tps)
      val ifsExcn = g.ifs.any {canThrowExcn(it, newTyper) }
      val mapExcn = canThrowExcn(g.iter, newTyper)
      iterExcn || ifsExcn || mapExcn
    }
    is Tuple -> e.elts.any { canThrowExcn(it, typer) }
    is Lambda -> false // lambda def doesn't throw an exception by itself
    is Let -> {
      val bindingsExcn = e.bindings.any { canThrowExcn(it.value, typer) }
      val newTyper = typer.forLet(e)
      bindingsExcn || canThrowExcn(e.value, newTyper)
    }
    else -> TODO()
  }
  fun canThrowExcn(e: Call, typer: ExprTyper): Boolean {
    return when(e.func) {
      is Name -> {
        if (e.func.id in TypeResolver.specialFuncNames) {
          if (e.keywords.isNotEmpty()) fail()
          false
//        when (val op = e.func.id.substring(1, e.func.id.length-1)) {
//          in TypeResolver.binOps -> {
//            if (e.args.size != 2) fail()
//            canThrowExcn(BinOp(e.args[0], EBinOp.valueOf(op), e.args[1]))
//          }
//          in TypeResolver.cmpOps, in TypeResolver.binOps -> false
//          in TypeResolver.unaryOp -> {
//            if (e.args.size != 1) fail()
//            canThrowExcn(UnaryOp(EUnaryOp.valueOf(op), e.args[0]))
//          }
//          else -> TODO()
//        }
        } else TODO()
      }
      is Attribute -> false
      is CTV -> when(e.func.v) {
        is ClassVal -> false //canThrowExcn(e.func.v)
        is FuncInst -> when {
          e.func.v.name == "<assert>" -> true
          e.func.v.name == "<check>" -> true
          e.func.v.name.endsWith("::new") -> false
          e.func.v.name in noExcFuncs -> false
          e.func.v.name in knownFuncs -> knownFuncs[e.func.v.name]!!
          findLambdaArgument(e.func.v.sig) != null -> {
            val lamArgNo = findLambdaArgument(e.func.v.sig)!!
            val lam = e.args[lamArgNo] as Lambda
            val newCtx = typer.forLambda(lam)
            canThrowExcn(lam.body, newCtx)
          }
          e.func.v.name == "pylib.max_f" -> TODO()
          else -> TODO()
        }
        else -> TODO()
      }
      else -> TODO()
    }
  }
  fun canThrowExcn(e: ClassVal): Boolean = TODO()
  fun canThrowExcn(e: BinOp): Boolean = when(e.op) {
    EBinOp.BitAnd, EBinOp.BitOr, EBinOp.BitXor -> false
    EBinOp.MatMult, EBinOp.Div -> fail("not supported")
    EBinOp.LShift, EBinOp.RShift -> TODO()
    EBinOp.Add -> {
      TODO()
    }
    EBinOp.FloorDiv -> when {
      e.right is Num -> e.right.n.toInt() != 0
      else -> TODO()
    }
    else -> TODO()
  }
  fun canThrowExcn(e: UnaryOp): Boolean = TODO()
  fun canThrowExcn(e: Subscript, typer: ExprTyper): Boolean = canThrowException(typer, e)
}

private val fixedSizeCollections: Map<String,Int?> = mapOf(
    "ssz.Bitvector" to null,
    "ssz.Bytes32" to 32,
    "phase0.Root" to 32
)
fun isFixedSizeCollection(t: Sort): Boolean {
  return t is NamedType && t.name in fixedSizeCollections
}
fun extractFixedSize(t: Sort): Int? {
  return fixedSizeCollections[(t as NamedType).name]
}

fun canThrowException(typer: ExprTyper, e: Subscript): Boolean {
  val vt = typer[e.value]
  val fixedSizeCol = isFixedSizeCollection(vt)
  val fixedSize = if (fixedSizeCol) extractFixedSize(vt) else null
  when(e.slice) {
    is Index -> {
      if (e.slice.value is Num) {
        val const = e.slice.value.n.toInt()
        if (fixedSizeCol) {
          if (fixedSize != null && const >= fixedSize) fail()
        } else {
          TODO()
        }
      } else {
        if (e.ctx == ExprContext.Store && vt is NamedType && (vt.name == "Dict" || vt.name == "pylib.Dict")) {
          false
        } else
          return true
      }
    }
    is Slice -> {
      if (fixedSizeCol) {
        val lower = e.slice.lower ?: Num(0)
        val upper = e.slice.upper ?: fixedSize?.let { Num(it) }
        if (lower !is Num || upper != null && upper !is Num) {
          return true
        } else {
          if (lower.n.toInt() < 0) fail()
          if (upper != null && (upper as Num).n.toInt() < 0) fail()
          if (fixedSize != null) {
            if (lower.n.toInt() >= fixedSize) fail()
            if (upper != null && (upper as Num).n.toInt() >= fixedSize) fail()
          }
        }
      } else {
        return true
      }
    }
    else -> TODO()
  }
  return false
}
class ExceptionAnalysis(val phase: String, val funcs: Collection<FunctionDef>, val excnChecker: ExcnChecker, val globalTyper: ExprTyper) {
  val bottom = false
  fun join(a: Boolean, b: Boolean) = a || b

  fun solve(): Map<String, Boolean> {
    val initial = funcs.associate { it.name to false }
    var curr: Map<String, Boolean> = initial
    do {
      var prev = curr
      val checker = excnChecker.updated(curr.toList())
      var res = funcs.filter { !curr[it.name]!! }.associate {
        val typer = globalTyper.updated(it.args.args.map { it.arg to parseType(globalTyper, it.annotation!!) })
        var excn = false
        visitExprs(it.body, typer, checker) { canThrow ->
          if (canThrow)
            excn = true
        }
        it.name to excn
      }
      curr = prev.plus(res)
    } while (curr != prev)
    return  curr
  }
  fun visitExprs(ls: List<Stmt>, exprTyper: ExprTyper, checker: ExcnChecker, action: (Boolean) -> Unit) {
    object : TypedExprVisitor() {
      override fun procStmt(s: Stmt, ctx: ExprTyper): ExprTyper {
        if (s is Assert) {
          action(true)
        }
        return super.procStmt(s, ctx)
      }
      override fun visitExpr(e: TExpr, ctx: ExprTyper) {
        action(checker.canThrowExcn(e, ctx, recursive = true))
      }
    }.procStmts(ls, exprTyper)
  }

}

//fun main() {
//  PyLib.init()
//  SSZLib.init()
//  BLS.init()
//  val specVersion = "phase0"
//  Additional.init(specVersion)
//  val tlDefs = loadSpecDefs(specVersion)
//  PhaseInfo.getPkgDeps(specVersion).forEach {
//    TypeResolver.importFromPackage(it)
//  }
//  TypeResolver.importFromPackage(specVersion)
//
//  val phase = specVersion
//  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_${phase}_dev.txt")
//  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
//  val defs = parsed.map { toStmt(it) }
//
//  val fDefs = defs.filterIsInstance<FunctionDef>()
//  fDefs.forEach { registerFuncInfo(phase, it) }
//
//  val funcs = fDefs.map { fd ->
//    val fi = getFuncDefInfo(phase, fd.name)
//    fi.fd
//  }
//
//  val funcNames = funcs.map { it.name }
//  val t1 = System.nanoTime()
//  val analysis = ExceptionAnalysis(phase, funcs, pylib_exc)
//  funcNames.forEach(analysis::getInitial)
//  val t2 = System.nanoTime()
//  val r = analysis.solve(funcNames).filter { it.key in funcNames }
//  val t3 = System.nanoTime()
//  r.forEach { n, f ->
//    if (f)
//      println("\"" + n + "\" to " + analysis.getInitial(n) + ",")
//  }
//  println(" " + (t2-t1)/1000000 + " " + (t3-t2)/1000000)
//}

val pylib_exc = mapOf(
    "<assert>" to true,
).plus(EBinOp.values().map { "<$it>" to true})
    .plus(EBoolOp.values().map { "<$it>" to false })
    .plus(ECmpOp.values().map { "<$it>" to false })
    .plus(EUnaryOp.values().map { "<$it>" to false })

val phase0_exc = mapOf(
    "integer_squareroot" to true,
    "is_valid_indexed_attestation" to true,
    "get_domain" to false,
    "is_valid_merkle_branch" to true,
    "compute_shuffled_index" to true,
    "compute_proposer_index" to true,
    "compute_committee" to true,
    "compute_epoch_at_slot" to true,
    "get_current_epoch" to false,
    "get_previous_epoch" to false,
    "get_block_root" to false,
    "get_block_root_at_slot" to true,
    "get_randao_mix" to true,
    "get_validator_churn_limit" to true,
    "get_seed" to false,
    "get_committee_count_per_slot" to true,
    "get_beacon_committee" to false,
    "get_beacon_proposer_index" to false,
    "get_total_balance" to true,
    "get_total_active_balance" to false,
    "get_indexed_attestation" to false,
    "get_attesting_indices" to true,
    "increase_balance" to true,
    "decrease_balance" to true,
    "initiate_validator_exit" to true,
    "slash_validator" to true,
    "initialize_beacon_state_from_eth1" to true,
    "process_deposit" to true,
    "state_transition" to true,
    "process_slots" to true,
    "verify_block_signature" to true,
    "process_block" to false,
    "process_slot" to true,
    "process_epoch" to false,
    "process_justification_and_finalization" to false,
    "process_rewards_and_penalties" to true,
    "process_registry_updates" to true,
    "process_slashings" to true,
    "process_eth1_data_reset" to false,
    "process_effective_balance_updates" to true,
    "process_slashings_reset" to true,
    "process_randao_mixes_reset" to true,
    "process_historical_roots_update" to true,
    "get_matching_source_attestations" to true,
    "get_matching_target_attestations" to false,
    "get_matching_head_attestations" to false,
    "get_unslashed_attesting_indices" to true,
    "get_attesting_balance" to false,
    "weigh_justification_and_finalization" to true,
    "get_base_reward" to true,
    "get_proposer_reward" to true,
    "get_finality_delay" to false,
    "is_in_inactivity_leak" to false,
    "get_eligible_validator_indices" to false,
    "get_attestation_component_deltas" to true,
    "get_source_deltas" to false,
    "get_target_deltas" to false,
    "get_head_deltas" to false,
    "get_inclusion_delay_deltas" to true,
    "get_inactivity_penalty_deltas" to true,
    "get_attestation_deltas" to true,
    "process_block_header" to true,
    "process_randao" to true,
    "process_operations" to true,
    "process_proposer_slashing" to true,
    "process_attester_slashing" to true,
    "process_attestation" to true,
    "process_voluntary_exit" to true,
    "get_forkchoice_store" to true,
    "get_slots_since_genesis" to true,
    "get_current_slot" to false,
    "compute_slots_since_epoch_start" to false,
    "get_ancestor" to true,
    "get_latest_attesting_balance" to true,
    "filter_block_tree" to true,
    "get_filtered_block_tree" to false,
    "get_head" to true,
    "should_update_justified_checkpoint" to false,
    "validate_on_attestation" to true,
    "store_target_checkpoint_state" to true,
    "update_latest_messages" to true,
    "on_tick" to false,
    "on_block" to true,
    "on_attestation" to true,
    "check_if_validator_active" to true,
    "get_committee_assignment" to true,
    "is_proposer" to false,
    "get_epoch_signature" to false,
    "compute_new_state_root" to false,
    "get_block_signature" to false,
    "get_attestation_signature" to false,
    "get_slot_signature" to false,
    "is_aggregator" to true,
    "get_aggregate_and_proof" to false,
    "get_aggregate_and_proof_signature" to false,
    "compute_weak_subjectivity_period" to true,
    "is_within_weak_subjectivity_period" to true,
)
