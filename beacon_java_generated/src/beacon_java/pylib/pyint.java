package beacon_java.pylib;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public interface pyint {
  public static final pyint ZERO = create(0L);

  static pyint create(pyint v) {
    return v;
  }

  static pyint create(long v) {
    return null;
  }

  static pyint create(String v) {
    return null;
  }

  static pyint from_bytes(pybytes data_0, String endianness) {
    return null;
  }

  pybytes to_bytes(pyint q, String b);
  pyint bit_length();
}
