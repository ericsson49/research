package beacon_java.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class Shard extends uint64 {
  public Shard(uint64 value) { super(value); }
  public Shard() { super(uint64.ZERO); }
  public Shard(pyint value) { super(value); }
}
