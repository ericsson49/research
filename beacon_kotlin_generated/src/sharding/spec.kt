package sharding

import deps.*
import phase0.*
import merge.*
import merge.BeaconBlock
import pylib.*
import ssz.*

fun next_power_of_two(x: pyint): pyint {
  return pyint(2uL.pow(uint64((x - 1uL).bit_length())))
}

fun compute_previous_slot(slot: Slot): Slot {
  if (slot > 0uL) {
    return Slot(slot - 1uL)
  } else {
    return Slot(0uL)
  }
}

fun compute_updated_gasprice(prev_gasprice: Gwei, shard_block_length: uint64, adjustment_quotient: uint64): Gwei {
  if (shard_block_length > TARGET_SAMPLES_PER_BLOCK) {
    val delta = max(1uL, prev_gasprice * (shard_block_length - TARGET_SAMPLES_PER_BLOCK) / TARGET_SAMPLES_PER_BLOCK / adjustment_quotient)
    return min(prev_gasprice + delta, MAX_GASPRICE)
  } else {
    val delta_1 = max(1uL, prev_gasprice * (TARGET_SAMPLES_PER_BLOCK - shard_block_length) / TARGET_SAMPLES_PER_BLOCK / adjustment_quotient)
    return max(prev_gasprice, MIN_GASPRICE + delta_1) - delta_1
  }
}

/*
    Return the source epoch for computing the committee.
    */
fun compute_committee_source_epoch(epoch: Epoch, period: uint64): Epoch {
  val source_epoch = Epoch(epoch - (epoch % period))
  val source_epoch_2: Epoch
  if (source_epoch >= period) {
    val source_epoch_1 = source_epoch - period
    source_epoch_2 = source_epoch_1
  } else {
    source_epoch_2 = source_epoch
  }
  return source_epoch_2
}

/*
    Return the number of committees in each slot for the given ``epoch``.
    */
fun get_committee_count_per_slot(state: BeaconState, epoch: Epoch): uint64 {
  return max(uint64(1uL), min(get_active_shard_count(state, epoch), uint64(len(get_active_validator_indices(state, epoch))) / SLOTS_PER_EPOCH / TARGET_COMMITTEE_SIZE))
}

/*
    Return the number of active shards.
    Note that this puts an upper bound on the number of committees per slot.
    */
fun get_active_shard_count(state: BeaconState, epoch: Epoch): uint64 {
  return INITIAL_ACTIVE_SHARDS
}

/*
    Return the shard committee of the given ``epoch`` of the given ``shard``.
    */
fun get_shard_committee(beacon_state: BeaconState, epoch: Epoch, shard: Shard): Sequence<ValidatorIndex> {
  val source_epoch = compute_committee_source_epoch(epoch, SHARD_COMMITTEE_PERIOD)
  val active_validator_indices = get_active_validator_indices(beacon_state, source_epoch)
  val seed = get_seed(beacon_state, source_epoch, DOMAIN_SHARD_COMMITTEE)
  return compute_committee(indices = active_validator_indices, seed = seed, index = shard, count = get_active_shard_count(beacon_state, epoch))
}

/*
    Return from ``indices`` a random index sampled by effective balance.
    */
fun compute_proposer_index(beacon_state: BeaconState, indices: Sequence<ValidatorIndex>, seed: Bytes32, min_effective_balance: Gwei = Gwei(0uL)): ValidatorIndex {
  assert(len(indices) > 0uL)
  val MAX_RANDOM_BYTE = 2uL.pow(8uL) - 1uL
  val i = uint64(0uL)
  val total = uint64(len(indices))
  var i_2 = i
  while (true) {
    val candidate_index = indices[compute_shuffled_index(i_2 % total, total, seed)]
    val random_byte = hash(seed + uint_to_bytes(uint64(i_2 / 32uL)))[i_2 % 32uL]
    val effective_balance = beacon_state.validators[candidate_index].effective_balance
    if (effective_balance <= min_effective_balance) {
      i_2 = i_2
      continue
    }
    if ((effective_balance * MAX_RANDOM_BYTE) >= (MAX_EFFECTIVE_BALANCE * uint8(random_byte))) {
      return candidate_index
    }
    val i_1 = i_2 + 1uL
    i_2 = i_1
  }
}

