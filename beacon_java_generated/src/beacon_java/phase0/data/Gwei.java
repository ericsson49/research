package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.uint64;

public class Gwei extends uint64 {
  public Gwei(uint64 value) { super(value); }
  public Gwei() { super(uint64.ZERO); }
  public Gwei(pyint value) { super(value); }
}
