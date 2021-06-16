package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.sharding.data.DataCommitment;
import beacon_java.sharding.data.ShardWorkStatus;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShardWorkStatus_1 extends ShardWorkStatus {
  public static DataCommitment value_default = new DataCommitment();
  public DataCommitment value = value_default;
  public ShardWorkStatus_1 copy() { return this; }
  public ShardWorkStatus_1(pyint selector, DataCommitment value) {
    super(selector);
    this.value = value;
  }
}