/*
    Return the proposer's index of shard block at ``slot``.
    */
fun get_shard_proposer_index(beacon_state: BeaconState, slot: Slot, shard: Shard): ValidatorIndex {
  val epoch = compute_epoch_at_slot(slot)
  val committee = get_shard_committee(beacon_state, epoch, shard)
  val seed = hash(get_seed(beacon_state, epoch, DOMAIN_SHARD_PROPOSER) + uint_to_bytes(slot))
  val EFFECTIVE_BALANCE_MAX_DOWNWARD_DEVIATION = EFFECTIVE_BALANCE_INCREMENT - (EFFECTIVE_BALANCE_INCREMENT * HYSTERESIS_DOWNWARD_MULTIPLIER / HYSTERESIS_QUOTIENT)
  val min_effective_balance = (beacon_state.shard_gasprice * MAX_SAMPLES_PER_BLOCK / TARGET_SAMPLES_PER_BLOCK) + EFFECTIVE_BALANCE_MAX_DOWNWARD_DEVIATION
  return compute_proposer_index(beacon_state, committee, seed, min_effective_balance)
}

/*
    Return the start shard at ``slot``.
    */
fun get_start_shard(state: BeaconState, slot: Slot): Shard {
  val current_epoch_start_slot = compute_start_slot_at_epoch(get_current_epoch(state))
  val shard = state.current_epoch_start_shard
  val shard_3: Shard
  if (slot > current_epoch_start_slot) {
    var shard_5 = shard
    for (_slot in range(current_epoch_start_slot, slot)) {
      val committee_count = get_committee_count_per_slot(state, compute_epoch_at_slot(Slot(_slot)))
      val active_shard_count = get_active_shard_count(state, compute_epoch_at_slot(Slot(_slot)))
      val shard_1 = (shard_5 + committee_count) % active_shard_count
      shard_5 = shard_1
    }
    shard_3 = shard_5
  } else {
    if (slot < current_epoch_start_slot) {
      var shard_4 = shard
      for (_slot_1 in list(range(slot, current_epoch_start_slot)).reversed()) {
        val committee_count_1 = get_committee_count_per_slot(state, compute_epoch_at_slot(Slot(_slot_1)))
        val active_shard_count_1 = get_active_shard_count(state, compute_epoch_at_slot(Slot(_slot_1)))
        val shard_2 = (shard_4 + active_shard_count_1 - committee_count_1) % active_shard_count_1
        shard_4 = shard_2
      }
      shard_3 = shard_4
    } else {
      shard_3 = shard
    }
  }
  return Shard(shard_3)
}

fun compute_shard_from_committee_index(state: BeaconState, slot: Slot, index: CommitteeIndex): Shard {
  val active_shards = get_active_shard_count(state, compute_epoch_at_slot(slot))
  assert(index < active_shards)
  return Shard((index + get_start_shard(state, slot)) % active_shards)
}

fun compute_committee_index_from_shard(state: BeaconState, slot: Slot, shard: Shard): CommitteeIndex {
  val epoch = compute_epoch_at_slot(slot)
  val active_shards = get_active_shard_count(state, epoch)
  val index = CommitteeIndex((active_shards + shard - get_start_shard(state, slot)) % active_shards)
  assert(index < get_committee_count_per_slot(state, epoch))
  return index
}

fun process_block(state: BeaconState, block: BeaconBlock): Unit {
  process_block_header(state, block)
  process_randao(state, block.body)
  process_eth1_data(state, block.body)
  process_operations(state, block.body)
  if (is_execution_enabled(state, block.body)) {
    process_execution_payload(state, block.body.execution_payload, EXECUTION_ENGINE)
  }
}

