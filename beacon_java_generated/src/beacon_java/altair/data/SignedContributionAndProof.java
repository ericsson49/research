package beacon_java.altair.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedContributionAndProof {
  public static ContributionAndProof message_default = new ContributionAndProof();
  public static BLSSignature signature_default = new BLSSignature();
  public ContributionAndProof message = message_default;
  public BLSSignature signature = signature_default;
  public SignedContributionAndProof copy() { return this; }
}
