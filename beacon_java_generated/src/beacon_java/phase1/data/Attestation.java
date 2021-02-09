package beacon_java.phase1.data;

import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class Attestation {
  public static SSZBitlist aggregation_bits_default = new SSZBitlist();
  public static AttestationData data_default = new AttestationData();
  public static SSZList<SSZBitlist> custody_bits_blocks_default = new SSZList<SSZBitlist>();
  public static BLSSignature signature_default = new BLSSignature();
  public SSZBitlist aggregation_bits = aggregation_bits_default;
  public AttestationData data = data_default;
  public SSZList<SSZBitlist> custody_bits_blocks = custody_bits_blocks_default;
  public BLSSignature signature = signature_default;
  public Attestation copy() { return this; }
}
