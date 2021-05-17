package beacon_java.merge.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.SSZByteList;
import static beacon_java.merge.Constants.MAX_BYTES_PER_OPAQUE_TRANSACTION;

public class OpaqueTransaction extends SSZByteList {
  public OpaqueTransaction(SSZByteList value) { super(value); }
  public OpaqueTransaction() { super(new SSZByteList()); }
  public OpaqueTransaction(String value) { super(value); }
}
