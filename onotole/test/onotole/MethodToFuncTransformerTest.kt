package onotole

import onotole.typelib.TLSig
import onotole.typelib.TLTClass
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class MethodToFuncTransformerTest {

  @Test
  fun transformAssign() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", CTV(ClassVal("A"))))),
        returns = CTV(ClassVal("A")),
        body = listOf(
            Assign(mkName("b", true), mkName("a")),
            Return(mkName("b"))
        )
    )
    val res = MethodToFuncTransformer(f).transform()
    assertEquals(Let(listOf(Keyword("b", mkName("a"))), mkName("b")), res)
  }

  @Test
  fun transformIf() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", CTV(ClassVal("A"))))),
        returns = CTV(ClassVal("A")),
        body = listOf(
            If(mkName("*"),
                listOf(
                    Assign(mkName("b", true), mkName("a")),
                    Return(mkName("b"))
                ),
                listOf(
                    Return(mkName("a"))
                )
            )
        )
    )
    val res = MethodToFuncTransformer(f).transform()
    assertEquals(IfExp(mkName("*"),
        Let(listOf(Keyword("b", mkName("a"))), mkName("b")),
        mkName("a")), res)
  }

  @Test
  fun transform_Assign_If() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", CTV(ClassVal("A"))))),
        returns = CTV(ClassVal("A")),
        body = listOf(
            Assign(mkName("c", true), mkName("a")),
            If(mkName("*"),
                listOf(
                    Assign(mkName("b", true), mkName("c")),
                    Return(mkName("b"))
                ),
                listOf(
                    Return(mkName("c"))
                )
            )
        )
    )
    val res = MethodToFuncTransformer(f).transform()
    assertEquals(
        Let(
            listOf(Keyword("c", mkName("a"))),
            IfExp(mkName("*"),
                Let(
                    listOf(Keyword("b", mkName("c"))),
                    mkName("b")),
                mkName("c"))
        ), res)
  }

  @Test
  fun transform_If_Return() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", CTV(ClassVal("A"))))),
        returns = CTV(ClassVal("A")),
        body = listOf(
            If(mkName("*"),
                listOf(
                    Assign(mkName("b", true), mkName("a"))
                ),
                listOf(
                    Assign(mkName("b", true), Num(1))
                )
            ),
            Return(mkName("b"))
        )
    )
    val res = MethodToFuncTransformer(f).transform()
    assertEquals(
        Let(
            listOf(Keyword("b", IfExp(mkName("*"),
                Let(listOf(Keyword("b", mkName("a"))), mkName("b")),
                Let(listOf(Keyword("b", Num(1))), mkName("b"))
            ))),
            mkName("b")
        ), res)
  }

  @Test
  fun transform_While_Return() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", CTV(ClassVal("A"))))),
        returns = CTV(ClassVal("A")),
        body = listOf(
            Assign(mkName("b", true), Num(0)),
            While(mkName("*"),
                listOf(
                    Assign(mkName("b", true), BinOp(mkName("a"), EBinOp.Add, mkName("b")))
                )
            ),
            Return(mkName("b"))
        )
    )
    val res = MethodToFuncTransformer(f).transform()
    println(pyPrint(res))
    assertEquals(
        Let(
            listOf(Keyword("b", IfExp(mkName("*"),
                Let(listOf(Keyword("b", mkName("a"))), mkName("b")),
                Let(listOf(Keyword("b", Num(1))), mkName("b"))
            ))),
            mkName("b")
        ), res)
  }

  @Test
  fun transform_Loop_Iteration() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", CTV(ClassVal("A"))))),
        returns = CTV(ClassVal("A")),
        body = listOf(
            Assign(mkName("b", true), Num(0)),
            While(mkCall(mkName("has_next"), listOf()),
                listOf(
                    Assign(mkName("i", true), mkCall(mkName("next"), listOf())),
                    Assign(mkName("b", true), BinOp(mkName("i"), EBinOp.Add, mkName("b")))
                )
            ),
            Return(mkName("b"))
        )
    )
    val res = MethodToFuncTransformer(f).transform()
    println(pyPrint(res))
    val cls = TLTClass("pylib.object", listOf())
    val sig = TLSig(listOf(), listOf("coll" to cls, "init" to cls, "lam" to cls), cls)
    assertEquals(
        Let(
            listOf(
                Keyword("b", Num(0)),
                Keyword("b", mkCall(CTV(FuncInst("seq_loop", sig)), listOf(
                )))
            ),
            mkName("b")
        ), res)
  }
}