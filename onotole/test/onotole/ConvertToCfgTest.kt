package onotole

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ConvertToCfgTest {
  @Test
  fun convertAssign() {
    val f = mkTestFun("test", body = listOf(
        mkAssign(mkName("a", true), mkName("b"))
    ))
    val stmts = cfgToStmts2(f, convertToCFG(f))
    assertEquals(cat("a = b", "return"), toStr(stmts))
  }

  @Test
  fun convertNestedIf() {
    val f = mkTestFun("test", body = listOf(
        If(mkName("t"),
            listOf(If(mkName("t2"),
                listOf(mkAssign(mkName("a"), mkName("b"))),
                listOf(mkAssign(mkName("c"), mkName("d"))),
            ))
        )
    ))
    val cfg = convertToCFG(f)
    val stmts = cfgToStmts2(f, cfg)
    assertEquals(cat(
        "if t:",
        "  if t2:",
        "    a = b",
        "  else:",
        "    c = d",
        "return"
    ), toStr(stmts))
  }

  @Test
  fun convertTwoIfs() {
    val f = mkTestFun("test", body = listOf(
        If(mkName("t"),
            listOf(mkAssign(mkName("a"), mkName("b"))),
            listOf(mkAssign(mkName("c"), mkName("d")))),
        If(mkName("t2"),
            listOf(mkAssign(mkName("e"), mkName("f")))
        )
    ))
    val cfg = convertToCFG(f)
    val stmts = cfgToStmts2(f, cfg)
    assertEquals(cat(
        "if t:",
        "  a = b",
        "else:",
        "  c = d",
        "if t2:",
        "  e = f",
        "return"
    ), toStr(stmts))
  }

  @Test
  fun convertWhileIf() {
    val f = mkTestFun("test", body = listOf(
        While(
            mkName("t"),
            listOf(If(
                mkName("t2"),
                listOf(mkAssign(mkName("a"), mkName("b"))),
                listOf(mkAssign(mkName("c"), mkName("d"))),
            ))
        )
    ))
    val cfg = convertToCFG(f)
    val stmts = cfgToStmts2(f, cfg)
    assertEquals(cat(
        "while t:",
        "  if t2:",
        "    a = b",
        "  else:",
        "    c = d",
        "return"
    ), toStr(stmts))
  }
}