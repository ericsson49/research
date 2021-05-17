package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Bytes4;

public class Version extends Bytes4 {
  public Version(Bytes4 value) { super(value); }
  public Version() { super(new Bytes4()); }
  public Version(String value) { super(value); }
}
