
abstract class BaseGen {
  fun genCmpop(o: ECmpOp): String {
    return when (o) {
      ECmpOp.Eq -> "=="
      ECmpOp.NotEq -> "!="
      ECmpOp.Lt -> "<"
      ECmpOp.LtE -> "<="
      ECmpOp.Gt -> ">"
      ECmpOp.GtE -> ">="
      ECmpOp.Is -> "is"
      ECmpOp.IsNot -> "!is"
      ECmpOp.In -> "in"
      ECmpOp.NotIn -> "!in"
    }
  }

  fun genTBoolop(o: EBoolOp): String {
    return when (o) {
      EBoolOp.And -> "&&"
      EBoolOp.Or -> "||"
    }
  }

  fun genOperator(o: EBinOp): String {
    return when (o) {
      EBinOp.Add -> "+"
      EBinOp.Sub -> "-"
      EBinOp.Mult -> "*"
      EBinOp.MatMult -> fail("@ is not supported")
      EBinOp.Div -> fail("/ is not supported")
      EBinOp.Mod -> "%"
      EBinOp.Pow -> " shl "
      EBinOp.LShift -> "<<"
      EBinOp.RShift -> ">>"
      EBinOp.BitOr -> "|"
      EBinOp.BitXor -> "^"
      EBinOp.BitAnd -> "&"
      EBinOp.FloorDiv -> "/"
    }
  }

  fun genUnaryop(o: EUnaryOp): String {
    return when (o) {
      EUnaryOp.Invert -> "~"
      EUnaryOp.Not -> "!"
      EUnaryOp.UAdd -> "+"
      EUnaryOp.USub -> "-"
    }
  }

  fun genComprehension(e: TExpr, gs: List<Comprehension>): String {
    if (gs.size != 1)
      fail<String>("too many generators")
    val c = gs[0]
    val target = genExpr(c.target, true)
    val ifs = c.ifs.map { "filter{" + target + " -> " + genExpr(it) + "}" }
    return (listOf(genExpr(c.iter)) + ifs + ("map{" + target + " -> " + genExpr(e) + "}")).joinToString(".")
  }

  fun genExpr(e: TExpr, storeCtx: Boolean = false): String {
    return when (e) {
      is Str -> "\"" + e.s + "\""
      is Name -> e.id
      is Compare -> {
        val left = genExpr(e.left)
        val rights = e.comparators.map { genExpr(it) }
        val lefts = listOf(left) + rights.subList(0, rights.size - 1)
        val ops = e.ops.map(::genCmpop)
        lefts.zip(ops).zip(rights).map { "(" + it.first.first + " " + it.first.second + " " + it.second + ")" }.joinToString(" && ")
      }
      is Attribute -> genExpr(e.value) + "." + e.attr
      is BoolOp -> e.values.map { genExpr(it) }.joinToString(" " + genTBoolop(e.op) + " ")
      is BinOp -> "(" + genExpr(e.left) + genOperator(e.op) + genExpr(e.right) + ")"
      is Call -> {
        val a1 = e.args.map { genExpr(it) }
        val a2 = e.keywords.map { it.arg + "=" + genExpr(it.value) }
        genExpr(e.func) + "(" + (a1 + a2).joinToString(", ") + ")"
      }
      is Num -> e.n.toString()
      is UnaryOp -> genUnaryop(e.op) + " (" + genExpr(e.operand) + ")"
      is NameConstant -> e.value.toString()
      is Subscript -> {
        val slice = when (e.slice) {
          is Index -> genExpr(e.slice.value)
          is Slice -> (e.slice.lower?.let { genExpr(it) } ?: "") + ":" + (e.slice.upper?.let { genExpr(it) }
              ?: "") + (e.slice.step?.let { ":" + genExpr(it) } ?: "")
          else -> fail(e.slice.toString())
        }
        genExpr(e.value) + "[" + slice + "]"
      }

      is IfExp -> "if " + genExpr(e.test) + " " + genExpr(e.body) + " else " + genExpr(e.orelse)
      is ListComp -> genComprehension(e.elt, e.generators) + ".toMutableList()"
      is Tuple -> (if (storeCtx) "" else "Tuple") + "(" + e.elts.map { genExpr(it) }.joinToString(", ") + ")"
      is Dict -> "mutableMapOf(" + e.keys.zip(e.values).map { genExpr(it.first) + " := " + genExpr(it.second) }.joinToString(", ") + ")"
      is TList -> "mutableListOf(" + e.elts.map { genExpr(it) }.joinToString(", ") + ")"
      is GeneratorExp -> genComprehension(e.elt, e.generators)
      is Bytes -> "bytes(" + e.s + ")"
      is Lambda -> "{(" + e.args.args.map(::genArg).joinToString(",") + ") -> " + genExpr(e.body) + "}"
      is Starred -> "*" + genExpr(e.value)
      else -> fail(e.toString())
    }
  }

  fun genArg(a: Arg): String {
    return a.arg + (a.annotation?.let { ": " + genNativeType(it) } ?: "")
  }

