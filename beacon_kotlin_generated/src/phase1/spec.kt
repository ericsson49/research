package phase1

import deps.bls
import deps.hash
import deps.hash_tree_root
import pylib.PyDict
import pylib.PyList
import pylib.Tuple2
import pylib.all
import pylib.any
import pylib.append
import pylib.bit_length
import pylib.pybool
import pylib.contains
import pylib.count
import pylib.enumerate
import pylib.filter
import pylib.from_bytes
import pylib.get
import pylib.index
import pylib.intersection
import pylib.items
import pylib.join
import pylib.keys
import pylib.len
import pylib.list
import pylib.map
import pylib.max
import pylib.min
import pylib.plus
import pylib.pow
import pylib.pybytes
import pylib.pyint
import pylib.range
import pylib.set
import pylib.shl
import pylib.shr
import pylib.slice
import pylib.sorted
import pylib.sum
import pylib.times
import pylib.toPyDict
import pylib.toPyList
import pylib.to_bytes
import pylib.unaryMinus
import pylib.updateSlice
import pylib.zip
import ssz.Bytes
import ssz.Bytes32
import ssz.SSZBitlist
import ssz.SSZByteList
import ssz.SSZDict
import ssz.SSZList
import ssz.SSZObject
import ssz.Sequence
import ssz.bit
import ssz.boolean
import ssz.get_backing
import ssz.toPyBytes
import ssz.uint64
import kotlin.experimental.xor

fun ceillog2(x: uint64): pyint {
  return (x - 1uL).bit_length()
}

/*
    Return the largest integer ``x`` such that ``x**2 <= n``.
    */
fun integer_squareroot(n: uint64): uint64 {
  var x = n
  var y = ((x + 1uL) / 2uL)
  while (y < x) {
    x = y
    y = ((x + (n / x)) / 2uL)
  }
  return x
}

/*
    Return the exclusive-or of two 32-byte strings.
    */
fun xor(bytes_1: Bytes32, bytes_2: Bytes32): Bytes32 {
  return Bytes32(zip(bytes_1, bytes_2).map { (a, b) -> (a xor b) })
}

/*
    Return the ``length``-byte serialization of ``n`` in ``ENDIANNESS``-endian.
    */
fun int_to_bytes(n: uint64, length: uint64): pybytes {
  return n.to_bytes(length, ENDIANNESS)
}

/*
    Return the integer deserialization of ``data`` interpreted as ``ENDIANNESS``-endian.
    */
fun bytes_to_int(data: pybytes): uint64 {
  return from_bytes(data, ENDIANNESS)
}

/*
    Check if ``validator`` is active.
    */
fun is_active_validator(validator: Validator, epoch: Epoch): pybool {
  return (validator.activation_epoch <= epoch) && (epoch < validator.exit_epoch)
}

/*
    Check if ``validator`` is eligible to be placed into the activation queue.
    */
fun is_eligible_for_activation_queue(validator: Validator): pybool {
  return (validator.activation_eligibility_epoch == FAR_FUTURE_EPOCH) && (validator.effective_balance == MAX_EFFECTIVE_BALANCE)
}

/*
    Check if ``validator`` is eligible for activation.
    */
fun is_eligible_for_activation(state: BeaconState, validator: Validator): pybool {
  return (validator.activation_eligibility_epoch <= state.finalized_checkpoint.epoch) && (validator.activation_epoch == FAR_FUTURE_EPOCH)
}

/*
    Check if ``validator`` is slashable.
    */
fun is_slashable_validator(validator: Validator, epoch: Epoch): pybool {
  return !(validator.slashed) && (validator.activation_epoch <= epoch) && (epoch < validator.withdrawable_epoch)
}

/*
    Check if ``data_1`` and ``data_2`` are slashable according to Casper FFG rules.
    */
fun is_slashable_attestation_data(data_1: AttestationData, data_2: AttestationData): pybool {
  return (data_1 != data_2) && (data_1.target.epoch == data_2.target.epoch) || (data_1.source.epoch < data_2.source.epoch) && (data_2.target.epoch < data_1.target.epoch)
}

/*
    Check if ``indexed_attestation`` is not empty, has sorted and unique indices and has a valid aggregate signature.
    */
fun is_valid_indexed_attestation(state: BeaconState, indexed_attestation: IndexedAttestation): pybool {
  val indices = indexed_attestation.attesting_indices
  if ((len(indices) == 0uL) || !((indices == sorted(set(indices))))) {
    return false
  }
  val pubkeys = indices.map { i -> state.validators[i].pubkey }.toPyList()
  val domain = get_domain(state, DOMAIN_BEACON_ATTESTER, indexed_attestation.data.target.epoch)
  val signing_root = compute_signing_root(indexed_attestation.data, domain)
  return bls.FastAggregateVerify(pubkeys, signing_root, indexed_attestation.signature)
}

/*
    Check if ``leaf`` at ``index`` verifies against the Merkle ``root`` and ``branch``.
    */
fun is_valid_merkle_branch(leaf: Bytes32, branch: Sequence<Bytes32>, depth: uint64, index: uint64, root: Root): pybool {
  var value = leaf
  for (i in range(depth)) {
    if (pybool(((index / (2uL.pow(i))) % 2uL))) {
      value = hash((branch[i] + value))
    } else {
      value = hash((value + branch[i]))
    }
  }
  return (value == root)
}

/*
    Return the shuffled index corresponding to ``seed`` (and ``index_count``).
    */
fun compute_shuffled_index(index: uint64, index_count: uint64, seed: Bytes32): uint64 {
  assert((index < index_count))
  var index_ = index
  for (current_round in range(SHUFFLE_ROUND_COUNT)) {
    val pivot = (bytes_to_int(hash((seed + int_to_bytes(current_round, length = 1uL))).slice(0uL, 8uL)) % index_count)
    val flip = (((pivot + index_count) - index_) % index_count)
    val position = max(index_, flip)
    val source = hash(((seed + int_to_bytes(current_round, length = 1uL)) + int_to_bytes((position / 256uL), length = 4uL)))
    val byte = source[((position % 256uL) / 8uL)]
    val bit = ((byte shr (position % 8uL)) % 2uL)
    index_ = if (pybool(bit)) flip else index_
  }
  return index_
}

/*
    Return from ``indices`` a random index sampled by effective balance.
    */
fun compute_proposer_index(state: BeaconState, indices: Sequence<ValidatorIndex>, seed: Bytes32): ValidatorIndex {
  assert((len(indices) > 0uL))
  val MAX_RANDOM_BYTE = ((2uL.pow(8uL)) - 1uL)
  var i = 0uL
  while (true) {
    val candidate_index = indices[compute_shuffled_index((i % len(indices)), len(indices), seed)]
    val random_byte = hash((seed + int_to_bytes((i / 32uL), length = 8uL)))[(i % 32uL)]
    val effective_balance = state.validators[candidate_index].effective_balance
    if (((effective_balance * MAX_RANDOM_BYTE) >= (MAX_EFFECTIVE_BALANCE * random_byte.toUInt()))) {
      return candidate_index
    }
    i += 1uL
  }
}

/*
    Return the committee corresponding to ``indices``, ``seed``, ``index``, and committee ``count``.
    */
fun compute_committee(indices: Sequence<ValidatorIndex>, seed: Bytes32, index: uint64, count: uint64): Sequence<ValidatorIndex> {
  val start = ((len(indices) * index) / count)
  val end = ((len(indices) * (index + 1uL)) / count)
  return range(start, end).map { i -> indices[compute_shuffled_index(i, len(indices), seed)] }.toPyList()
}

/*
    Return the epoch number at ``slot``.
    */
fun compute_epoch_at_slot(slot: Slot): Epoch {
  return Epoch((slot / SLOTS_PER_EPOCH))
}

/*
    Return the start slot of ``epoch``.
    */
fun compute_start_slot_at_epoch(epoch: Epoch): Slot {
  return Slot((epoch * SLOTS_PER_EPOCH))
}

/*
    Return the epoch during which validator activations and exits initiated in ``epoch`` take effect.
    */
fun compute_activation_exit_epoch(epoch: Epoch): Epoch {
  return Epoch(((epoch + 1uL) + MAX_SEED_LOOKAHEAD))
}

/*
    Return the 32-byte fork data root for the ``current_version`` and ``genesis_validators_root``.
    This is used primarily in signature domains to avoid collisions across forks/chains.
    */
fun compute_fork_data_root(current_version: Version, genesis_validators_root: Root): Root {
  return hash_tree_root(ForkData(current_version = current_version, genesis_validators_root = genesis_validators_root))
}

/*
    Return the 4-byte fork digest for the ``current_version`` and ``genesis_validators_root``.
    This is a digest primarily used for domain separation on the p2p layer.
    4-bytes suffices for practical separation of forks/chains.
    */
fun compute_fork_digest(current_version: Version, genesis_validators_root: Root): ForkDigest {
  return ForkDigest(compute_fork_data_root(current_version, genesis_validators_root).slice(0uL, 4uL))
}

/*
    Return the domain for the ``domain_type`` and ``fork_version``.
    */
fun compute_domain(domain_type: DomainType, fork_version: Version? = null, genesis_validators_root: Root? = null): Domain {
  val fork_version_ = fork_version ?: GENESIS_FORK_VERSION
  val genesis_validators_root_ = genesis_validators_root ?: Root()
  val fork_data_root = compute_fork_data_root(fork_version_, genesis_validators_root_)
  return Domain((domain_type + fork_data_root.slice(0uL, 28uL)))
}

/*
    Return the signing root for the corresponding signing data.
    */
fun compute_signing_root(ssz_object: SSZObject, domain: Domain): Root {
  return hash_tree_root(SigningData(object_root = hash_tree_root(ssz_object), domain = domain))
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
  return if ((current_epoch == GENESIS_EPOCH)) GENESIS_EPOCH else Epoch((current_epoch - 1uL))
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
  return state.block_roots[(slot % SLOTS_PER_HISTORICAL_ROOT)]
}

/*
    Return the randao mix at a recent ``epoch``.
    */
fun get_randao_mix(state: BeaconState, epoch: Epoch): Bytes32 {
  return state.randao_mixes[(epoch % EPOCHS_PER_HISTORICAL_VECTOR)]
}

/*
    Return the sequence of active validator indices at ``epoch``.
    */
fun get_active_validator_indices(state: BeaconState, epoch: Epoch): Sequence<ValidatorIndex> {
  return enumerate(state.validators).filter { (_, v) -> is_active_validator(v, epoch) }.map { (i, _) -> ValidatorIndex(i) }.toPyList()
}

/*
    Return the validator churn limit for the current epoch.
    */
fun get_validator_churn_limit(state: BeaconState): uint64 {
  val active_validator_indices = get_active_validator_indices(state, get_current_epoch(state))
  return max(MIN_PER_EPOCH_CHURN_LIMIT, (len(active_validator_indices) / CHURN_LIMIT_QUOTIENT))
}

/*
    Return the seed at ``epoch``.
    */
fun get_seed(state: BeaconState, epoch: Epoch, domain_type: DomainType): Bytes32 {
  val mix = get_randao_mix(state, Epoch((((epoch + EPOCHS_PER_HISTORICAL_VECTOR) - MIN_SEED_LOOKAHEAD) - 1uL)))
  return hash(((domain_type + int_to_bytes(epoch, length = 8uL)) + mix))
}

/*
    Return the number of committees in each slot for the given ``epoch``.
    */
fun get_committee_count_per_slot(state: BeaconState, epoch: Epoch): uint64 {
  return max(1uL, min(MAX_COMMITTEES_PER_SLOT, ((len(get_active_validator_indices(state, epoch)) / SLOTS_PER_EPOCH) / TARGET_COMMITTEE_SIZE)))
}

/*
    Return the beacon committee at ``slot`` for ``index``.
    */
fun get_beacon_committee(state: BeaconState, slot: Slot, index: CommitteeIndex): Sequence<ValidatorIndex> {
  val epoch = compute_epoch_at_slot(slot)
  val committees_per_slot = get_committee_count_per_slot(state, epoch)
  return compute_committee(indices = get_active_validator_indices(state, epoch), seed = get_seed(state, epoch, DOMAIN_BEACON_ATTESTER), index = (((slot % SLOTS_PER_EPOCH) * committees_per_slot) + index), count = (committees_per_slot * SLOTS_PER_EPOCH))
}

/*
    Return the beacon proposer index at the current slot.
    */
fun get_beacon_proposer_index(state: BeaconState): ValidatorIndex {
  val epoch = get_current_epoch(state)
  val seed = hash((get_seed(state, epoch, DOMAIN_BEACON_PROPOSER) + int_to_bytes(state.slot, length = 8uL)))
  val indices = get_active_validator_indices(state, epoch)
  return compute_proposer_index(state, indices, seed)
}

/*
    Return the combined effective balance of the ``indices``.
    ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
    Math safe up to ~10B ETH, afterwhich this overflows uint64.
    */
fun get_total_balance(state: BeaconState, indices: Set<ValidatorIndex>): Gwei {
  return Gwei(max(EFFECTIVE_BALANCE_INCREMENT, sum(indices.map { index -> state.validators[index].effective_balance }.toPyList())))
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
  val epoch_ = if (epoch == null) get_current_epoch(state) else epoch
  val fork_version = if (epoch_ < state.fork.epoch) state.fork.previous_version else state.fork.current_version
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
  return set(enumerate(committee).filter { (i, _) -> bits[i] }.map { (_, index) -> index })
}

/*
    Increase the validator balance at index ``index`` by ``delta``.
    */
fun increase_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei): Unit {
  state.balances[index] += delta
}

/*
    Decrease the validator balance at index ``index`` by ``delta``, with underflow protection.
    */
fun decrease_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei): Unit {
  state.balances[index] = if ((delta > state.balances[index])) 0uL else (state.balances[index] - delta)
}

/*
    Initiate the exit of the validator with index ``index``.
    */
fun initiate_validator_exit(state: BeaconState, index: ValidatorIndex): Unit {
  val validator = state.validators[index]
  if ((validator.exit_epoch != FAR_FUTURE_EPOCH)) {
    return
  }
  val exit_epochs = state.validators.filter { v -> (v.exit_epoch != FAR_FUTURE_EPOCH) }.map { v -> v.exit_epoch }.toPyList()
  var exit_queue_epoch = max((exit_epochs + PyList(compute_activation_exit_epoch(get_current_epoch(state)))))
  val exit_queue_churn = len(state.validators.filter { v -> (v.exit_epoch == exit_queue_epoch) }.map { v -> v }.toPyList())
  if ((exit_queue_churn >= get_validator_churn_limit(state))) {
    exit_queue_epoch += Epoch(1uL)
  }
  validator.exit_epoch = exit_queue_epoch
  validator.withdrawable_epoch = Epoch((validator.exit_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY))
}

