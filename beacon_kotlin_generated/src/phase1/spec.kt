package phase1

import pylib.Tuple2
import pylib.all
import pylib.any
import pylib.append
import pylib.bit_length
import pylib.bool
import pylib.contains
import pylib.count
import pylib.enumerate
import pylib.filter
import pylib.from_bytes
import pylib.get
import pylib.index
import pylib.intersection
import pylib.keys
import pylib.len
import pylib.list
import pylib.map
import pylib.max
import pylib.min
import pylib.plus
import pylib.pow
import pylib.pybool
import pylib.pybytes
import pylib.pyint
import pylib.range
import pylib.set
import pylib.shl
import pylib.shr
import pylib.slice
import pylib.sorted
import pylib.sum
import pylib.to_bytes
import pylib.unaryMinus
import pylib.updateSlice
import pylib.zip
import ssz.Bytes
import ssz.Bytes32
import ssz.CBitlist
import ssz.CDict
import ssz.CList
import ssz.CVector
import ssz.SSZObject
import ssz.Sequence
import ssz.boolean
import ssz.uint64
import ssz.uint8
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
    Check if ``indexed_attestation`` has valid indices and signature.
    */
fun is_valid_indexed_attestation(state: BeaconState, indexed_attestation: IndexedAttestation): pybool {
  var all_pubkeys = mutableListOf<BLSPubkey>()
  var all_signing_roots = mutableListOf<Root>()
  var attestation = indexed_attestation.attestation
  var domain = get_domain(state, DOMAIN_BEACON_ATTESTER, attestation.data.target.epoch)
  var aggregation_bits = attestation.aggregation_bits
  assert((len(aggregation_bits) == len(indexed_attestation.committee)))
  if (len(attestation.custody_bits_blocks) == 0uL) {
    for ((participant, abit) in zip(indexed_attestation.committee, aggregation_bits)) {
      if (abit) {
        all_pubkeys.append(state.validators[participant].pubkey)
      }
    }
    var signing_root = compute_signing_root(indexed_attestation.attestation.data, domain)
    return bls.FastAggregateVerify(all_pubkeys, signing_root, signature = attestation.signature)
  } else {
    for ((i, custody_bits) in enumerate(attestation.custody_bits_blocks)) {
      assert((len(custody_bits) == len(indexed_attestation.committee)))
      for ((participant, abit, cbit) in zip(indexed_attestation.committee, aggregation_bits, custody_bits)) {
        if (abit) {
          all_pubkeys.append(state.validators[participant].pubkey)
          var attestation_wrapper = AttestationCustodyBitWrapper(attestation_data_root = hash_tree_root(attestation.data), block_index = i.toULong(), bit = cbit)
          all_signing_roots.append(compute_signing_root(attestation_wrapper, domain))
        } else {
          assert(!(cbit))
        }
      }
    }
    return bls.AggregateVerify(zip(all_pubkeys, all_signing_roots), signature = attestation.signature)
  }
}

/*
    Check if ``leaf`` at ``index`` verifies against the Merkle ``root`` and ``branch``.
    */
fun is_valid_merkle_branch(leaf: Bytes32, branch: Sequence<Bytes32>, depth: uint64, index: uint64, root: Root): pybool {
  var value = leaf
  for (i in range(depth)) {
    if (bool(((index / (2uL.pow(i))) % 2uL))) {
      value = hash((branch[i] + value))
    } else {
      value = hash((value + branch[i]))
    }
  }
  return (value == root)
}

/*
    Return the shuffled validator index corresponding to ``seed`` (and ``index_count``).
    */
fun compute_shuffled_index(index: ValidatorIndex, index_count: uint64, seed: Bytes32): ValidatorIndex {
  assert((index < index_count))
  var index_ = index
  for (current_round in range(SHUFFLE_ROUND_COUNT)) {
    val pivot = (bytes_to_int(hash((seed + int_to_bytes(current_round, length = 1uL))).slice(0uL, 8uL)) % index_count)
    val flip = ValidatorIndex((((pivot + index_count) - index_) % index_count))
    val position = max(index_, flip)
    val source = hash(((seed + int_to_bytes(current_round, length = 1uL)) + int_to_bytes((position / 256uL), length = 4uL)))
    val byte = source[((position % 256uL) / 8uL)]
    val bit = ((byte shr (position % 8uL)) % 2uL)
    index_ = if (bool(bit)) flip else index_
  }
  return ValidatorIndex(index_)
}

/*
    Return from ``indices`` a random index sampled by effective balance.
    */
fun compute_proposer_index(state: BeaconState, indices: Sequence<ValidatorIndex>, seed: Bytes32): ValidatorIndex {
  assert((len(indices) > 0uL))
  var MAX_RANDOM_BYTE = ((2uL.pow(8uL)) - 1uL)
  var i = 0uL
  while (true) {
    var candidate_index = indices[compute_shuffled_index(ValidatorIndex((i % len(indices))), len(indices), seed)]
    var random_byte = hash((seed + int_to_bytes((i / 32uL), length = 8uL)))[(i % 32uL)]
    var effective_balance = state.validators[candidate_index].effective_balance
    if (((effective_balance * MAX_RANDOM_BYTE) >= (MAX_EFFECTIVE_BALANCE * random_byte.toUInt()))) {
      return ValidatorIndex(candidate_index)
    }
    i += 1uL
  }
}

/*
    Return the committee corresponding to ``indices``, ``seed``, ``index``, and committee ``pylib.count``.
    */
fun compute_committee(indices: Sequence<ValidatorIndex>, seed: Bytes32, index: uint64, count: uint64): Sequence<ValidatorIndex> {
  var start = ((len(indices) * index) / count)
  var end = ((len(indices) * (index + 1uL)) / count)
  return range(start, end).map { i -> indices[compute_shuffled_index(ValidatorIndex(i), len(indices), seed)] }.toMutableList()
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
  var fork_data_root = compute_fork_data_root(fork_version
      ?: GENESIS_FORK_VERSION, genesis_validators_root
      ?: Root())
  return Domain((domain_type + fork_data_root.slice(0uL, 28uL)))
}

/*
    Return the signing root of an object by calculating the root of the object-domain tree.
    */
fun compute_signing_root(ssz_object: SSZObject, domain: Domain): Root {
  var domain_wrapped_object = SigningRoot(object_root = hash_tree_root(ssz_object), domain = domain)
  return hash_tree_root(domain_wrapped_object)
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
  var current_epoch = get_current_epoch(state)
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
  return enumerate(state.validators).filter { (_, v) -> is_active_validator(v, epoch) }.map { (i, _) -> ValidatorIndex(i) }.toMutableList()
}

/*
    Return the validator churn limit for the current epoch.
    */
fun get_validator_churn_limit(state: BeaconState): uint64 {
  var active_validator_indices = get_active_validator_indices(state, get_current_epoch(state))
  return max(MIN_PER_EPOCH_CHURN_LIMIT, (len(active_validator_indices) / CHURN_LIMIT_QUOTIENT))
}

/*
    Return the seed at ``epoch``.
    */
fun get_seed(state: BeaconState, epoch: Epoch, domain_type: DomainType): Bytes32 {
  var mix = get_randao_mix(state, Epoch((((epoch + EPOCHS_PER_HISTORICAL_VECTOR) - MIN_SEED_LOOKAHEAD) - 1uL)))
  return hash(((domain_type + int_to_bytes(epoch, length = 8uL)) + mix))
}

/*
    Return the number of committees at ``slot``.
    */
fun get_committee_count_at_slot(state: BeaconState, slot: Slot): uint64 {
  var epoch = compute_epoch_at_slot(slot)
  return max(1uL, min(MAX_COMMITTEES_PER_SLOT, ((len(get_active_validator_indices(state, epoch)) / SLOTS_PER_EPOCH) / TARGET_COMMITTEE_SIZE)))
}

/*
    Return the beacon committee at ``slot`` for ``index``.
    */
fun get_beacon_committee(state: BeaconState, slot: Slot, index: CommitteeIndex): Sequence<ValidatorIndex> {
  var epoch = compute_epoch_at_slot(slot)
  var committees_per_slot = get_committee_count_at_slot(state, slot)
  return compute_committee(indices = get_active_validator_indices(state, epoch), seed = get_seed(state, epoch, DOMAIN_BEACON_ATTESTER), index = (((slot % SLOTS_PER_EPOCH) * committees_per_slot) + index), count = (committees_per_slot * SLOTS_PER_EPOCH))
}

/*
    Return the beacon proposer index at the current slot.
    */
fun get_beacon_proposer_index(state: BeaconState): ValidatorIndex {
  var epoch = get_current_epoch(state)
  var seed = hash((get_seed(state, epoch, DOMAIN_BEACON_PROPOSER) + int_to_bytes(state.slot, length = 8uL)))
  var indices = get_active_validator_indices(state, epoch)
  return compute_proposer_index(state, indices, seed)
}

/*
    Return the combined effective balance of the ``indices``.
    ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
    Math safe up to ~10B ETH, afterwhich this overflows uint64.
    */
