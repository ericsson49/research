package onotole

import onotole.util.toTExpr

fun pyPrint(op: EBinOp) = when(op) {
  EBinOp.Add -> "+"
  EBinOp.Sub -> "-"
  EBinOp.Mult -> "*"
  EBinOp.Div -> "/"
  EBinOp.FloorDiv -> "//"
  EBinOp.Mod -> "%"
  EBinOp.Pow -> "**"
  EBinOp.BitXor -> "^"
  EBinOp.BitOr -> "|"
  EBinOp.BitAnd -> "&"
  EBinOp.RShift -> ">>"
  EBinOp.LShift -> "<<"
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

fun pyPrint(op: EUnaryOp) = when(op) {
  EUnaryOp.Not -> "not"
  EUnaryOp.Invert -> "~"
  EUnaryOp.UAdd -> "+"
  EUnaryOp.USub -> "-"
}
fun pyPrint(c: Comprehension): String = "for " + pyPrint(c.target) + " in " + pyPrint(c.iter) + c.ifs.joinToString { " if " + pyPrint(it) }

fun pyPrint(e: TExpr): String {
  return when(e) {
    is Name -> e.id
    is Num -> e.n.toString()
    is NameConstant -> e.value.toString()
    is Str -> "\"\"" + e.s + "\"\""
    is Bytes -> "b" + e.s
    is Tuple -> e.elts.joinToString(", ", "(", ")") { pyPrint(it) }
    is PyList -> e.elts.joinToString(", ", "[", "]") { pyPrint(it) }
    is PyDict -> e.keys.zip(e.values)
        .joinToString(", ", "{", "}") { pyPrint(it.first) + ": " + pyPrint(it.second) }
    is PySet -> if (e.elts.isEmpty()) "set()" else e.elts.joinToString(", ", "{", "}") { pyPrint(it) }
    is BinOp -> pyPrint(e.left) + " " + pyPrint(e.op) + " " + pyPrint(e.right)
    is BoolOp -> e.values.joinToString(" " + pyPrint(e.op) + " ") { pyPrint(it) }
    is Compare -> pyPrint(e.left) + e.ops.zip(e.comparators).joinToString { " " + pyPrint(it.first) + " " + pyPrint(it.second) }
    is UnaryOp -> pyPrint(e.op) + " " + pyPrint(e.operand)
    is Call -> {
      if (e.func is Name && e.func.id in TypeResolver.specialFuncNames) {
        val op = e.func.id.substring(1,e.func.id.length-1)
        val args = e.args
        val expr = when(op) {
          "list" -> PyList(args)
          "dict" -> {
            val (keys, values) = args.map { t ->
              t as Tuple
              t.elts[0] to t.elts[1]
            }.unzip()
            PyDict(keys, values)
          }
          in TypeResolver.binOps -> if (e.args.size == 2) BinOp(args[0], EBinOp.valueOf(op), args[1]) else fail()
          in TypeResolver.boolOps -> BoolOp(EBoolOp.valueOf(op), args)
          in TypeResolver.unaryOp -> if (args.size == 1) UnaryOp(EUnaryOp.valueOf(op), args[0]) else fail()
          in TypeResolver.cmpOps -> if (args.size == 2) Compare(args[0], listOf(ECmpOp.valueOf(op)), listOf(args[1])) else fail()
          else -> TODO()
        }
        pyPrint(expr)
      } else {
        val func = pyPrint(e.func)
        when {
          e.func is Lambda && e.func.args.args.size == 1 && (e.func.body is Name) && e.func.args.args[0].arg == e.func.body.id -> {
            if (e.args.size != 1 || e.keywords.isNotEmpty()) TODO()
            pyPrint(e.args[0])
          }

          else -> (if (e.func is Lambda) "($func)" else func) + "(" + e.args.map { pyPrint(it) }.union(e.keywords.map { "${it.arg} = ${pyPrint(it.value)}" }).joinToString(", ") + ")"
        }
      }
    }
    is Subscript -> pyPrint(e.value) + "[" + pyPrint(e.slice) + "]"
    is Attribute -> pyPrint(e.value) + "." + e.attr
    is Lambda -> "lambda " + e.args.args.joinToString(", ") { pyPrint(it) } + ": " + pyPrint(e.body)
    is IfExp -> pyPrint(e.body) + " if (" + pyPrint(e.test) + ") else " + pyPrint(e.orelse)
    is GeneratorExp -> pyPrint(e.elt) + " " + e.generators.joinToString(" ") { pyPrint(it) }
    is ListComp -> "[" + pyPrint(e.elt) + " " + e.generators.joinToString(" ") { pyPrint(it) } + "]"
    is DictComp -> "{" + pyPrint(e.key) + ": " + pyPrint(e.value) + " " + e.generators.joinToString(" ") { pyPrint(it) } + "}"
    is Starred -> "*" + pyPrint(e.value)
    is Let -> when {
      e.bindings.isEmpty() -> TODO()
      e.bindings.size == 1 -> {
        pyPrint(mkCall(Lambda(Arguments(args = listOf(Arg(e.bindings[0].arg!!))), e.value), listOf(e.bindings[0].value)))
      }
      else -> {
        val let0 = Let(e.bindings.subList(e.bindings.size-1, e.bindings.size), e.value)
        val let1 = Let(e.bindings.subList(0, e.bindings.size-1), let0)
        pyPrint(let1)
      }
    }
    is CTV -> {
      when(e.v) {
        is ConstExpr -> pyPrint(e.v.e)
        is ClassVal -> pyPrint(e.v.toTExpr())
        is FuncTempl -> e.v.func.name
        is FuncInst -> e.v.name + (if (e.v.sig.tParams.isNotEmpty()) e.v.sig.tParams.joinToString(",", "[", "]") else "")
        else -> TODO()
      }
    }
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
    is VarDeclaration -> {
      val type = "# " + (if (s.isVar) "var" else "val")
      val st = if (s.annotation != null) {
        if (s.target.size != 1) fail()
        pyPrintStmt(AnnAssign(s.target[0], s.annotation, s.value))
      } else if (s.target.size == 1) {
        pyPrintStmt(Assign(s.target[0], s.value!!))
      } else {
        pyPrintStmt(Assign(Tuple(s.target, ctx = ExprContext.Load), s.value!!))
      }
      listOf(type).plus(st)
    }
    is Assign -> listOf(pyPrint(s.target) + " = " + pyPrint(s.value))
    is AnnAssign -> listOf(pyPrint(s.target) + ": " + pyPrintType(s.annotation) + (s.value?.let { " = " + pyPrint(it) } ?: ""))
    is AugAssign -> listOf(pyPrint(s.target) + " ${pyPrint(s.op)}= " + pyPrint(s.value))
    is Assert -> listOf("assert " + pyPrint(s.test) + (s.msg?.let { ", " + pyPrint(it) } ?: ""))
    is Expr -> listOf(pyPrint(s.value))
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
    is Continue -> listOf("continue")
    is Break -> listOf("break")
    is Pass -> listOf("pass")
    is Return -> listOf("return" + (s.value?.let { " " + pyPrint(it) } ?: ""))
    else -> fail("unsupported $s")
  }
}

fun pyPrintType(e: TExpr): String = when(e) {
  is NameConstant -> if (e.value == null) "None" else fail()
  is Name -> e.id
  is Subscript -> pyPrintType(e.value) + "[" + pyPrintType((e.slice as Index).value) + "]"
  is Tuple -> when(e.elts.size) {
    0 -> "()"
    1 -> "(" + pyPrint(e.elts[0]) + ",)"
    else -> e.elts.joinToString(", ") { pyPrint(it) }
  }
  is Attribute -> {
    fun getFullName(p: TExpr): String = when(p) {
      is Name -> p.id
      is Attribute -> getFullName(p.value) + "." + p.attr
      else -> fail()
    }
    getFullName(e)
  }
  is CTV -> {
    when(e.v) {
      is ConstExpr -> pyPrint(e.v.e)
      is ClassVal -> pyPrint(e.v.toTExpr())
      is ExTypeVar -> "Any"
      else -> TODO()
    }
  }
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