package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BeaconBlockBody;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Root;
import beacon_java.phase0.data.Slot;
import beacon_java.phase0.data.ValidatorIndex;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconBlock extends Container {
  public static Slot slot_default = new Slot();
  public static ValidatorIndex proposer_index_default = new ValidatorIndex();
  public static Root parent_root_default = new Root();
  public static Root state_root_default = new Root();
  public static BeaconBlockBody body_default = new BeaconBlockBody();
  public Slot slot = slot_default;
  public ValidatorIndex proposer_index = proposer_index_default;
  public Root parent_root = parent_root_default;
  public Root state_root = state_root_default;
  public BeaconBlockBody body = body_default;
  public BeaconBlock copy() { return this; }
}
