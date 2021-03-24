package beacon_java.lightclient.data;

import beacon_java.ssz.uint64;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class SyncCommitteeSigningData {
  public static Slot slot_default = new Slot();
  public static uint64 subcommittee_index_default = uint64.ZERO;
  public Slot slot = slot_default;
  public uint64 subcommittee_index = subcommittee_index_default;
  public SyncCommitteeSigningData copy() { return this; }
}
