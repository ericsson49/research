package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.Epoch;
import beacon_java.phase0.data.Root;

@Data @NoArgsConstructor @AllArgsConstructor
public class LatestMessage extends Object {
  public static Epoch epoch_default = new Epoch();
  public static Root root_default = new Root();
  public Epoch epoch = epoch_default;
  public Root root = root_default;
  public LatestMessage copy() { return this; }
}
