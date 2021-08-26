package beacon_java.merge.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.merge.data.OpaqueTransaction;

public class Transaction extends OpaqueTransaction {
  public Transaction(OpaqueTransaction value) { super(value); }
  public Transaction() { super(new OpaqueTransaction()); }
  public Transaction(String value) { super(value); }
}
