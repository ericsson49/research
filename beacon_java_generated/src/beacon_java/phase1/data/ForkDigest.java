package beacon_java.phase1.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class ForkDigest extends Bytes4 {
  public ForkDigest(Bytes4 value) { super(value); }
  public ForkDigest() { super(new Bytes4()); }
  public ForkDigest(String value) { super(value); }
  public ForkDigest(pybytes value) { super(value); }
}
