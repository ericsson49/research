package beacon_java.phase1.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class ValidatorIndex extends uint64 {
  public ValidatorIndex(uint64 value) { super(value); }
  public ValidatorIndex() { super(uint64.ZERO); }
  public ValidatorIndex(pyint value) { super(value); }
}