fun get_total_balance(state: BeaconState, indices: Set<ValidatorIndex>): Gwei {
  return Gwei(max(EFFECTIVE_BALANCE_INCREMENT, sum(indices.map { index -> state.validators[index].effective_balance }.toMutableList())))
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
  var epoch_ = if (epoch == null) get_current_epoch(state) else epoch
  var fork_version = if (epoch_ < state.fork.epoch) state.fork.previous_version else state.fork.current_version
  return compute_domain(domain_type, fork_version, state.genesis_validators_root)
}

fun get_indexed_attestation(beacon_state: BeaconState, attestation: Attestation): IndexedAttestation {
  var committee = get_beacon_committee(beacon_state, attestation.data.slot, attestation.data.index).toMutableList()
  return IndexedAttestation(committee = committee, attestation = attestation)
}

/*
    Return the pylib.set of attesting indices corresponding to ``data`` and ``bits``.
    */
fun get_attesting_indices(state: BeaconState, data: AttestationData, bits: CBitlist): Set<ValidatorIndex> {
  var committee = get_beacon_committee(state, data.slot, data.index)
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
  var validator = state.validators[index]
  if ((validator.exit_epoch != FAR_FUTURE_EPOCH)) {
    return
  }
  var exit_epochs = state.validators.filter { v -> (v.exit_epoch != FAR_FUTURE_EPOCH) }.map { v -> v.exit_epoch }.toMutableList()
  var exit_queue_epoch = max((exit_epochs + mutableListOf(compute_activation_exit_epoch(get_current_epoch(state)))))
  var exit_queue_churn = len(state.validators.filter { v -> (v.exit_epoch == exit_queue_epoch) }.map { v -> v }.toMutableList())
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
  var epoch = get_current_epoch(state)
  initiate_validator_exit(state, slashed_index)
  var validator = state.validators[slashed_index]
  validator.slashed = true
  validator.withdrawable_epoch = max(validator.withdrawable_epoch, Epoch((epoch + EPOCHS_PER_SLASHINGS_VECTOR)))
  state.slashings[(epoch % EPOCHS_PER_SLASHINGS_VECTOR)] += validator.effective_balance
  decrease_balance(state, slashed_index, (validator.effective_balance / MIN_SLASHING_PENALTY_QUOTIENT))
  var proposer_index = get_beacon_proposer_index(state)
  val whistleblower_index_ = whistleblower_index ?: proposer_index
  var whistleblower_reward = Gwei((validator.effective_balance / WHISTLEBLOWER_REWARD_QUOTIENT))
  var proposer_reward = Gwei((whistleblower_reward / PROPOSER_REWARD_QUOTIENT))
  increase_balance(state, proposer_index, proposer_reward)
  increase_balance(state, whistleblower_index_, (whistleblower_reward - proposer_reward))
}

fun initialize_beacon_state_from_eth1(eth1_block_hash: Bytes32, eth1_timestamp: uint64, deposits: Sequence<Deposit>): BeaconState {
  var fork = Fork(previous_version = GENESIS_FORK_VERSION, current_version = GENESIS_FORK_VERSION, epoch = GENESIS_EPOCH)
  var state = BeaconState(
      genesis_time = ((eth1_timestamp - (eth1_timestamp % MIN_GENESIS_DELAY)) + (2uL * MIN_GENESIS_DELAY)),
      fork = fork,
      eth1_data = Eth1Data(
          block_hash = eth1_block_hash,
          deposit_count = len(deposits)),
      latest_block_header = BeaconBlockHeader(
          body_root = hash_tree_root(BeaconBlockBody())),
      randao_mixes = MutableList(EPOCHS_PER_HISTORICAL_VECTOR.toInt()) { eth1_block_hash })
  var leaves = list(map({ deposit -> deposit.data }, deposits))
  for ((index, deposit) in enumerate(deposits)) {
    var deposit_data_list = leaves.slice(0uL, (index.toULong() + 1uL))
    state.eth1_data.deposit_root = hash_tree_root(deposit_data_list)
    process_deposit(state, deposit)
  }
  for ((index, validator) in enumerate(state.validators)) {
    var balance = state.balances[index]
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
  var block = signed_block.message
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
  var proposer = state.validators[signed_block.message.proposer_index]
  var signing_root = compute_signing_root(signed_block.message, get_domain(state, DOMAIN_BEACON_PROPOSER))
  return bls.Verify(proposer.pubkey, signing_root, signed_block.signature)
}

fun process_slots(state: BeaconState, slot: Slot): Unit {
  assert((state.slot <= slot))
  while ((state.slot < slot)) {
    process_slot(state)
    if ((((state.slot + 1uL) % SLOTS_PER_EPOCH) == 0uL)) {
      process_epoch(state)
    }
    state.slot += Slot(1uL)
  }
}

fun process_slot(state: BeaconState): Unit {
  var previous_state_root = hash_tree_root(state)
  state.state_roots[(state.slot % SLOTS_PER_HISTORICAL_ROOT)] = previous_state_root
  if ((state.latest_block_header.state_root == Bytes32())) {
    state.latest_block_header.state_root = previous_state_root
  }
  var previous_block_root = hash_tree_root(state.latest_block_header)
  state.block_roots[(state.slot % SLOTS_PER_HISTORICAL_ROOT)] = previous_block_root
}

fun process_epoch(state: BeaconState): Unit {
  process_justification_and_finalization(state)
  process_rewards_and_penalties(state)
  process_registry_updates(state)
  process_reveal_deadlines(state)
  process_slashings(state)
  process_final_updates(state)
  process_custody_final_updates(state)
  process_online_tracking(state)
  process_light_client_committee_updates(state)
}

fun get_matching_source_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
  assert((epoch in Pair(get_previous_epoch(state), get_current_epoch(state))))
  return if ((epoch == get_current_epoch(state))) state.current_epoch_attestations else state.previous_epoch_attestations
}

fun get_matching_target_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
  return get_matching_source_attestations(state, epoch).filter { a -> (a.data.target.root == get_block_root(state, epoch)) }.map { a -> a }.toMutableList()
}

fun get_matching_head_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
  return get_matching_target_attestations(state, epoch).filter { a -> (a.data.beacon_block_root == get_block_root_at_slot(state, a.data.slot)) }.map { a -> a }.toMutableList()
}

fun get_unslashed_attesting_indices(state: BeaconState, attestations: Sequence<PendingAttestation>): Set<ValidatorIndex> {
  var output = set<ValidatorIndex>()
  for (a in attestations) {
    output = output.union(get_attesting_indices(state, a.data, a.aggregation_bits))
  }
  return set(filter({ index -> !(state.validators[index].slashed) }, output))
}

/*
    Return the combined effective balance of the pylib.set of unslashed validators participating in ``attestations``.
    Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
    */
fun get_attesting_balance(state: BeaconState, attestations: Sequence<PendingAttestation>): Gwei {
  return get_total_balance(state, get_unslashed_attesting_indices(state, attestations))
}

fun process_justification_and_finalization(state: BeaconState): Unit {
  if ((get_current_epoch(state) <= (GENESIS_EPOCH + 1uL))) {
    return
  }
  var previous_epoch = get_previous_epoch(state)
  var current_epoch = get_current_epoch(state)
  var old_previous_justified_checkpoint = state.previous_justified_checkpoint
  var old_current_justified_checkpoint = state.current_justified_checkpoint
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
  var bits = state.justification_bits
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
  var total_balance = get_total_active_balance(state)
  var effective_balance = state.validators[index].effective_balance
  return Gwei((((effective_balance * BASE_REWARD_FACTOR) / integer_squareroot(total_balance)) / BASE_REWARDS_PER_EPOCH))
}

fun get_eligible_validator_indices(state: BeaconState): Sequence<ValidatorIndex> {
  var previous_epoch = get_previous_epoch(state)
  return enumerate(state.validators).filter { (_, v) -> is_active_validator(v, previous_epoch) || v.slashed && ((previous_epoch + 1uL) < v.withdrawable_epoch) }.map { (index, _) -> ValidatorIndex(index) }.toMutableList()
}

/*
    Helper with shared logic for use by pylib.get source, target, and head deltas functions
    */
fun get_attestation_component_deltas(state: BeaconState, attestations: Sequence<PendingAttestation>): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  var rewards = MutableList(len(state.validators).toInt()) { Gwei(0uL) }
  var penalties = MutableList(len(state.validators).toInt()) { Gwei(0uL) }
  var total_balance = get_total_active_balance(state)
  var unslashed_attesting_indices = get_unslashed_attesting_indices(state, attestations)
  var attesting_balance = get_total_balance(state, unslashed_attesting_indices)
  for (index in get_eligible_validator_indices(state)) {
    if ((index in unslashed_attesting_indices)) {
      var increment = EFFECTIVE_BALANCE_INCREMENT
      var reward_numerator = (get_base_reward(state, index) * (attesting_balance / increment))
      rewards[index] += (reward_numerator / (total_balance / increment))
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
  var matching_source_attestations = get_matching_source_attestations(state, get_previous_epoch(state))
  return get_attestation_component_deltas(state, matching_source_attestations)
}

/*
    Return attester micro-rewards/penalties for target-vote for each validator.
    */
fun get_target_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  var matching_target_attestations = get_matching_target_attestations(state, get_previous_epoch(state))
  return get_attestation_component_deltas(state, matching_target_attestations)
}

