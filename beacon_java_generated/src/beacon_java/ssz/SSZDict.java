package beacon_java.ssz;

import beacon_java.pylib.Dictionary;
import beacon_java.pylib.Pair;
import beacon_java.pylib.Sequence;
import beacon_java.pylib.Set;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public class SSZDict<K, V> implements Dictionary<K, V> {
  public SSZDict() {
  }

  public SSZDict(Pair<K, V>... args) {
  }

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
