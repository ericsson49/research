package beacon_java.altair;

import static beacon_java.phase0.Constants.*;
import static beacon_java.phase0.Spec.*;
import static beacon_java.altair.Constants.*;
import static beacon_java.altair.Utils.hash_tree_root;
import static beacon_java.deps.BLS.bls;
import static beacon_java.altair.Utils.copy;
import static beacon_java.pylib.Exports.*;
import static beacon_java.util.Exports.floorlog2;

import beacon_java.altair.data.BeaconBlock;
import beacon_java.altair.data.BeaconBlockBody;
import beacon_java.phase0.data.BeaconBlockHeader;
import beacon_java.altair.data.BeaconState;
import beacon_java.phase0.data.*;
import beacon_java.altair.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class Spec {
  /*
      Return a new ``ParticipationFlags`` adding ``flag_index`` to ``flags``.
      */
  public static ParticipationFlags add_flag(ParticipationFlags flags, pyint flag_index) {
    var flag = new ParticipationFlags(pow(pyint.create(2L), flag_index));
    return bitOr(flags, flag);
  }

  /*
      Return whether ``flags`` has ``flag_index`` set.
      */
  public static pybool has_flag(ParticipationFlags flags, pyint flag_index) {
    var flag = new ParticipationFlags(pow(pyint.create(2L), flag_index));
    return eq(bitAnd(flags, flag), flag);
  }

  /*
      Return the sync committee indices, with possible duplicates, for the next sync committee.
      */
  public static Sequence<ValidatorIndex> get_next_sync_committee_indices(BeaconState state) {
    var epoch = new Epoch(plus(get_current_epoch(state), pyint.create(1L)));
    var MAX_RANDOM_BYTE = minus(pow(pyint.create(2L), pyint.create(8L)), pyint.create(1L));
    var active_validator_indices = get_active_validator_indices(state, epoch);
    var active_validator_count = new uint64(len(active_validator_indices));
    var seed = get_seed(state, epoch, DOMAIN_SYNC_COMMITTEE);
    var i = pyint.create(0L);
    PyList<ValidatorIndex> sync_committee_indices = PyList.of();
    var i_2 = i;
    while (less(len(sync_committee_indices), SYNC_COMMITTEE_SIZE).v()) {
      var shuffled_index = compute_shuffled_index(new uint64(modulo(i_2, active_validator_count)), active_validator_count, seed);
      var candidate_index = active_validator_indices.get(shuffled_index);
      var random_byte = hash(plus(seed, uint_to_bytes(new uint64(divide(i_2, pyint.create(32L)))))).get(modulo(i_2, pyint.create(32L)));
      var effective_balance = state.getValidators().get(candidate_index).getEffective_balance();
      if (greaterOrEqual(multiply(effective_balance, MAX_RANDOM_BYTE), multiply(MAX_EFFECTIVE_BALANCE, random_byte)).v()) {
        sync_committee_indices.append(candidate_index);
      }
      var i_1 = plus(i_2, pyint.create(1L));
      i_2 = i_1;
    }
    return sync_committee_indices;
  }

  /*
      Return the next sync committee, with possible pubkey duplicates.
      */
  public static SyncCommittee get_next_sync_committee(BeaconState state) {
    var indices = get_next_sync_committee_indices(state);
    var pubkeys = list(indices.map((index) -> state.getValidators().get(index).getPubkey()));
    var aggregate_pubkey = eth2_aggregate_pubkeys(pubkeys);
    return new SyncCommittee(new SSZVector<>(pubkeys), aggregate_pubkey);
  }

  public static Gwei get_base_reward_per_increment(BeaconState state) {
    return new Gwei(divide(multiply(EFFECTIVE_BALANCE_INCREMENT, BASE_REWARD_FACTOR), integer_squareroot(get_total_active_balance(state))));
  }

  /*
      Return the base reward for the validator defined by ``index`` with respect to the current ``state``.
      */
  public static Gwei get_base_reward(BeaconState state, ValidatorIndex index) {
    var increments = divide(state.getValidators().get(index).getEffective_balance(), EFFECTIVE_BALANCE_INCREMENT);
    return new Gwei(multiply(increments, get_base_reward_per_increment(state)));
  }

  /*
      Return the set of validator indices that are both active and unslashed for the given ``flag_index`` and ``epoch``.
      */
  public static Set<ValidatorIndex> get_unslashed_participating_indices(BeaconState state, pyint flag_index, Epoch epoch) {
    pyassert(contains(new Pair<>(get_previous_epoch(state), get_current_epoch(state)), epoch));
    SSZList<ParticipationFlags> epoch_participation_2;
    if (eq(epoch, get_current_epoch(state)).v()) {
      var epoch_participation = state.getCurrent_epoch_participation();
      epoch_participation_2 = epoch_participation;
    } else {
      var epoch_participation_1 = state.getPrevious_epoch_participation();
      epoch_participation_2 = epoch_participation_1;
    }
    var active_validator_indices = get_active_validator_indices(state, epoch);
    var participating_indices = list(active_validator_indices.filter((i) -> has_flag(epoch_participation_2.get(i), flag_index)).map((i) -> i));
    return set(filter((index) -> not(state.getValidators().get(index).getSlashed()), participating_indices));
  }

  /*
      Return the flag indices that are satisfied by an attestation.
      */
  public static Sequence<pyint> get_attestation_participation_flag_indices(BeaconState state, AttestationData data, uint64 inclusion_delay) {
    Checkpoint justified_checkpoint_2;
    if (eq(data.getTarget().getEpoch(), get_current_epoch(state)).v()) {
      var justified_checkpoint = state.getCurrent_justified_checkpoint();
      justified_checkpoint_2 = justified_checkpoint;
    } else {
      var justified_checkpoint_1 = state.getPrevious_justified_checkpoint();
      justified_checkpoint_2 = justified_checkpoint_1;
    }
    var is_matching_source = eq(data.getSource(), justified_checkpoint_2);
    var is_matching_target = and(is_matching_source, eq(data.getTarget().getRoot(), get_block_root(state, data.getTarget().getEpoch())));
    var is_matching_head = and(is_matching_target, eq(data.getBeacon_block_root(), get_block_root_at_slot(state, data.getSlot())));
    pyassert(is_matching_source);
    var participation_flag_indices = new PyList<pyint>();
    if (and(is_matching_source, lessOrEqual(inclusion_delay, integer_squareroot(SLOTS_PER_EPOCH))).v()) {
      participation_flag_indices.append(TIMELY_SOURCE_FLAG_INDEX);
    }
    if (and(is_matching_target, lessOrEqual(inclusion_delay, SLOTS_PER_EPOCH)).v()) {
      participation_flag_indices.append(TIMELY_TARGET_FLAG_INDEX);
    }
    if (and(is_matching_head, eq(inclusion_delay, MIN_ATTESTATION_INCLUSION_DELAY)).v()) {
      participation_flag_indices.append(TIMELY_HEAD_FLAG_INDEX);
    }
    return participation_flag_indices;
  }

  /*
      Return the deltas for a given ``flag_index`` by scanning through the participation flags.
      */
  public static Pair<Sequence<Gwei>,Sequence<Gwei>> get_flag_index_deltas(BeaconState state, pyint flag_index) {
    var rewards = multiply(PyList.of(new Gwei(pyint.create(0L))), len(state.getValidators()));
    var penalties = multiply(PyList.of(new Gwei(pyint.create(0L))), len(state.getValidators()));
    var previous_epoch = get_previous_epoch(state);
    var unslashed_participating_indices = get_unslashed_participating_indices(state, flag_index, previous_epoch);
    var weight = PARTICIPATION_FLAG_WEIGHTS.get(flag_index);
    var unslashed_participating_balance = get_total_balance(state, unslashed_participating_indices);
    var unslashed_participating_increments = divide(unslashed_participating_balance, EFFECTIVE_BALANCE_INCREMENT);
    var active_increments = divide(get_total_active_balance(state), EFFECTIVE_BALANCE_INCREMENT);
    for (var index: get_eligible_validator_indices(state)) {
      var base_reward = get_base_reward(state, index);
      if (contains(unslashed_participating_indices, index).v()) {
        if (not(is_in_inactivity_leak(state)).v()) {
          var reward_numerator = multiply(multiply(base_reward, weight), unslashed_participating_increments);
          rewards.set(index, plus(rewards.get(index), new Gwei(divide(reward_numerator, multiply(active_increments, WEIGHT_DENOMINATOR)))));
        }
      } else {
        if (not(eq(flag_index, TIMELY_HEAD_FLAG_INDEX)).v()) {
          penalties.set(index, plus(penalties.get(index), new Gwei(divide(multiply(base_reward, weight), WEIGHT_DENOMINATOR))));
        }
      }
    }
    return new Pair<>(rewards, penalties);
  }

  /*
      Return the inactivity penalty deltas by considering timely target participation flags and inactivity scores.
      */
  public static Pair<Sequence<Gwei>,Sequence<Gwei>> get_inactivity_penalty_deltas(BeaconState state) {
    var rewards = list(range(len(state.getValidators())).map((_0) -> new Gwei(pyint.create(0L))));
    var penalties = list(range(len(state.getValidators())).map((_0) -> new Gwei(pyint.create(0L))));
    var previous_epoch = get_previous_epoch(state);
    var matching_target_indices = get_unslashed_participating_indices(state, TIMELY_TARGET_FLAG_INDEX, previous_epoch);
    for (var index: get_eligible_validator_indices(state)) {
      if (not(contains(matching_target_indices, index)).v()) {
        var penalty_numerator = multiply(state.getValidators().get(index).getEffective_balance(), state.getInactivity_scores().get(index));
        var penalty_denominator = multiply(INACTIVITY_SCORE_BIAS, INACTIVITY_PENALTY_QUOTIENT_ALTAIR);
        penalties.set(index, plus(penalties.get(index), new Gwei(divide(penalty_numerator, penalty_denominator))));
      }
    }
    return new Pair<>(rewards, penalties);
  }

  /*
      Slash the validator with index ``slashed_index``.
      */
  public static void slash_validator(BeaconState state, ValidatorIndex slashed_index, ValidatorIndex whistleblower_index) {
    var epoch = get_current_epoch(state);
    initiate_validator_exit(state, slashed_index);
    var validator = state.getValidators().get(slashed_index);
    validator.setSlashed(new beacon_java.ssz.SSZBoolean(pybool.create(true)));
    validator.setWithdrawable_epoch(max(validator.getWithdrawable_epoch(), new Epoch(plus(epoch, EPOCHS_PER_SLASHINGS_VECTOR))));
    state.getSlashings().set(modulo(epoch, EPOCHS_PER_SLASHINGS_VECTOR), plus(state.getSlashings().get(modulo(epoch, EPOCHS_PER_SLASHINGS_VECTOR)), validator.getEffective_balance()));
    decrease_balance(state, slashed_index, divide(validator.getEffective_balance(), MIN_SLASHING_PENALTY_QUOTIENT_ALTAIR));
    var proposer_index = get_beacon_proposer_index(state);
    ValidatorIndex whistleblower_index_2;
    if (whistleblower_index == null) {
      var whistleblower_index_1 = proposer_index;
      whistleblower_index_2 = whistleblower_index_1;
    } else {
      whistleblower_index_2 = whistleblower_index;
    }
    var whistleblower_reward = new Gwei(divide(validator.getEffective_balance(), WHISTLEBLOWER_REWARD_QUOTIENT));
    var proposer_reward = new Gwei(divide(multiply(whistleblower_reward, PROPOSER_WEIGHT), WEIGHT_DENOMINATOR));
    increase_balance(state, proposer_index, proposer_reward);
    increase_balance(state, whistleblower_index_2, new Gwei(minus(whistleblower_reward, proposer_reward)));
  }

  public static void process_block(BeaconState state, BeaconBlock block) {
    process_block_header(state, block);
    process_randao(state, block.getBody());
    process_eth1_data(state, block.getBody());
    process_operations(state, block.getBody());
    process_sync_aggregate(state, block.getBody().getSync_aggregate());
  }

  public static void process_attestation(BeaconState state, Attestation attestation) {
    var data = attestation.getData();
    pyassert(contains(new Pair<>(get_previous_epoch(state), get_current_epoch(state)), data.getTarget().getEpoch()));
    pyassert(eq(data.getTarget().getEpoch(), compute_epoch_at_slot(data.getSlot())));
    pyassert(and(lessOrEqual(plus(data.getSlot(), MIN_ATTESTATION_INCLUSION_DELAY), state.getSlot()), lessOrEqual(state.getSlot(), plus(data.getSlot(), SLOTS_PER_EPOCH))));
    pyassert(less(data.getIndex(), get_committee_count_per_slot(state, data.getTarget().getEpoch())));
    var committee = get_beacon_committee(state, data.getSlot(), data.getIndex());
    pyassert(eq(len(attestation.getAggregation_bits()), len(committee)));
    var participation_flag_indices = get_attestation_participation_flag_indices(state, data, minus(state.getSlot(), data.getSlot()));
    pyassert(is_valid_indexed_attestation(state, get_indexed_attestation(state, attestation)));
    SSZList<ParticipationFlags> epoch_participation_2;
    if (eq(data.getTarget().getEpoch(), get_current_epoch(state)).v()) {
      var epoch_participation = state.getCurrent_epoch_participation();
      epoch_participation_2 = epoch_participation;
    } else {
      var epoch_participation_1 = state.getPrevious_epoch_participation();
      epoch_participation_2 = epoch_participation_1;
    }
    var proposer_reward_numerator = pyint.create(0L);
    var proposer_reward_numerator_2 = proposer_reward_numerator;
    for (var index: get_attesting_indices(state, data, attestation.getAggregation_bits())) {
      var proposer_reward_numerator_3 = proposer_reward_numerator_2;
      for (var tmp_0: enumerate(PARTICIPATION_FLAG_WEIGHTS)) {
        var flag_index = tmp_0.first;
        var weight = tmp_0.second;
        if (and(contains(participation_flag_indices, flag_index), not(has_flag(epoch_participation_2.get(index), flag_index))).v()) {
          epoch_participation_2.set(index, add_flag(epoch_participation_2.get(index), flag_index));
          var proposer_reward_numerator_1 = plus(proposer_reward_numerator_3, multiply(get_base_reward(state, index), weight));
          proposer_reward_numerator_3 = proposer_reward_numerator_1;
        } else {
          proposer_reward_numerator_3 = proposer_reward_numerator_3;
        }
      }
      proposer_reward_numerator_2 = proposer_reward_numerator_3;
    }
    var proposer_reward_denominator = divide(multiply(minus(WEIGHT_DENOMINATOR, PROPOSER_WEIGHT), WEIGHT_DENOMINATOR), PROPOSER_WEIGHT);
    var proposer_reward = new Gwei(divide(proposer_reward_numerator_2, proposer_reward_denominator));
    increase_balance(state, get_beacon_proposer_index(state), proposer_reward);
  }

  public static void process_deposit(BeaconState state, Deposit deposit) {
    pyassert(is_valid_merkle_branch(hash_tree_root(deposit.getData()), deposit.getProof(), plus(DEPOSIT_CONTRACT_TREE_DEPTH, pyint.create(1L)), state.getEth1_deposit_index(), state.getEth1_data().getDeposit_root()));
    state.setEth1_deposit_index(plus(state.getEth1_deposit_index(), pyint.create(1L)));
    var pubkey = deposit.getData().getPubkey();
    var amount = deposit.getData().getAmount();
    var validator_pubkeys = list(state.getValidators().map((validator) -> validator.getPubkey()));
    if (not(contains(validator_pubkeys, pubkey)).v()) {
      var deposit_message = new DepositMessage(deposit.getData().getPubkey(), deposit.getData().getWithdrawal_credentials(), deposit.getData().getAmount());
      var domain = compute_domain(DOMAIN_DEPOSIT, null, null);
      var signing_root = compute_signing_root(deposit_message, domain);
      if (bls.Verify(pubkey, signing_root, deposit.getData().getSignature()).v()) {
        state.getValidators().append(get_validator_from_deposit(state, deposit));
        state.getBalances().append(amount);
        state.getPrevious_epoch_participation().append(new ParticipationFlags(pyint.create(0L)));
        state.getCurrent_epoch_participation().append(new ParticipationFlags(pyint.create(0L)));
        state.getInactivity_scores().append(new uint64(pyint.create(0L)));
      }
    } else {
      var index = new ValidatorIndex(validator_pubkeys.index(pubkey));
      increase_balance(state, index, amount);
    }
  }

  public static void process_sync_aggregate(BeaconState state, SyncAggregate sync_aggregate) {
    var committee_pubkeys = state.getCurrent_sync_committee().getPubkeys();
    var participant_pubkeys = list(zip(committee_pubkeys, sync_aggregate.getSync_committee_bits()).filter((tmp_1) -> { var pubkey = tmp_1.first; var bit = tmp_1.second; return bit; }).map((tmp_2) -> { var pubkey = tmp_2.first; var bit = tmp_2.second; return pubkey; }));
    var previous_slot = minus(max(state.getSlot(), new Slot(pyint.create(1L))), new Slot(pyint.create(1L)));
    var domain = get_domain(state, DOMAIN_SYNC_COMMITTEE, compute_epoch_at_slot(previous_slot));
    var signing_root = compute_signing_root(get_block_root_at_slot(state, previous_slot), domain);
    pyassert(eth2_fast_aggregate_verify(participant_pubkeys, signing_root, sync_aggregate.getSync_committee_signature()));
    var total_active_increments = divide(get_total_active_balance(state), EFFECTIVE_BALANCE_INCREMENT);
    var total_base_rewards = new Gwei(multiply(get_base_reward_per_increment(state), total_active_increments));
    var max_participant_rewards = new Gwei(divide(divide(multiply(total_base_rewards, SYNC_REWARD_WEIGHT), WEIGHT_DENOMINATOR), SLOTS_PER_EPOCH));
    var participant_reward = new Gwei(divide(max_participant_rewards, SYNC_COMMITTEE_SIZE));
    var proposer_reward = new Gwei(divide(multiply(participant_reward, PROPOSER_WEIGHT), minus(WEIGHT_DENOMINATOR, PROPOSER_WEIGHT)));
    var all_pubkeys = list(state.getValidators().map((v) -> v.getPubkey()));
    var committee_indices = list(state.getCurrent_sync_committee().getPubkeys().map((pubkey) -> new ValidatorIndex(all_pubkeys.index(pubkey))));
    for (var tmp_3: zip(committee_indices, sync_aggregate.getSync_committee_bits())) {
    var participant_index = tmp_3.first;
    var participation_bit = tmp_3.second;
      if (pybool(participation_bit).v()) {
        increase_balance(state, participant_index, participant_reward);
        increase_balance(state, get_beacon_proposer_index(state), proposer_reward);
      } else {
        decrease_balance(state, participant_index, participant_reward);
      }
    }
  }

  public static void process_epoch(BeaconState state) {
    process_justification_and_finalization(state);
    process_inactivity_updates(state);
    process_rewards_and_penalties(state);
    process_registry_updates(state);
    process_slashings(state);
    process_eth1_data_reset(state);
    process_effective_balance_updates(state);
    process_slashings_reset(state);
    process_randao_mixes_reset(state);
    process_historical_roots_update(state);
    process_participation_flag_updates(state);
    process_sync_committee_updates(state);
  }

  public static void process_justification_and_finalization(BeaconState state) {
    if (lessOrEqual(get_current_epoch(state), plus(GENESIS_EPOCH, pyint.create(1L))).v()) {
      return;
    }
    var previous_indices = get_unslashed_participating_indices(state, TIMELY_TARGET_FLAG_INDEX, get_previous_epoch(state));
    var current_indices = get_unslashed_participating_indices(state, TIMELY_TARGET_FLAG_INDEX, get_current_epoch(state));
    var total_active_balance = get_total_active_balance(state);
    var previous_target_balance = get_total_balance(state, previous_indices);
    var current_target_balance = get_total_balance(state, current_indices);
    weigh_justification_and_finalization(state, total_active_balance, previous_target_balance, current_target_balance);
  }

  public static void process_inactivity_updates(BeaconState state) {
    if (eq(get_current_epoch(state), GENESIS_EPOCH).v()) {
      return;
    }
    for (var index: get_eligible_validator_indices(state)) {
      if (contains(get_unslashed_participating_indices(state, TIMELY_TARGET_FLAG_INDEX, get_previous_epoch(state)), index).v()) {
        state.getInactivity_scores().set(index, minus(state.getInactivity_scores().get(index), min(pyint.create(1L), state.getInactivity_scores().get(index))));
      } else {
        state.getInactivity_scores().set(index, plus(state.getInactivity_scores().get(index), INACTIVITY_SCORE_BIAS));
      }
      if (not(is_in_inactivity_leak(state)).v()) {
        state.getInactivity_scores().set(index, minus(state.getInactivity_scores().get(index), min(INACTIVITY_SCORE_RECOVERY_RATE, state.getInactivity_scores().get(index))));
      }
    }
  }

  public static void process_rewards_and_penalties(BeaconState state) {
    if (eq(get_current_epoch(state), GENESIS_EPOCH).v()) {
      return;
    }
    var flag_deltas = list(range(len(PARTICIPATION_FLAG_WEIGHTS)).map((flag_index) -> get_flag_index_deltas(state, flag_index)));
    var deltas = plus(flag_deltas, PyList.of(get_inactivity_penalty_deltas(state)));
    for (var tmp_4: deltas) {
    var rewards = tmp_4.first;
    var penalties = tmp_4.second;
      for (var index: range(len(state.getValidators()))) {
        increase_balance(state, new ValidatorIndex(index), rewards.get(index));
        decrease_balance(state, new ValidatorIndex(index), penalties.get(index));
      }
    }
  }

  public static void process_slashings(BeaconState state) {
    var epoch = get_current_epoch(state);
    var total_balance = get_total_active_balance(state);
    var adjusted_total_slashing_balance = min(multiply(sum(state.getSlashings()), PROPORTIONAL_SLASHING_MULTIPLIER_ALTAIR), total_balance);
    for (var tmp_5: enumerate(state.getValidators())) {
    var index = tmp_5.first;
    var validator = tmp_5.second;
      if (and(validator.getSlashed(), eq(plus(epoch, divide(EPOCHS_PER_SLASHINGS_VECTOR, pyint.create(2L))), validator.getWithdrawable_epoch())).v()) {
        var increment = EFFECTIVE_BALANCE_INCREMENT;
        var penalty_numerator = multiply(divide(validator.getEffective_balance(), increment), adjusted_total_slashing_balance);
        var penalty = multiply(divide(penalty_numerator, total_balance), increment);
        decrease_balance(state, new ValidatorIndex(index), penalty);
      }
    }
  }

  public static void process_participation_flag_updates(BeaconState state) {
    state.setPrevious_epoch_participation(state.getCurrent_epoch_participation());
    state.setCurrent_epoch_participation(new SSZList<ParticipationFlags>(list(range(len(state.getValidators())).map((_0) -> new ParticipationFlags(pyint.create(0L))))));
  }

  public static void process_sync_committee_updates(BeaconState state) {
    var next_epoch = plus(get_current_epoch(state), new Epoch(pyint.create(1L)));
    if (eq(modulo(next_epoch, EPOCHS_PER_SYNC_COMMITTEE_PERIOD), pyint.create(0L)).v()) {
      state.setCurrent_sync_committee(state.getNext_sync_committee());
      state.setNext_sync_committee(get_next_sync_committee(state));
    }
  }

  public static BeaconState initialize_beacon_state_from_eth1(Bytes32 eth1_block_hash, uint64 eth1_timestamp, Sequence<Deposit> deposits) {
    var fork = new Fork(GENESIS_FORK_VERSION, ALTAIR_FORK_VERSION, GENESIS_EPOCH);
    var state = new BeaconState(plus(eth1_timestamp, GENESIS_DELAY), BeaconState.genesis_validators_root_default, BeaconState.slot_default, fork, new BeaconBlockHeader(BeaconBlockHeader.slot_default, BeaconBlockHeader.proposer_index_default, BeaconBlockHeader.parent_root_default, BeaconBlockHeader.state_root_default, hash_tree_root(new BeaconBlockBody(BeaconBlockBody.randao_reveal_default, BeaconBlockBody.eth1_data_default, BeaconBlockBody.graffiti_default, BeaconBlockBody.proposer_slashings_default, BeaconBlockBody.attester_slashings_default, BeaconBlockBody.attestations_default, BeaconBlockBody.deposits_default, BeaconBlockBody.voluntary_exits_default, BeaconBlockBody.sync_aggregate_default))), BeaconState.block_roots_default, BeaconState.state_roots_default, BeaconState.historical_roots_default, new Eth1Data(Eth1Data.deposit_root_default, new uint64(len(deposits)), new Hash32(eth1_block_hash)), BeaconState.eth1_data_votes_default, BeaconState.eth1_deposit_index_default, BeaconState.validators_default, BeaconState.balances_default, new SSZVector<>(multiply(PyList.of(eth1_block_hash), EPOCHS_PER_HISTORICAL_VECTOR)), BeaconState.slashings_default, BeaconState.previous_epoch_participation_default, BeaconState.current_epoch_participation_default, BeaconState.justification_bits_default, BeaconState.previous_justified_checkpoint_default, BeaconState.current_justified_checkpoint_default, BeaconState.finalized_checkpoint_default, BeaconState.inactivity_scores_default, BeaconState.current_sync_committee_default, BeaconState.next_sync_committee_default);
    var leaves = list(map((deposit) -> deposit.getData(), deposits));
    for (var tmp_7: enumerate(deposits)) {
      var index = tmp_7.first;
      var deposit = tmp_7.second;
      var deposit_data_list = new SSZList<DepositData>(leaves.getSlice(null, plus(index, pyint.create(1L))));
      state.getEth1_data().setDeposit_root(hash_tree_root(deposit_data_list));
      process_deposit(state, deposit);
    }
    for (var tmp_8: enumerate(state.getValidators())) {
      var index_1 = tmp_8.first;
      var validator = tmp_8.second;
      var balance = state.getBalances().get(index_1);
      validator.setEffective_balance(min(minus(balance, modulo(balance, EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE));
      if (eq(validator.getEffective_balance(), MAX_EFFECTIVE_BALANCE).v()) {
        validator.setActivation_eligibility_epoch(GENESIS_EPOCH);
        validator.setActivation_epoch(GENESIS_EPOCH);
      }
    }
    state.setGenesis_validators_root(hash_tree_root(state.getValidators()));
    state.setCurrent_sync_committee(get_next_sync_committee(state));
    state.setNext_sync_committee(get_next_sync_committee(state));
    return state;
  }

  /*
      Return the aggregate public key for the public keys in ``pubkeys``.

      NOTE: the ``+`` operation should be interpreted as elliptic curve point addition, which takes as input
      elliptic curve points that must be decoded from the input ``BLSPubkey``s.
      This implementation is for demonstrative purposes only and ignores encoding/decoding concerns.
      Refer to the BLS signature draft standard for more information.
      */
  public static BLSPubkey eth2_aggregate_pubkeys(Sequence<BLSPubkey> pubkeys) {
    pyassert(greater(len(pubkeys), pyint.create(0L)));
    var result = copy(pubkeys.get(pyint.create(0L)));
    var result_2 = result;
    for (var pubkey: pubkeys.getSlice(pyint.create(1L), null)) {
      var result_1 = bls_plus(result_2, pubkey);
      result_2 = result_1;
    }
    return result_2;
  }

  /*
      Wrapper to ``bls.FastAggregateVerify`` accepting the ``G2_POINT_AT_INFINITY`` signature when ``pubkeys`` is empty.
      */
  public static pybool eth2_fast_aggregate_verify(Sequence<BLSPubkey> pubkeys, Bytes32 message, BLSSignature signature) {
    if (and(eq(len(pubkeys), pyint.create(0L)), eq(signature, G2_POINT_AT_INFINITY)).v()) {
      return pybool.create(true);
    }
    return bls.FastAggregateVerify(pubkeys, message, signature);
  }

  public static void translate_participation(BeaconState state, Sequence<beacon_java.phase0.data.PendingAttestation> pending_attestations) {
    for (var attestation: pending_attestations) {
      var data = attestation.getData();
      var inclusion_delay = attestation.getInclusion_delay();
      var participation_flag_indices = get_attestation_participation_flag_indices(state, data, inclusion_delay);
      var epoch_participation = state.getPrevious_epoch_participation();
      for (var index: get_attesting_indices(state, data, attestation.getAggregation_bits())) {
        for (var flag_index: participation_flag_indices) {
          epoch_participation.set(index, add_flag(epoch_participation.get(index), flag_index));
        }
      }
    }
  }

  public static BeaconState upgrade_to_altair(beacon_java.phase0.data.BeaconState pre) {
    var epoch = beacon_java.phase0.Spec.get_current_epoch(pre);
    var post = new BeaconState(pre.getGenesis_time(), pre.getGenesis_validators_root(), pre.getSlot(), new Fork(pre.getFork().getCurrent_version(), ALTAIR_FORK_VERSION, epoch), pre.getLatest_block_header(), pre.getBlock_roots(), pre.getState_roots(), pre.getHistorical_roots(), pre.getEth1_data(), pre.getEth1_data_votes(), pre.getEth1_deposit_index(), pre.getValidators(), pre.getBalances(), pre.getRandao_mixes(), pre.getSlashings(), new SSZList<>(list(range(len(pre.getValidators())).map((_0) -> new ParticipationFlags(pyint.create(0L))))), new SSZList<>(list(range(len(pre.getValidators())).map((_0) -> new ParticipationFlags(pyint.create(0L))))), pre.getJustification_bits(), pre.getPrevious_justified_checkpoint(), pre.getCurrent_justified_checkpoint(), pre.getFinalized_checkpoint(), new SSZList<>(list(range(len(pre.getValidators())).map((_0) -> new uint64(pyint.create(0L))))), BeaconState.current_sync_committee_default, BeaconState.next_sync_committee_default);
    translate_participation(post, pre.getPrevious_epoch_attestations());
    post.setCurrent_sync_committee(get_next_sync_committee(post));
    post.setNext_sync_committee(get_next_sync_committee(post));
    return post;
  }

  public static uint64 compute_sync_committee_period(Epoch epoch) {
    return divide(epoch, EPOCHS_PER_SYNC_COMMITTEE_PERIOD);
  }

  public static pybool is_assigned_to_sync_committee(BeaconState state, Epoch epoch, ValidatorIndex validator_index) {
    var sync_committee_period = compute_sync_committee_period(epoch);
    var current_epoch = get_current_epoch(state);
    var current_sync_committee_period = compute_sync_committee_period(current_epoch);
    var next_sync_committee_period = plus(current_sync_committee_period, pyint.create(1L));
    pyassert(contains(new Pair<>(current_sync_committee_period, next_sync_committee_period), sync_committee_period));
    var pubkey = state.getValidators().get(validator_index).getPubkey();
    if (eq(sync_committee_period, current_sync_committee_period).v()) {
      return contains(state.getCurrent_sync_committee().getPubkeys(), pubkey);
    } else {
      return contains(state.getNext_sync_committee().getPubkeys(), pubkey);
    }
  }

  public static void process_sync_committee_contributions(BeaconBlock block, Set<SyncCommitteeContribution> contributions) {
    var sync_aggregate = new SyncAggregate(SyncAggregate.sync_committee_bits_default, SyncAggregate.sync_committee_signature_default);
    var signatures = new PyList<BLSSignature>();
    var sync_subcommittee_size = divide(SYNC_COMMITTEE_SIZE, SYNC_COMMITTEE_SUBNET_COUNT);
    for (var contribution: contributions) {
      var subcommittee_index = contribution.getSubcommittee_index();
      for (var tmp_8: enumerate(contribution.getAggregation_bits())) {
      var index = tmp_8.first;
      var participated = tmp_8.second;
        if (pybool(participated).v()) {
          var participant_index = plus(multiply(sync_subcommittee_size, subcommittee_index), index);
          sync_aggregate.getSync_committee_bits().set(participant_index, new beacon_java.ssz.SSZBoolean(pybool.create(true)));
        }
      }
      signatures.append(contribution.getSignature());
    }
    sync_aggregate.setSync_committee_signature(new BLSSignature(bls.Aggregate(signatures)));
    block.getBody().setSync_aggregate(sync_aggregate);
  }

  public static SyncCommitteeMessage get_sync_committee_message(BeaconState state, Root block_root, ValidatorIndex validator_index, pyint privkey) {
    var epoch = get_current_epoch(state);
    var domain = get_domain(state, DOMAIN_SYNC_COMMITTEE, epoch);
    var signing_root = compute_signing_root(block_root, domain);
    var signature = bls.<BLSSignature>Sign(privkey, signing_root);
    return new SyncCommitteeMessage(state.getSlot(), block_root, validator_index, signature);
  }

  public static Set<uint64> compute_subnets_for_sync_committee(BeaconState state, ValidatorIndex validator_index) {
    var next_slot_epoch = compute_epoch_at_slot(new Slot(plus(state.getSlot(), pyint.create(1L))));
    SyncCommittee sync_committee_2;
    if (eq(compute_sync_committee_period(get_current_epoch(state)), compute_sync_committee_period(next_slot_epoch)).v()) {
      var sync_committee = state.getCurrent_sync_committee();
      sync_committee_2 = sync_committee;
    } else {
      var sync_committee_1 = state.getNext_sync_committee();
      sync_committee_2 = sync_committee_1;
    }
    var target_pubkey = state.getValidators().get(validator_index).getPubkey();
    var sync_committee_indices = list(enumerate(sync_committee_2.getPubkeys()).filter((tmp_9) -> { var index = tmp_9.first; var pubkey = tmp_9.second; return eq(pubkey, target_pubkey); }).map((tmp_10) -> { var index = tmp_10.first; var pubkey = tmp_10.second; return index; }));
    return set(list(sync_committee_indices.map((index) -> new uint64(divide(index, divide(SYNC_COMMITTEE_SIZE, SYNC_COMMITTEE_SUBNET_COUNT))))));
  }

  public static BLSSignature get_sync_committee_selection_proof(BeaconState state, Slot slot, uint64 subcommittee_index, pyint privkey) {
    var domain = get_domain(state, DOMAIN_SYNC_COMMITTEE_SELECTION_PROOF, compute_epoch_at_slot(slot));
    var signing_data = new SyncAggregatorSelectionData(slot, subcommittee_index);
    var signing_root = compute_signing_root(signing_data, domain);
    return bls.Sign(privkey, signing_root);
  }

  public static pybool is_sync_committee_aggregator(BLSSignature signature) {
    var modulo = max(pyint.create(1L), divide(divide(SYNC_COMMITTEE_SIZE, SYNC_COMMITTEE_SUBNET_COUNT), TARGET_AGGREGATORS_PER_SYNC_SUBCOMMITTEE));
    return eq(modulo(bytes_to_uint64(hash(signature).getSlice(pyint.create(0L), pyint.create(8L))), modulo), pyint.create(0L));
  }

  public static ContributionAndProof get_contribution_and_proof(BeaconState state, ValidatorIndex aggregator_index, SyncCommitteeContribution contribution, pyint privkey) {
    var selection_proof = get_sync_committee_selection_proof(state, contribution.getSlot(), contribution.getSubcommittee_index(), privkey);
    return new ContributionAndProof(aggregator_index, contribution, selection_proof);
  }

  public static BLSSignature get_contribution_and_proof_signature(BeaconState state, ContributionAndProof contribution_and_proof, pyint privkey) {
    var contribution = contribution_and_proof.getContribution();
    var domain = get_domain(state, DOMAIN_CONTRIBUTION_AND_PROOF, compute_epoch_at_slot(contribution.getSlot()));
    var signing_root = compute_signing_root(contribution_and_proof, domain);
    return bls.Sign(privkey, signing_root);
  }

  public static uint64 get_subtree_index(GeneralizedIndex generalized_index) {
    return new uint64(modulo(generalized_index, pow(pyint.create(2L), floorlog2(generalized_index))));
  }

  public static void validate_light_client_update(LightClientSnapshot snapshot, LightClientUpdate update, Root genesis_validators_root) {
    pyassert(greater(update.getHeader().getSlot(), snapshot.getHeader().getSlot()));
    var snapshot_period = divide(compute_epoch_at_slot(snapshot.getHeader().getSlot()), EPOCHS_PER_SYNC_COMMITTEE_PERIOD);
    var update_period = divide(compute_epoch_at_slot(update.getHeader().getSlot()), EPOCHS_PER_SYNC_COMMITTEE_PERIOD);
    pyassert(contains(new Pair<>(snapshot_period, plus(snapshot_period, pyint.create(1L))), update_period));
    BeaconBlockHeader signed_header_2;
    if (eq(update.getFinality_header(), new BeaconBlockHeader(BeaconBlockHeader.slot_default, BeaconBlockHeader.proposer_index_default, BeaconBlockHeader.parent_root_default, BeaconBlockHeader.state_root_default, BeaconBlockHeader.body_root_default)).v()) {
      var signed_header = update.getHeader();
      pyassert(eq(update.getFinality_branch(), list(range(floorlog2(FINALIZED_ROOT_INDEX)).map((_0) -> new Bytes32()))));
      signed_header_2 = signed_header;
    } else {
      var signed_header_1 = update.getFinality_header();
      pyassert(is_valid_merkle_branch(hash_tree_root(update.getHeader()), update.getFinality_branch(), floorlog2(FINALIZED_ROOT_INDEX), get_subtree_index(FINALIZED_ROOT_INDEX), update.getFinality_header().getState_root()));
      signed_header_2 = signed_header_1;
    }
    SyncCommittee sync_committee_2;
    if (eq(update_period, snapshot_period).v()) {
      var sync_committee = snapshot.getCurrent_sync_committee();
      pyassert(eq(update.getNext_sync_committee_branch(), list(range(floorlog2(NEXT_SYNC_COMMITTEE_INDEX)).map((_0) -> new Bytes32()))));
      sync_committee_2 = sync_committee;
    } else {
      var sync_committee_1 = snapshot.getNext_sync_committee();
      pyassert(is_valid_merkle_branch(hash_tree_root(update.getNext_sync_committee()), update.getNext_sync_committee_branch(), floorlog2(NEXT_SYNC_COMMITTEE_INDEX), get_subtree_index(NEXT_SYNC_COMMITTEE_INDEX), update.getHeader().getState_root()));
      sync_committee_2 = sync_committee_1;
    }
    pyassert(greaterOrEqual(sum(update.getSync_committee_bits()), MIN_SYNC_COMMITTEE_PARTICIPANTS));
    var participant_pubkeys = list(zip(update.getSync_committee_bits(), sync_committee_2.getPubkeys()).filter((tmp_11) -> { var bit = tmp_11.first; var pubkey = tmp_11.second; return bit; }).map((tmp_12) -> { var bit = tmp_12.first; var pubkey = tmp_12.second; return pubkey; }));
    var domain = compute_domain(DOMAIN_SYNC_COMMITTEE, update.getFork_version(), genesis_validators_root);
    var signing_root = compute_signing_root(signed_header_2, domain);
    pyassert(bls.FastAggregateVerify(participant_pubkeys, signing_root, update.getSync_committee_signature()));
  }

  public static void apply_light_client_update(LightClientSnapshot snapshot, LightClientUpdate update) {
    var snapshot_period = divide(compute_epoch_at_slot(snapshot.getHeader().getSlot()), EPOCHS_PER_SYNC_COMMITTEE_PERIOD);
    var update_period = divide(compute_epoch_at_slot(update.getHeader().getSlot()), EPOCHS_PER_SYNC_COMMITTEE_PERIOD);
    if (eq(update_period, plus(snapshot_period, pyint.create(1L))).v()) {
      snapshot.setCurrent_sync_committee(snapshot.getNext_sync_committee());
      snapshot.setNext_sync_committee(update.getNext_sync_committee());
    }
    snapshot.setHeader(update.getHeader());
  }

  public static void process_light_client_update(LightClientStore store, LightClientUpdate update, Slot current_slot, Root genesis_validators_root) {
    validate_light_client_update(store.getSnapshot(), update, genesis_validators_root);
    store.getValid_updates().add(update);
    var update_timeout = multiply(SLOTS_PER_EPOCH, EPOCHS_PER_SYNC_COMMITTEE_PERIOD);
    if (and(greaterOrEqual(multiply(sum(update.getSync_committee_bits()), pyint.create(3L)), multiply(len(update.getSync_committee_bits()), pyint.create(2L))), not(eq(update.getFinality_header(), new BeaconBlockHeader(BeaconBlockHeader.slot_default, BeaconBlockHeader.proposer_index_default, BeaconBlockHeader.parent_root_default, BeaconBlockHeader.state_root_default, BeaconBlockHeader.body_root_default)))).v()) {
      apply_light_client_update(store.getSnapshot(), update);
      store.setValid_updates(new Set<LightClientUpdate>());
    } else {
      if (greater(current_slot, plus(store.getSnapshot().getHeader().getSlot(), update_timeout)).v()) {
        apply_light_client_update(store.getSnapshot(), max(store.getValid_updates(), (update_) -> sum(update_.getSync_committee_bits())));
        store.setValid_updates(new Set<LightClientUpdate>());
      }
    }
  }

  /*
      Check if ``validator`` is eligible for activation.
      */
  public static pybool is_eligible_for_activation(BeaconState state, Validator validator) {
    return and(lessOrEqual(validator.getActivation_eligibility_epoch(), state.getFinalized_checkpoint().getEpoch()), eq(validator.getActivation_epoch(), FAR_FUTURE_EPOCH));
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
      Return the current epoch.
      */
  public static Epoch get_current_epoch(BeaconState state) {
    return compute_epoch_at_slot(state.getSlot());
  }

  /*  `
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
    return list(enumerate(state.getValidators()).filter((tmp_13) -> { var i = tmp_13.first; var v = tmp_13.second; return is_active_validator(v, epoch); }).map((tmp_14) -> { var i = tmp_14.first; var v = tmp_14.second; return new ValidatorIndex(i); }));
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
    return set(enumerate(committee).filter((tmp_16) -> { var i = tmp_16.first; var index = tmp_16.second; return bits.get(i); }).map((tmp_17) -> { var i = tmp_17.first; var index = tmp_17.second; return index; }));
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

  public static void weigh_justification_and_finalization(BeaconState state, Gwei total_active_balance, Gwei previous_epoch_target_balance, Gwei current_epoch_target_balance) {
    var previous_epoch = get_previous_epoch(state);
    var current_epoch = get_current_epoch(state);
    var old_previous_justified_checkpoint = state.getPrevious_justified_checkpoint();
    var old_current_justified_checkpoint = state.getCurrent_justified_checkpoint();
    state.setPrevious_justified_checkpoint(state.getCurrent_justified_checkpoint());
    state.getJustification_bits().setSlice(pyint.create(1L), null, state.getJustification_bits().getSlice(null, minus(JUSTIFICATION_BITS_LENGTH, pyint.create(1L))));
    state.getJustification_bits().set(pyint.create(0L), new beacon_java.ssz.SSZBoolean(pyint.create(0L)));
    if (greaterOrEqual(multiply(previous_epoch_target_balance, pyint.create(3L)), multiply(total_active_balance, pyint.create(2L))).v()) {
      state.setCurrent_justified_checkpoint(new Checkpoint(previous_epoch, get_block_root(state, previous_epoch)));
      state.getJustification_bits().set(pyint.create(1L), new beacon_java.ssz.SSZBoolean(pyint.create(1L)));
    }
    if (greaterOrEqual(multiply(current_epoch_target_balance, pyint.create(3L)), multiply(total_active_balance, pyint.create(2L))).v()) {
      state.setCurrent_justified_checkpoint(new Checkpoint(current_epoch, get_block_root(state, current_epoch)));
      state.getJustification_bits().set(pyint.create(0L), new beacon_java.ssz.SSZBoolean(pyint.create(1L)));
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

  public static uint64 get_finality_delay(BeaconState state) {
    return minus(get_previous_epoch(state), state.getFinalized_checkpoint().getEpoch());
  }

  public static pybool is_in_inactivity_leak(BeaconState state) {
    return greater(get_finality_delay(state), MIN_EPOCHS_TO_INACTIVITY_PENALTY);
  }

  public static Sequence<ValidatorIndex> get_eligible_validator_indices(BeaconState state) {
    var previous_epoch = get_previous_epoch(state);
    return list(enumerate(state.getValidators()).filter((tmp_17) -> { var index = tmp_17.first; var v = tmp_17.second; return or(is_active_validator(v, previous_epoch), and(v.getSlashed(), less(plus(previous_epoch, pyint.create(1L)), v.getWithdrawable_epoch()))); }).map((tmp_18) -> { var index = tmp_18.first; var v = tmp_18.second; return new ValidatorIndex(index); }));
  }

  public static void process_registry_updates(BeaconState state) {
    for (var tmp_19: enumerate(state.getValidators())) {
    var index = tmp_19.first;
    var validator = tmp_19.second;
      if (is_eligible_for_activation_queue(validator).v()) {
        validator.setActivation_eligibility_epoch(plus(get_current_epoch(state), pyint.create(1L)));
      }
      if (and(is_active_validator(validator, get_current_epoch(state)), lessOrEqual(validator.getEffective_balance(), EJECTION_BALANCE)).v()) {
        initiate_validator_exit(state, new ValidatorIndex(index));
      }
    }
    var activation_queue = sorted(list(enumerate(state.getValidators()).filter((tmp_20) -> { var index = tmp_20.first; var validator = tmp_20.second; return is_eligible_for_activation(state, validator); }).map((tmp_21) -> { var index = tmp_21.first; var validator = tmp_21.second; return index; })), (index) -> new Pair<>(state.getValidators().get(index).getActivation_eligibility_epoch(), index));
    for (var index_1: activation_queue.getSlice(null, get_validator_churn_limit(state))) {
      var validator_1 = state.getValidators().get(index_1);
      validator_1.setActivation_epoch(compute_activation_exit_epoch(get_current_epoch(state)));
    }
  }

  public static void process_eth1_data_reset(BeaconState state) {
    var next_epoch = new Epoch(plus(get_current_epoch(state), pyint.create(1L)));
    if (eq(modulo(next_epoch, EPOCHS_PER_ETH1_VOTING_PERIOD), pyint.create(0L)).v()) {
      state.setEth1_data_votes(new SSZList<Eth1Data>());
    }
  }

  public static void process_effective_balance_updates(BeaconState state) {
    for (var tmp_22: enumerate(state.getValidators())) {
    var index = tmp_22.first;
    var validator = tmp_22.second;
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

  public static Validator get_validator_from_deposit(BeaconState state, Deposit deposit) {
    var amount = deposit.getData().getAmount();
    var effective_balance = min(minus(amount, modulo(amount, EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE);
    return new Validator(deposit.getData().getPubkey(), deposit.getData().getWithdrawal_credentials(), effective_balance, Validator.slashed_default, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH);
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

}