fun process_operations(state: BeaconState, body: BeaconBlockBody): Unit {
  assert(len(body.deposits) == min(MAX_DEPOSITS, state.eth1_data.deposit_count - state.eth1_deposit_index))
  for (operation in body.proposer_slashings) {
    process_proposer_slashing(state, operation)
  }
  for (operation_1 in body.attester_slashings) {
    process_attester_slashing(state, operation_1)
  }
  for (operation_2 in body.shard_proposer_slashings) {
    process_shard_proposer_slashing(state, operation_2)
  }
  assert(len(body.shard_headers) <= (MAX_SHARD_HEADERS_PER_SHARD * get_active_shard_count(state, get_current_epoch(state))))
  for (operation_3 in body.shard_headers) {
    process_shard_header(state, operation_3)
  }
  for (operation_4 in body.attestations) {
    process_attestation(state, operation_4)
  }
  for (operation_5 in body.deposits) {
    process_deposit(state, operation_5)
  }
  for (operation_6 in body.voluntary_exits) {
    process_voluntary_exit(state, operation_6)
  }
}

fun process_attestation(state: BeaconState, attestation: Attestation): Unit {
  phase0.process_attestation(state, attestation)
  update_pending_shard_work(state, attestation)
}

fun update_pending_shard_work(state: BeaconState, attestation: Attestation): Unit {
  val attestation_shard = compute_shard_from_committee_index(state, attestation.data.slot, attestation.data.index)
  val buffer_index = attestation.data.slot % SHARD_STATE_MEMORY_SLOTS
  val committee_work = state.shard_buffer[buffer_index][attestation_shard]
  if (committee_work.status.selector != SHARD_WORK_PENDING) {
    return
  }
  val current_headers: Sequence<PendingShardHeader> = (committee_work.status as ShardWorkStatus_2).value
  val attestation_data: AttestationData = attestation.data as AttestationData
  val header_index = list(current_headers.map { header -> header.root }).index(attestation_data.shard_header_root)
  val pending_header: PendingShardHeader = current_headers[header_index]
  val full_committee = get_beacon_committee(state, attestation.data.slot, attestation.data.index)
  if ((pending_header.weight != 0uL) && (compute_epoch_at_slot(pending_header.update_slot) < get_current_epoch(state))) {
    pending_header.weight = sum(zip(full_committee, pending_header.votes).filter { (index, bit) -> bit }.map { (index, bit) -> state.validators[index].effective_balance })
  }
  pending_header.update_slot = state.slot
  val full_committee_balance = Gwei(0uL)
  var full_committee_balance_2 = full_committee_balance
  for ((i, bit) in enumerate(attestation.aggregation_bits)) {
    val weight = state.validators[full_committee[i]].effective_balance
    val full_committee_balance_1 = full_committee_balance_2 + weight
    if (pybool(bit)) {
      if (!pending_header.votes[i]) {
        pending_header.weight = pending_header.weight + weight
        pending_header.votes[i] = true
        full_committee_balance_2 = full_committee_balance_1
      } else {
        full_committee_balance_2 = full_committee_balance_1
      }
    } else {
      full_committee_balance_2 = full_committee_balance_1
    }
  }
  if ((pending_header.weight * 3uL) >= (full_committee_balance_2 * 2uL)) {
    if (pending_header.commitment == DataCommitment()) {
      state.shard_buffer[buffer_index][attestation_shard].status = ShardWorkStatus_0()
    } else {
      state.shard_buffer[buffer_index][attestation_shard].status = ShardWorkStatus_1(pending_header.commitment)
    }
  }
}

