package beacon_java.altair.data;

import beacon_java.data.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class Eth1Block {
  public static uint64 timestamp_default = uint64.ZERO;
  public static Root deposit_root_default = new Root();
  public static uint64 deposit_count_default = uint64.ZERO;
  public uint64 timestamp = timestamp_default;
  public Root deposit_root = deposit_root_default;
  public uint64 deposit_count = deposit_count_default;
  public Eth1Block copy() { return this; }
}
