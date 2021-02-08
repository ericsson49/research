package beacon_java.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AttestationData {
  public static Slot slot_default = new Slot();
  public static CommitteeIndex index_default = new CommitteeIndex();
  public static Root beacon_block_root_default = new Root();
  public static Checkpoint source_default = new Checkpoint();
  public static Checkpoint target_default = new Checkpoint();
  public static Shard shard_default = new Shard();
  public static Root shard_head_root_default = new Root();
  public static Root shard_transition_root_default = new Root();
  public Slot slot = slot_default;
  public CommitteeIndex index = index_default;
  public Root beacon_block_root = beacon_block_root_default;
  public Checkpoint source = source_default;
  public Checkpoint target = target_default;
  public Shard shard = shard_default;
  public Root shard_head_root = shard_head_root_default;
  public Root shard_transition_root = shard_transition_root_default;
  public AttestationData copy() { return this; }
}
