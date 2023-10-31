package altair

import deps.bls
import deps.copy
import deps.hash
import deps.hash_tree_root
import phase0.*
import pylib.*
import ssz.*

/*
    Return a new ``ParticipationFlags`` adding ``flag_index`` to ``flags``.
    */
fun add_flag(flags: ParticipationFlags, flag_index: pyint): ParticipationFlags {
  val flag = ParticipationFlags(uint8(2uL.pow(uint64(flag_index))))
  return flags or flag
}

/*
    Return whether ``flags`` has ``flag_index`` set.
    */
fun has_flag(flags: ParticipationFlags, flag_index: pyint): pybool {
  val flag = ParticipationFlags(uint8(2uL.pow(uint64(flag_index))))
  return (flags and flag) == flag
}

/*
    Return the sync committee indices, with possible duplicates, for the next sync committee.
    */
fun get_next_sync_committee_indices(state: BeaconState): Sequence<ValidatorIndex> {
  val epoch = Epoch(get_current_epoch(state) + 1uL)
  val MAX_RANDOM_BYTE = 2uL.pow(8uL) - 1uL
  val active_validator_indices = get_active_validator_indices(state, epoch)
  val active_validator_count = uint64(len(active_validator_indices))
  val seed = get_seed(state, epoch, DOMAIN_SYNC_COMMITTEE)
  val i = 0uL
  val sync_committee_indices: PyList<ValidatorIndex> = PyList()
  var i_2 = i
  while (len(sync_committee_indices) < SYNC_COMMITTEE_SIZE) {
    val shuffled_index = compute_shuffled_index(uint64(i_2 % active_validator_count), active_validator_count, seed)
    val candidate_index = active_validator_indices[shuffled_index]
    val random_byte = hash(seed + uint_to_bytes(uint64(i_2 / 32uL)))[i_2 % 32uL]
    val effective_balance = state.validators[candidate_index].effective_balance
    if ((effective_balance * MAX_RANDOM_BYTE) >= (MAX_EFFECTIVE_BALANCE * uint8(random_byte))) {
      sync_committee_indices.append(candidate_index)
    }
    val i_1 = i_2 + 1uL
    i_2 = i_1
  }
  return sync_committee_indices
}

/*
    Return the next sync committee, with possible pubkey duplicates.
    */
fun get_next_sync_committee(state: BeaconState): SyncCommittee {
  val indices = get_next_sync_committee_indices(state)
  val pubkeys = list(indices.map { index -> state.validators[index].pubkey })
  val aggregate_pubkey = eth_aggregate_pubkeys(pubkeys)
  return SyncCommittee(pubkeys = pubkeys, aggregate_pubkey = aggregate_pubkey)
}

fun get_base_reward_per_increment(state: BeaconState): Gwei {
  return Gwei(EFFECTIVE_BALANCE_INCREMENT * BASE_REWARD_FACTOR / integer_squareroot(get_total_active_balance(state)))
}

/*
    Return the base reward for the validator defined by ``index`` with respect to the current ``state``.
    */
fun get_base_reward(state: BeaconState, index: ValidatorIndex): Gwei {
  val increments = state.validators[index].effective_balance / EFFECTIVE_BALANCE_INCREMENT
  return Gwei(increments * get_base_reward_per_increment(state))
}

/*
    Return the set of validator indices that are both active and unslashed for the given ``flag_index`` and ``epoch``.
    */
fun get_unslashed_participating_indices(state: BeaconState, flag_index: pyint, epoch: Epoch): Set<ValidatorIndex> {
  assert(epoch in Pair(get_previous_epoch(state), get_current_epoch(state)))
  val epoch_participation_2: SSZList<ParticipationFlags>
  if (epoch == get_current_epoch(state)) {
    val epoch_participation = state.current_epoch_participation
    epoch_participation_2 = epoch_participation
  } else {
    val epoch_participation_1 = state.previous_epoch_participation
    epoch_participation_2 = epoch_participation_1
  }
  val active_validator_indices = get_active_validator_indices(state, epoch)
  val participating_indices = list(active_validator_indices.filter { i -> has_flag(epoch_participation_2[i], flag_index) }.map { i -> i })
  return set(filter({ index -> !state.validators[index].slashed }, participating_indices))
}

/*
    Return the flag indices that are satisfied by an attestation.
    */
fun get_attestation_participation_flag_indices(state: BeaconState, data: AttestationData, inclusion_delay: uint64): Sequence<pyint> {
  val justified_checkpoint_2: Checkpoint
  if (data.target.epoch == get_current_epoch(state)) {
    val justified_checkpoint = state.current_justified_checkpoint
    justified_checkpoint_2 = justified_checkpoint
  } else {
    val justified_checkpoint_1 = state.previous_justified_checkpoint
    justified_checkpoint_2 = justified_checkpoint_1
  }
  val is_matching_source = data.source == justified_checkpoint_2
  val is_matching_target = is_matching_source && (data.target.root == get_block_root(state, data.target.epoch))
  val is_matching_head = is_matching_target && (data.beacon_block_root == get_block_root_at_slot(state, data.slot))
  assert(is_matching_source)
  val participation_flag_indices = PyList<pyint>()
  if (is_matching_source && (inclusion_delay <= integer_squareroot(SLOTS_PER_EPOCH))) {
    participation_flag_indices.append(TIMELY_SOURCE_FLAG_INDEX)
  }
  if (is_matching_target && (inclusion_delay <= SLOTS_PER_EPOCH)) {
    participation_flag_indices.append(TIMELY_TARGET_FLAG_INDEX)
  }
  if (is_matching_head && (inclusion_delay == MIN_ATTESTATION_INCLUSION_DELAY)) {
    participation_flag_indices.append(TIMELY_HEAD_FLAG_INDEX)
  }
  return participation_flag_indices
}

/*
    Return the deltas for a given ``flag_index`` by scanning through the participation flags.
    */
fun get_flag_index_deltas(state: BeaconState, flag_index: pyint): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  val rewards = PyList(Gwei(0uL)) * len(state.validators)
  val penalties = PyList(Gwei(0uL)) * len(state.validators)
  val previous_epoch = get_previous_epoch(state)
  val unslashed_participating_indices = get_unslashed_participating_indices(state, flag_index, previous_epoch)
  val weight = PARTICIPATION_FLAG_WEIGHTS[uint64(flag_index)]
  val unslashed_participating_balance = get_total_balance(state, unslashed_participating_indices)
  val unslashed_participating_increments = unslashed_participating_balance / EFFECTIVE_BALANCE_INCREMENT
  val active_increments = get_total_active_balance(state) / EFFECTIVE_BALANCE_INCREMENT
  for (index in get_eligible_validator_indices(state)) {
    val base_reward = get_base_reward(state, index)
    if (index in unslashed_participating_indices) {
      if (!is_in_inactivity_leak(state)) {
        val reward_numerator = base_reward * weight * unslashed_participating_increments
        rewards[index] = rewards[index] + Gwei(reward_numerator / (active_increments * WEIGHT_DENOMINATOR))
      }
    } else {
      if (flag_index != TIMELY_HEAD_FLAG_INDEX) {
        penalties[index] = penalties[index] + Gwei(base_reward * weight / WEIGHT_DENOMINATOR)
      }
    }
  }
  return Pair(rewards, penalties)
}

