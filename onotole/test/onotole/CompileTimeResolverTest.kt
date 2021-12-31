package onotole

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/*
internal class CompileTimeResolverTest {
  private val validatorClass = VTClass("phase0.Validator", emptyList(), emptyList())

  fun mkNameResolver(map: Map<String, VType>): VTResolver = object : VTResolver {
    override fun resolve(n: String): VType? {
      return map[n]
    }
  }

  fun mkPkgResolver(pkgs: Map<String, Map<String, VType>>): VTResolver = object : VTResolver {
    override fun resolve(n: String): VType? {
      return pkgs[n]?.let { VTPkg(n) }
    }
    override fun resolvePkgAttr(p: VTPkg, a: identifier): VType {
      return pkgs[p.name]?.get(a) ?: fail()
    }
  }

  @Test fun variable() {
    val e = mkName("a")
    assertEquals(e, compileTimeResolver(mkNameResolver(emptyMap()), e))
  }
  @Test fun attribute() {
    val e = mkAttribute(mkName("a"), "f")
    assertEquals(e, compileTimeResolver(mkNameResolver(emptyMap()), e))
  }

  @Test fun pkgAttribute() {
    val globals = mapOf("a" to mapOf("f" to VTQuote(Num(100))))
    val e = mkAttribute(mkName("a"), "f")
    assertEquals(CTValue(VTAttr(VTPkg("a"), "f")), compileTimeResolver(mkPkgResolver(globals), e))
  }
  @Test fun classHandle() {
    val globals = mapOf(
        "Validator" to validatorClass,
    )
    val e = mkName("Validator")
    assertEquals(CTValue(validatorClass), compileTimeResolver(mkNameResolver(globals), e))
  }
  @Test fun templateHandle() {
    val globals = mapOf(
        "List" to VTClassTemplate("ssz.List", 1, 1),
        "Validator" to validatorClass,
    )
    // List[Validator,100]
    val e = mkSubscript(mkName("List"), mkIndex(mkName("Validator"), Num(100)))
    val expected = CTValue(VTClass("ssz.List", listOf(validatorClass), listOf(VTQuote(Num(100)))))
    assertEquals(expected, compileTimeResolver(mkNameResolver(globals), e))
  }
  @Test fun classInstantiation() {
    val globals = mapOf(
        "Validator" to validatorClass,
    )
    // Validator()
    val e = mkCall(mkName("Validator"), emptyList())
    assertEquals(mkCall(CTValue(VTAttr(validatorClass, "<ctor>")), emptyList()), compileTimeResolver(mkNameResolver(globals), e))
  }

  @Test fun templateInstantiation() {
    val globals = mapOf(
        "List" to VTClassTemplate("ssz.List", 1, 1),
        "Validator" to validatorClass
    )
    // List[Validator,100](a=True,b=0)
    val ex1 = Call(
        mkSubscript(mkName("List"), mkIndex(mkName("Validator"), Num(100))),
        listOf(), listOf(Keyword("a", NameConstant(true)), Keyword("b", Num(0)))
    )
    val t1 = validatorClass
    val t2 = VTQuote(Num(100))
    val e1 = NameConstant(true)
    val e2 = Num(0)
    val valListCls = VTClass("ssz.List", listOf(t1), listOf(t2))
    val expected = mkCall(CTValue(VTAttr(valListCls, "<ctor>")), listOf(e1, e2))
    assertEquals(expected, compileTimeResolver(mkNameResolver(globals), ex1))
  }
}*/