/*
    Slash the validator with index ``slashed_index``.
    */
fun slash_validator(state: BeaconState, slashed_index: ValidatorIndex, whistleblower_index: ValidatorIndex? = null): Unit {
  val epoch = get_current_epoch(state)
  initiate_validator_exit(state, slashed_index)
  val validator = state.validators[slashed_index]
  validator.slashed = true
  validator.withdrawable_epoch = max(validator.withdrawable_epoch, Epoch((epoch + EPOCHS_PER_SLASHINGS_VECTOR)))
  state.slashings[(epoch % EPOCHS_PER_SLASHINGS_VECTOR)] += validator.effective_balance
  decrease_balance(state, slashed_index, (validator.effective_balance / MIN_SLASHING_PENALTY_QUOTIENT))
  val proposer_index = get_beacon_proposer_index(state)
  val whistleblower_index_ = whistleblower_index ?: proposer_index
  val whistleblower_reward = Gwei((validator.effective_balance / WHISTLEBLOWER_REWARD_QUOTIENT))
  val proposer_reward = Gwei((whistleblower_reward / PROPOSER_REWARD_QUOTIENT))
  increase_balance(state, proposer_index, proposer_reward)
  increase_balance(state, whistleblower_index_, Gwei((whistleblower_reward - proposer_reward)))
}

fun initialize_beacon_state_from_eth1(eth1_block_hash: Bytes32, eth1_timestamp: uint64, deposits: Sequence<Deposit>): BeaconState {
  val fork = Fork(previous_version = GENESIS_FORK_VERSION, current_version = GENESIS_FORK_VERSION, epoch = GENESIS_EPOCH)
  val state = BeaconState(
      genesis_time = (eth1_timestamp + GENESIS_DELAY),
      fork = fork,
      eth1_data = Eth1Data(
          block_hash = eth1_block_hash,
          deposit_count = len(deposits)),
      latest_block_header = BeaconBlockHeader(
          body_root = hash_tree_root(BeaconBlockBody())),
      randao_mixes = (PyList(eth1_block_hash) * EPOCHS_PER_HISTORICAL_VECTOR))
  val leaves = list(map({ deposit -> deposit.data }, deposits))
  for ((index, deposit) in enumerate(deposits)) {
    val deposit_data_list = SSZList<DepositData>(*leaves.slice(0uL, (index.toULong() + 1uL)).toTypedArray())
    state.eth1_data.deposit_root = hash_tree_root(deposit_data_list)
    process_deposit(state, deposit)
  }
  for ((index, validator) in enumerate(state.validators)) {
    val balance = state.balances[index]
    validator.effective_balance = min((balance - (balance % EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE)
    if ((validator.effective_balance == MAX_EFFECTIVE_BALANCE)) {
      validator.activation_eligibility_epoch = GENESIS_EPOCH
      validator.activation_epoch = GENESIS_EPOCH
    }
  }
  state.genesis_validators_root = hash_tree_root(state.validators)
  return state
}

fun is_valid_genesis_state(state: BeaconState): pybool {
  if ((state.genesis_time < MIN_GENESIS_TIME)) {
    return false
  }
  if ((len(get_active_validator_indices(state, GENESIS_EPOCH)) < MIN_GENESIS_ACTIVE_VALIDATOR_COUNT)) {
    return false
  }
  return true
}

fun state_transition(state: BeaconState, signed_block: SignedBeaconBlock, validate_result: pybool = true): BeaconState {
  val block = signed_block.message
  process_slots(state, block.slot)
  if (validate_result) {
    assert(verify_block_signature(state, signed_block))
  }
  process_block(state, block)
  if (validate_result) {
    assert((block.state_root == hash_tree_root(state)))
  }
  return state
}

fun verify_block_signature(state: BeaconState, signed_block: SignedBeaconBlock): pybool {
  val proposer = state.validators[signed_block.message.proposer_index]
  val signing_root = compute_signing_root(signed_block.message, get_domain(state, DOMAIN_BEACON_PROPOSER))
  return bls.Verify(proposer.pubkey, signing_root, signed_block.signature)
}

fun process_slots(state: BeaconState, slot: Slot): Unit {
  assert((state.slot < slot))
  while ((state.slot < slot)) {
    process_slot(state)
    if ((((state.slot + 1uL) % SLOTS_PER_EPOCH) == 0uL)) {
      process_epoch(state)
    }
    state.slot = Slot((state.slot + 1uL))
  }
}

fun process_slot(state: BeaconState): Unit {
  val previous_state_root = hash_tree_root(state)
  state.state_roots[(state.slot % SLOTS_PER_HISTORICAL_ROOT)] = previous_state_root
  if ((state.latest_block_header.state_root == Bytes32())) {
    state.latest_block_header.state_root = previous_state_root
  }
  val previous_block_root = hash_tree_root(state.latest_block_header)
  state.block_roots[(state.slot % SLOTS_PER_HISTORICAL_ROOT)] = previous_block_root
}

fun process_epoch(state: BeaconState): Unit {
  process_justification_and_finalization(state)
  process_rewards_and_penalties(state)
  process_registry_updates(state)
  process_reveal_deadlines(state)
  process_challenge_deadlines(state)
  process_slashings(state)
  process_final_updates(state)
  process_phase_1_final_updates(state)
}

fun get_matching_source_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
  assert((epoch in Pair(get_previous_epoch(state), get_current_epoch(state))))
  return if ((epoch == get_current_epoch(state))) state.current_epoch_attestations else state.previous_epoch_attestations
}

fun get_matching_target_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
  return get_matching_source_attestations(state, epoch).filter { a -> (a.data.target.root == get_block_root(state, epoch)) }.map { a -> a }.toPyList()
}

fun get_matching_head_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
  return get_matching_target_attestations(state, epoch).filter { a -> (a.data.beacon_block_root == get_block_root_at_slot(state, a.data.slot)) }.map { a -> a }.toPyList()
}

fun get_unslashed_attesting_indices(state: BeaconState, attestations: Sequence<PendingAttestation>): Set<ValidatorIndex> {
  var output: Set<ValidatorIndex> = set()
  for (a in attestations) {
    output = output.union(get_attesting_indices(state, a.data, a.aggregation_bits))
  }
  return set(filter({ index -> !(state.validators[index].slashed) }, output))
}

/*
    Return the combined effective balance of the set of unslashed validators participating in ``attestations``.
    Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
    */
fun get_attesting_balance(state: BeaconState, attestations: Sequence<PendingAttestation>): Gwei {
  return get_total_balance(state, get_unslashed_attesting_indices(state, attestations))
}

fun process_justification_and_finalization(state: BeaconState): Unit {
  if ((get_current_epoch(state) <= (GENESIS_EPOCH + 1uL))) {
    return
  }
  val previous_epoch = get_previous_epoch(state)
  val current_epoch = get_current_epoch(state)
  val old_previous_justified_checkpoint = state.previous_justified_checkpoint
  val old_current_justified_checkpoint = state.current_justified_checkpoint
  state.previous_justified_checkpoint = state.current_justified_checkpoint
  state.justification_bits.updateSlice(
      1uL, len(state.justification_bits),
      state.justification_bits.slice(0uL, len(state.justification_bits) - 1uL))

  state.justification_bits[0uL] = 0uL
  var matching_target_attestations = get_matching_target_attestations(state, previous_epoch)
  if (((get_attesting_balance(state, matching_target_attestations) * 3uL) >= (get_total_active_balance(state) * 2uL))) {
    state.current_justified_checkpoint = Checkpoint(epoch = previous_epoch, root = get_block_root(state, previous_epoch))
    state.justification_bits[1uL] = 1uL
  }
  matching_target_attestations = get_matching_target_attestations(state, current_epoch)
  if (((get_attesting_balance(state, matching_target_attestations) * 3uL) >= (get_total_active_balance(state) * 2uL))) {
    state.current_justified_checkpoint = Checkpoint(epoch = current_epoch, root = get_block_root(state, current_epoch))
    state.justification_bits[0uL] = 1uL
  }
  val bits = state.justification_bits
  if (all(bits.slice(1uL, 4uL)) && ((old_previous_justified_checkpoint.epoch + 3uL) == current_epoch)) {
    state.finalized_checkpoint = old_previous_justified_checkpoint
  }
  if (all(bits.slice(1uL, 3uL)) && ((old_previous_justified_checkpoint.epoch + 2uL) == current_epoch)) {
    state.finalized_checkpoint = old_previous_justified_checkpoint
  }
  if (all(bits.slice(0uL, 3uL)) && ((old_current_justified_checkpoint.epoch + 2uL) == current_epoch)) {
    state.finalized_checkpoint = old_current_justified_checkpoint
  }
  if (all(bits.slice(0uL, 2uL)) && ((old_current_justified_checkpoint.epoch + 1uL) == current_epoch)) {
    state.finalized_checkpoint = old_current_justified_checkpoint
  }
}

fun get_base_reward(state: BeaconState, index: ValidatorIndex): Gwei {
  val total_balance = get_total_active_balance(state)
  val effective_balance = state.validators[index].effective_balance
  return Gwei((((effective_balance * BASE_REWARD_FACTOR) / integer_squareroot(total_balance)) / BASE_REWARDS_PER_EPOCH))
}

fun get_proposer_reward(state: BeaconState, attesting_index: ValidatorIndex): Gwei {
  return Gwei((get_base_reward(state, attesting_index) / PROPOSER_REWARD_QUOTIENT))
}

fun get_finality_delay(state: BeaconState): uint64 {
  return (get_previous_epoch(state) - state.finalized_checkpoint.epoch)
}

fun is_in_inactivity_leak(state: BeaconState): pybool {
  return (get_finality_delay(state) > MIN_EPOCHS_TO_INACTIVITY_PENALTY)
}

fun get_eligible_validator_indices(state: BeaconState): Sequence<ValidatorIndex> {
  val previous_epoch = get_previous_epoch(state)
  return enumerate(state.validators).filter { (_, v) -> is_active_validator(v, previous_epoch) || v.slashed && ((previous_epoch + 1uL) < v.withdrawable_epoch) }.map { (index, _) -> ValidatorIndex(index) }.toPyList()
}

/*
    Helper with shared logic for use by get source, target, and head deltas functions
    */
fun get_attestation_component_deltas(state: BeaconState, attestations: Sequence<PendingAttestation>): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  val rewards = (PyList(Gwei(0uL)) * len(state.validators))
  val penalties = (PyList(Gwei(0uL)) * len(state.validators))
  val total_balance = get_total_active_balance(state)
  val unslashed_attesting_indices = get_unslashed_attesting_indices(state, attestations)
  val attesting_balance = get_total_balance(state, unslashed_attesting_indices)
  for (index in get_eligible_validator_indices(state)) {
    if ((index in unslashed_attesting_indices)) {
      val increment = EFFECTIVE_BALANCE_INCREMENT
      if (is_in_inactivity_leak(state)) {
        rewards[index] += get_base_reward(state, index)
      } else {
        val reward_numerator = (get_base_reward(state, index) * (attesting_balance / increment))
        rewards[index] += (reward_numerator / (total_balance / increment))
      }
    } else {
      penalties[index] += get_base_reward(state, index)
    }
  }
  return Pair(rewards, penalties)
}

/*
    Return attester micro-rewards/penalties for source-vote for each validator.
    */
fun get_source_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  val matching_source_attestations = get_matching_source_attestations(state, get_previous_epoch(state))
  return get_attestation_component_deltas(state, matching_source_attestations)
}

/*
    Return attester micro-rewards/penalties for target-vote for each validator.
    */
fun get_target_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  val matching_target_attestations = get_matching_target_attestations(state, get_previous_epoch(state))
  return get_attestation_component_deltas(state, matching_target_attestations)
}

/*
    Return attester micro-rewards/penalties for head-vote for each validator.
    */
fun get_head_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  val matching_head_attestations = get_matching_head_attestations(state, get_previous_epoch(state))
  return get_attestation_component_deltas(state, matching_head_attestations)
}

/*
    Return proposer and inclusion delay micro-rewards/penalties for each validator.
    */
fun get_inclusion_delay_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  val rewards = range(len(state.validators)).map { _ -> Gwei(0uL) }.toPyList()
  val matching_source_attestations = get_matching_source_attestations(state, get_previous_epoch(state))
  for (index in get_unslashed_attesting_indices(state, matching_source_attestations)) {
    val attestation = min(matching_source_attestations.filter { a -> (index in get_attesting_indices(state, a.data, a.aggregation_bits)) }.map { a -> a }.toPyList(), key = { a -> a.inclusion_delay })
    rewards[attestation.proposer_index] += get_proposer_reward(state, index)
    val max_attester_reward = (get_base_reward(state, index) - get_proposer_reward(state, index))
    rewards[index] += Gwei((max_attester_reward / attestation.inclusion_delay))
  }
  val penalties = range(len(state.validators)).map { _ -> Gwei(0uL) }.toPyList()
  return Pair(rewards, penalties)
}

/*
    Return inactivity reward/penalty deltas for each validator.
    */
fun get_inactivity_penalty_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  val penalties = range(len(state.validators)).map { _ -> Gwei(0uL) }.toPyList()
  if (is_in_inactivity_leak(state)) {
    val matching_target_attestations = get_matching_target_attestations(state, get_previous_epoch(state))
    val matching_target_attesting_indices = get_unslashed_attesting_indices(state, matching_target_attestations)
    for (index in get_eligible_validator_indices(state)) {
      val base_reward = get_base_reward(state, index)
      penalties[index] += Gwei(((BASE_REWARDS_PER_EPOCH * base_reward) - get_proposer_reward(state, index)))
      if ((index !in matching_target_attesting_indices)) {
        val effective_balance = state.validators[index].effective_balance
        penalties[index] += Gwei(((effective_balance * get_finality_delay(state)) / INACTIVITY_PENALTY_QUOTIENT))
      }
    }
  }
  val rewards = range(len(state.validators)).map { _ -> Gwei(0uL) }.toPyList()
  return Pair(rewards, penalties)
}

