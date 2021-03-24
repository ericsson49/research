package beacon_java.lightclient.data;

import beacon_java.data.BLSSignature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedContributionAndProof {
  public static ContributionAndProof message_default = new ContributionAndProof();
  public static BLSSignature signature_default = new BLSSignature();
  public ContributionAndProof message = message_default;
  public BLSSignature signature = signature_default;
  public SignedContributionAndProof copy() { return this; }
}
