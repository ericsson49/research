package beacon_java.merge.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.merge.data.BeaconBlock;
import beacon_java.ssz.Container;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedBeaconBlock extends Container {
  public static BeaconBlock message_default = new BeaconBlock();
  public static BLSSignature signature_default = new BLSSignature();
  public BeaconBlock message = message_default;
  public BLSSignature signature = signature_default;
  public SignedBeaconBlock copy() { return this; }
}