/*
    Return attester micro-rewards/penalties for head-vote for each validator.
    */
fun get_head_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  var matching_head_attestations = get_matching_head_attestations(state, get_previous_epoch(state))
  return get_attestation_component_deltas(state, matching_head_attestations)
}

/*
    Return proposer and inclusion delay micro-rewards/penalties for each validator.
    */
fun get_inclusion_delay_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  var rewards = range(len(state.validators)).map { _ -> Gwei(0uL) }.toMutableList()
  var matching_source_attestations = get_matching_source_attestations(state, get_previous_epoch(state))
  for (index in get_unslashed_attesting_indices(state, matching_source_attestations)) {
    var attestation = min(matching_source_attestations.filter { a -> (index in get_attesting_indices(state, a.data, a.aggregation_bits)) }.map { a -> a }.toMutableList(), key = { a -> a.inclusion_delay })
    var proposer_reward = Gwei((get_base_reward(state, index) / PROPOSER_REWARD_QUOTIENT))
    rewards[attestation.proposer_index] += proposer_reward
    var max_attester_reward = (get_base_reward(state, index) - proposer_reward)
    rewards[index] += Gwei((max_attester_reward / attestation.inclusion_delay))
  }
  var penalties = range(len(state.validators)).map { _ -> Gwei(0uL) }.toMutableList()
  return Pair(rewards, penalties)
}

/*
    Return inactivity reward/penalty deltas for each validator.
    */
fun get_inactivity_penalty_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  var penalties = range(len(state.validators)).map { _ -> Gwei(0uL) }.toMutableList()
  var finality_delay = (get_previous_epoch(state) - state.finalized_checkpoint.epoch)
  if ((finality_delay > MIN_EPOCHS_TO_INACTIVITY_PENALTY)) {
    var matching_target_attestations = get_matching_target_attestations(state, get_previous_epoch(state))
    var matching_target_attesting_indices = get_unslashed_attesting_indices(state, matching_target_attestations)
    for (index in get_eligible_validator_indices(state)) {
      penalties[index] += Gwei((BASE_REWARDS_PER_EPOCH * get_base_reward(state, index)))
      if ((index !in matching_target_attesting_indices)) {
        var effective_balance = state.validators[index].effective_balance
        penalties[index] += Gwei(((effective_balance * finality_delay) / INACTIVITY_PENALTY_QUOTIENT))
      }
    }
  }
  var rewards = range(len(state.validators)).map { _ -> Gwei(0uL) }.toMutableList()
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
  var rewards = range(len(state.validators)).map { i -> (((source_rewards[i] + target_rewards[i]) + head_rewards[i]) + inclusion_delay_rewards[i]) }.toMutableList()
  var penalties = range(len(state.validators)).map { i -> (((source_penalties[i] + target_penalties[i]) + head_penalties[i]) + inactivity_penalties[i]) }.toMutableList()
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
  var activation_queue = sorted(
      enumerate(state.validators)
          .filter { (_, validator) -> is_eligible_for_activation(state, validator) }
          .map { (index, _) -> index }.toMutableList(),
      key = { index -> Tuple2(state.validators[index].activation_eligibility_epoch, index) })
  for (index in activation_queue.slice(0uL, get_validator_churn_limit(state))) {
    var validator = state.validators[index]
    validator.activation_epoch = compute_activation_exit_epoch(get_current_epoch(state))
  }
}

fun process_slashings(state: BeaconState): Unit {
  var epoch = get_current_epoch(state)
  var total_balance = get_total_active_balance(state)
  for ((index, validator) in enumerate(state.validators)) {
    if (validator.slashed && ((epoch + (EPOCHS_PER_SLASHINGS_VECTOR / 2uL)) == validator.withdrawable_epoch)) {
      var increment = EFFECTIVE_BALANCE_INCREMENT
      var penalty_numerator = ((validator.effective_balance / increment) * min((sum(state.slashings) * 3uL), total_balance))
      var penalty = ((penalty_numerator / total_balance) * increment)
      decrease_balance(state, ValidatorIndex(index), penalty)
    }
  }
}

