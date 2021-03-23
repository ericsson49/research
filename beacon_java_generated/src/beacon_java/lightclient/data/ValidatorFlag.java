package beacon_java.lightclient.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class ValidatorFlag extends uint8 {
  public ValidatorFlag(uint8 value) { super(value); }
  public ValidatorFlag() { super(uint8.ZERO); }
  public ValidatorFlag(pyint value) { super(value); }
}
