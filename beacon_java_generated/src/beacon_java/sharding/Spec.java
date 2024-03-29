package beacon_java.sharding;

import beacon_java.merge.Utils;
import beacon_java.merge.data.ExecutionPayload;
import beacon_java.merge.data.ExecutionPayloadHeader;
import beacon_java.phase0.data.*;
import beacon_java.phase0.data.PendingAttestation;
import beacon_java.sharding.data.*;
import beacon_java.pylib.*;
import beacon_java.sharding.data.AttestationData;
import beacon_java.sharding.data.BeaconBlockBody;
import beacon_java.sharding.data.BeaconState;
import beacon_java.ssz.*;

import static beacon_java.deps.BLS.bls;
import static beacon_java.merge.Spec.is_execution_enabled;
import static beacon_java.merge.Spec.process_execution_payload;
import static beacon_java.merge.Utils.EXECUTION_ENGINE;
import static beacon_java.merge.Utils.verify_execution_state_transition;
import static beacon_java.phase0.Constants.*;
import static beacon_java.phase0.Spec.*;
import static beacon_java.phase0.Utils.hash_tree_root;
import static beacon_java.pylib.Exports.*;
import static beacon_java.sharding.Constants.*;

public class Spec {
  public static pyint next_power_of_two(pyint x) {
    return pow(pyint.create(2L), minus(x, pyint.create(1L)).bit_length());
  }

  public static Slot compute_previous_slot(Slot slot) {
    if (greater(slot, pyint.create(0L)).v()) {
      return new Slot(minus(slot, pyint.create(1L)));
    } else {
      return new Slot(pyint.create(0L));
    }
  }

  public static Gwei compute_updated_gasprice(Gwei prev_gasprice, uint64 shard_block_length, uint64 adjustment_quotient) {
    if (greater(shard_block_length, TARGET_SAMPLES_PER_BLOCK).v()) {
      var delta = max(pyint.create(1L), divide(divide(multiply(prev_gasprice, minus(shard_block_length, TARGET_SAMPLES_PER_BLOCK)), TARGET_SAMPLES_PER_BLOCK), adjustment_quotient));
      return min(plus(prev_gasprice, delta), MAX_GASPRICE);
    } else {
      var delta_1 = max(pyint.create(1L), divide(divide(multiply(prev_gasprice, minus(TARGET_SAMPLES_PER_BLOCK, shard_block_length)), TARGET_SAMPLES_PER_BLOCK), adjustment_quotient));
      return minus(max(prev_gasprice, plus(MIN_GASPRICE, delta_1)), delta_1);
    }
  }

  /*
      Return the source epoch for computing the committee.
      */
  public static Epoch compute_committee_source_epoch(Epoch epoch, uint64 period) {
    var source_epoch = new Epoch(minus(epoch, modulo(epoch, period)));
    Epoch source_epoch_2;
    if (greaterOrEqual(source_epoch, period).v()) {
      var source_epoch_1 = minus(source_epoch, period);
      source_epoch_2 = source_epoch_1;
    } else {
      source_epoch_2 = source_epoch;
    }
    return source_epoch_2;
  }

  /*
      Return the number of committees in each slot for the given ``epoch``.
      */
  public static uint64 get_committee_count_per_slot(BeaconState state, Epoch epoch) {
    return max(new uint64(pyint.create(1L)), min(get_active_shard_count(state, epoch), divide(divide(new uint64(len(get_active_validator_indices(state, epoch))), SLOTS_PER_EPOCH), TARGET_COMMITTEE_SIZE)));
  }

  /*
      Return the number of active shards.
      Note that this puts an upper bound on the number of committees per slot.
      */
  public static uint64 get_active_shard_count(BeaconState state, Epoch epoch) {
    return INITIAL_ACTIVE_SHARDS;
  }