/*
    Return the inactivity penalty deltas by considering timely target participation flags and inactivity scores.
    */
fun get_inactivity_penalty_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  val rewards = list(range(len(state.validators)).map { Gwei(0uL) })
  val penalties = list(range(len(state.validators)).map { Gwei(0uL) })
  val previous_epoch = get_previous_epoch(state)
  val matching_target_indices = get_unslashed_participating_indices(state, TIMELY_TARGET_FLAG_INDEX, previous_epoch)
  for (index in get_eligible_validator_indices(state)) {
    if (index !in matching_target_indices) {
      val penalty_numerator = state.validators[index].effective_balance * state.inactivity_scores[index]
      val penalty_denominator = INACTIVITY_SCORE_BIAS * INACTIVITY_PENALTY_QUOTIENT_ALTAIR
      penalties[index] = penalties[index] + Gwei(penalty_numerator / penalty_denominator)
    }
  }
  return Pair(rewards, penalties)
}

/*
    Slash the validator with index ``slashed_index``.
    */
fun slash_validator(state: BeaconState, slashed_index: ValidatorIndex, whistleblower_index: ValidatorIndex? = null) {
  val epoch = get_current_epoch(state)
  initiate_validator_exit(state, slashed_index)
  val validator = state.validators[slashed_index]
  validator.slashed = true
  validator.withdrawable_epoch = max(validator.withdrawable_epoch, Epoch(epoch + EPOCHS_PER_SLASHINGS_VECTOR))
  state.slashings[epoch % EPOCHS_PER_SLASHINGS_VECTOR] = state.slashings[epoch % EPOCHS_PER_SLASHINGS_VECTOR] + validator.effective_balance
  decrease_balance(state, slashed_index, validator.effective_balance / MIN_SLASHING_PENALTY_QUOTIENT_ALTAIR)
  val proposer_index = get_beacon_proposer_index(state)
  val whistleblower_index_2: ValidatorIndex
  if (whistleblower_index == null) {
    val whistleblower_index_1 = proposer_index
    whistleblower_index_2 = whistleblower_index_1
  } else {
    whistleblower_index_2 = whistleblower_index
  }
  val whistleblower_reward = Gwei(validator.effective_balance / WHISTLEBLOWER_REWARD_QUOTIENT)
  val proposer_reward = Gwei(whistleblower_reward * PROPOSER_WEIGHT / WEIGHT_DENOMINATOR)
  increase_balance(state, proposer_index, proposer_reward)
  increase_balance(state, whistleblower_index_2, Gwei(whistleblower_reward - proposer_reward))
}

fun process_block(state: BeaconState, block: BeaconBlock) {
  process_block_header(state, block)
  process_randao(state, block.body)
  process_eth1_data(state, block.body)
  process_operations(state, block.body)
  process_sync_aggregate(state, block.body.sync_aggregate)
}

fun process_attestation(state: BeaconState, attestation: Attestation) {
  val data = attestation.data
  assert(data.target.epoch in Pair(get_previous_epoch(state), get_current_epoch(state)))
  assert(data.target.epoch == compute_epoch_at_slot(data.slot))
  assert(((data.slot + MIN_ATTESTATION_INCLUSION_DELAY) <= state.slot) && (state.slot <= (data.slot + SLOTS_PER_EPOCH)))
  assert(data.index < get_committee_count_per_slot(state, data.target.epoch))
  val committee = get_beacon_committee(state, data.slot, data.index)
  assert(len(attestation.aggregation_bits) == len(committee))
  val participation_flag_indices = get_attestation_participation_flag_indices(state, data, state.slot - data.slot)
  assert(is_valid_indexed_attestation(state, get_indexed_attestation(state, attestation)))
  val epoch_participation_2: SSZList<ParticipationFlags>
  if (data.target.epoch == get_current_epoch(state)) {
    val epoch_participation = state.current_epoch_participation
    epoch_participation_2 = epoch_participation
  } else {
    val epoch_participation_1 = state.previous_epoch_participation
    epoch_participation_2 = epoch_participation_1
  }
  val proposer_reward_numerator = 0uL
  var proposer_reward_numerator_2 = proposer_reward_numerator
  for (index in get_attesting_indices(state, data, attestation.aggregation_bits)) {
    var proposer_reward_numerator_3 = proposer_reward_numerator_2
    for ((flag_index, weight) in enumerate(PARTICIPATION_FLAG_WEIGHTS)) {
      if ((pyint(flag_index) in participation_flag_indices) && !has_flag(epoch_participation_2[index], pyint(flag_index))) {
        epoch_participation_2[index] = add_flag(epoch_participation_2[index], pyint(flag_index))
        val proposer_reward_numerator_1 = proposer_reward_numerator_3 + (get_base_reward(state, index) * weight)
        proposer_reward_numerator_3 = proposer_reward_numerator_1
      } else {
        proposer_reward_numerator_3 = proposer_reward_numerator_3
      }
    }
    proposer_reward_numerator_2 = proposer_reward_numerator_3
  }
  val proposer_reward_denominator = (WEIGHT_DENOMINATOR - PROPOSER_WEIGHT) * WEIGHT_DENOMINATOR / PROPOSER_WEIGHT
  val proposer_reward = Gwei(proposer_reward_numerator_2 / proposer_reward_denominator)
  increase_balance(state, get_beacon_proposer_index(state), proposer_reward)
}

fun process_deposit(state: BeaconState, deposit: Deposit) {
  assert(is_valid_merkle_branch(leaf = hash_tree_root(deposit.data), branch = deposit.proof, depth = DEPOSIT_CONTRACT_TREE_DEPTH + 1uL, index = state.eth1_deposit_index, root = state.eth1_data.deposit_root))
  state.eth1_deposit_index = state.eth1_deposit_index + 1uL
  val pubkey = deposit.data.pubkey
  val amount = deposit.data.amount
  val validator_pubkeys = list(state.validators.map { validator -> validator.pubkey })
  if (pubkey !in validator_pubkeys) {
    val deposit_message = DepositMessage(pubkey = deposit.data.pubkey, withdrawal_credentials = deposit.data.withdrawal_credentials, amount = deposit.data.amount)
    val domain = compute_domain(DOMAIN_DEPOSIT)
    val signing_root = compute_signing_root(deposit_message, domain)
    if (bls.Verify(pubkey, signing_root, deposit.data.signature)) {
      state.validators.append(get_validator_from_deposit(state, deposit))
      state.balances.append(amount)
      state.previous_epoch_participation.append(ParticipationFlags(0uL))
      state.current_epoch_participation.append(ParticipationFlags(0uL))
      state.inactivity_scores.append(uint64(0uL))
    }
  } else {
    val index = ValidatorIndex(validator_pubkeys.index(pubkey))
    increase_balance(state, index, amount)
  }
}

