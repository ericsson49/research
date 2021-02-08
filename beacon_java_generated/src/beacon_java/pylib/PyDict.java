package beacon_java.pylib;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public class PyDict<K, V> implements Dictionary<K, V> {
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
