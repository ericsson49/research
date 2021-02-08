package beacon_java.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class Checkpoint {
  public static Epoch epoch_default = new Epoch();
  public static Root root_default = new Root();
  public Epoch epoch = epoch_default;
  public Root root = root_default;
  public Checkpoint copy() { return this; }
}
