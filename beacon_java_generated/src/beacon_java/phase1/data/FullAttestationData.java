package beacon_java.phase1.data;

import beacon_java.data.Root;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class FullAttestationData {
  public static Slot slot_default = new Slot();
  public static CommitteeIndex index_default = new CommitteeIndex();
  public static Root beacon_block_root_default = new Root();
  public static Checkpoint source_default = new Checkpoint();
  public static Checkpoint target_default = new Checkpoint();
  public static Root shard_head_root_default = new Root();
  public static ShardTransition shard_transition_default = new ShardTransition();
  public Slot slot = slot_default;
  public CommitteeIndex index = index_default;
  public Root beacon_block_root = beacon_block_root_default;
  public Checkpoint source = source_default;
  public Checkpoint target = target_default;
  public Root shard_head_root = shard_head_root_default;
  public ShardTransition shard_transition = shard_transition_default;
  public FullAttestationData copy() { return this; }
}