fun process_final_updates(state: BeaconState): Unit {
  var current_epoch = get_current_epoch(state)
  var next_epoch = Epoch((current_epoch + 1uL))
  if (((next_epoch % EPOCHS_PER_ETH1_VOTING_PERIOD) == 0uL)) {
    state.eth1_data_votes = mutableListOf()
  }
  for ((index, validator) in enumerate(state.validators)) {
    var balance = state.balances[index]
    var HYSTERESIS_INCREMENT = (EFFECTIVE_BALANCE_INCREMENT / HYSTERESIS_QUOTIENT)
    var DOWNWARD_THRESHOLD = (HYSTERESIS_INCREMENT * HYSTERESIS_DOWNWARD_MULTIPLIER)
    var UPWARD_THRESHOLD = (HYSTERESIS_INCREMENT * HYSTERESIS_UPWARD_MULTIPLIER)
    if (((balance + DOWNWARD_THRESHOLD) < validator.effective_balance) || ((validator.effective_balance + UPWARD_THRESHOLD) < balance)) {
      validator.effective_balance = min((balance - (balance % EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE)
    }
  }
  state.slashings[(next_epoch % EPOCHS_PER_SLASHINGS_VECTOR)] = Gwei(0uL)
  state.randao_mixes[(next_epoch % EPOCHS_PER_HISTORICAL_VECTOR)] = get_randao_mix(state, current_epoch)
  if (((next_epoch % (SLOTS_PER_HISTORICAL_ROOT / SLOTS_PER_EPOCH)) == 0uL)) {
    var historical_batch = HistoricalBatch(block_roots = state.block_roots, state_roots = state.state_roots)
    state.historical_roots.append(hash_tree_root(historical_batch))
  }
  state.previous_epoch_attestations = state.current_epoch_attestations
  state.current_epoch_attestations = mutableListOf()
}

fun process_block(state: BeaconState, block: BeaconBlock): Unit {
  process_block_header(state, block)
  process_randao(state, block.body)
  process_eth1_data(state, block.body)
  process_light_client_signatures(state, block.body)
  process_operations(state, block.body)
  verify_shard_transition_false_positives(state, block.body)
}

fun process_block_header(state: BeaconState, block: BeaconBlock): Unit {
  assert((block.slot == state.slot))
  assert((block.proposer_index == get_beacon_proposer_index(state)))
  assert((block.parent_root == hash_tree_root(state.latest_block_header)))
  state.latest_block_header = BeaconBlockHeader(slot = block.slot, proposer_index = block.proposer_index, parent_root = block.parent_root, state_root = Bytes32(), body_root = hash_tree_root(block.body))
  var proposer = state.validators[block.proposer_index]
  assert(!(proposer.slashed))
}

fun process_randao(state: BeaconState, body: BeaconBlockBody): Unit {
  var epoch = get_current_epoch(state)
  var proposer = state.validators[get_beacon_proposer_index(state)]
  var signing_root = compute_signing_root(epoch, get_domain(state, DOMAIN_RANDAO))
  assert(bls.Verify(proposer.pubkey, signing_root, body.randao_reveal))
  var mix = xor(get_randao_mix(state, epoch), hash(body.randao_reveal))
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
  process_crosslinks(state, body.shard_transitions, body.attestations)
}

fun process_proposer_slashing(state: BeaconState, proposer_slashing: ProposerSlashing): Unit {
  var header_1 = proposer_slashing.signed_header_1.message
  var header_2 = proposer_slashing.signed_header_2.message
  assert((header_1.slot == header_2.slot))
  assert((header_1.proposer_index == header_2.proposer_index))
  assert((header_1 != header_2))
  var proposer = state.validators[header_1.proposer_index]
  assert(is_slashable_validator(proposer, get_current_epoch(state)))
  for (signed_header in listOf(proposer_slashing.signed_header_1, proposer_slashing.signed_header_2)) {
    var domain = get_domain(state, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(signed_header.message.slot))
    var signing_root = compute_signing_root(signed_header.message, domain)
    assert(bls.Verify(proposer.pubkey, signing_root, signed_header.signature))
  }
  slash_validator(state, header_1.proposer_index)
}

fun process_attester_slashing(state: BeaconState, attester_slashing: AttesterSlashing): Unit {
  var indexed_attestation_1 = attester_slashing.attestation_1
  var indexed_attestation_2 = attester_slashing.attestation_2
  assert(is_slashable_attestation_data(indexed_attestation_1.attestation.data, indexed_attestation_2.attestation.data))
  assert(is_valid_indexed_attestation(state, indexed_attestation_1))
  assert(is_valid_indexed_attestation(state, indexed_attestation_2))
  var indices_1 = get_indices_from_committee(indexed_attestation_1.committee, indexed_attestation_1.attestation.aggregation_bits)
  var indices_2 = get_indices_from_committee(indexed_attestation_2.committee, indexed_attestation_2.attestation.aggregation_bits)
  var slashed_any = false
  var indices = set(indices_1).intersection(indices_2)
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
  var pending_attestation = PendingAttestation(aggregation_bits = attestation.aggregation_bits, data = attestation.data, inclusion_delay = (state.slot - attestation.data.slot), proposer_index = get_beacon_proposer_index(state), crosslink_success = false)
  if ((attestation.data.target.epoch == get_current_epoch(state))) {
    state.current_epoch_attestations.append(pending_attestation)
  } else {
    state.previous_epoch_attestations.append(pending_attestation)
  }
}

fun process_deposit(state: BeaconState, deposit: Deposit): Unit {
  assert(is_valid_merkle_branch(leaf = hash_tree_root(deposit.data), branch = deposit.proof, depth = (DEPOSIT_CONTRACT_TREE_DEPTH + 1uL), index = state.eth1_deposit_index, root = state.eth1_data.deposit_root))
  state.eth1_deposit_index += 1uL
  var pubkey = deposit.data.pubkey
  var amount = deposit.data.amount
  var validator_pubkeys = state.validators.map { v -> v.pubkey }.toMutableList()
  if ((pubkey !in validator_pubkeys)) {
    var deposit_message = DepositMessage(pubkey = deposit.data.pubkey, withdrawal_credentials = deposit.data.withdrawal_credentials, amount = deposit.data.amount)
    var domain = compute_domain(DOMAIN_DEPOSIT)
    var signing_root = compute_signing_root(deposit_message, domain)
    if (!(bls.Verify(pubkey, signing_root, deposit.data.signature))) {
      return
    }
    state.validators.append(Validator(pubkey = pubkey, withdrawal_credentials = deposit.data.withdrawal_credentials, activation_eligibility_epoch = FAR_FUTURE_EPOCH, activation_epoch = FAR_FUTURE_EPOCH, exit_epoch = FAR_FUTURE_EPOCH, withdrawable_epoch = FAR_FUTURE_EPOCH, effective_balance = min((amount - (amount % EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE)))
    state.balances.append(amount)
  } else {
    var index = ValidatorIndex(validator_pubkeys.index(pubkey))
    increase_balance(state, index, amount)
  }
}

fun process_voluntary_exit(state: BeaconState, signed_voluntary_exit: SignedVoluntaryExit): Unit {
  var voluntary_exit = signed_voluntary_exit.message
  var validator = state.validators[voluntary_exit.validator_index]
  assert(is_active_validator(validator, get_current_epoch(state)))
  assert((validator.exit_epoch == FAR_FUTURE_EPOCH))
  assert((get_current_epoch(state) >= voluntary_exit.epoch))
  assert((get_current_epoch(state) >= (validator.activation_epoch + PERSISTENT_COMMITTEE_PERIOD)))
  var domain = get_domain(state, DOMAIN_VOLUNTARY_EXIT, voluntary_exit.epoch)
  var signing_root = compute_signing_root(voluntary_exit, domain)
  assert(bls.Verify(validator.pubkey, signing_root, signed_voluntary_exit.signature))
  initiate_validator_exit(state, voluntary_exit.validator_index)
}

fun get_forkchoice_store(anchor_state: BeaconState): Store {
  var anchor_block_header = anchor_state.latest_block_header.copy()
  if ((anchor_block_header.state_root == Bytes32())) {
    anchor_block_header.state_root = hash_tree_root(anchor_state)
  }
  var anchor_root = hash_tree_root(anchor_block_header)
  var anchor_epoch = get_current_epoch(anchor_state)
  var justified_checkpoint = Checkpoint(epoch = anchor_epoch, root = anchor_root)
  var finalized_checkpoint = Checkpoint(epoch = anchor_epoch, root = anchor_root)
  return Store(
      time = (anchor_state.genesis_time + (SECONDS_PER_SLOT * anchor_state.slot)),
      genesis_time = anchor_state.genesis_time,
      justified_checkpoint = justified_checkpoint,
      finalized_checkpoint = finalized_checkpoint,
      best_justified_checkpoint = justified_checkpoint,
      blocks = mutableMapOf(anchor_root to anchor_block_header),
      block_states = mutableMapOf(anchor_root to anchor_state.copy()),
      checkpoint_states = mutableMapOf(justified_checkpoint to anchor_state.copy()))
}

fun get_slots_since_genesis(store: Store): pyint {
  return pyint((store.time - store.genesis_time) / SECONDS_PER_SLOT)
}

fun get_current_slot(store: Store): Slot {
  return Slot((GENESIS_SLOT + get_slots_since_genesis(store).value.toLong().toULong()))
}

fun compute_slots_since_epoch_start(slot: Slot): pyint {
  return pyint(slot - compute_start_slot_at_epoch(compute_epoch_at_slot(slot)))
}

fun get_ancestor(store: Store, root: Root, slot: Slot): Root {
  var block = store.blocks[root]!!
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
  var state = store.checkpoint_states[store.justified_checkpoint]!!
  var active_indices = get_active_validator_indices(state, get_current_epoch(state))
  return Gwei(sum(active_indices.filter { i -> (i in store.latest_messages) && (get_ancestor(store, store.latest_messages[i]!!.root, store.blocks[root]!!.slot) == root) }.map { i -> state.validators[i].effective_balance }))
}

fun filter_block_tree(store: Store, block_root: Root, blocks: CDict<Root, BeaconBlockHeader>): pybool {
  var block = store.blocks[block_root]!!
  var children = store.blocks.keys().filter { root -> (store.blocks[root]!!.parent_root == block_root) }.map { root -> root }.toMutableList()
  if (any(children)) {
    var filter_block_tree_result = children.map { child -> filter_block_tree(store, child, blocks) }.toMutableList()
    if (any(filter_block_tree_result)) {
      blocks[block_root] = block
      return true
    }
    return false
  }
  var head_state = store.block_states[block_root]!!
  var correct_justified = (store.justified_checkpoint.epoch == GENESIS_EPOCH) || (head_state.current_justified_checkpoint == store.justified_checkpoint)
  var correct_finalized = (store.finalized_checkpoint.epoch == GENESIS_EPOCH) || (head_state.finalized_checkpoint == store.finalized_checkpoint)
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
fun get_filtered_block_tree(store: Store): CDict<Root, BeaconBlockHeader> {
  var base = store.justified_checkpoint.root
  val blocks: CDict<Root, BeaconBlockHeader> = mutableMapOf()
  filter_block_tree(store, base, blocks)
  return blocks
}

fun get_head(store: Store): Root {
  var blocks = get_filtered_block_tree(store)
  var head = store.justified_checkpoint.root
  var justified_slot = compute_start_slot_at_epoch(store.justified_checkpoint.epoch)
  while (true) {
    var children = blocks.keys().filter { root -> (blocks[root]!!.parent_root == head) && (blocks[root]!!.slot > justified_slot) }.map { root -> root }.toMutableList()
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
  var justified_slot = compute_start_slot_at_epoch(store.justified_checkpoint.epoch)
  if (!((get_ancestor(store, new_justified_checkpoint.root, justified_slot) == store.justified_checkpoint.root))) {
    return false
  }
  return true
}

fun validate_on_attestation(store: Store, attestation: Attestation): Unit {
  var target = attestation.data.target
  var current_epoch = compute_epoch_at_slot(get_current_slot(store))
  var previous_epoch = if ((current_epoch > GENESIS_EPOCH)) (current_epoch - 1uL) else GENESIS_EPOCH
  assert((target.epoch in mutableListOf(current_epoch, previous_epoch)))
  assert((target.epoch == compute_epoch_at_slot(attestation.data.slot)))
  assert((target.root in store.blocks))
  assert((attestation.data.beacon_block_root in store.blocks))
  assert((store.blocks[attestation.data.beacon_block_root]!!.slot <= attestation.data.slot))
  assert((get_current_slot(store) >= (attestation.data.slot + 1uL)))
}

fun store_target_checkpoint_state(store: Store, target: Checkpoint): Unit {
  if ((target !in store.checkpoint_states)) {
    var base_state = store.block_states[target.root]!!.copy()
    process_slots(base_state, compute_start_slot_at_epoch(target.epoch))
    store.checkpoint_states[target] = base_state
  }
}

fun update_latest_messages(store: Store, attesting_indices: Sequence<ValidatorIndex>, attestation: Attestation): Unit {
  var target = attestation.data.target
  var beacon_block_root = attestation.data.beacon_block_root
  for (i in attesting_indices) {
    if ((i !in store.latest_messages) || (target.epoch > store.latest_messages[i]!!.epoch)) {
      store.latest_messages[i] = LatestMessage(epoch = target.epoch, root = beacon_block_root)
    }
  }
}

fun on_tick(store: Store, time: uint64): Unit {
  var previous_slot = get_current_slot(store)
  store.time = time
  var current_slot = get_current_slot(store)
  if (!((current_slot > previous_slot) && (compute_slots_since_epoch_start(current_slot) == pyint(0uL)))) {
    return
  }
  if ((store.best_justified_checkpoint.epoch > store.justified_checkpoint.epoch)) {
    store.justified_checkpoint = store.best_justified_checkpoint
  }
}

fun on_block(store: Store, signed_block: SignedBeaconBlock): Unit {
  var block = signed_block.message
  assert((block.parent_root in store.block_states))
  var pre_state = store.block_states[block.parent_root]!!.copy()
  assert((get_current_slot(store) >= block.slot))
  store.blocks[hash_tree_root(block)] = BeaconBlockHeader(slot = block.slot, proposer_index = block.proposer_index, state_root = block.state_root, body_root = hash_tree_root(block.body))
  var finalized_slot = compute_start_slot_at_epoch(store.finalized_checkpoint.epoch)
  assert((block.slot > finalized_slot))
  assert((get_ancestor(store, hash_tree_root(block), finalized_slot) == store.finalized_checkpoint.root))
  var state = state_transition(pre_state, signed_block, true)
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
    finalized_slot = compute_start_slot_at_epoch(store.finalized_checkpoint.epoch)
    if ((state.current_justified_checkpoint.epoch > store.justified_checkpoint.epoch) || (get_ancestor(store, store.justified_checkpoint.root, finalized_slot) != store.finalized_checkpoint.root)) {
      store.justified_checkpoint = state.current_justified_checkpoint
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
  var target_state = store.checkpoint_states[attestation.data.target]!!
  var indexed_attestation = get_indexed_attestation(target_state, attestation)
  assert(is_valid_indexed_attestation(target_state, indexed_attestation))
  var attesting_indices = enumerate(indexed_attestation.committee)
      .filter { (i, _) -> attestation.aggregation_bits[i] }
      .map { (_, index) -> index }.toMutableList()
  update_latest_messages(store, attesting_indices, attestation)
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
  val bytez_ = bytez + Bytes.fromHexString("0x" + List((BYTES_PER_CUSTODY_ATOM - (bytez.size().toUInt() % BYTES_PER_CUSTODY_ATOM)).toInt()) { "00" })
  return range(0uL, bytez_.size().toULong(), BYTES_PER_CUSTODY_ATOM).map { i -> bytez_.slice(i, (i + BYTES_PER_CUSTODY_ATOM)) }.toMutableList()
}

fun compute_custody_bit(key: BLSSignature, data: pybytes): boolean {
  var full_G2_element = bls.signature_to_G2(key)
  var s = full_G2_element.first.coeffs
  var custody_atoms = get_custody_atoms(data)
  var n = len(custody_atoms)
  val e1 = enumerate(custody_atoms)
  var a = sum(e1.map({ (i, atom) -> s[(i.toULong() % 2uL)].pow(i.toULong()) * from_bytes(atom, "little") })) + s[(n % 2uL)].pow(n)
  return legendre_bit(a, BLS12_381_Q)
}

fun get_randao_epoch_for_custody_period(period: uint64, validator_index: ValidatorIndex): Epoch {
  var next_period_start = (((period + 1uL) * EPOCHS_PER_CUSTODY_PERIOD) - (validator_index % EPOCHS_PER_CUSTODY_PERIOD))
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
  for_ops(body.custody_key_reveals, ::process_custody_key_reveal)
  for_ops(body.early_derived_secret_reveals, ::process_early_derived_secret_reveal)
  for_ops(body.custody_slashings, ::process_custody_slashing)
}

/*
    Process ``CustodyKeyReveal`` operation.
    Note that this function mutates ``state``.
    */
fun process_custody_key_reveal(state: BeaconState, reveal: CustodyKeyReveal): Unit {
  var revealer = state.validators[reveal.revealer_index]
  var epoch_to_sign = get_randao_epoch_for_custody_period(revealer.next_custody_secret_to_reveal, reveal.revealer_index)
  var custody_reveal_period = get_custody_period_for_validator(reveal.revealer_index, get_current_epoch(state))
  assert((revealer.next_custody_secret_to_reveal < custody_reveal_period.value.toLong().toULong()))
  assert(is_slashable_validator(revealer, get_current_epoch(state)))
  var domain = get_domain(state, DOMAIN_RANDAO, epoch_to_sign)
  var signing_root = compute_signing_root(epoch_to_sign, domain)
  assert(bls.Verify(revealer.pubkey, signing_root, reveal.reveal))
  if (((epoch_to_sign + EPOCHS_PER_CUSTODY_PERIOD) >= get_current_epoch(state))) {
    if ((revealer.max_reveal_lateness >= MAX_REVEAL_LATENESS_DECREMENT)) {
      revealer.max_reveal_lateness -= MAX_REVEAL_LATENESS_DECREMENT
    } else {
      revealer.max_reveal_lateness = 0uL
    }
  } else {
    revealer.max_reveal_lateness = max(revealer.max_reveal_lateness, ((get_current_epoch(state) - epoch_to_sign) - EPOCHS_PER_CUSTODY_PERIOD))
  }
  revealer.next_custody_secret_to_reveal += 1uL
  var proposer_index = get_beacon_proposer_index(state)
  increase_balance(state, proposer_index, Gwei((get_base_reward(state, reveal.revealer_index) / MINOR_REWARD_QUOTIENT)))
}

/*
    Process ``EarlyDerivedSecretReveal`` operation.
    Note that this function mutates ``state``.
    */
fun process_early_derived_secret_reveal(state: BeaconState, reveal: EarlyDerivedSecretReveal): Unit {
  var revealed_validator = state.validators[reveal.revealed_index]
  var derived_secret_location = (reveal.epoch % EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS)
  assert((reveal.epoch >= (get_current_epoch(state) + RANDAO_PENALTY_EPOCHS)))
  assert((reveal.epoch < (get_current_epoch(state) + EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS)))
  assert(!(revealed_validator.slashed))
  assert((reveal.revealed_index !in state.exposed_derived_secrets[derived_secret_location]))
  var masker = state.validators[reveal.masker_index]
  var pubkeys = mutableListOf(revealed_validator.pubkey, masker.pubkey)
  var domain = get_domain(state, DOMAIN_RANDAO, reveal.epoch)
  var signing_roots = mutableListOf(hash_tree_root(reveal.epoch), reveal.mask).map { root -> compute_signing_root(root, domain) }.toMutableList()
  assert(bls.AggregateVerify(zip(pubkeys, signing_roots), reveal.reveal))
  if ((reveal.epoch >= (get_current_epoch(state) + CUSTODY_PERIOD_TO_RANDAO_PADDING))) {
    slash_validator(state, reveal.revealed_index, reveal.masker_index)
  } else {
    var max_proposer_slot_reward = (((get_base_reward(state, reveal.revealed_index) * SLOTS_PER_EPOCH) / len(get_active_validator_indices(state, get_current_epoch(state)))) / PROPOSER_REWARD_QUOTIENT)
    var penalty = Gwei(((max_proposer_slot_reward * EARLY_DERIVED_SECRET_REVEAL_SLOT_REWARD_MULTIPLE) * (len(state.exposed_derived_secrets[derived_secret_location]) + 1uL)))
    var proposer_index = get_beacon_proposer_index(state)
    var whistleblower_index = reveal.masker_index
    var whistleblowing_reward = Gwei((penalty / WHISTLEBLOWER_REWARD_QUOTIENT))
    var proposer_reward = Gwei((whistleblowing_reward / PROPOSER_REWARD_QUOTIENT))
    increase_balance(state, proposer_index, proposer_reward)
    increase_balance(state, whistleblower_index, (whistleblowing_reward - proposer_reward))
    decrease_balance(state, reveal.revealed_index, penalty)
    state.exposed_derived_secrets[derived_secret_location].append(reveal.revealed_index)
  }
}

fun process_custody_slashing(state: BeaconState, signed_custody_slashing: SignedCustodySlashing): Unit {
  var custody_slashing = signed_custody_slashing.message
  var attestation = custody_slashing.attestation
  var malefactor = state.validators[custody_slashing.malefactor_index]
  var whistleblower = state.validators[custody_slashing.whistleblower_index]
  var domain = get_domain(state, DOMAIN_CUSTODY_BIT_SLASHING, get_current_epoch(state))
  var signing_root = compute_signing_root(custody_slashing, domain)
  assert(bls.Verify(whistleblower.pubkey, signing_root, signed_custody_slashing.signature))
  assert(is_slashable_validator(whistleblower, get_current_epoch(state)))
  assert(is_slashable_validator(malefactor, get_current_epoch(state)))
  assert(is_valid_indexed_attestation(state, get_indexed_attestation(state, attestation)))
  var shard_transition = custody_slashing.shard_transition
  assert((hash_tree_root(shard_transition) == attestation.data.shard_transition_root))
  assert((hash_tree_root(custody_slashing.data) == shard_transition.shard_data_roots[custody_slashing.data_index]))
  var attesters = get_attesting_indices(state, attestation.data, attestation.aggregation_bits)
  assert((custody_slashing.malefactor_index in attesters))
  var epoch_to_sign = get_randao_epoch_for_custody_period(get_custody_period_for_validator(custody_slashing.malefactor_index, attestation.data.target.epoch).value.toLong().toULong(), custody_slashing.malefactor_index)
  domain = get_domain(state, DOMAIN_RANDAO, epoch_to_sign)
  signing_root = compute_signing_root(epoch_to_sign, domain)
  assert(bls.Verify(malefactor.pubkey, signing_root, custody_slashing.malefactor_secret))
  var custody_bits = attestation.custody_bits_blocks[custody_slashing.data_index]
  var committee = get_beacon_committee(state, attestation.data.slot, attestation.data.index)
  var claimed_custody_bit = custody_bits[committee.index(custody_slashing.malefactor_index)]
  var computed_custody_bit = compute_custody_bit(custody_slashing.malefactor_secret, Bytes.wrap(custody_slashing.data.toByteArray()))
  if ((claimed_custody_bit != computed_custody_bit)) {
    slash_validator(state, custody_slashing.malefactor_index)
    var others_count = (len(committee) - 1uL)
    var whistleblower_reward = Gwei(((malefactor.effective_balance / WHISTLEBLOWER_REWARD_QUOTIENT) / others_count))
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
  var epoch = get_current_epoch(state)
  for ((index, validator) in enumerate(state.validators)) {
    if ((get_custody_period_for_validator(ValidatorIndex(index), epoch) > validator.next_custody_secret_to_reveal)) {
      // pass
    }
  }
}

fun process_custody_final_updates(state: BeaconState): Unit {
  state.exposed_derived_secrets[(get_current_epoch(state) % EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS)] = mutableListOf()
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
  return Triple(ValidatorIndex((compact_validator shr 16uL)), bool(((compact_validator shr 15uL) % 2uL)), (compact_validator and ((2uL.pow(15uL)) - 1uL)))
}

/*
    Given a state and a list of validator indices, outputs the CompactCommittee representing them.
    */
fun committee_to_compact_committee(state: BeaconState, committee: Sequence<ValidatorIndex>): CompactCommittee {
  var validators = committee.map { i -> state.validators[i] }.toMutableList()
  var compact_validators = zip(committee, validators).map { (i, v) -> pack_compact_validator(i, v.slashed, (v.effective_balance / EFFECTIVE_BALANCE_INCREMENT)) }.toMutableList()
  var pubkeys = validators.map { v -> v.pubkey }.toMutableList()
  return CompactCommittee(pubkeys = pubkeys, compact_validators = compact_validators)
}

fun compute_shard_from_committee_index(state: BeaconState, index: CommitteeIndex, slot: Slot): Shard {
  var active_shards = get_active_shard_count(state)
  return Shard(((index + get_start_shard(state, slot)) % active_shards))
}

/*
    Return the offset slots that are greater than ``start_slot`` and less than ``end_slot``.
    */
fun compute_offset_slots(start_slot: Slot, end_slot: Slot): Sequence<Slot> {
  return SHARD_BLOCK_OFFSETS.filter { x -> ((start_slot + x) < end_slot) }.map { x -> Slot((start_slot + x)) }.toMutableList()
}

fun compute_updated_gasprice(prev_gasprice: Gwei, length: uint8): Gwei {
  if ((length > TARGET_SHARD_BLOCK_SIZE)) {
    var delta = (((prev_gasprice * (length - TARGET_SHARD_BLOCK_SIZE)) / TARGET_SHARD_BLOCK_SIZE) / GASPRICE_ADJUSTMENT_COEFFICIENT)
    return min((prev_gasprice + delta), MAX_GASPRICE)
  } else {
    var delta = (((prev_gasprice * (TARGET_SHARD_BLOCK_SIZE - length)) / TARGET_SHARD_BLOCK_SIZE) / GASPRICE_ADJUSTMENT_COEFFICIENT)
    return (max(prev_gasprice, (MIN_GASPRICE + delta)) - delta)
  }
}

fun get_active_shard_count(state: BeaconState): uint64 {
  return len(state.shard_states)
}

fun get_online_validator_indices(state: BeaconState): Set<ValidatorIndex> {
  var active_validators = get_active_validator_indices(state, get_current_epoch(state))
  return set(active_validators.filter { i -> (state.online_countdown[i] != 0u.toUByte()) }.map { i -> i }.toMutableList())
}

fun get_shard_committee(beacon_state: BeaconState, epoch: Epoch, shard: Shard): Sequence<ValidatorIndex> {
  var source_epoch = (epoch - (epoch % SHARD_COMMITTEE_PERIOD))
  if ((source_epoch > 0uL)) {
    source_epoch -= SHARD_COMMITTEE_PERIOD
  }
  var active_validator_indices = get_active_validator_indices(beacon_state, source_epoch)
  var seed = get_seed(beacon_state, source_epoch, DOMAIN_SHARD_COMMITTEE)
  var active_shard_count = get_active_shard_count(beacon_state)
  return compute_committee(indices = active_validator_indices, seed = seed, index = shard, count = active_shard_count)
}

fun get_light_client_committee(beacon_state: BeaconState, epoch: Epoch): Sequence<ValidatorIndex> {
  var source_epoch = (epoch - (epoch % LIGHT_CLIENT_COMMITTEE_PERIOD))
  if ((source_epoch > 0uL)) {
    source_epoch -= LIGHT_CLIENT_COMMITTEE_PERIOD
  }
  var active_validator_indices = get_active_validator_indices(beacon_state, source_epoch)
  var seed = get_seed(beacon_state, source_epoch, DOMAIN_LIGHT_CLIENT)
  return compute_committee(indices = active_validator_indices, seed = seed, index = 0uL, count = get_active_shard_count(beacon_state)).slice(0uL, TARGET_COMMITTEE_SIZE)
}

fun get_shard_proposer_index(beacon_state: BeaconState, slot: Slot, shard: Shard): ValidatorIndex {
  var committee = get_shard_committee(beacon_state, compute_epoch_at_slot(slot), shard)
  var r = bytes_to_int(get_seed(beacon_state, get_current_epoch(beacon_state), DOMAIN_SHARD_COMMITTEE).slice(0uL, 8uL))
  return committee[(r % len(committee))]
}

fun get_start_shard(state: BeaconState, slot: Slot): Shard {
  return Shard(0uL)
}

fun get_shard(state: BeaconState, attestation: Attestation): Shard {
  return compute_shard_from_committee_index(state, attestation.data.index, attestation.data.slot)
}

fun get_latest_slot_for_shard(state: BeaconState, shard: Shard): Slot {
  return state.shard_states[shard].slot
}

fun get_offset_slots(state: BeaconState, shard: Shard): Sequence<Slot> {
  return compute_offset_slots(state.shard_states[shard].slot, state.slot)
}

fun is_shard_attestation(state: BeaconState, attestation: Attestation, committee_index: CommitteeIndex): pybool {
  if (!((attestation.data.index == committee_index) && ((attestation.data.slot + MIN_ATTESTATION_INCLUSION_DELAY) == state.slot))) {
    return false
  }
  return true
}

/*
    Check if ``attestation`` helped contribute to the successful crosslink of
    ``winning_root`` formed by ``committee_index`` committee at the current slot.
    */
fun is_winning_attestation(state: BeaconState, attestation: PendingAttestation, committee_index: CommitteeIndex, winning_root: Root): pybool {
  return (attestation.data.slot == state.slot) && (attestation.data.index == committee_index) && (attestation.data.shard_transition_root == winning_root)
}

fun validate_attestation(state: BeaconState, attestation: Attestation): Unit {
  var data = attestation.data
  assert((data.index < get_committee_count_at_slot(state, data.slot)))
  assert((data.index < get_active_shard_count(state)))
  assert((data.target.epoch in Pair(get_previous_epoch(state), get_current_epoch(state))))
  assert((data.target.epoch == compute_epoch_at_slot(data.slot)))
  assert(((data.slot + MIN_ATTESTATION_INCLUSION_DELAY) <= state.slot) && (state.slot <= (data.slot + SLOTS_PER_EPOCH)))
  var committee = get_beacon_committee(state, data.slot, data.index)
  assert((len(attestation.aggregation_bits) == len(committee)))
  if ((attestation.data.target.epoch == get_current_epoch(state))) {
    assert((attestation.data.source == state.current_justified_checkpoint))
  } else {
    assert((attestation.data.source == state.previous_justified_checkpoint))
  }
  var shard = get_shard(state, attestation)
  if ((attestation.custody_bits_blocks != mutableListOf<CBitlist>())) {
    assert(((data.slot + MIN_ATTESTATION_INCLUSION_DELAY) == state.slot))
    assert((len(attestation.custody_bits_blocks) == len(get_offset_slots(state, shard))))
    assert((data.beacon_block_root == get_block_root_at_slot(state, compute_previous_slot(state.slot))))
  } else {
    assert(((data.slot + MIN_ATTESTATION_INCLUSION_DELAY) < state.slot))
    assert((data.shard_transition_root == Root()))
  }
  assert(is_valid_indexed_attestation(state, get_indexed_attestation(state, attestation)))
}

fun apply_shard_transition(state: BeaconState, shard: Shard, transition: ShardTransition): Unit {
  assert((state.slot > PHASE_1_GENESIS_SLOT))
  var offset_slots = get_offset_slots(state, shard)
  assert((len(transition.shard_data_roots) == len(transition.shard_states)) && (len(transition.shard_states) == len(transition.shard_block_lengths)) && (len(transition.shard_block_lengths) == len(offset_slots)))
  assert((transition.start_slot == offset_slots[0uL]))
  var headers = mutableListOf<ShardBlockHeader>()
  var proposers = mutableListOf<ValidatorIndex>()
  var prev_gasprice = state.shard_states[shard].gasprice
  var shard_parent_root = state.shard_states[shard].latest_block_root
  for (i in range(len(offset_slots))) {
    var shard_block_length = transition.shard_block_lengths[i]
    var shard_state = transition.shard_states[i]
    assert((shard_state.gasprice == compute_updated_gasprice(prev_gasprice, shard_block_length.toUByte())))
    assert((shard_state.slot == offset_slots[i]))
    var is_empty_proposal = (shard_block_length == 0uL)
    if (!(is_empty_proposal)) {
      var proposal_index = get_shard_proposer_index(state, offset_slots[i], shard)
      var header = ShardBlockHeader(shard_parent_root = shard_parent_root, beacon_parent_root = get_block_root_at_slot(state, offset_slots[i]), proposer_index = proposal_index, slot = offset_slots[i], body_root = transition.shard_data_roots[i])
      shard_parent_root = hash_tree_root(header)
      headers.append(header)
      proposers.append(proposal_index)
    }
    prev_gasprice = shard_state.gasprice
  }
  var pubkeys = proposers.map { proposer -> state.validators[proposer].pubkey }.toMutableList()
  var signing_roots = headers.map { header -> compute_signing_root(header, get_domain(state, DOMAIN_SHARD_PROPOSAL, compute_epoch_at_slot(header.slot))) }.toMutableList()
  assert(bls.AggregateVerify(zip(pubkeys, signing_roots), signature = transition.proposer_signature_aggregate))
  state.shard_states[shard] = transition.shard_states[(len(transition.shard_states) - 1uL)]
  state.shard_states[shard].slot = (state.slot - 1uL)
}

fun process_crosslink_for_shard(state: BeaconState, committee_index: CommitteeIndex, shard_transition: ShardTransition, attestations: Sequence<Attestation>): Root {
  var committee = get_beacon_committee(state, state.slot, committee_index)
  var online_indices = get_online_validator_indices(state)
  var shard = compute_shard_from_committee_index(state, committee_index, state.slot)
  var shard_transition_roots = set(attestations.map { a -> a.data.shard_transition_root }.toMutableList())
  for (shard_transition_root in sorted(shard_transition_roots)) {
    var transition_attestations = attestations.filter { a -> (a.data.shard_transition_root == shard_transition_root) }.map { a -> a }.toMutableList()
    var transition_participants: Set<ValidatorIndex> = set()
    for (attestation in transition_attestations) {
      var participants = get_attesting_indices(state, attestation.data, attestation.aggregation_bits)
      transition_participants = transition_participants.union(participants)
      assert((attestation.data.head_shard_root == shard_transition.shard_data_roots[(len(shard_transition.shard_data_roots) - 1uL)]))
    }
    var enough_online_stake = ((get_total_balance(state, online_indices.intersection(transition_participants)) * 3uL) >= (get_total_balance(state, online_indices.intersection(committee)) * 2uL))
    if (!(enough_online_stake)) {
      continue
    }
    assert((shard_transition_root == hash_tree_root(shard_transition)))
    apply_shard_transition(state, shard, shard_transition)
    var beacon_proposer_index = get_beacon_proposer_index(state)
    var estimated_attester_reward = sum(transition_participants.map { attester -> get_base_reward(state, attester) }.toMutableList())
    var proposer_reward = Gwei((estimated_attester_reward / PROPOSER_REWARD_QUOTIENT))
    increase_balance(state, beacon_proposer_index, proposer_reward)
    var states_slots_lengths = zip(shard_transition.shard_states, get_offset_slots(state, shard), shard_transition.shard_block_lengths)
    for ((shard_state, slot, length) in states_slots_lengths) {
      var proposer_index = get_shard_proposer_index(state, slot, shard)
      decrease_balance(state, proposer_index, (shard_state.gasprice * length))
    }
    return shard_transition_root
  }
  assert((shard_transition == ShardTransition()))
  return Root()
}

fun process_crosslinks(state: BeaconState, shard_transitions: Sequence<ShardTransition>, attestations: Sequence<Attestation>): Unit {
  var committee_count = get_committee_count_at_slot(state, state.slot)
  for (committee_index in map(::CommitteeIndex, range(committee_count))) {
    var shard = compute_shard_from_committee_index(state, committee_index, state.slot)
    var shard_transition = shard_transitions[shard]
    var shard_attestations = attestations.filter { attestation -> is_shard_attestation(state, attestation, committee_index) }.map { attestation -> attestation }.toMutableList()
    var winning_root = process_crosslink_for_shard(state, committee_index, shard_transition, shard_attestations)
    if ((winning_root != Root())) {
      for (pending_attestation in state.current_epoch_attestations) {
        if (is_winning_attestation(state, pending_attestation, committee_index, winning_root)) {
          pending_attestation.crosslink_success = true
        }
      }
    }
  }
}

fun get_indices_from_committee(committee: CList<ValidatorIndex>, bits: CBitlist): CList<ValidatorIndex> {
  assert((len(bits) == len(committee)))
  return enumerate(committee).filter { (i, _) -> bits[i] }.map { (_, validator_index) -> validator_index }.toMutableList()
}

fun verify_shard_transition_false_positives(state: BeaconState, block_body: BeaconBlockBody): Unit {
  for (shard in range(get_active_shard_count(state))) {
    if ((state.shard_states[shard].slot != (state.slot - 1uL))) {
      assert((block_body.shard_transitions[shard] == ShardTransition()))
    }
  }
}

fun process_light_client_signatures(state: BeaconState, block_body: BeaconBlockBody): Unit {
  var committee = get_light_client_committee(state, get_current_epoch(state))
  var total_reward = Gwei(0uL)
  var signer_pubkeys = mutableListOf<BLSPubkey>()
  for ((bit_index, participant_index) in enumerate(committee)) {
    if (block_body.light_client_signature_bitfield[bit_index]) {
      signer_pubkeys.append(state.validators[participant_index].pubkey)
      increase_balance(state, participant_index, get_base_reward(state, participant_index))
      total_reward += get_base_reward(state, participant_index)
    }
  }
  increase_balance(state, get_beacon_proposer_index(state), Gwei((total_reward / PROPOSER_REWARD_QUOTIENT)))
  var slot = compute_previous_slot(state.slot)
  var signing_root = compute_signing_root(get_block_root_at_slot(state, slot), get_domain(state, DOMAIN_LIGHT_CLIENT, compute_epoch_at_slot(slot)))
  if ((len(signer_pubkeys) == 0uL)) {
    assert((block_body.light_client_signature == BLSSignature()))
    return
  } else {
    assert(bls.FastAggregateVerify(signer_pubkeys, signing_root, signature = block_body.light_client_signature))
  }
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

fun process_light_client_committee_updates(state: BeaconState): Unit {
  if (((get_current_epoch(state) % LIGHT_CLIENT_COMMITTEE_PERIOD) == 0uL)) {
    state.current_light_committee = state.next_light_committee
    var new_committee = get_light_client_committee(state, (get_current_epoch(state) + LIGHT_CLIENT_COMMITTEE_PERIOD))
    state.next_light_committee = committee_to_compact_committee(state, new_committee)
  }
}

fun compute_shard_transition_digest(beacon_state: BeaconState, shard_state: ShardState, beacon_parent_root: Root, shard_body_root: Root): Bytes32 {
  return hash(((hash_tree_root(shard_state) + beacon_parent_root) + shard_body_root))
}

fun verify_shard_block_message(beacon_state: BeaconState, shard_state: ShardState, block: ShardBlock, slot: Slot, shard: Shard): pybool {
  assert((block.shard_parent_root == shard_state.latest_block_root))
  assert((block.slot == slot))
  assert((block.proposer_index == get_shard_proposer_index(beacon_state, slot, shard)))
  assert((0uL < len(block.body)) && (len(block.body) <= MAX_SHARD_BLOCK_SIZE))
  return true
}

fun verify_shard_block_signature(beacon_state: BeaconState, signed_block: SignedShardBlock): pybool {
  var proposer = beacon_state.validators[signed_block.message.proposer_index]
  var domain = get_domain(beacon_state, DOMAIN_SHARD_PROPOSAL, compute_epoch_at_slot(signed_block.message.slot))
  var signing_root = compute_signing_root(signed_block.message, domain)
  return bls.Verify(proposer.pubkey, signing_root, signed_block.signature)
}

fun shard_state_transition(beacon_state: BeaconState, shard_state: ShardState, block: ShardBlock): Unit {
  var prev_gasprice = shard_state.gasprice
  var latest_block_root: Root
  if ((len(block.body) == 0uL)) {
    latest_block_root = shard_state.latest_block_root
  } else {
    latest_block_root = hash_tree_root(block)
  }
  shard_state.transition_digest = compute_shard_transition_digest(beacon_state, shard_state, block.beacon_parent_root, Bytes32(block.body))
  shard_state.gasprice = compute_updated_gasprice(prev_gasprice, len(block.body).toUByte())
  shard_state.slot = block.slot
  shard_state.latest_block_root = latest_block_root
}

/*
    A pure function that returns a new post ShardState instead of modifying the given `shard_state`.
    */
fun get_post_shard_state(beacon_state: BeaconState, shard_state: ShardState, block: ShardBlock): ShardState {
  var post_state = shard_state.copy()
  shard_state_transition(beacon_state, post_state, block)
  return post_state
}

fun is_valid_fraud_proof(beacon_state: BeaconState, attestation: Attestation, offset_index: uint64, transition: ShardTransition, block: ShardBlock, subkey: BLSPubkey, beacon_parent_block: BeaconBlock): pybool {
  var custody_bits = attestation.custody_bits_blocks
  for (j in range(len(custody_bits[offset_index]))) {
    if ((custody_bits[offset_index][j] != generate_custody_bit(subkey, block))) {
      return true
    }
  }
  var shard_state: ShardState
  if ((offset_index == 0uL)) {
    var shard = get_shard(beacon_state, attestation)
    shard_state = beacon_parent_block.body.shard_transitions[shard].shard_states[-(1uL)]
  } else {
    shard_state = transition.shard_states[(offset_index - 1uL)]
  }
  shard_state = get_post_shard_state(beacon_state, shard_state, block)
  if (shard_state.transition_digest != transition.shard_states[offset_index].transition_digest) {
    return true
  }
  return false
}

fun generate_custody_bit(subkey: BLSPubkey, block: ShardBlock): pybool {
  TODO("...")
}

fun get_winning_proposal(beacon_state: BeaconState, proposals: Sequence<SignedShardBlock>): SignedShardBlock {
  return proposals[-(1uL)]
}

fun compute_shard_body_roots(proposals: Sequence<SignedShardBlock>): Sequence<Root> {
  return proposals.map { proposal -> hash_tree_root(proposal.message.body) }.toMutableList()
}

/*
    Return the valid shard blocks at the given ``slot``.
    Note that this function doesn't change the state.
    */
fun get_proposal_choices_at_slot(beacon_state: BeaconState, shard_state: ShardState, slot: Slot, shard: Shard, shard_blocks: Sequence<SignedShardBlock>, validate_signature: pybool = true): Sequence<SignedShardBlock> {
  var choices = mutableListOf<SignedShardBlock>()
  var shard_blocks_at_slot = shard_blocks.filter { block -> (block.message.slot == slot) }.map { block -> block }.toMutableList()
  var shard_state_ = shard_state
  for (block in shard_blocks_at_slot) {
    try {
      assert(verify_shard_block_message(beacon_state, shard_state_, block.message, slot, shard))
      if (validate_signature) {
        assert(verify_shard_block_signature(beacon_state, block))
      }
      shard_state_ = get_post_shard_state(beacon_state, shard_state_, block.message)
    } catch (_: Exception) {
      // pass
    }
    run { // else
      choices.append(block)
    }
  }
  return choices
}

/*
    Return ``proposal``, ``shard_state`` of the given ``slot``.
    Note that this function doesn't change the state.
    */
fun get_proposal_at_slot(beacon_state: BeaconState, shard_state: ShardState, slot: Shard, shard: Shard, shard_blocks: Sequence<SignedShardBlock>, validate_signature: pybool = true): Pair<SignedShardBlock, ShardState> {
  var choices = get_proposal_choices_at_slot(beacon_state = beacon_state, shard_state = shard_state, slot = slot, shard = shard, shard_blocks = shard_blocks, validate_signature = validate_signature)
  var proposal: SignedShardBlock
  if ((len(choices) == 0uL)) {
    var block = ShardBlock(slot = slot)
    proposal = SignedShardBlock(message = block)
  } else {
    if ((len(choices) == 1uL)) {
      proposal = choices[0uL]
    } else {
      proposal = get_winning_proposal(beacon_state, choices)
    }
  }
  var shard_state_ = get_post_shard_state(beacon_state, shard_state, proposal.message)
  return Pair(proposal, shard_state_)
}

fun get_shard_state_transition_result(beacon_state: BeaconState, shard: Shard, shard_blocks: Sequence<SignedShardBlock>, validate_signature: pybool = true): Triple<Sequence<SignedShardBlock>, Sequence<ShardState>, Sequence<Root>> {
  var proposals = mutableListOf<SignedShardBlock>()
  var shard_states = mutableListOf<ShardState>()
  var shard_state = beacon_state.shard_states[shard]
  for (slot in get_offset_slots(beacon_state, shard)) {
    val (proposal, shard_state_) = get_proposal_at_slot(beacon_state = beacon_state, shard_state = shard_state, slot = slot, shard = shard, shard_blocks = shard_blocks, validate_signature = validate_signature)
    shard_states.append(shard_state_)
    proposals.append(proposal)
  }
  var shard_data_roots = compute_shard_body_roots(proposals)
  return Triple(proposals, shard_states, shard_data_roots)
}

fun get_shard_transition(beacon_state: BeaconState, shard: Shard, shard_blocks: Sequence<SignedShardBlock>): ShardTransition {
  var offset_slots = get_offset_slots(beacon_state, shard)
  var start_slot = offset_slots[0uL]
  val (proposals, shard_states, shard_data_roots) = get_shard_state_transition_result(beacon_state, shard, shard_blocks)
  assert((len(proposals) > 0uL))
  assert((len(shard_data_roots) > 0uL))
  var shard_block_lengths = mutableListOf<uint64>()
  var proposer_signatures = mutableListOf<BLSSignature>()
  for (proposal in proposals) {
    shard_block_lengths.append(len(proposal.message.body))
    if ((proposal.signature != BLSSignature())) {
      proposer_signatures.append(proposal.signature)
    }
  }
  var proposer_signature_aggregate = bls.Aggregate(proposer_signatures)
  return ShardTransition(start_slot = start_slot, shard_block_lengths = shard_block_lengths, shard_data_roots = shard_data_roots.toMutableList(), shard_states = shard_states.toMutableList(), proposer_signature_aggregate = proposer_signature_aggregate)
}

fun upgrade_to_phase1(pre: phase0.BeaconState): BeaconState {
  var epoch = phase0.get_current_epoch(pre)
  var post = BeaconState(
      genesis_time = pre.genesis_time,
      slot = pre.slot,
      fork = Fork(
          previous_version = pre.fork.current_version,
          current_version = PHASE_1_FORK_VERSION,
          epoch = epoch),
      latest_block_header = pre.latest_block_header,
      block_roots = pre.block_roots,
      state_roots = pre.state_roots,
      historical_roots = pre.historical_roots,
      eth1_data = pre.eth1_data,
      eth1_data_votes = pre.eth1_data_votes,
      eth1_deposit_index = pre.eth1_deposit_index,
      validators = enumerate(pre.validators).map { (i, phase0_validator) ->
        Validator(
            pubkey = phase0_validator.pubkey,
            withdrawal_credentials = phase0_validator.withdrawal_credentials,
            effective_balance = phase0_validator.effective_balance,
            slashed = phase0_validator.slashed,
            activation_eligibility_epoch = phase0_validator.activation_eligibility_epoch,
            activation_epoch = phase0_validator.activation_eligibility_epoch,
            exit_epoch = phase0_validator.exit_epoch,
            withdrawable_epoch = phase0_validator.withdrawable_epoch,
            next_custody_secret_to_reveal = get_custody_period_for_validator(ValidatorIndex(i), epoch).value.toLong().toULong(),
            max_reveal_lateness = 0uL)
      }.toMutableList(),
      balances = pre.balances,
      randao_mixes = pre.randao_mixes,
      slashings = pre.slashings,
      previous_epoch_attestations = CList(),
      current_epoch_attestations = CList(),
      justification_bits = pre.justification_bits,
      previous_justified_checkpoint = pre.previous_justified_checkpoint,
      current_justified_checkpoint = pre.current_justified_checkpoint,
      finalized_checkpoint = pre.finalized_checkpoint,
      shard_states = range(INITIAL_ACTIVE_SHARDS).map { ShardState(slot = pre.slot, gasprice = MIN_GASPRICE, transition_digest = Root(), latest_block_root = Root()) }.toMutableList(),
      online_countdown = List(len(pre.validators).toInt()) { mutableListOf(ONLINE_PERIOD) }.flatten().toMutableList(),
      current_light_committee = CompactCommittee(),
      next_light_committee = CompactCommittee(),
      exposed_derived_secrets = range(EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS).map { CVector<CList<ValidatorIndex>>() }.flatten().toMutableList()
  )
  var next_epoch = Epoch((epoch + 1uL))
  post.current_light_committee = committee_to_compact_committee(post, get_light_client_committee(post, epoch))
  post.next_light_committee = committee_to_compact_committee(post, get_light_client_committee(post, next_epoch))
  return post
}

fun get_eth1_data(distance: uint64): Bytes32 {
  return hash(distance)
}