  /*
      Return the shard committee of the given ``epoch`` of the given ``shard``.
      */
  public static Sequence<ValidatorIndex> get_shard_committee(BeaconState beacon_state, Epoch epoch, Shard shard) {
    var source_epoch = compute_committee_source_epoch(epoch, SHARD_COMMITTEE_PERIOD);
    var active_validator_indices = get_active_validator_indices(beacon_state, source_epoch);
    var seed = get_seed(beacon_state, source_epoch, DOMAIN_SHARD_COMMITTEE);
    return compute_committee(active_validator_indices, seed, shard, get_active_shard_count(beacon_state, epoch));
  }

  /*
      Return from ``indices`` a random index sampled by effective balance.
      */
  public static ValidatorIndex compute_proposer_index(BeaconState beacon_state, Sequence<ValidatorIndex> indices, Bytes32 seed, Gwei min_effective_balance) {
    pyassert(greater(len(indices), pyint.create(0L)));
    var MAX_RANDOM_BYTE = minus(pow(pyint.create(2L), pyint.create(8L)), pyint.create(1L));
    var i = new uint64(pyint.create(0L));
    var total = new uint64(len(indices));
    var i_2 = i;
    while (true) {
      var candidate_index = indices.get(compute_shuffled_index(modulo(i_2, total), total, seed));
      var random_byte = hash(plus(seed, uint_to_bytes(new uint64(divide(i_2, pyint.create(32L)))))).get(modulo(i_2, pyint.create(32L)));
      var effective_balance = beacon_state.getValidators().get(candidate_index).getEffective_balance();
      if (lessOrEqual(effective_balance, min_effective_balance).v()) {
        i_2 = i_2;
        continue;
      }
      if (greaterOrEqual(multiply(effective_balance, MAX_RANDOM_BYTE), multiply(MAX_EFFECTIVE_BALANCE, random_byte)).v()) {
        return candidate_index;
      }
      var i_1 = plus(i_2, pyint.create(1L));
      i_2 = i_1;
    }
  }

  /*
      Return the proposer's index of shard block at ``slot``.
      */
  public static ValidatorIndex get_shard_proposer_index(BeaconState beacon_state, Slot slot, Shard shard) {
    var epoch = compute_epoch_at_slot(slot);
    var committee = get_shard_committee(beacon_state, epoch, shard);
    var seed = hash(plus(get_seed(beacon_state, epoch, DOMAIN_SHARD_PROPOSER), uint_to_bytes(slot)));
    var EFFECTIVE_BALANCE_MAX_DOWNWARD_DEVIATION = minus(EFFECTIVE_BALANCE_INCREMENT, divide(multiply(EFFECTIVE_BALANCE_INCREMENT, HYSTERESIS_DOWNWARD_MULTIPLIER), HYSTERESIS_QUOTIENT));
    var min_effective_balance = plus(divide(multiply(beacon_state.getShard_gasprice(), MAX_SAMPLES_PER_BLOCK), TARGET_SAMPLES_PER_BLOCK), EFFECTIVE_BALANCE_MAX_DOWNWARD_DEVIATION);
    return compute_proposer_index(beacon_state, committee, seed, min_effective_balance);
  }

  /*
      Return the start shard at ``slot``.
      */
  public static Shard get_start_shard(BeaconState state, Slot slot) {
    var current_epoch_start_slot = compute_start_slot_at_epoch(get_current_epoch(state));
    var shard = state.getCurrent_epoch_start_shard();
    Shard shard_3;
    if (greater(slot, current_epoch_start_slot).v()) {
      var shard_5 = shard;
      for (var _slot : range(current_epoch_start_slot, slot)) {
        var committee_count = get_committee_count_per_slot(state, compute_epoch_at_slot(new Slot(_slot)));
        var active_shard_count = get_active_shard_count(state, compute_epoch_at_slot(new Slot(_slot)));
        var shard_1 = modulo(plus(shard_5, committee_count), active_shard_count);
        shard_5 = shard_1;
      }
      shard_3 = shard_5;
    } else {
      if (less(slot, current_epoch_start_slot).v()) {
        var shard_4 = shard;
        for (var _slot_1 : list(range(slot, current_epoch_start_slot)).getSlice(null, null, uminus(pyint.create(1L)))) {
          var committee_count_1 = get_committee_count_per_slot(state, compute_epoch_at_slot(new Slot(_slot_1)));
          var active_shard_count_1 = get_active_shard_count(state, compute_epoch_at_slot(new Slot(_slot_1)));
          var shard_2 = modulo(minus(plus(shard_4, active_shard_count_1), committee_count_1), active_shard_count_1);
          shard_4 = shard_2;
        }
        shard_3 = shard_4;
      } else {
        shard_3 = shard;
      }
    }
    return new Shard(shard_3);
  }

