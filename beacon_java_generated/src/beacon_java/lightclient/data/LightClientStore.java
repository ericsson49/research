package beacon_java.lightclient.data;

import beacon_java.ssz.SSZList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class LightClientStore {
  public static LightClientSnapshot snapshot_default = new LightClientSnapshot();
  public static SSZList<LightClientUpdate> valid_updates_default = new SSZList<LightClientUpdate>();
  public LightClientSnapshot snapshot = snapshot_default;
  public SSZList<LightClientUpdate> valid_updates = valid_updates_default;
  public LightClientStore copy() { return this; }
}
