package beacon_java.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class CommitteeIndex extends uint64 {
  public CommitteeIndex(uint64 value) { super(value); }
  public CommitteeIndex() { super(uint64.ZERO); }
  public CommitteeIndex(pyint value) { super(value); }
}