fun process_sync_aggregate(state: BeaconState, sync_aggregate: SyncAggregate) {
  val committee_pubkeys = state.current_sync_committee.pubkeys
  val participant_pubkeys = list(zip(committee_pubkeys, sync_aggregate.sync_committee_bits).filter { (pubkey, bit) -> bit }.map { (pubkey, bit) -> pubkey })
  val previous_slot = max(state.slot, Slot(1uL)) - Slot(1uL)
  val domain = get_domain(state, DOMAIN_SYNC_COMMITTEE, compute_epoch_at_slot(previous_slot))
  val signing_root = compute_signing_root(get_block_root_at_slot(state, previous_slot), domain)
  assert(eth_fast_aggregate_verify(participant_pubkeys, signing_root, sync_aggregate.sync_committee_signature))
  val total_active_increments = get_total_active_balance(state) / EFFECTIVE_BALANCE_INCREMENT
  val total_base_rewards = Gwei(get_base_reward_per_increment(state) * total_active_increments)
  val max_participant_rewards = Gwei(total_base_rewards * SYNC_REWARD_WEIGHT / WEIGHT_DENOMINATOR / SLOTS_PER_EPOCH)
  val participant_reward = Gwei(max_participant_rewards / SYNC_COMMITTEE_SIZE)
  val proposer_reward = Gwei(participant_reward * PROPOSER_WEIGHT / (WEIGHT_DENOMINATOR - PROPOSER_WEIGHT))
  val all_pubkeys = list(state.validators.map { v -> v.pubkey })
  val committee_indices = list(state.current_sync_committee.pubkeys.map { pubkey -> ValidatorIndex(all_pubkeys.index(pubkey)) })
  for ((participant_index, participation_bit) in zip(committee_indices, sync_aggregate.sync_committee_bits)) {
    if (pybool(participation_bit)) {
      increase_balance(state, participant_index, participant_reward)
      increase_balance(state, get_beacon_proposer_index(state), proposer_reward)
    } else {
      decrease_balance(state, participant_index, participant_reward)
    }
  }
}

fun process_epoch(state: BeaconState) {
  process_justification_and_finalization(state)
  process_inactivity_updates(state)
  process_rewards_and_penalties(state)
  process_registry_updates(state)
  process_slashings(state)
  process_eth1_data_reset(state)
  process_effective_balance_updates(state)
  process_slashings_reset(state)
  process_randao_mixes_reset(state)
  process_historical_roots_update(state)
  process_participation_flag_updates(state)
  process_sync_committee_updates(state)
}

fun process_justification_and_finalization(state: BeaconState) {
  if (get_current_epoch(state) <= (GENESIS_EPOCH + 1uL)) {
    return
  }
  val previous_indices = get_unslashed_participating_indices(state, TIMELY_TARGET_FLAG_INDEX, get_previous_epoch(state))
  val current_indices = get_unslashed_participating_indices(state, TIMELY_TARGET_FLAG_INDEX, get_current_epoch(state))
  val total_active_balance = get_total_active_balance(state)
  val previous_target_balance = get_total_balance(state, previous_indices)
  val current_target_balance = get_total_balance(state, current_indices)
  weigh_justification_and_finalization(state, total_active_balance, previous_target_balance, current_target_balance)
}

fun process_inactivity_updates(state: BeaconState) {
  if (get_current_epoch(state) == GENESIS_EPOCH) {
    return
  }
  for (index in get_eligible_validator_indices(state)) {
    if (index in get_unslashed_participating_indices(state, TIMELY_TARGET_FLAG_INDEX, get_previous_epoch(state))) {
      state.inactivity_scores[index] = state.inactivity_scores[index] - min(1uL, state.inactivity_scores[index])
    } else {
      state.inactivity_scores[index] = state.inactivity_scores[index] + INACTIVITY_SCORE_BIAS
    }
    if (!is_in_inactivity_leak(state)) {
      state.inactivity_scores[index] = state.inactivity_scores[index] - min(INACTIVITY_SCORE_RECOVERY_RATE, state.inactivity_scores[index])
    }
  }
}

fun process_rewards_and_penalties(state: BeaconState) {
  if (get_current_epoch(state) == GENESIS_EPOCH) {
    return
  }
  val flag_deltas = list(range(len(PARTICIPATION_FLAG_WEIGHTS)).map { flag_index: uint64 -> get_flag_index_deltas(state, pyint(flag_index)) })
  val deltas = flag_deltas + PyList(get_inactivity_penalty_deltas(state))
  for ((rewards, penalties) in deltas) {
    for (index in range(len(state.validators))) {
      increase_balance(state, ValidatorIndex(index), rewards[index])
      decrease_balance(state, ValidatorIndex(index), penalties[index])
    }
  }
}

fun process_slashings(state: BeaconState) {
  val epoch = get_current_epoch(state)
  val total_balance = get_total_active_balance(state)
  val adjusted_total_slashing_balance = min(sum(state.slashings) * PROPORTIONAL_SLASHING_MULTIPLIER_ALTAIR, total_balance)
  for ((index, validator) in enumerate(state.validators)) {
    if (validator.slashed && ((epoch + (EPOCHS_PER_SLASHINGS_VECTOR / 2uL)) == validator.withdrawable_epoch)) {
      val increment = EFFECTIVE_BALANCE_INCREMENT
      val penalty_numerator = validator.effective_balance / increment * adjusted_total_slashing_balance
      val penalty = penalty_numerator / total_balance * increment
      decrease_balance(state, ValidatorIndex(index), penalty)
    }
  }
}

fun process_participation_flag_updates(state: BeaconState) {
  state.previous_epoch_participation = state.current_epoch_participation
  state.current_epoch_participation = SSZList<ParticipationFlags>(list(range(len(state.validators)).map { _ -> ParticipationFlags(0uL) }))
}

fun process_sync_committee_updates(state: BeaconState) {
  val next_epoch = get_current_epoch(state) + Epoch(1uL)
  if ((next_epoch % EPOCHS_PER_SYNC_COMMITTEE_PERIOD) == 0uL) {
    state.current_sync_committee = state.next_sync_committee
    state.next_sync_committee = get_next_sync_committee(state)
  }
}

