package beacon_java.phase0;

import static beacon_java.phase0.Constants.*;
import static beacon_java.deps.BLS.bls;
import static beacon_java.phase0.Utils.copy;
import static beacon_java.phase0.Utils.hash_tree_root;
import static beacon_java.pylib.Exports.*;
import static beacon_java.pylib.pyint.from_bytes;

import beacon_java.phase0.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import beacon_java.ssz.uint8;

public class Spec {
  /*
      Return the largest integer ``x`` such that ``x**2 <= n``.
      */
  public static uint64 integer_squareroot(uint64 n) {
    var x = n;
    var y = divide(plus(x, pyint.create(1L)), pyint.create(2L));
    var x_2 = x;
    var y_2 = y;
    while (less(y_2, x_2).v()) {
      var x_1 = y_2;
      var y_1 = divide(plus(x_1, divide(n, x_1)), pyint.create(2L));
      x_2 = x_1;
      y_2 = y_1;
    }
    return x_2;
  }

  /*
      Return the exclusive-or of two 32-byte strings.
      */
  public static Bytes32 xor(Bytes32 bytes_1, Bytes32 bytes_2) {
    return new Bytes32(zip(bytes_1, bytes_2).map((tmp_0) -> { var a = tmp_0.first; var b = tmp_0.second; return bitXor(a, b); }));
  }

  /*
      Return the integer deserialization of ``data`` interpreted as ``ENDIANNESS``-endian.
      */
  public static uint64 bytes_to_uint64(pybytes data) {
    return new uint64(pyint.from_bytes(data, ENDIANNESS));
  }

  /*
      Check if ``validator`` is active.
      */
  public static pybool is_active_validator(Validator validator, Epoch epoch) {
    return and(lessOrEqual(validator.getActivation_epoch(), epoch), less(epoch, validator.getExit_epoch()));
  }

  /*
      Check if ``validator`` is eligible to be placed into the activation queue.
      */
  public static pybool is_eligible_for_activation_queue(Validator validator) {
    return and(eq(validator.getActivation_eligibility_epoch(), FAR_FUTURE_EPOCH), eq(validator.getEffective_balance(), MAX_EFFECTIVE_BALANCE));
  }

  /*
      Check if ``validator`` is eligible for activation.
      */
  public static pybool is_eligible_for_activation(BeaconState state, Validator validator) {
    return and(lessOrEqual(validator.getActivation_eligibility_epoch(), state.getFinalized_checkpoint().getEpoch()), eq(validator.getActivation_epoch(), FAR_FUTURE_EPOCH));
  }

  /*
      Check if ``validator`` is slashable.
      */
  public static pybool is_slashable_validator(Validator validator, Epoch epoch) {
    return and(not(validator.getSlashed()), and(lessOrEqual(validator.getActivation_epoch(), epoch), less(epoch, validator.getWithdrawable_epoch())));
  }

  /*
      Check if ``data_1`` and ``data_2`` are slashable according to Casper FFG rules.
      */
  public static pybool is_slashable_attestation_data(AttestationData data_1, AttestationData data_2) {
    return or(and(not(eq(data_1, data_2)), eq(data_1.getTarget().getEpoch(), data_2.getTarget().getEpoch())), and(less(data_1.getSource().getEpoch(), data_2.getSource().getEpoch()), less(data_2.getTarget().getEpoch(), data_1.getTarget().getEpoch())));
  }

  /*
      Check if ``indexed_attestation`` is not empty, has sorted and unique indices and has a valid aggregate signature.
      */
  public static pybool is_valid_indexed_attestation(BeaconState state, IndexedAttestation indexed_attestation) {
    var indices = indexed_attestation.getAttesting_indices();
    if (or(eq(len(indices), pyint.create(0L)), not(eq(indices, sorted(set(indices))))).v()) {
      return pybool.create(false);
    }
    var pubkeys = list(indices.map((i) -> state.getValidators().get(i).getPubkey()));
    var domain = get_domain(state, DOMAIN_BEACON_ATTESTER, indexed_attestation.getData().getTarget().getEpoch());
    var signing_root = compute_signing_root(indexed_attestation.getData(), domain);
    return bls.FastAggregateVerify(pubkeys, signing_root, indexed_attestation.getSignature());
  }

  /*
      Check if ``leaf`` at ``index`` verifies against the Merkle ``root`` and ``branch``.
      */
  public static pybool is_valid_merkle_branch(Bytes32 leaf, Sequence<Bytes32> branch, uint64 depth, uint64 index, Root root) {
    var value = leaf;
    var value_3 = value;
    for (var i: range(depth)) {
      if (pybool(modulo(divide(index, pow(pyint.create(2L), i)), pyint.create(2L))).v()) {
        var value_1 = hash(plus(branch.get(i), value_3));
        value_3 = value_1;
      } else {
        var value_2 = hash(plus(value_3, branch.get(i)));
        value_3 = value_2;
      }
    }
    return eq(value_3, root);
  }

  /*
      Return the shuffled index corresponding to ``seed`` (and ``index_count``).
      */
  public static uint64 compute_shuffled_index(uint64 index, uint64 index_count, Bytes32 seed) {
    pyassert(less(index, index_count));
    var index_2 = index;
    for (var current_round: range(SHUFFLE_ROUND_COUNT)) {
      var pivot = modulo(bytes_to_uint64(hash(plus(seed, uint_to_bytes(new uint8(current_round)))).getSlice(pyint.create(0L), pyint.create(8L))), index_count);
      var flip = modulo(minus(plus(pivot, index_count), index_2), index_count);
      var position = max(index_2, flip);
      var source = hash(plus(plus(seed, uint_to_bytes(new uint8(current_round))), uint_to_bytes(new uint32(divide(position, pyint.create(256L))))));
      var byte_ = new uint8(source.get(divide(modulo(position, pyint.create(256L)), pyint.create(8L))));
      var bit = modulo(rightShift(byte_, modulo(position, pyint.create(8L))), pyint.create(2L));
      var index_1 = pybool(bit).v() ? flip : index_2;
      index_2 = index_1;
    }
    return index_2;
  }

  /*
      Return from ``indices`` a random index sampled by effective balance.
      */
  public static ValidatorIndex compute_proposer_index(BeaconState state, Sequence<ValidatorIndex> indices, Bytes32 seed) {
    pyassert(greater(len(indices), pyint.create(0L)));
    var MAX_RANDOM_BYTE = minus(pow(pyint.create(2L), pyint.create(8L)), pyint.create(1L));
    var i = new uint64(pyint.create(0L));
    var total = new uint64(len(indices));
    var i_2 = i;
    while (true) {
      var candidate_index = indices.get(compute_shuffled_index(modulo(i_2, total), total, seed));
      var random_byte = hash(plus(seed, uint_to_bytes(new uint64(divide(i_2, pyint.create(32L)))))).get(modulo(i_2, pyint.create(32L)));
      var effective_balance = state.getValidators().get(candidate_index).getEffective_balance();
      if (greaterOrEqual(multiply(effective_balance, MAX_RANDOM_BYTE), multiply(MAX_EFFECTIVE_BALANCE, random_byte)).v()) {
        return candidate_index;
      }
      var i_1 = plus(i_2, pyint.create(1L));
      i_2 = i_1;
    }
  }

  /*
      Return the committee corresponding to ``indices``, ``seed``, ``index``, and committee ``count``.
      */
  public static Sequence<ValidatorIndex> compute_committee(Sequence<ValidatorIndex> indices, Bytes32 seed, uint64 index, uint64 count) {
    var start = divide(multiply(len(indices), index), count);
    var end = divide(multiply(len(indices), new uint64(plus(index, pyint.create(1L)))), count);
    return list(range(start, end).map((i) -> indices.get(compute_shuffled_index(new uint64(i), new uint64(len(indices)), seed))));
  }

  /*
      Return the epoch number at ``slot``.
      */
  public static Epoch compute_epoch_at_slot(Slot slot) {
    return new Epoch(divide(slot, SLOTS_PER_EPOCH));
  }

  /*
      Return the start slot of ``epoch``.
      */
  public static Slot compute_start_slot_at_epoch(Epoch epoch) {
    return new Slot(multiply(epoch, SLOTS_PER_EPOCH));
  }

  /*
      Return the epoch during which validator activations and exits initiated in ``epoch`` take effect.
      */
  public static Epoch compute_activation_exit_epoch(Epoch epoch) {
    return new Epoch(plus(plus(epoch, pyint.create(1L)), MAX_SEED_LOOKAHEAD));
  }

  /*
      Return the 32-byte fork data root for the ``current_version`` and ``genesis_validators_root``.
      This is used primarily in signature domains to avoid collisions across forks/chains.
      */
  public static Root compute_fork_data_root(Version current_version, Root genesis_validators_root) {
    return hash_tree_root(new ForkData(current_version, genesis_validators_root));
  }

  /*
      Return the 4-byte fork digest for the ``current_version`` and ``genesis_validators_root``.
      This is a digest primarily used for domain separation on the p2p layer.
      4-bytes suffices for practical separation of forks/chains.
      */
  public static ForkDigest compute_fork_digest(Version current_version, Root genesis_validators_root) {
    return new ForkDigest(compute_fork_data_root(current_version, genesis_validators_root).getSlice(null, pyint.create(4L)));
  }

