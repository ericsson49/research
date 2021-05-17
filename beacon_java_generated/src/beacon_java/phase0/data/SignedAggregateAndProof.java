package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.AggregateAndProof;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.ssz.Container;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedAggregateAndProof extends Container {
  public static AggregateAndProof message_default = new AggregateAndProof();
  public static BLSSignature signature_default = new BLSSignature();
  public AggregateAndProof message = message_default;
  public BLSSignature signature = signature_default;
  public SignedAggregateAndProof copy() { return this; }
}
