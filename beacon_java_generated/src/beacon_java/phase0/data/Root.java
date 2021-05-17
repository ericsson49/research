package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Bytes32;

public class Root extends Bytes32 {
  public Root(Bytes32 value) { super(value); }
  public Root() { super(new Bytes32()); }
  public Root(String value) { super(value); }
}