  /*
      Return the domain for the ``domain_type`` and ``fork_version``.
      */
  public static Domain compute_domain(DomainType domain_type, Version fork_version, Root genesis_validators_root) {
    Version fork_version_2;
    if (fork_version == null) {
      var fork_version_1 = GENESIS_FORK_VERSION;
      fork_version_2 = fork_version_1;
    } else {
      fork_version_2 = fork_version;
    }
    Root genesis_validators_root_2;
    if (genesis_validators_root == null) {
      var genesis_validators_root_1 = new Root();
      genesis_validators_root_2 = genesis_validators_root_1;
    } else {
      genesis_validators_root_2 = genesis_validators_root;
    }
    var fork_data_root = compute_fork_data_root(fork_version_2, genesis_validators_root_2);
    return new Domain(plus(domain_type, fork_data_root.getSlice(null, pyint.create(28L))));
  }

  /*
      Return the signing root for the corresponding signing data.
      */
  public static Root compute_signing_root(Object ssz_object, Domain domain) {
    return hash_tree_root(new SigningData(hash_tree_root(ssz_object), domain));
  }

  /*
      Return the current epoch.
      */
  public static Epoch get_current_epoch(BeaconState state) {
    return compute_epoch_at_slot(state.getSlot());
  }

  /*`
      Return the previous epoch (unless the current epoch is ``GENESIS_EPOCH``).
      */
  public static Epoch get_previous_epoch(BeaconState state) {
    var current_epoch = get_current_epoch(state);
    return eq(current_epoch, GENESIS_EPOCH).v() ? GENESIS_EPOCH : new Epoch(minus(current_epoch, pyint.create(1L)));
  }

  /*
      Return the block root at the start of a recent ``epoch``.
      */
  public static Root get_block_root(BeaconState state, Epoch epoch) {
    return get_block_root_at_slot(state, compute_start_slot_at_epoch(epoch));
  }

  /*
      Return the block root at a recent ``slot``.
      */
  public static Root get_block_root_at_slot(BeaconState state, Slot slot) {
    pyassert(and(less(slot, state.getSlot()), lessOrEqual(state.getSlot(), plus(slot, SLOTS_PER_HISTORICAL_ROOT))));
    return state.getBlock_roots().get(modulo(slot, SLOTS_PER_HISTORICAL_ROOT));
  }

  /*
      Return the randao mix at a recent ``epoch``.
      */
  public static Bytes32 get_randao_mix(BeaconState state, Epoch epoch) {
    return state.getRandao_mixes().get(modulo(epoch, EPOCHS_PER_HISTORICAL_VECTOR));
  }

  /*
      Return the sequence of active validator indices at ``epoch``.
      */
  public static Sequence<ValidatorIndex> get_active_validator_indices(BeaconState state, Epoch epoch) {
    return list(enumerate(state.getValidators()).filter((tmp_1) -> { var i = tmp_1.first; var v = tmp_1.second; return is_active_validator(v, epoch); }).map((tmp_2) -> { var i = tmp_2.first; var v = tmp_2.second; return new ValidatorIndex(i); }));
  }

  /*
      Return the validator churn limit for the current epoch.
      */
  public static uint64 get_validator_churn_limit(BeaconState state) {
    var active_validator_indices = get_active_validator_indices(state, get_current_epoch(state));
    return max(MIN_PER_EPOCH_CHURN_LIMIT, divide(new uint64(len(active_validator_indices)), CHURN_LIMIT_QUOTIENT));
  }

  /*
      Return the seed at ``epoch``.
      */
  public static Bytes32 get_seed(BeaconState state, Epoch epoch, DomainType domain_type) {
    var mix = get_randao_mix(state, new Epoch(minus(minus(plus(epoch, EPOCHS_PER_HISTORICAL_VECTOR), MIN_SEED_LOOKAHEAD), pyint.create(1L))));
    return hash(plus(plus(domain_type, uint_to_bytes(epoch)), mix));
  }

  /*
      Return the number of committees in each slot for the given ``epoch``.
      */
  public static uint64 get_committee_count_per_slot(BeaconState state, Epoch epoch) {
    return max(new uint64(pyint.create(1L)), min(MAX_COMMITTEES_PER_SLOT, divide(divide(new uint64(len(get_active_validator_indices(state, epoch))), SLOTS_PER_EPOCH), TARGET_COMMITTEE_SIZE)));
  }

  /*
      Return the beacon committee at ``slot`` for ``index``.
      */
  public static Sequence<ValidatorIndex> get_beacon_committee(BeaconState state, Slot slot, CommitteeIndex index) {
    var epoch = compute_epoch_at_slot(slot);
    var committees_per_slot = get_committee_count_per_slot(state, epoch);
    return compute_committee(get_active_validator_indices(state, epoch), get_seed(state, epoch, DOMAIN_BEACON_ATTESTER), plus(multiply(modulo(slot, SLOTS_PER_EPOCH), committees_per_slot), index), multiply(committees_per_slot, SLOTS_PER_EPOCH));
  }

  /*
      Return the beacon proposer index at the current slot.
      */
  public static ValidatorIndex get_beacon_proposer_index(BeaconState state) {
    var epoch = get_current_epoch(state);
    var seed = hash(plus(get_seed(state, epoch, DOMAIN_BEACON_PROPOSER), uint_to_bytes(state.getSlot())));
    var indices = get_active_validator_indices(state, epoch);
    return compute_proposer_index(state, indices, seed);
  }

  /*
      Return the combined effective balance of the ``indices``.
      ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
      Math safe up to ~10B ETH, afterwhich this overflows uint64.
      */
  public static Gwei get_total_balance(BeaconState state, Set<ValidatorIndex> indices) {
    return new Gwei(max(EFFECTIVE_BALANCE_INCREMENT, sum(list(indices.map((index) -> state.getValidators().get(index).getEffective_balance())))));
  }

  /*
      Return the combined effective balance of the active validators.
      Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
      */
  public static Gwei get_total_active_balance(BeaconState state) {
    return get_total_balance(state, set(get_active_validator_indices(state, get_current_epoch(state))));
  }

  /*
      Return the signature domain (fork version concatenated with domain type) of a message.
      */
  public static Domain get_domain(BeaconState state, DomainType domain_type, Epoch epoch) {
    var epoch_1 = epoch == null ? get_current_epoch(state) : epoch;
    var fork_version = less(epoch_1, state.getFork().getEpoch()).v() ? state.getFork().getPrevious_version() : state.getFork().getCurrent_version();
    return compute_domain(domain_type, fork_version, state.getGenesis_validators_root());
  }

  /*
      Return the indexed attestation corresponding to ``attestation``.
      */
  public static IndexedAttestation get_indexed_attestation(BeaconState state, Attestation attestation) {
    var attesting_indices = get_attesting_indices(state, attestation.getData(), attestation.getAggregation_bits());
    return new IndexedAttestation(new SSZList<>(sorted(attesting_indices)), attestation.getData(), attestation.getSignature());
  }

  /*
      Return the set of attesting indices corresponding to ``data`` and ``bits``.
      */
  public static Set<ValidatorIndex> get_attesting_indices(BeaconState state, AttestationData data, SSZBitlist bits) {
    var committee = get_beacon_committee(state, data.getSlot(), data.getIndex());
    return set(enumerate(committee).filter((tmp_3) -> { var i = tmp_3.first; var index = tmp_3.second; return bits.get(i); }).map((tmp_4) -> { var i = tmp_4.first; var index = tmp_4.second; return index; }));
  }

  /*
      Increase the validator balance at index ``index`` by ``delta``.
      */
  public static void increase_balance(BeaconState state, ValidatorIndex index, Gwei delta) {
    state.getBalances().set(index, plus(state.getBalances().get(index), delta));
  }

  /*
      Decrease the validator balance at index ``index`` by ``delta``, with underflow protection.
      */
  public static void decrease_balance(BeaconState state, ValidatorIndex index, Gwei delta) {
    state.getBalances().set(index, new Gwei(greater(delta, state.getBalances().get(index)).v() ? pyint.create(0L) : minus(state.getBalances().get(index), delta)));
  }

  /*
      Initiate the exit of the validator with index ``index``.
      */
  public static void initiate_validator_exit(BeaconState state, ValidatorIndex index) {
    var validator = state.getValidators().get(index);
    if (not(eq(validator.getExit_epoch(), FAR_FUTURE_EPOCH)).v()) {
      return;
    }
    var exit_epochs = list(state.getValidators().filter((v) -> not(eq(v.getExit_epoch(), FAR_FUTURE_EPOCH))).map((v) -> v.getExit_epoch()));
    var exit_queue_epoch = max(plus(exit_epochs, PyList.of(compute_activation_exit_epoch(get_current_epoch(state)))));
    var exit_queue_churn = len(list(state.getValidators().filter((v) -> eq(v.getExit_epoch(), exit_queue_epoch)).map((v) -> v)));
    Epoch exit_queue_epoch_2;
    if (greaterOrEqual(exit_queue_churn, get_validator_churn_limit(state)).v()) {
      var exit_queue_epoch_1 = plus(exit_queue_epoch, new Epoch(pyint.create(1L)));
      exit_queue_epoch_2 = exit_queue_epoch_1;
    } else {
      exit_queue_epoch_2 = exit_queue_epoch;
    }
    validator.setExit_epoch(exit_queue_epoch_2);
    validator.setWithdrawable_epoch(new Epoch(plus(validator.getExit_epoch(), MIN_VALIDATOR_WITHDRAWABILITY_DELAY)));
  }

