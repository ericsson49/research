package beacon_java.phase1.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class LightClientVoteData {
  public static Slot slot_default = new Slot();
  public static Root beacon_block_root_default = new Root();
  public Slot slot = slot_default;
  public Root beacon_block_root = beacon_block_root_default;
  public LightClientVoteData copy() { return this; }
}
