package beacon_java.phase0.data;

import beacon_java.data.Root;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconBlockHeader {
  public static Slot slot_default = new Slot();
  public static ValidatorIndex proposer_index_default = new ValidatorIndex();
  public static Root parent_root_default = new Root();
  public static Root state_root_default = new Root();
  public static Root body_root_default = new Root();
  public Slot slot = slot_default;
  public ValidatorIndex proposer_index = proposer_index_default;
  public Root parent_root = parent_root_default;
  public Root state_root = state_root_default;
  public Root body_root = body_root_default;
  public BeaconBlockHeader copy() { return this; }
}
