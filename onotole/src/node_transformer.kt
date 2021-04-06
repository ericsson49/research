import java.nio.file.Files
import java.nio.file.Paths


abstract class SimpleStmtTransformer {
  abstract fun doTransform(s: Stmt): List<Stmt>
  fun transform(c: List<Stmt>): List<Stmt> {
    val res = mutableListOf<Stmt>()
    var foundNew = false
    for(s in c) {
      val newS = transform(s)
      if (newS.size != 1 || newS[0] != s) {
        foundNew = true
      }
      res.addAll(newS)
    }
    return if (foundNew) res else c
  }
  fun transform(s: Stmt): List<Stmt> {
    val res = doTransform(s)
    if (res.size != 1 || res[0] != s) {
      return res
    } else {
      return listOf(when (s) {
        is If -> {
          val newBody = transform(s.body)
          val newOrelse = transform(s.orelse)
          if (newBody == s.body && newOrelse == s.orelse) s
          else s.copy(body = newBody, orelse = newOrelse)
        }
        is While -> {
          val newBody = transform(s.body)
          val newOrelse = transform(s.orelse)
          if (newBody == s.body && newOrelse == s.orelse) s
          else s.copy(body = newBody, orelse = newOrelse)
        }
        is For -> {
          val newBody = transform(s.body)
          val newOrelse = transform(s.orelse)
          if (newBody == s.body && newOrelse == s.orelse) s
          else s.copy(body = newBody, orelse = newOrelse)
        }
        else -> s
      })
    }
  }
  fun transform(f: FunctionDef): FunctionDef {
    val newBody = transform(f.body)
    return if (newBody == f.body) f else f.copy(body = newBody)
  }
}

object ForOpsTransformer: SimpleStmtTransformer() {
  override fun doTransform(s: Stmt): List<Stmt> = when {
    s is FunctionDef -> emptyList()
    s is Expr && s.value is Call && s.value.func is Name && s.value.func.id == "for_ops" && s.value.args.size == 2 && s.value.keywords.isEmpty() -> {
      listOf(For(Name("operation", ExprContext.Store), s.value.args[0],
              listOf(Expr(Call(
                      s.value.args[1],
                      listOf(Name("state", ExprContext.Load), Name("operation", ExprContext.Load)),
                      emptyList()
              )))))
    }
    else -> listOf(s)
  }
}

fun transformForOps(f: FunctionDef) = ForOpsTransformer.transform(f)

fun main() {
  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_phase0_dev.txt")
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  val fDefs = parsed.map { toStmt(it) }.filterIsInstance<FunctionDef>()
  fDefs.forEach {
    pyPrintFunc(transformForOps(it))
    println()
  }
}
