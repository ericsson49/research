package beacon_java.phase1.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class OnlineEpochs extends uint8 {
  public OnlineEpochs(uint8 value) { super(value); }
  public OnlineEpochs() { super(uint8.ZERO); }
  public OnlineEpochs(pyint value) { super(value); }
}
