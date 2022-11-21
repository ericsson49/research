package onotole.typelib

import onotole.fail

interface NameRegistry {
  fun registerModule(modLoader: TLModLoader)
  fun resolveModule(name: String): TLModule
}

object TopLevelScope: NameRegistry {
  val modLoaders = mutableMapOf<String,TLModLoader>()
  val resolved = mutableMapOf<String, TLModule>()

  override fun registerModule(modLoader: TLModLoader) {
    modLoaders[modLoader.name] = modLoader
  }

  override fun resolveModule(name: String): TLModule {
    return resolved.getOrPut(name) {
      val loader = modLoaders[name] ?: fail("unknown module $name")
      val deps = loader.deps.map { resolveModule(it) }
      loader.load(deps)
    }
  }
}