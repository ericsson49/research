package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.uint64;

public class Slot extends uint64 {
  public Slot(uint64 value) { super(value); }
  public Slot() { super(uint64.ZERO); }
  public Slot(pyint value) { super(value); }
}
