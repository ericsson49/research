package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.sharding.data.ShardWorkStatus;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShardWork extends Container {
  public static ShardWorkStatus status_default = new ShardWorkStatus();
  public ShardWorkStatus status = status_default;
  public ShardWork copy() { return this; }
}
