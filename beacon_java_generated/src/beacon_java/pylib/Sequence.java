package beacon_java.pylib;

import java.util.function.Function;

public interface Sequence<E> extends Iterable<E> {
  E get(pyint i);

  void set(pyint i, E v);

  Sequence<E> getSlice(pyint start, pyint upper);

  void setSlice(pyint start, pyint upper, Sequence<E> v);

  <V> Sequence<V> map(Function<E, V> f);

  Sequence<E> filter(Function<E, pyint> f);

  pyint index(E e);

  pyint count(E e);
}
