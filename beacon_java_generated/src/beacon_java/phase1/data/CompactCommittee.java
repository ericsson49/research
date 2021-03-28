package beacon_java.phase1.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CompactCommittee {
  public static SSZList<BLSPubkey> pubkeys_default = new SSZList<BLSPubkey>();
  public static SSZList<uint64> compact_validators_default = new SSZList<uint64>();
  public SSZList<BLSPubkey> pubkeys = pubkeys_default;
  public SSZList<uint64> compact_validators = compact_validators_default;
  public CompactCommittee copy() { return this; }
}
