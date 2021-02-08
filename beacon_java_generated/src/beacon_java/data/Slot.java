package beacon_java.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class Slot extends uint64 {
  public Slot(uint64 value) { super(value); }
  public Slot() { super(uint64.ZERO); }
  public Slot(pyint value) { super(value); }
}
