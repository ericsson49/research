package beacon_java.phase0.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class Ether extends uint64 {
  public Ether(uint64 value) { super(value); }
  public Ether() { super(uint64.ZERO); }
  public Ether(pyint value) { super(value); }
}
