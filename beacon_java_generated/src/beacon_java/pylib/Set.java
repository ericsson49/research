package beacon_java.pylib;

import java.util.Iterator;
import java.util.function.Function;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public class Set<E> implements Iterable<E> {
  public Set<E> union(Iterable<E> a) {
    return null;
  }

  public Set<E> intersection(Iterable<E> a) {
    return null;
  }

  public void add(E e) {}

  public <V> Set<V> map(Function<E, V> f) {
    return null;
  }

  public Set<E> filter(Function<E, pyint> f) {
    return null;
  }

  @Override
  public Iterator<E> iterator() {
    return null;
  }
}
