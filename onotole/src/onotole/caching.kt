package onotole

import java.util.*


class MemoizingFunc<K,V>(val func: (K) -> V) {
  val cache = IdentityHashMap<K,V>()
  operator fun invoke(k: K): V {
    return cache.getOrPut(k) { func(k) }
  }
}
