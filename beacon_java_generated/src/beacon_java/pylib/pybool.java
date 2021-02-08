package beacon_java.pylib;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public interface pybool extends pyint {
  boolean v();

  static pybool create(boolean b) {
    return null;
  }
}