fun initialize_beacon_state_from_eth1(eth1_block_hash: Bytes32, eth1_timestamp: uint64, deposits: Sequence<Deposit>): BeaconState {
  val fork = Fork(previous_version = GENESIS_FORK_VERSION, current_version = ALTAIR_FORK_VERSION, epoch = GENESIS_EPOCH)
  val state = BeaconState(genesis_time = eth1_timestamp + GENESIS_DELAY, fork = fork, eth1_data = Eth1Data(block_hash = eth1_block_hash, deposit_count = uint64(len(deposits))), latest_block_header = BeaconBlockHeader(body_root = hash_tree_root(BeaconBlockBody())), randao_mixes = PyList(eth1_block_hash) * EPOCHS_PER_HISTORICAL_VECTOR)
  val leaves = list(map({ deposit -> deposit.data }, deposits))
  for ((index, deposit) in enumerate(deposits)) {
    val deposit_data_list = SSZList<DepositData>(*leaves[0uL until (index + 1uL)].toTypedArray())
    state.eth1_data.deposit_root = hash_tree_root(deposit_data_list)
    process_deposit(state, deposit)
  }
  for ((index_1, validator) in enumerate(state.validators)) {
    val balance = state.balances[index_1]
    validator.effective_balance = min(balance - (balance % EFFECTIVE_BALANCE_INCREMENT), MAX_EFFECTIVE_BALANCE)
    if (validator.effective_balance == MAX_EFFECTIVE_BALANCE) {
      validator.activation_eligibility_epoch = GENESIS_EPOCH
      validator.activation_epoch = GENESIS_EPOCH
    }
  }
  state.genesis_validators_root = hash_tree_root(state.validators)
  state.current_sync_committee = get_next_sync_committee(state)
  state.next_sync_committee = get_next_sync_committee(state)
  return state
}

/*
    Return the aggregate public key for the public keys in ``pubkeys``.

    NOTE: the ``+`` operation should be interpreted as elliptic curve point addition, which takes as input
    elliptic curve points that must be decoded from the input ``BLSPubkey``s.
    This implementation is for demonstrative purposes only and ignores encoding/decoding concerns.
    Refer to the BLS signature draft standard for more information.
    */
fun eth_aggregate_pubkeys(pubkeys: Sequence<BLSPubkey>): BLSPubkey {
  assert(len(pubkeys) > 0uL)
  assert(all(pubkeys.map { pubkey -> bls.KeyValidate(pubkey) }))
  var result = copy(pubkeys[0uL])
  for (pubkey in pubkeys[1uL until len(pubkeys)]) {
    result = bls_plus(result, pubkey)
  }
  return result
}

/*
    Wrapper to ``bls.FastAggregateVerify`` accepting the ``G2_POINT_AT_INFINITY`` signature when ``pubkeys`` is empty.
    */
fun eth_fast_aggregate_verify(pubkeys: Sequence<BLSPubkey>, message: Bytes32, signature: BLSSignature): pybool {
  if ((len(pubkeys) == 0uL) && (signature == G2_POINT_AT_INFINITY)) {
    return true
  }
  return bls.FastAggregateVerify(pubkeys, message, signature)
}

fun translate_participation(state: BeaconState, pending_attestations: Sequence<PendingAttestation>) {
  for (attestation in pending_attestations) {
    val data = attestation.data
    val inclusion_delay = attestation.inclusion_delay
    val participation_flag_indices = get_attestation_participation_flag_indices(state, data, inclusion_delay)
    val epoch_participation = state.previous_epoch_participation
    for (index in get_attesting_indices(state, data, attestation.aggregation_bits)) {
      for (flag_index in participation_flag_indices) {
        epoch_participation[index] = add_flag(epoch_participation[index], flag_index)
      }
    }
  }
}

fun upgrade_to_altair(pre: phase0.BeaconState): BeaconState {
  val epoch = get_current_epoch(pre)
  val post = BeaconState(genesis_time = pre.genesis_time, genesis_validators_root = pre.genesis_validators_root, slot = pre.slot, fork = Fork(previous_version = pre.fork.current_version, current_version = ALTAIR_FORK_VERSION, epoch = epoch), latest_block_header = pre.latest_block_header, block_roots = pre.block_roots, state_roots = pre.state_roots, historical_roots = pre.historical_roots, eth1_data = pre.eth1_data, eth1_data_votes = pre.eth1_data_votes, eth1_deposit_index = pre.eth1_deposit_index, validators = pre.validators, balances = pre.balances, randao_mixes = pre.randao_mixes, slashings = pre.slashings, previous_epoch_participation = list(range(len(pre.validators)).map { ParticipationFlags(0uL) }), current_epoch_participation = list(range(len(pre.validators)).map { ParticipationFlags(0uL) }), justification_bits = pre.justification_bits, previous_justified_checkpoint = pre.previous_justified_checkpoint, current_justified_checkpoint = pre.current_justified_checkpoint, finalized_checkpoint = pre.finalized_checkpoint, inactivity_scores = list(range(len(pre.validators)).map { _ -> uint64(0uL) }))
  translate_participation(post, pre.previous_epoch_attestations)
  post.current_sync_committee = get_next_sync_committee(post)
  post.next_sync_committee = get_next_sync_committee(post)
  return post
}

fun compute_sync_committee_period(epoch: Epoch): uint64 {
  return epoch / EPOCHS_PER_SYNC_COMMITTEE_PERIOD
}

fun is_assigned_to_sync_committee(state: BeaconState, epoch: Epoch, validator_index: ValidatorIndex): pybool {
  val sync_committee_period = compute_sync_committee_period(epoch)
  val current_epoch = get_current_epoch(state)
  val current_sync_committee_period = compute_sync_committee_period(current_epoch)
  val next_sync_committee_period = current_sync_committee_period + 1uL
  assert(sync_committee_period in Pair(current_sync_committee_period, next_sync_committee_period))
  val pubkey = state.validators[validator_index].pubkey
  if (sync_committee_period == current_sync_committee_period) {
    return pubkey in state.current_sync_committee.pubkeys
  } else {
    return pubkey in state.next_sync_committee.pubkeys
  }
}

fun process_sync_committee_contributions(block: BeaconBlock, contributions: Set<SyncCommitteeContribution>) {
  val sync_aggregate = SyncAggregate()
  val signatures = PyList<BLSSignature>()
  val sync_subcommittee_size = SYNC_COMMITTEE_SIZE / SYNC_COMMITTEE_SUBNET_COUNT
  for (contribution in contributions) {
    val subcommittee_index = contribution.subcommittee_index
    for ((index, participated) in enumerate(contribution.aggregation_bits)) {
      if (pybool(participated)) {
        val participant_index = (sync_subcommittee_size * subcommittee_index) + index
        sync_aggregate.sync_committee_bits[participant_index] = true
      }
    }
    signatures.append(contribution.signature)
  }
  sync_aggregate.sync_committee_signature = BLSSignature(bls.Aggregate(signatures))
  block.body.sync_aggregate = sync_aggregate
}

fun get_sync_committee_message(state: BeaconState, block_root: Root, validator_index: ValidatorIndex, privkey: pyint): SyncCommitteeMessage {
  val epoch = get_current_epoch(state)
  val domain = get_domain(state, DOMAIN_SYNC_COMMITTEE, epoch)
  val signing_root = compute_signing_root(block_root, domain)
  val signature = bls.Sign(privkey, signing_root)
  return SyncCommitteeMessage(slot = state.slot, beacon_block_root = block_root, validator_index = validator_index, signature = signature)
}

