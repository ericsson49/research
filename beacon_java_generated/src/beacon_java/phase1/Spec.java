package beacon_java.phase1;

import static beacon_java.phase1.Constants.*;
import static beacon_java.deps.BLS.bls;
import static beacon_java.phase1.Utils.copy;
import static beacon_java.phase1.Utils.hash_tree_root;
import static beacon_java.pylib.Exports.*;

import beacon_java.phase1.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import beacon_java.ssz.uint8;


public class Spec {
  /*
      Return the largest integer ``x`` such that ``x**2 <= n``.
      */
  public uint64 integer_squareroot(uint64 n_0) {
    var x_0 = n_0;
    var y_0 = divide(plus(x_0, pyint.create(1L)), pyint.create(2L));
    var x_2 = x_0;
    var y_2 = y_0;
    while (less(x_2, y_2).v()) {
      var x_1 = y_2;
      var y_1 = divide(plus(x_1, divide(n_0, x_1)), pyint.create(2L));
      x_2 = x_1;
      y_2 = y_1;
    }
    return x_2;
  }

  /*
      Return the exclusive-or of two 32-byte strings.
      */
  public Bytes32 xor(Bytes32 bytes_1_0, Bytes32 bytes_2_0) {
    return new Bytes32(zip(bytes_1_0, bytes_2_0).map((tmp_0) -> { var a = tmp_0.first; var b = tmp_0.second; return bitXor(a, b); }));
  }

  /*
      Return the integer deserialization of ``data`` interpreted as ``ENDIANNESS``-endian.
      */
  public uint64 bytes_to_uint64(pybytes data_0) {
    return new uint64(pyint.from_bytes(data_0, ENDIANNESS));
  }

  /*
      Check if ``validator`` is active.
      */
  public pybool is_active_validator(Validator validator_0, Epoch epoch_0) {
    return and(lessOrEqual(epoch_0, validator_0.getActivation_epoch()), less(validator_0.getExit_epoch(), epoch_0));
  }

  /*
      Check if ``validator`` is eligible to be placed into the activation queue.
      */
  public pybool is_eligible_for_activation_queue(Validator validator_0) {
    return and(eq(validator_0.getActivation_eligibility_epoch(), FAR_FUTURE_EPOCH), eq(validator_0.getEffective_balance(), MAX_EFFECTIVE_BALANCE));
  }

  /*
      Check if ``validator`` is eligible for activation.
      */
  public pybool is_eligible_for_activation(BeaconState state_0, Validator validator_0) {
    return and(lessOrEqual(state_0.getFinalized_checkpoint().getEpoch(), validator_0.getActivation_eligibility_epoch()), eq(validator_0.getActivation_epoch(), FAR_FUTURE_EPOCH));
  }

  /*
      Check if ``validator`` is slashable.
      */
  public pybool is_slashable_validator(Validator validator_0, Epoch epoch_0) {
    return and(not(validator_0.getSlashed()), and(lessOrEqual(epoch_0, validator_0.getActivation_epoch()), less(validator_0.getWithdrawable_epoch(), epoch_0)));
  }

  /*
      Check if ``data_1`` and ``data_2`` are slashable according to Casper FFG rules.
      */
  public pybool is_slashable_attestation_data(AttestationData data_1_0, AttestationData data_2_0) {
    return or(and(not(eq(data_1_0, data_2_0)), eq(data_1_0.getTarget().getEpoch(), data_2_0.getTarget().getEpoch())), and(less(data_2_0.getSource().getEpoch(), data_1_0.getSource().getEpoch()), less(data_1_0.getTarget().getEpoch(), data_2_0.getTarget().getEpoch())));
  }

  /*
      Check if ``indexed_attestation`` is not empty, has sorted and unique indices and has a valid aggregate signature.
      */
  public pybool is_valid_indexed_attestation(BeaconState state_0, IndexedAttestation indexed_attestation_0) {
    var indices_0 = indexed_attestation_0.getAttesting_indices();
    if (or(eq(len(indices_0), pyint.create(0L)), not(eq(indices_0, sorted(set(indices_0))))).v()) {
      return pybool.create(false);
    }
    var pubkeys_0 = indices_0.map((i) -> state_0.getValidators().get(i).getPubkey());
    var domain_0 = get_domain(state_0, DOMAIN_BEACON_ATTESTER, indexed_attestation_0.getData().getTarget().getEpoch());
    var signing_root_0 = compute_signing_root(indexed_attestation_0.getData(), domain_0);
    return bls.FastAggregateVerify(pubkeys_0, signing_root_0, indexed_attestation_0.getSignature());
  }

  /*
      Check if ``leaf`` at ``index`` verifies against the Merkle ``root`` and ``branch``.
      */
  public pybool is_valid_merkle_branch(Bytes32 leaf_0, Sequence<? extends Bytes32> branch_0, uint64 depth_0, uint64 index_0, Root root_0) {
    var value_0 = leaf_0;
    var value_3 = value_0;
    for (var i_0: range(depth_0)) {
      if (pybool(modulo(divide(index_0, power(pyint.create(2L), i_0)), pyint.create(2L))).v()) {
        var value_1 = hash(plus(branch_0.get(i_0), value_3));
        value_3 = value_1;
      } else {
        var value_2 = hash(plus(value_3, branch_0.get(i_0)));
        value_3 = value_2;
      }
    }
    return eq(value_3, root_0);
  }

  /*
      Return the shuffled index corresponding to ``seed`` (and ``index_count``).
      */
  public uint64 compute_shuffled_index(uint64 index_0, uint64 index_count_0, Bytes32 seed_0) {
    pyassert(less(index_count_0, index_0));
    var index_2 = index_0;
    for (var current_round_0: range(SHUFFLE_ROUND_COUNT)) {
      var pivot_0 = modulo(bytes_to_uint64(hash(plus(seed_0, uint_to_bytes(new uint8(current_round_0)))).getSlice(pyint.create(0L), pyint.create(8L))), index_count_0);
      var flip_0 = modulo(minus(plus(pivot_0, index_count_0), index_2), index_count_0);
      var position_0 = max(index_2, flip_0);
      var source_0 = hash(plus(plus(seed_0, uint_to_bytes(new uint8(current_round_0))), uint_to_bytes(new uint32(divide(position_0, pyint.create(256L))))));
      var byte_0 = new uint8(source_0.get(divide(modulo(position_0, pyint.create(256L)), pyint.create(8L))));
      var bit_0 = modulo(rightShift(byte_0, modulo(position_0, pyint.create(8L))), pyint.create(2L));
      var index_1 = pybool(bit_0).v() ? flip_0 : index_2;
      index_2 = index_1;
    }
    return index_2;
  }

  /*
      Return from ``indices`` a random index sampled by effective balance.
      */
  public ValidatorIndex compute_proposer_index(BeaconState state_0, Sequence<ValidatorIndex> indices_0, Bytes32 seed_0) {
    pyassert(greater(pyint.create(0L), len(indices_0)));
    var MAX_RANDOM_BYTE_0 = minus(power(pyint.create(2L), pyint.create(8L)), pyint.create(1L));
    var i_0 = new uint64(pyint.create(0L));
    var total_0 = new uint64(len(indices_0));
    var i_2 = i_0;
    while (true) {
      var candidate_index_0 = indices_0.get(compute_shuffled_index(modulo(i_2, total_0), total_0, seed_0));
      var random_byte_0 = hash(plus(seed_0, uint_to_bytes(new uint64(divide(i_2, pyint.create(32L)))))).get(modulo(i_2, pyint.create(32L)));
      var effective_balance_0 = state_0.getValidators().get(candidate_index_0).getEffective_balance();
      if (greaterOrEqual(multiply(MAX_EFFECTIVE_BALANCE, random_byte_0), multiply(effective_balance_0, MAX_RANDOM_BYTE_0)).v()) {
        return candidate_index_0;
      }
      var i_1 = plus(i_2, pyint.create(1L));
      i_2 = i_1;
    }
  }

  /*
      Return the committee corresponding to ``indices``, ``seed``, ``index``, and committee ``count``.
      */
  public Sequence<ValidatorIndex> compute_committee(Sequence<ValidatorIndex> indices_0, Bytes32 seed_0, uint64 index_0, uint64 count_0) {
    var start_0 = divide(multiply(len(indices_0), index_0), count_0);
    var end_0 = divide(multiply(len(indices_0), new uint64(plus(index_0, pyint.create(1L)))), count_0);
    return range(start_0, end_0).map((i) -> indices_0.get(compute_shuffled_index(new uint64(i), new uint64(len(indices_0)), seed_0)));
  }

  /*
      Return the epoch number at ``slot``.
      */
  public Epoch compute_epoch_at_slot(Slot slot_0) {
    return new Epoch(divide(slot_0, SLOTS_PER_EPOCH));
  }

  /*
      Return the start slot of ``epoch``.
      */
  public Slot compute_start_slot_at_epoch(Epoch epoch_0) {
    return new Slot(multiply(epoch_0, SLOTS_PER_EPOCH));
  }

  /*
      Return the epoch during which validator activations and exits initiated in ``epoch`` take effect.
      */
  public Epoch compute_activation_exit_epoch(Epoch epoch_0) {
    return new Epoch(plus(plus(epoch_0, pyint.create(1L)), MAX_SEED_LOOKAHEAD));
  }

  /*
      Return the 32-byte fork data root for the ``current_version`` and ``genesis_validators_root``.
      This is used primarily in signature domains to avoid collisions across forks/chains.
      */
  public Root compute_fork_data_root(Version current_version_0, Root genesis_validators_root_0) {
    return hash_tree_root(new ForkData(current_version_0, genesis_validators_root_0));
  }

  /*
      Return the 4-byte fork digest for the ``current_version`` and ``genesis_validators_root``.
      This is a digest primarily used for domain separation on the p2p layer.
      4-bytes suffices for practical separation of forks/chains.
      */
  public ForkDigest compute_fork_digest(Version current_version_0, Root genesis_validators_root_0) {
    return new ForkDigest(compute_fork_data_root(current_version_0, genesis_validators_root_0).getSlice(pyint.create(0L), pyint.create(4L)));
  }

  /*
      Return the domain for the ``domain_type`` and ``fork_version``.
      */
  public Domain compute_domain(DomainType domain_type_0, Version fork_version_0, Root genesis_validators_root_0) {
    Version fork_version_2;
    if (fork_version_0 == null) {
      var fork_version_1 = GENESIS_FORK_VERSION;
      fork_version_2 = fork_version_1;
    } else {
      fork_version_2 = fork_version_0;
    }
    Root genesis_validators_root_2;
    if (genesis_validators_root_0 == null) {
      var genesis_validators_root_1 = new Root();
      genesis_validators_root_2 = genesis_validators_root_1;
    } else {
      genesis_validators_root_2 = genesis_validators_root_0;
    }
    var fork_data_root_0 = compute_fork_data_root(fork_version_2, genesis_validators_root_2);
    return new Domain(plus(domain_type_0, fork_data_root_0.getSlice(pyint.create(0L), pyint.create(28L))));
  }

  /*
      Return the signing root for the corresponding signing data.
      */
  public Root compute_signing_root(Object ssz_object_0, Domain domain_0) {
    return hash_tree_root(new SigningData(hash_tree_root(ssz_object_0), domain_0));
  }

  /*
      Return the current epoch.
      */
  public Epoch get_current_epoch(BeaconState state_0) {
    return compute_epoch_at_slot(state_0.getSlot());
  }

  /*`
      Return the previous epoch (unless the current epoch is ``GENESIS_EPOCH``).
      */
  public Epoch get_previous_epoch(BeaconState state_0) {
    var current_epoch_0 = get_current_epoch(state_0);
    return eq(current_epoch_0, GENESIS_EPOCH).v() ? GENESIS_EPOCH : new Epoch(minus(current_epoch_0, pyint.create(1L)));
  }

  /*
      Return the block root at the start of a recent ``epoch``.
      */
  public Root get_block_root(BeaconState state_0, Epoch epoch_0) {
    return get_block_root_at_slot(state_0, compute_start_slot_at_epoch(epoch_0));
  }

  /*
      Return the block root at a recent ``slot``.
      */
  public Root get_block_root_at_slot(BeaconState state_0, Slot slot_0) {
    pyassert(and(less(state_0.getSlot(), slot_0), lessOrEqual(plus(slot_0, SLOTS_PER_HISTORICAL_ROOT), state_0.getSlot())));
    return state_0.getBlock_roots().get(modulo(slot_0, SLOTS_PER_HISTORICAL_ROOT));
  }

  /*
      Return the randao mix at a recent ``epoch``.
      */
  public Bytes32 get_randao_mix(BeaconState state_0, Epoch epoch_0) {
    return state_0.getRandao_mixes().get(modulo(epoch_0, EPOCHS_PER_HISTORICAL_VECTOR));
  }

  /*
      Return the sequence of active validator indices at ``epoch``.
      */
  public Sequence<ValidatorIndex> get_active_validator_indices(BeaconState state_0, Epoch epoch_0) {
    return enumerate(state_0.getValidators()).filter((tmp_1) -> { var i = tmp_1.first; var v = tmp_1.second; return is_active_validator(v, epoch_0); }).map((tmp_2) -> { var i = tmp_2.first; var v = tmp_2.second; return new ValidatorIndex(i); });
  }

  /*
      Return the validator churn limit for the current epoch.
      */
  public uint64 get_validator_churn_limit(BeaconState state_0) {
    var active_validator_indices_0 = get_active_validator_indices(state_0, get_current_epoch(state_0));
    return max(MIN_PER_EPOCH_CHURN_LIMIT, divide(new uint64(len(active_validator_indices_0)), CHURN_LIMIT_QUOTIENT));
  }

  /*
      Return the seed at ``epoch``.
      */
  public Bytes32 get_seed(BeaconState state_0, Epoch epoch_0, DomainType domain_type_0) {
    var mix_0 = get_randao_mix(state_0, new Epoch(minus(minus(plus(epoch_0, EPOCHS_PER_HISTORICAL_VECTOR), MIN_SEED_LOOKAHEAD), pyint.create(1L))));
    return hash(plus(plus(domain_type_0, uint_to_bytes(epoch_0)), mix_0));
  }

  /*
      Return the number of committees in each slot for the given ``epoch``.
      */
  public uint64 get_committee_count_per_slot(BeaconState state_0, Epoch epoch_0) {
    return max(new uint64(pyint.create(1L)), min(get_active_shard_count(state_0), divide(divide(new uint64(len(get_active_validator_indices(state_0, epoch_0))), SLOTS_PER_EPOCH), TARGET_COMMITTEE_SIZE)));
  }

  /*
      Return the beacon committee at ``slot`` for ``index``.
      */
  public Sequence<ValidatorIndex> get_beacon_committee(BeaconState state_0, Slot slot_0, CommitteeIndex index_0) {
    var epoch_0 = compute_epoch_at_slot(slot_0);
    var committees_per_slot_0 = get_committee_count_per_slot(state_0, epoch_0);
    return compute_committee(get_active_validator_indices(state_0, epoch_0), get_seed(state_0, epoch_0, DOMAIN_BEACON_ATTESTER), plus(multiply(modulo(slot_0, SLOTS_PER_EPOCH), committees_per_slot_0), index_0), multiply(committees_per_slot_0, SLOTS_PER_EPOCH));
  }

  /*
      Return the beacon proposer index at the current slot.
      */
  public ValidatorIndex get_beacon_proposer_index(BeaconState state_0) {
    var epoch_0 = get_current_epoch(state_0);
    var seed_0 = hash(plus(get_seed(state_0, epoch_0, DOMAIN_BEACON_PROPOSER), uint_to_bytes(state_0.getSlot())));
    var indices_0 = get_active_validator_indices(state_0, epoch_0);
    return compute_proposer_index(state_0, indices_0, seed_0);
  }

  /*
      Return the combined effective balance of the ``indices``.
      ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
      Math safe up to ~10B ETH, afterwhich this overflows uint64.
      */
  public Gwei get_total_balance(BeaconState state_0, Set<ValidatorIndex> indices_0) {
    return new Gwei(max(EFFECTIVE_BALANCE_INCREMENT, sum(indices_0.map((index) -> state_0.getValidators().get(index).getEffective_balance()))));
  }

  /*
      Return the combined effective balance of the active validators.
      Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
      */
  public Gwei get_total_active_balance(BeaconState state_0) {
    return get_total_balance(state_0, set(get_active_validator_indices(state_0, get_current_epoch(state_0))));
  }

  /*
      Return the signature domain (fork version concatenated with domain type) of a message.
      */
  public Domain get_domain(BeaconState state_0, DomainType domain_type_0, Epoch epoch_0) {
    var epoch_1 = epoch_0 == null ? get_current_epoch(state_0) : epoch_0;
    var fork_version_0 = less(state_0.getFork().getEpoch(), epoch_1).v() ? state_0.getFork().getPrevious_version() : state_0.getFork().getCurrent_version();
    return compute_domain(domain_type_0, fork_version_0, state_0.getGenesis_validators_root());
  }

  /*
      Return the indexed attestation corresponding to ``attestation``.
      */
  public IndexedAttestation get_indexed_attestation(BeaconState state_0, Attestation attestation_0) {
    var attesting_indices_0 = get_attesting_indices(state_0, attestation_0.getData(), attestation_0.getAggregation_bits());
    return new IndexedAttestation(new SSZList<>(sorted(attesting_indices_0)), attestation_0.getData(), attestation_0.getSignature());
  }

  /*
      Return the set of attesting indices corresponding to ``data`` and ``bits``.
      */
  public Set<ValidatorIndex> get_attesting_indices(BeaconState state_0, AttestationData data_0, SSZBitlist bits_0) {
    var committee_0 = get_beacon_committee(state_0, data_0.getSlot(), data_0.getIndex());
    return set(enumerate(committee_0).filter((tmp_3) -> { var i = tmp_3.first; var index = tmp_3.second; return bits_0.get(i); }).map((tmp_4) -> { var i = tmp_4.first; var index = tmp_4.second; return index; }));
  }

  /*
      Increase the validator balance at index ``index`` by ``delta``.
      */
  public void increase_balance(BeaconState state_0, ValidatorIndex index_0, Gwei delta_0) {
    state_0.getBalances().set(index_0, plus(state_0.getBalances().get(index_0), delta_0));
  }

  /*
      Decrease the validator balance at index ``index`` by ``delta``, with underflow protection.
      */
  public void decrease_balance(BeaconState state_0, ValidatorIndex index_0, Gwei delta_0) {
    state_0.getBalances().set(index_0, new Gwei(greater(state_0.getBalances().get(index_0), delta_0).v() ? pyint.create(0L) : minus(state_0.getBalances().get(index_0), delta_0)));
  }

  /*
      Initiate the exit of the validator with index ``index``.
      */
  public void initiate_validator_exit(BeaconState state_0, ValidatorIndex index_0) {
    var validator_0 = state_0.getValidators().get(index_0);
    if (not(eq(validator_0.getExit_epoch(), FAR_FUTURE_EPOCH)).v()) {
      return;
    }
    var exit_epochs_0 = state_0.getValidators().filter((v) -> not(eq(v.getExit_epoch(), FAR_FUTURE_EPOCH))).map((v) -> v.getExit_epoch());
    var exit_queue_epoch_0 = max(plus(exit_epochs_0, PyList.of(compute_activation_exit_epoch(get_current_epoch(state_0)))));
    var exit_queue_churn_0 = len(state_0.getValidators().filter((v) -> eq(v.getExit_epoch(), exit_queue_epoch_0)).map((v) -> v));
    Epoch exit_queue_epoch_2;
    if (greaterOrEqual(get_validator_churn_limit(state_0), exit_queue_churn_0).v()) {
      var exit_queue_epoch_1 = plus(exit_queue_epoch_0, new Epoch(pyint.create(1L)));
      exit_queue_epoch_2 = exit_queue_epoch_1;
    } else {
      exit_queue_epoch_2 = exit_queue_epoch_0;
    }
    validator_0.setExit_epoch(exit_queue_epoch_2);
    validator_0.setWithdrawable_epoch(new Epoch(plus(validator_0.getExit_epoch(), MIN_VALIDATOR_WITHDRAWABILITY_DELAY)));
  }