/*
    Return attestation reward/penalty deltas for each validator.
    */
fun get_attestation_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  val (source_rewards, source_penalties) = get_source_deltas(state)
  val (target_rewards, target_penalties) = get_target_deltas(state)
  val (head_rewards, head_penalties) = get_head_deltas(state)
  val (inclusion_delay_rewards, _) = get_inclusion_delay_deltas(state)
  val (_, inactivity_penalties) = get_inactivity_penalty_deltas(state)
  val rewards = range(len(state.validators)).map { i -> (((source_rewards[i] + target_rewards[i]) + head_rewards[i]) + inclusion_delay_rewards[i]) }.toPyList()
  val penalties = range(len(state.validators)).map { i -> (((source_penalties[i] + target_penalties[i]) + head_penalties[i]) + inactivity_penalties[i]) }.toPyList()
  return Pair(rewards, penalties)
}

fun process_rewards_and_penalties(state: BeaconState): Unit {
  if ((get_current_epoch(state) == GENESIS_EPOCH)) {
    return
  }
  val (rewards, penalties) = get_attestation_deltas(state)
  for (index in range(len(state.validators))) {
    increase_balance(state, ValidatorIndex(index), rewards[index])
    decrease_balance(state, ValidatorIndex(index), penalties[index])
  }
}

fun process_registry_updates(state: BeaconState): Unit {
  for ((index, validator) in enumerate(state.validators)) {
    if (is_eligible_for_activation_queue(validator)) {
      validator.activation_eligibility_epoch = (get_current_epoch(state) + 1uL)
    }
    if (is_active_validator(validator, get_current_epoch(state)) && (validator.effective_balance <= EJECTION_BALANCE)) {
      initiate_validator_exit(state, ValidatorIndex(index))
    }
  }
  val activation_queue = sorted(
      enumerate(state.validators)
          .filter { (_, validator) -> is_eligible_for_activation(state, validator) }
          .map { (index, _) -> index }.toPyList(),
      key = { index -> Tuple2(state.validators[index].activation_eligibility_epoch, index) })
  for (index in activation_queue.slice(0uL, get_validator_churn_limit(state))) {
    val validator = state.validators[index]
    validator.activation_epoch = compute_activation_exit_epoch(get_current_epoch(state))
  }
}

fun process_slashings(state: BeaconState): Unit {
  val epoch = get_current_epoch(state)
  val total_balance = get_total_active_balance(state)
  for ((index, validator) in enumerate(state.validators)) {
    if (validator.slashed && ((epoch + (EPOCHS_PER_SLASHINGS_VECTOR / 2uL)) == validator.withdrawable_epoch)) {
      val increment = EFFECTIVE_BALANCE_INCREMENT
      val penalty_numerator = ((validator.effective_balance / increment) * min((sum(state.slashings) * 3uL), total_balance))
      val penalty = ((penalty_numerator / total_balance) * increment)
      decrease_balance(state, ValidatorIndex(index), penalty)
    }
  }
}

