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
  return Shard((index + get_start_shard(state, slot)) % active_shards)
}

fun compute_committee_index_from_shard(state: BeaconState, slot: Slot, shard: Shard): CommitteeIndex {
  val active_shards = get_active_shard_count(state, compute_epoch_at_slot(slot))
  return CommitteeIndex((active_shards + shard - get_start_shard(state, slot)) % active_shards)
}

fun process_block(state: BeaconState, block: BeaconBlock) {
  process_block_header(state, block)
  process_randao(state, block.body)
  process_eth1_data(state, block.body)
  process_operations(state, block.body)
  process_execution_payload(state, block.body.execution_payload, EXECUTION_ENGINE)
}

fun process_operations(state: BeaconState, body: BeaconBlockBody) {
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
  update_pending_votes(state, attestation)
}

fun update_pending_votes(state: BeaconState, attestation: Attestation): Unit {
  // as AttestationData
  val pending_headers_2: SSZList<PendingShardHeader>
  if (compute_epoch_at_slot(attestation.data.slot) == get_current_epoch(state)) {
    val pending_headers = state.current_epoch_pending_shard_headers
    pending_headers_2 = pending_headers
  } else {
    val pending_headers_1 = state.previous_epoch_pending_shard_headers
    pending_headers_2 = pending_headers_1
  }
  val attestation_shard = compute_shard_from_committee_index(state, attestation.data.slot, attestation.data.index)
  val pending_header: PendingShardHeader? = null
  var pending_header_2 = pending_header
  for (header in pending_headers_2) {
    val data = attestation.data as AttestationData
    if ((header.root == data.shard_header_root) && (header.slot == data.slot) && (header.shard == attestation_shard)) {
      pending_header_2 = header
    }
  }
  assert(pending_header_2 != null)
  for (i in range(len(pending_header_2!!.votes))) {
    pending_header_2.votes[i] = pending_header_2.votes[i] || attestation.aggregation_bits[i]
  }
  val all_candidates = list(pending_headers_2.filter { c -> Pair(c.slot, c.shard) == Pair(pending_header_2.slot, pending_header_2.shard) }.map { c -> c })
  if (true in list(all_candidates.map { c -> c.confirmed })) {
    return
  }
  val participants = get_attesting_indices(state, attestation.data, pending_header_2.votes)
  val participants_balance = get_total_balance(state, participants)
  val full_committee = get_beacon_committee(state, attestation.data.slot, attestation.data.index)
  val full_committee_balance = get_total_balance(state, set(full_committee))
  if ((participants_balance * 3uL) >= (full_committee_balance * 2uL)) {
    pending_header_2.confirmed = true
  }
}

fun process_shard_header(state: BeaconState, signed_header: SignedShardBlobHeader): Unit {
  val header = signed_header.message
  assert((Slot(0uL) < header.slot) && (header.slot <= state.slot))
  val header_epoch = compute_epoch_at_slot(header.slot)
  assert(header_epoch in PyList(get_previous_epoch(state), get_current_epoch(state)))
  assert(header.shard < get_active_shard_count(state, header_epoch))
  assert(header.body_summary.beacon_block_root == get_block_root_at_slot(state, header.slot - 1uL))
  assert(header.proposer_index == get_shard_proposer_index(state, header.slot, header.shard))
  val signing_root = compute_signing_root(header, get_domain(state, DOMAIN_SHARD_PROPOSER))
  assert(bls.Verify(state.validators[header.proposer_index].pubkey, signing_root, signed_header.signature))
  val body_summary = header.body_summary
  if (body_summary.commitment.length == 0uL) {
    assert(body_summary.degree_proof == G1_SETUP[0uL])
  }
  assert(bls.Pairing(body_summary.degree_proof, G2_SETUP[0uL]) == bls.Pairing(body_summary.commitment.point, G2_SETUP[-body_summary.commitment.length]))
  val pending_headers_2: SSZList<PendingShardHeader>
  if (header_epoch == get_current_epoch(state)) {
    val pending_headers = state.current_epoch_pending_shard_headers
    pending_headers_2 = pending_headers
  } else {
    val pending_headers_1 = state.previous_epoch_pending_shard_headers
    pending_headers_2 = pending_headers_1
  }
  val header_root = hash_tree_root(header)
  assert(header_root !in list(pending_headers_2.map { pending_header -> pending_header.root }))
  val index = compute_committee_index_from_shard(state, header.slot, header.shard)
  val committee_length = len(get_beacon_committee(state, header.slot, index))
  pending_headers_2.append(PendingShardHeader(slot = header.slot, shard = header.shard, commitment = body_summary.commitment, root = header_root, votes = SSZBitlist(PyList(0uL) * committee_length), confirmed = false))
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
  process_justification_and_finalization(state)
  process_rewards_and_penalties(state)
  process_registry_updates(state)
  process_slashings(state)
  process_pending_headers(state)
  charge_confirmed_header_fees(state)
  reset_pending_headers(state)
  process_eth1_data_reset(state)
  process_effective_balance_updates(state)
  process_slashings_reset(state)
  process_randao_mixes_reset(state)
  process_historical_roots_update(state)
  process_participation_record_updates(state)
  process_shard_epoch_increment(state)
}

