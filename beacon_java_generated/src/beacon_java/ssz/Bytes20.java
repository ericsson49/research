package beacon_java.ssz;

import beacon_java.pylib.Sequence;
import beacon_java.pylib.pybytes;
import beacon_java.pylib.pyint;

import java.util.Iterator;
import java.util.function.Function;

public class Bytes20 implements pybytes {
  public Bytes20(Sequence<pyint> value) {
  }

  public Bytes20(pybytes value) {
  }

  public Bytes20(Bytes32 value) {
  }

  public Bytes20(String value) {
  }

  public Bytes20() {
  }

  public pybytes plus(pybytes b) {
    return null;
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