  /*
      Slash the validator with index ``slashed_index``.
      */
  public void slash_validator(BeaconState state_0, ValidatorIndex slashed_index_0, ValidatorIndex whistleblower_index_0) {
    var epoch_0 = get_current_epoch(state_0);
    initiate_validator_exit(state_0, slashed_index_0);
    var validator_0 = state_0.getValidators().get(slashed_index_0);
    validator_0.setSlashed(new SSZBoolean(pybool.create(true)));
    validator_0.setWithdrawable_epoch(max(validator_0.getWithdrawable_epoch(), new Epoch(plus(epoch_0, EPOCHS_PER_SLASHINGS_VECTOR))));
    state_0.getSlashings().set(modulo(epoch_0, EPOCHS_PER_SLASHINGS_VECTOR), plus(state_0.getSlashings().get(modulo(epoch_0, EPOCHS_PER_SLASHINGS_VECTOR)), validator_0.getEffective_balance()));
    decrease_balance(state_0, slashed_index_0, divide(validator_0.getEffective_balance(), MIN_SLASHING_PENALTY_QUOTIENT));
    var proposer_index_0 = get_beacon_proposer_index(state_0);
    ValidatorIndex whistleblower_index_2;
    if (whistleblower_index_0 == null) {
      var whistleblower_index_1 = proposer_index_0;
      whistleblower_index_2 = whistleblower_index_1;
    } else {
      whistleblower_index_2 = whistleblower_index_0;
    }
    var whistleblower_reward_0 = new Gwei(divide(validator_0.getEffective_balance(), WHISTLEBLOWER_REWARD_QUOTIENT));
    var proposer_reward_0 = new Gwei(divide(whistleblower_reward_0, PROPOSER_REWARD_QUOTIENT));
    increase_balance(state_0, proposer_index_0, proposer_reward_0);
    increase_balance(state_0, whistleblower_index_2, new Gwei(minus(whistleblower_reward_0, proposer_reward_0)));
  }

