package beacon_java.altair.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedVoluntaryExit {
  public static VoluntaryExit message_default = new VoluntaryExit();
  public static BLSSignature signature_default = new BLSSignature();
  public VoluntaryExit message = message_default;
  public BLSSignature signature = signature_default;
  public SignedVoluntaryExit copy() { return this; }
}
