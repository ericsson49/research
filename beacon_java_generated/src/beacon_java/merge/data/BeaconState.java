package beacon_java.merge.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.merge.data.ExecutionPayloadHeader;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconState extends beacon_java.phase0.data.BeaconState {
  public static ExecutionPayloadHeader latest_execution_payload_header_default = new ExecutionPayloadHeader();
  public ExecutionPayloadHeader latest_execution_payload_header = latest_execution_payload_header_default;
  public BeaconState copy() { return this; }
  public BeaconState(beacon_java.ssz.uint64 genesis_time, beacon_java.phase0.data.Root genesis_validators_root, beacon_java.phase0.data.Slot slot, beacon_java.phase0.data.Fork fork, beacon_java.phase0.data.BeaconBlockHeader latest_block_header, beacon_java.ssz.SSZVector<beacon_java.phase0.data.Root> block_roots, beacon_java.ssz.SSZVector<beacon_java.phase0.data.Root> state_roots, beacon_java.ssz.SSZList<beacon_java.phase0.data.Root> historical_roots, beacon_java.phase0.data.Eth1Data eth1_data, beacon_java.ssz.SSZList<beacon_java.phase0.data.Eth1Data> eth1_data_votes, beacon_java.ssz.uint64 eth1_deposit_index, beacon_java.ssz.SSZList<beacon_java.phase0.data.Validator> validators, beacon_java.ssz.SSZList<beacon_java.phase0.data.Gwei> balances, beacon_java.ssz.SSZVector<beacon_java.ssz.Bytes32> randao_mixes, beacon_java.ssz.SSZVector<beacon_java.phase0.data.Gwei> slashings, beacon_java.ssz.SSZList<beacon_java.phase0.data.PendingAttestation> previous_epoch_attestations, beacon_java.ssz.SSZList<beacon_java.phase0.data.PendingAttestation> current_epoch_attestations, beacon_java.ssz.SSZBitvector justification_bits, beacon_java.phase0.data.Checkpoint previous_justified_checkpoint, beacon_java.phase0.data.Checkpoint current_justified_checkpoint, beacon_java.phase0.data.Checkpoint finalized_checkpoint, ExecutionPayloadHeader latest_execution_payload_header) {
    super(genesis_time, genesis_validators_root, slot, fork, latest_block_header, block_roots, state_roots, historical_roots, eth1_data, eth1_data_votes, eth1_deposit_index, validators, balances, randao_mixes, slashings, previous_epoch_attestations, current_epoch_attestations, justification_bits, previous_justified_checkpoint, current_justified_checkpoint, finalized_checkpoint);
    this.latest_execution_payload_header = latest_execution_payload_header;
  }
}
