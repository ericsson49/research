package beacon_java.altair.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class LightClientSnapshot {
  public static BeaconBlockHeader header_default = new BeaconBlockHeader();
  public static SyncCommittee current_sync_committee_default = new SyncCommittee();
  public static SyncCommittee next_sync_committee_default = new SyncCommittee();
  public BeaconBlockHeader header = header_default;
  public SyncCommittee current_sync_committee = current_sync_committee_default;
  public SyncCommittee next_sync_committee = next_sync_committee_default;
  public LightClientSnapshot copy() { return this; }
}
