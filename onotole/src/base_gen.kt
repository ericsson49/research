import java.math.BigInteger

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

  fun genOperator(l: String, o: EBinOp, r: String): String = when(o) {
    EBinOp.Pow -> "${l}.pow($r)"
    else -> "$l ${genOperator(o)} $r"
  }

  fun genOperator(o: EBinOp): String {
    return when (o) {
      EBinOp.Add -> "+"
      EBinOp.Sub -> "-"
      EBinOp.Mult -> "*"
      EBinOp.MatMult -> fail("@ is not supported")
      EBinOp.Div -> fail("/ is not supported")
      EBinOp.Mod -> "%"
      EBinOp.Pow -> " pow "
      EBinOp.LShift -> " shl "
      EBinOp.RShift -> " shr "
      EBinOp.BitOr -> " or "
      EBinOp.BitXor -> " xor "
      EBinOp.BitAnd -> " and "
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
      fail("too many generators")
    val c = gs[0]
    val target = genExpr(c.target, true)
    val ifs = c.ifs.map { "filter{" + target + " -> " + genExpr(it) + "}" }
    return (listOf(genExpr(c.iter)) + ifs + ("map{" + target + " -> " + genExpr(e) + "}")).joinToString(".")
  }

  fun genExpr(e: TExpr, storeCtx: Boolean = false): String {
    return when (e) {
      is Str -> e.s
      is Name -> e.id
      is Compare -> {
        val left = genExpr(e.left)
        val rights = e.comparators.map { genExpr(it) }
        val lefts = listOf(left) + rights.subList(0, rights.size - 1)
        lefts.zip(e.ops).zip(rights).map {
          if (it.first.second == ECmpOp.Is && it.second == "null")
            "(" + it.first.first + " == null )"
          else if (it.first.second == ECmpOp.IsNot && it.second == "null")
            "(" + it.first.first + " != null )"
          else
            "(" + it.first.first + " " + genCmpop(it.first.second) + " " + it.second + ")"
        }.joinToString(" && ")
      }
      is Attribute -> genExpr(e.value) + "." + e.attr
      is BoolOp -> e.values.map { genExpr(it) }.joinToString(" " + genTBoolop(e.op) + " ")
      is BinOp -> "(" + genOperator(genExpr(e.left), e.op, genExpr(e.right)) + ")"
      is Call -> {
        val a1 = e.args.map { genExpr(it) }
        val a2 = e.keywords.map { it.arg + "=" + genExpr(it.value) }
        genExpr(e.func) + "(" + (a1 + a2).joinToString(", ") + ")"
      }
      is Num -> when(e.n) {
        is BigInteger -> "\"" + e.n.toString() + "\".toBigInteger()"
        is Int -> e.n.toString() + "uL"
        else -> fail("not supported yet")
      }
      is UnaryOp -> genUnaryop(e.op) + " (" + genExpr(e.operand) + ")"
      is NameConstant -> e.value.toString()
      is Subscript -> {
        when (e.slice) {
          is Index -> genExpr(e.value) + "[" + genExpr(e.slice.value) + "]" + (if (storeCtx) "" else "")
          is Slice -> {
            genExpr(e.value) + ".slice(" +
                (e.slice.lower?.let { genExpr(it) } ?: "0uL") + (e.slice.upper?.let { "," + genExpr(it) }
                ?: "") + (e.slice.step?.let { "," + genExpr(it) } ?: "") + ")"
          }
          else -> fail(e.slice.toString())
        }
      }

      is IfExp -> "if (pybool(" + genExpr(e.test) + ")) " + genExpr(e.body) + " else " + genExpr(e.orelse)
      is ListComp -> genComprehension(e.elt, e.generators) + ".toMutableList()"
      is Tuple -> {
        val tupleName = if (storeCtx)
          ""
        else if (e.elts.size == 2)
          "Pair"
        else if (e.elts.size == 3)
          "Triple"
        else
          fail(e.toString())
        tupleName + "(" + e.elts.map { genExpr(it) }.joinToString(", ") + ")"
      }
      is PyDict -> "mutableMapOf(" + e.keys.zip(e.values).map { genExpr(it.first) + " to " + genExpr(it.second) }.joinToString(", ") + ")"
      is PyList -> "mutableListOf(" + e.elts.map { genExpr(it) }.joinToString(", ") + ")"
      is GeneratorExp -> genComprehension(e.elt, e.generators)
      is Bytes -> "bytes(" + e.s + ")"
      is Lambda -> "{" + e.args.args.map(::genArg).joinToString(",") + " -> " + genExpr(e.body) + "}"
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
          fail("not implemented")
        val target = s.targets[0]
        val newVar = target is Name && target.id !in vars
        if (newVar) {
          vars.add((target as Name).id)
        }
        val tupleTarget = target is Tuple
        if (tupleTarget) {
          vars.addAll((target as Tuple).elts.map { (it as Name).id })
        }
        (if (newVar) "var " else (if (tupleTarget) "val " else "")) + s.targets.map { genExpr(it, true) }.joinToString(", ") + " = " + genExpr(s.value)
      }
      is While -> {
        if (s.orelse.isNotEmpty())
          fail("not implemented")
        else {
          println("while (pybool(" + genExpr(s.test) + ")) {")
          s.body.forEach {
            genStmt(it, vars)
          }
          "}"
        }
      }
      is If -> {
        println("if (pybool(" + genExpr(s.test) + ")) {")
        s.body.forEach { genStmt(it, vars) }
        println("} else {")
        s.orelse.forEach { genStmt(it, vars) }
        "}"
      }
      is For -> {
        if (s.orelse.isNotEmpty())
          fail("not implemented")
        else {
          println("for (" + genExpr(s.target, true) + " in " + genExpr(s.iter) + ") {")
          s.body.forEach { genStmt(it, vars) }
          "}"
        }
      }
      is Assert -> {
        "assert(" + genExpr(s.test) + (s.msg?.let { ", " + genExpr(it) } ?: "") + ")"
      }
      is AugAssign -> genExpr(s.target, true) + " " + genOperator(s.op) + "= " + genExpr(s.value)
      is AnnAssign -> {
        "val " + genExpr(s.target, true) + ": " + genNativeType(s.annotation) + " = " + genExpr(s.value!!)
      }
      is FunctionDef -> genFunc(s)
      is Pass -> "// pass"
      is Continue -> "continue"
      is Try -> {
        println("try {")
        s.body.forEach { genStmt(it, vars) }
        s.handlers.forEach {
          println("} catch(${it.name?:'_'}: ${genExpr(it.typ)}) {")
          it.body.forEach { genStmt(it, vars) }
        }
        if (s.orelse.isNotEmpty() && s.finalbody.isNotEmpty()) {
          fail("try/else/finally is not yet implemented")
        } else {
          if (s.orelse.isNotEmpty()) {
            println("}")
            println("run { // else")
            s.orelse.forEach { genStmt(it, vars) }
          } else if (s.finalbody.isNotEmpty()) {
            println("} finally {")
            s.finalbody.forEach { genStmt(it, vars) }
          }
          println("}")
        }
      }
      else -> fail(s.toString())
    }
    println(r)
  }

  fun genFunc(f: FunctionDef) {
    if (f.args.posonlyargs.isNotEmpty())
      fail("posonlyargs is not yet supported")
    if (f.args.kwonlyargs.isNotEmpty())
      fail("kwonlyargs is not yet supported")
    if (f.args.kw_defaults.isNotEmpty())
      fail("kw_defaults is not yet supported")
    if (f.args.vararg != null)
      fail("vararg is not yet supported")
    if (f.args.kwarg != null)
      fail("kwarg is not yet supported")

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
    val defautls = List(f.args.args.size - f.args.defaults.size) { null }.plus(f.args.defaults)
    val args = f.args.args.zip(defautls).map {
      genArg(it.first) + (it.second?.let { " = " + genExpr(it) } ?: "")
    }.joinToString(", ")
    val typ = genNativeType(f.returns!!)
    println("fun " + f.name + "(" + args + "): " + typ + " {")
    val vars = mutableListOf<String>()
    for (s in body) {
      genStmt(s, vars)
    }
    println("}")
  }

  fun getDefaultValueForBase(base: String): String? = run {
    when(base) {
      "boolean" -> "false"
      "uint8" -> "0u.toUByte()"
      "uint64" -> "0uL"
      else -> "${base}()"
    }
  }

  fun genNativeType(t: TExpr): String {
    return when (t) {
      is Name -> {
        val n = t.id
        val name = when (n) {
          "List" -> "CList"
          "Dict" -> "CDict"
          "int" -> "pyint"
          "bool" -> "pybool"
          "bytes" -> "pybytes"
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
            } else if (t.value.id == "Tuple") {
              val tArgs = r.value.elts.map(::genNativeType).joinToString(",")
              if (r.value.elts.size == 2)
                res = "Pair<" + tArgs + ">"
              else if (r.value.elts.size == 3)
                res = "Triple<" + tArgs + ">"
              else
                fail("not implemented")
            } else if (t.value.id == "Callable") {
              if (r.value.elts.size == 2 && r.value.elts[0] is PyList) {
                val tArgs = (r.value.elts[0] as PyList).elts.map(::genNativeType).joinToString(",", "(", ")")
                res = tArgs + "->" + genNativeType(r.value.elts[1])
              } else
                fail(t.toString())
            } else {
              res = genNativeType(t.value) + "<" + r.value.elts.map(::genNativeType).joinToString(",") + ">"
            }
          } else if (r.value is Name) {
            if (t.value.id == "Bitlist") {
              res = "CBitlist"
            } else if (t.value.id == "Bitvector") {
              res = "CBitvector"
            } else if (t.value.id == "ByteList") {
              res = "CByteList"
            } else {
              res = t.value.id + "<" + genNativeType(r.value) + ">"
            }
          } else if (r.value is Subscript) {
            res = t.value.id + "<" + genNativeType(r.value) + ">"
          }
        }
        //res ?: genNativeType(t.value) + "<" + genNativeType(r.value) + ">"
        if (res != null)
          res
        else
          fail(t.toString())
      }
      is Tuple -> {
        t.elts.map { if (it is Name) genNativeType(it) else it.toString() }.joinToString(", ")
      }
      is NameConstant -> t.value?.toString() ?: "Unit"
      is Attribute -> genNativeType(t.value) + "." + t.attr
      else -> fail(t.toString())
    }
  }

  fun genClsField(f: Stmt): Triple<String,String,String?> {
    val annAssign = f as AnnAssign
    val fName = (annAssign.target as Name).id
    val fTyp = genNativeType(annAssign.annotation)
    /*val init = annAssign.value?.let {
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
    } ?: let {*/
    val init = getDefaultValueForBase(fTyp) ?: "$fTyp()"
    //}
    return Triple(fName, fTyp, init)
  }

  abstract fun genValueClass(name: String, base: String)
  abstract fun genClsField(name: String, typ: String, init: String?): String
  abstract fun genContainerClass(name: String, fields: List<Triple<String,String,String?>>)

  fun genClass(c: ClassDef) {
    if (c.bases.size != 1)
      fail("")
    val bases = c.bases.map(::genNativeType)
    if (c.body.size == 1 && c.body[0] is Pass) {
      if (c.bases.size > 1)
        fail("too many bases classes for a Value type")
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