fun process_final_updates(state: BeaconState): Unit {
  val current_epoch = get_current_epoch(state)
  val next_epoch = Epoch((current_epoch + 1uL))
  if (((next_epoch % EPOCHS_PER_ETH1_VOTING_PERIOD) == 0uL)) {
    state.eth1_data_votes = SSZList<Eth1Data>()
  }
  for ((index, validator) in enumerate(state.validators)) {
    val balance = state.balances[index]
    val HYSTERESIS_INCREMENT = (EFFECTIVE_BALANCE_INCREMENT / HYSTERESIS_QUOTIENT)
    val DOWNWARD_THRESHOLD = (HYSTERESIS_INCREMENT * HYSTERESIS_DOWNWARD_MULTIPLIER)
    val UPWARD_THRESHOLD = (HYSTERESIS_INCREMENT * HYSTERESIS_UPWARD_MULTIPLIER)
    if (((balance + DOWNWARD_THRESHOLD) < validator.effective_balance) || ((validator.effective_balance + UPWARD_THRESHOLD) < balance)) {
      validator.effective_balance = min((balance - (balance % EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE)
    }
  }
  state.slashings[(next_epoch % EPOCHS_PER_SLASHINGS_VECTOR)] = Gwei(0uL)
  state.randao_mixes[(next_epoch % EPOCHS_PER_HISTORICAL_VECTOR)] = get_randao_mix(state, current_epoch)
  if (((next_epoch % (SLOTS_PER_HISTORICAL_ROOT / SLOTS_PER_EPOCH)) == 0uL)) {
    val historical_batch = HistoricalBatch(block_roots = state.block_roots, state_roots = state.state_roots)
    state.historical_roots.append(hash_tree_root(historical_batch))
  }
  state.previous_epoch_attestations = state.current_epoch_attestations
  state.current_epoch_attestations = SSZList<PendingAttestation>()
}

fun process_block(state: BeaconState, block: BeaconBlock): Unit {
  process_block_header(state, block)
  process_randao(state, block.body)
  process_eth1_data(state, block.body)
  process_light_client_aggregate(state, block.body)
  process_operations(state, block.body)
}

fun process_block_header(state: BeaconState, block: BeaconBlock): Unit {
  assert((block.slot == state.slot))
  assert((block.slot > state.latest_block_header.slot))
  assert((block.proposer_index == get_beacon_proposer_index(state)))
  assert((block.parent_root == hash_tree_root(state.latest_block_header)))
  state.latest_block_header = BeaconBlockHeader(slot = block.slot, proposer_index = block.proposer_index, parent_root = block.parent_root, state_root = Bytes32(), body_root = hash_tree_root(block.body))
  val proposer = state.validators[block.proposer_index]
  assert(!(proposer.slashed))
}

fun process_randao(state: BeaconState, body: BeaconBlockBody): Unit {
  val epoch = get_current_epoch(state)
  val proposer = state.validators[get_beacon_proposer_index(state)]
  val signing_root = compute_signing_root(epoch, get_domain(state, DOMAIN_RANDAO))
  assert(bls.Verify(proposer.pubkey, signing_root, body.randao_reveal))
  val mix = xor(get_randao_mix(state, epoch), hash(body.randao_reveal))
  state.randao_mixes[(epoch % EPOCHS_PER_HISTORICAL_VECTOR)] = mix
}

fun process_eth1_data(state: BeaconState, body: BeaconBlockBody): Unit {
  state.eth1_data_votes.append(body.eth1_data)
  if (((state.eth1_data_votes.count(body.eth1_data) * 2uL) > (EPOCHS_PER_ETH1_VOTING_PERIOD * SLOTS_PER_EPOCH))) {
    state.eth1_data = body.eth1_data
  }
}

fun process_operations(state: BeaconState, body: BeaconBlockBody): Unit {
  assert((len(body.deposits) == min(MAX_DEPOSITS, (state.eth1_data.deposit_count - state.eth1_deposit_index))))
  fun <T> for_ops(operations: Sequence<T>, fn: (BeaconState, T) -> Unit): Unit {
    for (operation in operations) {
      fn(state, operation)
    }
  }
  for_ops(body.proposer_slashings, ::process_proposer_slashing)
  for_ops(body.attester_slashings, ::process_attester_slashing)
  for_ops(body.attestations, ::process_attestation)
  for_ops(body.deposits, ::process_deposit)
  for_ops(body.voluntary_exits, ::process_voluntary_exit)
  process_custody_game_operations(state, body)
  process_shard_transitions(state, body.shard_transitions, body.attestations)
}

fun process_proposer_slashing(state: BeaconState, proposer_slashing: ProposerSlashing): Unit {
  val header_1 = proposer_slashing.signed_header_1.message
  val header_2 = proposer_slashing.signed_header_2.message
  assert((header_1.slot == header_2.slot))
  assert((header_1.proposer_index == header_2.proposer_index))
  assert((header_1 != header_2))
  val proposer = state.validators[header_1.proposer_index]
  assert(is_slashable_validator(proposer, get_current_epoch(state)))
  for (signed_header in listOf(proposer_slashing.signed_header_1, proposer_slashing.signed_header_2)) {
    val domain = get_domain(state, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(signed_header.message.slot))
    val signing_root = compute_signing_root(signed_header.message, domain)
    assert(bls.Verify(proposer.pubkey, signing_root, signed_header.signature))
  }
  slash_validator(state, header_1.proposer_index)
}

fun process_attester_slashing(state: BeaconState, attester_slashing: AttesterSlashing): Unit {
  val attestation_1 = attester_slashing.attestation_1
  val attestation_2 = attester_slashing.attestation_2
  assert(is_slashable_attestation_data(attestation_1.data, attestation_2.data))
  assert(is_valid_indexed_attestation(state, attestation_1))
  assert(is_valid_indexed_attestation(state, attestation_2))
  var slashed_any = false
  val indices = set(attestation_1.attesting_indices).intersection(attestation_2.attesting_indices)
  for (index in sorted(indices)) {
    if (is_slashable_validator(state.validators[index], get_current_epoch(state))) {
      slash_validator(state, index)
      slashed_any = true
    }
  }
  assert(slashed_any)
}

fun process_attestation(state: BeaconState, attestation: Attestation): Unit {
  validate_attestation(state, attestation)
  val pending_attestation = PendingAttestation(aggregation_bits = attestation.aggregation_bits, data = attestation.data, inclusion_delay = (state.slot - attestation.data.slot), proposer_index = get_beacon_proposer_index(state), crosslink_success = false)
  if ((attestation.data.target.epoch == get_current_epoch(state))) {
    state.current_epoch_attestations.append(pending_attestation)
  } else {
    state.previous_epoch_attestations.append(pending_attestation)
  }
}

fun get_validator_from_deposit(state: BeaconState, deposit: Deposit): Validator {
  val amount = deposit.data.amount
  val effective_balance = min((amount - (amount % EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE)
  val next_custody_secret_to_reveal = get_custody_period_for_validator(ValidatorIndex(len(state.validators)), get_current_epoch(state))
  return Validator(pubkey = deposit.data.pubkey, withdrawal_credentials = deposit.data.withdrawal_credentials, activation_eligibility_epoch = FAR_FUTURE_EPOCH, activation_epoch = FAR_FUTURE_EPOCH, exit_epoch = FAR_FUTURE_EPOCH, withdrawable_epoch = FAR_FUTURE_EPOCH, effective_balance = effective_balance, next_custody_secret_to_reveal = uint64(next_custody_secret_to_reveal), all_custody_secrets_revealed_epoch = FAR_FUTURE_EPOCH)
}

fun process_deposit(state: BeaconState, deposit: Deposit): Unit {
  assert(is_valid_merkle_branch(leaf = hash_tree_root(deposit.data), branch = deposit.proof, depth = (DEPOSIT_CONTRACT_TREE_DEPTH + 1uL), index = state.eth1_deposit_index, root = state.eth1_data.deposit_root))
  state.eth1_deposit_index += 1uL
  val pubkey = deposit.data.pubkey
  val amount = deposit.data.amount
  val validator_pubkeys = state.validators.map { v -> v.pubkey }.toPyList()
  if ((pubkey !in validator_pubkeys)) {
    val deposit_message = DepositMessage(pubkey = deposit.data.pubkey, withdrawal_credentials = deposit.data.withdrawal_credentials, amount = deposit.data.amount)
    val domain = compute_domain(DOMAIN_DEPOSIT)
    val signing_root = compute_signing_root(deposit_message, domain)
    if (!(bls.Verify(pubkey, signing_root, deposit.data.signature))) {
      return
    }
    state.validators.append(get_validator_from_deposit(state, deposit))
    state.balances.append(amount)
  } else {
    val index = ValidatorIndex(validator_pubkeys.index(pubkey))
    increase_balance(state, index, amount)
  }
}

fun process_voluntary_exit(state: BeaconState, signed_voluntary_exit: SignedVoluntaryExit): Unit {
  val voluntary_exit = signed_voluntary_exit.message
  val validator = state.validators[voluntary_exit.validator_index]
  assert(is_active_validator(validator, get_current_epoch(state)))
  assert((validator.exit_epoch == FAR_FUTURE_EPOCH))
  assert((get_current_epoch(state) >= voluntary_exit.epoch))
  assert((get_current_epoch(state) >= (validator.activation_epoch + SHARD_COMMITTEE_PERIOD)))
  val domain = get_domain(state, DOMAIN_VOLUNTARY_EXIT, voluntary_exit.epoch)
  val signing_root = compute_signing_root(voluntary_exit, domain)
  assert(bls.Verify(validator.pubkey, signing_root, signed_voluntary_exit.signature))
  initiate_validator_exit(state, voluntary_exit.validator_index)
}

fun get_forkchoice_store(anchor_state: BeaconState): Store {
  val anchor_block_header = anchor_state.latest_block_header.copy()
  if ((anchor_block_header.state_root == Bytes32())) {
    anchor_block_header.state_root = hash_tree_root(anchor_state)
  }
  val anchor_root = hash_tree_root(anchor_block_header)
  val anchor_epoch = get_current_epoch(anchor_state)
  val justified_checkpoint = Checkpoint(epoch = anchor_epoch, root = anchor_root)
  val finalized_checkpoint = Checkpoint(epoch = anchor_epoch, root = anchor_root)
  return Store(
      time = (anchor_state.genesis_time + (SECONDS_PER_SLOT * anchor_state.slot)),
      genesis_time = anchor_state.genesis_time,
      justified_checkpoint = justified_checkpoint,
      finalized_checkpoint = finalized_checkpoint,
      best_justified_checkpoint = justified_checkpoint,
      blocks = PyDict(anchor_root to anchor_block_header),
      block_states = PyDict(anchor_root to anchor_state.copy()),
      checkpoint_states = PyDict(justified_checkpoint to anchor_state.copy()))
}

fun get_slots_since_genesis(store: Store): pyint {
  return pyint((store.time - store.genesis_time) / SECONDS_PER_SLOT)
}

fun get_current_slot(store: Store): Slot {
  return Slot((GENESIS_SLOT + uint64(get_slots_since_genesis(store))))
}

fun compute_slots_since_epoch_start(slot: Slot): pyint {
  return pyint(slot - compute_start_slot_at_epoch(compute_epoch_at_slot(slot)))
}

fun get_ancestor(store: Store, root: Root, slot: Slot): Root {
  val block = store.blocks[root]!!
  if ((block.slot > slot)) {
    return get_ancestor(store, block.parent_root, slot)
  } else {
    if ((block.slot == slot)) {
      return root
    } else {
      return root
    }
  }
}

fun get_latest_attesting_balance(store: Store, root: Root): Gwei {
  val state = store.checkpoint_states[store.justified_checkpoint]!!
  val active_indices = get_active_validator_indices(state, get_current_epoch(state))
  return Gwei(sum(active_indices.filter { i -> (i in store.latest_messages) && (get_ancestor(store, store.latest_messages[i]!!.root, store.blocks[root]!!.slot) == root) }.map { i -> state.validators[i].effective_balance }))
}

fun filter_block_tree(store: Store, block_root: Root, blocks: SSZDict<Root, BeaconBlockHeader>): pybool {
  val block = store.blocks[block_root]!!
  val children = store.blocks.keys().filter { root -> (store.blocks[root]!!.parent_root == block_root) }.map { root -> root }.toPyList()
  if (any(children)) {
    val filter_block_tree_result = children.map { child -> filter_block_tree(store, child, blocks) }.toPyList()
    if (any(filter_block_tree_result)) {
      blocks[block_root] = block
      return true
    }
    return false
  }
  val head_state = store.block_states[block_root]!!
  val correct_justified = (store.justified_checkpoint.epoch == GENESIS_EPOCH) || (head_state.current_justified_checkpoint == store.justified_checkpoint)
  val correct_finalized = (store.finalized_checkpoint.epoch == GENESIS_EPOCH) || (head_state.finalized_checkpoint == store.finalized_checkpoint)
  if (correct_justified && correct_finalized) {
    blocks[block_root] = block
    return true
  }
  return false
}

/*
    Retrieve a filtered block tree from ``store``, only returning branches
    whose leaf state's justified/finalized info agrees with that in ``store``.
    */
fun get_filtered_block_tree(store: Store): SSZDict<Root, BeaconBlockHeader> {
  val base = store.justified_checkpoint.root
  val blocks: SSZDict<Root, BeaconBlockHeader> = PyDict()
  filter_block_tree(store, base, blocks)
  return blocks
}

fun get_head(store: Store): Root {
  val blocks = get_filtered_block_tree(store)
  var head = store.justified_checkpoint.root
  val justified_slot = compute_start_slot_at_epoch(store.justified_checkpoint.epoch)
  while (true) {
    val children = blocks.keys().filter { root -> (blocks[root]!!.parent_root == head) && (blocks[root]!!.slot > justified_slot) }.map { root -> root }.toPyList()
    if ((len(children) == 0uL)) {
      return head
    }
    head = max(children, key = { root -> Tuple2(get_latest_attesting_balance(store, root), root) })
  }
}

/*
    To address the bouncing attack, only update conflicting justified
    checkpoints in the fork choice if in the early slots of the epoch.
    Otherwise, delay incorporation of new justified checkpoint until next epoch boundary.

    See https://ethresear.ch/t/prevention-of-bouncing-attack-on-ffg/6114 for more detailed analysis and discussion.
    */
fun should_update_justified_checkpoint(store: Store, new_justified_checkpoint: Checkpoint): pybool {
  if ((compute_slots_since_epoch_start(get_current_slot(store)) < SAFE_SLOTS_TO_UPDATE_JUSTIFIED)) {
    return true
  }
  val justified_slot = compute_start_slot_at_epoch(store.justified_checkpoint.epoch)
  if (!((get_ancestor(store, new_justified_checkpoint.root, justified_slot) == store.justified_checkpoint.root))) {
    return false
  }
  return true
}

fun validate_on_attestation(store: Store, attestation: Attestation): Unit {
  val target = attestation.data.target
  val current_epoch = compute_epoch_at_slot(get_current_slot(store))
  val previous_epoch = if ((current_epoch > GENESIS_EPOCH)) (current_epoch - 1uL) else GENESIS_EPOCH
  assert((target.epoch in PyList(current_epoch, previous_epoch)))
  assert((target.epoch == compute_epoch_at_slot(attestation.data.slot)))
  assert((target.root in store.blocks))
  assert((attestation.data.beacon_block_root in store.blocks))
  assert((store.blocks[attestation.data.beacon_block_root]!!.slot <= attestation.data.slot))
  val target_slot = compute_start_slot_at_epoch(target.epoch)
  assert((target.root == get_ancestor(store, attestation.data.beacon_block_root, target_slot)))
  assert((get_current_slot(store) >= (attestation.data.slot + 1uL)))
}

fun store_target_checkpoint_state(store: Store, target: Checkpoint): Unit {
  if ((target !in store.checkpoint_states)) {
    val base_state = store.block_states[target.root]!!.copy()
    if ((base_state.slot < compute_start_slot_at_epoch(target.epoch))) {
      process_slots(base_state, compute_start_slot_at_epoch(target.epoch))
    }
    store.checkpoint_states[target] = base_state
  }
}

fun update_latest_messages(store: Store, attesting_indices: Sequence<ValidatorIndex>, attestation: Attestation): Unit {
  val target = attestation.data.target
  val beacon_block_root = attestation.data.beacon_block_root
  val shard = attestation.data.shard
  for (i in attesting_indices) {
    if ((i !in store.latest_messages) || (target.epoch > store.latest_messages[i]!!.epoch)) {
      store.latest_messages[i] = LatestMessage(epoch = target.epoch, root = beacon_block_root, shard = shard, shard_root = attestation.data.shard_head_root)
    }
  }
}

fun on_tick(store: Store, time: uint64): Unit {
  val previous_slot = get_current_slot(store)
  store.time = time
  val current_slot = get_current_slot(store)
  if (!((current_slot > previous_slot) && (compute_slots_since_epoch_start(current_slot) == pyint(0uL)))) {
    return
  }
  if ((store.best_justified_checkpoint.epoch > store.justified_checkpoint.epoch)) {
    store.justified_checkpoint = store.best_justified_checkpoint
  }
}

fun on_block(store: Store, signed_block: SignedBeaconBlock): Unit {
  val block = signed_block.message
  assert((block.parent_root in store.block_states))
  val pre_state = store.block_states[block.parent_root]!!.copy()
  assert((get_current_slot(store) >= block.slot))
  val finalized_slot = compute_start_slot_at_epoch(store.finalized_checkpoint.epoch)
  assert((block.slot > finalized_slot))
  assert((get_ancestor(store, block.parent_root, finalized_slot) == store.finalized_checkpoint.root))
  val state = state_transition(pre_state, signed_block, true)
  store.blocks[hash_tree_root(block)] = BeaconBlockHeader(slot = block.slot, proposer_index = block.proposer_index, parent_root = block.parent_root, state_root = block.state_root, body_root = hash_tree_root(block.body))
  store.block_states[hash_tree_root(block)] = state
  if ((state.current_justified_checkpoint.epoch > store.justified_checkpoint.epoch)) {
    if ((state.current_justified_checkpoint.epoch > store.best_justified_checkpoint.epoch)) {
      store.best_justified_checkpoint = state.current_justified_checkpoint
    }
    if (should_update_justified_checkpoint(store, state.current_justified_checkpoint)) {
      store.justified_checkpoint = state.current_justified_checkpoint
    }
  }
  if ((state.finalized_checkpoint.epoch > store.finalized_checkpoint.epoch)) {
    store.finalized_checkpoint = state.finalized_checkpoint
    if ((store.justified_checkpoint != state.current_justified_checkpoint)) {
      if ((state.current_justified_checkpoint.epoch > store.justified_checkpoint.epoch)) {
        store.justified_checkpoint = state.current_justified_checkpoint
        return
      }
      val finalized_slot = compute_start_slot_at_epoch(store.finalized_checkpoint.epoch)
      val ancestor_at_finalized_slot = get_ancestor(store, store.justified_checkpoint.root, finalized_slot)
      if ((ancestor_at_finalized_slot != store.finalized_checkpoint.root)) {
        store.justified_checkpoint = state.current_justified_checkpoint
      }
    }
  }
}

/*
    Run ``on_attestation`` upon receiving a new ``attestation`` from either within a block or directly on the wire.

    An ``attestation`` that is asserted as invalid may be valid at a later time,
    consider scheduling it for later processing in such case.
    */
fun on_attestation(store: Store, attestation: Attestation): Unit {
  validate_on_attestation(store, attestation)
  store_target_checkpoint_state(store, attestation.data.target)
  val target_state = store.checkpoint_states[attestation.data.target]!!
  val indexed_attestation = get_indexed_attestation(target_state, attestation)
  assert(is_valid_indexed_attestation(target_state, indexed_attestation))
  update_latest_messages(store, indexed_attestation.attesting_indices, attestation)
}

fun check_if_validator_active(state: BeaconState, validator_index: ValidatorIndex): pybool {
  val validator = state.validators[validator_index]
  return is_active_validator(validator, get_current_epoch(state))
}

/*
    Return the committee assignment in the ``epoch`` for ``validator_index``.
    ``assignment`` returned is a tuple of the following form:
        * ``assignmentlistOf(0)`` is the list of validators in the committee
        * ``assignmentlistOf(1)`` is the index to which the committee is assigned
        * ``assignmentlistOf(2)`` is the slot at which the committee is assigned
    Return None if no assignment.
    */
fun get_committee_assignment(state: BeaconState, epoch: Epoch, validator_index: ValidatorIndex): Triple<Sequence<ValidatorIndex>, CommitteeIndex, Slot>? {
  val next_epoch = (get_current_epoch(state) + 1uL)
  assert((epoch <= next_epoch))
  val start_slot = compute_start_slot_at_epoch(epoch)
  val committee_count_per_slot = get_committee_count_per_slot(state, epoch)
  for (slot in range(start_slot, (start_slot + SLOTS_PER_EPOCH))) {
    for (index in range(committee_count_per_slot)) {
      val committee = get_beacon_committee(state, Slot(slot), CommitteeIndex(index))
      if ((validator_index in committee)) {
        return Triple(committee, CommitteeIndex(index), Slot(slot))
      }
    }
  }
  return null
}

fun is_proposer(state: BeaconState, validator_index: ValidatorIndex): pybool {
  return (get_beacon_proposer_index(state) == validator_index)
}

fun get_epoch_signature(state: BeaconState, block: BeaconBlock, privkey: pyint): BLSSignature {
  val domain = get_domain(state, DOMAIN_RANDAO, compute_epoch_at_slot(block.slot))
  val signing_root = compute_signing_root(compute_epoch_at_slot(block.slot), domain)
  return bls.Sign(privkey, signing_root)
}

fun compute_time_at_slot(state: BeaconState, slot: Slot): uint64 {
  return (state.genesis_time + (slot * SECONDS_PER_SLOT))
}

fun voting_period_start_time(state: BeaconState): uint64 {
  val eth1_voting_period_start_slot = Slot((state.slot - (state.slot % (EPOCHS_PER_ETH1_VOTING_PERIOD * SLOTS_PER_EPOCH))))
  return compute_time_at_slot(state, eth1_voting_period_start_slot)
}

fun is_candidate_block(block: Eth1Block, period_start: uint64): pybool {
  return ((block.timestamp + (SECONDS_PER_ETH1_BLOCK * ETH1_FOLLOW_DISTANCE)) <= period_start) && ((block.timestamp + ((SECONDS_PER_ETH1_BLOCK * ETH1_FOLLOW_DISTANCE) * 2uL)) >= period_start)
}

fun get_eth1_vote(state: BeaconState, eth1_chain: Sequence<Eth1Block>): Eth1Data {
  val period_start = voting_period_start_time(state)
  val votes_to_consider = eth1_chain.filter { block -> is_candidate_block(block, period_start) && (get_eth1_data(block).deposit_count >= state.eth1_data.deposit_count) }.map { block -> get_eth1_data(block) }.toPyList()
  val valid_votes = state.eth1_data_votes.filter { vote -> (vote in votes_to_consider) }.map { vote -> vote }.toPyList()
  val default_vote = if (any(votes_to_consider)) votes_to_consider[(len(votes_to_consider) - 1uL)] else state.eth1_data
  return max(valid_votes, key = { v -> Tuple2(valid_votes.count(v), -(valid_votes.index(v))) }, default = default_vote)
}

fun compute_new_state_root(state: BeaconState, block: BeaconBlock): Root {
  var temp_state: BeaconState = state.copy()
  val signed_block = SignedBeaconBlock(message = block)
  temp_state = state_transition(temp_state, signed_block, validate_result = false)
  return hash_tree_root(temp_state)
}

fun get_block_signature(state: BeaconState, block: BeaconBlock, privkey: pyint): BLSSignature {
  val domain = get_domain(state, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(block.slot))
  val signing_root = compute_signing_root(block, domain)
  return bls.Sign(privkey, signing_root)
}

fun get_attestation_signature(state: BeaconState, attestation_data: AttestationData, privkey: pyint): BLSSignature {
  val domain = get_domain(state, DOMAIN_BEACON_ATTESTER, attestation_data.target.epoch)
  val signing_root = compute_signing_root(attestation_data, domain)
  return bls.Sign(privkey, signing_root)
}

/*
    Compute the correct subnet for an attestation for Phase 0.
    Note, this mimics expected Phase 1 behavior where attestations will be mapped to their shard subnet.
    */
fun compute_subnet_for_attestation(committees_per_slot: uint64, slot: Slot, committee_index: CommitteeIndex): uint64 {
  val slots_since_epoch_start = (slot % SLOTS_PER_EPOCH)
  val committees_since_epoch_start = (committees_per_slot * slots_since_epoch_start)
  return ((committees_since_epoch_start + committee_index) % ATTESTATION_SUBNET_COUNT)
}

fun get_slot_signature(state: BeaconState, slot: Slot, privkey: pyint): BLSSignature {
  val domain = get_domain(state, DOMAIN_SELECTION_PROOF, compute_epoch_at_slot(slot))
  val signing_root = compute_signing_root(slot, domain)
  return bls.Sign(privkey, signing_root)
}

fun is_aggregator(state: BeaconState, slot: Slot, index: CommitteeIndex, slot_signature: BLSSignature): pybool {
  val committee = get_beacon_committee(state, slot, index)
  val modulo = max(1uL, (len(committee) / TARGET_AGGREGATORS_PER_COMMITTEE))
  return ((bytes_to_int(hash(slot_signature).slice(0uL, 8uL)) % modulo) == 0uL)
}

fun get_aggregate_signature(attestations: Sequence<Attestation>): BLSSignature {
  val signatures = attestations.map { attestation -> attestation.signature }.toPyList()
  return bls.Aggregate(signatures)
}

fun get_aggregate_and_proof(state: BeaconState, aggregator_index: ValidatorIndex, aggregate: Attestation, privkey: pyint): AggregateAndProof {
  return AggregateAndProof(aggregator_index = aggregator_index, aggregate = aggregate, selection_proof = get_slot_signature(state, aggregate.data.slot, privkey))
}

fun get_aggregate_and_proof_signature(state: BeaconState, aggregate_and_proof: AggregateAndProof, privkey: pyint): BLSSignature {
  val aggregate = aggregate_and_proof.aggregate
  val domain = get_domain(state, DOMAIN_AGGREGATE_AND_PROOF, compute_epoch_at_slot(aggregate.data.slot))
  val signing_root = compute_signing_root(aggregate_and_proof, domain)
  return bls.Sign(privkey, signing_root)
}

fun replace_empty_or_append(l: PyList<CustodyChunkChallengeRecord>, new_element: CustodyChunkChallengeRecord): pyint {
  for (i in range(len(l))) {
    if ((l[i] == CustodyChunkChallengeRecord())) {
      l[i] = new_element
      return pyint(i)
    }
  }
  l.append(new_element)
  return pyint(len(l) - 1uL)
}

fun legendre_bit(a: pyint, q: pyint): boolean {
  if (a >= q) {
    return legendre_bit((a % q), q)
  }
  if (a == pyint(0uL)) {
    return false
  }
  assert((q > a) && (a > 0uL) && ((q % 2uL) == pyint(1uL)))
  var a_ = a
  var t = 1L
  var n = q
  while (a_ != pyint(0uL)) {
    while ((a_ % 2uL) == pyint(0uL)) {
      a_ /= 2uL
      val r = (n % 8uL)
      if ((r == pyint(3uL)) || (r == pyint(5uL))) {
        t = -t
      }
    }
    val tmp_a = a_
    a_ = n
    n = tmp_a
    if (((a_ % 4uL) == (n % 4uL)) && ((n % 4uL) == pyint(3uL))) {
      t = -t
    }
    a_ %= n
  }
  if (n == pyint(1uL)) {
    return ((t + 1) / 2) != 0L
  } else {
    return false
  }
}

fun get_custody_atoms(bytez: pybytes): Sequence<pybytes> {
  val length_remainder = (len(bytez) % BYTES_PER_CUSTODY_ATOM)
  val bytez_ = bytez + Bytes.fromHexString("0x" + List(((BYTES_PER_CUSTODY_ATOM - length_remainder) % BYTES_PER_CUSTODY_ATOM).toInt()) { "00" })
  return range(0uL, len(bytez_), BYTES_PER_CUSTODY_ATOM).map { i -> bytez_.slice(i, (i + BYTES_PER_CUSTODY_ATOM)) }.toPyList()
}

fun get_custody_secrets(key: BLSSignature): Sequence<pyint> {
  val full_G2_element = bls.signature_to_G2(key)
  val signature = full_G2_element.first.coeffs
  val signature_bytes = pybytes("").join(signature.map { x -> x.to_bytes(48uL, "little") })
  val secrets = range(0uL, len(signature_bytes), 32uL).map { i -> pyint(from_bytes(signature_bytes.slice(i, (i + BYTES_PER_CUSTODY_ATOM)), "little")) }.toPyList()
  return secrets
}

fun universal_hash_function(data_chunks: Sequence<pybytes>, secrets: Sequence<pyint>): pyint {
  val n = len(data_chunks)
  return ((sum(enumerate(data_chunks).map { (i, atom) -> (((secrets[(i.toULong() % CUSTODY_SECRETS)].pow(i)) * from_bytes(atom, "little")) % CUSTODY_PRIME) }) + (secrets[(n % CUSTODY_SECRETS)].pow(n))) % CUSTODY_PRIME)
}

fun compute_custody_bit(key: BLSSignature, data: SSZByteList): bit {
  val custody_atoms = get_custody_atoms(data.toPyBytes())
  val secrets = get_custody_secrets(key)
  val uhf = universal_hash_function(custody_atoms, secrets)
  val legendre_bits = range(CUSTODY_PROBABILITY_EXPONENT).map { i -> legendre_bit(((uhf + secrets[0uL]) + i), CUSTODY_PRIME) }.toPyList()
  return all(legendre_bits)
}

fun get_randao_epoch_for_custody_period(period: uint64, validator_index: ValidatorIndex): Epoch {
  val next_period_start = (((period + 1uL) * EPOCHS_PER_CUSTODY_PERIOD) - (validator_index % EPOCHS_PER_CUSTODY_PERIOD))
  return Epoch((next_period_start + CUSTODY_PERIOD_TO_RANDAO_PADDING))
}

/*
    Return the reveal period for a given validator.
    */
fun get_custody_period_for_validator(validator_index: ValidatorIndex, epoch: Epoch): pyint {
  return pyint((epoch + (validator_index % EPOCHS_PER_CUSTODY_PERIOD)) / EPOCHS_PER_CUSTODY_PERIOD)
}

fun process_custody_game_operations(state: BeaconState, body: BeaconBlockBody): Unit {
  fun <T> for_ops(operations: Sequence<T>, fn: (BeaconState, T) -> Unit): Unit {
    for (operation in operations) {
      fn(state, operation)
    }
  }
  for_ops(body.chunk_challenges, ::process_chunk_challenge)
  for_ops(body.chunk_challenge_responses, ::process_chunk_challenge_response)
  for_ops(body.custody_key_reveals, ::process_custody_key_reveal)
  for_ops(body.early_derived_secret_reveals, ::process_early_derived_secret_reveal)
  for_ops(body.custody_slashings, ::process_custody_slashing)
}

fun process_chunk_challenge(state: BeaconState, challenge: CustodyChunkChallenge): Unit {
  assert(is_valid_indexed_attestation(state, get_indexed_attestation(state, challenge.attestation)))
  val max_attestation_challenge_epoch = (challenge.attestation.data.target.epoch + MAX_CHUNK_CHALLENGE_DELAY)
  assert((get_current_epoch(state) <= max_attestation_challenge_epoch))
  val responder = state.validators[challenge.responder_index]
  if ((responder.exit_epoch < FAR_FUTURE_EPOCH)) {
    assert((get_current_epoch(state) <= (responder.exit_epoch + MAX_CHUNK_CHALLENGE_DELAY)))
  }
  assert(is_slashable_validator(responder, get_current_epoch(state)))
  val attesters = get_attesting_indices(state, challenge.attestation.data, challenge.attestation.aggregation_bits)
  assert((challenge.responder_index in attesters))
  assert((hash_tree_root(challenge.shard_transition) == challenge.attestation.data.shard_transition_root))
  val data_root = challenge.shard_transition.shard_data_roots[challenge.data_index]
  for (record in state.custody_chunk_challenge_records) {
    assert((record.data_root != data_root) || (record.chunk_index != challenge.chunk_index))
  }
  val shard_block_length = challenge.shard_transition.shard_block_lengths[challenge.data_index]
  val transition_chunks = (((shard_block_length + BYTES_PER_CUSTODY_CHUNK) - 1uL) / BYTES_PER_CUSTODY_CHUNK)
  assert((challenge.chunk_index < transition_chunks))
  val new_record = CustodyChunkChallengeRecord(challenge_index = state.custody_chunk_challenge_index, challenger_index = get_beacon_proposer_index(state), responder_index = challenge.responder_index, inclusion_epoch = get_current_epoch(state), data_root = challenge.shard_transition.shard_data_roots[challenge.data_index], chunk_index = challenge.chunk_index)
  replace_empty_or_append(state.custody_chunk_challenge_records, new_record)
  state.custody_chunk_challenge_index += 1uL
  responder.withdrawable_epoch = FAR_FUTURE_EPOCH
}

fun process_chunk_challenge_response(state: BeaconState, response: CustodyChunkResponse): Unit {
  val matching_challenges = state.custody_chunk_challenge_records.filter { record -> (record.challenge_index == response.challenge_index) }.map { record -> record }.toPyList()
  assert((len(matching_challenges) == 1uL))
  val challenge = matching_challenges[0uL]
  assert((response.chunk_index == challenge.chunk_index))
  assert(is_valid_merkle_branch(leaf = hash_tree_root(response.chunk), branch = response.branch, depth = uint64(CUSTODY_RESPONSE_DEPTH), index = response.chunk_index, root = challenge.data_root))
  val index_in_records = state.custody_chunk_challenge_records.index(challenge)
  state.custody_chunk_challenge_records[index_in_records] = CustodyChunkChallengeRecord()
  val proposer_index = get_beacon_proposer_index(state)
  increase_balance(state, proposer_index, Gwei((get_base_reward(state, proposer_index) / MINOR_REWARD_QUOTIENT)))
}

/*
    Process ``CustodyKeyReveal`` operation.
    Note that this function mutates ``state``.
    */
fun process_custody_key_reveal(state: BeaconState, reveal: CustodyKeyReveal): Unit {
  val revealer = state.validators[reveal.revealer_index]
  val epoch_to_sign = get_randao_epoch_for_custody_period(revealer.next_custody_secret_to_reveal, reveal.revealer_index)
  val custody_reveal_period = get_custody_period_for_validator(reveal.revealer_index, get_current_epoch(state))
  val is_past_reveal = (revealer.next_custody_secret_to_reveal < uint64(custody_reveal_period))
  val is_exited = (revealer.exit_epoch <= get_current_epoch(state))
  val is_exit_period_reveal = (revealer.next_custody_secret_to_reveal == uint64(get_custody_period_for_validator(reveal.revealer_index, (revealer.exit_epoch - 1uL))))
  assert(is_past_reveal || is_exited && is_exit_period_reveal)
  assert(is_slashable_validator(revealer, get_current_epoch(state)))
  val domain = get_domain(state, DOMAIN_RANDAO, epoch_to_sign)
  val signing_root = compute_signing_root(epoch_to_sign, domain)
  assert(bls.Verify(revealer.pubkey, signing_root, reveal.reveal))
  if (is_exited && is_exit_period_reveal) {
    revealer.all_custody_secrets_revealed_epoch = get_current_epoch(state)
  }
  revealer.next_custody_secret_to_reveal += 1uL
  val proposer_index = get_beacon_proposer_index(state)
  increase_balance(state, proposer_index, Gwei((get_base_reward(state, reveal.revealer_index) / MINOR_REWARD_QUOTIENT)))
}

/*
    Process ``EarlyDerivedSecretReveal`` operation.
    Note that this function mutates ``state``.
    */
fun process_early_derived_secret_reveal(state: BeaconState, reveal: EarlyDerivedSecretReveal): Unit {
  val revealed_validator = state.validators[reveal.revealed_index]
  val derived_secret_location = (reveal.epoch % EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS)
  assert((reveal.epoch >= (get_current_epoch(state) + RANDAO_PENALTY_EPOCHS)))
  assert((reveal.epoch < (get_current_epoch(state) + EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS)))
  assert(!(revealed_validator.slashed))
  assert((reveal.revealed_index !in state.exposed_derived_secrets[derived_secret_location]))
  val masker = state.validators[reveal.masker_index]
  val pubkeys = PyList(revealed_validator.pubkey, masker.pubkey)
  val domain = get_domain(state, DOMAIN_RANDAO, reveal.epoch)
  val signing_roots = PyList(hash_tree_root(reveal.epoch), reveal.mask).map { root -> compute_signing_root(root, domain) }.toPyList()
  assert(bls.AggregateVerify(pubkeys, signing_roots, reveal.reveal))
  if ((reveal.epoch >= (get_current_epoch(state) + CUSTODY_PERIOD_TO_RANDAO_PADDING))) {
    slash_validator(state, reveal.revealed_index, reveal.masker_index)
  } else {
    val max_proposer_slot_reward = (((get_base_reward(state, reveal.revealed_index) * SLOTS_PER_EPOCH) / len(get_active_validator_indices(state, get_current_epoch(state)))) / PROPOSER_REWARD_QUOTIENT)
    val penalty = Gwei(((max_proposer_slot_reward * EARLY_DERIVED_SECRET_REVEAL_SLOT_REWARD_MULTIPLE) * (len(state.exposed_derived_secrets[derived_secret_location]) + 1uL)))
    val proposer_index = get_beacon_proposer_index(state)
    val whistleblower_index = reveal.masker_index
    val whistleblowing_reward = Gwei((penalty / WHISTLEBLOWER_REWARD_QUOTIENT))
    val proposer_reward = Gwei((whistleblowing_reward / PROPOSER_REWARD_QUOTIENT))
    increase_balance(state, proposer_index, proposer_reward)
    increase_balance(state, whistleblower_index, (whistleblowing_reward - proposer_reward))
    decrease_balance(state, reveal.revealed_index, penalty)
    state.exposed_derived_secrets[derived_secret_location].append(reveal.revealed_index)
  }
}

fun process_custody_slashing(state: BeaconState, signed_custody_slashing: SignedCustodySlashing): Unit {
  val custody_slashing = signed_custody_slashing.message
  val attestation = custody_slashing.attestation
  val malefactor = state.validators[custody_slashing.malefactor_index]
  val whistleblower = state.validators[custody_slashing.whistleblower_index]
  var domain = get_domain(state, DOMAIN_CUSTODY_BIT_SLASHING, get_current_epoch(state))
  var signing_root = compute_signing_root(custody_slashing, domain)
  assert(bls.Verify(whistleblower.pubkey, signing_root, signed_custody_slashing.signature))
  assert(is_slashable_validator(whistleblower, get_current_epoch(state)))
  assert(is_slashable_validator(malefactor, get_current_epoch(state)))
  assert(is_valid_indexed_attestation(state, get_indexed_attestation(state, attestation)))
  val shard_transition = custody_slashing.shard_transition
  assert((hash_tree_root(shard_transition) == attestation.data.shard_transition_root))
  assert((get_block_data_merkle_root(custody_slashing.data) == shard_transition.shard_data_roots[custody_slashing.data_index]))
  assert((len(custody_slashing.data) == shard_transition.shard_block_lengths[custody_slashing.data_index]))
  val attesters = get_attesting_indices(state, attestation.data, attestation.aggregation_bits)
  assert((custody_slashing.malefactor_index in attesters))
  val epoch_to_sign = get_randao_epoch_for_custody_period(uint64(get_custody_period_for_validator(custody_slashing.malefactor_index, attestation.data.target.epoch)), custody_slashing.malefactor_index)
  domain = get_domain(state, DOMAIN_RANDAO, epoch_to_sign)
  signing_root = compute_signing_root(epoch_to_sign, domain)
  assert(bls.Verify(malefactor.pubkey, signing_root, custody_slashing.malefactor_secret))
  val computed_custody_bit = compute_custody_bit(custody_slashing.malefactor_secret, custody_slashing.data)
  if ((computed_custody_bit == pybool(1uL))) {
    slash_validator(state, custody_slashing.malefactor_index)
    val committee = get_beacon_committee(state, attestation.data.slot, attestation.data.index)
    val others_count = (len(committee) - 1uL)
    val whistleblower_reward = Gwei(((malefactor.effective_balance / WHISTLEBLOWER_REWARD_QUOTIENT) / others_count))
    for (attester_index in attesters) {
      if ((attester_index != custody_slashing.malefactor_index)) {
        increase_balance(state, attester_index, whistleblower_reward)
      }
    }
  } else {
    slash_validator(state, custody_slashing.whistleblower_index)
  }
}

fun process_reveal_deadlines(state: BeaconState): Unit {
  val epoch = get_current_epoch(state)
  for ((index, validator) in enumerate(state.validators)) {
    val deadline = (validator.next_custody_secret_to_reveal + 1uL)
    if ((get_custody_period_for_validator(ValidatorIndex(index), epoch) > deadline)) {
      slash_validator(state, ValidatorIndex(index))
    }
  }
}

fun process_challenge_deadlines(state: BeaconState): Unit {
  for (custody_chunk_challenge in state.custody_chunk_challenge_records) {
    if ((get_current_epoch(state) > (custody_chunk_challenge.inclusion_epoch + EPOCHS_PER_CUSTODY_PERIOD))) {
      slash_validator(state, custody_chunk_challenge.responder_index, custody_chunk_challenge.challenger_index)
      val index_in_records = state.custody_chunk_challenge_records.index(custody_chunk_challenge)
      state.custody_chunk_challenge_records[index_in_records] = CustodyChunkChallengeRecord()
    }
  }
}

fun process_custody_final_updates(state: BeaconState): Unit {
  state.exposed_derived_secrets[(get_current_epoch(state) % EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS)] = PyList()
  val records = state.custody_chunk_challenge_records
  val validator_indices_in_records = set(records.map { record -> record.responder_index })
  for ((index, validator) in enumerate(state.validators)) {
    if ((validator.exit_epoch != FAR_FUTURE_EPOCH)) {
      val not_all_secrets_are_revealed = (validator.all_custody_secrets_revealed_epoch == FAR_FUTURE_EPOCH)
      if ((ValidatorIndex(index) in validator_indices_in_records) || not_all_secrets_are_revealed) {
        validator.withdrawable_epoch = FAR_FUTURE_EPOCH
      } else {
        if ((validator.withdrawable_epoch == FAR_FUTURE_EPOCH)) {
          validator.withdrawable_epoch = Epoch((validator.all_custody_secrets_revealed_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY))
        }
      }
    }
  }
}

fun compute_previous_slot(slot: Slot): Slot {
  if ((slot > 0uL)) {
    return Slot((slot - 1uL))
  } else {
    return Slot(0uL)
  }
}

/*
    Create a compact validator object representing index, slashed status, and compressed balance.
    Takes as input balance-in-increments (// EFFECTIVE_BALANCE_INCREMENT) to preserve symmetry with
    the unpacking function.
    */
fun pack_compact_validator(index: ValidatorIndex, slashed: pybool, balance_in_increments: uint64): uint64 {
  return (((index shl 16uL) + (slashed shl 15uL)) + balance_in_increments)
}

/*
    Return validator index, slashed, balance // EFFECTIVE_BALANCE_INCREMENT
    */
fun unpack_compact_validator(compact_validator: uint64): Triple<ValidatorIndex, pybool, uint64> {
  return Triple(ValidatorIndex((compact_validator shr 16uL)), pybool(((compact_validator shr 15uL) % 2uL)), (compact_validator and ((2uL.pow(15uL)) - 1uL)))
}

/*
    Given a state and a list of validator indices, outputs the ``CompactCommittee`` representing them.
    */
fun committee_to_compact_committee(state: BeaconState, committee: Sequence<ValidatorIndex>): CompactCommittee {
  val validators = committee.map { i -> state.validators[i] }.toPyList()
  val compact_validators = zip(committee, validators).map { (i, v) -> pack_compact_validator(i, v.slashed, (v.effective_balance / EFFECTIVE_BALANCE_INCREMENT)) }.toPyList()
  val pubkeys = validators.map { v -> v.pubkey }.toPyList()
  return CompactCommittee(pubkeys = pubkeys, compact_validators = compact_validators)
}

fun compute_shard_from_committee_index(state: BeaconState, index: CommitteeIndex, slot: Slot): Shard {
  val active_shards = get_active_shard_count(state)
  return Shard(((index + get_start_shard(state, slot)) % active_shards))
}

/*
    Return the offset slots that are greater than ``start_slot`` and less than ``end_slot``.
    */
fun compute_offset_slots(start_slot: Slot, end_slot: Slot): Sequence<Slot> {
  return SHARD_BLOCK_OFFSETS.filter { x -> ((start_slot + x) < end_slot) }.map { x -> Slot((start_slot + x)) }.toPyList()
}

fun compute_updated_gasprice(prev_gasprice: Gwei, shard_block_length: uint64): Gwei {
  if ((shard_block_length > TARGET_SHARD_BLOCK_SIZE)) {
    val delta = (((prev_gasprice * (shard_block_length - TARGET_SHARD_BLOCK_SIZE)) / TARGET_SHARD_BLOCK_SIZE) / GASPRICE_ADJUSTMENT_COEFFICIENT)
    return min((prev_gasprice + delta), MAX_GASPRICE)
  } else {
    val delta = (((prev_gasprice * (TARGET_SHARD_BLOCK_SIZE - shard_block_length)) / TARGET_SHARD_BLOCK_SIZE) / GASPRICE_ADJUSTMENT_COEFFICIENT)
    return (max(prev_gasprice, (MIN_GASPRICE + delta)) - delta)
  }
}

/*
    Return the source epoch for computing the committee.
    */
fun compute_committee_source_epoch(epoch: Epoch, period: uint64): Epoch {
  var source_epoch = (epoch - (epoch % period))
  if ((source_epoch >= period)) {
    source_epoch -= period
  }
  return source_epoch
}

fun get_active_shard_count(state: BeaconState): uint64 {
  return len(state.shard_states)
}

fun get_online_validator_indices(state: BeaconState): Set<ValidatorIndex> {
  val active_validators = get_active_validator_indices(state, get_current_epoch(state))
  return set(active_validators.filter { i -> (state.online_countdown[i] != 0uL.toUByte()) }.map { i -> i })
}

/*
    Return the shard committee of the given ``epoch`` of the given ``shard``.
    */
fun get_shard_committee(beacon_state: BeaconState, epoch: Epoch, shard: Shard): Sequence<ValidatorIndex> {
  val source_epoch = compute_committee_source_epoch(epoch, SHARD_COMMITTEE_PERIOD)
  val active_validator_indices = get_active_validator_indices(beacon_state, source_epoch)
  val seed = get_seed(beacon_state, source_epoch, DOMAIN_SHARD_COMMITTEE)
  val active_shard_count = get_active_shard_count(beacon_state)
  return compute_committee(indices = active_validator_indices, seed = seed, index = shard, count = active_shard_count)
}

/*
    Return the light client committee of no more than ``LIGHT_CLIENT_COMMITTEE_SIZE`` validators.
    */
fun get_light_client_committee(beacon_state: BeaconState, epoch: Epoch): Sequence<ValidatorIndex> {
  val source_epoch = compute_committee_source_epoch(epoch, LIGHT_CLIENT_COMMITTEE_PERIOD)
  val active_validator_indices = get_active_validator_indices(beacon_state, source_epoch)
  val seed = get_seed(beacon_state, source_epoch, DOMAIN_LIGHT_CLIENT)
  return compute_committee(indices = active_validator_indices, seed = seed, index = 0uL, count = get_active_shard_count(beacon_state)).slice(0uL, LIGHT_CLIENT_COMMITTEE_SIZE)
}

/*
    Return the proposer's index of shard block at ``slot``.
    */
fun get_shard_proposer_index(beacon_state: BeaconState, slot: Slot, shard: Shard): ValidatorIndex {
  val epoch = compute_epoch_at_slot(slot)
  val committee = get_shard_committee(beacon_state, epoch, shard)
  val seed = hash((get_seed(beacon_state, epoch, DOMAIN_SHARD_COMMITTEE) + int_to_bytes(slot, length = 8uL)))
  val r = bytes_to_int(seed.slice(0uL, 8uL))
  return committee[(r % len(committee))]
}

/*
    Return the sum of committee counts in range ``listOf(start_slot, stop_slot)``.
    */
fun get_committee_count_delta(state: BeaconState, start_slot: Slot, stop_slot: Slot): uint64 {
  return sum(range(start_slot, stop_slot).map { slot -> get_committee_count_per_slot(state, compute_epoch_at_slot(Slot(slot))) })
}

/*
    Return the start shard at ``slot``.
    */
fun get_start_shard(state: BeaconState, slot: Slot): Shard {
  val current_epoch_start_slot = compute_start_slot_at_epoch(get_current_epoch(state))
  val active_shard_count = get_active_shard_count(state)
  if ((current_epoch_start_slot == slot)) {
    return state.current_epoch_start_shard
  } else {
    if ((slot > current_epoch_start_slot)) {
      val shard_delta = get_committee_count_delta(state, start_slot = current_epoch_start_slot, stop_slot = slot)
      return Shard(((state.current_epoch_start_shard + shard_delta) % active_shard_count))
    } else {
      val shard_delta = get_committee_count_delta(state, start_slot = slot, stop_slot = current_epoch_start_slot)
      val max_committees_per_epoch = (MAX_COMMITTEES_PER_SLOT * SLOTS_PER_EPOCH)
      return Shard((((state.current_epoch_start_shard + (max_committees_per_epoch * active_shard_count)) - shard_delta) % active_shard_count))
    }
  }
}

/*
    Return the latest slot number of the given ``shard``.
    */
fun get_latest_slot_for_shard(state: BeaconState, shard: Shard): Slot {
  return state.shard_states[shard].slot
}

/*
    Return the offset slots of the given ``shard``.
    The offset slot are after the latest slot and before current slot.
    */
fun get_offset_slots(state: BeaconState, shard: Shard): Sequence<Slot> {
  return compute_offset_slots(get_latest_slot_for_shard(state, shard), state.slot)
}

/*
    Check if the given ``attestation_data`` is on-time.
    */
fun is_on_time_attestation(state: BeaconState, attestation_data: AttestationData): pybool {
  return (attestation_data.slot == compute_previous_slot(state.slot))
}

/*
    Check if on-time ``attestation`` helped contribute to the successful crosslink of
    ``winning_root`` formed by ``committee_index`` committee.
    */
fun is_winning_attestation(state: BeaconState, attestation: PendingAttestation, committee_index: CommitteeIndex, winning_root: Root): pybool {
  return is_on_time_attestation(state, attestation.data) && (attestation.data.index == committee_index) && (attestation.data.shard_transition_root == winning_root)
}

/*
    If ``pubkeys`` is an empty list, the given ``signature`` should be a stub ``NO_SIGNATURE``.
    Otherwise, verify it with standard BLS AggregateVerify API.
    */
fun optional_aggregate_verify(pubkeys: Sequence<BLSPubkey>, messages: Sequence<Bytes32>, signature: BLSSignature): pybool {
  if ((len(pubkeys) == 0uL)) {
    return (signature == NO_SIGNATURE)
  } else {
    return bls.AggregateVerify(pubkeys, messages, signature)
  }
}

/*
    If ``pubkeys`` is an empty list, the given ``signature`` should be a stub ``NO_SIGNATURE``.
    Otherwise, verify it with standard BLS FastAggregateVerify API.
    */
fun optional_fast_aggregate_verify(pubkeys: Sequence<BLSPubkey>, message: Bytes32, signature: BLSSignature): pybool {
  if ((len(pubkeys) == 0uL)) {
    return (signature == NO_SIGNATURE)
  } else {
    return bls.FastAggregateVerify(pubkeys, message, signature)
  }
}

fun validate_attestation(state: BeaconState, attestation: Attestation): Unit {
  val data = attestation.data
  assert((data.index < get_committee_count_per_slot(state, data.target.epoch)))
  assert((data.index < get_active_shard_count(state)))
  assert((data.target.epoch in Pair(get_previous_epoch(state), get_current_epoch(state))))
  assert((data.target.epoch == compute_epoch_at_slot(data.slot)))
  assert(((data.slot + MIN_ATTESTATION_INCLUSION_DELAY) <= state.slot) && (state.slot <= (data.slot + SLOTS_PER_EPOCH)))
  val committee = get_beacon_committee(state, data.slot, data.index)
  assert((len(attestation.aggregation_bits) == len(committee)))
  if ((attestation.data.target.epoch == get_current_epoch(state))) {
    assert((attestation.data.source == state.current_justified_checkpoint))
  } else {
    assert((attestation.data.source == state.previous_justified_checkpoint))
  }
  if (is_on_time_attestation(state, attestation.data)) {
    assert((data.beacon_block_root == get_block_root_at_slot(state, compute_previous_slot(state.slot))))
    val shard = compute_shard_from_committee_index(state, attestation.data.index, attestation.data.slot)
    assert((attestation.data.shard == shard))
    assert((attestation.data.shard_transition_root != hash_tree_root(ShardTransition())))
  } else {
    assert((data.slot < compute_previous_slot(state.slot)))
    assert((data.shard_transition_root == Root()))
  }
  assert(is_valid_indexed_attestation(state, get_indexed_attestation(state, attestation)))
}

fun apply_shard_transition(state: BeaconState, shard: Shard, transition: ShardTransition): Unit {
  assert((state.slot > PHASE_1_GENESIS_SLOT))
  val offset_slots = get_offset_slots(state, shard)
  assert((len(transition.shard_data_roots) == len(transition.shard_states)) && (len(transition.shard_states) == len(transition.shard_block_lengths)) && (len(transition.shard_block_lengths) == len(offset_slots)))
  assert((transition.start_slot == offset_slots[0uL]))
  val headers: PyList<ShardBlockHeader> = PyList()
  val proposers: PyList<ValidatorIndex> = PyList()
  var prev_gasprice = state.shard_states[shard].gasprice
  var shard_parent_root = state.shard_states[shard].latest_block_root
  for ((i, offset_slot) in enumerate(offset_slots)) {
    val shard_block_length = transition.shard_block_lengths[i]
    val shard_state = transition.shard_states[i]
    assert((shard_state.gasprice == compute_updated_gasprice(prev_gasprice, shard_block_length)))
    assert((shard_state.slot == offset_slot))
    val is_empty_proposal = (shard_block_length == 0uL)
    if (!(is_empty_proposal)) {
      val proposal_index = get_shard_proposer_index(state, offset_slot, shard)
      val header = ShardBlockHeader(shard_parent_root = shard_parent_root, beacon_parent_root = get_block_root_at_slot(state, offset_slot), slot = offset_slot, shard = shard, proposer_index = proposal_index, body_root = transition.shard_data_roots[i])
      shard_parent_root = hash_tree_root(header)
      headers.append(header)
      proposers.append(proposal_index)
    } else {
      assert((transition.shard_data_roots[i] == Root()))
    }
    prev_gasprice = shard_state.gasprice
  }
  val pubkeys = proposers.map { proposer -> state.validators[proposer].pubkey }.toPyList()
  val signing_roots = headers.map { header -> compute_signing_root(header, get_domain(state, DOMAIN_SHARD_PROPOSAL, compute_epoch_at_slot(header.slot))) }.toPyList()
  assert(optional_aggregate_verify(pubkeys, signing_roots, transition.proposer_signature_aggregate))
  state.shard_states[shard] = transition.shard_states[(len(transition.shard_states) - 1uL)]
  state.shard_states[shard].slot = compute_previous_slot(state.slot)
}

fun process_crosslink_for_shard(state: BeaconState, committee_index: CommitteeIndex, shard_transition: ShardTransition, attestations: Sequence<Attestation>): Root {
  val on_time_attestation_slot = compute_previous_slot(state.slot)
  val committee = get_beacon_committee(state, on_time_attestation_slot, committee_index)
  val online_indices = get_online_validator_indices(state)
  val shard = compute_shard_from_committee_index(state, committee_index, on_time_attestation_slot)
  val shard_transition_roots = set(attestations.map { a -> a.data.shard_transition_root })
  for (shard_transition_root in sorted(shard_transition_roots)) {
    val transition_attestations = attestations.filter { a -> (a.data.shard_transition_root == shard_transition_root) }.map { a -> a }.toPyList()
    var transition_participants: Set<ValidatorIndex> = set()
    for (attestation in transition_attestations) {
      val participants = get_attesting_indices(state, attestation.data, attestation.aggregation_bits)
      transition_participants = transition_participants.union(participants)
    }
    val enough_online_stake = ((get_total_balance(state, online_indices.intersection(transition_participants)) * 3uL) >= (get_total_balance(state, online_indices.intersection(committee)) * 2uL))
    if (!(enough_online_stake)) {
      continue
    }
    assert((shard_transition_root == hash_tree_root(shard_transition)))
    val last_offset_index = (len(shard_transition.shard_states) - 1uL)
    val shard_head_root = shard_transition.shard_states[last_offset_index].latest_block_root
    for (attestation in transition_attestations) {
      assert((attestation.data.shard_head_root == shard_head_root))
    }
    apply_shard_transition(state, shard, shard_transition)
    val beacon_proposer_index = get_beacon_proposer_index(state)
    val estimated_attester_reward = sum(transition_participants.map { attester -> get_base_reward(state, attester) }.toPyList())
    val proposer_reward = Gwei((estimated_attester_reward / PROPOSER_REWARD_QUOTIENT))
    increase_balance(state, beacon_proposer_index, proposer_reward)
    val states_slots_lengths = zip(shard_transition.shard_states, get_offset_slots(state, shard), shard_transition.shard_block_lengths)
    for ((shard_state, slot, length) in states_slots_lengths) {
      val proposer_index = get_shard_proposer_index(state, slot, shard)
      decrease_balance(state, proposer_index, (shard_state.gasprice * length))
    }
    return shard_transition_root
  }
  assert((shard_transition == ShardTransition()))
  return Root()
}

fun process_crosslinks(state: BeaconState, shard_transitions: Sequence<ShardTransition>, attestations: Sequence<Attestation>): Unit {
  val on_time_attestation_slot = compute_previous_slot(state.slot)
  val committee_count = get_committee_count_per_slot(state, compute_epoch_at_slot(on_time_attestation_slot))
  for (committee_index in map({ index -> CommitteeIndex(index) }, range(committee_count))) {
    val shard = compute_shard_from_committee_index(state, committee_index, on_time_attestation_slot)
    val shard_attestations = attestations.filter { attestation -> is_on_time_attestation(state, attestation.data) && (attestation.data.index == committee_index) }.map { attestation -> attestation }.toPyList()
    val winning_root = process_crosslink_for_shard(state, committee_index, shard_transitions[shard], shard_attestations)
    if ((winning_root != Root())) {
      for (pending_attestation in state.current_epoch_attestations) {
        if (is_winning_attestation(state, pending_attestation, committee_index, winning_root)) {
          pending_attestation.crosslink_success = true
        }
      }
    }
  }
}

/*
    Verify that a `shard_transition` in a block is empty if an attestation was not processed for it.
    */
fun verify_empty_shard_transition(state: BeaconState, shard_transitions: Sequence<ShardTransition>): pybool {
  for (shard in range(get_active_shard_count(state))) {
    if ((state.shard_states[shard].slot != compute_previous_slot(state.slot))) {
      if ((shard_transitions[shard] != ShardTransition())) {
        return false
      }
    }
  }
  return true
}

fun process_shard_transitions(state: BeaconState, shard_transitions: Sequence<ShardTransition>, attestations: Sequence<Attestation>): Unit {
  process_crosslinks(state, shard_transitions, attestations)
  assert(verify_empty_shard_transition(state, shard_transitions))
}

fun process_light_client_aggregate(state: BeaconState, block_body: BeaconBlockBody): Unit {
  val committee = get_light_client_committee(state, get_current_epoch(state))
  val previous_slot = compute_previous_slot(state.slot)
  val previous_block_root = get_block_root_at_slot(state, previous_slot)
  var total_reward = Gwei(0uL)
  val signer_pubkeys: PyList<BLSPubkey> = PyList()
  for ((bit_index, participant_index) in enumerate(committee)) {
    if (block_body.light_client_bits[bit_index]) {
      signer_pubkeys.append(state.validators[participant_index].pubkey)
      if (!(state.validators[participant_index].slashed)) {
        increase_balance(state, participant_index, get_base_reward(state, participant_index))
        total_reward += get_base_reward(state, participant_index)
      }
    }
  }
  increase_balance(state, get_beacon_proposer_index(state), Gwei((total_reward / PROPOSER_REWARD_QUOTIENT)))
  val signing_root = compute_signing_root(previous_block_root, get_domain(state, DOMAIN_LIGHT_CLIENT, compute_epoch_at_slot(previous_slot)))
  assert(optional_fast_aggregate_verify(signer_pubkeys, signing_root, block_body.light_client_signature))
}

fun process_phase_1_final_updates(state: BeaconState): Unit {
  process_custody_final_updates(state)
  process_online_tracking(state)
  process_light_client_committee_updates(state)
  state.current_epoch_start_shard = get_start_shard(state, Slot((state.slot + 1uL)))
}

fun process_online_tracking(state: BeaconState): Unit {
  for (index in range(len(state.validators))) {
    if ((state.online_countdown[index] != 0uL.toUByte())) {
      state.online_countdown[index] = (state.online_countdown[index] - 1u.toUByte()).toUByte()
    }
  }
  for (pending_attestation in (state.current_epoch_attestations + state.previous_epoch_attestations)) {
    for (index in get_attesting_indices(state, pending_attestation.data, pending_attestation.aggregation_bits)) {
      state.online_countdown[index] = ONLINE_PERIOD
    }
  }
}

/*
    Update light client committees.
    */
fun process_light_client_committee_updates(state: BeaconState): Unit {
  val next_epoch = compute_epoch_at_slot(Slot((state.slot + 1uL)))
  if (((next_epoch % LIGHT_CLIENT_COMMITTEE_PERIOD) == 0uL)) {
    state.current_light_committee = state.next_light_committee
    val new_committee = get_light_client_committee(state, (next_epoch + LIGHT_CLIENT_COMMITTEE_PERIOD))
    state.next_light_committee = committee_to_compact_committee(state, new_committee)
  }
}

fun verify_shard_block_message(beacon_parent_state: BeaconState, shard_parent_state: ShardState, block: ShardBlock): pybool {
  assert((block.shard_parent_root == shard_parent_state.latest_block_root))
  val beacon_parent_block_header = beacon_parent_state.latest_block_header.copy()
  if ((beacon_parent_block_header.state_root == Root())) {
    beacon_parent_block_header.state_root = hash_tree_root(beacon_parent_state)
  }
  val beacon_parent_root = hash_tree_root(beacon_parent_block_header)
  assert((block.beacon_parent_root == beacon_parent_root))
  val shard = block.shard
  val next_slot = Slot((block.slot + 1uL))
  val offset_slots = compute_offset_slots(get_latest_slot_for_shard(beacon_parent_state, shard), next_slot)
  assert((block.slot in offset_slots))
  assert((block.proposer_index == get_shard_proposer_index(beacon_parent_state, block.slot, shard)))
  assert((0uL < len(block.body)) && (len(block.body) <= MAX_SHARD_BLOCK_SIZE))
  return true
}

fun verify_shard_block_signature(beacon_parent_state: BeaconState, signed_block: SignedShardBlock): pybool {
  val proposer = beacon_parent_state.validators[signed_block.message.proposer_index]
  val domain = get_domain(beacon_parent_state, DOMAIN_SHARD_PROPOSAL, compute_epoch_at_slot(signed_block.message.slot))
  val signing_root = compute_signing_root(signed_block.message, domain)
  return bls.Verify(proposer.pubkey, signing_root, signed_block.signature)
}

fun shard_state_transition(shard_state: ShardState, signed_block: SignedShardBlock, beacon_parent_state: BeaconState, validate_result: pybool = true): ShardState {
  assert(verify_shard_block_message(beacon_parent_state, shard_state, signed_block.message))
  if (validate_result) {
    assert(verify_shard_block_signature(beacon_parent_state, signed_block))
  }
  process_shard_block(shard_state, signed_block.message)
  return shard_state
}

/*
    Update ``shard_state`` with shard ``block``.
    */
fun process_shard_block(shard_state: ShardState, block: ShardBlock): Unit {
  shard_state.slot = block.slot
  val prev_gasprice = shard_state.gasprice
  val shard_block_length = len(block.body)
  shard_state.gasprice = compute_updated_gasprice(prev_gasprice, uint64(shard_block_length))
  if ((shard_block_length != 0uL)) {
    shard_state.latest_block_root = hash_tree_root(block)
  }
}

fun is_valid_fraud_proof(beacon_state: BeaconState, attestation: Attestation, offset_index: uint64, transition: ShardTransition, block: ShardBlock, subkey: BLSPubkey, beacon_parent_block: BeaconBlock): pybool {
  val custody_bits = attestation.custody_bits_blocks
  for (j in range(len(custody_bits[offset_index]))) {
    if ((custody_bits[offset_index][j] != generate_custody_bit(subkey, block))) {
      return true
    }
  }
  var shard_state: ShardState
  if ((offset_index == 0uL)) {
    val shard_states = beacon_parent_block.body.shard_transitions[attestation.data.shard].shard_states
    shard_state = shard_states[(len(shard_states) - 1uL)]
  } else {
    shard_state = transition.shard_states[(offset_index - 1uL)]
  }
  process_shard_block(shard_state, block)
  if ((shard_state != transition.shard_states[offset_index])) {
    return true
  }
  return false
}

fun generate_custody_bit(subkey: BLSPubkey, block: ShardBlock): pybool {
  TODO("...")
}

fun phase0.BeaconBlockHeader.toPhase1() = BeaconBlockHeader(slot = this.slot, proposer_index = this.proposer_index, parent_root = this.parent_root, state_root = this.state_root, body_root = this.body_root)
fun phase0.Eth1Data.toPhase1() = Eth1Data(deposit_root = this.deposit_root, deposit_count = this.deposit_count, block_hash = this.block_hash)
fun phase0.Checkpoint.toPhase1() = Checkpoint(epoch = this.epoch, root = this.root)

fun upgrade_to_phase1(pre: phase0.BeaconState): BeaconState {
  var epoch = phase0.get_current_epoch(pre)
  var post = BeaconState(
      genesis_time = pre.genesis_time,
      slot = pre.slot,
      fork = Fork(
          previous_version = pre.fork.current_version,
          current_version = PHASE_1_FORK_VERSION,
          epoch = epoch),
      latest_block_header = pre.latest_block_header.toPhase1(),
      block_roots = pre.block_roots,
      state_roots = pre.state_roots,
      historical_roots = pre.historical_roots,
      eth1_data = pre.eth1_data.toPhase1(),
      eth1_data_votes = pre.eth1_data_votes.map {it.toPhase1()}.toPyList(),
      eth1_deposit_index = pre.eth1_deposit_index,
      validators = SSZList<Validator>(enumerate(pre.validators).map { (i, phase0_validator) ->
        Validator(
            pubkey = phase0_validator.pubkey,
            withdrawal_credentials = phase0_validator.withdrawal_credentials,
            effective_balance = phase0_validator.effective_balance,
            slashed = phase0_validator.slashed,
            activation_eligibility_epoch = phase0_validator.activation_eligibility_epoch,
            activation_epoch = phase0_validator.activation_eligibility_epoch,
            exit_epoch = phase0_validator.exit_epoch,
            withdrawable_epoch = phase0_validator.withdrawable_epoch,
            next_custody_secret_to_reveal = uint64(get_custody_period_for_validator(ValidatorIndex(i), epoch)),
            all_custody_secrets_revealed_epoch = FAR_FUTURE_EPOCH)
      }.toPyList()),
      balances = pre.balances,
      randao_mixes = pre.randao_mixes,
      slashings = pre.slashings,
      previous_epoch_attestations = SSZList<PendingAttestation>(),
      current_epoch_attestations = SSZList<PendingAttestation>(),
      justification_bits = pre.justification_bits,
      previous_justified_checkpoint = pre.previous_justified_checkpoint.toPhase1(),
      current_justified_checkpoint = pre.current_justified_checkpoint.toPhase1(),
      finalized_checkpoint = pre.finalized_checkpoint.toPhase1(),
      current_epoch_start_shard = Shard(0uL),
      shard_states = SSZList<ShardState>(range(INITIAL_ACTIVE_SHARDS).map { ShardState(slot = pre.slot, gasprice = MIN_GASPRICE, latest_block_root = Root()) }.toPyList()),
      online_countdown = (PyList(ONLINE_PERIOD) * len(pre.validators)),
      current_light_committee = CompactCommittee(),
      next_light_committee = CompactCommittee(),
      exposed_derived_secrets = (PyList(SSZList<ValidatorIndex>()) * EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS)
  )
  val next_epoch = Epoch((epoch + 1uL))
  post.current_light_committee = committee_to_compact_committee(post, get_light_client_committee(post, epoch))
  post.next_light_committee = committee_to_compact_committee(post, get_light_client_committee(post, next_epoch))
  return post
}

fun get_forkchoice_shard_store(anchor_state: BeaconState, shard: Shard): ShardStore {
  return ShardStore(shard = shard, signed_blocks = PyDict(anchor_state.shard_states[shard].latest_block_root to SignedShardBlock(message = ShardBlock(slot = anchor_state.slot, shard = shard))), block_states = PyDict(anchor_state.shard_states[shard].latest_block_root to anchor_state.copy().shard_states[shard]))
}

fun get_shard_latest_attesting_balance(store: Store, shard_store: ShardStore, root: Root): Gwei {
  val state = store.checkpoint_states[store.justified_checkpoint]!!
  val active_indices = get_active_validator_indices(state, get_current_epoch(state))
  return Gwei(sum(active_indices.filter { i -> (i in store.latest_messages) && (store.latest_messages[i]!!.shard == shard_store.shard) && (get_shard_ancestor(store, shard_store, store.latest_messages[i]!!.shard_root, shard_store.signed_blocks[root]!!.message.slot) == root) }.map { i -> state.validators[i].effective_balance }))
}

fun get_shard_head(store: Store, shard_store: ShardStore): Root {
  val beacon_head_root = get_head(store)
  val shard_head_state = store.block_states[beacon_head_root]!!.shard_states[shard_store.shard]
  var shard_head_root = shard_head_state.latest_block_root
  val shard_blocks = shard_store.signed_blocks.items().filter { (_, signed_shard_block) -> (signed_shard_block.message.slot > shard_head_state.slot) }.map { (root, signed_shard_block) -> Pair(root, signed_shard_block.message) }.toPyDict()
  while (true) {
    val children = shard_blocks.items().filter { (_, shard_block) -> (shard_block.shard_parent_root == shard_head_root) }.map { (root, _) -> root }.toPyList()
    if ((len(children) == 0uL)) {
      return shard_head_root
    }
    shard_head_root = max(children, key = { root -> Tuple2(get_shard_latest_attesting_balance(store, shard_store, root), root) })
  }
}

fun get_shard_ancestor(store: Store, shard_store: ShardStore, root: Root, slot: Slot): Root {
  val block = shard_store.signed_blocks[root]!!.message
  if ((block.slot > slot)) {
    return get_shard_ancestor(store, shard_store, block.shard_parent_root, slot)
  } else {
    if ((block.slot == slot)) {
      return root
    } else {
      return root
    }
  }
}

/*
    Return the canonical shard block branch that has not yet been crosslinked.
    */
fun get_pending_shard_blocks(store: Store, shard_store: ShardStore): Sequence<SignedShardBlock> {
  val shard = shard_store.shard
  val beacon_head_root = get_head(store)
  val beacon_head_state = store.block_states[beacon_head_root]!!
  val latest_shard_block_root = beacon_head_state.shard_states[shard].latest_block_root
  val shard_head_root = get_shard_head(store, shard_store)
  var root = shard_head_root
  val signed_shard_blocks: PyList<SignedShardBlock> = PyList()
  while ((root != latest_shard_block_root)) {
    val signed_shard_block = shard_store.signed_blocks[root]!!
    signed_shard_blocks.append(signed_shard_block)
    root = signed_shard_block.message.shard_parent_root
  }
  signed_shard_blocks.reverse()
  return signed_shard_blocks
}

fun on_shard_block(store: Store, shard_store: ShardStore, signed_shard_block: SignedShardBlock): Unit {
  val shard_block = signed_shard_block.message
  val shard = shard_store.shard
  assert((shard_block.shard == shard))
  assert((shard_block.shard_parent_root in shard_store.block_states))
  val shard_parent_state = shard_store.block_states[shard_block.shard_parent_root]!!
  assert((shard_block.beacon_parent_root in store.block_states))
  val beacon_parent_state = store.block_states[shard_block.beacon_parent_root]!!
  val finalized_beacon_state = store.block_states[store.finalized_checkpoint.root]!!
  val finalized_shard_state = finalized_beacon_state.shard_states[shard]
  assert((shard_block.slot > finalized_shard_state.slot))
  val finalized_slot = compute_start_slot_at_epoch(store.finalized_checkpoint.epoch)
  assert((get_ancestor(store, shard_block.beacon_parent_root, finalized_slot) == store.finalized_checkpoint.root))
  val shard_state = shard_parent_state.copy()
  shard_state_transition(shard_state, signed_shard_block, beacon_parent_state, validate_result = true)
  shard_store.signed_blocks[hash_tree_root(shard_block)] = signed_shard_block
  shard_store.block_states[hash_tree_root(shard_block)] = shard_state
}

fun get_shard_winning_roots(state: BeaconState, attestations: Sequence<Attestation>): Pair<Sequence<Shard>, Sequence<Root>> {
  val shards: PyList<Shard> = PyList()
  val winning_roots: PyList<Root> = PyList()
  val online_indices = get_online_validator_indices(state)
  val on_time_attestation_slot = compute_previous_slot(state.slot)
  val committee_count = get_committee_count_per_slot(state, compute_epoch_at_slot(on_time_attestation_slot))
  for (committee_index in map({ index -> CommitteeIndex(index) }, range(committee_count))) {
    val shard = compute_shard_from_committee_index(state, committee_index, on_time_attestation_slot)
    val shard_attestations = attestations.filter { attestation -> is_on_time_attestation(state, attestation.data) && (attestation.data.index == committee_index) }.map { attestation -> attestation }.toPyList()
    val committee = get_beacon_committee(state, on_time_attestation_slot, committee_index)
    val shard_transition_roots = set(shard_attestations.map { a -> a.data.shard_transition_root })
    for (shard_transition_root in sorted(shard_transition_roots)) {
      val transition_attestations = shard_attestations.filter { a -> (a.data.shard_transition_root == shard_transition_root) }.map { a -> a }.toPyList()
      var transition_participants: Set<ValidatorIndex> = set()
      for (attestation in transition_attestations) {
        val participants = get_attesting_indices(state, attestation.data, attestation.aggregation_bits)
        transition_participants = transition_participants.union(participants)
      }
      val enough_online_stake = ((get_total_balance(state, online_indices.intersection(transition_participants)) * 3uL) >= (get_total_balance(state, online_indices.intersection(committee)) * 2uL))
      if (enough_online_stake) {
        shards.append(shard)
        winning_roots.append(shard_transition_root)
        break
      }
    }
  }
  return Pair(shards, winning_roots)
}

fun get_best_light_client_aggregate(block: BeaconBlock, aggregates: Sequence<LightClientVote>): LightClientVote {
  val viable_aggregates = aggregates.filter { aggregate -> (aggregate.data.slot == compute_previous_slot(block.slot)) && (aggregate.data.beacon_block_root == block.parent_root) }.map { aggregate -> aggregate }.toPyList()
  return max(viable_aggregates, key = { Tuple2(len(it.aggregation_bits.filter { i -> (i == pybool(1uL)) }.map { i -> i }.toPyList()), hash_tree_root(it)) }, default = LightClientVote())
}

fun get_shard_transition_fields(beacon_state: BeaconState, shard: Shard, shard_blocks: Sequence<SignedShardBlock>): Triple<Sequence<uint64>, Sequence<Root>, Sequence<ShardState>> {
  val shard_states: PyList<ShardState> = PyList()
  val shard_data_roots: PyList<Root> = PyList()
  val shard_block_lengths: PyList<uint64> = PyList()
  var shard_state = beacon_state.shard_states[shard]
  val shard_block_slots = shard_blocks.map { shard_block -> shard_block.message.slot }.toPyList()
  val offset_slots = compute_offset_slots(get_latest_slot_for_shard(beacon_state, shard), Slot((beacon_state.slot + 1uL)))
  for (slot in offset_slots) {
    val shard_block: SignedShardBlock
    if ((slot in shard_block_slots)) {
      shard_block = shard_blocks[shard_block_slots.index(slot)]
      shard_data_roots.append(get_block_data_merkle_root(shard_block.message.body))
    } else {
      shard_block = SignedShardBlock(message = ShardBlock(slot = slot, shard = shard))
      shard_data_roots.append(Root())
    }
    shard_state = shard_state.copy()
    process_shard_block(shard_state, shard_block.message)
    shard_states.append(shard_state)
    shard_block_lengths.append(len(shard_block.message.body))
  }
  return Triple(shard_block_lengths, shard_data_roots, shard_states)
}

fun get_shard_transition(beacon_state: BeaconState, shard: Shard, shard_blocks: Sequence<SignedShardBlock>): ShardTransition {
  val offset_slots = compute_offset_slots(get_latest_slot_for_shard(beacon_state, shard), Slot((beacon_state.slot + 1uL)))
  val (shard_block_lengths, shard_data_roots, shard_states) = get_shard_transition_fields(beacon_state, shard, shard_blocks)
  val proposer_signature_aggregate: BLSSignature
  if ((len(shard_blocks) > 0uL)) {
    val proposer_signatures = shard_blocks.map { shard_block -> shard_block.signature }.toPyList()
    proposer_signature_aggregate = bls.Aggregate(proposer_signatures)
  } else {
    proposer_signature_aggregate = NO_SIGNATURE
  }
  return ShardTransition(start_slot = offset_slots[0uL], shard_block_lengths = SSZList(shard_block_lengths), shard_data_roots = SSZList(shard_data_roots), shard_states = SSZList(shard_states), proposer_signature_aggregate = proposer_signature_aggregate)
}

fun is_in_next_light_client_committee(state: BeaconState, index: ValidatorIndex): pybool {
  val next_committee = get_light_client_committee(state, (get_current_epoch(state) + LIGHT_CLIENT_COMMITTEE_PERIOD))
  return (index in next_committee)
}

fun get_light_client_vote_signature(state: BeaconState, light_client_vote_data: LightClientVoteData, privkey: pyint): BLSSignature {
  val domain = get_domain(state, DOMAIN_LIGHT_CLIENT, compute_epoch_at_slot(light_client_vote_data.slot))
  val signing_root = compute_signing_root(light_client_vote_data, domain)
  return bls.Sign(privkey, signing_root)
}

fun get_light_client_slot_signature(state: BeaconState, slot: Slot, privkey: pyint): BLSSignature {
  val domain = get_domain(state, DOMAIN_LIGHT_SELECTION_PROOF, compute_epoch_at_slot(slot))
  val signing_root = compute_signing_root(slot, domain)
  return bls.Sign(privkey, signing_root)
}

fun is_light_client_aggregator(state: BeaconState, slot: Slot, slot_signature: BLSSignature): pybool {
  val committee = get_light_client_committee(state, compute_epoch_at_slot(slot))
  val modulo = max(1uL, (len(committee) / TARGET_LIGHT_CLIENT_AGGREGATORS_PER_SLOT))
  return ((bytes_to_int(hash(slot_signature).slice(0uL, 8uL)) % modulo) == 0uL)
}

fun get_aggregate_light_client_signature(light_client_votes: Sequence<LightClientVote>): BLSSignature {
  val signatures = light_client_votes.map { light_client_vote -> light_client_vote.signature }.toPyList()
  return bls.Aggregate(signatures)
}

fun get_light_aggregate_and_proof(state: BeaconState, aggregator_index: ValidatorIndex, aggregate: LightClientVote, privkey: pyint): LightAggregateAndProof {
  return LightAggregateAndProof(aggregator_index = aggregator_index, aggregate = aggregate, selection_proof = get_light_client_slot_signature(state, aggregate.data.slot, privkey))
}

fun get_light_aggregate_and_proof_signature(state: BeaconState, aggregate_and_proof: LightAggregateAndProof, privkey: pyint): BLSSignature {
  val aggregate = aggregate_and_proof.aggregate
  val domain = get_domain(state, DOMAIN_LIGHT_AGGREGATE_AND_PROOF, compute_epoch_at_slot(aggregate.data.slot))
  val signing_root = compute_signing_root(aggregate_and_proof, domain)
  return bls.Sign(privkey, signing_root)
}

fun get_custody_secret(state: BeaconState, validator_index: ValidatorIndex, privkey: pyint, epoch: Epoch? = null): BLSSignature {
  val epoch_: Epoch
  if ((epoch == null )) {
    epoch_ = get_current_epoch(state)
  } else {
    epoch_ = epoch
  }
  val period = get_custody_period_for_validator(validator_index, epoch_)
  val epoch_to_sign = get_randao_epoch_for_custody_period(uint64(period), validator_index)
  val domain = get_domain(state, DOMAIN_RANDAO, epoch_to_sign)
  val signing_root = compute_signing_root(Epoch(epoch_to_sign), domain)
  return bls.Sign(privkey, signing_root)
}

/*
    A stub function return mocking Eth1Data.
    */
fun get_eth1_data(block: Eth1Block): Eth1Data {
  return Eth1Data(deposit_root = block.deposit_root, deposit_count = block.deposit_count, block_hash = hash_tree_root(block))
}

fun get_block_data_merkle_root(data: SSZByteList): Root {
  return Root(data.get_backing().get_left().merkle_root())
}