fun process_shard_header(state: BeaconState, signed_header: SignedShardBlobHeader): Unit {
  val header = signed_header.message
  assert((Slot(0uL) < header.slot) && (header.slot <= state.slot))
  val header_epoch = compute_epoch_at_slot(header.slot)
  assert(header_epoch in PyList(get_previous_epoch(state), get_current_epoch(state)))
  assert(header.shard < get_active_shard_count(state, header_epoch))
  assert(header.body_summary.beacon_block_root == get_block_root_at_slot(state, header.slot - 1uL))
  val committee_work = state.shard_buffer[header.slot % SHARD_STATE_MEMORY_SLOTS][header.shard]
  assert(committee_work.status.selector == SHARD_WORK_PENDING)
  val pending_headers_status: ShardWorkStatus_2 = committee_work.status as ShardWorkStatus_2
  val current_headers: SSZList<PendingShardHeader> = pending_headers_status.value
  val header_root = hash_tree_root(header)
  assert(header_root !in list(current_headers.map { pending_header -> pending_header.root }))
  assert(header.proposer_index == get_shard_proposer_index(state, header.slot, header.shard))
  val signing_root = compute_signing_root(header, get_domain(state, DOMAIN_SHARD_PROPOSER))
  assert(bls.Verify(state.validators[header.proposer_index].pubkey, signing_root, signed_header.signature))
  val body_summary = header.body_summary
  if (body_summary.commitment.length == 0uL) {
    assert(body_summary.degree_proof == G1_SETUP[0uL])
  }
  assert(bls.Pairing(body_summary.degree_proof, G2_SETUP[0uL]) == bls.Pairing(body_summary.commitment.point, G2_SETUP[-body_summary.commitment.length]))
  val index = compute_committee_index_from_shard(state, header.slot, header.shard)
  val committee_length = len(get_beacon_committee(state, header.slot, index))
  val initial_votes = SSZBitlist(PyList(0uL) * committee_length)
  val pending_header = PendingShardHeader(commitment = body_summary.commitment, root = header_root, votes = initial_votes, weight = 0uL, update_slot = state.slot)
  current_headers.append(pending_header)
}

fun process_shard_proposer_slashing(state: BeaconState, proposer_slashing: ShardProposerSlashing): Unit {
  val reference_1 = proposer_slashing.signed_reference_1.message
  val reference_2 = proposer_slashing.signed_reference_2.message
  assert(reference_1.slot == reference_2.slot)
  assert(reference_1.shard == reference_2.shard)
  assert(reference_1.proposer_index == reference_2.proposer_index)
  assert(reference_1 != reference_2)
  val proposer = state.validators[reference_1.proposer_index]
  assert(is_slashable_validator(proposer, get_current_epoch(state)))
  for (signed_header in listOf(proposer_slashing.signed_reference_1, proposer_slashing.signed_reference_2)) {
    val domain = get_domain(state, DOMAIN_SHARD_PROPOSER, compute_epoch_at_slot(signed_header.message.slot))
    val signing_root = compute_signing_root(signed_header.message, domain)
    assert(bls.Verify(proposer.pubkey, signing_root, signed_header.signature))
  }
  slash_validator(state, reference_1.proposer_index)
}

fun process_epoch(state: BeaconState): Unit {
  process_pending_shard_confirmations(state)
  charge_confirmed_shard_fees(state)
  reset_pending_shard_work(state)
  process_justification_and_finalization(state)
  process_rewards_and_penalties(state)
  process_registry_updates(state)
  process_slashings(state)
  process_eth1_data_reset(state)
  process_effective_balance_updates(state)
  process_slashings_reset(state)
  process_randao_mixes_reset(state)
  process_historical_roots_update(state)
  process_participation_record_updates(state)
  process_shard_epoch_increment(state)
}

