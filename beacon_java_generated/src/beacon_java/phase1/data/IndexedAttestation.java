package beacon_java.phase1.data;

import beacon_java.data.BLSSignature;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class IndexedAttestation {
  public static SSZList<ValidatorIndex> attesting_indices_default = new SSZList<ValidatorIndex>();
  public static AttestationData data_default = new AttestationData();
  public static BLSSignature signature_default = new BLSSignature();
  public SSZList<ValidatorIndex> attesting_indices = attesting_indices_default;
  public AttestationData data = data_default;
  public BLSSignature signature = signature_default;
  public IndexedAttestation copy() { return this; }
}
