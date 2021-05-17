package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Root;
import beacon_java.ssz.uint64;

@Data @NoArgsConstructor @AllArgsConstructor
public class Eth1Block extends Container {
  public static uint64 timestamp_default = uint64.ZERO;
  public static Root deposit_root_default = new Root();
  public static uint64 deposit_count_default = uint64.ZERO;
  public uint64 timestamp = timestamp_default;
  public Root deposit_root = deposit_root_default;
  public uint64 deposit_count = deposit_count_default;
  public Eth1Block copy() { return this; }
}
