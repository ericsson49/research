package beacon_java.sharding.data;

import beacon_java.phase0.data.Slot;
import beacon_java.phase0.data.ValidatorIndex;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShardBlobHeader {
  public static Slot slot_default = new Slot();
  public static Shard shard_default = new Shard();
  public static ShardBlobBodySummary body_summary_default = new ShardBlobBodySummary();
  public static ValidatorIndex proposer_index_default = new ValidatorIndex();
  public Slot slot = slot_default;
  public Shard shard = shard_default;
  public ShardBlobBodySummary body_summary = body_summary_default;
  public ValidatorIndex proposer_index = proposer_index_default;
  public ShardBlobHeader copy() { return this; }
}