  public static Shard compute_shard_from_committee_index(BeaconState state, Slot slot, CommitteeIndex index) {
    var active_shards = get_active_shard_count(state, compute_epoch_at_slot(slot));
    pyassert(less(index, active_shards));
    return new Shard(modulo(plus(index, get_start_shard(state, slot)), active_shards));
  }

  public static CommitteeIndex compute_committee_index_from_shard(BeaconState state, Slot slot, Shard shard) {
    var epoch = compute_epoch_at_slot(slot);
    var active_shards = get_active_shard_count(state, epoch);
    var index = new CommitteeIndex(modulo(minus(plus(active_shards, shard), get_start_shard(state, slot)), active_shards));
    pyassert(less(index, get_committee_count_per_slot(state, epoch)));
    return index;
  }

  public static void process_block(BeaconState state, BeaconBlock block) {
    process_block_header(state, block);
    BeaconBlockBody body = (BeaconBlockBody) block.getBody();
    process_randao(state, body);
    process_eth1_data(state, body);
    process_operations(state, body);
    if (is_execution_enabled(state, block).v()) {
      process_execution_payload(state, body.getExecution_payload(), EXECUTION_ENGINE);
    }
  }

  public static void process_operations(BeaconState state, BeaconBlockBody body) {
    pyassert(eq(len(body.getDeposits()), min(MAX_DEPOSITS, minus(state.getEth1_data().getDeposit_count(), state.getEth1_deposit_index()))));
    for (var operation : body.getProposer_slashings()) {
      process_proposer_slashing(state, operation);
    }
    for (var operation_1 : body.getAttester_slashings()) {
      process_attester_slashing(state, operation_1);
    }
    for (var operation_2 : body.getShard_proposer_slashings()) {
      process_shard_proposer_slashing(state, operation_2);
    }
    pyassert(lessOrEqual(len(body.getShard_headers()), multiply(MAX_SHARD_HEADERS_PER_SHARD, get_active_shard_count(state, get_current_epoch(state)))));
    for (var operation_3 : body.getShard_headers()) {
      process_shard_header(state, operation_3);
    }
    for (var operation_4 : body.getAttestations()) {
      process_attestation(state, operation_4);
    }
    for (var operation_5 : body.getDeposits()) {
      process_deposit(state, operation_5);
    }
    for (var operation_6 : body.getVoluntary_exits()) {
      process_voluntary_exit(state, operation_6);
    }
  }

  public static void process_attestation(BeaconState state, beacon_java.phase0.data.Attestation attestation) {
    beacon_java.phase0.Spec.process_attestation(state, attestation);
    update_pending_shard_work(state, attestation);
  }

