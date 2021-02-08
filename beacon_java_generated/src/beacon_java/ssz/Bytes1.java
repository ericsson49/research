package beacon_java.ssz;

import beacon_java.pylib.Sequence;
import beacon_java.pylib.pybytes;
import beacon_java.pylib.pyint;

import java.util.Iterator;
import java.util.function.Function;

import static beacon_java.util.Exports.TODO;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public class Bytes1 implements pybytes {
  public Bytes1(String value) {
    TODO();
  }

  public Bytes1() {
    TODO();
  }

  @Override
  public void set(pyint i, pyint v) {
    TODO();
  }

  @Override
  public void setSlice(pyint start, pyint upper, Sequence<pyint> v) {
    TODO();
  }

  @Override
  public <V> Sequence<V> map(Function<pyint, V> f) {
    return TODO(Sequence.class);
  }

  @Override
  public Sequence<pyint> filter(Function<pyint, pyint> f) {
    return TODO(Sequence.class);
  }

  @Override
  public pyint index(pyint pyint) {
    return TODO(pyint.class);
  }

  @Override
  public pyint count(pyint pyint) {
    return TODO(pyint.class);
  }

  @Override
  public pyint get(pyint index) {
    return TODO(pyint.class);
  }

  @Override
  public pybytes getSlice(pyint start, pyint upper) {
    return TODO(pybytes.class);
  }

  @Override
  public pybytes join(Sequence<pybytes> s) {
    return TODO(pybytes.class);
  }

  @Override
  public Iterator<pyint> iterator() {
    return TODO(Iterator.class);
  }
}
