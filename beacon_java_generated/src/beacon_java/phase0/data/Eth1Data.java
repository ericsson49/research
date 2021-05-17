package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Hash32;
import beacon_java.phase0.data.Root;
import beacon_java.ssz.uint64;

@Data @NoArgsConstructor @AllArgsConstructor
public class Eth1Data extends Container {
  public static Root deposit_root_default = new Root();
  public static uint64 deposit_count_default = uint64.ZERO;
  public static Hash32 block_hash_default = new Hash32();
  public Root deposit_root = deposit_root_default;
  public uint64 deposit_count = deposit_count_default;
  public Hash32 block_hash = block_hash_default;
  public Eth1Data copy() { return this; }
}
