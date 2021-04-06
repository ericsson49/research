
interface NameResolver<T> {
  val keys: Set<String>
  fun resolve(name: identifier): T?
  operator fun contains(name: identifier) = resolve(name) != null
  operator fun get(name: identifier) = resolve(name)
  fun copy(names: Map<String, T>) = VarCtx(this, names.toMutableMap())
  fun copy(names: Collection<Pair<String, T>>) = VarCtx(this, names.toMap().toMutableMap())
}


class MapResolver<T>(val map: Map<String,T>): NameResolver<T> {
  override val keys = map.keys
  override fun resolve(name: identifier) = map[name]
}

class VarCtx<T>(val parent: NameResolver<T>, val localVars: Map<String,T> = mapOf()): NameResolver<T> {
  override val keys = parent.keys.union(localVars.keys)
  override fun resolve(name: identifier) = localVars[name] ?: parent[name]
}