fun compute_subnets_for_sync_committee(state: BeaconState, validator_index: ValidatorIndex): Set<uint64> {
  val next_slot_epoch = compute_epoch_at_slot(Slot(state.slot + 1uL))
  val sync_committee_2: SyncCommittee
  if (compute_sync_committee_period(get_current_epoch(state)) == compute_sync_committee_period(next_slot_epoch)) {
    val sync_committee = state.current_sync_committee
    sync_committee_2 = sync_committee
  } else {
    val sync_committee_1 = state.next_sync_committee
    sync_committee_2 = sync_committee_1
  }
  val target_pubkey = state.validators[validator_index].pubkey
  val sync_committee_indices = list(enumerate(sync_committee_2.pubkeys).filter { (index, pubkey) -> pubkey == target_pubkey }.map { (index, pubkey) -> index })
  return set(list(sync_committee_indices.map { index -> uint64(index / (SYNC_COMMITTEE_SIZE / SYNC_COMMITTEE_SUBNET_COUNT)) }))
}

fun get_sync_committee_selection_proof(state: BeaconState, slot: Slot, subcommittee_index: uint64, privkey: pyint): BLSSignature {
  val domain = get_domain(state, DOMAIN_SYNC_COMMITTEE_SELECTION_PROOF, compute_epoch_at_slot(slot))
  val signing_data = SyncAggregatorSelectionData(slot = slot, subcommittee_index = subcommittee_index)
  val signing_root = compute_signing_root(signing_data, domain)
  return bls.Sign(privkey, signing_root)
}

fun is_sync_committee_aggregator(signature: BLSSignature): pybool {
  val modulo = max(1uL, SYNC_COMMITTEE_SIZE / SYNC_COMMITTEE_SUBNET_COUNT / TARGET_AGGREGATORS_PER_SYNC_SUBCOMMITTEE)
  return (bytes_to_uint64(hash(signature)[0uL until 8uL]) % modulo) == 0uL
}

fun get_contribution_and_proof(state: BeaconState, aggregator_index: ValidatorIndex, contribution: SyncCommitteeContribution, privkey: pyint): ContributionAndProof {
  val selection_proof = get_sync_committee_selection_proof(state, contribution.slot, contribution.subcommittee_index, privkey)
  return ContributionAndProof(aggregator_index = aggregator_index, contribution = contribution, selection_proof = selection_proof)
}

fun get_contribution_and_proof_signature(state: BeaconState, contribution_and_proof: ContributionAndProof, privkey: pyint): BLSSignature {
  val contribution = contribution_and_proof.contribution
  val domain = get_domain(state, DOMAIN_CONTRIBUTION_AND_PROOF, compute_epoch_at_slot(contribution.slot))
  val signing_root = compute_signing_root(contribution_and_proof, domain)
  return bls.Sign(privkey, signing_root)
}

fun get_subtree_index(generalized_index: GeneralizedIndex): uint64 {
  return uint64(generalized_index % 2uL.pow(floorlog2(generalized_index)))
}

fun validate_light_client_update(snapshot: LightClientSnapshot, update: LightClientUpdate, genesis_validators_root: Root) {
  assert(update.header.slot > snapshot.header.slot)
  val snapshot_period = compute_epoch_at_slot(snapshot.header.slot) / EPOCHS_PER_SYNC_COMMITTEE_PERIOD
  val update_period = compute_epoch_at_slot(update.header.slot) / EPOCHS_PER_SYNC_COMMITTEE_PERIOD
  assert(update_period in Pair(snapshot_period, snapshot_period + 1uL))
  val signed_header_2: BeaconBlockHeader
  if (update.finality_header == BeaconBlockHeader()) {
    val signed_header = update.header
    assert(update.finality_branch == list(range(floorlog2(FINALIZED_ROOT_INDEX)).map { _ -> Bytes32() }))
    signed_header_2 = signed_header
  } else {
    val signed_header_1 = update.finality_header
    assert(is_valid_merkle_branch(leaf = hash_tree_root(update.header), branch = update.finality_branch, depth = floorlog2(FINALIZED_ROOT_INDEX), index = get_subtree_index(FINALIZED_ROOT_INDEX), root = update.finality_header.state_root))
    signed_header_2 = signed_header_1
  }
  val sync_committee_2: SyncCommittee
  if (update_period == snapshot_period) {
    val sync_committee = snapshot.current_sync_committee
    assert(update.next_sync_committee_branch == list(range(floorlog2(NEXT_SYNC_COMMITTEE_INDEX)).map { _ -> Bytes32() }))
    sync_committee_2 = sync_committee
  } else {
    val sync_committee_1 = snapshot.next_sync_committee
    assert(is_valid_merkle_branch(leaf = hash_tree_root(update.next_sync_committee), branch = update.next_sync_committee_branch, depth = floorlog2(NEXT_SYNC_COMMITTEE_INDEX), index = get_subtree_index(NEXT_SYNC_COMMITTEE_INDEX), root = update.header.state_root))
    sync_committee_2 = sync_committee_1
  }
  assert(sum(update.sync_committee_bits) >= MIN_SYNC_COMMITTEE_PARTICIPANTS)
  val participant_pubkeys = list(zip(update.sync_committee_bits, sync_committee_2.pubkeys).filter { (bit, pubkey) -> bit }.map { (bit, pubkey) -> pubkey })
  val domain = compute_domain(DOMAIN_SYNC_COMMITTEE, update.fork_version, genesis_validators_root)
  val signing_root = compute_signing_root(signed_header_2, domain)
  assert(bls.FastAggregateVerify(participant_pubkeys, signing_root, update.sync_committee_signature))
}

fun apply_light_client_update(snapshot: LightClientSnapshot, update: LightClientUpdate) {
  val snapshot_period = compute_epoch_at_slot(snapshot.header.slot) / EPOCHS_PER_SYNC_COMMITTEE_PERIOD
  val update_period = compute_epoch_at_slot(update.header.slot) / EPOCHS_PER_SYNC_COMMITTEE_PERIOD
  if (update_period == (snapshot_period + 1uL)) {
    snapshot.current_sync_committee = snapshot.next_sync_committee
    snapshot.next_sync_committee = update.next_sync_committee
  }
  snapshot.header = update.header
}

fun process_light_client_update(store: LightClientStore, update: LightClientUpdate, current_slot: Slot, genesis_validators_root: Root) {
  validate_light_client_update(store.snapshot, update, genesis_validators_root)
  store.valid_updates.add(update)
  val update_timeout = SLOTS_PER_EPOCH * EPOCHS_PER_SYNC_COMMITTEE_PERIOD
  if (((sum(update.sync_committee_bits) * 3uL) >= (len(update.sync_committee_bits) * 2uL)) && (update.finality_header != BeaconBlockHeader())) {
    apply_light_client_update(store.snapshot, update)
    store.valid_updates = mutableSetOf<LightClientUpdate>()
  } else {
    if (current_slot > (store.snapshot.header.slot + update_timeout)) {
      apply_light_client_update(store.snapshot, max(store.valid_updates, key = { update -> sum(update.sync_committee_bits) }))
      store.valid_updates = mutableSetOf<LightClientUpdate>()
    }
  }
}

