package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.uint64;

public class Epoch extends uint64 {
  public Epoch(uint64 value) { super(value); }
  public Epoch() { super(uint64.ZERO); }
  public Epoch(pyint value) { super(value); }
}
