package beacon_java.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class BLSSignature extends Bytes96 {
  public BLSSignature(Bytes96 value) { super(value); }
  public BLSSignature() { super(new Bytes96()); }
  public BLSSignature(String value) { super(value); }
}
