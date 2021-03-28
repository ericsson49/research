package beacon_java.altair.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ProposerSlashing {
  public static SignedBeaconBlockHeader signed_header_1_default = new SignedBeaconBlockHeader();
  public static SignedBeaconBlockHeader signed_header_2_default = new SignedBeaconBlockHeader();
  public SignedBeaconBlockHeader signed_header_1 = signed_header_1_default;
  public SignedBeaconBlockHeader signed_header_2 = signed_header_2_default;
  public ProposerSlashing copy() { return this; }
}
