package beacon_java.altair.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BLSPubkey;
import beacon_java.ssz.Container;
import static beacon_java.altair.Constants.SYNC_COMMITTEE_SIZE;
import beacon_java.ssz.SSZVector;

@Data @NoArgsConstructor @AllArgsConstructor
public class SyncCommittee extends Container {
  public static SSZVector<BLSPubkey> pubkeys_default = new SSZVector<BLSPubkey>();
  public static BLSPubkey aggregate_pubkey_default = new BLSPubkey();
  public SSZVector<BLSPubkey> pubkeys = pubkeys_default;
  public BLSPubkey aggregate_pubkey = aggregate_pubkey_default;
  public SyncCommittee copy() { return this; }
}
