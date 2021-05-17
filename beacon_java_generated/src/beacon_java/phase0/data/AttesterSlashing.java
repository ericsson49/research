package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.IndexedAttestation;

@Data @NoArgsConstructor @AllArgsConstructor
public class AttesterSlashing extends Container {
  public static IndexedAttestation attestation_1_default = new IndexedAttestation();
  public static IndexedAttestation attestation_2_default = new IndexedAttestation();
  public IndexedAttestation attestation_1 = attestation_1_default;
  public IndexedAttestation attestation_2 = attestation_2_default;
  public AttesterSlashing copy() { return this; }
}