  public static void update_pending_shard_work(BeaconState state, beacon_java.phase0.data.Attestation attestation) {
    var attestation_shard = compute_shard_from_committee_index(state, attestation.getData().getSlot(), attestation.getData().getIndex());
    var buffer_index = modulo(attestation.getData().getSlot(), SHARD_STATE_MEMORY_SLOTS);
    var committee_work = state.getShard_buffer().get(buffer_index).get(attestation_shard);
    if (not(eq(committee_work.getStatus().getSelector(), SHARD_WORK_PENDING)).v()) {
      return;
    }
    Sequence<PendingShardHeader> current_headers = ((ShardWorkStatus_2)committee_work.getStatus()).getValue();
    AttestationData attestation_data = (AttestationData) attestation.getData();
    var header_index = list(current_headers.map((header) -> header.getRoot())).index(attestation_data.getShard_header_root());
    PendingShardHeader pending_header = current_headers.get(header_index);
    var full_committee = get_beacon_committee(state, attestation.getData().getSlot(), attestation.getData().getIndex());
    if (and(not(eq(pending_header.getWeight(), pyint.create(0L))), less(compute_epoch_at_slot(pending_header.getUpdate_slot()), get_current_epoch(state))).v()) {
      pending_header.setWeight(sum(zip(full_committee, pending_header.getVotes()).filter((tmp_0) -> {
        var index = tmp_0.first;
        var bit = tmp_0.second;
        return bit;
      }).map((tmp_1) -> {
        var index = tmp_1.first;
        var bit = tmp_1.second;
        return state.getValidators().get(index).getEffective_balance();
      })));
    }
    pending_header.setUpdate_slot(state.getSlot());
    var full_committee_balance = new Gwei(pyint.create(0L));
    var full_committee_balance_2 = full_committee_balance;
    for (var tmp_2 : enumerate(attestation.getAggregation_bits())) {
      var i = tmp_2.first;
      var bit = tmp_2.second;
      var weight = state.getValidators().get(full_committee.get(i)).getEffective_balance();
      var full_committee_balance_1 = plus(full_committee_balance_2, weight);
      if (pybool(bit).v()) {
        if (not(pending_header.getVotes().get(i)).v()) {
          pending_header.setWeight(plus(pending_header.getWeight(), weight));
          pending_header.getVotes().set(i, new beacon_java.ssz.SSZBoolean(pybool.create(true)));
          full_committee_balance_2 = full_committee_balance_1;
        } else {
          full_committee_balance_2 = full_committee_balance_1;
        }
      } else {
        full_committee_balance_2 = full_committee_balance_1;
      }
    }
    if (greaterOrEqual(multiply(pending_header.getWeight(), pyint.create(3L)), multiply(full_committee_balance_2, pyint.create(2L))).v()) {
      if (eq(pending_header.getCommitment(), new DataCommitment(DataCommitment.point_default, DataCommitment.length_default)).v()) {
        state.getShard_buffer().get(buffer_index).get(attestation_shard).setStatus(new ShardWorkStatus_0(ShardWorkStatus_0.value_default));
      } else {
        state.getShard_buffer().get(buffer_index).get(attestation_shard).setStatus(new ShardWorkStatus_1(pending_header.getCommitment()));
      }
    }
  }

