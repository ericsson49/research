package onotole

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CompileTimeCalcTest {
  @Test fun testClassHandle() {
    val globals = mapOf(
        "Validator" to VTClass("phase0.Validator", emptyList(), emptyList()),
    )
    val e = mkName("Validator")
    assertEquals(LExtVal("phase0.Validator"), compileTimeEval(globals, e))

  }
  @Test fun testTemplateHandle() {
    val globals = mapOf(
        "List" to VTClassTemplate("ssz.List", 1, 1),
        "Validator" to VTClass("phase0.Validator", emptyList(), emptyList()),
    )
    // List[Validator,100]
    val e = mkSubscript(mkName("List"), mkIndex(mkName("Validator"), Num(100)))
    assertEquals(LExtVal("ssz.List", listOf(LExtVal("phase0.Validator"), LQuote(Num(100)))), compileTimeEval(globals, e))

  }
  @Test fun testClassInstantiation() {
    val globals = mapOf(
        "Validator" to VTClass("phase0.Validator", emptyList(), emptyList()),
    )
    // Validator()
    val e = mkCall(mkName("Validator"), emptyList())
    assertEquals(LApp(LOp("phase0.Validator#<ctor>"), emptyList()), compileTimeEval(globals, e))
  }

  @Test fun templateInstantiation() {
    val globals = mapOf(
        "List" to VTClassTemplate("ssz.List", 1, 1),
        "Validator" to VTClass("phase0.Validator", emptyList(), emptyList())
    )
    // List[Validator,100](a=True,b=0)
    val ex1 = Call(
        mkSubscript(mkName("List"), mkIndex(mkName("Validator"), Num(100))),
        listOf(), listOf(Keyword("a", NameConstant(true)), Keyword("b", Num(0)))
    )
    val t1 = LExtVal("phase0.Validator")
    val t2 = LQuote(Num(100))
    val e1 = LQuote(NameConstant(true))
    val e2 = LQuote(Num(0))
    val expected = LApp(LOp("ssz.List#<ctor>", listOf(t1, t2)), listOf(e1, e2))
    assertEquals(expected, compileTimeEval(globals, ex1))
  }
}