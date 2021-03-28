package beacon_java.phase1.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class Root extends Bytes32 {
  public Root(Bytes32 value) { super(value); }
  public Root() { super(new Bytes32()); }
  public Root(String value) { super(value); }
}
