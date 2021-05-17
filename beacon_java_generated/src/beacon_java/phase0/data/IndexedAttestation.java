package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.AttestationData;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.ssz.Container;
import beacon_java.ssz.SSZList;
import static beacon_java.phase0.Constants.MAX_VALIDATORS_PER_COMMITTEE;
import beacon_java.phase0.data.ValidatorIndex;

@Data @NoArgsConstructor @AllArgsConstructor
public class IndexedAttestation extends Container {
  public static SSZList<ValidatorIndex> attesting_indices_default = new SSZList<ValidatorIndex>();
  public static AttestationData data_default = new AttestationData();
  public static BLSSignature signature_default = new BLSSignature();
  public SSZList<ValidatorIndex> attesting_indices = attesting_indices_default;
  public AttestationData data = data_default;
  public BLSSignature signature = signature_default;
  public IndexedAttestation copy() { return this; }
}