/*
    Check if ``validator`` is eligible for activation.
    */
fun is_eligible_for_activation(state: BeaconState, validator: Validator): pybool {
  return (validator.activation_eligibility_epoch <= state.finalized_checkpoint.epoch) && (validator.activation_epoch == FAR_FUTURE_EPOCH)
}

/*
    Check if ``indexed_attestation`` is not empty, has sorted and unique indices and has a valid aggregate signature.
    */
fun is_valid_indexed_attestation(state: BeaconState, indexed_attestation: IndexedAttestation): pybool {
  val indices = indexed_attestation.attesting_indices
  if ((len(indices) == 0uL) || !(indices == sorted(set(indices)))) {
    return false
  }
  val pubkeys = list(indices.map { i -> state.validators[i].pubkey })
  val domain = get_domain(state, DOMAIN_BEACON_ATTESTER, indexed_attestation.data.target.epoch)
  val signing_root = compute_signing_root(indexed_attestation.data, domain)
  return bls.FastAggregateVerify(pubkeys, signing_root, indexed_attestation.signature)
}

/*
    Return from ``indices`` a random index sampled by effective balance.
    */
fun compute_proposer_index(state: BeaconState, indices: Sequence<ValidatorIndex>, seed: Bytes32): ValidatorIndex {
  assert(len(indices) > 0uL)
  val MAX_RANDOM_BYTE = 2uL.pow(8uL) - 1uL
  val i = uint64(0uL)
  val total = uint64(len(indices))
  var i_2 = i
  while (true) {
    val candidate_index = indices[compute_shuffled_index(i_2 % total, total, seed)]
    val random_byte = hash(seed + uint_to_bytes(uint64(i_2 / 32uL)))[i_2 % 32uL]
    val effective_balance = state.validators[candidate_index].effective_balance
    if ((effective_balance * MAX_RANDOM_BYTE) >= (MAX_EFFECTIVE_BALANCE * uint8(random_byte))) {
      return candidate_index
    }
    val i_1 = i_2 + 1uL
    i_2 = i_1
  }
}

/*
    Return the current epoch.
    */
fun get_current_epoch(state: BeaconState): Epoch {
  return compute_epoch_at_slot(state.slot)
}

/*`
    Return the previous epoch (unless the current epoch is ``GENESIS_EPOCH``).
    */
fun get_previous_epoch(state: BeaconState): Epoch {
  val current_epoch = get_current_epoch(state)
  return if (current_epoch == GENESIS_EPOCH) GENESIS_EPOCH else Epoch(current_epoch - 1uL)
}

/*
    Return the block root at the start of a recent ``epoch``.
    */
fun get_block_root(state: BeaconState, epoch: Epoch): Root {
  return get_block_root_at_slot(state, compute_start_slot_at_epoch(epoch))
}

/*
    Return the block root at a recent ``slot``.
    */
fun get_block_root_at_slot(state: BeaconState, slot: Slot): Root {
  assert((slot < state.slot) && (state.slot <= (slot + SLOTS_PER_HISTORICAL_ROOT)))
  return state.block_roots[slot % SLOTS_PER_HISTORICAL_ROOT]
}

/*
    Return the randao mix at a recent ``epoch``.
    */
fun get_randao_mix(state: BeaconState, epoch: Epoch): Bytes32 {
  return state.randao_mixes[epoch % EPOCHS_PER_HISTORICAL_VECTOR]
}

/*
    Return the sequence of active validator indices at ``epoch``.
    */
fun get_active_validator_indices(state: BeaconState, epoch: Epoch): Sequence<ValidatorIndex> {
  return list(enumerate(state.validators).filter { (i, v) -> is_active_validator(v, epoch) }.map { (i, v) -> ValidatorIndex(i) })
}

/*
    Return the validator churn limit for the current epoch.
    */
fun get_validator_churn_limit(state: BeaconState): uint64 {
  val active_validator_indices = get_active_validator_indices(state, get_current_epoch(state))
  return max(MIN_PER_EPOCH_CHURN_LIMIT, uint64(len(active_validator_indices)) / CHURN_LIMIT_QUOTIENT)
}

/*
    Return the seed at ``epoch``.
    */
fun get_seed(state: BeaconState, epoch: Epoch, domain_type: DomainType): Bytes32 {
  val mix = get_randao_mix(state, Epoch(epoch + EPOCHS_PER_HISTORICAL_VECTOR - MIN_SEED_LOOKAHEAD - 1uL))
  return hash(domain_type + uint_to_bytes(epoch) + mix)
}

/*
    Return the number of committees in each slot for the given ``epoch``.
    */
fun get_committee_count_per_slot(state: BeaconState, epoch: Epoch): uint64 {
  return max(uint64(1uL), min(MAX_COMMITTEES_PER_SLOT, uint64(len(get_active_validator_indices(state, epoch))) / SLOTS_PER_EPOCH / TARGET_COMMITTEE_SIZE))
}

/*
    Return the beacon committee at ``slot`` for ``index``.
    */
fun get_beacon_committee(state: BeaconState, slot: Slot, index: CommitteeIndex): Sequence<ValidatorIndex> {
  val epoch = compute_epoch_at_slot(slot)
  val committees_per_slot = get_committee_count_per_slot(state, epoch)
  return compute_committee(indices = get_active_validator_indices(state, epoch), seed = get_seed(state, epoch, DOMAIN_BEACON_ATTESTER), index = (slot % SLOTS_PER_EPOCH * committees_per_slot) + index, count = committees_per_slot * SLOTS_PER_EPOCH)
}

/*
    Return the beacon proposer index at the current slot.
    */
fun get_beacon_proposer_index(state: BeaconState): ValidatorIndex {
  val epoch = get_current_epoch(state)
  val seed = hash(get_seed(state, epoch, DOMAIN_BEACON_PROPOSER) + uint_to_bytes(state.slot))
  val indices = get_active_validator_indices(state, epoch)
  return compute_proposer_index(state, indices, seed)
}

/*
    Return the combined effective balance of the ``indices``.
    ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
    Math safe up to ~10B ETH, afterwhich this overflows uint64.
    */
fun get_total_balance(state: BeaconState, indices: Set<ValidatorIndex>): Gwei {
  return Gwei(max(EFFECTIVE_BALANCE_INCREMENT, sum(list(indices.map { index -> state.validators[index].effective_balance }))))
}

/*
    Return the combined effective balance of the active validators.
    Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
    */
fun get_total_active_balance(state: BeaconState): Gwei {
  return get_total_balance(state, set(get_active_validator_indices(state, get_current_epoch(state))))
}

/*
    Return the signature domain (fork version concatenated with domain type) of a message.
    */
fun get_domain(state: BeaconState, domain_type: DomainType, epoch: Epoch? = null): Domain {
  val epoch_1 = if (epoch == null) get_current_epoch(state) else epoch
  val fork_version = if (epoch_1 < state.fork.epoch) state.fork.previous_version else state.fork.current_version
  return compute_domain(domain_type, fork_version, state.genesis_validators_root)
}

