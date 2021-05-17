package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.uint256;

public class BLSPoint extends uint256 {
  public BLSPoint(uint256 value) { super(value); }
  public BLSPoint() { super(new uint256()); }
  public BLSPoint(pyint value) { super(value); }
}
