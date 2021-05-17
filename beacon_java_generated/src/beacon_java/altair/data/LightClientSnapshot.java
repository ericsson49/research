package beacon_java.altair.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BeaconBlockHeader;
import beacon_java.ssz.Container;
import beacon_java.altair.data.SyncCommittee;

@Data @NoArgsConstructor @AllArgsConstructor
public class LightClientSnapshot extends Container {
  public static BeaconBlockHeader header_default = new BeaconBlockHeader();
  public static SyncCommittee current_sync_committee_default = new SyncCommittee();
  public static SyncCommittee next_sync_committee_default = new SyncCommittee();
  public BeaconBlockHeader header = header_default;
  public SyncCommittee current_sync_committee = current_sync_committee_default;
  public SyncCommittee next_sync_committee = next_sync_committee_default;
  public LightClientSnapshot copy() { return this; }
}
