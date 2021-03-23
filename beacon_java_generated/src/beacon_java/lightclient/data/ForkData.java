package beacon_java.lightclient.data;

import beacon_java.data.Root;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ForkData {
  public static Version current_version_default = new Version();
  public static Root genesis_validators_root_default = new Root();
  public Version current_version = current_version_default;
  public Root genesis_validators_root = genesis_validators_root_default;
  public ForkData copy() { return this; }
}
