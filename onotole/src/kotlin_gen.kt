class KotlinGen: BaseGen() {
  override fun genValueClass(name: String, base: String) {
    println("typealias $name = $base")
    println("fun $name(x: $base): $name = x")
    val default = getDefaultValueForBase(base)
    println("fun $name() = $name($default)")
  }

  override fun genClsField(name: String, typ: String, init: String?): String {
    return "var ${name}: $typ" + (init?.let { " = $it" } ?: "")
  }

  override fun genContainerClass(name: String, fields: List<Triple<String, String, String?>>) {
    println("data class $name(")
    println(fields.joinToString(",\n") {
      genClsField(it.first, it.second, it.third)
    })
    println(")")
  }
}