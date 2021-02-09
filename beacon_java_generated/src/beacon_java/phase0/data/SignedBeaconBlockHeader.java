package beacon_java.phase0.data;

import beacon_java.data.BLSSignature;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedBeaconBlockHeader {
  public static BeaconBlockHeader message_default = new BeaconBlockHeader();
  public static BLSSignature signature_default = new BLSSignature();
  public BeaconBlockHeader message = message_default;
  public BLSSignature signature = signature_default;
  public SignedBeaconBlockHeader copy() { return this; }
}
