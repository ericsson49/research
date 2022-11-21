package onotole.static_analysis

import onotole.Arg
import onotole.Arguments
import onotole.CTV
import onotole.ClassVal
import onotole.FuncInst
import onotole.Lambda
import onotole.NamedType
import onotole.PyList
import onotole.SideEffectDetector
import onotole.TypeResolver
import onotole.mkAttribute
import onotole.mkCall
import onotole.mkName
import onotole.typelib.TLSig
import onotole.typelib.TLTClass
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class SideEffectTest {
  @Test
  fun testGetAttr() {
    val typer = TypeResolver.topLevelTyper.updated(mapOf("a" to NamedType("Test")))
    val sed = SideEffectDetector(mapOf("Test" to setOf("g")))
    assertFalse(sed.processNode(mkAttribute(mkName("a"), "f"), typer))
    assertTrue(sed.processNode(mkAttribute(mkName("a"), "g"), typer))
  }
  @Test
  fun testMapLamFresh() {
    val typer = TypeResolver.topLevelTyper
    val sed = SideEffectDetector(mapOf("Test" to setOf("g")))
    val lam = Lambda(args = Arguments(args = listOf(Arg("e", CTV(ClassVal("int"))))), PyList(elts = listOf(mkName("e"))))
    val mapFI = FuncInst("map", TLSig(emptyList(), args = emptyList(), TLTClass("A", emptyList())))
    assertTrue(sed.processNode(mkCall(CTV(mapFI), listOf(lam, PyList(elts = emptyList()))), typer))
  }
  @Test
  fun testMapLamPure() {
    val typer = TypeResolver.topLevelTyper
    val sed = SideEffectDetector(mapOf("Test" to setOf("g")))
    val lam = Lambda(args = Arguments(args = listOf(Arg("e", CTV(ClassVal("int"))))), mkName("e"))
    val mapFI = FuncInst("map", TLSig(emptyList(), args = emptyList(), TLTClass("A", emptyList())))
    assertFalse(sed.processNode(mkCall(CTV(mapFI), listOf(lam, PyList(elts = emptyList()))), typer))
  }
  @Test
  fun testMapLam_GetAttrFresh() {
    val typer = TypeResolver.topLevelTyper
    val sed = SideEffectDetector(mapOf("Test" to setOf("g")))
    val lam = Lambda(args = Arguments(args = listOf(Arg("e", CTV(ClassVal("Test"))))), mkAttribute(mkName("e"), "g"))
    val mapFI = FuncInst("map", TLSig(emptyList(), args = emptyList(), TLTClass("A", emptyList())))
    assertTrue(sed.processNode(mkCall(CTV(mapFI), listOf(lam, PyList(elts = emptyList()))), typer))
  }
}