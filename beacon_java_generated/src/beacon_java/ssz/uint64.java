package beacon_java.ssz;

import beacon_java.pylib.pyint;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public class uint64 extends uint {
  public static final uint64 ZERO = new uint64(0L);

  public uint64(long value) {
  }

  public uint64(uint64 value) {
  }

  public uint64(pyint value) {
  }

  public uint64 plus(long l) {
    return null;
  }

  public uint64 plus(uint64 b) {
    return null;
  }

  public uint64 minus(long l) {
    return null;
  }

  public uint64 minus(uint64 b) {
    return null;
  }

  public uint64 multiply(long l) {
    return null;
  }

  public uint64 multiply(uint64 b) {
    return null;
  }

  public uint64 divide(long l) {
    return null;
  }

  public uint64 divide(uint64 x_1) {
    return null;
  }

  public uint64 modulo(long l) {
    return null;
  }

  public uint64 modulo(uint64 b) {
    return null;
  }


  public boolean less(uint64 y_2) {
    return false;
  }

  public boolean lessOrEqual(uint64 y_2) {
    return false;
  }

  public boolean greater(uint64 y_2) {
    return false;
  }

  public boolean greaterOrEqual(uint64 y_2) {
    return false;
  }
}
