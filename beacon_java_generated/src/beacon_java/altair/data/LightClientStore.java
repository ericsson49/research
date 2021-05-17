package beacon_java.altair.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.altair.data.LightClientSnapshot;
import beacon_java.altair.data.LightClientUpdate;

@Data @NoArgsConstructor @AllArgsConstructor
public class LightClientStore extends Object {
  public static LightClientSnapshot snapshot_default = new LightClientSnapshot();
  public static Set<LightClientUpdate> valid_updates_default = new Set<LightClientUpdate>();
  public LightClientSnapshot snapshot = snapshot_default;
  public Set<LightClientUpdate> valid_updates = valid_updates_default;
  public LightClientStore copy() { return this; }
}
