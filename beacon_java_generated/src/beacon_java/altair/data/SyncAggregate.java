package beacon_java.altair.data;

import beacon_java.data.BLSSignature;
import beacon_java.ssz.SSZBitvector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class SyncAggregate {
  public static SSZBitvector sync_committee_bits_default = new SSZBitvector();
  public static BLSSignature sync_committee_signature_default = new BLSSignature();
  public SSZBitvector sync_committee_bits = sync_committee_bits_default;
  public BLSSignature sync_committee_signature = sync_committee_signature_default;
  public SyncAggregate copy() { return this; }
}
