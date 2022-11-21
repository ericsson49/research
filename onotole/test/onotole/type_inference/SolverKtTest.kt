package onotole.type_inference

import onotole.FreshNames
import org.junit.jupiter.api.Test
import onotole.type_inference.st as st_
import onotole.type_inference.join as join_
import onotole.type_inference.solve as solve_

import org.junit.jupiter.api.Assertions.*

internal class SolverKtTest {
  fun st(a: FAtom, b: FAtom): Pair<Boolean,Set<Constraint>> {
    return with(TypingContext) { st_(a, b) }
  }
  fun join(a: FAtom?, b: FAtom?, fn: FreshNames): Pair<FAtom?,List<Constraint>> {
    return with(TypingContext) { join_(a, b, fn) }
  }
  fun solve(cs: Collection<Constraint>, fn: FreshNames): Map<FVar,FAtom> {
    return with(TypingContext) { solve_(cs, fn) }
  }
    @Test
  fun subType_list() {
    val aList = FAtom("list", listOf(FVar("A")))
    val bList = FAtom("list", listOf(FVar("B")))
    assertEquals(true to setOf(FVar("A") to FVar("B")), st(aList, bList))
    val aSeq = FAtom("seq", listOf(FVar("A")))
    val bSeq = FAtom("seq", listOf(FVar("B")))
    assertEquals(true to setOf(FVar("A") to FVar("B")), st(aList, bSeq))
    assertEquals(true to setOf(FVar("A") to FVar("B")), st(aSeq, bSeq))
  }

  @Test
  fun join_list() {
    val aList = FAtom("list", listOf(FVar("A")))
    val bList = FAtom("list", listOf(FVar("B")))
    assertEquals(
        FAtom("list", listOf(FVar("TJ_0")))
            to setOf(FVar("A") to FVar("TJ_0"), FVar("B") to FVar("TJ_0")), join(aList, bList, FreshNames()))
    val aSeq = FAtom("seq", listOf(FVar("A")))
    val bSeq = FAtom("seq", listOf(FVar("B")))
    assertEquals(
        FAtom("seq", listOf(FVar("TJ_0")))
            to setOf(FVar("A") to FVar("TJ_0"), FVar("B") to FVar("TJ_0")), join(aSeq, bSeq, FreshNames()))
    assertEquals(
        FAtom("seq", listOf(FVar("TJ_0")))
            to setOf(FVar("A") to FVar("TJ_0"), FVar("B") to FVar("TJ_0")), join(aList, bSeq, FreshNames()))
    assertEquals(
        FAtom("seq", listOf(FVar("TJ_0")))
            to setOf(FVar("A") to FVar("TJ_0"), FVar("B") to FVar("TJ_0")), join(aSeq, bList, FreshNames()))
  }

  @Test
  fun solve1() {
    val cs = listOf(
        FAtom("int") to FVar("A"),
        FAtom("int") to FVar("A")
    )
    assertEquals(mapOf(FVar("A") to FAtom("int")), solve(cs, FreshNames()))
  }
  @Test
  fun solve2() {
    val cs = listOf(
        FAtom("uint") to FVar("A"),
        FAtom("int") to FVar("A")
    )
    assertEquals(mapOf(FVar("A") to FAtom("int")), solve(cs, FreshNames()))
  }
  @Test
  fun solve3() {
    val cs = listOf(
        FAtom("uint") to FVar("A"),
        FAtom("bool") to FVar("A")
    )
    assertEquals(mapOf(FVar("A") to FAtom("int")), solve(cs, FreshNames()))
  }
  @Test
  fun solve4() {
    val cs = listOf(
        FAtom("uint") to FVar("A"),
        FAtom("str") to FVar("A")
    )
    assertEquals(mapOf(FVar("A") to FAtom("object")), solve(cs, FreshNames()))
  }
  @Test
  fun solveCycle1() {
    val cs = listOf(
        FVar("A") to FVar("B"),
        FVar("B") to FVar("A"),
        FAtom("int") to FVar("A"),
    )
    assertEquals(mapOf(FVar("A") to FAtom("int"), FVar("B") to FAtom("int")), solve(cs, FreshNames()))
  }

  @Test
  fun solveList1() {
    val cs = listOf(
        FAtom("list", listOf(FAtom("uint"))) to FVar("A"),
        FAtom("list", listOf(FAtom("bool"))) to FVar("A"),
    )
    assertEquals(
        mapOf(
            FVar("A") to FAtom("list", listOf(FVar("TJ_0"))),
            FVar("TJ_0") to FAtom("int")
        ),
        solve(cs, FreshNames()))
  }
}