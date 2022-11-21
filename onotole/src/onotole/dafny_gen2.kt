package onotole

enum class MemoryModel {
  MUT_HEAP, RO_HEAP, FRESH, VALUE
}
data class PFDescr(val memoryModel: MemoryModel, val function: Boolean, val exception: Boolean,
                   val framing: String? = null, val precondition: String? = null)
val fcFuncsDescr = mapOf(
    "get_current_epoch" to PFDescr(MemoryModel.VALUE, true, false),
    "is_valid_indexed_attestation" to PFDescr(MemoryModel.VALUE, true, false),
    "get_indexed_attestation" to PFDescr(MemoryModel.VALUE, true, false),
    "get_active_validator_indices" to PFDescr(MemoryModel.FRESH, true, false),
    "get_total_active_balance" to PFDescr(MemoryModel.VALUE, true, false),
    "is_slashable_attestation_data" to PFDescr(MemoryModel.VALUE, true, false),
    "process_slots" to PFDescr(MemoryModel.MUT_HEAP, false, false),
    "state_transition" to PFDescr(MemoryModel.MUT_HEAP, false, false),

    "compute_epoch_at_slot" to PFDescr(MemoryModel.VALUE, true, false,
        precondition = "valid_constants()"),
    "compute_start_slot_at_epoch" to PFDescr(MemoryModel.VALUE, true, false),

    "is_previous_epoch_justified" to PFDescr(MemoryModel.RO_HEAP, true, false),
    "get_forkchoice_store" to PFDescr(MemoryModel.RO_HEAP, false, true),
    "get_slots_since_genesis" to PFDescr(MemoryModel.RO_HEAP, true, false,
        framing = "store",
        precondition = "valid_time_slots(store) && valid_constants()"),
    "get_current_slot" to PFDescr(MemoryModel.RO_HEAP, true, false,
        framing = "store",
        precondition = "valid_time_slots(store) && valid_constants()"),
    "compute_slots_since_epoch_start" to PFDescr(MemoryModel.VALUE, true, false,
        precondition = "valid_constants()"),
    "get_ancestor" to PFDescr(MemoryModel.RO_HEAP, true, true,
        framing = "store, store.blocks",
        precondition = "valid_blocks(store.blocks)"),
    "get_latest_attesting_balance" to PFDescr(MemoryModel.RO_HEAP, true, true,
        framing = "store, store.blocks, store.checkpoint_states",
        precondition = "valid_constants() && valid_blocks(store.blocks)"),
    "filter_block_tree" to PFDescr(MemoryModel.MUT_HEAP, false, true),
    "get_filtered_block_tree" to PFDescr(MemoryModel.RO_HEAP, false, true),
    "get_head" to PFDescr(MemoryModel.RO_HEAP, false, true,
        framing = "store, store.blocks",
        precondition = "valid_constants() && valid_blocks(store.blocks)"),
    "should_update_justified_checkpoint" to PFDescr(MemoryModel.RO_HEAP, true, true,
        framing = "store, store.blocks",
        precondition = "valid_time_slots(store) && valid_constants() && valid_blocks(store.blocks)"),
    "update_checkpoints" to PFDescr(MemoryModel.MUT_HEAP, false, false),
    "pull_up_tip" to PFDescr(MemoryModel.MUT_HEAP, false, false),
    "on_tick_per_slot" to PFDescr(MemoryModel.MUT_HEAP, false, false),
    "validate_target_epoch_against_current_time" to PFDescr(MemoryModel.RO_HEAP, true, true,
        framing = "store",
        precondition = "valid_constants() && valid_time_slots(store)"),
    "validate_on_attestation" to PFDescr(MemoryModel.RO_HEAP, true, true,
        framing = "store, store.blocks",
        precondition = "valid_constants() && valid_time_slots(store) && valid_blocks(store.blocks)"),
    "store_target_checkpoint_state" to PFDescr(MemoryModel.MUT_HEAP, false, true),
    "update_latest_messages" to PFDescr(MemoryModel.MUT_HEAP, false, true,
        framing = "store.latest_messages"),
    "on_tick" to PFDescr(MemoryModel.MUT_HEAP, false, true,
        framing = "store",
        precondition = "time >= store.time && valid_constants() && valid_time_slots(store) && valid_blocks(store.blocks)"),
    "on_block" to PFDescr(MemoryModel.MUT_HEAP, false, true,
        framing = "store, store.blocks, store.block_states",
        precondition = "valid_constants() && valid_time_slots(store) && valid_blocks(store.blocks)"),
    "on_attestation" to PFDescr(MemoryModel.MUT_HEAP, false, true,
        framing = "store.latest_messages, store.checkpoint_states",
        precondition = "valid_constants() && valid_time_slots(store) && valid_blocks(store.blocks)"),
    "on_attester_slashing" to PFDescr(MemoryModel.MUT_HEAP, false, true,
        framing = "store.equivocating_indices")
)

fun findFuncDescr(name: String): PFDescr? {
  return if (name.startsWith("phase0.") && name.substring("phase0.".length) in fcFuncsDescr) {
    fcFuncsDescr[name.substring("phase0.".length)]
  } else null
}

fun genDafnyFunction(name: String, args: String, ret: String): List<String> {
  val name = if (name.contains(".")) {
    if (name.startsWith("phase0.")) name.substring("phase0.".length)
    else TODO()
  } else name
  val ret = if (ret == "pylib.None" || ret == "None") null else ret
  val descr = fcFuncsDescr[name] ?: fail()

  return genDafnyFunction(name, args, ret, descr.memoryModel, descr.function, descr.exception)
}


fun genDafnyFunction(name: String, args: String, ret: String?, memoryModel: MemoryModel, function: Boolean, exceptions: Boolean): List<String> {
  val procedure = ret == null
  val name = if (name.contains(".")) name.substring(name.lastIndexOf(".")+1) else name
  val outcomeRes = if (exceptions)
    listOf("ret_: " + (ret ?: "()"))
  else if (!procedure)
    listOf("ret_: $ret")
  else emptyList()
  return if (!function) {
    val returnsDafny = if (outcomeRes.isEmpty()) "" else "returns (" + outcomeRes.joinToString(", ") + ") "
    listOf("method $name($args)", returnsDafny)
  } else {
    if (outcomeRes.size != 1) fail()
    listOf("function $name($args): (${outcomeRes[0]})")
  }
}