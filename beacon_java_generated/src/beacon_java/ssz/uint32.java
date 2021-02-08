package beacon_java.ssz;

import beacon_java.pylib.pyint;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public class uint32 extends uint {
  public static final uint32 ZERO = new uint32(0L);

  public uint32(long value) {
  }

  public uint32(uint32 value) {
  }

  public uint32(pyint value) {
  }
}
