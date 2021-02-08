package beacon_java.util;

import beacon_java.ssz.uint64;

import static beacon_java.pylib.Exports.less;
import static beacon_java.pylib.Exports.minus;

public class Exports {
  public static uint64 ceillog2(uint64 x) {
    if (less(x, new uint64(1L)).v())
      throw new IllegalArgumentException("ceillog2 accepts only positive values, x=${x}");
    return new uint64(minus(x, new uint64(1L)).bit_length());
  }
  public static void TODO() { throw new RuntimeException("TODO: not yet implemented"); }
  public static <T> T TODO(Class<T> p) { throw new RuntimeException("TODO: not yet implemented"); }
}
