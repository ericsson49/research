package beacon_java.phase1.data;

import beacon_java.ssz.*;

public class DomainType extends Bytes4 {
  public DomainType(Bytes4 value) { super(value); }
  public DomainType() { super(new Bytes4()); }
  public DomainType(String value) { super(value); }
}
