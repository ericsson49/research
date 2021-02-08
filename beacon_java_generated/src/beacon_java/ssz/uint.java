package beacon_java.ssz;

import beacon_java.pylib.pybytes;
import beacon_java.pylib.pyint;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public class uint implements pyint {
  @Override
  public pybytes to_bytes(pyint q, String b) {
    return null;
  }

  @Override
  public pyint bit_length() {
    return null;
  }

}