fun process_pending_shard_confirmations(state: BeaconState): Unit {
  if (get_current_epoch(state) == GENESIS_EPOCH) {
    return
  }
  val previous_epoch = get_previous_epoch(state)
  val previous_epoch_start_slot = compute_start_slot_at_epoch(previous_epoch)
  for (slot in range(previous_epoch_start_slot, previous_epoch_start_slot + SLOTS_PER_EPOCH)) {
    val buffer_index = slot % SHARD_STATE_MEMORY_SLOTS
    for (shard_index in range(len(state.shard_buffer[buffer_index]))) {
      val committee_work = state.shard_buffer[buffer_index][shard_index]
      if (committee_work.status.selector == SHARD_WORK_PENDING) {
        val pending_headers_status: ShardWorkStatus_2 = committee_work.status as ShardWorkStatus_2
        val winning_header = max(pending_headers_status.value, key = { header -> header.weight })
        if (winning_header.commitment == DataCommitment()) {
          committee_work.status = ShardWorkStatus_0()
        } else {
          committee_work.status = ShardWorkStatus_1(winning_header.commitment)
        }
      }
    }
  }
}

fun charge_confirmed_shard_fees(state: BeaconState): Unit {
  val new_gasprice = state.shard_gasprice
  val previous_epoch = get_previous_epoch(state)
  val previous_epoch_start_slot = compute_start_slot_at_epoch(previous_epoch)
  val adjustment_quotient = get_active_shard_count(state, previous_epoch) * SLOTS_PER_EPOCH * GASPRICE_ADJUSTMENT_COEFFICIENT
  var new_gasprice_2 = new_gasprice
  for (slot in range(previous_epoch_start_slot, previous_epoch_start_slot + SLOTS_PER_EPOCH)) {
    val buffer_index = slot % SHARD_STATE_MEMORY_SLOTS
    var new_gasprice_3 = new_gasprice_2
    for (shard_index in range(len(state.shard_buffer[buffer_index]))) {
      val committee_work = state.shard_buffer[buffer_index][shard_index]
      if (committee_work.status.selector == SHARD_WORK_CONFIRMED) {
        val commitment: DataCommitment = (committee_work.status as ShardWorkStatus_1).value
        val proposer = get_shard_proposer_index(state, slot, Shard(shard_index))
        val fee = state.shard_gasprice * commitment.length / TARGET_SAMPLES_PER_BLOCK
        decrease_balance(state, proposer, fee)
        val new_gasprice_1 = compute_updated_gasprice(new_gasprice_3, commitment.length, adjustment_quotient)
        new_gasprice_3 = new_gasprice_1
      } else {
        new_gasprice_3 = new_gasprice_3
      }
    }
    new_gasprice_2 = new_gasprice_3
  }
  state.shard_gasprice = new_gasprice_2
}

fun reset_pending_shard_work(state: BeaconState): Unit {
  val next_epoch = get_current_epoch(state) + 1uL
  val next_epoch_start_slot = compute_start_slot_at_epoch(next_epoch)
  val committees_per_slot = get_committee_count_per_slot(state, next_epoch)
  val active_shards = get_active_shard_count(state, next_epoch)
  for (slot in range(next_epoch_start_slot, next_epoch_start_slot + SLOTS_PER_EPOCH)) {
    val buffer_index = slot % SHARD_STATE_MEMORY_SLOTS
    state.shard_buffer[buffer_index] = SSZList<ShardWork>(list(range(active_shards).map { _ -> ShardWork() }))
    val start_shard = get_start_shard(state, slot)
    for (committee_index in range(committees_per_slot)) {
      val shard = (start_shard + committee_index) % active_shards
      val committee_length = len(get_beacon_committee(state, slot, committee_index))
      state.shard_buffer[buffer_index][shard].status = ShardWorkStatus_2(SSZList<PendingShardHeader>(PendingShardHeader(commitment = DataCommitment(), root = Root(), votes = SSZBitlist(PyList(0uL) * committee_length), weight = 0uL, update_slot = slot)))
    }
  }
}

fun process_shard_epoch_increment(state: BeaconState): Unit {
  state.current_epoch_start_shard = get_start_shard(state, Slot(state.slot + 1uL))
}

