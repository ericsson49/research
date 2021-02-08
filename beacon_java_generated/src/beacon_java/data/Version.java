package beacon_java.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class Version extends Bytes4 {
  public Version(Bytes4 value) { super(value); }
  public Version() { super(new Bytes4()); }
  public Version(String value) { super(value); }
}