/*
    Return the indexed attestation corresponding to ``attestation``.
    */
fun get_indexed_attestation(state: BeaconState, attestation: Attestation): IndexedAttestation {
  val attesting_indices = get_attesting_indices(state, attestation.data, attestation.aggregation_bits)
  return IndexedAttestation(attesting_indices = sorted(attesting_indices), data = attestation.data, signature = attestation.signature)
}

/*
    Return the set of attesting indices corresponding to ``data`` and ``bits``.
    */
fun get_attesting_indices(state: BeaconState, data: AttestationData, bits: SSZBitlist): Set<ValidatorIndex> {
  val committee = get_beacon_committee(state, data.slot, data.index)
  return set(enumerate(committee).filter { (i, index) -> bits[i] }.map { (i, index) -> index })
}

/*
    Increase the validator balance at index ``index`` by ``delta``.
    */
fun increase_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei) {
  state.balances[index] = state.balances[index] + delta
}

/*
    Decrease the validator balance at index ``index`` by ``delta``, with underflow protection.
    */
fun decrease_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei) {
  state.balances[index] = Gwei(if (delta > state.balances[index]) 0uL else state.balances[index] - delta)
}

/*
    Initiate the exit of the validator with index ``index``.
    */
fun initiate_validator_exit(state: BeaconState, index: ValidatorIndex) {
  val validator = state.validators[index]
  if (validator.exit_epoch != FAR_FUTURE_EPOCH) {
    return
  }
  val exit_epochs = list(state.validators.filter { v -> v.exit_epoch != FAR_FUTURE_EPOCH }.map { v -> v.exit_epoch })
  val exit_queue_epoch = max(exit_epochs + PyList(compute_activation_exit_epoch(get_current_epoch(state))))
  val exit_queue_churn = len(list(state.validators.filter { v -> v.exit_epoch == exit_queue_epoch }.map { v -> v }))
  val exit_queue_epoch_2: Epoch
  if (exit_queue_churn >= get_validator_churn_limit(state)) {
    val exit_queue_epoch_1 = exit_queue_epoch + Epoch(1uL)
    exit_queue_epoch_2 = exit_queue_epoch_1
  } else {
    exit_queue_epoch_2 = exit_queue_epoch
  }
  validator.exit_epoch = exit_queue_epoch_2
  validator.withdrawable_epoch = Epoch(validator.exit_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY)
}

fun weigh_justification_and_finalization(state: BeaconState, total_active_balance: Gwei, previous_epoch_target_balance: Gwei, current_epoch_target_balance: Gwei) {
  val previous_epoch = get_previous_epoch(state)
  val current_epoch = get_current_epoch(state)
  val old_previous_justified_checkpoint = state.previous_justified_checkpoint
  val old_current_justified_checkpoint = state.current_justified_checkpoint
  state.previous_justified_checkpoint = state.current_justified_checkpoint
  state.justification_bits[1uL until len(state.justification_bits)] = state.justification_bits[0uL until (JUSTIFICATION_BITS_LENGTH - 1uL)]
  state.justification_bits[0uL] = 0uL
  if ((previous_epoch_target_balance * 3uL) >= (total_active_balance * 2uL)) {
    state.current_justified_checkpoint = Checkpoint(epoch = previous_epoch, root = get_block_root(state, previous_epoch))
    state.justification_bits[1uL] = 1uL
  }
  if ((current_epoch_target_balance * 3uL) >= (total_active_balance * 2uL)) {
    state.current_justified_checkpoint = Checkpoint(epoch = current_epoch, root = get_block_root(state, current_epoch))
    state.justification_bits[0uL] = 1uL
  }
  val bits = state.justification_bits
  if (all(bits[1uL until 4uL]) && ((old_previous_justified_checkpoint.epoch + 3uL) == current_epoch)) {
    state.finalized_checkpoint = old_previous_justified_checkpoint
  }
  if (all(bits[1uL until 3uL]) && ((old_previous_justified_checkpoint.epoch + 2uL) == current_epoch)) {
    state.finalized_checkpoint = old_previous_justified_checkpoint
  }
  if (all(bits[0uL until 3uL]) && ((old_current_justified_checkpoint.epoch + 2uL) == current_epoch)) {
    state.finalized_checkpoint = old_current_justified_checkpoint
  }
  if (all(bits[0uL until 2uL]) && ((old_current_justified_checkpoint.epoch + 1uL) == current_epoch)) {
    state.finalized_checkpoint = old_current_justified_checkpoint
  }
}

fun get_finality_delay(state: BeaconState): uint64 {
  return get_previous_epoch(state) - state.finalized_checkpoint.epoch
}

fun is_in_inactivity_leak(state: BeaconState): pybool {
  return get_finality_delay(state) > MIN_EPOCHS_TO_INACTIVITY_PENALTY
}

fun get_eligible_validator_indices(state: BeaconState): Sequence<ValidatorIndex> {
  val previous_epoch = get_previous_epoch(state)
  return list(enumerate(state.validators).filter { (index, v) -> is_active_validator(v, previous_epoch) || (v.slashed && ((previous_epoch + 1uL) < v.withdrawable_epoch)) }.map { (index, v) -> ValidatorIndex(index) })
}

fun process_registry_updates(state: BeaconState) {
  for ((index, validator) in enumerate(state.validators)) {
    if (is_eligible_for_activation_queue(validator)) {
      validator.activation_eligibility_epoch = get_current_epoch(state) + 1uL
    }
    if (is_active_validator(validator, get_current_epoch(state)) && (validator.effective_balance <= EJECTION_BALANCE)) {
      initiate_validator_exit(state, ValidatorIndex(index))
    }
  }
  val activation_queue = sorted(list(enumerate(state.validators).filter { (index, validator) -> is_eligible_for_activation(state, validator) }.map { (index, validator) -> index }), key = { index -> Tuple2(state.validators[index].activation_eligibility_epoch, index) })
  for (index_1 in activation_queue[0uL until get_validator_churn_limit(state)]) {
    val validator_1 = state.validators[index_1]
    validator_1.activation_epoch = compute_activation_exit_epoch(get_current_epoch(state))
  }
}

fun process_eth1_data_reset(state: BeaconState) {
  val next_epoch = Epoch(get_current_epoch(state) + 1uL)
  if ((next_epoch % EPOCHS_PER_ETH1_VOTING_PERIOD) == 0uL) {
    state.eth1_data_votes = SSZList<Eth1Data>()
  }
}

fun process_effective_balance_updates(state: BeaconState) {
  for ((index, validator) in enumerate(state.validators)) {
    val balance = state.balances[index]
    val HYSTERESIS_INCREMENT = uint64(EFFECTIVE_BALANCE_INCREMENT / HYSTERESIS_QUOTIENT)
    val DOWNWARD_THRESHOLD = HYSTERESIS_INCREMENT * HYSTERESIS_DOWNWARD_MULTIPLIER
    val UPWARD_THRESHOLD = HYSTERESIS_INCREMENT * HYSTERESIS_UPWARD_MULTIPLIER
    if (((balance + DOWNWARD_THRESHOLD) < validator.effective_balance) || ((validator.effective_balance + UPWARD_THRESHOLD) < balance)) {
      validator.effective_balance = min(balance - (balance % EFFECTIVE_BALANCE_INCREMENT), MAX_EFFECTIVE_BALANCE)
    }
  }
}

