package beacon_java.phase1.data;

import beacon_java.data.BLSSignature;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedAggregateAndProof {
  public static AggregateAndProof message_default = new AggregateAndProof();
  public static BLSSignature signature_default = new BLSSignature();
  public AggregateAndProof message = message_default;
  public BLSSignature signature = signature_default;
  public SignedAggregateAndProof copy() { return this; }
}
