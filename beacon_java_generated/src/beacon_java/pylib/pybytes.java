package beacon_java.pylib;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public interface pybytes extends Sequence<pyint> {
  static pybytes create(String v) {
    return null;
  }

  pyint get(pyint index);

  pybytes getSlice(pyint start, pyint upper);

  pybytes join(Sequence<pybytes> s);
}
