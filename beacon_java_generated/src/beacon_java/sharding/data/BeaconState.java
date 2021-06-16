package beacon_java.sharding.data;

import beacon_java.phase0.data.Gwei;
import beacon_java.phase0.data.PendingAttestation;
import lombok.*;
import beacon_java.pylib.*;
import beacon_java.sharding.data.DataCommitment;
import beacon_java.ssz.SSZList;
import static beacon_java.phase0.Constants.MAX_ATTESTATIONS;
import static beacon_java.sharding.Constants.MAX_SHARDS;
import static beacon_java.sharding.Constants.MAX_SHARD_HEADERS_PER_SHARD;
import beacon_java.sharding.data.PendingShardHeader;
import static beacon_java.phase0.Constants.SLOTS_PER_EPOCH;
import beacon_java.sharding.data.Shard;
import beacon_java.ssz.SSZVector;
import beacon_java.ssz.uint64;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconState extends beacon_java.merge.data.BeaconState {
  public static SSZList<PendingAttestation> previous_epoch_attestations_default = new SSZList<PendingAttestation>();
  public static SSZList<PendingAttestation> current_epoch_attestations_default = new SSZList<PendingAttestation>();
  public static SSZVector<SSZList<ShardWork>> shard_buffer_default = new SSZVector<SSZList<ShardWork>>();
  public static Gwei shard_gasprice_default = new Gwei(pyint.create(0));
  public static Shard current_epoch_start_shard_default = new Shard();
  public SSZList<PendingAttestation> previous_epoch_attestations = previous_epoch_attestations_default;
  public SSZList<PendingAttestation> current_epoch_attestations = current_epoch_attestations_default;
  public SSZVector<SSZList<ShardWork>> shard_buffer = shard_buffer_default;
  public Gwei shard_gasprice = shard_gasprice_default;
  public Shard current_epoch_start_shard = current_epoch_start_shard_default;
  public BeaconState copy() { return this; }
  public BeaconState(uint64 genesis_time, beacon_java.phase0.data.Root genesis_validators_root, beacon_java.phase0.data.Slot slot, beacon_java.phase0.data.Fork fork, beacon_java.phase0.data.BeaconBlockHeader latest_block_header, SSZVector<beacon_java.phase0.data.Root> block_roots, SSZVector<beacon_java.phase0.data.Root> state_roots, SSZList<beacon_java.phase0.data.Root> historical_roots, beacon_java.phase0.data.Eth1Data eth1_data, SSZList<beacon_java.phase0.data.Eth1Data> eth1_data_votes, uint64 eth1_deposit_index, SSZList<beacon_java.phase0.data.Validator> validators, SSZList<beacon_java.phase0.data.Gwei> balances, SSZVector<beacon_java.ssz.Bytes32> randao_mixes, SSZVector<beacon_java.phase0.data.Gwei> slashings, SSZList<beacon_java.phase0.data.PendingAttestation> previous_epoch_attestations, SSZList<beacon_java.phase0.data.PendingAttestation> current_epoch_attestations, beacon_java.ssz.SSZBitvector justification_bits, beacon_java.phase0.data.Checkpoint previous_justified_checkpoint, beacon_java.phase0.data.Checkpoint current_justified_checkpoint, beacon_java.phase0.data.Checkpoint finalized_checkpoint, beacon_java.merge.data.ExecutionPayloadHeader latest_execution_payload_header, SSZVector<SSZList<ShardWork>> shard_buffer, Gwei shard_gasprice, Shard current_epoch_start_shard) {
    super(genesis_time, genesis_validators_root, slot, fork, latest_block_header, block_roots, state_roots, historical_roots, eth1_data, eth1_data_votes, eth1_deposit_index, validators, balances, randao_mixes, slashings, previous_epoch_attestations, current_epoch_attestations, justification_bits, previous_justified_checkpoint, current_justified_checkpoint, finalized_checkpoint, latest_execution_payload_header);
    this.previous_epoch_attestations = previous_epoch_attestations;
    this.current_epoch_attestations = current_epoch_attestations;
    this.shard_buffer = shard_buffer;
    this.shard_gasprice = shard_gasprice;
    this.current_epoch_start_shard = current_epoch_start_shard;
  }
}
