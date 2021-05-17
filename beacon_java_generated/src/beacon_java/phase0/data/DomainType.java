package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Bytes4;

public class DomainType extends Bytes4 {
  public DomainType(Bytes4 value) { super(value); }
  public DomainType() { super(new Bytes4()); }
  public DomainType(String value) { super(value); }
}
