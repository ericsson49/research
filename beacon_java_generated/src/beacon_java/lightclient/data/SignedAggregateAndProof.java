package beacon_java.lightclient.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedAggregateAndProof {
  public static AggregateAndProof message_default = new AggregateAndProof();
  public static BLSSignature signature_default = new BLSSignature();
  public AggregateAndProof message = message_default;
  public BLSSignature signature = signature_default;
  public SignedAggregateAndProof copy() { return this; }
}
