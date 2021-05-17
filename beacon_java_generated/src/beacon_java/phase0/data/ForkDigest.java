package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Bytes4;

public class ForkDigest extends Bytes4 {
  public ForkDigest(Bytes4 value) { super(value); }
  public ForkDigest() { super(new Bytes4()); }
  public ForkDigest(String value) { super(value); }
  public ForkDigest(pybytes value) { super(value); }
}
