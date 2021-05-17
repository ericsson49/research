package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.AttestationData;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.ssz.SSZBitlist;
import beacon_java.ssz.Container;
import static beacon_java.phase0.Constants.MAX_VALIDATORS_PER_COMMITTEE;

@Data @NoArgsConstructor @AllArgsConstructor
public class Attestation extends Container {
  public static SSZBitlist aggregation_bits_default = new SSZBitlist();
  public static AttestationData data_default = new AttestationData();
  public static BLSSignature signature_default = new BLSSignature();
  public SSZBitlist aggregation_bits = aggregation_bits_default;
  public AttestationData data = data_default;
  public BLSSignature signature = signature_default;
  public Attestation copy() { return this; }
}
