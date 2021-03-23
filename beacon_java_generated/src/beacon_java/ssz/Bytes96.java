package beacon_java.ssz;

import beacon_java.pylib.Sequence;
import beacon_java.pylib.pybytes;
import beacon_java.pylib.pyint;

import java.util.Iterator;
import java.util.function.Function;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public class Bytes96 implements pybytes {
  public Bytes96(Bytes96 value) {
  }

  public Bytes96(pybytes value) {
  }

  public Bytes96(String value) {
  }

  public Bytes96() {
  }

  @Override
  public void set(pyint i, pyint v) {

  }

  @Override
  public void setSlice(pyint start, pyint upper, Sequence<pyint> v) {

  }

  @Override
  public <V> Sequence<V> map(Function<pyint, V> f) {
    return null;
  }

  @Override
  public Sequence<pyint> filter(Function<pyint, pyint> f) {
    return null;
  }

  @Override
  public pyint index(pyint pyint) {
    return null;
  }

  @Override
  public pyint count(pyint pyint) {
    return null;
  }

  @Override
  public pyint get(pyint index) {
    return null;
  }

  @Override
  public pybytes getSlice(pyint start, pyint upper) {
    return null;
  }

  @Override
  public pybytes join(Sequence<pybytes> s) {
    return null;
  }

  @Override
  public Iterator<pyint> iterator() {
    return null;
  }
}
