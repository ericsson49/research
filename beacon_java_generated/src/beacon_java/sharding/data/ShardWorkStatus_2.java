package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.SSZList;
import static beacon_java.sharding.Constants.MAX_SHARD_HEADERS_PER_SHARD;
import beacon_java.sharding.data.PendingShardHeader;
import beacon_java.sharding.data.ShardWorkStatus;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShardWorkStatus_2 extends ShardWorkStatus {
  public static SSZList<PendingShardHeader> value_default = new SSZList<PendingShardHeader>();
  public SSZList<PendingShardHeader> value = value_default;
  public ShardWorkStatus_2 copy() { return this; }
  public ShardWorkStatus_2(pyint selector, SSZList<PendingShardHeader> value) {
    super(selector);
    this.value = value;
  }
}
