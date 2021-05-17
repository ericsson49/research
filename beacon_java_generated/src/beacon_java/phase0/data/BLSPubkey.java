package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Bytes48;

public class BLSPubkey extends Bytes48 {
  public BLSPubkey(Bytes48 value) { super(value); }
  public BLSPubkey() { super(new Bytes48()); }
  public BLSPubkey(String value) { super(value); }
}