  public BeaconState initialize_beacon_state_from_eth1(Bytes32 eth1_block_hash_0, uint64 eth1_timestamp_0, Sequence<Deposit> deposits_0) {
    var fork_0 = new Fork(GENESIS_FORK_VERSION, GENESIS_FORK_VERSION, GENESIS_EPOCH);
    var state_0 = new BeaconState(
        plus(eth1_timestamp_0, GENESIS_DELAY),
        BeaconState.genesis_validators_root_default,
        BeaconState.slot_default,
        fork_0,
        new BeaconBlockHeader(BeaconBlockHeader.slot_default, BeaconBlockHeader.proposer_index_default, BeaconBlockHeader.parent_root_default, BeaconBlockHeader.state_root_default, hash_tree_root(new BeaconBlockBody(BeaconBlockBody.randao_reveal_default, BeaconBlockBody.eth1_data_default, BeaconBlockBody.graffiti_default, BeaconBlockBody.proposer_slashings_default, BeaconBlockBody.attester_slashings_default, BeaconBlockBody.attestations_default, BeaconBlockBody.deposits_default, BeaconBlockBody.voluntary_exits_default, BeaconBlockBody.chunk_challenges_default, BeaconBlockBody.chunk_challenge_responses_default, BeaconBlockBody.custody_key_reveals_default, BeaconBlockBody.early_derived_secret_reveals_default, BeaconBlockBody.custody_slashings_default, BeaconBlockBody.shard_transitions_default, BeaconBlockBody.light_client_bits_default, BeaconBlockBody.light_client_signature_default))),
        BeaconState.block_roots_default,
        BeaconState.state_roots_default,
        BeaconState.historical_roots_default,
        new Eth1Data(Eth1Data.deposit_root_default, new uint64(len(deposits_0)), eth1_block_hash_0),
        BeaconState.eth1_data_votes_default,
        BeaconState.eth1_deposit_index_default,
        BeaconState.validators_default,
        BeaconState.balances_default,
        new SSZVector<Root>(multiply(PyList.of(new Root(eth1_block_hash_0)), EPOCHS_PER_HISTORICAL_VECTOR)),
        BeaconState.slashings_default,
        BeaconState.previous_epoch_attestations_default,
        BeaconState.current_epoch_attestations_default,
        BeaconState.justification_bits_default,
        BeaconState.previous_justified_checkpoint_default,
        BeaconState.current_justified_checkpoint_default,
        BeaconState.finalized_checkpoint_default,
        BeaconState.current_epoch_start_shard_default,
        BeaconState.shard_states_default,
        BeaconState.online_countdown_default,
        BeaconState.current_light_committee_default,
        BeaconState.next_light_committee_default,
        BeaconState.exposed_derived_secrets_default,
        BeaconState.custody_chunk_challenge_records_default,
        BeaconState.custody_chunk_challenge_index_default);
    var leaves_0 = list(map((deposit) -> deposit.getData(), deposits_0));
    for (var tmp_5: enumerate(deposits_0)) {
      var index_0 = tmp_5.first;
      var deposit_0 = tmp_5.second;
      var deposit_data_list_0 = new SSZList<DepositData>(leaves_0.getSlice(pyint.create(0L), plus(index_0, pyint.create(1L))));
      state_0.getEth1_data().setDeposit_root(hash_tree_root(deposit_data_list_0));
      process_deposit(state_0, deposit_0);
    }
    for (var tmp_6: enumerate(state_0.getValidators())) {
      var index_1 = tmp_6.first;
      var validator_0 = tmp_6.second;
      var balance_0 = state_0.getBalances().get(index_1);
      validator_0.setEffective_balance(min(minus(balance_0, modulo(balance_0, EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE));
      if (eq(validator_0.getEffective_balance(), MAX_EFFECTIVE_BALANCE).v()) {
        validator_0.setActivation_eligibility_epoch(GENESIS_EPOCH);
        validator_0.setActivation_epoch(GENESIS_EPOCH);
      }
    }
    state_0.setGenesis_validators_root(hash_tree_root(state_0.getValidators()));
    return state_0;
  }

  public pybool is_valid_genesis_state(BeaconState state_0) {
    if (less(MIN_GENESIS_TIME, state_0.getGenesis_time()).v()) {
      return pybool.create(false);
    }
    if (less(MIN_GENESIS_ACTIVE_VALIDATOR_COUNT, len(get_active_validator_indices(state_0, GENESIS_EPOCH))).v()) {
      return pybool.create(false);
    }
    return pybool.create(true);
  }

  public void state_transition(BeaconState state_0, SignedBeaconBlock signed_block_0, pybool validate_result_0) {
    var block_0 = signed_block_0.getMessage();
    process_slots(state_0, block_0.getSlot());
    if (validate_result_0.v()) {
      pyassert(verify_block_signature(state_0, signed_block_0));
    }
    process_block(state_0, block_0);
    if (validate_result_0.v()) {
      pyassert(eq(block_0.getState_root(), hash_tree_root(state_0)));
    }
  }

  public pybool verify_block_signature(BeaconState state_0, SignedBeaconBlock signed_block_0) {
    var proposer_0 = state_0.getValidators().get(signed_block_0.getMessage().getProposer_index());
    var signing_root_0 = compute_signing_root(signed_block_0.getMessage(), get_domain(state_0, DOMAIN_BEACON_PROPOSER, null));
    return bls.Verify(proposer_0.getPubkey(), signing_root_0, signed_block_0.getSignature());
  }

  public void process_slots(BeaconState state_0, Slot slot_0) {
    pyassert(less(slot_0, state_0.getSlot()));
    while (less(slot_0, state_0.getSlot()).v()) {
      process_slot(state_0);
      if (eq(modulo(plus(state_0.getSlot(), pyint.create(1L)), SLOTS_PER_EPOCH), pyint.create(0L)).v()) {
        process_epoch(state_0);
      }
      state_0.setSlot(new Slot(plus(state_0.getSlot(), pyint.create(1L))));
    }
  }

  public void process_slot(BeaconState state_0) {
    var previous_state_root_0 = hash_tree_root(state_0);
    state_0.getState_roots().set(modulo(state_0.getSlot(), SLOTS_PER_HISTORICAL_ROOT), previous_state_root_0);
    if (eq(state_0.getLatest_block_header().getState_root(), new Bytes32()).v()) {
      state_0.getLatest_block_header().setState_root(previous_state_root_0);
    }
    var previous_block_root_0 = hash_tree_root(state_0.getLatest_block_header());
    state_0.getBlock_roots().set(modulo(state_0.getSlot(), SLOTS_PER_HISTORICAL_ROOT), previous_block_root_0);
  }

  public void process_epoch(BeaconState state_0) {
    process_justification_and_finalization(state_0);
    process_rewards_and_penalties(state_0);
    process_registry_updates(state_0);
    process_reveal_deadlines(state_0);
    process_challenge_deadlines(state_0);
    process_slashings(state_0);
    process_eth1_data_reset(state_0);
    process_effective_balance_updates(state_0);
    process_slashings_reset(state_0);
    process_randao_mixes_reset(state_0);
    process_historical_roots_update(state_0);
    process_participation_record_updates(state_0);
    process_phase_1_final_updates(state_0);
  }

  public Sequence<PendingAttestation> get_matching_source_attestations(BeaconState state_0, Epoch epoch_0) {
    pyassert(contains(new Pair<>(get_previous_epoch(state_0), get_current_epoch(state_0)), epoch_0));
    return eq(epoch_0, get_current_epoch(state_0)).v() ? state_0.getCurrent_epoch_attestations() : state_0.getPrevious_epoch_attestations();
  }

  public Sequence<PendingAttestation> get_matching_target_attestations(BeaconState state_0, Epoch epoch_0) {
    return get_matching_source_attestations(state_0, epoch_0).filter((a) -> eq(a.getData().getTarget().getRoot(), get_block_root(state_0, epoch_0))).map((a) -> a);
  }

  public Sequence<PendingAttestation> get_matching_head_attestations(BeaconState state_0, Epoch epoch_0) {
    return get_matching_target_attestations(state_0, epoch_0).filter((a) -> eq(a.getData().getBeacon_block_root(), get_block_root_at_slot(state_0, a.getData().getSlot()))).map((a) -> a);
  }

  public Set<ValidatorIndex> get_unslashed_attesting_indices(BeaconState state_0, Sequence<PendingAttestation> attestations_0) {
    var output_0 = new Set<ValidatorIndex>();
    var output_2 = output_0;
    for (var a_0: attestations_0) {
      var output_1 = output_2.union(get_attesting_indices(state_0, a_0.getData(), a_0.getAggregation_bits()));
      output_2 = output_1;
    }
    return set(filter((index) -> not(state_0.getValidators().get(index).getSlashed()), output_2));
  }

  /*
      Return the combined effective balance of the set of unslashed validators participating in ``attestations``.
      Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
      */
  public Gwei get_attesting_balance(BeaconState state_0, Sequence<PendingAttestation> attestations_0) {
    return get_total_balance(state_0, get_unslashed_attesting_indices(state_0, attestations_0));
  }

  public void process_justification_and_finalization(BeaconState state_0) {
    if (lessOrEqual(plus(GENESIS_EPOCH, pyint.create(1L)), get_current_epoch(state_0)).v()) {
      return;
    }
    var previous_attestations_0 = get_matching_target_attestations(state_0, get_previous_epoch(state_0));
    var current_attestations_0 = get_matching_target_attestations(state_0, get_current_epoch(state_0));
    var total_active_balance_0 = get_total_active_balance(state_0);
    var previous_target_balance_0 = get_attesting_balance(state_0, previous_attestations_0);
    var current_target_balance_0 = get_attesting_balance(state_0, current_attestations_0);
    weigh_justification_and_finalization(state_0, total_active_balance_0, previous_target_balance_0, current_target_balance_0);
  }

  public void weigh_justification_and_finalization(BeaconState state_0, Gwei total_active_balance_0, Gwei previous_epoch_target_balance_0, Gwei current_epoch_target_balance_0) {
    var previous_epoch_0 = get_previous_epoch(state_0);
    var current_epoch_0 = get_current_epoch(state_0);
    var old_previous_justified_checkpoint_0 = state_0.getPrevious_justified_checkpoint();
    var old_current_justified_checkpoint_0 = state_0.getCurrent_justified_checkpoint();
    state_0.setPrevious_justified_checkpoint(state_0.getCurrent_justified_checkpoint());
    state_0.getJustification_bits().setSlice(pyint.create(1L), null, state_0.getJustification_bits().getSlice(pyint.create(0L), minus(JUSTIFICATION_BITS_LENGTH, pyint.create(1L))));
    state_0.getJustification_bits().set(pyint.create(0L), new SSZBoolean(pyint.create(0L)));
    if (greaterOrEqual(multiply(total_active_balance_0, pyint.create(2L)), multiply(previous_epoch_target_balance_0, pyint.create(3L))).v()) {
      state_0.setCurrent_justified_checkpoint(new Checkpoint(previous_epoch_0, get_block_root(state_0, previous_epoch_0)));
      state_0.getJustification_bits().set(pyint.create(1L), new SSZBoolean(pyint.create(1L)));
    }
    if (greaterOrEqual(multiply(total_active_balance_0, pyint.create(2L)), multiply(current_epoch_target_balance_0, pyint.create(3L))).v()) {
      state_0.setCurrent_justified_checkpoint(new Checkpoint(current_epoch_0, get_block_root(state_0, current_epoch_0)));
      state_0.getJustification_bits().set(pyint.create(0L), new SSZBoolean(pyint.create(1L)));
    }
    var bits_0 = state_0.getJustification_bits();
    if (and(all(bits_0.getSlice(pyint.create(1L), pyint.create(4L))), eq(plus(old_previous_justified_checkpoint_0.getEpoch(), pyint.create(3L)), current_epoch_0)).v()) {
      state_0.setFinalized_checkpoint(old_previous_justified_checkpoint_0);
    }
    if (and(all(bits_0.getSlice(pyint.create(1L), pyint.create(3L))), eq(plus(old_previous_justified_checkpoint_0.getEpoch(), pyint.create(2L)), current_epoch_0)).v()) {
      state_0.setFinalized_checkpoint(old_previous_justified_checkpoint_0);
    }
    if (and(all(bits_0.getSlice(pyint.create(0L), pyint.create(3L))), eq(plus(old_current_justified_checkpoint_0.getEpoch(), pyint.create(2L)), current_epoch_0)).v()) {
      state_0.setFinalized_checkpoint(old_current_justified_checkpoint_0);
    }
    if (and(all(bits_0.getSlice(pyint.create(0L), pyint.create(2L))), eq(plus(old_current_justified_checkpoint_0.getEpoch(), pyint.create(1L)), current_epoch_0)).v()) {
      state_0.setFinalized_checkpoint(old_current_justified_checkpoint_0);
    }
  }

  public Gwei get_base_reward(BeaconState state_0, ValidatorIndex index_0) {
    var total_balance_0 = get_total_active_balance(state_0);
    var effective_balance_0 = state_0.getValidators().get(index_0).getEffective_balance();
    return new Gwei(divide(divide(multiply(effective_balance_0, BASE_REWARD_FACTOR), integer_squareroot(total_balance_0)), BASE_REWARDS_PER_EPOCH));
  }

  public Gwei get_proposer_reward(BeaconState state_0, ValidatorIndex attesting_index_0) {
    return new Gwei(divide(get_base_reward(state_0, attesting_index_0), PROPOSER_REWARD_QUOTIENT));
  }

  public uint64 get_finality_delay(BeaconState state_0) {
    return minus(get_previous_epoch(state_0), state_0.getFinalized_checkpoint().getEpoch());
  }

  public pybool is_in_inactivity_leak(BeaconState state_0) {
    return greater(MIN_EPOCHS_TO_INACTIVITY_PENALTY, get_finality_delay(state_0));
  }

  public Sequence<ValidatorIndex> get_eligible_validator_indices(BeaconState state_0) {
    var previous_epoch_0 = get_previous_epoch(state_0);
    return enumerate(state_0.getValidators()).filter((tmp_7) -> { var index = tmp_7.first; var v = tmp_7.second; return or(is_active_validator(v, previous_epoch_0), and(v.getSlashed(), less(v.getWithdrawable_epoch(), plus(previous_epoch_0, pyint.create(1L))))); }).map((tmp_8) -> { var index = tmp_8.first; var v = tmp_8.second; return new ValidatorIndex(index); });
  }

  /*
      Helper with shared logic for use by get source, target, and head deltas functions
      */
  public Pair<Sequence<Gwei>,Sequence<Gwei>> get_attestation_component_deltas(BeaconState state_0, Sequence<PendingAttestation> attestations_0) {
    var rewards_0 = multiply(PyList.of(new Gwei(pyint.create(0L))), len(state_0.getValidators()));
    var penalties_0 = multiply(PyList.of(new Gwei(pyint.create(0L))), len(state_0.getValidators()));
    var total_balance_0 = get_total_active_balance(state_0);
    var unslashed_attesting_indices_0 = get_unslashed_attesting_indices(state_0, attestations_0);
    var attesting_balance_0 = get_total_balance(state_0, unslashed_attesting_indices_0);
    for (var index_0: get_eligible_validator_indices(state_0)) {
      if (contains(unslashed_attesting_indices_0, index_0).v()) {
        var increment_0 = EFFECTIVE_BALANCE_INCREMENT;
        if (is_in_inactivity_leak(state_0).v()) {
          rewards_0.set(index_0, plus(rewards_0.get(index_0), get_base_reward(state_0, index_0)));
        } else {
          var reward_numerator_0 = multiply(get_base_reward(state_0, index_0), divide(attesting_balance_0, increment_0));
          rewards_0.set(index_0, plus(rewards_0.get(index_0), divide(reward_numerator_0, divide(total_balance_0, increment_0))));
        }
      } else {
        penalties_0.set(index_0, plus(penalties_0.get(index_0), get_base_reward(state_0, index_0)));
      }
    }
    return new Pair<>(rewards_0, penalties_0);
  }

  /*
      Return attester micro-rewards/penalties for source-vote for each validator.
      */
  public Pair<Sequence<Gwei>,Sequence<Gwei>> get_source_deltas(BeaconState state_0) {
    var matching_source_attestations_0 = get_matching_source_attestations(state_0, get_previous_epoch(state_0));
    return get_attestation_component_deltas(state_0, matching_source_attestations_0);
  }

  /*
      Return attester micro-rewards/penalties for target-vote for each validator.
      */
  public Pair<Sequence<Gwei>,Sequence<Gwei>> get_target_deltas(BeaconState state_0) {
    var matching_target_attestations_0 = get_matching_target_attestations(state_0, get_previous_epoch(state_0));
    return get_attestation_component_deltas(state_0, matching_target_attestations_0);
  }

  /*
      Return attester micro-rewards/penalties for head-vote for each validator.
      */
  public Pair<Sequence<Gwei>,Sequence<Gwei>> get_head_deltas(BeaconState state_0) {
    var matching_head_attestations_0 = get_matching_head_attestations(state_0, get_previous_epoch(state_0));
    return get_attestation_component_deltas(state_0, matching_head_attestations_0);
  }

  /*
      Return proposer and inclusion delay micro-rewards/penalties for each validator.
      */
  public Pair<Sequence<Gwei>,Sequence<Gwei>> get_inclusion_delay_deltas(BeaconState state_0) {
    var rewards_0 = range(len(state_0.getValidators())).map((_0) -> new Gwei(pyint.create(0L)));
    var matching_source_attestations_0 = get_matching_source_attestations(state_0, get_previous_epoch(state_0));
    for (var index_0: get_unslashed_attesting_indices(state_0, matching_source_attestations_0)) {
      var attestation_0 = min(matching_source_attestations_0.filter((a) -> contains(get_attesting_indices(state_0, a.getData(), a.getAggregation_bits()), index_0)).map((a) -> a), (a) -> a.getInclusion_delay());
      rewards_0.set(attestation_0.getProposer_index(), plus(rewards_0.get(attestation_0.getProposer_index()), get_proposer_reward(state_0, index_0)));
      var max_attester_reward_0 = new Gwei(minus(get_base_reward(state_0, index_0), get_proposer_reward(state_0, index_0)));
      rewards_0.set(index_0, plus(rewards_0.get(index_0), new Gwei(divide(max_attester_reward_0, attestation_0.getInclusion_delay()))));
    }
    var penalties_0 = range(len(state_0.getValidators())).map((_0) -> new Gwei(pyint.create(0L)));
    return new Pair<>(rewards_0, penalties_0);
  }

  /*
      Return inactivity reward/penalty deltas for each validator.
      */
  public Pair<Sequence<Gwei>,Sequence<Gwei>> get_inactivity_penalty_deltas(BeaconState state_0) {
    var penalties_0 = range(len(state_0.getValidators())).map((_0) -> new Gwei(pyint.create(0L)));
    if (is_in_inactivity_leak(state_0).v()) {
      var matching_target_attestations_0 = get_matching_target_attestations(state_0, get_previous_epoch(state_0));
      var matching_target_attesting_indices_0 = get_unslashed_attesting_indices(state_0, matching_target_attestations_0);
      for (var index_0: get_eligible_validator_indices(state_0)) {
        var base_reward_0 = get_base_reward(state_0, index_0);
        penalties_0.set(index_0, plus(penalties_0.get(index_0), new Gwei(minus(multiply(BASE_REWARDS_PER_EPOCH, base_reward_0), get_proposer_reward(state_0, index_0)))));
        if (not(contains(matching_target_attesting_indices_0, index_0)).v()) {
          var effective_balance_0 = state_0.getValidators().get(index_0).getEffective_balance();
          penalties_0.set(index_0, plus(penalties_0.get(index_0), new Gwei(divide(multiply(effective_balance_0, get_finality_delay(state_0)), INACTIVITY_PENALTY_QUOTIENT))));
        }
      }
    }
    var rewards_0 = range(len(state_0.getValidators())).map((_0) -> new Gwei(pyint.create(0L)));
    return new Pair<>(rewards_0, penalties_0);
  }

  /*
      Return attestation reward/penalty deltas for each validator.
      */
  public Pair<Sequence<Gwei>,Sequence<Gwei>> get_attestation_deltas(BeaconState state_0) {
    var tmp_9 = get_source_deltas(state_0);
    var source_rewards_0 = tmp_9.first;
    var source_penalties_0 = tmp_9.second;
    var tmp_10 = get_target_deltas(state_0);
    var target_rewards_0 = tmp_10.first;
    var target_penalties_0 = tmp_10.second;
    var tmp_11 = get_head_deltas(state_0);
    var head_rewards_0 = tmp_11.first;
    var head_penalties_0 = tmp_11.second;
    var tmp_12 = get_inclusion_delay_deltas(state_0);
    var inclusion_delay_rewards_0 = tmp_12.first;
    var __0 = tmp_12.second;
    var tmp_13 = get_inactivity_penalty_deltas(state_0);
    var __1 = tmp_13.first;
    var inactivity_penalties_0 = tmp_13.second;
    var rewards_0 = range(len(state_0.getValidators())).map((i) -> plus(plus(plus(source_rewards_0.get(i), target_rewards_0.get(i)), head_rewards_0.get(i)), inclusion_delay_rewards_0.get(i)));
    var penalties_0 = range(len(state_0.getValidators())).map((i) -> plus(plus(plus(source_penalties_0.get(i), target_penalties_0.get(i)), head_penalties_0.get(i)), inactivity_penalties_0.get(i)));
    return new Pair<>(rewards_0, penalties_0);
  }

  public void process_rewards_and_penalties(BeaconState state_0) {
    if (eq(get_current_epoch(state_0), GENESIS_EPOCH).v()) {
      return;
    }
    var tmp_14 = get_attestation_deltas(state_0);
    var rewards_0 = tmp_14.first;
    var penalties_0 = tmp_14.second;
    for (var index_0: range(len(state_0.getValidators()))) {
      increase_balance(state_0, new ValidatorIndex(index_0), rewards_0.get(index_0));
      decrease_balance(state_0, new ValidatorIndex(index_0), penalties_0.get(index_0));
    }
  }

  public void process_registry_updates(BeaconState state_0) {
    for (var tmp_15: enumerate(state_0.getValidators())) {
      var index_0 = tmp_15.first;
      var validator_0 = tmp_15.second;
      if (is_eligible_for_activation_queue(validator_0).v()) {
        validator_0.setActivation_eligibility_epoch(plus(get_current_epoch(state_0), pyint.create(1L)));
      }
      if (and(is_active_validator(validator_0, get_current_epoch(state_0)), lessOrEqual(EJECTION_BALANCE, validator_0.getEffective_balance())).v()) {
        initiate_validator_exit(state_0, new ValidatorIndex(index_0));
      }
    }
    var activation_queue_0 = sorted(enumerate(state_0.getValidators()).filter((tmp_16) -> { var index = tmp_16.first; var validator = tmp_16.second; return is_eligible_for_activation(state_0, validator); }).map((tmp_17) -> { var index = tmp_17.first; var validator = tmp_17.second; return index; }), (index) -> new Pair<>(state_0.getValidators().get(index).getActivation_eligibility_epoch(), index));
    for (var index_1: activation_queue_0.getSlice(pyint.create(0L), get_validator_churn_limit(state_0))) {
      var validator_1 = state_0.getValidators().get(index_1);
      validator_1.setActivation_epoch(compute_activation_exit_epoch(get_current_epoch(state_0)));
    }
  }

  public void process_slashings(BeaconState state_0) {
    var epoch_0 = get_current_epoch(state_0);
    var total_balance_0 = get_total_active_balance(state_0);
    var adjusted_total_slashing_balance_0 = min(multiply(sum(state_0.getSlashings()), PROPORTIONAL_SLASHING_MULTIPLIER), total_balance_0);
    for (var tmp_18: enumerate(state_0.getValidators())) {
      var index_0 = tmp_18.first;
      var validator_0 = tmp_18.second;
      if (and(validator_0.getSlashed(), eq(plus(epoch_0, divide(EPOCHS_PER_SLASHINGS_VECTOR, pyint.create(2L))), validator_0.getWithdrawable_epoch())).v()) {
        var increment_0 = EFFECTIVE_BALANCE_INCREMENT;
        var penalty_numerator_0 = multiply(divide(validator_0.getEffective_balance(), increment_0), adjusted_total_slashing_balance_0);
        var penalty_0 = multiply(divide(penalty_numerator_0, total_balance_0), increment_0);
        decrease_balance(state_0, new ValidatorIndex(index_0), penalty_0);
      }
    }
  }

  public void process_eth1_data_reset(BeaconState state_0) {
    var next_epoch_0 = new Epoch(plus(get_current_epoch(state_0), pyint.create(1L)));
    if (eq(modulo(next_epoch_0, EPOCHS_PER_ETH1_VOTING_PERIOD), pyint.create(0L)).v()) {
      state_0.setEth1_data_votes(new SSZList<Eth1Data>());
    }
  }

  public void process_effective_balance_updates(BeaconState state_0) {
    for (var tmp_19: enumerate(state_0.getValidators())) {
      var index_0 = tmp_19.first;
      var validator_0 = tmp_19.second;
      var balance_0 = state_0.getBalances().get(index_0);
      var HYSTERESIS_INCREMENT_0 = new uint64(divide(EFFECTIVE_BALANCE_INCREMENT, HYSTERESIS_QUOTIENT));
      var DOWNWARD_THRESHOLD_0 = multiply(HYSTERESIS_INCREMENT_0, HYSTERESIS_DOWNWARD_MULTIPLIER);
      var UPWARD_THRESHOLD_0 = multiply(HYSTERESIS_INCREMENT_0, HYSTERESIS_UPWARD_MULTIPLIER);
      if (or(less(validator_0.getEffective_balance(), plus(balance_0, DOWNWARD_THRESHOLD_0)), less(balance_0, plus(validator_0.getEffective_balance(), UPWARD_THRESHOLD_0))).v()) {
        validator_0.setEffective_balance(min(minus(balance_0, modulo(balance_0, EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE));
      }
    }
  }

  public void process_slashings_reset(BeaconState state_0) {
    var next_epoch_0 = new Epoch(plus(get_current_epoch(state_0), pyint.create(1L)));
    state_0.getSlashings().set(modulo(next_epoch_0, EPOCHS_PER_SLASHINGS_VECTOR), new Gwei(pyint.create(0L)));
  }

  public void process_randao_mixes_reset(BeaconState state_0) {
    var current_epoch_0 = get_current_epoch(state_0);
    var next_epoch_0 = new Epoch(plus(current_epoch_0, pyint.create(1L)));
    state_0.getRandao_mixes().set(modulo(next_epoch_0, EPOCHS_PER_HISTORICAL_VECTOR), new Root(get_randao_mix(state_0, current_epoch_0)));
  }

  public void process_historical_roots_update(BeaconState state_0) {
    var next_epoch_0 = new Epoch(plus(get_current_epoch(state_0), pyint.create(1L)));
    if (eq(modulo(next_epoch_0, divide(SLOTS_PER_HISTORICAL_ROOT, SLOTS_PER_EPOCH)), pyint.create(0L)).v()) {
      var historical_batch_0 = new HistoricalBatch(state_0.getBlock_roots(), state_0.getState_roots());
      state_0.getHistorical_roots().append(hash_tree_root(historical_batch_0));
    }
  }

  public void process_participation_record_updates(BeaconState state_0) {
    state_0.setPrevious_epoch_attestations(state_0.getCurrent_epoch_attestations());
    state_0.setCurrent_epoch_attestations(new SSZList<PendingAttestation>());
  }

  public void process_block(BeaconState state_0, BeaconBlock block_0) {
    process_block_header(state_0, block_0);
    process_randao(state_0, block_0.getBody());
    process_eth1_data(state_0, block_0.getBody());
    process_light_client_aggregate(state_0, block_0.getBody());
    process_operations(state_0, block_0.getBody());
  }

  public void process_block_header(BeaconState state_0, BeaconBlock block_0) {
    pyassert(eq(block_0.getSlot(), state_0.getSlot()));
    pyassert(greater(state_0.getLatest_block_header().getSlot(), block_0.getSlot()));
    pyassert(eq(block_0.getProposer_index(), get_beacon_proposer_index(state_0)));
    pyassert(eq(block_0.getParent_root(), hash_tree_root(state_0.getLatest_block_header())));
    state_0.setLatest_block_header(
        new BeaconBlockHeader(block_0.getSlot(), block_0.getProposer_index(), block_0.getParent_root(),
            new Root(), hash_tree_root(block_0.getBody())));
    var proposer_0 = state_0.getValidators().get(block_0.getProposer_index());
    pyassert(not(proposer_0.getSlashed()));
  }

  public void process_randao(BeaconState state_0, BeaconBlockBody body_0) {
    var epoch_0 = get_current_epoch(state_0);
    var proposer_0 = state_0.getValidators().get(get_beacon_proposer_index(state_0));
    var signing_root_0 = compute_signing_root(epoch_0, get_domain(state_0, DOMAIN_RANDAO, null));
    pyassert(bls.Verify(proposer_0.getPubkey(), signing_root_0, body_0.getRandao_reveal()));
    var mix_0 = xor(get_randao_mix(state_0, epoch_0), hash(body_0.getRandao_reveal()));
    state_0.getRandao_mixes().set(modulo(epoch_0, EPOCHS_PER_HISTORICAL_VECTOR), new Root(mix_0));
  }

  public void process_eth1_data(BeaconState state_0, BeaconBlockBody body_0) {
    state_0.getEth1_data_votes().append(body_0.getEth1_data());
    if (greater(multiply(EPOCHS_PER_ETH1_VOTING_PERIOD, SLOTS_PER_EPOCH), multiply(state_0.getEth1_data_votes().count(body_0.getEth1_data()), pyint.create(2L))).v()) {
      state_0.setEth1_data(body_0.getEth1_data());
    }
  }

  public void process_operations(BeaconState state_0, BeaconBlockBody body_0) {
    pyassert(eq(len(body_0.getDeposits()), min(MAX_DEPOSITS, minus(state_0.getEth1_data().getDeposit_count(), state_0.getEth1_deposit_index()))));
    for (var operation_0: body_0.getProposer_slashings()) {
      process_proposer_slashing(state_0, operation_0);
    }
    for (var operation_1: body_0.getAttester_slashings()) {
      process_attester_slashing(state_0, operation_1);
    }
    for (var operation_2: body_0.getAttestations()) {
      process_attestation(state_0, operation_2);
    }
    for (var operation_3: body_0.getDeposits()) {
      process_deposit(state_0, operation_3);
    }
    for (var operation_4: body_0.getVoluntary_exits()) {
      process_voluntary_exit(state_0, operation_4);
    }
    process_custody_game_operations(state_0, body_0);
    process_shard_transitions(state_0, body_0.getShard_transitions(), body_0.getAttestations());
  }

  public void process_proposer_slashing(BeaconState state_0, ProposerSlashing proposer_slashing_0) {
    var header_1_0 = proposer_slashing_0.getSigned_header_1().getMessage();
    var header_2_0 = proposer_slashing_0.getSigned_header_2().getMessage();
    pyassert(eq(header_1_0.getSlot(), header_2_0.getSlot()));
    pyassert(eq(header_1_0.getProposer_index(), header_2_0.getProposer_index()));
    pyassert(not(eq(header_1_0, header_2_0)));
    var proposer_0 = state_0.getValidators().get(header_1_0.getProposer_index());
    pyassert(is_slashable_validator(proposer_0, get_current_epoch(state_0)));
    for (var signed_header_0: Pair.of(proposer_slashing_0.getSigned_header_1(), proposer_slashing_0.getSigned_header_2())) {
      var domain_0 = get_domain(state_0, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(signed_header_0.getMessage().getSlot()));
      var signing_root_0 = compute_signing_root(signed_header_0.getMessage(), domain_0);
      pyassert(bls.Verify(proposer_0.getPubkey(), signing_root_0, signed_header_0.getSignature()));
    }
    slash_validator(state_0, header_1_0.getProposer_index(), null);
  }

  public void process_attester_slashing(BeaconState state_0, AttesterSlashing attester_slashing_0) {
    var attestation_1_0 = attester_slashing_0.getAttestation_1();
    var attestation_2_0 = attester_slashing_0.getAttestation_2();
    pyassert(is_slashable_attestation_data(attestation_1_0.getData(), attestation_2_0.getData()));
    pyassert(is_valid_indexed_attestation(state_0, attestation_1_0));
    pyassert(is_valid_indexed_attestation(state_0, attestation_2_0));
    var slashed_any_0 = pybool.create(false);
    var indices_0 = set(attestation_1_0.getAttesting_indices()).intersection(attestation_2_0.getAttesting_indices());
    var slashed_any_2 = slashed_any_0;
    for (var index_0: sorted(indices_0)) {
      if (is_slashable_validator(state_0.getValidators().get(index_0), get_current_epoch(state_0)).v()) {
        slash_validator(state_0, index_0, null);
        var slashed_any_1 = pybool.create(true);
        slashed_any_2 = slashed_any_1;
      } else {
        slashed_any_2 = slashed_any_2;
      }
    }
    pyassert(slashed_any_2);
  }

  public void process_attestation(BeaconState state_0, Attestation attestation_0) {
    validate_attestation(state_0, attestation_0);
    var pending_attestation_0 = new PendingAttestation(
        attestation_0.getAggregation_bits(),
        attestation_0.getData(),
        minus(state_0.getSlot(), attestation_0.getData().getSlot()),
        get_beacon_proposer_index(state_0),
        new SSZBoolean(pybool.create(false)));
    if (eq(attestation_0.getData().getTarget().getEpoch(), get_current_epoch(state_0)).v()) {
      state_0.getCurrent_epoch_attestations().append(pending_attestation_0);
    } else {
      state_0.getPrevious_epoch_attestations().append(pending_attestation_0);
    }
  }

  public Validator get_validator_from_deposit(BeaconState state_0, Deposit deposit_0) {
    var amount_0 = deposit_0.getData().getAmount();
    var effective_balance_0 = min(minus(amount_0, modulo(amount_0, EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE);
    var next_custody_secret_to_reveal_0 = get_custody_period_for_validator(new ValidatorIndex(len(state_0.getValidators())), get_current_epoch(state_0));
    return new Validator(deposit_0.getData().getPubkey(), deposit_0.getData().getWithdrawal_credentials(), effective_balance_0, Validator.slashed_default, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH, next_custody_secret_to_reveal_0, FAR_FUTURE_EPOCH);
  }

  public void process_deposit(BeaconState state_0, Deposit deposit_0) {
    pyassert(is_valid_merkle_branch(hash_tree_root(deposit_0.getData()), deposit_0.getProof(), plus(DEPOSIT_CONTRACT_TREE_DEPTH, pyint.create(1L)), state_0.getEth1_deposit_index(), state_0.getEth1_data().getDeposit_root()));
    state_0.setEth1_deposit_index(plus(state_0.getEth1_deposit_index(), pyint.create(1L)));
    var pubkey_0 = deposit_0.getData().getPubkey();
    var amount_0 = deposit_0.getData().getAmount();
    var validator_pubkeys_0 = state_0.getValidators().map((v) -> v.getPubkey());
    if (not(contains(validator_pubkeys_0, pubkey_0)).v()) {
      var deposit_message_0 = new DepositMessage(deposit_0.getData().getPubkey(), deposit_0.getData().getWithdrawal_credentials(), deposit_0.getData().getAmount());
      var domain_0 = compute_domain(DOMAIN_DEPOSIT, null, null);
      var signing_root_0 = compute_signing_root(deposit_message_0, domain_0);
      if (not(bls.Verify(pubkey_0, signing_root_0, deposit_0.getData().getSignature())).v()) {
        return;
      }
      state_0.getValidators().append(get_validator_from_deposit(state_0, deposit_0));
      state_0.getBalances().append(amount_0);
    } else {
      var index_0 = new ValidatorIndex(validator_pubkeys_0.index(pubkey_0));
      increase_balance(state_0, index_0, amount_0);
    }
  }

  public void process_voluntary_exit(BeaconState state_0, SignedVoluntaryExit signed_voluntary_exit_0) {
    var voluntary_exit_0 = signed_voluntary_exit_0.getMessage();
    var validator_0 = state_0.getValidators().get(voluntary_exit_0.getValidator_index());
    pyassert(is_active_validator(validator_0, get_current_epoch(state_0)));
    pyassert(eq(validator_0.getExit_epoch(), FAR_FUTURE_EPOCH));
    pyassert(greaterOrEqual(voluntary_exit_0.getEpoch(), get_current_epoch(state_0)));
    pyassert(greaterOrEqual(plus(validator_0.getActivation_epoch(), SHARD_COMMITTEE_PERIOD), get_current_epoch(state_0)));
    var domain_0 = get_domain(state_0, DOMAIN_VOLUNTARY_EXIT, voluntary_exit_0.getEpoch());
    var signing_root_0 = compute_signing_root(voluntary_exit_0, domain_0);
    pyassert(bls.Verify(validator_0.getPubkey(), signing_root_0, signed_voluntary_exit_0.getSignature()));
    initiate_validator_exit(state_0, voluntary_exit_0.getValidator_index());
  }

  public Store get_forkchoice_store(BeaconState anchor_state_0, BeaconBlock anchor_block_0) {
    pyassert(eq(anchor_block_0.getState_root(), hash_tree_root(anchor_state_0)));
    var anchor_root_0 = hash_tree_root(anchor_block_0);
    var anchor_epoch_0 = get_current_epoch(anchor_state_0);
    var justified_checkpoint_0 = new Checkpoint(anchor_epoch_0, anchor_root_0);
    var finalized_checkpoint_0 = new Checkpoint(anchor_epoch_0, anchor_root_0);
    return new Store(
        plus(anchor_state_0.getGenesis_time(), multiply(SECONDS_PER_SLOT, anchor_state_0.getSlot())),
        anchor_state_0.getGenesis_time(),
        justified_checkpoint_0,
        finalized_checkpoint_0,
        justified_checkpoint_0,
        new PyDict<>(new Pair<>(anchor_root_0, copy(anchor_block_0))),
        new PyDict<>(new Pair<>(anchor_root_0, anchor_state_0.copy())),
        new PyDict<>(new Pair<>(justified_checkpoint_0, anchor_state_0.copy())),
        Store.latest_messages_default,
        new PyDict<>(range(get_active_shard_count(anchor_state_0))
            .map((shard) -> new Pair<>(new Shard(shard), get_forkchoice_shard_store(anchor_state_0, new Shard(shard))))));
  }

  public pyint get_slots_since_genesis(Store store_0) {
    return divide(minus(store_0.getTime(), store_0.getGenesis_time()), SECONDS_PER_SLOT);
  }

  public Slot get_current_slot(Store store_0) {
    return new Slot(plus(GENESIS_SLOT, get_slots_since_genesis(store_0)));
  }

  public pyint compute_slots_since_epoch_start(Slot slot_0) {
    return minus(slot_0, compute_start_slot_at_epoch(compute_epoch_at_slot(slot_0)));
  }

  public Root get_ancestor(Store store_0, Root root_0, Slot slot_0) {
    var block_0 = store_0.getBlocks().get(root_0);
    if (greater(slot_0, block_0.getSlot()).v()) {
      return get_ancestor(store_0, block_0.getParent_root(), slot_0);
    } else {
      if (eq(block_0.getSlot(), slot_0).v()) {
        return root_0;
      } else {
        return root_0;
      }
    }
  }

  public Gwei get_latest_attesting_balance(Store store_0, Root root_0) {
    var state_0 = store_0.getCheckpoint_states().get(store_0.getJustified_checkpoint());
    var active_indices_0 = get_active_validator_indices(state_0, get_current_epoch(state_0));
    return new Gwei(sum(active_indices_0.filter((i) -> and(contains(store_0.getLatest_messages(), i), eq(get_ancestor(store_0, store_0.getLatest_messages().get(i).getRoot(), store_0.getBlocks().get(root_0).getSlot()), root_0))).map((i) -> state_0.getValidators().get(i).getEffective_balance())));
  }

  public pybool filter_block_tree(Store store_0, Root block_root_0, PyDict<Root,BeaconBlock> blocks_0) {
    var block_0 = store_0.getBlocks().get(block_root_0);
    var children_0 = store_0.getBlocks().keys().filter((root) -> eq(store_0.getBlocks().get(root).getParent_root(), block_root_0)).map((root) -> root);
    if (any(children_0).v()) {
      var filter_block_tree_result_0 = children_0.map((child) -> filter_block_tree(store_0, child, blocks_0));
      if (any(filter_block_tree_result_0).v()) {
        blocks_0.set(block_root_0, block_0);
        return pybool.create(true);
      }
      return pybool.create(false);
    }
    var head_state_0 = store_0.getBlock_states().get(block_root_0);
    var correct_justified_0 = or(eq(store_0.getJustified_checkpoint().getEpoch(), GENESIS_EPOCH), eq(head_state_0.getCurrent_justified_checkpoint(), store_0.getJustified_checkpoint()));
    var correct_finalized_0 = or(eq(store_0.getFinalized_checkpoint().getEpoch(), GENESIS_EPOCH), eq(head_state_0.getFinalized_checkpoint(), store_0.getFinalized_checkpoint()));
    if (and(correct_justified_0, correct_finalized_0).v()) {
      blocks_0.set(block_root_0, block_0);
      return pybool.create(true);
    }
    return pybool.create(false);
  }

  /*
      Retrieve a filtered block tree from ``store``, only returning branches
      whose leaf state's justified/finalized info agrees with that in ``store``.
      */
  public PyDict<Root,BeaconBlock> get_filtered_block_tree(Store store_0) {
    var base_0 = store_0.getJustified_checkpoint().getRoot();
    PyDict<Root,BeaconBlock> blocks_0 = new PyDict<>();
    filter_block_tree(store_0, base_0, blocks_0);
    return blocks_0;
  }

  public Root get_head(Store store_0) {
    var blocks_0 = get_filtered_block_tree(store_0);
    var head_0 = store_0.getJustified_checkpoint().getRoot();
    var head_2 = head_0;
    while (true) {
      Root finalHead_ = head_2;
      var children_0 = blocks_0.keys().filter((root) -> eq(blocks_0.get(root).getParent_root(), finalHead_)).map((root) -> root);
      if (eq(len(children_0), pyint.create(0L)).v()) {
        return head_2;
      }
      var head_1 = max(children_0, (root) -> new Pair<>(get_latest_attesting_balance(store_0, root), root));
      head_2 = head_1;
    }
  }

  /*
      To address the bouncing attack, only update conflicting justified
      checkpoints in the fork choice if in the early slots of the epoch.
      Otherwise, delay incorporation of new justified checkpoint until next epoch boundary.

      See https://ethresear.ch/t/prevention-of-bouncing-attack-on-ffg/6114 for more detailed analysis and discussion.
      */
  public pybool should_update_justified_checkpoint(Store store_0, Checkpoint new_justified_checkpoint_0) {
    if (less(SAFE_SLOTS_TO_UPDATE_JUSTIFIED, compute_slots_since_epoch_start(get_current_slot(store_0))).v()) {
      return pybool.create(true);
    }
    var justified_slot_0 = compute_start_slot_at_epoch(store_0.getJustified_checkpoint().getEpoch());
    if (not(eq(get_ancestor(store_0, new_justified_checkpoint_0.getRoot(), justified_slot_0), store_0.getJustified_checkpoint().getRoot())).v()) {
      return pybool.create(false);
    }
    return pybool.create(true);
  }

  public void validate_on_attestation(Store store_0, Attestation attestation_0) {
    var target_0 = attestation_0.getData().getTarget();
    var current_epoch_0 = compute_epoch_at_slot(get_current_slot(store_0));
    var previous_epoch_0 = greater(GENESIS_EPOCH, current_epoch_0).v() ? minus(current_epoch_0, pyint.create(1L)) : GENESIS_EPOCH;
    pyassert(contains(PyList.of(current_epoch_0, previous_epoch_0), target_0.getEpoch()));
    pyassert(eq(target_0.getEpoch(), compute_epoch_at_slot(attestation_0.getData().getSlot())));
    pyassert(contains(store_0.getBlocks(), target_0.getRoot()));
    pyassert(contains(store_0.getBlocks(), attestation_0.getData().getBeacon_block_root()));
    pyassert(lessOrEqual(attestation_0.getData().getSlot(), store_0.getBlocks().get(attestation_0.getData().getBeacon_block_root()).getSlot()));
    var target_slot_0 = compute_start_slot_at_epoch(target_0.getEpoch());
    pyassert(eq(target_0.getRoot(), get_ancestor(store_0, attestation_0.getData().getBeacon_block_root(), target_slot_0)));
    pyassert(greaterOrEqual(plus(attestation_0.getData().getSlot(), pyint.create(1L)), get_current_slot(store_0)));
  }

  public void store_target_checkpoint_state(Store store_0, Checkpoint target_0) {
    if (not(contains(store_0.getCheckpoint_states(), target_0)).v()) {
      var base_state_0 = copy(store_0.getBlock_states().get(target_0.getRoot()));
      if (less(compute_start_slot_at_epoch(target_0.getEpoch()), base_state_0.getSlot()).v()) {
        process_slots(base_state_0, compute_start_slot_at_epoch(target_0.getEpoch()));
      }
      store_0.getCheckpoint_states().set(target_0, base_state_0);
    }
  }

  public void update_latest_messages(Store store_0, Sequence<ValidatorIndex> attesting_indices_0, Attestation attestation_0) {
    var target_0 = attestation_0.getData().getTarget();
    var beacon_block_root_0 = attestation_0.getData().getBeacon_block_root();
    var shard_0 = attestation_0.getData().getShard();
    for (var i_0: attesting_indices_0) {
      if (or(not(contains(store_0.getLatest_messages(), i_0)), greater(store_0.getLatest_messages().get(i_0).getEpoch(), target_0.getEpoch())).v()) {
        store_0.getLatest_messages().set(i_0, new LatestMessage(target_0.getEpoch(), beacon_block_root_0));
        var shard_latest_message_0 = new ShardLatestMessage(target_0.getEpoch(), attestation_0.getData().getShard_head_root());
        store_0.getShard_stores().get(shard_0).getLatest_messages().set(i_0, shard_latest_message_0);
      }
    }
  }

  public void on_tick(Store store_0, uint64 time_0) {
    var previous_slot_0 = get_current_slot(store_0);
    store_0.setTime(time_0);
    var current_slot_0 = get_current_slot(store_0);
    if (not(and(greater(previous_slot_0, current_slot_0), eq(compute_slots_since_epoch_start(current_slot_0), pyint.create(0L)))).v()) {
      return;
    }
    if (greater(store_0.getJustified_checkpoint().getEpoch(), store_0.getBest_justified_checkpoint().getEpoch()).v()) {
      store_0.setJustified_checkpoint(store_0.getBest_justified_checkpoint());
    }
  }

  public void on_block(Store store_0, SignedBeaconBlock signed_block_0) {
    var block_0 = signed_block_0.getMessage();
    pyassert(contains(store_0.getBlock_states(), block_0.getParent_root()));
    var pre_state_0 = copy(store_0.getBlock_states().get(block_0.getParent_root()));
    pyassert(greaterOrEqual(block_0.getSlot(), get_current_slot(store_0)));
    var finalized_slot_0 = compute_start_slot_at_epoch(store_0.getFinalized_checkpoint().getEpoch());
    pyassert(greater(finalized_slot_0, block_0.getSlot()));
    pyassert(eq(get_ancestor(store_0, block_0.getParent_root(), finalized_slot_0), store_0.getFinalized_checkpoint().getRoot()));
    var state_0 = pre_state_0.copy();
    state_transition(state_0, signed_block_0, pybool.create(true));
    store_0.getBlocks().set(hash_tree_root(block_0), block_0);
    store_0.getBlock_states().set(hash_tree_root(block_0), state_0);
    if (greater(store_0.getJustified_checkpoint().getEpoch(), state_0.getCurrent_justified_checkpoint().getEpoch()).v()) {
      if (greater(store_0.getBest_justified_checkpoint().getEpoch(), state_0.getCurrent_justified_checkpoint().getEpoch()).v()) {
        store_0.setBest_justified_checkpoint(state_0.getCurrent_justified_checkpoint());
      }
      if (should_update_justified_checkpoint(store_0, state_0.getCurrent_justified_checkpoint()).v()) {
        store_0.setJustified_checkpoint(state_0.getCurrent_justified_checkpoint());
      }
    }
    if (greater(store_0.getFinalized_checkpoint().getEpoch(), state_0.getFinalized_checkpoint().getEpoch()).v()) {
      store_0.setFinalized_checkpoint(state_0.getFinalized_checkpoint());
      if (not(eq(store_0.getJustified_checkpoint(), state_0.getCurrent_justified_checkpoint())).v()) {
        if (greater(store_0.getJustified_checkpoint().getEpoch(), state_0.getCurrent_justified_checkpoint().getEpoch()).v()) {
          store_0.setJustified_checkpoint(state_0.getCurrent_justified_checkpoint());
          return;
        }
        var finalized_slot_1 = compute_start_slot_at_epoch(store_0.getFinalized_checkpoint().getEpoch());
        var ancestor_at_finalized_slot_0 = get_ancestor(store_0, store_0.getJustified_checkpoint().getRoot(), finalized_slot_1);
        if (not(eq(ancestor_at_finalized_slot_0, store_0.getFinalized_checkpoint().getRoot())).v()) {
          store_0.setJustified_checkpoint(state_0.getCurrent_justified_checkpoint());
        }
      }
    }
  }

  /*
      Run ``on_attestation`` upon receiving a new ``attestation`` from either within a block or directly on the wire.

      An ``attestation`` that is asserted as invalid may be valid at a later time,
      consider scheduling it for later processing in such case.
      */
  public void on_attestation(Store store_0, Attestation attestation_0) {
    validate_on_attestation(store_0, attestation_0);
    store_target_checkpoint_state(store_0, attestation_0.getData().getTarget());
    var target_state_0 = store_0.getCheckpoint_states().get(attestation_0.getData().getTarget());
    var indexed_attestation_0 = get_indexed_attestation(target_state_0, attestation_0);
    pyassert(is_valid_indexed_attestation(target_state_0, indexed_attestation_0));
    update_latest_messages(store_0, indexed_attestation_0.getAttesting_indices(), attestation_0);
  }

  public pybool check_if_validator_active(BeaconState state_0, ValidatorIndex validator_index_0) {
    var validator_0 = state_0.getValidators().get(validator_index_0);
    return is_active_validator(validator_0, get_current_epoch(state_0));
  }

  /*
      Return the committee assignment in the ``epoch`` for ``validator_index``.
      ``assignment`` returned is a tuple of the following form:
          * ``assignmentlistOf(0)`` is the list of validators in the committee
          * ``assignmentlistOf(1)`` is the index to which the committee is assigned
          * ``assignmentlistOf(2)`` is the slot at which the committee is assigned
      Return None if no assignment.
      */
  public Triple<Sequence<ValidatorIndex>,CommitteeIndex,Slot> get_committee_assignment(BeaconState state_0, Epoch epoch_0, ValidatorIndex validator_index_0) {
    var next_epoch_0 = new Epoch(plus(get_current_epoch(state_0), pyint.create(1L)));
    pyassert(lessOrEqual(next_epoch_0, epoch_0));
    var start_slot_0 = compute_start_slot_at_epoch(epoch_0);
    var committee_count_per_slot_0 = get_committee_count_per_slot(state_0, epoch_0);
    for (var slot_0: range(start_slot_0, plus(start_slot_0, SLOTS_PER_EPOCH))) {
      for (var index_0: range(committee_count_per_slot_0)) {
        var committee_0 = get_beacon_committee(state_0, new Slot(slot_0), new CommitteeIndex(index_0));
        if (contains(committee_0, validator_index_0).v()) {
          return new Triple<>(committee_0, new CommitteeIndex(index_0), new Slot(slot_0));
        }
      }
    }
    return null;
  }

  public pybool is_proposer(BeaconState state_0, ValidatorIndex validator_index_0) {
    return eq(get_beacon_proposer_index(state_0), validator_index_0);
  }

  public BLSSignature get_epoch_signature(BeaconState state_0, BeaconBlock block_0, pyint privkey_0) {
    var domain_0 = get_domain(state_0, DOMAIN_RANDAO, compute_epoch_at_slot(block_0.getSlot()));
    var signing_root_0 = compute_signing_root(compute_epoch_at_slot(block_0.getSlot()), domain_0);
    return bls.Sign(privkey_0, signing_root_0);
  }

  public uint64 compute_time_at_slot(BeaconState state_0, Slot slot_0) {
    return new uint64(plus(state_0.getGenesis_time(), multiply(slot_0, SECONDS_PER_SLOT)));
  }

  public uint64 voting_period_start_time(BeaconState state_0) {
    var eth1_voting_period_start_slot_0 = new Slot(minus(state_0.getSlot(), modulo(state_0.getSlot(), multiply(EPOCHS_PER_ETH1_VOTING_PERIOD, SLOTS_PER_EPOCH))));
    return compute_time_at_slot(state_0, eth1_voting_period_start_slot_0);
  }

  public pybool is_candidate_block(Eth1Block block_0, uint64 period_start_0) {
    return and(lessOrEqual(period_start_0, plus(block_0.getTimestamp(), multiply(SECONDS_PER_ETH1_BLOCK, ETH1_FOLLOW_DISTANCE))), greaterOrEqual(period_start_0, plus(block_0.getTimestamp(), multiply(multiply(SECONDS_PER_ETH1_BLOCK, ETH1_FOLLOW_DISTANCE), pyint.create(2L)))));
  }

  public Eth1Data get_eth1_vote(BeaconState state_0, Sequence<Eth1Block> eth1_chain_0) {
    var period_start_0 = voting_period_start_time(state_0);
    var votes_to_consider_0 = eth1_chain_0.filter((block) -> and(is_candidate_block(block, period_start_0), greaterOrEqual(state_0.getEth1_data().getDeposit_count(), get_eth1_data(block).getDeposit_count()))).map((block) -> get_eth1_data(block));
    var valid_votes_0 = state_0.getEth1_data_votes().filter((vote) -> contains(votes_to_consider_0, vote)).map((vote) -> vote);
    Eth1Data state_eth1_data_0 = state_0.getEth1_data();
    var default_vote_0 = any(votes_to_consider_0).v() ? votes_to_consider_0.get(minus(len(votes_to_consider_0), pyint.create(1L))) : state_eth1_data_0;
    return max(valid_votes_0, (v) -> new Pair<>(valid_votes_0.count(v), uminus(valid_votes_0.index(v))), default_vote_0);
  }

  public Root compute_new_state_root(BeaconState state_0, BeaconBlock block_0) {
    BeaconState temp_state_0 = state_0.copy();
    var signed_block_0 = new SignedBeaconBlock(block_0, SignedBeaconBlock.signature_default);
    state_transition(temp_state_0, signed_block_0, pybool.create(false));
    return hash_tree_root(temp_state_0);
  }

  public BLSSignature get_block_signature(BeaconState state_0, BeaconBlock block_0, pyint privkey_0) {
    var domain_0 = get_domain(state_0, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(block_0.getSlot()));
    var signing_root_0 = compute_signing_root(block_0, domain_0);
    return bls.Sign(privkey_0, signing_root_0);
  }

  public BLSSignature get_attestation_signature(BeaconState state_0, AttestationData attestation_data_0, pyint privkey_0) {
    var domain_0 = get_domain(state_0, DOMAIN_BEACON_ATTESTER, attestation_data_0.getTarget().getEpoch());
    var signing_root_0 = compute_signing_root(attestation_data_0, domain_0);
    return bls.Sign(privkey_0, signing_root_0);
  }

  /*
      Compute the correct subnet for an attestation for Phase 0.
      Note, this mimics expected Phase 1 behavior where attestations will be mapped to their shard subnet.
      */
  public uint64 compute_subnet_for_attestation(uint64 committees_per_slot_0, Slot slot_0, CommitteeIndex committee_index_0) {
    var slots_since_epoch_start_0 = new uint64(modulo(slot_0, SLOTS_PER_EPOCH));
    var committees_since_epoch_start_0 = multiply(committees_per_slot_0, slots_since_epoch_start_0);
    return new uint64(modulo(plus(committees_since_epoch_start_0, committee_index_0), ATTESTATION_SUBNET_COUNT));
  }

  public BLSSignature get_slot_signature(BeaconState state_0, Slot slot_0, pyint privkey_0) {
    var domain_0 = get_domain(state_0, DOMAIN_SELECTION_PROOF, compute_epoch_at_slot(slot_0));
    var signing_root_0 = compute_signing_root(slot_0, domain_0);
    return bls.Sign(privkey_0, signing_root_0);
  }

  public pybool is_aggregator(BeaconState state_0, Slot slot_0, CommitteeIndex index_0, BLSSignature slot_signature_0) {
    var committee_0 = get_beacon_committee(state_0, slot_0, index_0);
    var modulo_0 = max(pyint.create(1L), divide(len(committee_0), TARGET_AGGREGATORS_PER_COMMITTEE));
    return eq(modulo(bytes_to_uint64(hash(slot_signature_0).getSlice(pyint.create(0L), pyint.create(8L))), modulo_0), pyint.create(0L));
  }

  public BLSSignature get_aggregate_signature(Sequence<Attestation> attestations_0) {
    var signatures_0 = attestations_0.map((attestation) -> attestation.getSignature());
    return bls.Aggregate(signatures_0);
  }

  public AggregateAndProof get_aggregate_and_proof(BeaconState state_0, ValidatorIndex aggregator_index_0, Attestation aggregate_0, pyint privkey_0) {
    return new AggregateAndProof(aggregator_index_0, aggregate_0, get_slot_signature(state_0, aggregate_0.getData().getSlot(), privkey_0));
  }

  public BLSSignature get_aggregate_and_proof_signature(BeaconState state_0, AggregateAndProof aggregate_and_proof_0, pyint privkey_0) {
    var aggregate_0 = aggregate_and_proof_0.getAggregate();
    var domain_0 = get_domain(state_0, DOMAIN_AGGREGATE_AND_PROOF, compute_epoch_at_slot(aggregate_0.getData().getSlot()));
    var signing_root_0 = compute_signing_root(aggregate_and_proof_0, domain_0);
    return bls.Sign(privkey_0, signing_root_0);
  }

  /*
      Returns the weak subjectivity period for the current ``state``.
      This computation takes into account the effect of:
          - validator set churn (bounded by ``get_validator_churn_limit()`` per epoch), and
          - validator balance top-ups (bounded by ``MAX_DEPOSITS * SLOTS_PER_EPOCH`` per epoch).
      A detailed calculation can be found at:
      https://github.com/runtimeverification/beacon-chain-verification/blob/master/weak-subjectivity/weak-subjectivity-analysis.pdf
      */
  public uint64 compute_weak_subjectivity_period(BeaconState state_0) {
    var ws_period_0 = MIN_VALIDATOR_WITHDRAWABILITY_DELAY;
    var N_0 = len(get_active_validator_indices(state_0, get_current_epoch(state_0)));
    var t_0 = divide(divide(get_total_active_balance(state_0), N_0), ETH_TO_GWEI);
    var T_0 = divide(MAX_EFFECTIVE_BALANCE, ETH_TO_GWEI);
    var delta_0 = get_validator_churn_limit(state_0);
    var Delta_0 = multiply(MAX_DEPOSITS, SLOTS_PER_EPOCH);
    var D_0 = SAFETY_DECAY;
    uint64 ws_period_3;
    if (less(multiply(t_0, plus(pyint.create(200L), multiply(pyint.create(12L), D_0))), multiply(T_0, plus(pyint.create(200L), multiply(pyint.create(3L), D_0)))).v()) {
      var epochs_for_validator_set_churn_0 = divide(multiply(N_0, minus(multiply(t_0, plus(pyint.create(200L), multiply(pyint.create(12L), D_0))), multiply(T_0, plus(pyint.create(200L), multiply(pyint.create(3L), D_0))))), multiply(multiply(pyint.create(600L), delta_0), plus(multiply(pyint.create(2L), t_0), T_0)));
      var epochs_for_balance_top_ups_0 = divide(multiply(N_0, plus(pyint.create(200L), multiply(pyint.create(3L), D_0))), multiply(pyint.create(600L), Delta_0));
      var ws_period_1 = plus(ws_period_0, max(epochs_for_validator_set_churn_0, epochs_for_balance_top_ups_0));
      ws_period_3 = ws_period_1;
    } else {
      var ws_period_2 = plus(ws_period_0, divide(multiply(multiply(multiply(pyint.create(3L), N_0), D_0), t_0), multiply(multiply(pyint.create(200L), Delta_0), minus(T_0, t_0))));
      ws_period_3 = ws_period_2;
    }
    return ws_period_3;
  }

  public pybool is_within_weak_subjectivity_period(Store store_0, BeaconState ws_state_0, Checkpoint ws_checkpoint_0) {
    pyassert(eq(ws_state_0.getLatest_block_header().getState_root(), ws_checkpoint_0.getRoot()));
    pyassert(eq(compute_epoch_at_slot(ws_state_0.getSlot()), ws_checkpoint_0.getEpoch()));
    var ws_period_0 = compute_weak_subjectivity_period(ws_state_0);
    var ws_state_epoch_0 = compute_epoch_at_slot(ws_state_0.getSlot());
    var current_epoch_0 = compute_epoch_at_slot(get_current_slot(store_0));
    return lessOrEqual(plus(ws_state_epoch_0, ws_period_0), current_epoch_0);
  }

  public pyint replace_empty_or_append(SSZList<CustodyChunkChallengeRecord> l_0, CustodyChunkChallengeRecord new_element_0) {
    for (var i_0: range(len(l_0))) {
      if (eq(l_0.get(i_0), new CustodyChunkChallengeRecord(CustodyChunkChallengeRecord.challenge_index_default, CustodyChunkChallengeRecord.challenger_index_default, CustodyChunkChallengeRecord.responder_index_default, CustodyChunkChallengeRecord.inclusion_epoch_default, CustodyChunkChallengeRecord.data_root_default, CustodyChunkChallengeRecord.chunk_index_default)).v()) {
        l_0.set(i_0, new_element_0);
        return i_0;
      }
    }
    l_0.append(new_element_0);
    return minus(len(l_0), pyint.create(1L));
  }

  public pyint legendre_bit(pyint a_0, pyint q_0) {
    if (greaterOrEqual(q_0, a_0).v()) {
      return legendre_bit(modulo(a_0, q_0), q_0);
    }
    if (eq(a_0, pyint.create(0L)).v()) {
      return pyint.create(0L);
    }
    pyassert(and(and(greater(a_0, q_0), greater(pyint.create(0L), a_0)), eq(modulo(q_0, pyint.create(2L)), pyint.create(1L))));
    var t_0 = pyint.create(1L);
    var n_0 = q_0;
    var a_4 = a_0;
    var t_4 = t_0;
    var n_2 = n_0;
    while (not(eq(a_4, pyint.create(0L))).v()) {
      var a_5 = a_4;
      var t_5 = t_4;
      while (eq(modulo(a_5, pyint.create(2L)), pyint.create(0L)).v()) {
        var a_1 = divide(a_5, pyint.create(2L));
        var r_0 = modulo(n_2, pyint.create(8L));
        if (or(eq(r_0, pyint.create(3L)), eq(r_0, pyint.create(5L))).v()) {
          var t_1 = uminus(t_5);
          a_5 = a_1;
          t_5 = t_1;
        } else {
          a_5 = a_1;
          t_5 = t_5;
        }
      }
      var tmp_20 = new Pair<>(n_2, a_5);
      var a_2 = tmp_20.first;
      var n_1 = tmp_20.second;
      pyint t_3;
      if (and(eq(modulo(a_2, pyint.create(4L)), modulo(n_1, pyint.create(4L))), eq(modulo(n_1, pyint.create(4L)), pyint.create(3L))).v()) {
        var t_2 = uminus(t_5);
        t_3 = t_2;
      } else {
        t_3 = t_5;
      }
      var a_3 = modulo(a_2, n_1);
      a_4 = a_3;
      t_4 = t_3;
      n_2 = n_1;
    }
    if (eq(n_2, pyint.create(1L)).v()) {
      return divide(plus(t_4, pyint.create(1L)), pyint.create(2L));
    } else {
      return pyint.create(0L);
    }
  }

  public Sequence<pybytes> get_custody_atoms(pybytes bytez_0) {
    var length_remainder_0 = modulo(len(bytez_0), BYTES_PER_CUSTODY_ATOM);
    var bytez_1 = plus(bytez_0, multiply(pybytes.create("\\x00"), modulo(minus(BYTES_PER_CUSTODY_ATOM, length_remainder_0), BYTES_PER_CUSTODY_ATOM)));
    return range(pyint.create(0L), len(bytez_1), BYTES_PER_CUSTODY_ATOM).map((i) -> bytez_1.getSlice(i, plus(i, BYTES_PER_CUSTODY_ATOM)));
  }

  public Sequence<pyint> get_custody_secrets(BLSSignature key_0) {
    var full_G2_element_0 = bls.signature_to_G2(key_0);
    var signature_0 = full_G2_element_0.first.getCoeffs();
    var signature_bytes_0 = pybytes.create("").join(signature_0.map((x) -> x.to_bytes(pyint.create(48L), "little")));
    var secrets_0 = range(pyint.create(0L), len(signature_bytes_0), pyint.create(32L)).map((i) -> pyint.from_bytes(signature_bytes_0.getSlice(i, plus(i, BYTES_PER_CUSTODY_ATOM)), "little"));
    return secrets_0;
  }

  public pyint universal_hash_function(Sequence<pybytes> data_chunks_0, Sequence<pyint> secrets_0) {
    var n_0 = len(data_chunks_0);
    return modulo(plus(sum(enumerate(data_chunks_0)
        .map((tmp_21) -> {
          var i = tmp_21.first; var atom = tmp_21.second;
          return modulo(multiply(power(secrets_0.get(modulo(i, CUSTODY_SECRETS)), i), pyint.from_bytes(atom, "little")), CUSTODY_PRIME); })), power(secrets_0.get(modulo(n_0, CUSTODY_SECRETS)), n_0)), CUSTODY_PRIME);
  }

  public SSZBit compute_custody_bit(BLSSignature key_0, SSZByteList data_0) {
    var custody_atoms_0 = get_custody_atoms(data_0);
    var secrets_0 = get_custody_secrets(key_0);
    var uhf_0 = universal_hash_function(custody_atoms_0, secrets_0);
    var legendre_bits_0 = range(CUSTODY_PROBABILITY_EXPONENT).map((i) -> legendre_bit(plus(plus(uhf_0, secrets_0.get(pyint.create(0L))), i), CUSTODY_PRIME));
    return new SSZBit(all(legendre_bits_0));
  }

  public Epoch get_randao_epoch_for_custody_period(uint64 period_0, ValidatorIndex validator_index_0) {
    var next_period_start_0 = minus(multiply(plus(period_0, pyint.create(1L)), EPOCHS_PER_CUSTODY_PERIOD), modulo(validator_index_0, EPOCHS_PER_CUSTODY_PERIOD));
    return new Epoch(plus(next_period_start_0, CUSTODY_PERIOD_TO_RANDAO_PADDING));
  }

  /*
      Return the reveal period for a given validator.
      */
  public uint64 get_custody_period_for_validator(ValidatorIndex validator_index_0, Epoch epoch_0) {
    return divide(plus(epoch_0, modulo(validator_index_0, EPOCHS_PER_CUSTODY_PERIOD)), EPOCHS_PER_CUSTODY_PERIOD);
  }

  public void process_custody_game_operations(BeaconState state_0, BeaconBlockBody body_0) {
    for (var operation_0: body_0.getChunk_challenges()) {
      process_chunk_challenge(state_0, operation_0);
    }
    for (var operation_1: body_0.getChunk_challenge_responses()) {
      process_chunk_challenge_response(state_0, operation_1);
    }
    for (var operation_2: body_0.getCustody_key_reveals()) {
      process_custody_key_reveal(state_0, operation_2);
    }
    for (var operation_3: body_0.getEarly_derived_secret_reveals()) {
      process_early_derived_secret_reveal(state_0, operation_3);
    }
    for (var operation_4: body_0.getCustody_slashings()) {
      process_custody_slashing(state_0, operation_4);
    }
  }

  public void process_chunk_challenge(BeaconState state_0, CustodyChunkChallenge challenge_0) {
    pyassert(is_valid_indexed_attestation(state_0, get_indexed_attestation(state_0, challenge_0.getAttestation())));
    var max_attestation_challenge_epoch_0 = new Epoch(plus(challenge_0.getAttestation().getData().getTarget().getEpoch(), MAX_CHUNK_CHALLENGE_DELAY));
    pyassert(lessOrEqual(max_attestation_challenge_epoch_0, get_current_epoch(state_0)));
    var responder_0 = state_0.getValidators().get(challenge_0.getResponder_index());
    if (less(FAR_FUTURE_EPOCH, responder_0.getExit_epoch()).v()) {
      pyassert(lessOrEqual(plus(responder_0.getExit_epoch(), MAX_CHUNK_CHALLENGE_DELAY), get_current_epoch(state_0)));
    }
    pyassert(is_slashable_validator(responder_0, get_current_epoch(state_0)));
    var attesters_0 = get_attesting_indices(state_0, challenge_0.getAttestation().getData(), challenge_0.getAttestation().getAggregation_bits());
    pyassert(contains(attesters_0, challenge_0.getResponder_index()));
    pyassert(eq(hash_tree_root(challenge_0.getShard_transition()), challenge_0.getAttestation().getData().getShard_transition_root()));
    var data_root_0 = challenge_0.getShard_transition().getShard_data_roots().get(challenge_0.getData_index());
    for (var record_0: state_0.getCustody_chunk_challenge_records()) {
      pyassert(or(not(eq(record_0.getData_root(), data_root_0)), not(eq(record_0.getChunk_index(), challenge_0.getChunk_index()))));
    }
    var shard_block_length_0 = challenge_0.getShard_transition().getShard_block_lengths().get(challenge_0.getData_index());
    var transition_chunks_0 = divide(minus(plus(shard_block_length_0, BYTES_PER_CUSTODY_CHUNK), pyint.create(1L)), BYTES_PER_CUSTODY_CHUNK);
    pyassert(less(transition_chunks_0, challenge_0.getChunk_index()));
    var new_record_0 = new CustodyChunkChallengeRecord(
        state_0.getCustody_chunk_challenge_index(),
        get_beacon_proposer_index(state_0), challenge_0.getResponder_index(), get_current_epoch(state_0),
        challenge_0.getShard_transition().getShard_data_roots().get(challenge_0.getData_index()), challenge_0.getChunk_index());
    replace_empty_or_append(state_0.getCustody_chunk_challenge_records(), new_record_0);
    state_0.setCustody_chunk_challenge_index(plus(state_0.getCustody_chunk_challenge_index(), pyint.create(1L)));
    responder_0.setWithdrawable_epoch(FAR_FUTURE_EPOCH);
  }

  public void process_chunk_challenge_response(BeaconState state_0, CustodyChunkResponse response_0) {
    var matching_challenges_0 = state_0.getCustody_chunk_challenge_records().filter((record) -> eq(record.getChallenge_index(), response_0.getChallenge_index())).map((record) -> record);
    pyassert(eq(len(matching_challenges_0), pyint.create(1L)));
    var challenge_0 = matching_challenges_0.get(pyint.create(0L));
    pyassert(eq(response_0.getChunk_index(), challenge_0.getChunk_index()));
    pyassert(is_valid_merkle_branch(hash_tree_root(response_0.getChunk()), response_0.getBranch(), plus(CUSTODY_RESPONSE_DEPTH, pyint.create(1L)), response_0.getChunk_index(), challenge_0.getData_root()));
    var index_in_records_0 = state_0.getCustody_chunk_challenge_records().index(challenge_0);
    state_0.getCustody_chunk_challenge_records().set(index_in_records_0, new CustodyChunkChallengeRecord(CustodyChunkChallengeRecord.challenge_index_default, CustodyChunkChallengeRecord.challenger_index_default, CustodyChunkChallengeRecord.responder_index_default, CustodyChunkChallengeRecord.inclusion_epoch_default, CustodyChunkChallengeRecord.data_root_default, CustodyChunkChallengeRecord.chunk_index_default));
    var proposer_index_0 = get_beacon_proposer_index(state_0);
    increase_balance(state_0, proposer_index_0, new Gwei(divide(get_base_reward(state_0, proposer_index_0), MINOR_REWARD_QUOTIENT)));
  }

  /*
      Process ``CustodyKeyReveal`` operation.
      Note that this function mutates ``state``.
      */
  public void process_custody_key_reveal(BeaconState state_0, CustodyKeyReveal reveal_0) {
    var revealer_0 = state_0.getValidators().get(reveal_0.getRevealer_index());
    var epoch_to_sign_0 = get_randao_epoch_for_custody_period(revealer_0.getNext_custody_secret_to_reveal(), reveal_0.getRevealer_index());
    var custody_reveal_period_0 = get_custody_period_for_validator(reveal_0.getRevealer_index(), get_current_epoch(state_0));
    var is_past_reveal_0 = less(custody_reveal_period_0, revealer_0.getNext_custody_secret_to_reveal());
    var is_exited_0 = lessOrEqual(get_current_epoch(state_0), revealer_0.getExit_epoch());
    var is_exit_period_reveal_0 = eq(revealer_0.getNext_custody_secret_to_reveal(), get_custody_period_for_validator(reveal_0.getRevealer_index(), minus(revealer_0.getExit_epoch(), pyint.create(1L))));
    pyassert(or(is_past_reveal_0, and(is_exited_0, is_exit_period_reveal_0)));
    pyassert(is_slashable_validator(revealer_0, get_current_epoch(state_0)));
    var domain_0 = get_domain(state_0, DOMAIN_RANDAO, epoch_to_sign_0);
    var signing_root_0 = compute_signing_root(epoch_to_sign_0, domain_0);
    pyassert(bls.Verify(revealer_0.getPubkey(), signing_root_0, reveal_0.getReveal()));
    if (and(is_exited_0, is_exit_period_reveal_0).v()) {
      revealer_0.setAll_custody_secrets_revealed_epoch(get_current_epoch(state_0));
    }
    revealer_0.setNext_custody_secret_to_reveal(plus(revealer_0.getNext_custody_secret_to_reveal(), pyint.create(1L)));
    var proposer_index_0 = get_beacon_proposer_index(state_0);
    increase_balance(state_0, proposer_index_0, new Gwei(divide(get_base_reward(state_0, reveal_0.getRevealer_index()), MINOR_REWARD_QUOTIENT)));
  }

  /*
      Process ``EarlyDerivedSecretReveal`` operation.
      Note that this function mutates ``state``.
      */
  public void process_early_derived_secret_reveal(BeaconState state_0, EarlyDerivedSecretReveal reveal_0) {
    var revealed_validator_0 = state_0.getValidators().get(reveal_0.getRevealed_index());
    var derived_secret_location_0 = new uint64(modulo(reveal_0.getEpoch(), EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS));
    pyassert(greaterOrEqual(plus(get_current_epoch(state_0), RANDAO_PENALTY_EPOCHS), reveal_0.getEpoch()));
    pyassert(less(plus(get_current_epoch(state_0), EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS), reveal_0.getEpoch()));
    pyassert(not(revealed_validator_0.getSlashed()));
    pyassert(not(contains(state_0.getExposed_derived_secrets().get(derived_secret_location_0), reveal_0.getRevealed_index())));
    var masker_0 = state_0.getValidators().get(reveal_0.getMasker_index());
    var pubkeys_0 = PyList.of(revealed_validator_0.getPubkey(), masker_0.getPubkey());
    var domain_0 = get_domain(state_0, DOMAIN_RANDAO, reveal_0.getEpoch());
    var signing_roots_0 = PyList.of(hash_tree_root(reveal_0.getEpoch()), reveal_0.getMask()).map((root) -> compute_signing_root(root, domain_0));
    pyassert(bls.AggregateVerify(pubkeys_0, signing_roots_0, reveal_0.getReveal()));
    if (greaterOrEqual(plus(get_current_epoch(state_0), CUSTODY_PERIOD_TO_RANDAO_PADDING), reveal_0.getEpoch()).v()) {
      slash_validator(state_0, reveal_0.getRevealed_index(), reveal_0.getMasker_index());
    } else {
      var max_proposer_slot_reward_0 = divide(divide(multiply(get_base_reward(state_0, reveal_0.getRevealed_index()), SLOTS_PER_EPOCH), len(get_active_validator_indices(state_0, get_current_epoch(state_0)))), PROPOSER_REWARD_QUOTIENT);
      var penalty_0 = new Gwei(multiply(multiply(max_proposer_slot_reward_0, EARLY_DERIVED_SECRET_REVEAL_SLOT_REWARD_MULTIPLE), plus(len(state_0.getExposed_derived_secrets().get(derived_secret_location_0)), pyint.create(1L))));
      var proposer_index_0 = get_beacon_proposer_index(state_0);
      var whistleblower_index_0 = reveal_0.getMasker_index();
      var whistleblowing_reward_0 = new Gwei(divide(penalty_0, WHISTLEBLOWER_REWARD_QUOTIENT));
      var proposer_reward_0 = new Gwei(divide(whistleblowing_reward_0, PROPOSER_REWARD_QUOTIENT));
      increase_balance(state_0, proposer_index_0, proposer_reward_0);
      increase_balance(state_0, whistleblower_index_0, minus(whistleblowing_reward_0, proposer_reward_0));
      decrease_balance(state_0, reveal_0.getRevealed_index(), penalty_0);
      state_0.getExposed_derived_secrets().get(derived_secret_location_0).append(reveal_0.getRevealed_index());
    }
  }

  public void process_custody_slashing(BeaconState state_0, SignedCustodySlashing signed_custody_slashing_0) {
    var custody_slashing_0 = signed_custody_slashing_0.getMessage();
    var attestation_0 = custody_slashing_0.getAttestation();
    var malefactor_0 = state_0.getValidators().get(custody_slashing_0.getMalefactor_index());
    var whistleblower_0 = state_0.getValidators().get(custody_slashing_0.getWhistleblower_index());
    var domain_0 = get_domain(state_0, DOMAIN_CUSTODY_BIT_SLASHING, get_current_epoch(state_0));
    var signing_root_0 = compute_signing_root(custody_slashing_0, domain_0);
    pyassert(bls.Verify(whistleblower_0.getPubkey(), signing_root_0, signed_custody_slashing_0.getSignature()));
    pyassert(is_slashable_validator(whistleblower_0, get_current_epoch(state_0)));
    pyassert(is_slashable_validator(malefactor_0, get_current_epoch(state_0)));
    pyassert(is_valid_indexed_attestation(state_0, get_indexed_attestation(state_0, attestation_0)));
    var shard_transition_0 = custody_slashing_0.getShard_transition();
    pyassert(eq(hash_tree_root(shard_transition_0), attestation_0.getData().getShard_transition_root()));
    pyassert(eq(len(custody_slashing_0.getData()), shard_transition_0.getShard_block_lengths().get(custody_slashing_0.getData_index())));
    pyassert(eq(hash_tree_root(custody_slashing_0.getData()), shard_transition_0.getShard_data_roots().get(custody_slashing_0.getData_index())));
    var attesters_0 = get_attesting_indices(state_0, attestation_0.getData(), attestation_0.getAggregation_bits());
    pyassert(contains(attesters_0, custody_slashing_0.getMalefactor_index()));
    var epoch_to_sign_0 = get_randao_epoch_for_custody_period(get_custody_period_for_validator(custody_slashing_0.getMalefactor_index(), attestation_0.getData().getTarget().getEpoch()), custody_slashing_0.getMalefactor_index());
    var domain_1 = get_domain(state_0, DOMAIN_RANDAO, epoch_to_sign_0);
    var signing_root_1 = compute_signing_root(epoch_to_sign_0, domain_1);
    pyassert(bls.Verify(malefactor_0.getPubkey(), signing_root_1, custody_slashing_0.getMalefactor_secret()));
    var computed_custody_bit_0 = compute_custody_bit(custody_slashing_0.getMalefactor_secret(), custody_slashing_0.getData());
    if (eq(computed_custody_bit_0, pyint.create(1L)).v()) {
      slash_validator(state_0, custody_slashing_0.getMalefactor_index(), null);
      var committee_0 = get_beacon_committee(state_0, attestation_0.getData().getSlot(), attestation_0.getData().getIndex());
      var others_count_0 = minus(len(committee_0), pyint.create(1L));
      var whistleblower_reward_0 = new Gwei(divide(divide(malefactor_0.getEffective_balance(), WHISTLEBLOWER_REWARD_QUOTIENT), others_count_0));
      for (var attester_index_0: attesters_0) {
        if (not(eq(attester_index_0, custody_slashing_0.getMalefactor_index())).v()) {
          increase_balance(state_0, attester_index_0, whistleblower_reward_0);
        }
      }
    } else {
      slash_validator(state_0, custody_slashing_0.getWhistleblower_index(), null);
    }
  }

  public void process_reveal_deadlines(BeaconState state_0) {
    var epoch_0 = get_current_epoch(state_0);
    for (var tmp_22: enumerate(state_0.getValidators())) {
      var index_0 = tmp_22.first;
      var validator_0 = tmp_22.second;
      var deadline_0 = plus(validator_0.getNext_custody_secret_to_reveal(), pyint.create(1L));
      if (greater(deadline_0, get_custody_period_for_validator(new ValidatorIndex(index_0), epoch_0)).v()) {
        slash_validator(state_0, new ValidatorIndex(index_0), null);
      }
    }
  }

  public void process_challenge_deadlines(BeaconState state_0) {
    for (var custody_chunk_challenge_0: state_0.getCustody_chunk_challenge_records()) {
      if (greater(plus(custody_chunk_challenge_0.getInclusion_epoch(), EPOCHS_PER_CUSTODY_PERIOD), get_current_epoch(state_0)).v()) {
        slash_validator(state_0, custody_chunk_challenge_0.getResponder_index(), custody_chunk_challenge_0.getChallenger_index());
        var index_in_records_0 = state_0.getCustody_chunk_challenge_records().index(custody_chunk_challenge_0);
        state_0.getCustody_chunk_challenge_records().set(index_in_records_0, new CustodyChunkChallengeRecord(CustodyChunkChallengeRecord.challenge_index_default, CustodyChunkChallengeRecord.challenger_index_default, CustodyChunkChallengeRecord.responder_index_default, CustodyChunkChallengeRecord.inclusion_epoch_default, CustodyChunkChallengeRecord.data_root_default, CustodyChunkChallengeRecord.chunk_index_default));
      }
    }
  }

  public void process_custody_final_updates(BeaconState state_0) {
    state_0.getExposed_derived_secrets().set(modulo(get_current_epoch(state_0), EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS), new SSZList<ValidatorIndex>(PyList.of()));
    var records_0 = state_0.getCustody_chunk_challenge_records();
    var validator_indices_in_records_0 = set(records_0.map((record) -> record.getResponder_index()));
    for (var tmp_23: enumerate(state_0.getValidators())) {
      var index_0 = tmp_23.first;
      var validator_0 = tmp_23.second;
      if (not(eq(validator_0.getExit_epoch(), FAR_FUTURE_EPOCH)).v()) {
        var not_all_secrets_are_revealed_0 = eq(validator_0.getAll_custody_secrets_revealed_epoch(), FAR_FUTURE_EPOCH);
        if (or(contains(validator_indices_in_records_0, new ValidatorIndex(index_0)), not_all_secrets_are_revealed_0).v()) {
          validator_0.setWithdrawable_epoch(FAR_FUTURE_EPOCH);
        } else {
          if (eq(validator_0.getWithdrawable_epoch(), FAR_FUTURE_EPOCH).v()) {
            validator_0.setWithdrawable_epoch(new Epoch(plus(validator_0.getAll_custody_secrets_revealed_epoch(), MIN_VALIDATOR_WITHDRAWABILITY_DELAY)));
          }
        }
      }
    }
  }

  public Slot compute_previous_slot(Slot slot_0) {
    if (greater(pyint.create(0L), slot_0).v()) {
      return new Slot(minus(slot_0, pyint.create(1L)));
    } else {
      return new Slot(pyint.create(0L));
    }
  }

  /*
      Create a compact validator object representing index, slashed status, and compressed balance.
      Takes as input balance-in-increments (// EFFECTIVE_BALANCE_INCREMENT) to preserve symmetry with
      the unpacking function.
      */
  public uint64 pack_compact_validator(ValidatorIndex index_0, SSZBoolean slashed_0, uint64 balance_in_increments_0) {
    return plus(plus(leftShift(index_0, pyint.create(16L)), leftShift(slashed_0, pyint.create(15L))), balance_in_increments_0);
  }

  /*
      Return validator index, slashed, balance // EFFECTIVE_BALANCE_INCREMENT
      */
  public Triple<ValidatorIndex,pybool,uint64> unpack_compact_validator(uint64 compact_validator_0) {
    return new Triple<>(new ValidatorIndex(rightShift(compact_validator_0, pyint.create(16L))), pybool(modulo(rightShift(compact_validator_0, pyint.create(15L)), pyint.create(2L))), bitAnd(compact_validator_0, minus(power(pyint.create(2L), pyint.create(15L)), pyint.create(1L))));
  }

  /*
      Given a state and a list of validator indices, outputs the ``CompactCommittee`` representing them.
      */
  public CompactCommittee committee_to_compact_committee(BeaconState state_0, Sequence<ValidatorIndex> committee_0) {
    var validators_0 = committee_0.map((i) -> state_0.getValidators().get(i));
    var compact_validators_0 = zip(committee_0, validators_0)
        .map((tmp_24) -> {
          var i = tmp_24.first; var v = tmp_24.second;
          return pack_compact_validator(i, v.getSlashed(), divide(v.getEffective_balance(), EFFECTIVE_BALANCE_INCREMENT)); });
    var pubkeys_0 = validators_0.map((v) -> v.getPubkey());
    return new CompactCommittee(new SSZList<>(pubkeys_0), new SSZList<>(compact_validators_0));
  }

  public Shard compute_shard_from_committee_index(BeaconState state_0, CommitteeIndex index_0, Slot slot_0) {
    var active_shards_0 = get_active_shard_count(state_0);
    return new Shard(modulo(plus(index_0, get_start_shard(state_0, slot_0)), active_shards_0));
  }

  /*
      Return the offset slots that are greater than ``start_slot`` and less than ``end_slot``.
      */
  public Sequence<Slot> compute_offset_slots(Slot start_slot_0, Slot end_slot_0) {
    return SHARD_BLOCK_OFFSETS.filter((x) -> less(end_slot_0, plus(start_slot_0, x))).map((x) -> new Slot(plus(start_slot_0, x)));
  }

  public Gwei compute_updated_gasprice(Gwei prev_gasprice_0, uint64 shard_block_length_0) {
    if (greater(TARGET_SHARD_BLOCK_SIZE, shard_block_length_0).v()) {
      var delta_0 = divide(divide(multiply(prev_gasprice_0, minus(shard_block_length_0, TARGET_SHARD_BLOCK_SIZE)), TARGET_SHARD_BLOCK_SIZE), GASPRICE_ADJUSTMENT_COEFFICIENT);
      return min(plus(prev_gasprice_0, delta_0), MAX_GASPRICE);
    } else {
      var delta_1 = divide(divide(multiply(prev_gasprice_0, minus(TARGET_SHARD_BLOCK_SIZE, shard_block_length_0)), TARGET_SHARD_BLOCK_SIZE), GASPRICE_ADJUSTMENT_COEFFICIENT);
      return minus(max(prev_gasprice_0, plus(MIN_GASPRICE, delta_1)), delta_1);
    }
  }

  /*
      Return the source epoch for computing the committee.
      */
  public Epoch compute_committee_source_epoch(Epoch epoch_0, uint64 period_0) {
    var source_epoch_0 = new Epoch(minus(epoch_0, modulo(epoch_0, period_0)));
    Epoch source_epoch_2;
    if (greaterOrEqual(period_0, source_epoch_0).v()) {
      var source_epoch_1 = minus(source_epoch_0, period_0);
      source_epoch_2 = source_epoch_1;
    } else {
      source_epoch_2 = source_epoch_0;
    }
    return source_epoch_2;
  }

  /*
      Return the number of active shards.
      Note that this puts an upper bound on the number of committees per slot.
      */
  public uint64 get_active_shard_count(BeaconState state_0) {
    return INITIAL_ACTIVE_SHARDS;
  }

  public Set<ValidatorIndex> get_online_validator_indices(BeaconState state_0) {
    var active_validators_0 = get_active_validator_indices(state_0, get_current_epoch(state_0));
    return set(active_validators_0.filter((i) -> not(eq(state_0.getOnline_countdown().get(i), pyint.create(0L)))).map((i) -> i));
  }

  /*
      Return the shard committee of the given ``epoch`` of the given ``shard``.
      */
  public Sequence<ValidatorIndex> get_shard_committee(BeaconState beacon_state_0, Epoch epoch_0, Shard shard_0) {
    var source_epoch_0 = compute_committee_source_epoch(epoch_0, SHARD_COMMITTEE_PERIOD);
    var active_validator_indices_0 = get_active_validator_indices(beacon_state_0, source_epoch_0);
    var seed_0 = get_seed(beacon_state_0, source_epoch_0, DOMAIN_SHARD_COMMITTEE);
    return compute_committee(active_validator_indices_0, seed_0, shard_0, get_active_shard_count(beacon_state_0));
  }

  /*
      Return the light client committee of no more than ``LIGHT_CLIENT_COMMITTEE_SIZE`` validators.
      */
  public Sequence<ValidatorIndex> get_light_client_committee(BeaconState beacon_state_0, Epoch epoch_0) {
    var source_epoch_0 = compute_committee_source_epoch(epoch_0, LIGHT_CLIENT_COMMITTEE_PERIOD);
    var active_validator_indices_0 = get_active_validator_indices(beacon_state_0, source_epoch_0);
    var seed_0 = get_seed(beacon_state_0, source_epoch_0, DOMAIN_LIGHT_CLIENT);
    return compute_committee(active_validator_indices_0, seed_0, new uint64(pyint.create(0L)), get_active_shard_count(beacon_state_0)).getSlice(pyint.create(0L), LIGHT_CLIENT_COMMITTEE_SIZE);
  }

  /*
      Return the proposer's index of shard block at ``slot``.
      */
  public ValidatorIndex get_shard_proposer_index(BeaconState beacon_state_0, Slot slot_0, Shard shard_0) {
    var epoch_0 = compute_epoch_at_slot(slot_0);
    var committee_0 = get_shard_committee(beacon_state_0, epoch_0, shard_0);
    var seed_0 = hash(plus(get_seed(beacon_state_0, epoch_0, DOMAIN_SHARD_COMMITTEE), uint_to_bytes(slot_0)));
    var r_0 = bytes_to_uint64(seed_0.getSlice(pyint.create(0L), pyint.create(8L)));
    return committee_0.get(modulo(r_0, len(committee_0)));
  }

  /*
      Return the sum of committee counts in range ``listOf(start_slot, stop_slot)``.
      */
  public uint64 get_committee_count_delta(BeaconState state_0, Slot start_slot_0, Slot stop_slot_0) {
    return new uint64(sum(range(start_slot_0, stop_slot_0).map((slot) -> get_committee_count_per_slot(state_0, compute_epoch_at_slot(new Slot(slot))))));
  }

  /*
      Return the start shard at ``slot``.
      */
  public Shard get_start_shard(BeaconState state_0, Slot slot_0) {
    var current_epoch_start_slot_0 = compute_start_slot_at_epoch(get_current_epoch(state_0));
    var active_shard_count_0 = get_active_shard_count(state_0);
    if (eq(current_epoch_start_slot_0, slot_0).v()) {
      return state_0.getCurrent_epoch_start_shard();
    } else {
      if (greater(current_epoch_start_slot_0, slot_0).v()) {
        var shard_delta_0 = get_committee_count_delta(state_0, current_epoch_start_slot_0, slot_0);
        return new Shard(modulo(plus(state_0.getCurrent_epoch_start_shard(), shard_delta_0), active_shard_count_0));
      } else {
        var shard_delta_1 = get_committee_count_delta(state_0, slot_0, current_epoch_start_slot_0);
        var max_committees_per_slot_0 = active_shard_count_0;
        var max_committees_in_span_0 = multiply(max_committees_per_slot_0, minus(current_epoch_start_slot_0, slot_0));
        return new Shard(modulo(minus(plus(state_0.getCurrent_epoch_start_shard(), max_committees_in_span_0), shard_delta_1), active_shard_count_0));
      }
    }
  }

  /*
      Return the latest slot number of the given ``shard``.
      */
  public Slot get_latest_slot_for_shard(BeaconState state_0, Shard shard_0) {
    return state_0.getShard_states().get(shard_0).getSlot();
  }

  /*
      Return the offset slots of the given ``shard``.
      The offset slot are after the latest slot and before current slot.
      */
  public Sequence<Slot> get_offset_slots(BeaconState state_0, Shard shard_0) {
    return compute_offset_slots(get_latest_slot_for_shard(state_0, shard_0), state_0.getSlot());
  }

  /*
      Check if the given ``attestation_data`` is on-time.
      */
  public pybool is_on_time_attestation(BeaconState state_0, AttestationData attestation_data_0) {
    return eq(attestation_data_0.getSlot(), compute_previous_slot(state_0.getSlot()));
  }

  /*
      Check if on-time ``attestation`` helped contribute to the successful crosslink of
      ``winning_root`` formed by ``committee_index`` committee.
      */
  public pybool is_winning_attestation(BeaconState state_0, PendingAttestation attestation_0, CommitteeIndex committee_index_0, Root winning_root_0) {
    return and(is_on_time_attestation(state_0, attestation_0.getData()), eq(attestation_0.getData().getIndex(), committee_index_0), eq(attestation_0.getData().getShard_transition_root(), winning_root_0));
  }

  /*
      If ``pubkeys`` is an empty list, the given ``signature`` should be a stub ``NO_SIGNATURE``.
      Otherwise, verify it with standard BLS AggregateVerify API.
      */
  public pybool optional_aggregate_verify(Sequence<BLSPubkey> pubkeys_0, Sequence<? extends Bytes32> messages_0, BLSSignature signature_0) {
    if (eq(len(pubkeys_0), pyint.create(0L)).v()) {
      return eq(signature_0, NO_SIGNATURE);
    } else {
      return bls.AggregateVerify(pubkeys_0, messages_0, signature_0);
    }
  }

  /*
      If ``pubkeys`` is an empty list, the given ``signature`` should be a stub ``NO_SIGNATURE``.
      Otherwise, verify it with standard BLS FastAggregateVerify API.
      */
  public pybool optional_fast_aggregate_verify(Sequence<BLSPubkey> pubkeys_0, Bytes32 message_0, BLSSignature signature_0) {
    if (eq(len(pubkeys_0), pyint.create(0L)).v()) {
      return eq(signature_0, NO_SIGNATURE);
    } else {
      return bls.FastAggregateVerify(pubkeys_0, message_0, signature_0);
    }
  }

  public void validate_attestation(BeaconState state_0, Attestation attestation_0) {
    var data_0 = attestation_0.getData();
    pyassert(less(get_committee_count_per_slot(state_0, data_0.getTarget().getEpoch()), data_0.getIndex()));
    pyassert(contains(new Pair<>(get_previous_epoch(state_0), get_current_epoch(state_0)), data_0.getTarget().getEpoch()));
    pyassert(eq(data_0.getTarget().getEpoch(), compute_epoch_at_slot(data_0.getSlot())));
    pyassert(and(lessOrEqual(state_0.getSlot(), plus(data_0.getSlot(), MIN_ATTESTATION_INCLUSION_DELAY)), lessOrEqual(plus(data_0.getSlot(), SLOTS_PER_EPOCH), state_0.getSlot())));
    var committee_0 = get_beacon_committee(state_0, data_0.getSlot(), data_0.getIndex());
    pyassert(eq(len(attestation_0.getAggregation_bits()), len(committee_0)));
    if (eq(data_0.getTarget().getEpoch(), get_current_epoch(state_0)).v()) {
      pyassert(eq(data_0.getSource(), state_0.getCurrent_justified_checkpoint()));
    } else {
      pyassert(eq(data_0.getSource(), state_0.getPrevious_justified_checkpoint()));
    }
    if (is_on_time_attestation(state_0, data_0).v()) {
      pyassert(eq(data_0.getBeacon_block_root(), get_block_root_at_slot(state_0, compute_previous_slot(state_0.getSlot()))));
      var shard_0 = compute_shard_from_committee_index(state_0, data_0.getIndex(), data_0.getSlot());
      pyassert(eq(data_0.getShard(), shard_0));
      if (greater(GENESIS_SLOT, data_0.getSlot()).v()) {
        pyassert(not(eq(data_0.getShard_transition_root(), hash_tree_root(new ShardTransition(ShardTransition.start_slot_default, ShardTransition.shard_block_lengths_default, ShardTransition.shard_data_roots_default, ShardTransition.shard_states_default, ShardTransition.proposer_signature_aggregate_default)))));
      } else {
        pyassert(eq(data_0.getShard_transition_root(), hash_tree_root(new ShardTransition(ShardTransition.start_slot_default, ShardTransition.shard_block_lengths_default, ShardTransition.shard_data_roots_default, ShardTransition.shard_states_default, ShardTransition.proposer_signature_aggregate_default))));
      }
    } else {
      pyassert(less(compute_previous_slot(state_0.getSlot()), data_0.getSlot()));
      pyassert(eq(data_0.getShard_transition_root(), new Root()));
    }
    pyassert(is_valid_indexed_attestation(state_0, get_indexed_attestation(state_0, attestation_0)));
  }

  public void apply_shard_transition(BeaconState state_0, Shard shard_0, ShardTransition transition_0) {
    pyassert(greater(PHASE_1_FORK_SLOT, state_0.getSlot()));
    var offset_slots_0 = get_offset_slots(state_0, shard_0);
    pyassert(and(eq(len(transition_0.getShard_data_roots()), len(transition_0.getShard_states())), eq(len(transition_0.getShard_states()), len(transition_0.getShard_block_lengths())), eq(len(transition_0.getShard_block_lengths()), len(offset_slots_0))));
    pyassert(eq(transition_0.getStart_slot(), offset_slots_0.get(pyint.create(0L))));
    var headers_0 = new PyList<ShardBlockHeader>();
    var proposers_0 = new PyList<ValidatorIndex>();
    var prev_gasprice_0 = state_0.getShard_states().get(shard_0).getGasprice();
    var shard_parent_root_0 = state_0.getShard_states().get(shard_0).getLatest_block_root();
    var prev_gasprice_2 = prev_gasprice_0;
    var shard_parent_root_3 = shard_parent_root_0;
    for (var tmp_25: enumerate(offset_slots_0)) {
      var i_0 = tmp_25.first;
      var offset_slot_0 = tmp_25.second;
      var shard_block_length_0 = transition_0.getShard_block_lengths().get(i_0);
      var shard_state_0 = transition_0.getShard_states().get(i_0);
      pyassert(eq(shard_state_0.getGasprice(), compute_updated_gasprice(prev_gasprice_2, shard_block_length_0)));
      pyassert(eq(shard_state_0.getSlot(), offset_slot_0));
      var is_empty_proposal_0 = eq(shard_block_length_0, pyint.create(0L));
      Root shard_parent_root_2;
      if (not(is_empty_proposal_0).v()) {
        var proposal_index_0 = get_shard_proposer_index(state_0, offset_slot_0, shard_0);
        var header_0 = new ShardBlockHeader(
            shard_parent_root_3,
            get_block_root_at_slot(state_0, offset_slot_0),
            offset_slot_0, shard_0, proposal_index_0, transition_0.getShard_data_roots().get(i_0));
        var shard_parent_root_1 = hash_tree_root(header_0);
        headers_0.append(header_0);
        proposers_0.append(proposal_index_0);
        shard_parent_root_2 = shard_parent_root_1;
      } else {
        pyassert(eq(transition_0.getShard_data_roots().get(i_0), new Root()));
        shard_parent_root_2 = shard_parent_root_3;
      }
      var prev_gasprice_1 = shard_state_0.getGasprice();
      prev_gasprice_2 = prev_gasprice_1;
      shard_parent_root_3 = shard_parent_root_2;
    }
    var pubkeys_0 = proposers_0.map((proposer) -> state_0.getValidators().get(proposer).getPubkey());
    var signing_roots_0 = headers_0.map((header) -> compute_signing_root(header, get_domain(state_0, DOMAIN_SHARD_PROPOSAL, compute_epoch_at_slot(header.getSlot()))));
    pyassert(optional_aggregate_verify(pubkeys_0, signing_roots_0, transition_0.getProposer_signature_aggregate()));
    var shard_state_1 = copy(transition_0.getShard_states().get(minus(len(transition_0.getShard_states()), pyint.create(1L))));
    shard_state_1.setSlot(compute_previous_slot(state_0.getSlot()));
    state_0.getShard_states().set(shard_0, shard_state_1);
  }

  public Root process_crosslink_for_shard(BeaconState state_0, CommitteeIndex committee_index_0, ShardTransition shard_transition_0, Sequence<Attestation> attestations_0) {
    var on_time_attestation_slot_0 = compute_previous_slot(state_0.getSlot());
    var committee_0 = get_beacon_committee(state_0, on_time_attestation_slot_0, committee_index_0);
    var online_indices_0 = get_online_validator_indices(state_0);
    var shard_0 = compute_shard_from_committee_index(state_0, committee_index_0, on_time_attestation_slot_0);
    var shard_transition_roots_0 = set(attestations_0.map((a) -> a.getData().getShard_transition_root()));
    for (var shard_transition_root_0: sorted(shard_transition_roots_0)) {
      var transition_attestations_0 = attestations_0.filter((a) -> eq(a.getData().getShard_transition_root(), shard_transition_root_0)).map((a) -> a);
      Set<ValidatorIndex> transition_participants_0 = new Set<ValidatorIndex>();
      var transition_participants_2 = transition_participants_0;
      for (var attestation_0: transition_attestations_0) {
        var participants_0 = get_attesting_indices(state_0, attestation_0.getData(), attestation_0.getAggregation_bits());
        var transition_participants_1 = transition_participants_2.union(participants_0);
        transition_participants_2 = transition_participants_1;
      }
      var enough_online_stake_0 = greaterOrEqual(multiply(get_total_balance(state_0, online_indices_0.intersection(committee_0)), pyint.create(2L)), multiply(get_total_balance(state_0, online_indices_0.intersection(transition_participants_2)), pyint.create(3L)));
      if (not(enough_online_stake_0).v()) {
        continue;
      }
      pyassert(eq(shard_transition_root_0, hash_tree_root(shard_transition_0)));
      var last_offset_index_0 = minus(len(shard_transition_0.getShard_states()), pyint.create(1L));
      var shard_head_root_0 = shard_transition_0.getShard_states().get(last_offset_index_0).getLatest_block_root();
      for (var attestation_1: transition_attestations_0) {
        pyassert(eq(attestation_1.getData().getShard_head_root(), shard_head_root_0));
      }
      apply_shard_transition(state_0, shard_0, shard_transition_0);
      var beacon_proposer_index_0 = get_beacon_proposer_index(state_0);
      var estimated_attester_reward_0 = sum(transition_participants_2.map((attester) -> get_base_reward(state_0, attester)));
      var proposer_reward_0 = new Gwei(divide(estimated_attester_reward_0, PROPOSER_REWARD_QUOTIENT));
      increase_balance(state_0, beacon_proposer_index_0, proposer_reward_0);
      var states_slots_lengths_0 = zip(shard_transition_0.getShard_states(), get_offset_slots(state_0, shard_0), shard_transition_0.getShard_block_lengths());
      for (var tmp_26: states_slots_lengths_0) {
        var shard_state_0 = tmp_26.first;
        var slot_0 = tmp_26.second;
        var length_0 = tmp_26.third;
        var proposer_index_0 = get_shard_proposer_index(state_0, slot_0, shard_0);
        decrease_balance(state_0, proposer_index_0, multiply(shard_state_0.getGasprice(), length_0));
      }
      return shard_transition_root_0;
    }
    pyassert(eq(shard_transition_0, new ShardTransition(ShardTransition.start_slot_default, ShardTransition.shard_block_lengths_default, ShardTransition.shard_data_roots_default, ShardTransition.shard_states_default, ShardTransition.proposer_signature_aggregate_default)));
    return new Root();
  }

  public void process_crosslinks(BeaconState state_0, Sequence<ShardTransition> shard_transitions_0, Sequence<Attestation> attestations_0) {
    var on_time_attestation_slot_0 = compute_previous_slot(state_0.getSlot());
    var committee_count_0 = get_committee_count_per_slot(state_0, compute_epoch_at_slot(on_time_attestation_slot_0));
    for (var committee_index_0: map(CommitteeIndex::new, range(committee_count_0))) {
      var shard_0 = compute_shard_from_committee_index(state_0, committee_index_0, on_time_attestation_slot_0);
      var shard_attestations_0 = attestations_0.filter((attestation) -> and(is_on_time_attestation(state_0, attestation.getData()), eq(attestation.getData().getIndex(), committee_index_0))).map((attestation) -> attestation);
      var winning_root_0 = process_crosslink_for_shard(state_0, committee_index_0, shard_transitions_0.get(shard_0), shard_attestations_0);
      if (not(eq(winning_root_0, new Root())).v()) {
        for (var pending_attestation_0: state_0.getCurrent_epoch_attestations()) {
          if (is_winning_attestation(state_0, pending_attestation_0, committee_index_0, winning_root_0).v()) {
            pending_attestation_0.setCrosslink_success(new SSZBoolean(pybool.create(true)));
          }
        }
      }
    }
  }

  /*
      Verify that a `shard_transition` in a block is empty if an attestation was not processed for it.
      */
  public pybool verify_empty_shard_transition(BeaconState state_0, Sequence<ShardTransition> shard_transitions_0) {
    for (var shard_0: range(get_active_shard_count(state_0))) {
      if (not(eq(state_0.getShard_states().get(shard_0).getSlot(), compute_previous_slot(state_0.getSlot()))).v()) {
        if (not(eq(shard_transitions_0.get(shard_0), new ShardTransition(ShardTransition.start_slot_default, ShardTransition.shard_block_lengths_default, ShardTransition.shard_data_roots_default, ShardTransition.shard_states_default, ShardTransition.proposer_signature_aggregate_default))).v()) {
          return pybool.create(false);
        }
      }
    }
    return pybool.create(true);
  }

  public void process_shard_transitions(BeaconState state_0, Sequence<ShardTransition> shard_transitions_0, Sequence<Attestation> attestations_0) {
    if (greater(GENESIS_SLOT, compute_previous_slot(state_0.getSlot())).v()) {
      process_crosslinks(state_0, shard_transitions_0, attestations_0);
    }
    pyassert(verify_empty_shard_transition(state_0, shard_transitions_0));
  }

  public void process_light_client_aggregate(BeaconState state_0, BeaconBlockBody block_body_0) {
    var committee_0 = get_light_client_committee(state_0, get_current_epoch(state_0));
    var previous_slot_0 = compute_previous_slot(state_0.getSlot());
    var previous_block_root_0 = get_block_root_at_slot(state_0, previous_slot_0);
    var total_reward_0 = new Gwei(pyint.create(0L));
    var signer_pubkeys_0 = new PyList<BLSPubkey>();
    var total_reward_2 = total_reward_0;
    for (var tmp_27: enumerate(committee_0)) {
      var bit_index_0 = tmp_27.first;
      var participant_index_0 = tmp_27.second;
      if (pybool(block_body_0.getLight_client_bits().get(bit_index_0)).v()) {
        signer_pubkeys_0.append(state_0.getValidators().get(participant_index_0).getPubkey());
        if (not(state_0.getValidators().get(participant_index_0).getSlashed()).v()) {
          increase_balance(state_0, participant_index_0, get_base_reward(state_0, participant_index_0));
          var total_reward_1 = plus(total_reward_2, get_base_reward(state_0, participant_index_0));
          total_reward_2 = total_reward_1;
        } else {
          total_reward_2 = total_reward_2;
        }
      } else {
        total_reward_2 = total_reward_2;
      }
    }
    increase_balance(state_0, get_beacon_proposer_index(state_0), new Gwei(divide(total_reward_2, PROPOSER_REWARD_QUOTIENT)));
    var signing_root_0 = compute_signing_root(previous_block_root_0, get_domain(state_0, DOMAIN_LIGHT_CLIENT, compute_epoch_at_slot(previous_slot_0)));
    pyassert(optional_fast_aggregate_verify(signer_pubkeys_0, signing_root_0, block_body_0.getLight_client_signature()));
  }

  public void process_phase_1_final_updates(BeaconState state_0) {
    process_custody_final_updates(state_0);
    process_online_tracking(state_0);
    process_light_client_committee_updates(state_0);
    state_0.setCurrent_epoch_start_shard(get_start_shard(state_0, new Slot(plus(state_0.getSlot(), pyint.create(1L)))));
  }

  public void process_online_tracking(BeaconState state_0) {
    for (var index_0: range(len(state_0.getValidators()))) {
      if (not(eq(state_0.getOnline_countdown().get(index_0), pyint.create(0L))).v()) {
        state_0.getOnline_countdown().set(index_0, minus(state_0.getOnline_countdown().get(index_0), pyint.create(1L)));
      }
    }
    for (var pending_attestation_0: plus(state_0.getCurrent_epoch_attestations(), state_0.getPrevious_epoch_attestations())) {
      for (var index_1: get_attesting_indices(state_0, pending_attestation_0.getData(), pending_attestation_0.getAggregation_bits())) {
        state_0.getOnline_countdown().set(index_1, ONLINE_PERIOD);
      }
    }
  }

  /*
      Update light client committees.
      */
  public void process_light_client_committee_updates(BeaconState state_0) {
    var next_epoch_0 = compute_epoch_at_slot(new Slot(plus(state_0.getSlot(), pyint.create(1L))));
    if (eq(modulo(next_epoch_0, LIGHT_CLIENT_COMMITTEE_PERIOD), pyint.create(0L)).v()) {
      state_0.setCurrent_light_committee(state_0.getNext_light_committee());
      var new_committee_0 = get_light_client_committee(state_0, plus(next_epoch_0, LIGHT_CLIENT_COMMITTEE_PERIOD));
      state_0.setNext_light_committee(committee_to_compact_committee(state_0, new_committee_0));
    }
  }

  public pybool verify_shard_block_message(BeaconState beacon_parent_state_0, ShardState shard_parent_state_0, ShardBlock block_0) {
    pyassert(eq(block_0.getShard_parent_root(), shard_parent_state_0.getLatest_block_root()));
    var beacon_parent_block_header_0 = beacon_parent_state_0.getLatest_block_header().copy();
    if (eq(beacon_parent_block_header_0.getState_root(), new Root()).v()) {
      beacon_parent_block_header_0.setState_root(hash_tree_root(beacon_parent_state_0));
    }
    var beacon_parent_root_0 = hash_tree_root(beacon_parent_block_header_0);
    pyassert(eq(block_0.getBeacon_parent_root(), beacon_parent_root_0));
    var shard_0 = block_0.getShard();
    var next_slot_0 = new Slot(plus(block_0.getSlot(), pyint.create(1L)));
    var offset_slots_0 = compute_offset_slots(get_latest_slot_for_shard(beacon_parent_state_0, shard_0), next_slot_0);
    pyassert(contains(offset_slots_0, block_0.getSlot()));
    pyassert(eq(block_0.getProposer_index(), get_shard_proposer_index(beacon_parent_state_0, block_0.getSlot(), shard_0)));
    pyassert(and(less(len(block_0.getBody()), pyint.create(0L)), lessOrEqual(MAX_SHARD_BLOCK_SIZE, len(block_0.getBody()))));
    return pybool.create(true);
  }

  public pybool verify_shard_block_signature(BeaconState beacon_parent_state_0, SignedShardBlock signed_block_0) {
    var proposer_0 = beacon_parent_state_0.getValidators().get(signed_block_0.getMessage().getProposer_index());
    var domain_0 = get_domain(beacon_parent_state_0, DOMAIN_SHARD_PROPOSAL, compute_epoch_at_slot(signed_block_0.getMessage().getSlot()));
    var signing_root_0 = compute_signing_root(signed_block_0.getMessage(), domain_0);
    return bls.Verify(proposer_0.getPubkey(), signing_root_0, signed_block_0.getSignature());
  }

  public void shard_state_transition(ShardState shard_state_0, SignedShardBlock signed_block_0, BeaconState beacon_parent_state_0, pybool validate_result_0) {
    pyassert(verify_shard_block_message(beacon_parent_state_0, shard_state_0, signed_block_0.getMessage()));
    if (validate_result_0.v()) {
      pyassert(verify_shard_block_signature(beacon_parent_state_0, signed_block_0));
    }
    process_shard_block(shard_state_0, signed_block_0.getMessage());
  }

  /*
      Update ``shard_state`` with shard ``block``.
      */
  public void process_shard_block(ShardState shard_state_0, ShardBlock block_0) {
    shard_state_0.setSlot(block_0.getSlot());
    var prev_gasprice_0 = shard_state_0.getGasprice();
    var shard_block_length_0 = len(block_0.getBody());
    shard_state_0.setGasprice(compute_updated_gasprice(prev_gasprice_0, new uint64(shard_block_length_0)));
    if (not(eq(shard_block_length_0, pyint.create(0L))).v()) {
      shard_state_0.setLatest_block_root(hash_tree_root(block_0));
    }
  }

  public pybool is_valid_fraud_proof(BeaconState beacon_state_0, Attestation attestation_0, uint64 offset_index_0, ShardTransition transition_0, ShardBlock block_0, BLSPubkey subkey_0, BeaconBlock beacon_parent_block_0) {
    var custody_bits_0 = attestation_0.getCustody_bits_blocks();
    for (var j_0: range(len(custody_bits_0.get(offset_index_0)))) {
      if (not(eq(custody_bits_0.get(offset_index_0).get(j_0), generate_custody_bit(subkey_0, block_0))).v()) {
        return pybool.create(true);
      }
    }
    ShardState shard_state_2;
    if (eq(offset_index_0, pyint.create(0L)).v()) {
      var shard_states_0 = beacon_parent_block_0.getBody().getShard_transitions().get(attestation_0.getData().getShard()).getShard_states();
      var shard_state_0 = shard_states_0.get(minus(len(shard_states_0), pyint.create(1L)));
      shard_state_2 = shard_state_0;
    } else {
      var shard_state_1 = transition_0.getShard_states().get(minus(offset_index_0, pyint.create(1L)));
      shard_state_2 = shard_state_1;
    }
    process_shard_block(shard_state_2, block_0);
    if (not(eq(shard_state_2, transition_0.getShard_states().get(offset_index_0))).v()) {
      return pybool.create(true);
    }
    return pybool.create(false);
  }

  public pybool generate_custody_bit(BLSPubkey subkey_0, ShardBlock block_0) {
    throw new RuntimeException("Not implemented (elipsis)");
  }

  public BeaconState upgrade_to_phase1(BeaconState pre_0) {
    var epoch_0 = get_current_epoch(pre_0);
    var post_0 = new BeaconState(pre_0.getGenesis_time(), BeaconState.genesis_validators_root_default, pre_0.getSlot(), new Fork(pre_0.getFork().getCurrent_version(), PHASE_1_FORK_VERSION, epoch_0), pre_0.getLatest_block_header(), pre_0.getBlock_roots(), pre_0.getState_roots(), pre_0.getHistorical_roots(), pre_0.getEth1_data(), pre_0.getEth1_data_votes(), pre_0.getEth1_deposit_index(), new SSZList<Validator>(enumerate(pre_0.getValidators()).map((tmp_28) -> { var i = tmp_28.first; var phase0_validator = tmp_28.second; return new Validator(phase0_validator.getPubkey(), phase0_validator.getWithdrawal_credentials(), phase0_validator.getEffective_balance(), phase0_validator.getSlashed(), phase0_validator.getActivation_eligibility_epoch(), phase0_validator.getActivation_eligibility_epoch(), phase0_validator.getExit_epoch(), phase0_validator.getWithdrawable_epoch(), get_custody_period_for_validator(new ValidatorIndex(i), epoch_0), FAR_FUTURE_EPOCH); })), pre_0.getBalances(), pre_0.getRandao_mixes(), pre_0.getSlashings(), new SSZList<PendingAttestation>(), new SSZList<PendingAttestation>(), pre_0.getJustification_bits(), pre_0.getPrevious_justified_checkpoint(), pre_0.getCurrent_justified_checkpoint(), pre_0.getFinalized_checkpoint(), new Shard(pyint.create(0L)), new SSZList<ShardState>(range(INITIAL_ACTIVE_SHARDS).map((i) -> new ShardState(compute_previous_slot(pre_0.getSlot()), MIN_GASPRICE, new Root()))),
        new SSZList<>(multiply(PyList.of(ONLINE_PERIOD), len(pre_0.getValidators()))),
        new CompactCommittee(CompactCommittee.pubkeys_default, CompactCommittee.compact_validators_default), new CompactCommittee(CompactCommittee.pubkeys_default, CompactCommittee.compact_validators_default),
        new SSZVector<>(multiply(PyList.of(new SSZList<>(PyList.of())), EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS)),
        BeaconState.custody_chunk_challenge_records_default,
        BeaconState.custody_chunk_challenge_index_default);
    var next_epoch_0 = new Epoch(plus(epoch_0, pyint.create(1L)));
    post_0.setCurrent_light_committee(committee_to_compact_committee(post_0, get_light_client_committee(post_0, epoch_0)));
    post_0.setNext_light_committee(committee_to_compact_committee(post_0, get_light_client_committee(post_0, next_epoch_0)));
    return post_0;
  }

  public ShardStore get_forkchoice_shard_store(BeaconState anchor_state_0, Shard shard_0) {
    return new ShardStore(shard_0, new PyDict<>(new Pair<>(anchor_state_0.getShard_states().get(shard_0).getLatest_block_root(), new SignedShardBlock(new ShardBlock(ShardBlock.shard_parent_root_default, ShardBlock.beacon_parent_root_default, compute_previous_slot(anchor_state_0.getSlot()), shard_0, ShardBlock.proposer_index_default, ShardBlock.body_default), SignedShardBlock.signature_default))), new PyDict<>(new Pair<>(anchor_state_0.getShard_states().get(shard_0).getLatest_block_root(), anchor_state_0.copy().getShard_states().get(shard_0))), ShardStore.latest_messages_default);
  }

  public Gwei get_shard_latest_attesting_balance(Store store_0, Shard shard_0, Root root_0) {
    var shard_store_0 = store_0.getShard_stores().get(shard_0);
    var state_0 = store_0.getCheckpoint_states().get(store_0.getJustified_checkpoint());
    var active_indices_0 = get_active_validator_indices(state_0, get_current_epoch(state_0));
    return new Gwei(sum(active_indices_0.filter((i) -> and(contains(shard_store_0.getLatest_messages(), i), eq(get_shard_ancestor(store_0, shard_0, shard_store_0.getLatest_messages().get(i).getRoot(), shard_store_0.getSigned_blocks().get(root_0).getMessage().getSlot()), root_0))).map((i) -> state_0.getValidators().get(i).getEffective_balance())));
  }

  /*
      Execute the LMD-GHOST fork choice.
      */
  public Root get_shard_head(Store store_0, Shard shard_0) {
    var shard_store_0 = store_0.getShard_stores().get(shard_0);
    var beacon_head_root_0 = get_head(store_0);
    var shard_head_state_0 = store_0.getBlock_states().get(beacon_head_root_0).getShard_states().get(shard_0);
    var shard_head_root_0 = shard_head_state_0.getLatest_block_root();
    var shard_blocks_0 = new PyDict<>(shard_store_0.getSigned_blocks().items().filter((tmp_29) -> { var root = tmp_29.first; var signed_shard_block = tmp_29.second; return greater(shard_head_state_0.getSlot(), signed_shard_block.getMessage().getSlot()); }).map((tmp_30) -> { var root = tmp_30.first; var signed_shard_block = tmp_30.second; return new Pair<>(root, signed_shard_block.getMessage()); }));
    var shard_head_root_2 = shard_head_root_0;
    while (true) {
      Root finalShard_head_root_ = shard_head_root_2;
      var children_0 = shard_blocks_0.items()
          .filter((tmp_31) -> {
            var root = tmp_31.first; var shard_block = tmp_31.second;
            return eq(shard_block.getShard_parent_root(), finalShard_head_root_); })
          .map((tmp_32) -> {
            var root = tmp_32.first; var shard_block = tmp_32.second;
            return root; });
      if (eq(len(children_0), pyint.create(0L)).v()) {
        return shard_head_root_2;
      }
      var shard_head_root_1 = max(children_0, (root) -> new Pair<>(get_shard_latest_attesting_balance(store_0, shard_0, root), root));
      shard_head_root_2 = shard_head_root_1;
    }
  }

  public Root get_shard_ancestor(Store store_0, Shard shard_0, Root root_0, Slot slot_0) {
    var shard_store_0 = store_0.getShard_stores().get(shard_0);
    var block_0 = shard_store_0.getSigned_blocks().get(root_0).getMessage();
    if (greater(slot_0, block_0.getSlot()).v()) {
      return get_shard_ancestor(store_0, shard_0, block_0.getShard_parent_root(), slot_0);
    } else {
      if (eq(block_0.getSlot(), slot_0).v()) {
        return root_0;
      } else {
        return root_0;
      }
    }
  }

  /*
      Return the canonical shard block branch that has not yet been crosslinked.
      */
  public Sequence<SignedShardBlock> get_pending_shard_blocks(Store store_0, Shard shard_0) {
    var shard_store_0 = store_0.getShard_stores().get(shard_0);
    var beacon_head_root_0 = get_head(store_0);
    var beacon_head_state_0 = store_0.getBlock_states().get(beacon_head_root_0);
    var latest_shard_block_root_0 = beacon_head_state_0.getShard_states().get(shard_0).getLatest_block_root();
    var shard_head_root_0 = get_shard_head(store_0, shard_0);
    var root_0 = shard_head_root_0;
    var signed_shard_blocks_0 = new PyList<SignedShardBlock>();
    var root_2 = root_0;
    while (not(eq(root_2, latest_shard_block_root_0)).v()) {
      var signed_shard_block_0 = shard_store_0.getSigned_blocks().get(root_2);
      signed_shard_blocks_0.append(signed_shard_block_0);
      var root_1 = signed_shard_block_0.getMessage().getShard_parent_root();
      root_2 = root_1;
    }
    signed_shard_blocks_0.reverse();
    return signed_shard_blocks_0;
  }

  public void on_shard_block(Store store_0, SignedShardBlock signed_shard_block_0) {
    var shard_block_0 = signed_shard_block_0.getMessage();
    var shard_0 = shard_block_0.getShard();
    var shard_store_0 = store_0.getShard_stores().get(shard_0);
    pyassert(contains(shard_store_0.getBlock_states(), shard_block_0.getShard_parent_root()));
    var shard_parent_state_0 = shard_store_0.getBlock_states().get(shard_block_0.getShard_parent_root());
    pyassert(contains(store_0.getBlock_states(), shard_block_0.getBeacon_parent_root()));
    var beacon_parent_state_0 = store_0.getBlock_states().get(shard_block_0.getBeacon_parent_root());
    var finalized_beacon_state_0 = store_0.getBlock_states().get(store_0.getFinalized_checkpoint().getRoot());
    var finalized_shard_state_0 = finalized_beacon_state_0.getShard_states().get(shard_0);
    pyassert(greater(finalized_shard_state_0.getSlot(), shard_block_0.getSlot()));
    var finalized_slot_0 = compute_start_slot_at_epoch(store_0.getFinalized_checkpoint().getEpoch());
    pyassert(eq(get_ancestor(store_0, shard_block_0.getBeacon_parent_root(), finalized_slot_0), store_0.getFinalized_checkpoint().getRoot()));
    var shard_state_0 = shard_parent_state_0.copy();
    shard_state_transition(shard_state_0, signed_shard_block_0, beacon_parent_state_0, pybool.create(true));
    shard_store_0.getSigned_blocks().set(hash_tree_root(shard_block_0), signed_shard_block_0);
    shard_store_0.getBlock_states().set(hash_tree_root(shard_block_0), shard_state_0);
  }

  public Pair<Sequence<Shard>,Sequence<Root>> get_shard_winning_roots(BeaconState state_0, Sequence<Attestation> attestations_0) {
    var shards_0 = new PyList<Shard>();
    var winning_roots_0 = new PyList<Root>();
    var online_indices_0 = get_online_validator_indices(state_0);
    var on_time_attestation_slot_0 = compute_previous_slot(state_0.getSlot());
    var committee_count_0 = get_committee_count_per_slot(state_0, compute_epoch_at_slot(on_time_attestation_slot_0));
    for (var committee_index_0: map(CommitteeIndex::new, range(committee_count_0))) {
      var shard_0 = compute_shard_from_committee_index(state_0, committee_index_0, on_time_attestation_slot_0);
      var shard_attestations_0 = attestations_0.filter((attestation) -> and(is_on_time_attestation(state_0, attestation.getData()), eq(attestation.getData().getIndex(), committee_index_0))).map((attestation) -> attestation);
      var committee_0 = get_beacon_committee(state_0, on_time_attestation_slot_0, committee_index_0);
      var shard_transition_roots_0 = set(shard_attestations_0.map((a) -> a.getData().getShard_transition_root()));
      for (var shard_transition_root_0: sorted(shard_transition_roots_0)) {
        var transition_attestations_0 = shard_attestations_0.filter((a) -> eq(a.getData().getShard_transition_root(), shard_transition_root_0)).map((a) -> a);
        Set<ValidatorIndex> transition_participants_0 = new Set<ValidatorIndex>();
        var transition_participants_2 = transition_participants_0;
        for (var attestation_0: transition_attestations_0) {
          var participants_0 = get_attesting_indices(state_0, attestation_0.getData(), attestation_0.getAggregation_bits());
          var transition_participants_1 = transition_participants_2.union(participants_0);
          transition_participants_2 = transition_participants_1;
        }
        var enough_online_stake_0 = greaterOrEqual(multiply(get_total_balance(state_0, online_indices_0.intersection(committee_0)), pyint.create(2L)), multiply(get_total_balance(state_0, online_indices_0.intersection(transition_participants_2)), pyint.create(3L)));
        if (enough_online_stake_0.v()) {
          shards_0.append(shard_0);
          winning_roots_0.append(shard_transition_root_0);
          break;
        }
      }
    }
    return new Pair<>(shards_0, winning_roots_0);
  }

  public LightClientVote get_best_light_client_aggregate(BeaconBlock block_0, Sequence<LightClientVote> aggregates_0) {
    var viable_aggregates_0 = aggregates_0.filter((aggregate) -> and(eq(aggregate.getData().getSlot(), compute_previous_slot(block_0.getSlot())), eq(aggregate.getData().getBeacon_block_root(), block_0.getParent_root()))).map((aggregate) -> aggregate);
    return max(viable_aggregates_0, (a) -> new Pair<>(len(a.getAggregation_bits().filter((i) -> eq(i, pyint.create(1L))).map((i) -> i)), hash_tree_root(a)), new LightClientVote(LightClientVote.data_default, LightClientVote.aggregation_bits_default, LightClientVote.signature_default));
  }

  public Triple<Sequence<uint64>,Sequence<Root>,Sequence<ShardState>> get_shard_transition_fields(BeaconState beacon_state_0, Shard shard_0, Sequence<SignedShardBlock> shard_blocks_0) {
    var shard_block_lengths_0 = new PyList<uint64>();
    var shard_data_roots_0 = new PyList<Root>();
    var shard_states_0 = new PyList<ShardState>();
    var shard_state_0 = beacon_state_0.getShard_states().get(shard_0);
    var shard_block_slots_0 = shard_blocks_0.map((shard_block) -> shard_block.getMessage().getSlot());
    var offset_slots_0 = compute_offset_slots(get_latest_slot_for_shard(beacon_state_0, shard_0), new Slot(plus(beacon_state_0.getSlot(), pyint.create(1L))));
    var shard_state_2 = shard_state_0;
    for (var slot_0: offset_slots_0) {
      SignedShardBlock shard_block_2;
      if (contains(shard_block_slots_0, slot_0).v()) {
        var shard_block_0 = shard_blocks_0.get(shard_block_slots_0.index(slot_0));
        shard_data_roots_0.append(hash_tree_root(shard_block_0.getMessage().getBody()));
        shard_block_2 = shard_block_0;
      } else {
        var shard_block_1 = new SignedShardBlock(new ShardBlock(ShardBlock.shard_parent_root_default, ShardBlock.beacon_parent_root_default, slot_0, shard_0, ShardBlock.proposer_index_default, ShardBlock.body_default), SignedShardBlock.signature_default);
        shard_data_roots_0.append(new Root());
        shard_block_2 = shard_block_1;
      }
      var shard_state_1 = shard_state_2.copy();
      process_shard_block(shard_state_1, shard_block_2.getMessage());
      shard_states_0.append(shard_state_1);
      shard_block_lengths_0.append(new uint64(len(shard_block_2.getMessage().getBody())));
      shard_state_2 = shard_state_1;
    }
    return new Triple<>(shard_block_lengths_0, shard_data_roots_0, shard_states_0);
  }

  public ShardTransition get_shard_transition(BeaconState beacon_state_0, Shard shard_0, Sequence<SignedShardBlock> shard_blocks_0) {
    if (eq(beacon_state_0.getSlot(), GENESIS_SLOT).v()) {
      return new ShardTransition(ShardTransition.start_slot_default, ShardTransition.shard_block_lengths_default, ShardTransition.shard_data_roots_default, ShardTransition.shard_states_default, ShardTransition.proposer_signature_aggregate_default);
    }
    var offset_slots_0 = compute_offset_slots(get_latest_slot_for_shard(beacon_state_0, shard_0), new Slot(plus(beacon_state_0.getSlot(), pyint.create(1L))));
    var tmp_33 = get_shard_transition_fields(beacon_state_0, shard_0, shard_blocks_0);
    var shard_block_lengths_0 = tmp_33.first;
    var shard_data_roots_0 = tmp_33.second;
    var shard_states_0 = tmp_33.third;
    BLSSignature proposer_signature_aggregate_2;
    if (greater(pyint.create(0L), len(shard_blocks_0)).v()) {
      var proposer_signatures_0 = shard_blocks_0.map((shard_block) -> shard_block.getSignature());
      var proposer_signature_aggregate_0 = bls.Aggregate(proposer_signatures_0);
      proposer_signature_aggregate_2 = proposer_signature_aggregate_0;
    } else {
      var proposer_signature_aggregate_1 = NO_SIGNATURE;
      proposer_signature_aggregate_2 = proposer_signature_aggregate_1;
    }
    return new ShardTransition(offset_slots_0.get(pyint.create(0L)), new SSZList<>(shard_block_lengths_0), new SSZList<>(shard_data_roots_0), new SSZList<>(shard_states_0), proposer_signature_aggregate_2);
  }

  public pybool is_in_next_light_client_committee(BeaconState state_0, ValidatorIndex index_0) {
    var next_committee_0 = get_light_client_committee(state_0, plus(get_current_epoch(state_0), LIGHT_CLIENT_COMMITTEE_PERIOD));
    return contains(next_committee_0, index_0);
  }

  public BLSSignature get_light_client_vote_signature(BeaconState state_0, LightClientVoteData light_client_vote_data_0, pyint privkey_0) {
    var domain_0 = get_domain(state_0, DOMAIN_LIGHT_CLIENT, compute_epoch_at_slot(light_client_vote_data_0.getSlot()));
    var signing_root_0 = compute_signing_root(light_client_vote_data_0, domain_0);
    return bls.Sign(privkey_0, signing_root_0);
  }

  public BLSSignature get_light_client_slot_signature(BeaconState state_0, Slot slot_0, pyint privkey_0) {
    var domain_0 = get_domain(state_0, DOMAIN_LIGHT_SELECTION_PROOF, compute_epoch_at_slot(slot_0));
    var signing_root_0 = compute_signing_root(slot_0, domain_0);
    return bls.Sign(privkey_0, signing_root_0);
  }

  public pybool is_light_client_aggregator(BeaconState state_0, Slot slot_0, BLSSignature slot_signature_0) {
    var committee_0 = get_light_client_committee(state_0, compute_epoch_at_slot(slot_0));
    var modulo_0 = max(pyint.create(1L), divide(len(committee_0), TARGET_LIGHT_CLIENT_AGGREGATORS_PER_SLOT));
    return eq(modulo(bytes_to_uint64(hash(slot_signature_0).getSlice(pyint.create(0L), pyint.create(8L))), modulo_0), pyint.create(0L));
  }

  public BLSSignature get_aggregate_light_client_signature(Sequence<LightClientVote> light_client_votes_0) {
    var signatures_0 = light_client_votes_0.map((light_client_vote) -> light_client_vote.getSignature());
    return bls.Aggregate(signatures_0);
  }

  public LightAggregateAndProof get_light_aggregate_and_proof(BeaconState state_0, ValidatorIndex aggregator_index_0, LightClientVote aggregate_0, pyint privkey_0) {
    return new LightAggregateAndProof(aggregator_index_0, aggregate_0, get_light_client_slot_signature(state_0, aggregate_0.getData().getSlot(), privkey_0));
  }

  public BLSSignature get_light_aggregate_and_proof_signature(BeaconState state_0, LightAggregateAndProof aggregate_and_proof_0, pyint privkey_0) {
    var aggregate_0 = aggregate_and_proof_0.getAggregate();
    var domain_0 = get_domain(state_0, DOMAIN_LIGHT_AGGREGATE_AND_PROOF, compute_epoch_at_slot(aggregate_0.getData().getSlot()));
    var signing_root_0 = compute_signing_root(aggregate_and_proof_0, domain_0);
    return bls.Sign(privkey_0, signing_root_0);
  }

  public BLSSignature get_custody_secret(BeaconState state_0, ValidatorIndex validator_index_0, pyint privkey_0, Epoch epoch_0) {
    Epoch epoch_2;
    if (epoch_0 == null) {
      var epoch_1 = get_current_epoch(state_0);
      epoch_2 = epoch_1;
    } else {
      epoch_2 = epoch_0;
    }
    var period_0 = get_custody_period_for_validator(validator_index_0, epoch_2);
    var epoch_to_sign_0 = get_randao_epoch_for_custody_period(period_0, validator_index_0);
    var domain_0 = get_domain(state_0, DOMAIN_RANDAO, epoch_to_sign_0);
    var signing_root_0 = compute_signing_root(new Epoch(epoch_to_sign_0), domain_0);
    return bls.Sign(privkey_0, signing_root_0);
  }

  /*
      A stub function return mocking Eth1Data.
      */
  public Eth1Data get_eth1_data(Eth1Block block_0) {
    return new Eth1Data(block_0.getDeposit_root(), block_0.getDeposit_count(), hash_tree_root(block_0));
  }
}