package onotole

import java.util.*

class StmtAnnoMap<T>(): IdentityHashMap<Stmt,T>() {
  override fun put(key: Stmt?, value: T): T? {
    if (key in this)
      throw IllegalArgumentException("Anno for $key is already set")
    return super.put(key, value)
  }
}