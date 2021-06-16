package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShardWorkStatus extends Container {
  public static pyint selector_default = new pyint();
  public pyint selector = selector_default;
  public ShardWorkStatus copy() { return this; }
}