  public static void process_shard_header(BeaconState state, SignedShardBlobHeader signed_header) {
    var header = signed_header.getMessage();
    pyassert(and(less(new Slot(pyint.create(0L)), header.getSlot()), lessOrEqual(header.getSlot(), state.getSlot())));
    var header_epoch = compute_epoch_at_slot(header.getSlot());
    pyassert(contains(PyList.of(get_previous_epoch(state), get_current_epoch(state)), header_epoch));
    pyassert(less(header.getShard(), get_active_shard_count(state, header_epoch)));
    pyassert(eq(header.getBody_summary().getBeacon_block_root(), get_block_root_at_slot(state, minus(header.getSlot(), pyint.create(1L)))));
    var committee_work = state.getShard_buffer().get(modulo(header.getSlot(), SHARD_STATE_MEMORY_SLOTS)).get(header.getShard());
    pyassert(eq(committee_work.getStatus().getSelector(), SHARD_WORK_PENDING));
    ShardWorkStatus_2 pending_headers_status = (ShardWorkStatus_2) committee_work.getStatus();
    SSZList<PendingShardHeader> current_headers = pending_headers_status.getValue();
    var header_root = hash_tree_root(header);
    pyassert(not(contains(list(current_headers.map((pending_header) -> pending_header.getRoot())), header_root)));
    pyassert(eq(header.getProposer_index(), get_shard_proposer_index(state, header.getSlot(), header.getShard())));
    var signing_root = compute_signing_root(header, get_domain(state, DOMAIN_SHARD_PROPOSER, null));
    pyassert(bls.Verify(state.getValidators().get(header.getProposer_index()).getPubkey(), signing_root, signed_header.getSignature()));
    var body_summary = header.getBody_summary();
    if (eq(body_summary.getCommitment().getLength(), pyint.create(0L)).v()) {
      pyassert(eq(body_summary.getDegree_proof(), G1_SETUP.get(pyint.create(0L))));
    }
    pyassert(eq(bls.Pairing(body_summary.getDegree_proof(), G2_SETUP.get(pyint.create(0L))), bls.Pairing(body_summary.getCommitment().getPoint(), G2_SETUP.get(uminus(body_summary.getCommitment().getLength())))));
    var index = compute_committee_index_from_shard(state, header.getSlot(), header.getShard());
    var committee_length = len(get_beacon_committee(state, header.getSlot(), index));
    var initial_votes = new SSZBitlist(multiply(PyList.of(pyint.create(0L)), committee_length));
    var pending_header = new PendingShardHeader(body_summary.getCommitment(), header_root, initial_votes, new Gwei(pyint.create(0L)), state.getSlot());
    current_headers.append(pending_header);
  }

  public static void process_shard_proposer_slashing(BeaconState state, ShardProposerSlashing proposer_slashing) {
    var reference_1 = proposer_slashing.getSigned_reference_1().getMessage();
    var reference_2 = proposer_slashing.getSigned_reference_2().getMessage();
    pyassert(eq(reference_1.getSlot(), reference_2.getSlot()));
    pyassert(eq(reference_1.getShard(), reference_2.getShard()));
    pyassert(eq(reference_1.getProposer_index(), reference_2.getProposer_index()));
    pyassert(not(eq(reference_1, reference_2)));
    var proposer = state.getValidators().get(reference_1.getProposer_index());
    pyassert(is_slashable_validator(proposer, get_current_epoch(state)));
    for (var signed_header : Pair.of(proposer_slashing.getSigned_reference_1(), proposer_slashing.getSigned_reference_2())) {
      var domain = get_domain(state, DOMAIN_SHARD_PROPOSER, compute_epoch_at_slot(signed_header.getMessage().getSlot()));
      var signing_root = compute_signing_root(signed_header.getMessage(), domain);
      pyassert(bls.Verify(proposer.getPubkey(), signing_root, signed_header.getSignature()));
    }
    slash_validator(state, reference_1.getProposer_index(), null);
  }

  public static void process_epoch(BeaconState state) {
    process_pending_shard_confirmations(state);
    charge_confirmed_shard_fees(state);
    reset_pending_shard_work(state);
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
    process_shard_epoch_increment(state);
  }

  public static void process_pending_shard_confirmations(BeaconState state) {
    if (eq(get_current_epoch(state), GENESIS_EPOCH).v()) {
      return;
    }
    var previous_epoch = get_previous_epoch(state);
    var previous_epoch_start_slot = compute_start_slot_at_epoch(previous_epoch);
    for (var slot : range(previous_epoch_start_slot, plus(previous_epoch_start_slot, SLOTS_PER_EPOCH))) {
      var buffer_index = modulo(slot, SHARD_STATE_MEMORY_SLOTS);
      for (var shard_index : range(len(state.getShard_buffer().get(buffer_index)))) {
        var committee_work = state.getShard_buffer().get(buffer_index).get(shard_index);
        if (eq(committee_work.getStatus().getSelector(), SHARD_WORK_PENDING).v()) {
          ShardWorkStatus_2 pending_headers_status = (ShardWorkStatus_2) committee_work.getStatus();
          var winning_header = max(pending_headers_status.getValue(), (header) -> header.getWeight());
          if (eq(winning_header.getCommitment(), new DataCommitment(DataCommitment.point_default, DataCommitment.length_default)).v()) {
            committee_work.setStatus(new ShardWorkStatus_0(ShardWorkStatus_0.value_default));
          } else {
            committee_work.setStatus(new ShardWorkStatus_1(winning_header.getCommitment()));
          }
        }
      }
    }
  }

