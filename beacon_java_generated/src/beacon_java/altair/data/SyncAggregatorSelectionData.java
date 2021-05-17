package beacon_java.altair.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Slot;
import beacon_java.ssz.uint64;

@Data @NoArgsConstructor @AllArgsConstructor
public class SyncAggregatorSelectionData extends Container {
  public static Slot slot_default = new Slot();
  public static uint64 subcommittee_index_default = uint64.ZERO;
  public Slot slot = slot_default;
  public uint64 subcommittee_index = subcommittee_index_default;
  public SyncAggregatorSelectionData copy() { return this; }
}
