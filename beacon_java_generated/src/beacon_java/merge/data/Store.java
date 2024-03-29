package beacon_java.merge.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.merge.data.BeaconBlock;
import beacon_java.merge.data.BeaconState;
import beacon_java.phase0.data.Checkpoint;
import beacon_java.phase0.data.LatestMessage;
import beacon_java.phase0.data.Root;
import beacon_java.phase0.data.ValidatorIndex;
import beacon_java.ssz.uint64;

@Data @NoArgsConstructor @AllArgsConstructor
public class Store extends Object {
  public static uint64 time_default = uint64.ZERO;
  public static uint64 genesis_time_default = uint64.ZERO;
  public static Checkpoint justified_checkpoint_default = new Checkpoint();
  public static Checkpoint finalized_checkpoint_default = new Checkpoint();
  public static Checkpoint best_justified_checkpoint_default = new Checkpoint();
  public static PyDict<Root,BeaconBlock> blocks_default = new PyDict<Root,BeaconBlock>();
  public static PyDict<Root,BeaconState> block_states_default = new PyDict<Root,BeaconState>();
  public static PyDict<Checkpoint,BeaconState> checkpoint_states_default = new PyDict<Checkpoint,BeaconState>();
  public static PyDict<ValidatorIndex,LatestMessage> latest_messages_default = new PyDict<ValidatorIndex,LatestMessage>();
  public uint64 time = time_default;
  public uint64 genesis_time = genesis_time_default;
  public Checkpoint justified_checkpoint = justified_checkpoint_default;
  public Checkpoint finalized_checkpoint = finalized_checkpoint_default;
  public Checkpoint best_justified_checkpoint = best_justified_checkpoint_default;
  public PyDict<Root,BeaconBlock> blocks = blocks_default;
  public PyDict<Root,BeaconState> block_states = block_states_default;
  public PyDict<Checkpoint,BeaconState> checkpoint_states = checkpoint_states_default;
  public PyDict<ValidatorIndex,LatestMessage> latest_messages = latest_messages_default;
  public Store copy() { return this; }
}
