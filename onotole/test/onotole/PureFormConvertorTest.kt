package onotole

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class PureFormConvertorTest {

  val testFunc = FunctionDef("", Arguments())
  val testConvertor = PureFormConvertor(testFunc, emptyMap())

  @Test
  fun assign_to_Attr() {
    val s = mkAssign(mkAttribute("a", "b"), Num(1))
    assertEquals("a = a.updated(b = 1)", toStr(testConvertor.transformStmt(s) ?: listOf(s)))
  }

  @Test
  fun assign_to_Attr_alias() {
    val s = mkAssign(mkAttribute("b", "g"), Num(1))
    val aliasInfo = mapOf<String, List<Pair<String, TExpr>>>(
        "a" to listOf("b" to mkAttribute(mkName("a"), "f"))
    )
    val testConvertor = PureFormConvertor(testFunc, emptyMap(), aliasInfo)
    val res = testConvertor.transformStmt(s) ?: listOf(s)
    assertEquals("b = b.updated(g = 1)\na = a.updated(f = b)", toStr(res))
  }

  @Test
  fun assign_to_Attr_alias2() {
    val s = mkAssign(mkAttribute("b", "g"), Num(1))
    val aliasInfo = mapOf<String, List<Pair<String, TExpr>>>(
        "a" to listOf("b" to mkSubscript(mkName("a"), Index(mkName("i"))))
    )
    val testConvertor = PureFormConvertor(testFunc, emptyMap(), aliasInfo)
    val res = testConvertor.transformStmt(s) ?: listOf(s)
    assertEquals("b = b.updated(g = 1)\na = a.updated_at(i, b)", toStr(res))
  }

  @Test
  fun assign_to_Attr_Attr() {
    val s = mkAssign(mkAttribute(mkAttribute("a", "b"), "c"), Num(1))
    assertEquals("a = a.updated(b = a.b.updated(c = 1))", toStr(testConvertor.transformStmt(s) ?: listOf(s)))
  }

  @Test
  fun aug_assign() {
    val s = AugAssign(mkAttribute("a", "b"), EBinOp.Add, Num(1))
    assertEquals("a = a.updated(b = a.b + 1)", toStr(testConvertor.transformStmt(s) ?: listOf(s)))
  }

  @Test
  fun transformLVal_Subscript_Index() {
    val s = mkAssign(mkSubscript("a", Index(mkName("i"))), Num(1))
    assertEquals("a = a.updated_at(i, 1)", toStr(testConvertor.transformStmt(s) ?: listOf(s)))
  }

  @Test
  fun transformLVal_Subscript_Index_alias() {
    val s = mkAssign(mkSubscript("b", Index(mkName("i"))), Num(1))
    val aliasInfo = mapOf<String, List<Pair<String, TExpr>>>(
        "a" to listOf("b" to mkAttribute(mkName("a"), "f"))
    )
    val testConvertor = PureFormConvertor(testFunc, emptyMap(), aliasInfo)
    assertEquals("b = b.updated_at(i, 1)\na = a.updated(f = b)", toStr(testConvertor.transformStmt(s) ?: listOf(s)))
  }

  @Test
  fun transformLVal_Subscript_Index_Attr() {
    val s = mkAssign(mkAttribute(mkSubscript("a", Index(mkName("i"))), "b"), Num(1))
    assertEquals("a = a.updated_at(i, a[i].updated(b = 1))", toStr(testConvertor.transformStmt(s) ?: listOf(s)))
  }

  @Test
  fun transformLVal_Attr_Subscript_Index() {
    val s = mkAssign(mkSubscript(mkAttribute("a", "b"), Index(mkName("i"))), Num(1))
    assertEquals("a = a.updated(b = a.b.updated_at(i, 1))", toStr(testConvertor.transformStmt(s) ?: listOf(s)))
  }

  @Test
  fun transformExpr_procedure() {
    val descrs = mapOf("test" to PurityDescriptor(true, listOf(0), "test_pure"))
    val s = Expr(mkCall("test", listOf(mkName("state"))))
    assertEquals("state = test_pure(state)", toStr(PureFormConvertor(testFunc, descrs).transformStmt(s) ?: listOf(s)))
  }

  @Test
  fun transformExpr_func_no_receiver() {
    val descrs = mapOf("test" to PurityDescriptor(false, listOf(0), "test_pure"))
    val s = Expr(mkCall("test", listOf(mkName("state"))))
    assertEquals("(state, _) = test_pure(state)", toStr(PureFormConvertor(testFunc, descrs).transformStmt(s) ?: listOf(s)))
  }

  @Test
  fun transformExpr_func_with_name() {
    val descrs = mapOf("test" to PurityDescriptor(false, listOf(0), "test_pure"))
    val s = Assign(mkName("res", true), mkCall("test", listOf(mkName("state"))))
    assertEquals("(state, res) = test_pure(state)", toStr(PureFormConvertor(testFunc, descrs).transformStmt(s) ?: listOf(s)))
  }

  @Test
  fun transformExpr_func_with_attr() {
    val descrs = mapOf("test" to PurityDescriptor(false, listOf(0), "test_pure"))
    val s = Assign(mkName("res", true), mkCall("test", listOf(mkAttribute("state", "validators"))))
    assertEquals("(tmp, res) = test_pure(state.validators)\nstate = state.updated(validators = tmp)", toStr(PureFormConvertor(testFunc, descrs).transformStmt(s) ?: listOf(s)))
  }

  @Test
  fun transformProc_no_arg() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", mkName("A")))),
        returns = NameConstant(null),
        body = listOf(
          mkAssign(mkName("a"), Num(1))
        )
    )
    val descrs = mapOf("test" to PurityDescriptor(true, emptyList(), "test_pure"))
    val pf = PureFormConvertor(f, descrs).transformFunction()
    assertEquals("test_pure", pf.name)
    assertEquals(f.args, pf.args)
    assertEquals(null, pf.returns)
    assertEquals("a = 1", toStr(pf.body))
  }

  @Test
  fun transformProc_one_arg() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", mkName("A")))), body = listOf(
        mkAssign(mkAttribute(mkName("a"), "f"), Num(1))
    ))
    val descrs = mapOf("test" to PurityDescriptor(true, listOf(0), "test_pure"))
    val pf = PureFormConvertor(f, descrs).transformFunction()
    assertEquals("test_pure", pf.name)
    assertEquals(f.args, pf.args)
    assertEquals(mkName("A"), pf.returns)
    assertEquals("a = a.updated(f = 1)\nreturn a", toStr(pf.body))
  }

  @Test
  fun transformProc_one_arg_return() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", mkName("A")))),
        returns = NameConstant(null),
        body = listOf(
            mkAssign(mkAttribute(mkName("a"), "f"), Num(1)),
            Return()
        )
    )
    val descrs = mapOf("test" to PurityDescriptor(true, listOf(0), "test_pure"))
    val pf = PureFormConvertor(f, descrs).transformFunction()
    assertEquals("test_pure", pf.name)
    assertEquals(f.args, pf.args)
    assertEquals(mkName("A"), pf.returns)
    assertEquals("a = a.updated(f = 1)\nreturn a", toStr(pf.body))
  }

  @Test
  fun transformProc_two_args() {
    val f = FunctionDef("test", Arguments(args = listOf(
        Arg("a", mkName("A")), Arg("b", mkName("B")))),
        returns = NameConstant(null),
        body = listOf(
          mkAssign(mkAttribute(mkName("a"), "f"), Num(1)),
          mkAssign(mkAttribute("b", "g"), Num(2))
        )
    )
    val descrs = mapOf("test" to PurityDescriptor(true, listOf(0,1), "test_pure"))
    val pf = PureFormConvertor(f, descrs).transformFunction()
    assertEquals("test_pure", pf.name)
    assertEquals(f.args, pf.args)
    assertEquals("Tuple[A, B]", pyPrintType(pf.returns!!))
    assertEquals("a = a.updated(f = 1)\nb = b.updated(g = 2)\nreturn (a, b)", toStr(pf.body))
  }

  @Test
  fun transformFunc_no_arg() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", mkName("A")))),
        returns = mkName("B"),
        body = listOf(
            mkAssign(mkName("a"), Num(1)),
            Return(mkCall("B", emptyList()))
        )
    )
    val descrs = mapOf("test" to PurityDescriptor(false, listOf(), "test_pure"))
    val pf = PureFormConvertor(f, descrs).transformFunction()
    assertEquals("test_pure", pf.name)
    assertEquals(f.args, pf.args)
    assertEquals("B", pyPrintType(pf.returns!!))
    assertEquals("a = 1\nreturn B()", toStr(pf.body))
  }

  @Test
  fun transformFunc_one_arg() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", mkName("A")))),
        returns = mkName("B"),
        body = listOf(
            mkAssign(mkAttribute(mkName("a"), "f"), Num(1)),
            Return(mkCall("B", emptyList()))
        )
    )
    val descrs = mapOf("test" to PurityDescriptor(false, listOf(0), "test_pure"))
    val pf = PureFormConvertor(f, descrs).transformFunction()
    assertEquals("test_pure", pf.name)
    assertEquals(f.args, pf.args)
    assertEquals("Tuple[A, B]", pyPrintType(pf.returns!!))
    assertEquals("a = a.updated(f = 1)\nreturn (a, B())", toStr(pf.body))
  }

  @Test
  fun transformFunc_if_stmt() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", mkName("A")))),
        returns = mkName("B"),
        body = listOf(
            If(NameConstant("True"),
                body = listOf(
                    mkAssign(mkAttribute(mkName("a"), "f"), Num(1)),
                    Return(mkCall("B", emptyList()))
                ),
                orelse = listOf(
                    mkAssign(mkAttribute(mkName("a"), "f"), Num(2)),
                    Return(mkCall("B", emptyList()))
                )
            )
        )
    )
    val descrs = mapOf("test" to PurityDescriptor(false, listOf(0), "test_pure"))
    val pf = PureFormConvertor(f, descrs).transformFunction()
    assertEquals("test_pure", pf.name)
    assertEquals(f.args, pf.args)
    assertEquals("Tuple[A, B]", pyPrintType(pf.returns!!))
    assertEquals(cat(
        "if True:",
        "  a = a.updated(f = 1)",
        "  return (a, B())",
        "else:",
        "  a = a.updated(f = 2)",
        "  return (a, B())"
    ),
        toStr(pf.body))

  }

  @Test
  fun transformProc_if_stmt() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", mkName("A")))),
        returns = NameConstant(null),
        body = listOf(
            If(NameConstant("True"),
                body = listOf(
                    mkAssign(mkAttribute(mkName("a"), "f"), Num(1)),
                    Return()
                ),
                orelse = listOf(
                    mkAssign(mkAttribute(mkName("a"), "f"), Num(2)),
                    Return()
                )
            )
        )
    )
    val descrs = mapOf("test" to PurityDescriptor(true, listOf(0), "test_pure"))
    val pf = PureFormConvertor(f, descrs).transformFunction()
    assertEquals("test_pure", pf.name)
    assertEquals(f.args, pf.args)
    assertEquals("A", pyPrintType(pf.returns!!))
    assertEquals(cat(
        "if True:",
        "  a = a.updated(f = 1)",
        "  return a",
        "else:",
        "  a = a.updated(f = 2)",
        "  return a",
        "return a"
    ),
        toStr(pf.body))

  }

  @Test
  fun transformProc_AttrCall() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", mkName("A")))),
        returns = NameConstant(null),
        body = listOf(
            Expr(mkCall(mkAttribute("a", "append"), listOf(Num(1))))
        )
    )
    val descrs = mapOf(
        "test" to PurityDescriptor(true, listOf(0), "test_pure"),
        "attr_append" to PurityDescriptor(true, listOf(0), "attr_append_pure")
    )
    val pf = PureFormConvertor(f, descrs).transformFunction()
    assertEquals("test_pure", pf.name)
    assertEquals(f.args, pf.args)
    assertEquals("A", pyPrintType(pf.returns!!))
    assertEquals(cat("a = attr_append_pure(a, 1)", "return a"), toStr(pf.body))
  }

  @Test
  fun transform_alias1() {
    val f = FunctionDef("test", Arguments(args = listOf(Arg("a", mkName("A")))), body = listOf(
        mkAssign(mkName("b", true), mkAttribute("a", "f")),
        mkAssign(mkAttribute("b", "g"), Num(1))
    ))
    val cfg = convertToCFG(f)
    val ssa = convertToSSA(cfg)
    val descr = PurityDescriptor(true, listOf(0), "test_pure")
    val descrs = mapOf("test" to descr)
    val aliasInfo = findMutableAliases(f, ssa, descr)
    val pf = PureFormConvertor(f, descrs, aliasInfo).transformFunction()
    assertEquals("test_pure", pf.name)
    assertEquals(f.args, pf.args)
    assertEquals(mkName("A"), pf.returns)
    assertEquals("b = a.f\nb = b.updated(g = 1)\na = a.updated(f = b)\nreturn a", toStr(pf.body))
  }

  fun toStmt(p: Pair<List<TExpr>, TExpr>) = toStmt(p.first, p.second)
  fun toStmt(tgts: List<TExpr>, e: TExpr): Stmt = when {
    tgts.isEmpty() -> Expr(e)
    tgts.size == 1 -> Assign(target = tgts[0], e)
    else -> Assign(target = Tuple(tgts, ExprContext.Store), e)
  }
}