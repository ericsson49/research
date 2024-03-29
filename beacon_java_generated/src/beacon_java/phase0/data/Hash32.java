package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Bytes32;

public class Hash32 extends Bytes32 {
  public Hash32(Bytes32 value) { super(value); }
  public Hash32() { super(new Bytes32()); }
  public Hash32(String value) { super(value); }
}
