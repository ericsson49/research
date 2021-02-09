package beacon_java.phase1.data;

import beacon_java.data.BLSSignature;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class FullAttestation {
  public static SSZBitlist aggregation_bits_default = new SSZBitlist();
  public static FullAttestationData data_default = new FullAttestationData();
  public static BLSSignature signature_default = new BLSSignature();
  public SSZBitlist aggregation_bits = aggregation_bits_default;
  public FullAttestationData data = data_default;
  public BLSSignature signature = signature_default;
  public FullAttestation copy() { return this; }
}
