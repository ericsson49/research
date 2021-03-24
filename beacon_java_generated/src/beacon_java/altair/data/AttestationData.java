package beacon_java.altair.data;

import beacon_java.data.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AttestationData {
  public static Slot slot_default = new Slot();
  public static CommitteeIndex index_default = new CommitteeIndex();
  public static Root beacon_block_root_default = new Root();
  public static Checkpoint source_default = new Checkpoint();
  public static Checkpoint target_default = new Checkpoint();
  public Slot slot = slot_default;
  public CommitteeIndex index = index_default;
  public Root beacon_block_root = beacon_block_root_default;
  public Checkpoint source = source_default;
  public Checkpoint target = target_default;
  public AttestationData copy() { return this; }
}
