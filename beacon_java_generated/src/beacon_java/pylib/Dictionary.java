package beacon_java.pylib;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public interface Dictionary<K, V> {
  V get(K k);

  void set(K k, V v);

  Set<K> keys();

  Sequence<V> values();

  Sequence<Pair<K, V>> items();
}
