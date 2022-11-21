package onotole.rewrite

import onotole.Attribute
import onotole.Constant
import onotole.FreshNames
import onotole.IfExp
import onotole.Index
import onotole.Keyword
import onotole.Lambda
import onotole.Let
import onotole.Name
import onotole.Subscript
import onotole.TExpr
import onotole.mkName
import onotole.util.deconstruct
import onotole.util.deconstructS

fun isSemanticallySimple(e: TExpr) = e is Constant || e is Name || e is Lambda

fun makeReductions(e: TExpr, freshNames: FreshNames, testF: (TExpr) -> Boolean): TExpr {
  val (exprs, assembler) = deconstructS(e)
  val kwds = mutableListOf<Keyword>()
  val newExprs = exprs.map {
    if (testF(it)) {
      val v = freshNames.fresh("tmp_")
      kwds.add(Keyword(v, it))
      mkName(v)
    } else it
  }
  return if (kwds.isNotEmpty())
    Let(kwds, assembler.assemble(newExprs))
  else e
}