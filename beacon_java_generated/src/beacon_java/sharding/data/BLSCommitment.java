package beacon_java.sharding.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class BLSCommitment extends Bytes48 {
  public BLSCommitment(Bytes48 value) { super(value); }
  public BLSCommitment() { super(new Bytes48()); }
  public BLSCommitment(String value) { super(value); }
}
