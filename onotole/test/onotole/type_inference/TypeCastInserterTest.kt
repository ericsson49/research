package onotole.type_inference

import onotole.Assign
import onotole.CTV
import onotole.ClassVal
import onotole.ExprTyper
import onotole.FuncInst
import onotole.NamedType
import onotole.RTType
import onotole.TExpr
import onotole.TypeResolver
import onotole.mkAttribute
import onotole.mkCall
import onotole.mkName
import onotole.pyPrint
import onotole.pyPrintStmt
import onotole.typelib.TLClassDecl
import onotole.typelib.TLClassHead
import onotole.typelib.TLSig
import onotole.typelib.TLTClass
import onotole.typelib.parseClassDescr
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll

internal object TestTypeCastInserter: TypeCastInserter() {
  override fun castFunc(from: RTType, to: RTType): TExpr? {
    return mkName("<cast>[$to]")
  }
}
internal fun getTyper(): ExprTyper {
  val simpleClasses = TypingContext.classes.filterValues { (it.head.noTParams + it.head.noEParams) == 0 }.mapValues { NamedType(it.value.head.name) }
  return TypeResolver.topLevelTyper.updated(simpleClasses)
}
internal class TypeCastInserterTest {
  companion object {
    @BeforeAll @JvmStatic
    fun initTypingCtx() {
      TypingContext.classes["D"] = parseClassDescr("D <: object")
      TypingContext.classes["Cls"] = TLClassDecl(true, TLClassHead("Cls"), null, mapOf(
          "f" to TLTClass("B", emptyList())
      ))
    }
  }
  @Test
  fun testFuncCall() {
    val tlTyper = getTyper()
    val sig = TLSig(emptyList(), listOf("a" to TLTClass("A", emptyList())), TLTClass("B", emptyList()))
    val e = mkCall(CTV(FuncInst("func", sig)), listOf(mkName("a")))
    val typer = tlTyper.updated(listOf(
        "a" to NamedType("D", emptyList())
    ))
    val r = with(TypingContext) {
      TestTypeCastInserter.procExpr(e, NamedType("C"), typer)
    }
    assertEquals("<cast>[C](func(<cast>[A](a)))", pyPrint(r))
  }
  @Test
  fun testCtorCall() {
    val e = mkCall(CTV(ClassVal("Cls")), listOf(mkName("a")))
    val typer = getTyper().updated(listOf(
        "a" to NamedType("D", emptyList())
    ))
    val r = with(TypingContext) {
      TestTypeCastInserter.procExpr(e, NamedType("C"), typer)
    }
    assertEquals("<cast>[C](func(<cast>[A](a)))", pyPrint(r))
  }

  @Test
  fun testAssignName() {
    val (r, _) = with(TypingContext) {
      TestTypeCastInserter.procStmt(Assign(mkName("a", true), mkName("b")), getTyper())
    }
    assertEquals("a = b", pyPrintStmt(r).joinToString("\n"))
  }

  @Test
  fun testAssignAttribute() {
    val typer = getTyper().updated(
        listOf("a" to NamedType("Cls"), "b" to NamedType("B"))
    )
    val (r, _) = with(TypingContext) {
      TestTypeCastInserter.procStmt(Assign(mkAttribute("a", "f", true), mkName("b")), typer)
    }
    assertEquals("a.f = b", pyPrintStmt(r).joinToString("\n"))
  }
}