package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.phase0.data.BeaconBlockHeader;
import beacon_java.ssz.Container;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedBeaconBlockHeader extends Container {
  public static BeaconBlockHeader message_default = new BeaconBlockHeader();
  public static BLSSignature signature_default = new BLSSignature();
  public BeaconBlockHeader message = message_default;
  public BLSSignature signature = signature_default;
  public SignedBeaconBlockHeader copy() { return this; }
}
