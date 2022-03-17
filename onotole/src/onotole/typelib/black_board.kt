package onotole.typelib

import onotole.fail

object TopLevelScope {
  val modLoaders = mutableMapOf<String,TLModLoader>()
  val resolved = mutableMapOf<String, TLModule>()

  fun registerModule(modLoader: TLModLoader) {
    modLoaders[modLoader.name] = modLoader
  }

  fun resolveModule(name: String): TLModule {
    return resolved.getOrPut(name) {
      val loader = modLoaders[name] ?: fail("unknown module $name")
      val deps = loader.deps.map { resolveModule(it) }
      loader.load(deps)
    }
  }
}