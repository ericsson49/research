package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Bytes96;

public class BLSSignature extends Bytes96 {
  public BLSSignature(Bytes96 value) { super(value); }
  public BLSSignature() { super(new Bytes96()); }
  public BLSSignature(String value) { super(value); }
  public BLSSignature(pybytes value) { super(value); }
}
