package beacon_java.ssz;

import beacon_java.pylib.Sequence;
import beacon_java.pylib.pyint;

import java.util.Iterator;
import java.util.function.Function;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public class SSZList<E> implements Sequence<E> {
  public SSZList() {
  }

  public SSZList(Sequence<? extends E> a) {
  }

  public void append(E e) {
  }

  @Override
  public E get(pyint i) {
    return null;
  }

  @Override
  public void set(pyint i, E v) {

  }

  @Override
  public Sequence<E> getSlice(pyint start, pyint upper) {
    return null;
  }

  @Override
  public void setSlice(pyint start, pyint upper, Sequence<E> v) {

  }

  @Override
  public <V> Sequence<V> map(Function<E, V> f) {
    return null;
  }

  @Override
  public Sequence<E> filter(Function<E, pyint> f) {
    return null;
  }

  @Override
  public pyint index(E e) {
    return null;
  }

  @Override
  public pyint count(E e) {
    return null;
  }

  @Override
  public Iterator<E> iterator() {
    return null;
  }
}
