package beacon_java.altair.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.ssz.SSZBitvector;
import beacon_java.ssz.Container;
import static beacon_java.altair.Constants.SYNC_COMMITTEE_SIZE;

@Data @NoArgsConstructor @AllArgsConstructor
public class SyncAggregate extends Container {
  public static SSZBitvector sync_committee_bits_default = new SSZBitvector();
  public static BLSSignature sync_committee_signature_default = new BLSSignature();
  public SSZBitvector sync_committee_bits = sync_committee_bits_default;
  public BLSSignature sync_committee_signature = sync_committee_signature_default;
  public SyncAggregate copy() { return this; }
}