  /*
      Slash the validator with index ``slashed_index``.
      */
  public static void slash_validator(BeaconState state, ValidatorIndex slashed_index, ValidatorIndex whistleblower_index) {
    var epoch = get_current_epoch(state);
    initiate_validator_exit(state, slashed_index);
    var validator = state.getValidators().get(slashed_index);
    validator.setSlashed(new SSZBoolean(pybool.create(true)));
    validator.setWithdrawable_epoch(max(validator.getWithdrawable_epoch(), new Epoch(plus(epoch, EPOCHS_PER_SLASHINGS_VECTOR))));
    state.getSlashings().set(modulo(epoch, EPOCHS_PER_SLASHINGS_VECTOR), plus(state.getSlashings().get(modulo(epoch, EPOCHS_PER_SLASHINGS_VECTOR)), validator.getEffective_balance()));
    decrease_balance(state, slashed_index, divide(validator.getEffective_balance(), MIN_SLASHING_PENALTY_QUOTIENT));
    var proposer_index = get_beacon_proposer_index(state);
    ValidatorIndex whistleblower_index_2;
    if (whistleblower_index == null) {
      var whistleblower_index_1 = proposer_index;
      whistleblower_index_2 = whistleblower_index_1;
    } else {
      whistleblower_index_2 = whistleblower_index;
    }
    var whistleblower_reward = new Gwei(divide(validator.getEffective_balance(), WHISTLEBLOWER_REWARD_QUOTIENT));
    var proposer_reward = new Gwei(divide(whistleblower_reward, PROPOSER_REWARD_QUOTIENT));
    increase_balance(state, proposer_index, proposer_reward);
    increase_balance(state, whistleblower_index_2, new Gwei(minus(whistleblower_reward, proposer_reward)));
  }

