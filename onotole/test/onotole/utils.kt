package onotole

fun mkTestFun0(name: String, args: List<Arg> = emptyList(), body: List<Stmt> = emptyList()): FunctionDef {
  return FunctionDef(name, args = Arguments(args = args), body = body)
}
fun mkTestFun(name: String, args: List<String> = emptyList(), body: List<Stmt> = emptyList()): FunctionDef {
  return mkTestFun0(name, args.map { Arg(it) }, body = body)
}

fun toStr(ls: Iterable<Stmt>) = ls.flatMap(::pyPrintStmt).joinToString("\n")
fun cat(vararg s: String): String = s.joinToString("\n")
