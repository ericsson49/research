package beacon_java.phase0;

import static beacon_java.phase0.Constants.*;
import static beacon_java.deps.BLS.bls;
import static beacon_java.phase0.Utils.copy;
import static beacon_java.pylib.Exports.*;

import beacon_java.data.BLSSignature;
import beacon_java.data.Root;
import beacon_java.phase0.data.*;
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



  public pybool is_valid_merkle_branch(Bytes32 leaf_0, Sequence<Bytes32> branch_0, uint64 depth_0, uint64 index_0, Root root_0) {
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
    return max(new uint64(pyint.create(1L)), min(MAX_COMMITTEES_PER_SLOT, divide(divide(new uint64(len(get_active_validator_indices(state_0, epoch_0))), SLOTS_PER_EPOCH), TARGET_COMMITTEE_SIZE)));
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
        BeaconState.genesis_validators_root_default, BeaconState.slot_default, fork_0,
        new BeaconBlockHeader(BeaconBlockHeader.slot_default, BeaconBlockHeader.proposer_index_default, BeaconBlockHeader.parent_root_default, BeaconBlockHeader.state_root_default, hash_tree_root(new BeaconBlockBody(BeaconBlockBody.randao_reveal_default, BeaconBlockBody.eth1_data_default, BeaconBlockBody.graffiti_default, BeaconBlockBody.proposer_slashings_default, BeaconBlockBody.attester_slashings_default, BeaconBlockBody.attestations_default, BeaconBlockBody.deposits_default, BeaconBlockBody.voluntary_exits_default))),
        BeaconState.block_roots_default,
        BeaconState.state_roots_default,
        BeaconState.historical_roots_default,
        new Eth1Data(Eth1Data.deposit_root_default, len(deposits_0), eth1_block_hash_0),
        BeaconState.eth1_data_votes_default,
        BeaconState.eth1_deposit_index_default,
        BeaconState.validators_default,
        BeaconState.balances_default,
        new SSZVector<>(multiply(PyList.of(eth1_block_hash_0), EPOCHS_PER_HISTORICAL_VECTOR)),
        BeaconState.slashings_default,
        BeaconState.previous_epoch_attestations_default,
        BeaconState.current_epoch_attestations_default,
        BeaconState.justification_bits_default,
        BeaconState.previous_justified_checkpoint_default,
        BeaconState.current_justified_checkpoint_default,
        BeaconState.finalized_checkpoint_default);
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
    process_slashings(state_0);
    process_eth1_data_reset(state_0);
    process_effective_balance_updates(state_0);
    process_slashings_reset(state_0);
    process_randao_mixes_reset(state_0);
    process_historical_roots_update(state_0);
    process_participation_record_updates(state_0);
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
    var previous_epoch_0 = get_previous_epoch(state_0);
    var current_epoch_0 = get_current_epoch(state_0);
    var old_previous_justified_checkpoint_0 = state_0.getPrevious_justified_checkpoint();
    var old_current_justified_checkpoint_0 = state_0.getCurrent_justified_checkpoint();
    state_0.setPrevious_justified_checkpoint(state_0.getCurrent_justified_checkpoint());
    state_0.getJustification_bits().setSlice(pyint.create(1L), null, state_0.getJustification_bits().getSlice(pyint.create(0L), minus(JUSTIFICATION_BITS_LENGTH, pyint.create(1L))));
    state_0.getJustification_bits().set(pyint.create(0L), new SSZBoolean(pyint.create(0L)));
    var matching_target_attestations_0 = get_matching_target_attestations(state_0, previous_epoch_0);
    if (greaterOrEqual(multiply(get_total_active_balance(state_0), pyint.create(2L)), multiply(get_attesting_balance(state_0, matching_target_attestations_0), pyint.create(3L))).v()) {
      state_0.setCurrent_justified_checkpoint(new Checkpoint(previous_epoch_0, get_block_root(state_0, previous_epoch_0)));
      state_0.getJustification_bits().set(pyint.create(1L), new SSZBoolean(pyint.create(1L)));
    }
    var matching_target_attestations_1 = get_matching_target_attestations(state_0, current_epoch_0);
    if (greaterOrEqual(multiply(get_total_active_balance(state_0), pyint.create(2L)), multiply(get_attesting_balance(state_0, matching_target_attestations_1), pyint.create(3L))).v()) {
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
    state_0.getRandao_mixes().set(modulo(next_epoch_0, EPOCHS_PER_HISTORICAL_VECTOR), get_randao_mix(state_0, current_epoch_0));
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
    process_operations(state_0, block_0.getBody());
  }



  public void process_block_header(BeaconState state_0, BeaconBlock block_0) {
    pyassert(eq(block_0.getSlot(), state_0.getSlot()));
    pyassert(greater(state_0.getLatest_block_header().getSlot(), block_0.getSlot()));
    pyassert(eq(block_0.getProposer_index(), get_beacon_proposer_index(state_0)));
    pyassert(eq(block_0.getParent_root(), hash_tree_root(state_0.getLatest_block_header())));
    state_0.setLatest_block_header(new BeaconBlockHeader(
        block_0.getSlot(),
        block_0.getProposer_index(),
        block_0.getParent_root(),
        new Root(new Bytes32()),
        hash_tree_root(block_0.getBody())));
    var proposer_0 = state_0.getValidators().get(block_0.getProposer_index());
    pyassert(not(proposer_0.getSlashed()));
  }


  public void process_randao(BeaconState state_0, BeaconBlockBody body_0) {
    var epoch_0 = get_current_epoch(state_0);
    var proposer_0 = state_0.getValidators().get(get_beacon_proposer_index(state_0));
    var signing_root_0 = compute_signing_root(epoch_0, get_domain(state_0, DOMAIN_RANDAO, null));
    pyassert(bls.Verify(proposer_0.getPubkey(), signing_root_0, body_0.getRandao_reveal()));
    var mix_0 = xor(get_randao_mix(state_0, epoch_0), hash(body_0.getRandao_reveal()));
    state_0.getRandao_mixes().set(modulo(epoch_0, EPOCHS_PER_HISTORICAL_VECTOR), mix_0);
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
  }

  public void process_proposer_slashing(BeaconState state_0, ProposerSlashing proposer_slashing_0) {
    var header_1_0 = proposer_slashing_0.getSigned_header_1().getMessage();
    var header_2_0 = proposer_slashing_0.getSigned_header_2().getMessage();
    pyassert(eq(header_1_0.getSlot(), header_2_0.getSlot()));
    pyassert(eq(header_1_0.getProposer_index(), header_2_0.getProposer_index()));
    pyassert(not(eq(header_1_0, header_2_0)));
    var proposer_0 = state_0.getValidators().get(header_1_0.getProposer_index());
    pyassert(is_slashable_validator(proposer_0, get_current_epoch(state_0)));
    for (var signed_header_0: PyList.of(proposer_slashing_0.getSigned_header_1(), proposer_slashing_0.getSigned_header_2())) {
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
    var data_0 = attestation_0.getData();
    pyassert(contains(new Pair<>(get_previous_epoch(state_0), get_current_epoch(state_0)), data_0.getTarget().getEpoch()));
    pyassert(eq(data_0.getTarget().getEpoch(), compute_epoch_at_slot(data_0.getSlot())));
    pyassert(and(lessOrEqual(state_0.getSlot(), plus(data_0.getSlot(), MIN_ATTESTATION_INCLUSION_DELAY)), lessOrEqual(plus(data_0.getSlot(), SLOTS_PER_EPOCH), state_0.getSlot())));
    pyassert(less(get_committee_count_per_slot(state_0, data_0.getTarget().getEpoch()), data_0.getIndex()));
    var committee_0 = get_beacon_committee(state_0, data_0.getSlot(), data_0.getIndex());
    pyassert(eq(len(attestation_0.getAggregation_bits()), len(committee_0)));
    var pending_attestation_0 = new PendingAttestation(attestation_0.getAggregation_bits(), data_0, minus(state_0.getSlot(), data_0.getSlot()), get_beacon_proposer_index(state_0));
    if (eq(data_0.getTarget().getEpoch(), get_current_epoch(state_0)).v()) {
      pyassert(eq(data_0.getSource(), state_0.getCurrent_justified_checkpoint()));
      state_0.getCurrent_epoch_attestations().append(pending_attestation_0);
    } else {
      pyassert(eq(data_0.getSource(), state_0.getPrevious_justified_checkpoint()));
      state_0.getPrevious_epoch_attestations().append(pending_attestation_0);
    }
    pyassert(is_valid_indexed_attestation(state_0, get_indexed_attestation(state_0, attestation_0)));
  }


  public Validator get_validator_from_deposit(BeaconState state_0, Deposit deposit_0) {
    var amount_0 = deposit_0.getData().getAmount();
    var effective_balance_0 = min(minus(amount_0, modulo(amount_0, EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE);
    return new Validator(deposit_0.getData().getPubkey(), deposit_0.getData().getWithdrawal_credentials(), effective_balance_0, Validator.slashed_default, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH);
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
        new uint64(plus(anchor_state_0.getGenesis_time(), multiply(SECONDS_PER_SLOT, anchor_state_0.getSlot()))),
        anchor_state_0.getGenesis_time(),
        justified_checkpoint_0,
        finalized_checkpoint_0,
        justified_checkpoint_0,
        new PyDict<>(new Pair<>(anchor_root_0, copy(anchor_block_0))),
        new PyDict<>(new Pair<>(anchor_root_0, copy(anchor_state_0))),
        new PyDict<>(new Pair<>(justified_checkpoint_0, copy(anchor_state_0))),
        Store.latest_messages_default);
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
    for (var i_0: attesting_indices_0) {
      if (or(not(contains(store_0.getLatest_messages(), i_0)), greater(store_0.getLatest_messages().get(i_0).getEpoch(), target_0.getEpoch())).v()) {
        store_0.getLatest_messages().set(i_0, new LatestMessage(target_0.getEpoch(), beacon_block_root_0));
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

/*
    A stub function return mocking Eth1Data.
    */

  public Eth1Data get_eth1_data(Eth1Block block_0) {
    return new Eth1Data(block_0.getDeposit_root(), block_0.getDeposit_count(), hash_tree_root(block_0));
  }
}
