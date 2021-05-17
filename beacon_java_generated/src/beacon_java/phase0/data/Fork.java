package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Epoch;
import beacon_java.phase0.data.Version;

@Data @NoArgsConstructor @AllArgsConstructor
public class Fork extends Container {
  public static Version previous_version_default = new Version();
  public static Version current_version_default = new Version();
  public static Epoch epoch_default = new Epoch();
  public Version previous_version = previous_version_default;
  public Version current_version = current_version_default;
  public Epoch epoch = epoch_default;
  public Fork copy() { return this; }
}
