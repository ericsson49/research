package beacon_java.phase0.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class Eth1Data {
  public static Root deposit_root_default = new Root();
  public static uint64 deposit_count_default = uint64.ZERO;
  public static Bytes32 block_hash_default = new Bytes32();
  public Root deposit_root = deposit_root_default;
  public uint64 deposit_count = deposit_count_default;
  public Bytes32 block_hash = block_hash_default;
  public Eth1Data copy() { return this; }
}
