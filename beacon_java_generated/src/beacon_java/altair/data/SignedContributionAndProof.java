package beacon_java.altair.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.ssz.Container;
import beacon_java.altair.data.ContributionAndProof;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedContributionAndProof extends Container {
  public static ContributionAndProof message_default = new ContributionAndProof();
  public static BLSSignature signature_default = new BLSSignature();
  public ContributionAndProof message = message_default;
  public BLSSignature signature = signature_default;
  public SignedContributionAndProof copy() { return this; }
}