  public static void charge_confirmed_shard_fees(BeaconState state) {
    var new_gasprice = state.getShard_gasprice();
    var previous_epoch = get_previous_epoch(state);
    var previous_epoch_start_slot = compute_start_slot_at_epoch(previous_epoch);
    var adjustment_quotient = multiply(multiply(get_active_shard_count(state, previous_epoch), SLOTS_PER_EPOCH), GASPRICE_ADJUSTMENT_COEFFICIENT);
    var new_gasprice_2 = new_gasprice;
    for (var slot : range(previous_epoch_start_slot, plus(previous_epoch_start_slot, SLOTS_PER_EPOCH))) {
      var buffer_index = modulo(slot, SHARD_STATE_MEMORY_SLOTS);
      var new_gasprice_3 = new_gasprice_2;
      for (var shard_index : range(len(state.getShard_buffer().get(buffer_index)))) {
        var committee_work = state.getShard_buffer().get(buffer_index).get(shard_index);
        if (eq(committee_work.getStatus().getSelector(), SHARD_WORK_CONFIRMED).v()) {
          DataCommitment commitment = ((ShardWorkStatus_1) committee_work.getStatus()).getValue();
          var proposer = get_shard_proposer_index(state, slot, new Shard(shard_index));
          var fee = divide(multiply(state.getShard_gasprice(), commitment.getLength()), TARGET_SAMPLES_PER_BLOCK);
          decrease_balance(state, proposer, fee);
          var new_gasprice_1 = compute_updated_gasprice(new_gasprice_3, commitment.getLength(), adjustment_quotient);
          new_gasprice_3 = new_gasprice_1;
        } else {
          new_gasprice_3 = new_gasprice_3;
        }
      }
      new_gasprice_2 = new_gasprice_3;
    }
    state.setShard_gasprice(new_gasprice_2);
  }

  public static void reset_pending_shard_work(BeaconState state) {
    var next_epoch = plus(get_current_epoch(state), pyint.create(1L));
    var next_epoch_start_slot = compute_start_slot_at_epoch(next_epoch);
    var committees_per_slot = get_committee_count_per_slot(state, next_epoch);
    var active_shards = get_active_shard_count(state, next_epoch);
    for (var slot : range(next_epoch_start_slot, plus(next_epoch_start_slot, SLOTS_PER_EPOCH))) {
      var buffer_index = modulo(slot, SHARD_STATE_MEMORY_SLOTS);
      state.getShard_buffer().set(buffer_index, new SSZList<ShardWork>(list(range(active_shards).map((_0) -> new ShardWork(ShardWork.status_default)))));
      var start_shard = get_start_shard(state, slot);
      for (var committee_index : range(committees_per_slot)) {
        var shard = modulo(plus(start_shard, committee_index), active_shards);
        var committee_length = len(get_beacon_committee(state, slot, new CommitteeIndex(committee_index)));
        state.getShard_buffer().get(buffer_index).get(shard).setStatus(new ShardWorkStatus_2(SSZList.of(new PendingShardHeader(new DataCommitment(DataCommitment.point_default, DataCommitment.length_default), new Root(), new SSZBitlist(multiply(PyList.of(pyint.create(0L)), committee_length)), new Gwei(pyint.create(0L)), slot))));
      }
    }
  }

  public static void process_shard_epoch_increment(BeaconState state) {
    state.setCurrent_epoch_start_shard(get_start_shard(state, new Slot(plus(state.getSlot(), pyint.create(1L)))));
  }
}