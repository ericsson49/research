package beacon_java.lightclient.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class Domain extends Bytes32 {
  public Domain(Bytes32 value) { super(value); }
  public Domain() { super(new Bytes32()); }
  public Domain(String value) { super(value); }
  public Domain(pybytes value) { super(value); }
}
