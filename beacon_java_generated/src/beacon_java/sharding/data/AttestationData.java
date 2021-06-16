package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.Root;

@Data @NoArgsConstructor @AllArgsConstructor
public class AttestationData extends beacon_java.phase0.data.AttestationData {
  public static Root shard_header_root_default = new Root();
  public Root shard_header_root = shard_header_root_default;
  public AttestationData copy() { return this; }
  public AttestationData(beacon_java.phase0.data.Slot slot, beacon_java.phase0.data.CommitteeIndex index, Root beacon_block_root, beacon_java.phase0.data.Checkpoint source, beacon_java.phase0.data.Checkpoint target, Root shard_header_root) {
    super(slot, index, beacon_block_root, source, target);
    this.shard_header_root = shard_header_root;
  }
}