  fun genStmt(s: Stmt, vars: MutableList<String>) {
    val r = when (s) {
      is Return -> "return " + (s.value?.let { " " + genExpr(it) } ?: "")
      is Expr -> genExpr(s.value)
      is Assign -> {
        if (s.targets.size != 1)
          fail<String>("ttt")
        val target = s.targets[0]
        val newVar = target is Name && target.id !in vars
        if (newVar) {
          vars.add((target as Name).id)
        }
        (if (newVar) "var " else "") + s.targets.map { genExpr(it, true) }.joinToString(", ") + " = " + genExpr(s.value)
      }
      is While -> {
        println("while (" + genExpr(s.test) + ") {")
        s.body.forEach {
          genStmt(it, vars)
        }
        "}"
      }
      is If -> {
        println("if (" + genExpr(s.test) + ") {")
        s.body.forEach { genStmt(it, vars) }
        println("} else {")
        s.orelse.forEach { genStmt(it, vars) }
        "}"
      }
      is For -> {
        println("for (" + genExpr(s.target, true) + " in " + genExpr(s.iter) + ") {")
        s.body.forEach { genStmt(it, vars) }
        "}"
      }
      is Assert -> {
        "assert(" + genExpr(s.test) + (s.msg?.let { ", " + genExpr(it) } ?: "") + ")"
      }
      is AugAssign -> genExpr(s.target) + " " + genOperator(s.op) + "= " + genExpr(s.value)
      is AnnAssign -> {
        genExpr(s.target) + ": " + genNativeType(s.annotation) + " = " + genExpr(s.value!!)
      }
      else -> fail(s.toString())
    }
    println(r)
  }

  fun genFunc(f: FunctionDef) {
    val firstStmt = f.body[0]
    val (body, comment) = if (firstStmt is Expr && firstStmt.value is Str) {
      Pair(f.body.subList(1, f.body.size), firstStmt.value.s)
    } else {
      Pair(f.body, null)
    }

    if (comment != null) {
      print("/*")
      print(comment)
      println("*/")
    }
    val args = f.args.args.map(::genArg).joinToString(", ")
    val typ = genNativeType(f.returns!!)
    println("fun " + f.name + "(" + args + "): " + typ + " {")
    val vars = mutableListOf<String>()
    for (s in body) {
      genStmt(s, vars)
    }
    println("}")
  }

  fun genNativeType(t: TExpr): String {
    return when (t) {
      is Name -> {
        val n = t.id
        val name = when (n) {
          "List" -> "CList"
          "Dict" -> "CDict"
          else -> n
        }
        name
      }
      is Subscript -> {
        val r = (t.slice as Index)
        var res:String? = null
        if (t.value is Name) {
          if (r.value is Tuple) {
            if (t.value.id == "List" && r.value.elts.size == 2) {
              res = "CList<" + genNativeType(r.value.elts[0]) + ">"
            } else if (t.value.id == "Vector" && r.value.elts.size == 2) {
              res = "CVector<" + genNativeType(r.value.elts[0]) + ">"
            }
          } else if (r.value is Name) {
            if (t.value.id == "Bitlist") {
              res = "CBitlist"
            } else if (t.value.id == "Bitvector") {
              res = "CBitvector"
            }
          }
        }
        res ?: genNativeType(t.value) + "<" + genNativeType(r.value) + ">"
      }
      is Tuple -> {
        t.elts.map { if (it is Name) genNativeType(it) else it.toString() }.joinToString(", ")
      }
      is NameConstant -> t.value?.toString() ?: "Unit"
      else -> fail(t.toString())
    }
  }

  fun genClsField(f: Stmt): Triple<String,String,String?> {
    val annAssign = f as AnnAssign
    val fName = (annAssign.target as Name).id
    val fTyp = genNativeType(annAssign.annotation)
    val init = annAssign.value?.let {
      when (it) {
        is Call -> {
          if (it.func is Name && it.func.id == "field" && it.args.isEmpty()
              && it.keywords.size == 1 && it.keywords[0].arg == "default_factory" && it.keywords[0].value is Name
              && (it.keywords[0].value as Name).id == "dict"
          ) {
            "mutableMapOf()"
          } else {
            fail("")
          }
        }
        else -> fail(it.toString())
      }
    }
    return Triple(fName, fTyp, init)
  }

  abstract fun genValueClass(name: String, base: String)
  abstract fun genClsField(name: String, typ: String, init: String?): String
  abstract fun genContainerClass(name: String, fields: List<Triple<String,String,String?>>)

  fun genClass(c: ClassDef) {
    if (c.bases.size != 1)
      fail<Unit>("")
    val bases = c.bases.map(::genNativeType)
    if (c.body.size == 1 && c.body[0] is Pass) {
      genValueClass(c.name, bases[0])
    } else {
      genContainerClass(c.name, c.body.map(::genClsField))
    }
  }

  fun genTopLevelAssign(a: Assign) {
    val names = a.targets.map{genExpr(it)}.joinToString(", ")
    println("val " + names + " = " + genExpr(a.value))
  }
}