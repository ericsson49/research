package beacon_java.phase1.data;

import beacon_java.data.BLSSignature;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedLightAggregateAndProof {
  public static LightAggregateAndProof message_default = new LightAggregateAndProof();
  public static BLSSignature signature_default = new BLSSignature();
  public LightAggregateAndProof message = message_default;
  public BLSSignature signature = signature_default;
  public SignedLightAggregateAndProof copy() { return this; }
}
