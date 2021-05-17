package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Root;
import beacon_java.phase0.data.Version;

@Data @NoArgsConstructor @AllArgsConstructor
public class ForkData extends Container {
  public static Version current_version_default = new Version();
  public static Root genesis_validators_root_default = new Root();
  public Version current_version = current_version_default;
  public Root genesis_validators_root = genesis_validators_root_default;
  public ForkData copy() { return this; }
}
