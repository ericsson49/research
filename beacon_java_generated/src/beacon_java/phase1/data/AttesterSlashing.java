package beacon_java.phase1.data;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AttesterSlashing {
  public static IndexedAttestation attestation_1_default = new IndexedAttestation();
  public static IndexedAttestation attestation_2_default = new IndexedAttestation();
  public IndexedAttestation attestation_1 = attestation_1_default;
  public IndexedAttestation attestation_2 = attestation_2_default;
  public AttesterSlashing copy() { return this; }
}