  public static BeaconState initialize_beacon_state_from_eth1(Bytes32 eth1_block_hash, uint64 eth1_timestamp, Sequence<Deposit> deposits) {
    var fork = new Fork(GENESIS_FORK_VERSION, GENESIS_FORK_VERSION, GENESIS_EPOCH);
    var state = new BeaconState(plus(eth1_timestamp, GENESIS_DELAY), BeaconState.genesis_validators_root_default, BeaconState.slot_default, fork, new BeaconBlockHeader(BeaconBlockHeader.slot_default, BeaconBlockHeader.proposer_index_default, BeaconBlockHeader.parent_root_default, BeaconBlockHeader.state_root_default, hash_tree_root(new BeaconBlockBody(BeaconBlockBody.randao_reveal_default, BeaconBlockBody.eth1_data_default, BeaconBlockBody.graffiti_default, BeaconBlockBody.proposer_slashings_default, BeaconBlockBody.attester_slashings_default, BeaconBlockBody.attestations_default, BeaconBlockBody.deposits_default, BeaconBlockBody.voluntary_exits_default))), BeaconState.block_roots_default, BeaconState.state_roots_default, BeaconState.historical_roots_default,
            new Eth1Data(Eth1Data.deposit_root_default, new uint64(len(deposits)), new Hash32(eth1_block_hash)), BeaconState.eth1_data_votes_default, BeaconState.eth1_deposit_index_default, BeaconState.validators_default, BeaconState.balances_default,
            new SSZVector<>(multiply(PyList.of(eth1_block_hash), EPOCHS_PER_HISTORICAL_VECTOR)),
            BeaconState.slashings_default,
            BeaconState.previous_epoch_attestations_default,
            BeaconState.current_epoch_attestations_default,
            BeaconState.justification_bits_default,
            BeaconState.previous_justified_checkpoint_default,
            BeaconState.current_justified_checkpoint_default,
            BeaconState.finalized_checkpoint_default);
    var leaves = list(map((deposit) -> deposit.getData(), deposits));
    for (var tmp_5: enumerate(deposits)) {
      var index = tmp_5.first;
      var deposit = tmp_5.second;
      var deposit_data_list = new SSZList<DepositData>(leaves.getSlice(null, plus(index, pyint.create(1L))));
      state.getEth1_data().setDeposit_root(hash_tree_root(deposit_data_list));
      process_deposit(state, deposit);
    }
    for (var tmp_6: enumerate(state.getValidators())) {
      var index_1 = tmp_6.first;
      var validator = tmp_6.second;
      var balance = state.getBalances().get(index_1);
      validator.setEffective_balance(min(minus(balance, modulo(balance, EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE));
      if (eq(validator.getEffective_balance(), MAX_EFFECTIVE_BALANCE).v()) {
        validator.setActivation_eligibility_epoch(GENESIS_EPOCH);
        validator.setActivation_epoch(GENESIS_EPOCH);
      }
    }
    state.setGenesis_validators_root(hash_tree_root(state.getValidators()));
    return state;
  }

  public static pybool is_valid_genesis_state(BeaconState state) {
    if (less(state.getGenesis_time(), MIN_GENESIS_TIME).v()) {
      return pybool.create(false);
    }
    if (less(len(get_active_validator_indices(state, GENESIS_EPOCH)), MIN_GENESIS_ACTIVE_VALIDATOR_COUNT).v()) {
      return pybool.create(false);
    }
    return pybool.create(true);
  }

  public static void state_transition(BeaconState state, SignedBeaconBlock signed_block, pybool validate_result) {
    var block = signed_block.getMessage();
    process_slots(state, block.getSlot());
    if (validate_result.v()) {
      pyassert(verify_block_signature(state, signed_block));
    }
    process_block(state, block);
    if (validate_result.v()) {
      pyassert(eq(block.getState_root(), hash_tree_root(state)));
    }
  }

  public static pybool verify_block_signature(BeaconState state, SignedBeaconBlock signed_block) {
    var proposer = state.getValidators().get(signed_block.getMessage().getProposer_index());
    var signing_root = compute_signing_root(signed_block.getMessage(), get_domain(state, DOMAIN_BEACON_PROPOSER, null));
    return bls.Verify(proposer.getPubkey(), signing_root, signed_block.getSignature());
  }

  public static void process_slots(BeaconState state, Slot slot) {
    pyassert(less(state.getSlot(), slot));
    while (less(state.getSlot(), slot).v()) {
      process_slot(state);
      if (eq(modulo(plus(state.getSlot(), pyint.create(1L)), SLOTS_PER_EPOCH), pyint.create(0L)).v()) {
        process_epoch(state);
      }
      state.setSlot(new Slot(plus(state.getSlot(), pyint.create(1L))));
    }
  }

  public static void process_slot(BeaconState state) {
    var previous_state_root = hash_tree_root(state);
    state.getState_roots().set(modulo(state.getSlot(), SLOTS_PER_HISTORICAL_ROOT), previous_state_root);
    if (eq(state.getLatest_block_header().getState_root(), new Bytes32()).v()) {
      state.getLatest_block_header().setState_root(previous_state_root);
    }
    var previous_block_root = hash_tree_root(state.getLatest_block_header());
    state.getBlock_roots().set(modulo(state.getSlot(), SLOTS_PER_HISTORICAL_ROOT), previous_block_root);
  }

  public static void process_epoch(BeaconState state) {
    process_justification_and_finalization(state);
    process_rewards_and_penalties(state);
    process_registry_updates(state);
    process_slashings(state);
    process_eth1_data_reset(state);
    process_effective_balance_updates(state);
    process_slashings_reset(state);
    process_randao_mixes_reset(state);
    process_historical_roots_update(state);
    process_participation_record_updates(state);
  }

  public static Sequence<PendingAttestation> get_matching_source_attestations(BeaconState state, Epoch epoch) {
    pyassert(contains(new Pair<>(get_previous_epoch(state), get_current_epoch(state)), epoch));
    return eq(epoch, get_current_epoch(state)).v() ? state.getCurrent_epoch_attestations() : state.getPrevious_epoch_attestations();
  }

  public static Sequence<PendingAttestation> get_matching_target_attestations(BeaconState state, Epoch epoch) {
    return list(get_matching_source_attestations(state, epoch).filter((a) -> eq(a.getData().getTarget().getRoot(), get_block_root(state, epoch))).map((a) -> a));
  }

  public static Sequence<PendingAttestation> get_matching_head_attestations(BeaconState state, Epoch epoch) {
    return list(get_matching_target_attestations(state, epoch).filter((a) -> eq(a.getData().getBeacon_block_root(), get_block_root_at_slot(state, a.getData().getSlot()))).map((a) -> a));
  }

  public static Set<ValidatorIndex> get_unslashed_attesting_indices(BeaconState state, Sequence<PendingAttestation> attestations) {
    var output = new Set<ValidatorIndex>();
    var output_2 = output;
    for (var a: attestations) {
      var output_1 = output_2.union(get_attesting_indices(state, a.getData(), a.getAggregation_bits()));
      output_2 = output_1;
    }
    return set(filter((index) -> not(state.getValidators().get(index).getSlashed()), output_2));
  }

  /*
      Return the combined effective balance of the set of unslashed validators participating in ``attestations``.
      Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
      */
  public static Gwei get_attesting_balance(BeaconState state, Sequence<PendingAttestation> attestations) {
    return get_total_balance(state, get_unslashed_attesting_indices(state, attestations));
  }

  public static void process_justification_and_finalization(BeaconState state) {
    if (lessOrEqual(get_current_epoch(state), plus(GENESIS_EPOCH, pyint.create(1L))).v()) {
      return;
    }
    var previous_attestations = get_matching_target_attestations(state, get_previous_epoch(state));
    var current_attestations = get_matching_target_attestations(state, get_current_epoch(state));
    var total_active_balance = get_total_active_balance(state);
    var previous_target_balance = get_attesting_balance(state, previous_attestations);
    var current_target_balance = get_attesting_balance(state, current_attestations);
    weigh_justification_and_finalization(state, total_active_balance, previous_target_balance, current_target_balance);
  }

  public static void weigh_justification_and_finalization(BeaconState state, Gwei total_active_balance, Gwei previous_epoch_target_balance, Gwei current_epoch_target_balance) {
    var previous_epoch = get_previous_epoch(state);
    var current_epoch = get_current_epoch(state);
    var old_previous_justified_checkpoint = state.getPrevious_justified_checkpoint();
    var old_current_justified_checkpoint = state.getCurrent_justified_checkpoint();
    state.setPrevious_justified_checkpoint(state.getCurrent_justified_checkpoint());
    state.getJustification_bits().setSlice(pyint.create(1L), null, state.getJustification_bits().getSlice(null, minus(JUSTIFICATION_BITS_LENGTH, pyint.create(1L))));
    state.getJustification_bits().set(pyint.create(0L), new SSZBoolean(pyint.create(0L)));
    if (greaterOrEqual(multiply(previous_epoch_target_balance, pyint.create(3L)), multiply(total_active_balance, pyint.create(2L))).v()) {
      state.setCurrent_justified_checkpoint(new Checkpoint(previous_epoch, get_block_root(state, previous_epoch)));
      state.getJustification_bits().set(pyint.create(1L), new SSZBoolean(pyint.create(1L)));
    }
    if (greaterOrEqual(multiply(current_epoch_target_balance, pyint.create(3L)), multiply(total_active_balance, pyint.create(2L))).v()) {
      state.setCurrent_justified_checkpoint(new Checkpoint(current_epoch, get_block_root(state, current_epoch)));
      state.getJustification_bits().set(pyint.create(0L), new SSZBoolean(pyint.create(1L)));
    }
    var bits = state.getJustification_bits();
    if (and(all(bits.getSlice(pyint.create(1L), pyint.create(4L))), eq(plus(old_previous_justified_checkpoint.getEpoch(), pyint.create(3L)), current_epoch)).v()) {
      state.setFinalized_checkpoint(old_previous_justified_checkpoint);
    }
    if (and(all(bits.getSlice(pyint.create(1L), pyint.create(3L))), eq(plus(old_previous_justified_checkpoint.getEpoch(), pyint.create(2L)), current_epoch)).v()) {
      state.setFinalized_checkpoint(old_previous_justified_checkpoint);
    }
    if (and(all(bits.getSlice(pyint.create(0L), pyint.create(3L))), eq(plus(old_current_justified_checkpoint.getEpoch(), pyint.create(2L)), current_epoch)).v()) {
      state.setFinalized_checkpoint(old_current_justified_checkpoint);
    }
    if (and(all(bits.getSlice(pyint.create(0L), pyint.create(2L))), eq(plus(old_current_justified_checkpoint.getEpoch(), pyint.create(1L)), current_epoch)).v()) {
      state.setFinalized_checkpoint(old_current_justified_checkpoint);
    }
  }

  public static Gwei get_base_reward(BeaconState state, ValidatorIndex index) {
    var total_balance = get_total_active_balance(state);
    var effective_balance = state.getValidators().get(index).getEffective_balance();
    return new Gwei(divide(divide(multiply(effective_balance, BASE_REWARD_FACTOR), integer_squareroot(total_balance)), BASE_REWARDS_PER_EPOCH));
  }

  public static Gwei get_proposer_reward(BeaconState state, ValidatorIndex attesting_index) {
    return new Gwei(divide(get_base_reward(state, attesting_index), PROPOSER_REWARD_QUOTIENT));
  }

  public static uint64 get_finality_delay(BeaconState state) {
    return minus(get_previous_epoch(state), state.getFinalized_checkpoint().getEpoch());
  }

  public static pybool is_in_inactivity_leak(BeaconState state) {
    return greater(get_finality_delay(state), MIN_EPOCHS_TO_INACTIVITY_PENALTY);
  }

  public static Sequence<ValidatorIndex> get_eligible_validator_indices(BeaconState state) {
    var previous_epoch = get_previous_epoch(state);
    return list(enumerate(state.getValidators()).filter((tmp_7) -> { var index = tmp_7.first; var v = tmp_7.second; return or(is_active_validator(v, previous_epoch), and(v.getSlashed(), less(plus(previous_epoch, pyint.create(1L)), v.getWithdrawable_epoch()))); }).map((tmp_8) -> { var index = tmp_8.first; var v = tmp_8.second; return new ValidatorIndex(index); }));
  }

  /*
      Helper with shared logic for use by get source, target, and head deltas functions
      */
  public static Pair<Sequence<Gwei>,Sequence<Gwei>> get_attestation_component_deltas(BeaconState state, Sequence<PendingAttestation> attestations) {
    var rewards = multiply(PyList.of(new Gwei(pyint.create(0L))), len(state.getValidators()));
    var penalties = multiply(PyList.of(new Gwei(pyint.create(0L))), len(state.getValidators()));
    var total_balance = get_total_active_balance(state);
    var unslashed_attesting_indices = get_unslashed_attesting_indices(state, attestations);
    var attesting_balance = get_total_balance(state, unslashed_attesting_indices);
    for (var index: get_eligible_validator_indices(state)) {
      if (contains(unslashed_attesting_indices, index).v()) {
        var increment = EFFECTIVE_BALANCE_INCREMENT;
        if (is_in_inactivity_leak(state).v()) {
          rewards.set(index, plus(rewards.get(index), get_base_reward(state, index)));
        } else {
          var reward_numerator = multiply(get_base_reward(state, index), divide(attesting_balance, increment));
          rewards.set(index, plus(rewards.get(index), divide(reward_numerator, divide(total_balance, increment))));
        }
      } else {
        penalties.set(index, plus(penalties.get(index), get_base_reward(state, index)));
      }
    }
    return new Pair<>(rewards, penalties);
  }

  /*
      Return attester micro-rewards/penalties for source-vote for each validator.
      */
  public static Pair<Sequence<Gwei>,Sequence<Gwei>> get_source_deltas(BeaconState state) {
    var matching_source_attestations = get_matching_source_attestations(state, get_previous_epoch(state));
    return get_attestation_component_deltas(state, matching_source_attestations);
  }

  /*
      Return attester micro-rewards/penalties for target-vote for each validator.
      */
  public static Pair<Sequence<Gwei>,Sequence<Gwei>> get_target_deltas(BeaconState state) {
    var matching_target_attestations = get_matching_target_attestations(state, get_previous_epoch(state));
    return get_attestation_component_deltas(state, matching_target_attestations);
  }

  /*
      Return attester micro-rewards/penalties for head-vote for each validator.
      */
  public static Pair<Sequence<Gwei>,Sequence<Gwei>> get_head_deltas(BeaconState state) {
    var matching_head_attestations = get_matching_head_attestations(state, get_previous_epoch(state));
    return get_attestation_component_deltas(state, matching_head_attestations);
  }

  /*
      Return proposer and inclusion delay micro-rewards/penalties for each validator.
      */
  public static Pair<Sequence<Gwei>,Sequence<Gwei>> get_inclusion_delay_deltas(BeaconState state) {
    var rewards = list(range(len(state.getValidators())).map((_0) -> new Gwei(pyint.create(0L))));
    var matching_source_attestations = get_matching_source_attestations(state, get_previous_epoch(state));
    for (var index: get_unslashed_attesting_indices(state, matching_source_attestations)) {
      var attestation = min(list(matching_source_attestations.filter((a) -> contains(get_attesting_indices(state, a.getData(), a.getAggregation_bits()), index)).map((a) -> a)), (a) -> a.getInclusion_delay());
      rewards.set(attestation.getProposer_index(), plus(rewards.get(attestation.getProposer_index()), get_proposer_reward(state, index)));
      var max_attester_reward = new Gwei(minus(get_base_reward(state, index), get_proposer_reward(state, index)));
      rewards.set(index, plus(rewards.get(index), new Gwei(divide(max_attester_reward, attestation.getInclusion_delay()))));
    }
    var penalties = list(range(len(state.getValidators())).map((_0) -> new Gwei(pyint.create(0L))));
    return new Pair<>(rewards, penalties);
  }

  /*
      Return inactivity reward/penalty deltas for each validator.
      */
  public static Pair<Sequence<Gwei>,Sequence<Gwei>> get_inactivity_penalty_deltas(BeaconState state) {
    var penalties = list(range(len(state.getValidators())).map((_0) -> new Gwei(pyint.create(0L))));
    if (is_in_inactivity_leak(state).v()) {
      var matching_target_attestations = get_matching_target_attestations(state, get_previous_epoch(state));
      var matching_target_attesting_indices = get_unslashed_attesting_indices(state, matching_target_attestations);
      for (var index: get_eligible_validator_indices(state)) {
        var base_reward = get_base_reward(state, index);
        penalties.set(index, plus(penalties.get(index), new Gwei(minus(multiply(BASE_REWARDS_PER_EPOCH, base_reward), get_proposer_reward(state, index)))));
        if (not(contains(matching_target_attesting_indices, index)).v()) {
          var effective_balance = state.getValidators().get(index).getEffective_balance();
          penalties.set(index, plus(penalties.get(index), new Gwei(divide(multiply(effective_balance, get_finality_delay(state)), INACTIVITY_PENALTY_QUOTIENT))));
        }
      }
    }
    var rewards = list(range(len(state.getValidators())).map((_0) -> new Gwei(pyint.create(0L))));
    return new Pair<>(rewards, penalties);
  }

  /*
      Return attestation reward/penalty deltas for each validator.
      */
  public static Pair<Sequence<Gwei>,Sequence<Gwei>> get_attestation_deltas(BeaconState state) {
    var tmp_9 = get_source_deltas(state);
    var source_rewards = tmp_9.first;
    var source_penalties = tmp_9.second;
    var tmp_10 = get_target_deltas(state);
    var target_rewards = tmp_10.first;
    var target_penalties = tmp_10.second;
    var tmp_11 = get_head_deltas(state);
    var head_rewards = tmp_11.first;
    var head_penalties = tmp_11.second;
    var tmp_12 = get_inclusion_delay_deltas(state);
    var inclusion_delay_rewards = tmp_12.first;
    var __ = tmp_12.second;
    var tmp_13 = get_inactivity_penalty_deltas(state);
    var __1 = tmp_13.first;
    var inactivity_penalties = tmp_13.second;
    var rewards = list(range(len(state.getValidators())).map((i) -> plus(plus(plus(source_rewards.get(i), target_rewards.get(i)), head_rewards.get(i)), inclusion_delay_rewards.get(i))));
    var penalties = list(range(len(state.getValidators())).map((i) -> plus(plus(plus(source_penalties.get(i), target_penalties.get(i)), head_penalties.get(i)), inactivity_penalties.get(i))));
    return new Pair<>(rewards, penalties);
  }

  public static void process_rewards_and_penalties(BeaconState state) {
    if (eq(get_current_epoch(state), GENESIS_EPOCH).v()) {
      return;
    }
    var tmp_14 = get_attestation_deltas(state);
    var rewards = tmp_14.first;
    var penalties = tmp_14.second;
    for (var index: range(len(state.getValidators()))) {
      increase_balance(state, new ValidatorIndex(index), rewards.get(index));
      decrease_balance(state, new ValidatorIndex(index), penalties.get(index));
    }
  }

  public static void process_registry_updates(BeaconState state) {
    for (var tmp_15: enumerate(state.getValidators())) {
      var index = tmp_15.first;
      var validator = tmp_15.second;
      if (is_eligible_for_activation_queue(validator).v()) {
        validator.setActivation_eligibility_epoch(plus(get_current_epoch(state), pyint.create(1L)));
      }
      if (and(is_active_validator(validator, get_current_epoch(state)), lessOrEqual(validator.getEffective_balance(), EJECTION_BALANCE)).v()) {
        initiate_validator_exit(state, new ValidatorIndex(index));
      }
    }
    var activation_queue = sorted(list(enumerate(state.getValidators()).filter((tmp_16) -> { var index = tmp_16.first; var validator = tmp_16.second; return is_eligible_for_activation(state, validator); }).map((tmp_17) -> { var index = tmp_17.first; var validator = tmp_17.second; return index; })), (index) -> new Pair<>(state.getValidators().get(index).getActivation_eligibility_epoch(), index));
    for (var index_1: activation_queue.getSlice(null, get_validator_churn_limit(state))) {
      var validator_1 = state.getValidators().get(index_1);
      validator_1.setActivation_epoch(compute_activation_exit_epoch(get_current_epoch(state)));
    }
  }

  public static void process_slashings(BeaconState state) {
    var epoch = get_current_epoch(state);
    var total_balance = get_total_active_balance(state);
    var adjusted_total_slashing_balance = min(multiply(sum(state.getSlashings()), PROPORTIONAL_SLASHING_MULTIPLIER), total_balance);
    for (var tmp_18: enumerate(state.getValidators())) {
      var index = tmp_18.first;
      var validator = tmp_18.second;
      if (and(validator.getSlashed(), eq(plus(epoch, divide(EPOCHS_PER_SLASHINGS_VECTOR, pyint.create(2L))), validator.getWithdrawable_epoch())).v()) {
        var increment = EFFECTIVE_BALANCE_INCREMENT;
        var penalty_numerator = multiply(divide(validator.getEffective_balance(), increment), adjusted_total_slashing_balance);
        var penalty = multiply(divide(penalty_numerator, total_balance), increment);
        decrease_balance(state, new ValidatorIndex(index), penalty);
      }
    }
  }

  public static void process_eth1_data_reset(BeaconState state) {
    var next_epoch = new Epoch(plus(get_current_epoch(state), pyint.create(1L)));
    if (eq(modulo(next_epoch, EPOCHS_PER_ETH1_VOTING_PERIOD), pyint.create(0L)).v()) {
      state.setEth1_data_votes(new SSZList<Eth1Data>());
    }
  }

  public static void process_effective_balance_updates(BeaconState state) {
    for (var tmp_19: enumerate(state.getValidators())) {
      var index = tmp_19.first;
      var validator = tmp_19.second;
      var balance = state.getBalances().get(index);
      var HYSTERESIS_INCREMENT = new uint64(divide(EFFECTIVE_BALANCE_INCREMENT, HYSTERESIS_QUOTIENT));
      var DOWNWARD_THRESHOLD = multiply(HYSTERESIS_INCREMENT, HYSTERESIS_DOWNWARD_MULTIPLIER);
      var UPWARD_THRESHOLD = multiply(HYSTERESIS_INCREMENT, HYSTERESIS_UPWARD_MULTIPLIER);
      if (or(less(plus(balance, DOWNWARD_THRESHOLD), validator.getEffective_balance()), less(plus(validator.getEffective_balance(), UPWARD_THRESHOLD), balance)).v()) {
        validator.setEffective_balance(min(minus(balance, modulo(balance, EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE));
      }
    }
  }

  public static void process_slashings_reset(BeaconState state) {
    var next_epoch = new Epoch(plus(get_current_epoch(state), pyint.create(1L)));
    state.getSlashings().set(modulo(next_epoch, EPOCHS_PER_SLASHINGS_VECTOR), new Gwei(pyint.create(0L)));
  }

  public static void process_randao_mixes_reset(BeaconState state) {
    var current_epoch = get_current_epoch(state);
    var next_epoch = new Epoch(plus(current_epoch, pyint.create(1L)));
    state.getRandao_mixes().set(modulo(next_epoch, EPOCHS_PER_HISTORICAL_VECTOR), get_randao_mix(state, current_epoch));
  }

  public static void process_historical_roots_update(BeaconState state) {
    var next_epoch = new Epoch(plus(get_current_epoch(state), pyint.create(1L)));
    if (eq(modulo(next_epoch, divide(SLOTS_PER_HISTORICAL_ROOT, SLOTS_PER_EPOCH)), pyint.create(0L)).v()) {
      var historical_batch = new HistoricalBatch(state.getBlock_roots(), state.getState_roots());
      state.getHistorical_roots().append(hash_tree_root(historical_batch));
    }
  }

  public static void process_participation_record_updates(BeaconState state) {
    state.setPrevious_epoch_attestations(state.getCurrent_epoch_attestations());
    state.setCurrent_epoch_attestations(new SSZList<PendingAttestation>());
  }

  public static void process_block(BeaconState state, BeaconBlock block) {
    process_block_header(state, block);
    process_randao(state, block.getBody());
    process_eth1_data(state, block.getBody());
    process_operations(state, block.getBody());
  }

  public static void process_block_header(BeaconState state, BeaconBlock block) {
    pyassert(eq(block.getSlot(), state.getSlot()));
    pyassert(greater(block.getSlot(), state.getLatest_block_header().getSlot()));
    pyassert(eq(block.getProposer_index(), get_beacon_proposer_index(state)));
    pyassert(eq(block.getParent_root(), hash_tree_root(state.getLatest_block_header())));
    state.setLatest_block_header(new BeaconBlockHeader(block.getSlot(), block.getProposer_index(), block.getParent_root(), new Root(), hash_tree_root(block.getBody())));
    var proposer = state.getValidators().get(block.getProposer_index());
    pyassert(not(proposer.getSlashed()));
  }

  public static void process_randao(BeaconState state, BeaconBlockBody body) {
    var epoch = get_current_epoch(state);
    var proposer = state.getValidators().get(get_beacon_proposer_index(state));
    var signing_root = compute_signing_root(epoch, get_domain(state, DOMAIN_RANDAO, null));
    pyassert(bls.Verify(proposer.getPubkey(), signing_root, body.getRandao_reveal()));
    var mix = xor(get_randao_mix(state, epoch), hash(body.getRandao_reveal()));
    state.getRandao_mixes().set(modulo(epoch, EPOCHS_PER_HISTORICAL_VECTOR), mix);
  }

  public static void process_eth1_data(BeaconState state, BeaconBlockBody body) {
    state.getEth1_data_votes().append(body.getEth1_data());
    if (greater(multiply(state.getEth1_data_votes().count(body.getEth1_data()), pyint.create(2L)), multiply(EPOCHS_PER_ETH1_VOTING_PERIOD, SLOTS_PER_EPOCH)).v()) {
      state.setEth1_data(body.getEth1_data());
    }
  }

  public static void process_operations(BeaconState state, BeaconBlockBody body) {
    pyassert(eq(len(body.getDeposits()), min(MAX_DEPOSITS, minus(state.getEth1_data().getDeposit_count(), state.getEth1_deposit_index()))));
    for (var operation: body.getProposer_slashings()) {
      process_proposer_slashing(state, operation);
    }
    for (var operation_1: body.getAttester_slashings()) {
      process_attester_slashing(state, operation_1);
    }
    for (var operation_2: body.getAttestations()) {
      process_attestation(state, operation_2);
    }
    for (var operation_3: body.getDeposits()) {
      process_deposit(state, operation_3);
    }
    for (var operation_4: body.getVoluntary_exits()) {
      process_voluntary_exit(state, operation_4);
    }
  }

  public static void process_proposer_slashing(BeaconState state, ProposerSlashing proposer_slashing) {
    var header_1 = proposer_slashing.getSigned_header_1().getMessage();
    var header_2 = proposer_slashing.getSigned_header_2().getMessage();
    pyassert(eq(header_1.getSlot(), header_2.getSlot()));
    pyassert(eq(header_1.getProposer_index(), header_2.getProposer_index()));
    pyassert(not(eq(header_1, header_2)));
    var proposer = state.getValidators().get(header_1.getProposer_index());
    pyassert(is_slashable_validator(proposer, get_current_epoch(state)));
    for (var signed_header: Pair.of(proposer_slashing.getSigned_header_1(), proposer_slashing.getSigned_header_2())) {
      var domain = get_domain(state, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(signed_header.getMessage().getSlot()));
      var signing_root = compute_signing_root(signed_header.getMessage(), domain);
      pyassert(bls.Verify(proposer.getPubkey(), signing_root, signed_header.getSignature()));
    }
    slash_validator(state, header_1.getProposer_index(), null);
  }

  public static void process_attester_slashing(BeaconState state, AttesterSlashing attester_slashing) {
    var attestation_1 = attester_slashing.getAttestation_1();
    var attestation_2 = attester_slashing.getAttestation_2();
    pyassert(is_slashable_attestation_data(attestation_1.getData(), attestation_2.getData()));
    pyassert(is_valid_indexed_attestation(state, attestation_1));
    pyassert(is_valid_indexed_attestation(state, attestation_2));
    var slashed_any = pybool.create(false);
    var indices = set(attestation_1.getAttesting_indices()).intersection(attestation_2.getAttesting_indices());
    var slashed_any_2 = slashed_any;
    for (var index: sorted(indices)) {
      if (is_slashable_validator(state.getValidators().get(index), get_current_epoch(state)).v()) {
        slash_validator(state, index, null);
        var slashed_any_1 = pybool.create(true);
        slashed_any_2 = slashed_any_1;
      } else {
        slashed_any_2 = slashed_any_2;
      }
    }
    pyassert(slashed_any_2);
  }

  public static void process_attestation(BeaconState state, Attestation attestation) {
    var data = attestation.getData();
    pyassert(contains(new Pair<>(get_previous_epoch(state), get_current_epoch(state)), data.getTarget().getEpoch()));
    pyassert(eq(data.getTarget().getEpoch(), compute_epoch_at_slot(data.getSlot())));
    pyassert(and(lessOrEqual(plus(data.getSlot(), MIN_ATTESTATION_INCLUSION_DELAY), state.getSlot()), lessOrEqual(state.getSlot(), plus(data.getSlot(), SLOTS_PER_EPOCH))));
    pyassert(less(data.getIndex(), get_committee_count_per_slot(state, data.getTarget().getEpoch())));
    var committee = get_beacon_committee(state, data.getSlot(), data.getIndex());
    pyassert(eq(len(attestation.getAggregation_bits()), len(committee)));
    var pending_attestation = new PendingAttestation(attestation.getAggregation_bits(), data, minus(state.getSlot(), data.getSlot()), get_beacon_proposer_index(state));
    if (eq(data.getTarget().getEpoch(), get_current_epoch(state)).v()) {
      pyassert(eq(data.getSource(), state.getCurrent_justified_checkpoint()));
      state.getCurrent_epoch_attestations().append(pending_attestation);
    } else {
      pyassert(eq(data.getSource(), state.getPrevious_justified_checkpoint()));
      state.getPrevious_epoch_attestations().append(pending_attestation);
    }
    pyassert(is_valid_indexed_attestation(state, get_indexed_attestation(state, attestation)));
  }

  public static Validator get_validator_from_deposit(BeaconState state, Deposit deposit) {
    var amount = deposit.getData().getAmount();
    var effective_balance = min(minus(amount, modulo(amount, EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE);
    return new Validator(deposit.getData().getPubkey(), deposit.getData().getWithdrawal_credentials(), effective_balance, Validator.slashed_default, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH);
  }

  public static void process_deposit(BeaconState state, Deposit deposit) {
    pyassert(is_valid_merkle_branch(hash_tree_root(deposit.getData()), deposit.getProof(), plus(DEPOSIT_CONTRACT_TREE_DEPTH, pyint.create(1L)), state.getEth1_deposit_index(), state.getEth1_data().getDeposit_root()));
    state.setEth1_deposit_index(plus(state.getEth1_deposit_index(), pyint.create(1L)));
    var pubkey = deposit.getData().getPubkey();
    var amount = deposit.getData().getAmount();
    var validator_pubkeys = list(state.getValidators().map((v) -> v.getPubkey()));
    if (not(contains(validator_pubkeys, pubkey)).v()) {
      var deposit_message = new DepositMessage(deposit.getData().getPubkey(), deposit.getData().getWithdrawal_credentials(), deposit.getData().getAmount());
      var domain = compute_domain(DOMAIN_DEPOSIT, null, null);
      var signing_root = compute_signing_root(deposit_message, domain);
      if (not(bls.Verify(pubkey, signing_root, deposit.getData().getSignature())).v()) {
        return;
      }
      state.getValidators().append(get_validator_from_deposit(state, deposit));
      state.getBalances().append(amount);
    } else {
      var index = new ValidatorIndex(validator_pubkeys.index(pubkey));
      increase_balance(state, index, amount);
    }
  }

  public static void process_voluntary_exit(BeaconState state, SignedVoluntaryExit signed_voluntary_exit) {
    var voluntary_exit = signed_voluntary_exit.getMessage();
    var validator = state.getValidators().get(voluntary_exit.getValidator_index());
    pyassert(is_active_validator(validator, get_current_epoch(state)));
    pyassert(eq(validator.getExit_epoch(), FAR_FUTURE_EPOCH));
    pyassert(greaterOrEqual(get_current_epoch(state), voluntary_exit.getEpoch()));
    pyassert(greaterOrEqual(get_current_epoch(state), plus(validator.getActivation_epoch(), SHARD_COMMITTEE_PERIOD)));
    var domain = get_domain(state, DOMAIN_VOLUNTARY_EXIT, voluntary_exit.getEpoch());
    var signing_root = compute_signing_root(voluntary_exit, domain);
    pyassert(bls.Verify(validator.getPubkey(), signing_root, signed_voluntary_exit.getSignature()));
    initiate_validator_exit(state, voluntary_exit.getValidator_index());
  }

  public static Store get_forkchoice_store(BeaconState anchor_state, BeaconBlock anchor_block) {
    pyassert(eq(anchor_block.getState_root(), hash_tree_root(anchor_state)));
    var anchor_root = hash_tree_root(anchor_block);
    var anchor_epoch = get_current_epoch(anchor_state);
    var justified_checkpoint = new Checkpoint(anchor_epoch, anchor_root);
    var finalized_checkpoint = new Checkpoint(anchor_epoch, anchor_root);
    return new Store(new uint64(plus(anchor_state.getGenesis_time(), multiply(SECONDS_PER_SLOT, anchor_state.getSlot()))), anchor_state.getGenesis_time(), justified_checkpoint, finalized_checkpoint, justified_checkpoint, PyDict.of(new Pair<>(anchor_root, copy(anchor_block))), PyDict.of(new Pair<>(anchor_root, copy(anchor_state))), PyDict.of(new Pair<>(justified_checkpoint, copy(anchor_state))), Store.latest_messages_default);
  }

  public static pyint get_slots_since_genesis(Store store) {
    return divide(minus(store.getTime(), store.getGenesis_time()), SECONDS_PER_SLOT);
  }

  public static Slot get_current_slot(Store store) {
    return new Slot(plus(GENESIS_SLOT, get_slots_since_genesis(store)));
  }

  public static pyint compute_slots_since_epoch_start(Slot slot) {
    return minus(slot, compute_start_slot_at_epoch(compute_epoch_at_slot(slot)));
  }

  public static Root get_ancestor(Store store, Root root, Slot slot) {
    var block = store.getBlocks().get(root);
    if (greater(block.getSlot(), slot).v()) {
      return get_ancestor(store, block.getParent_root(), slot);
    } else {
      if (eq(block.getSlot(), slot).v()) {
        return root;
      } else {
        return root;
      }
    }
  }

  public static Gwei get_latest_attesting_balance(Store store, Root root) {
    var state = store.getCheckpoint_states().get(store.getJustified_checkpoint());
    var active_indices = get_active_validator_indices(state, get_current_epoch(state));
    return new Gwei(sum(active_indices.filter((i) -> and(contains(store.getLatest_messages(), i), eq(get_ancestor(store, store.getLatest_messages().get(i).getRoot(), store.getBlocks().get(root).getSlot()), root))).map((i) -> state.getValidators().get(i).getEffective_balance())));
  }

  public static pybool filter_block_tree(Store store, Root block_root, PyDict<Root,BeaconBlock> blocks) {
    var block = store.getBlocks().get(block_root);
    var children = list(store.getBlocks().keys().filter((root) -> eq(store.getBlocks().get(root).getParent_root(), block_root)).map((root) -> root));
    if (any(children).v()) {
      var filter_block_tree_result = list(children.map((child) -> filter_block_tree(store, child, blocks)));
      if (any(filter_block_tree_result).v()) {
        blocks.set(block_root, block);
        return pybool.create(true);
      }
      return pybool.create(false);
    }
    var head_state = store.getBlock_states().get(block_root);
    var correct_justified = or(eq(store.getJustified_checkpoint().getEpoch(), GENESIS_EPOCH), eq(head_state.getCurrent_justified_checkpoint(), store.getJustified_checkpoint()));
    var correct_finalized = or(eq(store.getFinalized_checkpoint().getEpoch(), GENESIS_EPOCH), eq(head_state.getFinalized_checkpoint(), store.getFinalized_checkpoint()));
    if (and(correct_justified, correct_finalized).v()) {
      blocks.set(block_root, block);
      return pybool.create(true);
    }
    return pybool.create(false);
  }

  /*
      Retrieve a filtered block tree from ``store``, only returning branches
      whose leaf state's justified/finalized info agrees with that in ``store``.
      */
  public static PyDict<Root,BeaconBlock> get_filtered_block_tree(Store store) {
    var base = store.getJustified_checkpoint().getRoot();
    PyDict<Root,BeaconBlock> blocks = PyDict.of();
    filter_block_tree(store, base, blocks);
    return blocks;
  }

  public static Root get_head(Store store) {
    var blocks = get_filtered_block_tree(store);
    var head = store.getJustified_checkpoint().getRoot();
    var head_2 = head;
    while (true) {
      Root finalHead_ = head_2;
      var children = list(blocks.keys().filter((root) -> eq(blocks.get(root).getParent_root(), finalHead_)).map((root) -> root));
      if (eq(len(children), pyint.create(0L)).v()) {
        return head_2;
      }
      var head_1 = max(children, (root) -> new Pair<>(get_latest_attesting_balance(store, root), root));
      head_2 = head_1;
    }
  }

  /*
      To address the bouncing attack, only update conflicting justified
      checkpoints in the fork choice if in the early slots of the epoch.
      Otherwise, delay incorporation of new justified checkpoint until next epoch boundary.

      See https://ethresear.ch/t/prevention-of-bouncing-attack-on-ffg/6114 for more detailed analysis and discussion.
      */
  public static pybool should_update_justified_checkpoint(Store store, Checkpoint new_justified_checkpoint) {
    if (less(compute_slots_since_epoch_start(get_current_slot(store)), SAFE_SLOTS_TO_UPDATE_JUSTIFIED).v()) {
      return pybool.create(true);
    }
    var justified_slot = compute_start_slot_at_epoch(store.getJustified_checkpoint().getEpoch());
    if (not(eq(get_ancestor(store, new_justified_checkpoint.getRoot(), justified_slot), store.getJustified_checkpoint().getRoot())).v()) {
      return pybool.create(false);
    }
    return pybool.create(true);
  }

  public static void validate_on_attestation(Store store, Attestation attestation) {
    var target = attestation.getData().getTarget();
    var current_epoch = compute_epoch_at_slot(get_current_slot(store));
    var previous_epoch = greater(current_epoch, GENESIS_EPOCH).v() ? minus(current_epoch, pyint.create(1L)) : GENESIS_EPOCH;
    pyassert(contains(PyList.of(current_epoch, previous_epoch), target.getEpoch()));
    pyassert(eq(target.getEpoch(), compute_epoch_at_slot(attestation.getData().getSlot())));
    pyassert(contains(store.getBlocks(), target.getRoot()));
    pyassert(contains(store.getBlocks(), attestation.getData().getBeacon_block_root()));
    pyassert(lessOrEqual(store.getBlocks().get(attestation.getData().getBeacon_block_root()).getSlot(), attestation.getData().getSlot()));
    var target_slot = compute_start_slot_at_epoch(target.getEpoch());
    pyassert(eq(target.getRoot(), get_ancestor(store, attestation.getData().getBeacon_block_root(), target_slot)));
    pyassert(greaterOrEqual(get_current_slot(store), plus(attestation.getData().getSlot(), pyint.create(1L))));
  }

  public static void store_target_checkpoint_state(Store store, Checkpoint target) {
    if (not(contains(store.getCheckpoint_states(), target)).v()) {
      var base_state = copy(store.getBlock_states().get(target.getRoot()));
      if (less(base_state.getSlot(), compute_start_slot_at_epoch(target.getEpoch())).v()) {
        process_slots(base_state, compute_start_slot_at_epoch(target.getEpoch()));
      }
      store.getCheckpoint_states().set(target, base_state);
    }
  }

  public static void update_latest_messages(Store store, Sequence<ValidatorIndex> attesting_indices, Attestation attestation) {
    var target = attestation.getData().getTarget();
    var beacon_block_root = attestation.getData().getBeacon_block_root();
    for (var i: attesting_indices) {
      if (or(not(contains(store.getLatest_messages(), i)), greater(target.getEpoch(), store.getLatest_messages().get(i).getEpoch())).v()) {
        store.getLatest_messages().set(i, new LatestMessage(target.getEpoch(), beacon_block_root));
      }
    }
  }

  public static void on_tick(Store store, uint64 time) {
    var previous_slot = get_current_slot(store);
    store.setTime(time);
    var current_slot = get_current_slot(store);
    if (not(and(greater(current_slot, previous_slot), eq(compute_slots_since_epoch_start(current_slot), pyint.create(0L)))).v()) {
      return;
    }
    if (greater(store.getBest_justified_checkpoint().getEpoch(), store.getJustified_checkpoint().getEpoch()).v()) {
      store.setJustified_checkpoint(store.getBest_justified_checkpoint());
    }
  }

  public static void on_block(Store store, SignedBeaconBlock signed_block) {
    var block = signed_block.getMessage();
    pyassert(contains(store.getBlock_states(), block.getParent_root()));
    var pre_state = copy(store.getBlock_states().get(block.getParent_root()));
    pyassert(greaterOrEqual(get_current_slot(store), block.getSlot()));
    var finalized_slot = compute_start_slot_at_epoch(store.getFinalized_checkpoint().getEpoch());
    pyassert(greater(block.getSlot(), finalized_slot));
    pyassert(eq(get_ancestor(store, block.getParent_root(), finalized_slot), store.getFinalized_checkpoint().getRoot()));
    var state = pre_state.copy();
    state_transition(state, signed_block, pybool.create(true));
    store.getBlocks().set(hash_tree_root(block), block);
    store.getBlock_states().set(hash_tree_root(block), state);
    if (greater(state.getCurrent_justified_checkpoint().getEpoch(), store.getJustified_checkpoint().getEpoch()).v()) {
      if (greater(state.getCurrent_justified_checkpoint().getEpoch(), store.getBest_justified_checkpoint().getEpoch()).v()) {
        store.setBest_justified_checkpoint(state.getCurrent_justified_checkpoint());
      }
      if (should_update_justified_checkpoint(store, state.getCurrent_justified_checkpoint()).v()) {
        store.setJustified_checkpoint(state.getCurrent_justified_checkpoint());
      }
    }
    if (greater(state.getFinalized_checkpoint().getEpoch(), store.getFinalized_checkpoint().getEpoch()).v()) {
      store.setFinalized_checkpoint(state.getFinalized_checkpoint());
      if (not(eq(store.getJustified_checkpoint(), state.getCurrent_justified_checkpoint())).v()) {
        if (greater(state.getCurrent_justified_checkpoint().getEpoch(), store.getJustified_checkpoint().getEpoch()).v()) {
          store.setJustified_checkpoint(state.getCurrent_justified_checkpoint());
          return;
        }
        var finalized_slot_1 = compute_start_slot_at_epoch(store.getFinalized_checkpoint().getEpoch());
        var ancestor_at_finalized_slot = get_ancestor(store, store.getJustified_checkpoint().getRoot(), finalized_slot_1);
        if (not(eq(ancestor_at_finalized_slot, store.getFinalized_checkpoint().getRoot())).v()) {
          store.setJustified_checkpoint(state.getCurrent_justified_checkpoint());
        }
      }
    }
  }

  /*
      Run ``on_attestation`` upon receiving a new ``attestation`` from either within a block or directly on the wire.

      An ``attestation`` that is asserted as invalid may be valid at a later time,
      consider scheduling it for later processing in such case.
      */
  public static void on_attestation(Store store, Attestation attestation) {
    validate_on_attestation(store, attestation);
    store_target_checkpoint_state(store, attestation.getData().getTarget());
    var target_state = store.getCheckpoint_states().get(attestation.getData().getTarget());
    var indexed_attestation = get_indexed_attestation(target_state, attestation);
    pyassert(is_valid_indexed_attestation(target_state, indexed_attestation));
    update_latest_messages(store, indexed_attestation.getAttesting_indices(), attestation);
  }

  public static pybool check_if_validator_active(BeaconState state, ValidatorIndex validator_index) {
    var validator = state.getValidators().get(validator_index);
    return is_active_validator(validator, get_current_epoch(state));
  }

  /*
      Return the committee assignment in the ``epoch`` for ``validator_index``.
      ``assignment`` returned is a tuple of the following form:
          * ``assignmentlistOf(0)`` is the list of validators in the committee
          * ``assignmentlistOf(1)`` is the index to which the committee is assigned
          * ``assignmentlistOf(2)`` is the slot at which the committee is assigned
      Return None if no assignment.
      */
  public static Triple<Sequence<ValidatorIndex>,CommitteeIndex,Slot> get_committee_assignment(BeaconState state, Epoch epoch, ValidatorIndex validator_index) {
    var next_epoch = new Epoch(plus(get_current_epoch(state), pyint.create(1L)));
    pyassert(lessOrEqual(epoch, next_epoch));
    var start_slot = compute_start_slot_at_epoch(epoch);
    var committee_count_per_slot = get_committee_count_per_slot(state, epoch);
    for (var slot: range(start_slot, plus(start_slot, SLOTS_PER_EPOCH))) {
      for (var index: range(committee_count_per_slot)) {
        var committee = get_beacon_committee(state, new Slot(slot), new CommitteeIndex(index));
        if (contains(committee, validator_index).v()) {
          return new Triple<>(committee, new CommitteeIndex(index), new Slot(slot));
        }
      }
    }
    return null;
  }

  public static pybool is_proposer(BeaconState state, ValidatorIndex validator_index) {
    return eq(get_beacon_proposer_index(state), validator_index);
  }

  public static BLSSignature get_epoch_signature(BeaconState state, BeaconBlock block, pyint privkey) {
    var domain = get_domain(state, DOMAIN_RANDAO, compute_epoch_at_slot(block.getSlot()));
    var signing_root = compute_signing_root(compute_epoch_at_slot(block.getSlot()), domain);
    return bls.Sign(privkey, signing_root);
  }

  public static uint64 compute_time_at_slot(BeaconState state, Slot slot) {
    return new uint64(plus(state.getGenesis_time(), multiply(slot, SECONDS_PER_SLOT)));
  }

  public static uint64 voting_period_start_time(BeaconState state) {
    var eth1_voting_period_start_slot = new Slot(minus(state.getSlot(), modulo(state.getSlot(), multiply(EPOCHS_PER_ETH1_VOTING_PERIOD, SLOTS_PER_EPOCH))));
    return compute_time_at_slot(state, eth1_voting_period_start_slot);
  }

  public static pybool is_candidate_block(Eth1Block block, uint64 period_start) {
    return and(lessOrEqual(plus(block.getTimestamp(), multiply(SECONDS_PER_ETH1_BLOCK, ETH1_FOLLOW_DISTANCE)), period_start), greaterOrEqual(plus(block.getTimestamp(), multiply(multiply(SECONDS_PER_ETH1_BLOCK, ETH1_FOLLOW_DISTANCE), pyint.create(2L))), period_start));
  }

  public static Eth1Data get_eth1_vote(BeaconState state, Sequence<Eth1Block> eth1_chain) {
    var period_start = voting_period_start_time(state);
    var votes_to_consider = list(eth1_chain.filter((block) -> and(is_candidate_block(block, period_start), greaterOrEqual(get_eth1_data(block).getDeposit_count(), state.getEth1_data().getDeposit_count()))).map((block) -> get_eth1_data(block)));
    var valid_votes = list(state.getEth1_data_votes().filter((vote) -> contains(votes_to_consider, vote)).map((vote) -> vote));
    Eth1Data state_eth1_data = state.getEth1_data();
    var default_vote = any(votes_to_consider).v() ? votes_to_consider.get(minus(len(votes_to_consider), pyint.create(1L))) : state_eth1_data;
    return max(valid_votes, (v) -> new Pair<>(valid_votes.count(v), uminus(valid_votes.index(v))), default_vote);
  }

  public static Root compute_new_state_root(BeaconState state, BeaconBlock block) {
    BeaconState temp_state = state.copy();
    var signed_block = new SignedBeaconBlock(block, SignedBeaconBlock.signature_default);
    state_transition(temp_state, signed_block, pybool.create(false));
    return hash_tree_root(temp_state);
  }

  public static BLSSignature get_block_signature(BeaconState state, BeaconBlock block, pyint privkey) {
    var domain = get_domain(state, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(block.getSlot()));
    var signing_root = compute_signing_root(block, domain);
    return bls.Sign(privkey, signing_root);
  }

  public static BLSSignature get_attestation_signature(BeaconState state, AttestationData attestation_data, pyint privkey) {
    var domain = get_domain(state, DOMAIN_BEACON_ATTESTER, attestation_data.getTarget().getEpoch());
    var signing_root = compute_signing_root(attestation_data, domain);
    return bls.Sign(privkey, signing_root);
  }

  /*
      Compute the correct subnet for an attestation for Phase 0.
      Note, this mimics expected future behavior where attestations will be mapped to their shard subnet.
      */
  public static uint64 compute_subnet_for_attestation(uint64 committees_per_slot, Slot slot, CommitteeIndex committee_index) {
    var slots_since_epoch_start = new uint64(modulo(slot, SLOTS_PER_EPOCH));
    var committees_since_epoch_start = multiply(committees_per_slot, slots_since_epoch_start);
    return new uint64(modulo(plus(committees_since_epoch_start, committee_index), ATTESTATION_SUBNET_COUNT));
  }

  public static BLSSignature get_slot_signature(BeaconState state, Slot slot, pyint privkey) {
    var domain = get_domain(state, DOMAIN_SELECTION_PROOF, compute_epoch_at_slot(slot));
    var signing_root = compute_signing_root(slot, domain);
    return bls.Sign(privkey, signing_root);
  }

  public static pybool is_aggregator(BeaconState state, Slot slot, CommitteeIndex index, BLSSignature slot_signature) {
    var committee = get_beacon_committee(state, slot, index);
    var modulo = max(pyint.create(1L), divide(len(committee), TARGET_AGGREGATORS_PER_COMMITTEE));
    return eq(modulo(bytes_to_uint64(hash(slot_signature).getSlice(pyint.create(0L), pyint.create(8L))), modulo), pyint.create(0L));
  }

  public static BLSSignature get_aggregate_signature(Sequence<Attestation> attestations) {
    var signatures = list(attestations.map((attestation) -> attestation.getSignature()));
    return bls.Aggregate(signatures);
  }

  public static AggregateAndProof get_aggregate_and_proof(BeaconState state, ValidatorIndex aggregator_index, Attestation aggregate, pyint privkey) {
    return new AggregateAndProof(aggregator_index, aggregate, get_slot_signature(state, aggregate.getData().getSlot(), privkey));
  }

  public static BLSSignature get_aggregate_and_proof_signature(BeaconState state, AggregateAndProof aggregate_and_proof, pyint privkey) {
    var aggregate = aggregate_and_proof.getAggregate();
    var domain = get_domain(state, DOMAIN_AGGREGATE_AND_PROOF, compute_epoch_at_slot(aggregate.getData().getSlot()));
    var signing_root = compute_signing_root(aggregate_and_proof, domain);
    return bls.Sign(privkey, signing_root);
  }

  /*
      Returns the weak subjectivity period for the current ``state``.
      This computation takes into account the effect of:
          - validator set churn (bounded by ``get_validator_churn_limit()`` per epoch), and
          - validator balance top-ups (bounded by ``MAX_DEPOSITS * SLOTS_PER_EPOCH`` per epoch).
      A detailed calculation can be found at:
      https://github.com/runtimeverification/beacon-chain-verification/blob/master/weak-subjectivity/weak-subjectivity-analysis.pdf
      */
  public static uint64 compute_weak_subjectivity_period(BeaconState state) {
    var ws_period = MIN_VALIDATOR_WITHDRAWABILITY_DELAY;
    var N = len(get_active_validator_indices(state, get_current_epoch(state)));
    var t = divide(divide(get_total_active_balance(state), N), ETH_TO_GWEI);
    var T = divide(MAX_EFFECTIVE_BALANCE, ETH_TO_GWEI);
    var delta = get_validator_churn_limit(state);
    var Delta = multiply(MAX_DEPOSITS, SLOTS_PER_EPOCH);
    var D = SAFETY_DECAY;
    uint64 ws_period_3;
    if (less(multiply(T, plus(pyint.create(200L), multiply(pyint.create(3L), D))), multiply(t, plus(pyint.create(200L), multiply(pyint.create(12L), D)))).v()) {
      var epochs_for_validator_set_churn = divide(multiply(N, minus(multiply(t, plus(pyint.create(200L), multiply(pyint.create(12L), D))), multiply(T, plus(pyint.create(200L), multiply(pyint.create(3L), D))))), multiply(multiply(pyint.create(600L), delta), plus(multiply(pyint.create(2L), t), T)));
      var epochs_for_balance_top_ups = divide(multiply(N, plus(pyint.create(200L), multiply(pyint.create(3L), D))), multiply(pyint.create(600L), Delta));
      var ws_period_1 = plus(ws_period, max(epochs_for_validator_set_churn, epochs_for_balance_top_ups));
      ws_period_3 = ws_period_1;
    } else {
      var ws_period_2 = plus(ws_period, divide(multiply(multiply(multiply(pyint.create(3L), N), D), t), multiply(multiply(pyint.create(200L), Delta), minus(T, t))));
      ws_period_3 = ws_period_2;
    }
    return ws_period_3;
  }

  public static pybool is_within_weak_subjectivity_period(Store store, BeaconState ws_state, Checkpoint ws_checkpoint) {
    pyassert(eq(ws_state.getLatest_block_header().getState_root(), ws_checkpoint.getRoot()));
    pyassert(eq(compute_epoch_at_slot(ws_state.getSlot()), ws_checkpoint.getEpoch()));
    var ws_period = compute_weak_subjectivity_period(ws_state);
    var ws_state_epoch = compute_epoch_at_slot(ws_state.getSlot());
    var current_epoch = compute_epoch_at_slot(get_current_slot(store));
    return lessOrEqual(current_epoch, plus(ws_state_epoch, ws_period));
  }

  /*
      A stub function return mocking Eth1Data.
      */
  public static Eth1Data get_eth1_data(Eth1Block block_0) {
    return new Eth1Data(block_0.getDeposit_root(), block_0.getDeposit_count(), new Hash32(hash_tree_root(block_0)));
  }
}
