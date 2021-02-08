package beacon_java.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShardBlock {
  public static Root shard_parent_root_default = new Root();
  public static Root beacon_parent_root_default = new Root();
  public static Slot slot_default = new Slot();
  public static Shard shard_default = new Shard();
  public static ValidatorIndex proposer_index_default = new ValidatorIndex();
  public static SSZByteList body_default = new SSZByteList();
  public Root shard_parent_root = shard_parent_root_default;
  public Root beacon_parent_root = beacon_parent_root_default;
  public Slot slot = slot_default;
  public Shard shard = shard_default;
  public ValidatorIndex proposer_index = proposer_index_default;
  public SSZByteList body = body_default;
  public ShardBlock copy() { return this; }
}
