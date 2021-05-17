package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.VoluntaryExit;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedVoluntaryExit extends Container {
  public static VoluntaryExit message_default = new VoluntaryExit();
  public static BLSSignature signature_default = new BLSSignature();
  public VoluntaryExit message = message_default;
  public BLSSignature signature = signature_default;
  public SignedVoluntaryExit copy() { return this; }
}
