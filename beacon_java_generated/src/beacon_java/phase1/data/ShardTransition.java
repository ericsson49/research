package beacon_java.phase1.data;

import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShardTransition {
  public static Slot start_slot_default = new Slot();
  public static SSZList<uint64> shard_block_lengths_default = new SSZList<uint64>();
  public static SSZList<Bytes32> shard_data_roots_default = new SSZList<Bytes32>();
  public static SSZList<ShardState> shard_states_default = new SSZList<ShardState>();
  public static BLSSignature proposer_signature_aggregate_default = new BLSSignature();
  public Slot start_slot = start_slot_default;
  public SSZList<uint64> shard_block_lengths = shard_block_lengths_default;
  public SSZList<Bytes32> shard_data_roots = shard_data_roots_default;
  public SSZList<ShardState> shard_states = shard_states_default;
  public BLSSignature proposer_signature_aggregate = proposer_signature_aggregate_default;
  public ShardTransition copy() { return this; }
}
