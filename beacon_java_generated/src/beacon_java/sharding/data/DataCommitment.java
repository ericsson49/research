package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.sharding.data.BLSCommitment;
import beacon_java.ssz.Container;
import beacon_java.ssz.uint64;

@Data @NoArgsConstructor @AllArgsConstructor
public class DataCommitment extends Container {
  public static BLSCommitment point_default = new BLSCommitment();
  public static uint64 length_default = uint64.ZERO;
  public BLSCommitment point = point_default;
  public uint64 length = length_default;
  public DataCommitment copy() { return this; }
}
