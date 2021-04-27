package beacon_java.sharding.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class BLSPoint extends uint256 {
  public BLSPoint(uint256 value) { super(value); }
  public BLSPoint() { super(new uint256()); }
  public BLSPoint(pyint value) { super(value); }
}
