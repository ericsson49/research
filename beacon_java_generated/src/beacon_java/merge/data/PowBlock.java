package beacon_java.merge.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Hash32;
import beacon_java.ssz.SSZBoolean;
import beacon_java.ssz.uint256;

@Data @NoArgsConstructor @AllArgsConstructor
public class PowBlock extends Container {
  public static Hash32 block_hash_default = new Hash32();
  public static SSZBoolean is_processed_default = new SSZBoolean();
  public static SSZBoolean is_valid_default = new SSZBoolean();
  public static uint256 total_difficulty_default = new uint256();
  public Hash32 block_hash = block_hash_default;
  public SSZBoolean is_processed = is_processed_default;
  public SSZBoolean is_valid = is_valid_default;
  public uint256 total_difficulty = total_difficulty_default;
  public PowBlock copy() { return this; }
}
