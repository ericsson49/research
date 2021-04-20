package beacon_java.merge.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

public class OpaqueTransaction extends SSZByteList {
  public OpaqueTransaction(SSZByteList value) { super(value); }
  public OpaqueTransaction() { super(new SSZByteList()); }
  public OpaqueTransaction(String value) { super(value); }
}
