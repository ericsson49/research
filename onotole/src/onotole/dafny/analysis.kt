package onotole.dafny

import onotole.For
import onotole.FunctionDef
import onotole.SimpleInterprocAnalysis
import onotole.Stmt
import onotole.StmtVisitor
import onotole.While

class DafnyLoopsAnalysis(_funcs: Collection<FunctionDef>): SimpleInterprocAnalysis<Boolean>(_funcs) {
  override val bottom = true
  override fun join(a: Boolean, b: Boolean) = a && b
  override fun getInitial(n: String): Boolean {
    val f = funcMap[n]!!
    var hasLoops = false
    object : StmtVisitor<Unit>() {
      override fun visitStmt(s: Stmt, ctx: Unit) {
        if (s is For || s is While)
          hasLoops = true
      }
    }.procStmts(f.body, Unit)
    return hasLoops
  }
}
