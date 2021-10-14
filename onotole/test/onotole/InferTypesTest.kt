package onotole

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class InferTypesTest {
  @Test
  fun constant_int() {
    val f = mkTestFun("test", body = listOf(
        mkAssign(mkName("a", true), Num(1))
    ))
    val res = inferTypes_FD(f)
    assertEquals(mapOf("a" to TPyInt), res)
  }
}