fun process_pending_headers(state: BeaconState): Unit {
  if (get_current_epoch(state) == GENESIS_EPOCH) {
    return
  }
  val previous_epoch = get_previous_epoch(state)
  val previous_epoch_start_slot = compute_start_slot_at_epoch(previous_epoch)
  for (slot in range(previous_epoch_start_slot, previous_epoch_start_slot + SLOTS_PER_EPOCH)) {
    for (shard_index in range(get_active_shard_count(state, previous_epoch))) {
      val shard = Shard(shard_index)
      val candidates = list(state.previous_epoch_pending_shard_headers.filter { c -> Pair(c.slot, c.shard) == Pair(slot, shard) }.map { c -> c })
      if (true in list(candidates.map { c -> c.confirmed })) {
        continue
      }
      val index = compute_committee_index_from_shard(state, slot, shard)
      val full_committee = get_beacon_committee(state, slot, index)
      val voting_sets = list(candidates.map { c -> set(enumerate(full_committee).filter { (i, v) -> c.votes[i] }.map { (i, v) -> v }) })
      val voting_balances = list(voting_sets.map { voters -> get_total_balance(state, voters) })
      val winning_index: Int
      if (max(voting_balances) > 0uL) {
        winning_index = voting_balances.index(max(voting_balances))
      } else {
        winning_index = list(candidates.map { c -> c.root }).index(Root())
      }
      candidates[winning_index].confirmed = true
    }
  }
  for (slot_index in range(SLOTS_PER_EPOCH)) {
    for (shard_1 in range(MAX_SHARDS)) {
      state.grandparent_epoch_confirmed_commitments[shard_1][slot_index] = DataCommitment()
    }
  }
  val confirmed_headers = list(state.previous_epoch_pending_shard_headers.filter { candidate -> candidate.confirmed }.map { candidate -> candidate })
  for (header in confirmed_headers) {
    state.grandparent_epoch_confirmed_commitments[header.shard][header.slot % SLOTS_PER_EPOCH] = header.commitment
  }
}

fun charge_confirmed_header_fees(state: BeaconState): Unit {
  val new_gasprice = state.shard_gasprice
  val previous_epoch = get_previous_epoch(state)
  val adjustment_quotient = get_active_shard_count(state, previous_epoch) * SLOTS_PER_EPOCH * GASPRICE_ADJUSTMENT_COEFFICIENT
  val previous_epoch_start_slot = compute_start_slot_at_epoch(previous_epoch)
  var new_gasprice_2 = new_gasprice
  for (slot in range(previous_epoch_start_slot, previous_epoch_start_slot + SLOTS_PER_EPOCH)) {
    var new_gasprice_3 = new_gasprice_2
    for (shard_index in range(get_active_shard_count(state, previous_epoch))) {
      val shard = Shard(shard_index)
      val confirmed_candidates = list(state.previous_epoch_pending_shard_headers.filter { c -> Triple(c.slot, c.shard, c.confirmed) == Triple(slot, shard, true) }.map { c -> c })
      if (!any(confirmed_candidates)) {
        new_gasprice_3 = new_gasprice_3
        continue
      }
      val candidate = confirmed_candidates[0uL]
      val proposer = get_shard_proposer_index(state, slot, shard)
      val fee = state.shard_gasprice * candidate.commitment.length / TARGET_SAMPLES_PER_BLOCK
      decrease_balance(state, proposer, fee)
      val new_gasprice_1 = compute_updated_gasprice(new_gasprice_3, candidate.commitment.length, adjustment_quotient)
      new_gasprice_3 = new_gasprice_1
    }
    new_gasprice_2 = new_gasprice_3
  }
  state.shard_gasprice = new_gasprice_2
}

fun reset_pending_headers(state: BeaconState): Unit {
  state.previous_epoch_pending_shard_headers = state.current_epoch_pending_shard_headers
  state.current_epoch_pending_shard_headers = SSZList<PendingShardHeader>()
  val next_epoch = get_current_epoch(state) + 1uL
  val next_epoch_start_slot = compute_start_slot_at_epoch(next_epoch)
  for (slot in range(next_epoch_start_slot, next_epoch_start_slot + SLOTS_PER_EPOCH)) {
    for (index in range(get_committee_count_per_slot(state, next_epoch))) {
      val committee_index = CommitteeIndex(index)
      val shard = compute_shard_from_committee_index(state, slot, committee_index)
      val committee_length = len(get_beacon_committee(state, slot, committee_index))
      state.current_epoch_pending_shard_headers.append(PendingShardHeader(slot = slot, shard = shard, commitment = DataCommitment(), root = Root(), votes = SSZBitlist(PyList(0uL) * committee_length), confirmed = false))
    }
  }
}

fun process_shard_epoch_increment(state: BeaconState): Unit {
  state.current_epoch_start_shard = get_start_shard(state, Slot(state.slot + 1uL))
}

