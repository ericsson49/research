package onotole

import onotole.lib_defs.PyLib
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class LoopToRecFuncConverterTest {
  companion object {
    @BeforeAll @JvmStatic fun setUp() {
      PyLib.init()
    }

  }
  @Test
  fun single_loop() {
    val f = mkTestFun0("test", listOf(Arg("k", mkName("int"))),body = listOf(
        Assign(mkName("r", true), Num(0)),
        Assign(mkName("i", true), Num(0)),
        While(Compare(mkName("i"), listOf(ECmpOp.Lt), listOf(mkName("k"))), listOf(
            AugAssign(mkName("r", true), EBinOp.Add, Num(2)),
            AugAssign(mkName("i", true), EBinOp.Add, Num(1))
        )),
        Assert(Compare(mkName("r"), listOf(ECmpOp.Eq), listOf(BinOp(Num(2), EBinOp.Mult, mkName("k")))))
    ))

    val res = transformLoopsToFuncs(f)
    assertEquals(2, res.size)
    val f1 = res[0]
    val f2 = res[1]
    pyPrintFunc(f1)
    pyPrintFunc(f2)
  }

  @Test
  fun two_loops() {
    val f = mkTestFun0("test", listOf(Arg("k", mkName("int"))),body = listOf(
        Assign(mkName("r", true), Num(0)),
        Assign(mkName("i", true), Num(0)),
        While(Compare(mkName("i"), listOf(ECmpOp.Lt), listOf(mkName("k"))), listOf(
            AugAssign(mkName("r", true), EBinOp.Add, Num(2)),
            AugAssign(mkName("i", true), EBinOp.Add, Num(1))
        )),
        Assign(mkName("j", true), Num(0)),
        While(Compare(mkName("j"), listOf(ECmpOp.Lt), listOf(mkName("k"))), listOf(
            AugAssign(mkName("j", true), EBinOp.Add, Num(1))
        )),
        Assert(Compare(mkName("r"), listOf(ECmpOp.Eq), listOf(BinOp(Num(2), EBinOp.Mult, mkName("k")))))
    ))

    val res = transformLoopsToFuncs(f)
    assertEquals(3, res.size)
    val f1 = res[0]
    val f2 = res[1]
    val f3 = res[2]
    pyPrintFunc(f1)
    pyPrintFunc(f2)
    pyPrintFunc(f3)
  }

  @Test
  fun nested_loops() {
    val f = mkTestFun0("test", listOf(Arg("k", mkName("int"))),body = listOf(
        Assign(mkName("r", true), Num(0)),
        Assign(mkName("i", true), Num(0)),
        While(Compare(mkName("i"), listOf(ECmpOp.Lt), listOf(mkName("k"))), listOf(
            Assign(mkName("j", true), Num(0)),
            While(Compare(mkName("j"), listOf(ECmpOp.Lt), listOf(mkName("k"))), listOf(
                AugAssign(mkName("j", true), EBinOp.Add, Num(1))
            )),
            AugAssign(mkName("r", true), EBinOp.Add, Num(2)),
            AugAssign(mkName("i", true), EBinOp.Add, Num(1))
        )),
        Assert(Compare(mkName("r"), listOf(ECmpOp.Eq), listOf(BinOp(Num(2), EBinOp.Mult, mkName("k")))))
    ))

    val res = transformLoopsToFuncs(f)
    assertEquals(3, res.size)
    val f1 = res[0]
    val f2 = res[1]
    val f3 = res[2]
    pyPrintFunc(f1)
    pyPrintFunc(f2)
    pyPrintFunc(f3)
  }
}