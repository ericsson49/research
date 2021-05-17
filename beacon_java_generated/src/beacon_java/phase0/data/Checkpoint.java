package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Epoch;
import beacon_java.phase0.data.Root;

@Data @NoArgsConstructor @AllArgsConstructor
public class Checkpoint extends Container {
  public static Epoch epoch_default = new Epoch();
  public static Root root_default = new Root();
  public Epoch epoch = epoch_default;
  public Root root = root_default;
  public Checkpoint copy() { return this; }
}
