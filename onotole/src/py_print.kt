fun pyPrint(op: EBinOp) = when(op) {
  EBinOp.Add -> "+"
  EBinOp.Sub -> "-"
  EBinOp.Mult -> "*"
  EBinOp.Div -> "/"
  EBinOp.FloorDiv -> "//"
  EBinOp.Mod -> "%"
  else -> fail("not implemented $op")
}

fun pyPrint(op: EBoolOp) = when(op) {
  EBoolOp.And -> "and"
  EBoolOp.Or -> "or"
}

fun pyPrint(op: ECmpOp) = when(op) {
  ECmpOp.Is -> "is"
  ECmpOp.IsNot -> "is not"
  ECmpOp.In -> "in"
  ECmpOp.NotIn -> "not in"
  ECmpOp.Eq -> "=="
  ECmpOp.NotEq -> "!="
  ECmpOp.Gt -> ">"
  ECmpOp.GtE -> ">="
  ECmpOp.Lt -> "<"
  ECmpOp.LtE -> "<="
}

fun pyPrint(e: TExpr): String {
  return when(e) {
    is Name -> e.id
    is Num -> e.n.toString()
    is NameConstant -> e.value.toString()
    is Tuple -> e.elts.joinToString(", ", "(", ")") { pyPrint(it) }
    is PyList -> e.elts.joinToString(", ", "[", "]") { pyPrint(it) }
    is PyDict -> e.keys.zip(e.values)
        .joinToString(", ", "{", "}") { pyPrint(it.first) + ": " + pyPrint(it.second) }
    is PySet -> e.elts.joinToString(", ", "set(", ")") { pyPrint(it) }
    is BinOp -> pyPrint(e.left) + " " + pyPrint(e.op) + " " + pyPrint(e.right)
    is BoolOp -> e.values.joinToString(" " + pyPrint(e.op) + " ") { pyPrint(it) }
    is Compare -> pyPrint(e.left) + e.ops.zip(e.comparators).joinToString { " " + pyPrint(it.first) + " " + pyPrint(it.second) }
    is Call -> pyPrint(e.func) + "(" + e.args.map { pyPrint(it) }.union(e.keywords.map { "${it.arg} = ${pyPrint(it.value)}" }).joinToString(", ") + ")"
    is Subscript -> pyPrint(e.value) + "[" + pyPrint(e.slice) + "]"
    else -> fail("unsupported $e")
  }
}

fun pyPrint(slice1: TSlice) = when (slice1) {
  is Index -> pyPrint(slice1.value)
  is Slice -> (slice1.lower?.let { listOf(pyPrint(it)) } ?: emptyList())
      .union(slice1.upper?.let { listOf(pyPrint(it)) } ?: emptyList())
      .union(slice1.step?.let { listOf(pyPrint(it)) } ?: emptyList())
      .joinToString(":")
  else -> fail("not supported $slice1")
}

fun pyPrintStmt(s: Stmt): List<String> {
  return when(s) {
    is Assign -> listOf(s.targets.joinToString(", ") { pyPrint(it) } + " = " + pyPrint(s.value))
    is AugAssign -> listOf(pyPrint(s.target) + " ${pyPrint(s.op)}= " + pyPrint(s.value))
    is If -> {
      val res = mutableListOf("if " + pyPrint(s.test) + ":")
      res.addAll(s.body.flatMap { pyPrintStmt(it).map { "  $it" } })
      if (s.orelse.isNotEmpty()) {
        res.add("else:")
        res.addAll(s.orelse.flatMap { pyPrintStmt(it).map { "  $it" } })
      }
      res.toList()
    }
    is While -> {
      val res = mutableListOf("while " + pyPrint(s.test) + ":")
      res.addAll(s.body.flatMap { pyPrintStmt(it).map { "  $it" } })
      res.toList()
    }
    is For -> {
      val res = mutableListOf("for " + pyPrint(s.target) + " in " + pyPrint(s.iter) + ":")
      res.addAll(s.body.flatMap { pyPrintStmt(it).map { "  $it" } })
      res.toList()
    }
    is Return -> listOf("return" + (s.value?.let { " " + pyPrint(it) } ?: ""))
    else -> fail("unsupported $s")
  }
}

fun pyPrintType(e: TExpr): String = when(e) {
  is Name -> e.id
  is Subscript -> pyPrintType(e.value) + "[" + pyPrintType((e.slice as Index).value) + "]"
  else -> fail("not implemented $e")
}

fun pyPrint(a: Arg) = a.arg + (a.annotation?.let { ": " + pyPrintType(it) } ?: "")
fun pyPrintFunc(f: FunctionDef) {
  val startIndex = f.args.posonlyargs.size + f.args.args.size - f.args.defaults.size
  val posOnlyArgs = f.args.posonlyargs.mapIndexed { i, a ->
    pyPrint(a) + (if (i >= startIndex) " = " + pyPrint(f.args.defaults[i - startIndex]) else "")
  }
  val args = f.args.args.mapIndexed { i, a ->
    pyPrint(a) + (if (f.args.posonlyargs.size + i >= startIndex) " = " + pyPrint(f.args.defaults[i - startIndex]) else "")
  }
  val kwOnlyArgs = f.args.kwonlyargs.mapIndexed { i, a ->
    pyPrint(a) + " = " + pyPrint(f.args.kw_defaults[i])
  }
  val funcArgs = mutableListOf<String>()
  if (posOnlyArgs.isNotEmpty()) {
    funcArgs.addAll(posOnlyArgs)
    funcArgs.add("/")
  }
  funcArgs.addAll(args)
  if (f.args.vararg != null || kwOnlyArgs.isNotEmpty()) {
    funcArgs.add("*" + (f.args.vararg?.let { pyPrint(it) } ?: ""))
  }
  funcArgs.addAll(kwOnlyArgs)
  if (f.args.kwarg != null) {
    funcArgs.add("**" + pyPrint(f.args.kwarg))
  }
  println("def " + f.name + "(" + funcArgs.joinToString(", ") + ")"
      + (f.returns?.let { " -> " + pyPrintType(it) } ?: "") + ":")
  f.body.flatMap { pyPrintStmt(it).map { "  $it" } }.forEach { println(it) }
}