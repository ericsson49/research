package beacon_java.altair.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SyncCommittee {
  public static SSZVector<BLSPubkey> pubkeys_default = new SSZVector<BLSPubkey>();
  public static SSZVector<BLSPubkey> pubkey_aggregates_default = new SSZVector<BLSPubkey>();
  public SSZVector<BLSPubkey> pubkeys = pubkeys_default;
  public SSZVector<BLSPubkey> pubkey_aggregates = pubkey_aggregates_default;
  public SyncCommittee copy() { return this; }
}
