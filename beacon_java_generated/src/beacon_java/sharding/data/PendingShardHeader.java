package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.SSZBitlist;
import beacon_java.ssz.Container;
import beacon_java.sharding.data.DataCommitment;
import beacon_java.phase0.data.Gwei;
import static beacon_java.phase0.Constants.MAX_VALIDATORS_PER_COMMITTEE;
import beacon_java.phase0.data.Root;
import beacon_java.phase0.data.Slot;

@Data @NoArgsConstructor @AllArgsConstructor
public class PendingShardHeader extends Container {
  public static DataCommitment commitment_default = new DataCommitment();
  public static Root root_default = new Root();
  public static SSZBitlist votes_default = new SSZBitlist();
  public static Gwei weight_default = new Gwei();
  public static Slot update_slot_default = new Slot();
  public DataCommitment commitment = commitment_default;
  public Root root = root_default;
  public SSZBitlist votes = votes_default;
  public Gwei weight = weight_default;
  public Slot update_slot = update_slot_default;
  public PendingShardHeader copy() { return this; }
}
