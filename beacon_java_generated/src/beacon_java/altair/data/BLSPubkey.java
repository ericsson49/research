package beacon_java.altair.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class BLSPubkey extends Bytes48 {
  public BLSPubkey(Bytes48 value) { super(value); }
  public BLSPubkey() { super(new Bytes48()); }
  public BLSPubkey(String value) { super(value); }
}
