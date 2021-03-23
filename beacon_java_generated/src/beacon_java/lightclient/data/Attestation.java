package beacon_java.lightclient.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class Attestation {
  public static SSZBitlist aggregation_bits_default = new SSZBitlist();
  public static AttestationData data_default = new AttestationData();
  public static BLSSignature signature_default = new BLSSignature();
  public SSZBitlist aggregation_bits = aggregation_bits_default;
  public AttestationData data = data_default;
  public BLSSignature signature = signature_default;
  public Attestation copy() { return this; }
}
