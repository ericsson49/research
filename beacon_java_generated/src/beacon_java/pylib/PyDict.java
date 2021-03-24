package beacon_java.pylib;

import static beacon_java.util.Exports.TODO;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public class PyDict<K, V> implements Dictionary<K, V> {
  public static <K,V> PyDict<K,V> of(Pair<K, V>... pairs) { return TODO(PyDict.class); }
  public PyDict(Pair<K, V>... pairs) {
  }

  public PyDict(Sequence<Pair<K, V>> pairs) {
  }

  @Override
  public V get(K k) {
    return null;
  }

  @Override
  public void set(K k, V v) {

  }

  @Override
  public Set<K> keys() {
    return null;
  }

  @Override
  public Sequence<V> values() {
    return null;
  }

  @Override
  public Sequence<Pair<K, V>> items() {
    return null;
  }
}
