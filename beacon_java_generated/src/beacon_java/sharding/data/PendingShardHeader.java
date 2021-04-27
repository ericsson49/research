package beacon_java.sharding.data;

import beacon_java.phase0.data.Root;
import beacon_java.phase0.data.Slot;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PendingShardHeader {
  public static Slot slot_default = new Slot();
  public static Shard shard_default = new Shard();
  public static DataCommitment commitment_default = new DataCommitment();
  public static Root root_default = new Root();
  public static SSZBitlist votes_default = new SSZBitlist();
  public static SSZBoolean confirmed_default = new SSZBoolean();
  public Slot slot = slot_default;
  public Shard shard = shard_default;
  public DataCommitment commitment = commitment_default;
  public Root root = root_default;
  public SSZBitlist votes = votes_default;
  public SSZBoolean confirmed = confirmed_default;
  public PendingShardHeader copy() { return this; }
}
