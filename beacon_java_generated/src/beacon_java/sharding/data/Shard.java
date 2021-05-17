package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.uint64;

public class Shard extends uint64 {
  public Shard(uint64 value) { super(value); }
  public Shard() { super(uint64.ZERO); }
  public Shard(pyint value) { super(value); }
}