///*
//    Return the seed at ``epoch``.
//    */
//fun get_seed(state: BeaconState, epoch: Epoch, domain_type: DomainType): Bytes32 {
//  val mix = get_randao_mix(state, Epoch(epoch + EPOCHS_PER_HISTORICAL_VECTOR - MIN_SEED_LOOKAHEAD - 1uL))
//  return hash(domain_type + uint_to_bytes(epoch) + mix)
//}
//
//fun is_transition_completed(state: BeaconState): pybool {
//  return state.latest_execution_payload_header != merge.ExecutionPayloadHeader()
//}
//
//fun is_transition_block(state: BeaconState, block_body: BeaconBlockBody): pybool {
//  return !is_transition_completed(state) && (block_body.execution_payload != merge.ExecutionPayload())
//}
//
//fun compute_time_at_slot(state: BeaconState, slot: Slot): uint64 {
//  val slots_since_genesis = slot - GENESIS_SLOT
//  return uint64(state.genesis_time + (slots_since_genesis * SECONDS_PER_SLOT))
//}
//
///*
//    Note: This function is designed to be able to be run in parallel with the other `process_block` sub-functions
//    */
//fun process_execution_payload(state: BeaconState, body: BeaconBlockBody): Unit {
//  if (!is_transition_completed(state) && !is_transition_block(state, body)) {
//    return
//  }
//  val execution_payload = body.execution_payload
//  if (is_transition_completed(state)) {
//    assert(execution_payload.parent_hash == state.latest_execution_payload_header.block_hash)
//    assert(execution_payload.number == (state.latest_execution_payload_header.number + 1uL))
//  }
//  assert(execution_payload.timestamp == compute_time_at_slot(state, state.slot))
//  assert(verify_execution_state_transition(execution_payload))
//  state.latest_execution_payload_header = merge.ExecutionPayloadHeader(block_hash = execution_payload.block_hash, parent_hash = execution_payload.parent_hash, coinbase = execution_payload.coinbase, state_root = execution_payload.state_root, number = execution_payload.number, gas_limit = execution_payload.gas_limit, gas_used = execution_payload.gas_used, timestamp = execution_payload.timestamp, receipt_root = execution_payload.receipt_root, logs_bloom = execution_payload.logs_bloom, transactions_root = hash_tree_root(execution_payload.transactions))
//}
//
///*
//    Check if ``validator`` is eligible for activation.
//    */
//fun is_eligible_for_activation(state: BeaconState, validator: Validator): pybool {
//  return (validator.activation_eligibility_epoch <= state.finalized_checkpoint.epoch) && (validator.activation_epoch == FAR_FUTURE_EPOCH)
//}
//
///*
//    Return the current epoch.
//    */
//fun get_current_epoch(state: BeaconState): Epoch {
//  return compute_epoch_at_slot(state.slot)
//}
//
///*`
//    Return the previous epoch (unless the current epoch is ``GENESIS_EPOCH``).
//    */
//fun get_previous_epoch(state: BeaconState): Epoch {
//  val current_epoch = get_current_epoch(state)
//  return if (current_epoch == GENESIS_EPOCH) GENESIS_EPOCH else Epoch(current_epoch - 1uL)
//}
//
///*
//    Return the block root at the start of a recent ``epoch``.
//    */
//fun get_block_root(state: BeaconState, epoch: Epoch): Root {
//  return get_block_root_at_slot(state, compute_start_slot_at_epoch(epoch))
//}
//
///*
//    Return the block root at a recent ``slot``.
//    */
//fun get_block_root_at_slot(state: BeaconState, slot: Slot): Root {
//  assert((slot < state.slot) && (state.slot <= (slot + SLOTS_PER_HISTORICAL_ROOT)))
//  return state.block_roots[slot % SLOTS_PER_HISTORICAL_ROOT]
//}
//
///*
//    Return the randao mix at a recent ``epoch``.
//    */
//fun get_randao_mix(state: BeaconState, epoch: Epoch): Bytes32 {
//  return state.randao_mixes[epoch % EPOCHS_PER_HISTORICAL_VECTOR]
//}
//
///*
//    Return the sequence of active validator indices at ``epoch``.
//    */
//fun get_active_validator_indices(state: BeaconState, epoch: Epoch): Sequence<ValidatorIndex> {
//  return list(enumerate(state.validators).filter { (i, v) -> is_active_validator(v, epoch) }.map { (i, v) -> ValidatorIndex(i) })
//}
//
///*
//    Return the validator churn limit for the current epoch.
//    */
//fun get_validator_churn_limit(state: BeaconState): uint64 {
//  val active_validator_indices = get_active_validator_indices(state, get_current_epoch(state))
//  return max(MIN_PER_EPOCH_CHURN_LIMIT, uint64(len(active_validator_indices)) / CHURN_LIMIT_QUOTIENT)
//}
//
///*
//    Return the beacon committee at ``slot`` for ``index``.
//    */
//fun get_beacon_committee(state: BeaconState, slot: Slot, index: CommitteeIndex): Sequence<ValidatorIndex> {
//  val epoch = compute_epoch_at_slot(slot)
//  val committees_per_slot = get_committee_count_per_slot(state, epoch)
//  return compute_committee(indices = get_active_validator_indices(state, epoch), seed = get_seed(state, epoch, DOMAIN_BEACON_ATTESTER), index = (slot % SLOTS_PER_EPOCH * committees_per_slot) + index, count = committees_per_slot * SLOTS_PER_EPOCH)
//}
//
///*
//    Return the beacon proposer index at the current slot.
//    */
//fun get_beacon_proposer_index(state: BeaconState): ValidatorIndex {
//  val epoch = get_current_epoch(state)
//  val seed = hash(get_seed(state, epoch, DOMAIN_BEACON_PROPOSER) + uint_to_bytes(state.slot))
//  val indices = get_active_validator_indices(state, epoch)
//  return compute_proposer_index(state, indices, seed)
//}
//
///*
//    Return the combined effective balance of the ``indices``.
//    ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
//    Math safe up to ~10B ETH, afterwhich this overflows uint64.
//    */
//fun get_total_balance(state: BeaconState, indices: Set<ValidatorIndex>): Gwei {
//  return Gwei(max(EFFECTIVE_BALANCE_INCREMENT, sum(list(indices.map { index -> state.validators[index].effective_balance }))))
//}
//
///*
//    Return the combined effective balance of the active validators.
//    Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
//    */
//fun get_total_active_balance(state: BeaconState): Gwei {
//  return get_total_balance(state, set(get_active_validator_indices(state, get_current_epoch(state))))
//}
//
///*
//    Return the signature domain (fork version concatenated with domain type) of a message.
//    */
//fun get_domain(state: BeaconState, domain_type: DomainType, epoch: Epoch? = null): Domain {
//  val epoch_1 = if (epoch == null) get_current_epoch(state) else epoch
//  val fork_version = if (epoch_1 < state.fork.epoch) state.fork.previous_version else state.fork.current_version
//  return compute_domain(domain_type, fork_version, state.genesis_validators_root)
//}
//
///*
//    Return the set of attesting indices corresponding to ``data`` and ``bits``.
//    */
//fun get_attesting_indices(state: BeaconState, data: AttestationData, bits: SSZBitlist): Set<ValidatorIndex> {
//  val committee = get_beacon_committee(state, data.slot, data.index)
//  return set(enumerate(committee).filter { (i, index) -> bits[i] }.map { (i, index) -> index })
//}
//
///*
//    Increase the validator balance at index ``index`` by ``delta``.
//    */
//fun increase_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei): Unit {
//  state.balances[index] = state.balances[index] + delta
//}
//
///*
//    Decrease the validator balance at index ``index`` by ``delta``, with underflow protection.
//    */
//fun decrease_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei): Unit {
//  state.balances[index] = Gwei(if (delta > state.balances[index]) 0uL else state.balances[index] - delta)
//}
//
///*
//    Initiate the exit of the validator with index ``index``.
//    */
//fun initiate_validator_exit(state: BeaconState, index: ValidatorIndex): Unit {
//  val validator = state.validators[index]
//  if (validator.exit_epoch != FAR_FUTURE_EPOCH) {
//    return
//  }
//  val exit_epochs = list(state.validators.filter { v -> v.exit_epoch != FAR_FUTURE_EPOCH }.map { v -> v.exit_epoch })
//  val exit_queue_epoch = max(exit_epochs + PyList(compute_activation_exit_epoch(get_current_epoch(state))))
//  val exit_queue_churn = len(list(state.validators.filter { v -> v.exit_epoch == exit_queue_epoch }.map { v -> v }))
//  val exit_queue_epoch_2: Epoch
//  if (exit_queue_churn >= get_validator_churn_limit(state)) {
//    val exit_queue_epoch_1 = exit_queue_epoch + Epoch(1uL)
//    exit_queue_epoch_2 = exit_queue_epoch_1
//  } else {
//    exit_queue_epoch_2 = exit_queue_epoch
//  }
//  validator.exit_epoch = exit_queue_epoch_2
//  validator.withdrawable_epoch = Epoch(validator.exit_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY)
//}
//
///*
//    Slash the validator with index ``slashed_index``.
//    */
//fun slash_validator(state: BeaconState, slashed_index: ValidatorIndex, whistleblower_index: ValidatorIndex? = null): Unit {
//  val epoch = get_current_epoch(state)
//  initiate_validator_exit(state, slashed_index)
//  val validator = state.validators[slashed_index]
//  validator.slashed = true
//  validator.withdrawable_epoch = max(validator.withdrawable_epoch, Epoch(epoch + EPOCHS_PER_SLASHINGS_VECTOR))
//  state.slashings[epoch % EPOCHS_PER_SLASHINGS_VECTOR] = state.slashings[epoch % EPOCHS_PER_SLASHINGS_VECTOR] + validator.effective_balance
//  decrease_balance(state, slashed_index, validator.effective_balance / MIN_SLASHING_PENALTY_QUOTIENT)
//  val proposer_index = get_beacon_proposer_index(state)
//  val whistleblower_index_2: ValidatorIndex
//  if (whistleblower_index == null) {
//    val whistleblower_index_1 = proposer_index
//    whistleblower_index_2 = whistleblower_index_1
//  } else {
//    whistleblower_index_2 = whistleblower_index
//  }
//  val whistleblower_reward = Gwei(validator.effective_balance / WHISTLEBLOWER_REWARD_QUOTIENT)
//  val proposer_reward = Gwei(whistleblower_reward / PROPOSER_REWARD_QUOTIENT)
//  increase_balance(state, proposer_index, proposer_reward)
//  increase_balance(state, whistleblower_index_2, Gwei(whistleblower_reward - proposer_reward))
//}
//
//fun get_matching_source_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
//  assert(epoch in Pair(get_previous_epoch(state), get_current_epoch(state)))
//  return if (epoch == get_current_epoch(state)) state.current_epoch_attestations else state.previous_epoch_attestations
//}
//
//fun get_matching_target_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
//  return list(get_matching_source_attestations(state, epoch).filter { a -> a.data.target.root == get_block_root(state, epoch) }.map { a -> a })
//}
//
//fun get_matching_head_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
//  return list(get_matching_target_attestations(state, epoch).filter { a -> a.data.beacon_block_root == get_block_root_at_slot(state, a.data.slot) }.map { a -> a })
//}
//
//fun get_unslashed_attesting_indices(state: BeaconState, attestations: Sequence<PendingAttestation>): Set<ValidatorIndex> {
//  val output = set<ValidatorIndex>()
//  var output_2 = output
//  for (a in attestations) {
//    val output_1 = output_2.union(get_attesting_indices(state, a.data, a.aggregation_bits))
//    output_2 = output_1
//  }
//  return set(filter({ index -> !state.validators[index].slashed }, output_2))
//}
//
///*
//    Return the combined effective balance of the set of unslashed validators participating in ``attestations``.
//    Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
//    */
//fun get_attesting_balance(state: BeaconState, attestations: Sequence<PendingAttestation>): Gwei {
//  return get_total_balance(state, get_unslashed_attesting_indices(state, attestations))
//}
//
//fun process_justification_and_finalization(state: BeaconState): Unit {
//  if (get_current_epoch(state) <= (GENESIS_EPOCH + 1uL)) {
//    return
//  }
//  val previous_attestations = get_matching_target_attestations(state, get_previous_epoch(state))
//  val current_attestations = get_matching_target_attestations(state, get_current_epoch(state))
//  val total_active_balance = get_total_active_balance(state)
//  val previous_target_balance = get_attesting_balance(state, previous_attestations)
//  val current_target_balance = get_attesting_balance(state, current_attestations)
//  weigh_justification_and_finalization(state, total_active_balance, previous_target_balance, current_target_balance)
//}
//
//fun weigh_justification_and_finalization(state: BeaconState, total_active_balance: Gwei, previous_epoch_target_balance: Gwei, current_epoch_target_balance: Gwei): Unit {
//  val previous_epoch = get_previous_epoch(state)
//  val current_epoch = get_current_epoch(state)
//  val old_previous_justified_checkpoint = state.previous_justified_checkpoint
//  val old_current_justified_checkpoint = state.current_justified_checkpoint
//  state.previous_justified_checkpoint = state.current_justified_checkpoint
//  state.justification_bits[1uL until len(state.justification_bits)] = state.justification_bits[0uL until (JUSTIFICATION_BITS_LENGTH - 1uL)]
//  state.justification_bits[0uL] = 0uL
//  if ((previous_epoch_target_balance * 3uL) >= (total_active_balance * 2uL)) {
//    state.current_justified_checkpoint = Checkpoint(epoch = previous_epoch, root = get_block_root(state, previous_epoch))
//    state.justification_bits[1uL] = 1uL
//  }
//  if ((current_epoch_target_balance * 3uL) >= (total_active_balance * 2uL)) {
//    state.current_justified_checkpoint = Checkpoint(epoch = current_epoch, root = get_block_root(state, current_epoch))
//    state.justification_bits[0uL] = 1uL
//  }
//  val bits = state.justification_bits
//  if (all(bits[1uL until 4uL]) && ((old_previous_justified_checkpoint.epoch + 3uL) == current_epoch)) {
//    state.finalized_checkpoint = old_previous_justified_checkpoint
//  }
//  if (all(bits[1uL until 3uL]) && ((old_previous_justified_checkpoint.epoch + 2uL) == current_epoch)) {
//    state.finalized_checkpoint = old_previous_justified_checkpoint
//  }
//  if (all(bits[0uL until 3uL]) && ((old_current_justified_checkpoint.epoch + 2uL) == current_epoch)) {
//    state.finalized_checkpoint = old_current_justified_checkpoint
//  }
//  if (all(bits[0uL until 2uL]) && ((old_current_justified_checkpoint.epoch + 1uL) == current_epoch)) {
//    state.finalized_checkpoint = old_current_justified_checkpoint
//  }
//}
//
//fun get_base_reward(state: BeaconState, index: ValidatorIndex): Gwei {
//  val total_balance = get_total_active_balance(state)
//  val effective_balance = state.validators[index].effective_balance
//  return Gwei(effective_balance * BASE_REWARD_FACTOR / integer_squareroot(total_balance) / BASE_REWARDS_PER_EPOCH)
//}
//
//fun get_proposer_reward(state: BeaconState, attesting_index: ValidatorIndex): Gwei {
//  return Gwei(get_base_reward(state, attesting_index) / PROPOSER_REWARD_QUOTIENT)
//}
//
//fun get_finality_delay(state: BeaconState): uint64 {
//  return get_previous_epoch(state) - state.finalized_checkpoint.epoch
//}
//
//fun is_in_inactivity_leak(state: BeaconState): pybool {
//  return get_finality_delay(state) > MIN_EPOCHS_TO_INACTIVITY_PENALTY
//}
//
//fun get_eligible_validator_indices(state: BeaconState): Sequence<ValidatorIndex> {
//  val previous_epoch = get_previous_epoch(state)
//  return list(enumerate(state.validators).filter { (index, v) -> is_active_validator(v, previous_epoch) || (v.slashed && ((previous_epoch + 1uL) < v.withdrawable_epoch)) }.map { (index, v) -> ValidatorIndex(index) })
//}
//
///*
//    Helper with shared logic for use by get source, target, and head deltas functions
//    */
//fun get_attestation_component_deltas(state: BeaconState, attestations: Sequence<PendingAttestation>): Pair<Sequence<Gwei>, Sequence<Gwei>> {
//  val rewards = PyList(Gwei(0uL)) * len(state.validators)
//  val penalties = PyList(Gwei(0uL)) * len(state.validators)
//  val total_balance = get_total_active_balance(state)
//  val unslashed_attesting_indices = get_unslashed_attesting_indices(state, attestations)
//  val attesting_balance = get_total_balance(state, unslashed_attesting_indices)
//  for (index in get_eligible_validator_indices(state)) {
//    if (index in unslashed_attesting_indices) {
//      val increment = EFFECTIVE_BALANCE_INCREMENT
//      if (is_in_inactivity_leak(state)) {
//        rewards[index] = rewards[index] + get_base_reward(state, index)
//      } else {
//        val reward_numerator = get_base_reward(state, index) * (attesting_balance / increment)
//        rewards[index] = rewards[index] + (reward_numerator / (total_balance / increment))
//      }
//    } else {
//      penalties[index] = penalties[index] + get_base_reward(state, index)
//    }
//  }
//  return Pair(rewards, penalties)
//}
//
///*
//    Return attester micro-rewards/penalties for source-vote for each validator.
//    */
//fun get_source_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
//  val matching_source_attestations = get_matching_source_attestations(state, get_previous_epoch(state))
//  return get_attestation_component_deltas(state, matching_source_attestations)
//}
//
///*
//    Return attester micro-rewards/penalties for target-vote for each validator.
//    */
//fun get_target_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
//  val matching_target_attestations = get_matching_target_attestations(state, get_previous_epoch(state))
//  return get_attestation_component_deltas(state, matching_target_attestations)
//}
//
///*
//    Return attester micro-rewards/penalties for head-vote for each validator.
//    */
//fun get_head_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
//  val matching_head_attestations = get_matching_head_attestations(state, get_previous_epoch(state))
//  return get_attestation_component_deltas(state, matching_head_attestations)
//}
//
///*
//    Return proposer and inclusion delay micro-rewards/penalties for each validator.
//    */
//fun get_inclusion_delay_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
//  val rewards = list(range(len(state.validators)).map { _ -> Gwei(0uL) })
//  val matching_source_attestations = get_matching_source_attestations(state, get_previous_epoch(state))
//  for (index in get_unslashed_attesting_indices(state, matching_source_attestations)) {
//    val attestation = min(list(matching_source_attestations.filter { a -> index in get_attesting_indices(state, a.data, a.aggregation_bits) }.map { a -> a }), key = { a -> a.inclusion_delay })
//    rewards[attestation.proposer_index] = rewards[attestation.proposer_index] + get_proposer_reward(state, index)
//    val max_attester_reward = Gwei(get_base_reward(state, index) - get_proposer_reward(state, index))
//    rewards[index] = rewards[index] + Gwei(max_attester_reward / attestation.inclusion_delay)
//  }
//  val penalties = list(range(len(state.validators)).map { _ -> Gwei(0uL) })
//  return Pair(rewards, penalties)
//}
//
///*
//    Return inactivity reward/penalty deltas for each validator.
//    */
//fun get_inactivity_penalty_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
//  val penalties = list(range(len(state.validators)).map { _ -> Gwei(0uL) })
//  if (is_in_inactivity_leak(state)) {
//    val matching_target_attestations = get_matching_target_attestations(state, get_previous_epoch(state))
//    val matching_target_attesting_indices = get_unslashed_attesting_indices(state, matching_target_attestations)
//    for (index in get_eligible_validator_indices(state)) {
//      val base_reward = get_base_reward(state, index)
//      penalties[index] = penalties[index] + Gwei((BASE_REWARDS_PER_EPOCH * base_reward) - get_proposer_reward(state, index))
//      if (index !in matching_target_attesting_indices) {
//        val effective_balance = state.validators[index].effective_balance
//        penalties[index] = penalties[index] + Gwei(effective_balance * get_finality_delay(state) / INACTIVITY_PENALTY_QUOTIENT)
//      }
//    }
//  }
//  val rewards = list(range(len(state.validators)).map { _ -> Gwei(0uL) })
//  return Pair(rewards, penalties)
//}
//
///*
//    Return attestation reward/penalty deltas for each validator.
//    */
//fun get_attestation_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
//  val (source_rewards,source_penalties) = get_source_deltas(state)
//  val (target_rewards,target_penalties) = get_target_deltas(state)
//  val (head_rewards,head_penalties) = get_head_deltas(state)
//  val (inclusion_delay_rewards,_) = get_inclusion_delay_deltas(state)
//  val (__1,inactivity_penalties) = get_inactivity_penalty_deltas(state)
//  val rewards = list(range(len(state.validators)).map { i -> source_rewards[i] + target_rewards[i] + head_rewards[i] + inclusion_delay_rewards[i] })
//  val penalties = list(range(len(state.validators)).map { i -> source_penalties[i] + target_penalties[i] + head_penalties[i] + inactivity_penalties[i] })
//  return Pair(rewards, penalties)
//}
//
//fun process_rewards_and_penalties(state: BeaconState): Unit {
//  if (get_current_epoch(state) == GENESIS_EPOCH) {
//    return
//  }
//  val (rewards,penalties) = get_attestation_deltas(state)
//  for (index in range(len(state.validators))) {
//    increase_balance(state, ValidatorIndex(index), rewards[index])
//    decrease_balance(state, ValidatorIndex(index), penalties[index])
//  }
//}
//
//fun process_registry_updates(state: BeaconState): Unit {
//  for ((index, validator) in enumerate(state.validators)) {
//    if (is_eligible_for_activation_queue(validator)) {
//      validator.activation_eligibility_epoch = get_current_epoch(state) + 1uL
//    }
//    if (is_active_validator(validator, get_current_epoch(state)) && (validator.effective_balance <= EJECTION_BALANCE)) {
//      initiate_validator_exit(state, ValidatorIndex(index))
//    }
//  }
//  val activation_queue = sorted(list(enumerate(state.validators).filter { (index, validator) -> is_eligible_for_activation(state, validator) }.map { (index, validator) -> index }), key = { index -> Tuple2(state.validators[index].activation_eligibility_epoch, index) })
//  for (index_1 in activation_queue[0uL until get_validator_churn_limit(state)]) {
//    val validator_1 = state.validators[index_1]
//    validator_1.activation_epoch = compute_activation_exit_epoch(get_current_epoch(state))
//  }
//}
//
//fun process_slashings(state: BeaconState): Unit {
//  val epoch = get_current_epoch(state)
//  val total_balance = get_total_active_balance(state)
//  val adjusted_total_slashing_balance = min(sum(state.slashings) * PROPORTIONAL_SLASHING_MULTIPLIER, total_balance)
//  for ((index, validator) in enumerate(state.validators)) {
//    if (validator.slashed && ((epoch + (EPOCHS_PER_SLASHINGS_VECTOR / 2uL)) == validator.withdrawable_epoch)) {
//      val increment = EFFECTIVE_BALANCE_INCREMENT
//      val penalty_numerator = validator.effective_balance / increment * adjusted_total_slashing_balance
//      val penalty = penalty_numerator / total_balance * increment
//      decrease_balance(state, ValidatorIndex(index), penalty)
//    }
//  }
//}
//
//fun process_eth1_data_reset(state: BeaconState): Unit {
//  val next_epoch = Epoch(get_current_epoch(state) + 1uL)
//  if ((next_epoch % EPOCHS_PER_ETH1_VOTING_PERIOD) == 0uL) {
//    state.eth1_data_votes = SSZList<Eth1Data>()
//  }
//}
//
//fun process_effective_balance_updates(state: BeaconState): Unit {
//  for ((index, validator) in enumerate(state.validators)) {
//    val balance = state.balances[index]
//    val HYSTERESIS_INCREMENT = uint64(EFFECTIVE_BALANCE_INCREMENT / HYSTERESIS_QUOTIENT)
//    val DOWNWARD_THRESHOLD = HYSTERESIS_INCREMENT * HYSTERESIS_DOWNWARD_MULTIPLIER
//    val UPWARD_THRESHOLD = HYSTERESIS_INCREMENT * HYSTERESIS_UPWARD_MULTIPLIER
//    if (((balance + DOWNWARD_THRESHOLD) < validator.effective_balance) || ((validator.effective_balance + UPWARD_THRESHOLD) < balance)) {
//      validator.effective_balance = min(balance - (balance % EFFECTIVE_BALANCE_INCREMENT), MAX_EFFECTIVE_BALANCE)
//    }
//  }
//}
//
//fun process_slashings_reset(state: BeaconState): Unit {
//  val next_epoch = Epoch(get_current_epoch(state) + 1uL)
//  state.slashings[next_epoch % EPOCHS_PER_SLASHINGS_VECTOR] = Gwei(0uL)
//}
//
//fun process_randao_mixes_reset(state: BeaconState): Unit {
//  val current_epoch = get_current_epoch(state)
//  val next_epoch = Epoch(current_epoch + 1uL)
//  state.randao_mixes[next_epoch % EPOCHS_PER_HISTORICAL_VECTOR] = get_randao_mix(state, current_epoch)
//}
//
//fun process_historical_roots_update(state: BeaconState): Unit {
//  val next_epoch = Epoch(get_current_epoch(state) + 1uL)
//  if ((next_epoch % (SLOTS_PER_HISTORICAL_ROOT / SLOTS_PER_EPOCH)) == 0uL) {
//    val historical_batch = HistoricalBatch(block_roots = state.block_roots, state_roots = state.state_roots)
//    state.historical_roots.append(hash_tree_root(historical_batch))
//  }
//}
//
//fun process_participation_record_updates(state: BeaconState): Unit {
//  state.previous_epoch_attestations = state.current_epoch_attestations
//  state.current_epoch_attestations = SSZList<PendingAttestation>()
//}
//
//fun process_block_header(state: BeaconState, block: BeaconBlock): Unit {
//  assert(block.slot == state.slot)
//  assert(block.slot > state.latest_block_header.slot)
//  assert(block.proposer_index == get_beacon_proposer_index(state))
//  assert(block.parent_root == hash_tree_root(state.latest_block_header))
//  state.latest_block_header = BeaconBlockHeader(slot = block.slot, proposer_index = block.proposer_index, parent_root = block.parent_root, state_root = Bytes32(), body_root = hash_tree_root(block.body))
//  val proposer = state.validators[block.proposer_index]
//  assert(!proposer.slashed)
//}
//
//fun process_randao(state: BeaconState, body: BeaconBlockBody): Unit {
//  val epoch = get_current_epoch(state)
//  val proposer = state.validators[get_beacon_proposer_index(state)]
//  val signing_root = compute_signing_root(epoch, get_domain(state, DOMAIN_RANDAO))
//  assert(bls.Verify(proposer.pubkey, signing_root, body.randao_reveal))
//  val mix = xor(get_randao_mix(state, epoch), hash(body.randao_reveal))
//  state.randao_mixes[epoch % EPOCHS_PER_HISTORICAL_VECTOR] = mix
//}
//
//fun process_eth1_data(state: BeaconState, body: BeaconBlockBody): Unit {
//  state.eth1_data_votes.append(body.eth1_data)
//  if ((state.eth1_data_votes.count(body.eth1_data) * 2uL) > (EPOCHS_PER_ETH1_VOTING_PERIOD * SLOTS_PER_EPOCH)) {
//    state.eth1_data = body.eth1_data
//  }
//}
//
//fun process_proposer_slashing(state: BeaconState, proposer_slashing: ProposerSlashing): Unit {
//  val header_1 = proposer_slashing.signed_header_1.message
//  val header_2 = proposer_slashing.signed_header_2.message
//  assert(header_1.slot == header_2.slot)
//  assert(header_1.proposer_index == header_2.proposer_index)
//  assert(header_1 != header_2)
//  val proposer = state.validators[header_1.proposer_index]
//  assert(is_slashable_validator(proposer, get_current_epoch(state)))
//  for (signed_header in listOf(proposer_slashing.signed_header_1, proposer_slashing.signed_header_2)) {
//    val domain = get_domain(state, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(signed_header.message.slot))
//    val signing_root = compute_signing_root(signed_header.message, domain)
//    assert(bls.Verify(proposer.pubkey, signing_root, signed_header.signature))
//  }
//  slash_validator(state, header_1.proposer_index)
//}
//
//fun get_validator_from_deposit(state: BeaconState, deposit: Deposit): Validator {
//  val amount = deposit.data.amount
//  val effective_balance = min(amount - (amount % EFFECTIVE_BALANCE_INCREMENT), MAX_EFFECTIVE_BALANCE)
//  return Validator(pubkey = deposit.data.pubkey, withdrawal_credentials = deposit.data.withdrawal_credentials, activation_eligibility_epoch = FAR_FUTURE_EPOCH, activation_epoch = FAR_FUTURE_EPOCH, exit_epoch = FAR_FUTURE_EPOCH, withdrawable_epoch = FAR_FUTURE_EPOCH, effective_balance = effective_balance)
//}
//
//fun process_deposit(state: BeaconState, deposit: Deposit): Unit {
//  assert(is_valid_merkle_branch(leaf = hash_tree_root(deposit.data), branch = deposit.proof, depth = DEPOSIT_CONTRACT_TREE_DEPTH + 1uL, index = state.eth1_deposit_index, root = state.eth1_data.deposit_root))
//  state.eth1_deposit_index = state.eth1_deposit_index + 1uL
//  val pubkey = deposit.data.pubkey
//  val amount = deposit.data.amount
//  val validator_pubkeys = list(state.validators.map { v -> v.pubkey })
//  if (pubkey !in validator_pubkeys) {
//    val deposit_message = DepositMessage(pubkey = deposit.data.pubkey, withdrawal_credentials = deposit.data.withdrawal_credentials, amount = deposit.data.amount)
//    val domain = compute_domain(DOMAIN_DEPOSIT)
//    val signing_root = compute_signing_root(deposit_message, domain)
//    if (!bls.Verify(pubkey, signing_root, deposit.data.signature)) {
//      return
//    }
//    state.validators.append(get_validator_from_deposit(state, deposit))
//    state.balances.append(amount)
//  } else {
//    val index = ValidatorIndex(validator_pubkeys.index(pubkey))
//    increase_balance(state, index, amount)
//  }
//}
//
//fun process_voluntary_exit(state: BeaconState, signed_voluntary_exit: SignedVoluntaryExit): Unit {
//  val voluntary_exit = signed_voluntary_exit.message
//  val validator = state.validators[voluntary_exit.validator_index]
//  assert(is_active_validator(validator, get_current_epoch(state)))
//  assert(validator.exit_epoch == FAR_FUTURE_EPOCH)
//  assert(get_current_epoch(state) >= voluntary_exit.epoch)
//  assert(get_current_epoch(state) >= (validator.activation_epoch + SHARD_COMMITTEE_PERIOD))
//  val domain = get_domain(state, DOMAIN_VOLUNTARY_EXIT, voluntary_exit.epoch)
//  val signing_root = compute_signing_root(voluntary_exit, domain)
//  assert(bls.Verify(validator.pubkey, signing_root, signed_voluntary_exit.signature))
//  initiate_validator_exit(state, voluntary_exit.validator_index)
//}
