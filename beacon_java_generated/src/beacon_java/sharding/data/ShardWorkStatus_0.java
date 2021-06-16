package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.sharding.data.ShardWorkStatus;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShardWorkStatus_0 extends ShardWorkStatus {
  public static void value_default = new void();
  public void value = value_default;
  public ShardWorkStatus_0 copy() { return this; }
  public ShardWorkStatus_0(pyint selector, void value) {
    super(selector);
    this.value = value;
  }
}