fun process_slashings_reset(state: BeaconState) {
  val next_epoch = Epoch(get_current_epoch(state) + 1uL)
  state.slashings[next_epoch % EPOCHS_PER_SLASHINGS_VECTOR] = Gwei(0uL)
}

fun process_randao_mixes_reset(state: BeaconState) {
  val current_epoch = get_current_epoch(state)
  val next_epoch = Epoch(current_epoch + 1uL)
  state.randao_mixes[next_epoch % EPOCHS_PER_HISTORICAL_VECTOR] = get_randao_mix(state, current_epoch)
}

fun process_historical_roots_update(state: BeaconState) {
  val next_epoch = Epoch(get_current_epoch(state) + 1uL)
  if ((next_epoch % (SLOTS_PER_HISTORICAL_ROOT / SLOTS_PER_EPOCH)) == 0uL) {
    val historical_batch = HistoricalBatch(block_roots = state.block_roots, state_roots = state.state_roots)
    state.historical_roots.append(hash_tree_root(historical_batch))
  }
}

fun process_block_header(state: BeaconState, block: BeaconBlock) {
  assert(block.slot == state.slot)
  assert(block.slot > state.latest_block_header.slot)
  assert(block.proposer_index == get_beacon_proposer_index(state))
  assert(block.parent_root == hash_tree_root(state.latest_block_header))
  state.latest_block_header = BeaconBlockHeader(slot = block.slot, proposer_index = block.proposer_index, parent_root = block.parent_root, state_root = Bytes32(), body_root = hash_tree_root(block.body))
  val proposer = state.validators[block.proposer_index]
  assert(!proposer.slashed)
}

fun process_randao(state: BeaconState, body: BeaconBlockBody) {
  val epoch = get_current_epoch(state)
  val proposer = state.validators[get_beacon_proposer_index(state)]
  val signing_root = compute_signing_root(epoch, get_domain(state, DOMAIN_RANDAO))
  assert(bls.Verify(proposer.pubkey, signing_root, body.randao_reveal))
  val mix = xor(get_randao_mix(state, epoch), hash(body.randao_reveal))
  state.randao_mixes[epoch % EPOCHS_PER_HISTORICAL_VECTOR] = mix
}

fun process_eth1_data(state: BeaconState, body: BeaconBlockBody) {
  state.eth1_data_votes.append(body.eth1_data)
  if ((state.eth1_data_votes.count(body.eth1_data) * 2uL) > (EPOCHS_PER_ETH1_VOTING_PERIOD * SLOTS_PER_EPOCH)) {
    state.eth1_data = body.eth1_data
  }
}

fun process_operations(state: BeaconState, body: BeaconBlockBody) {
  assert(len(body.deposits) == min(MAX_DEPOSITS, state.eth1_data.deposit_count - state.eth1_deposit_index))
  for (operation in body.proposer_slashings) {
    process_proposer_slashing(state, operation)
  }
  for (operation_1 in body.attester_slashings) {
    process_attester_slashing(state, operation_1)
  }
  for (operation_2 in body.attestations) {
    process_attestation(state, operation_2)
  }
  for (operation_3 in body.deposits) {
    process_deposit(state, operation_3)
  }
  for (operation_4 in body.voluntary_exits) {
    process_voluntary_exit(state, operation_4)
  }
}

fun process_proposer_slashing(state: BeaconState, proposer_slashing: ProposerSlashing) {
  val header_1 = proposer_slashing.signed_header_1.message
  val header_2 = proposer_slashing.signed_header_2.message
  assert(header_1.slot == header_2.slot)
  assert(header_1.proposer_index == header_2.proposer_index)
  assert(header_1 != header_2)
  val proposer = state.validators[header_1.proposer_index]
  assert(is_slashable_validator(proposer, get_current_epoch(state)))
  for (signed_header in listOf(proposer_slashing.signed_header_1, proposer_slashing.signed_header_2)) {
    val domain = get_domain(state, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(signed_header.message.slot))
    val signing_root = compute_signing_root(signed_header.message, domain)
    assert(bls.Verify(proposer.pubkey, signing_root, signed_header.signature))
  }
  slash_validator(state, header_1.proposer_index)
}

fun process_attester_slashing(state: BeaconState, attester_slashing: AttesterSlashing) {
  val attestation_1 = attester_slashing.attestation_1
  val attestation_2 = attester_slashing.attestation_2
  assert(is_slashable_attestation_data(attestation_1.data, attestation_2.data))
  assert(is_valid_indexed_attestation(state, attestation_1))
  assert(is_valid_indexed_attestation(state, attestation_2))
  val slashed_any = false
  val indices = set(attestation_1.attesting_indices).intersection(attestation_2.attesting_indices)
  var slashed_any_2 = slashed_any
  for (index in sorted(indices)) {
    if (is_slashable_validator(state.validators[index], get_current_epoch(state))) {
      slash_validator(state, index)
      val slashed_any_1 = true
      slashed_any_2 = slashed_any_1
    } else {
      slashed_any_2 = slashed_any_2
    }
  }
  assert(slashed_any_2)
}

fun get_validator_from_deposit(state: BeaconState, deposit: Deposit): Validator {
  val amount = deposit.data.amount
  val effective_balance = min(amount - (amount % EFFECTIVE_BALANCE_INCREMENT), MAX_EFFECTIVE_BALANCE)
  return Validator(pubkey = deposit.data.pubkey, withdrawal_credentials = deposit.data.withdrawal_credentials, activation_eligibility_epoch = FAR_FUTURE_EPOCH, activation_epoch = FAR_FUTURE_EPOCH, exit_epoch = FAR_FUTURE_EPOCH, withdrawable_epoch = FAR_FUTURE_EPOCH, effective_balance = effective_balance)
}

fun process_voluntary_exit(state: BeaconState, signed_voluntary_exit: SignedVoluntaryExit) {
  val voluntary_exit = signed_voluntary_exit.message
  val validator = state.validators[voluntary_exit.validator_index]
  assert(is_active_validator(validator, get_current_epoch(state)))
  assert(validator.exit_epoch == FAR_FUTURE_EPOCH)
  assert(get_current_epoch(state) >= voluntary_exit.epoch)
  assert(get_current_epoch(state) >= (validator.activation_epoch + SHARD_COMMITTEE_PERIOD))
  val domain = get_domain(state, DOMAIN_VOLUNTARY_EXIT, voluntary_exit.epoch)
  val signing_root = compute_signing_root(voluntary_exit, domain)
  assert(bls.Verify(validator.pubkey, signing_root, signed_voluntary_exit.signature))
  initiate_validator_exit(state, voluntary_exit.validator_index)
}
