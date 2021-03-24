package beacon_java.altair.data;

import beacon_java.data.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedBeaconBlock {
  public static BeaconBlock message_default = new BeaconBlock();
  public static BLSSignature signature_default = new BLSSignature();
  public BeaconBlock message = message_default;
  public BLSSignature signature = signature_default;
  public SignedBeaconBlock copy() { return this; }
}