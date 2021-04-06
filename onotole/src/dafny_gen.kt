class DafnyGen: BaseGen() {
    override val parenthesesAroundLambda = true

    override fun genNum(n: Num): String {
        return when(n.n) {
            //is BigInteger -> "\"" + n.n.toString() + "\".toBigInteger()"
            is Int -> n.n.toString()
            else -> fail("not supported yet")
        }
    }

    override fun genNameConstant(e: NameConstant): PExpr = atomic(e.value.toString())

    override fun genLambda(args: List<String>, preBody: List<String>, body: String): String {
        val args = args.mapIndexed { i, a -> if (a == "_") "_$i" else a }
        val begin = "(" + args.joinToString(", ") + ") => "
        return if (preBody.isEmpty())
            begin + body
        else
            begin + preBody.joinToString(" ")  + " $body"
    }

    override fun genLambdaArg(a: LVInfo): String = when(a) {
        is LVName -> a.name
        else -> fail("unsupported $a")
    }

    override fun genIfExpr(t: PExpr, b: PExpr, e: PExpr): PExpr = atomic("if (${t.expr}) then ${b.expr} else ${e.expr}")

    override fun genCmpOp(a: PExpr, op: ECmpOp, b: PExpr): PExpr {
        return if (op == ECmpOp.Is && b.expr == "null")
            format_binop_expr(a, b, "==")
        else if (op == ECmpOp.IsNot && b.expr == "null")
            format_binop_expr(a, b, "!=")
        else
            format_binop_expr(a, b, genCmpop(op))
    }

    override fun genIndexSubscript(e: LVInfo, typ: RTType): PExpr = toLoad(e)

    override fun genBinOp(l: PExpr, o: EBinOp, r: PExpr): PExpr = when(o) {
        EBinOp.Pow -> atomic("${l}.pow($r)")
        else -> {
            val op = genOperator(o)
            if (op[0].isLetter()) {
                atomic("$op(${l.expr}, ${r.expr})")
            } else {
                format_binop_expr(l, r, op)
            }
        }
    }

    override fun genBoolOp(exprs: List<PExpr>, op: EBoolOp): PExpr = format_commutative_op_expr(exprs, genTBoolop(op))

    override fun genUnaryOp(a: PExpr, op: EUnaryOp): PExpr = format_unop_expr(a, genUnaryop(op))

    override fun genOperator(o: EBinOp): String {
        return when (o) {
            EBinOp.Add -> "+"
            EBinOp.Sub -> "-"
            EBinOp.Mult -> "*"
            EBinOp.MatMult -> fail("@ is not supported")
            EBinOp.Div -> fail("/ is not supported")
            EBinOp.Mod -> "%"
            EBinOp.Pow -> "pow"
            EBinOp.LShift -> "shl"
            EBinOp.RShift -> "shr"
            EBinOp.BitOr -> "or"
            EBinOp.BitXor -> "xor"
            EBinOp.BitAnd -> "and"
            EBinOp.FloorDiv -> "/"
        }
    }

    override fun genPyDict(elts: List<Pair<String, String>>): String = "PyDict(" + elts.map { "${it.first} to ${it.second}" }.joinToString(", ") + ")"

    override fun genPyList(elts: List<String>): PExpr = atomic("PyList(" + elts.joinToString(", ") + ")")

    override fun genTuple(elts: List<String>): PExpr {
        val tupleName = if (elts.size == 0)
                "Unit"
            else if (elts.size == 2)
                "Pair"
            else if (elts.size == 3)
                "Triple"
            else
                fail("too many tuple elements")
        return atomic(tupleName + "(" + elts.joinToString(", ") + ")")
    }

    override fun genAttrLoad(e: PExpr, a: identifier): PExpr = atomic("${e.expr}.$a")

    fun toLoad(lhs: LVInfo): PExpr = when(lhs) {
        is LVName -> atomic(lhs.name)
        is LVAttr -> genAttrLoad(toLoad(lhs.e), lhs.attr)
        is LVIndex -> atomic(toLoad(lhs.e).expr + "[" + lhs.index + "]")
        is LVSlice -> atomic(toLoad(lhs.e).expr + "[" + lhs.start?.expr + ".." + lhs.upper?.expr + "]")
        else -> TODO("$lhs")
        /*is LVTuple -> fail("not supported $lhs")*/
    }

    override fun genFunHandle(e: TExpr, type: Sort, fh: FunSignature, argRefs: List<ArgRef>, args: List<String>, kwdArgs: List<Pair<String,String>>, exprTypes: ExprTypes): Pair<String, List<String>> {
        val resArgs = argRefs.map { ar ->
            when(ar) {
                is PositionalRef -> args[ar.idx]
                is KeywordRef -> kwdArgs[ar.idx].second
                is DefaultRef -> genExpr(fh.defaults[ar.idx], exprTypes).expr
            }
        }
        return if (type is Clazz) {
            Pair(typeToStr(type.toInstance()), resArgs)
        } else if (e is Attribute) {
            Pair(genAttrBase(genExpr(e.value, exprTypes), e.attr).expr, resArgs)
        } else {
            Pair(genExpr(e, exprTypes).expr, resArgs)
        }
    }


    override fun genReturn(v: String?, t: RTType): String {
        return "return" + (v?.let {" " + it} ?: "") + ";"
    }

    override fun genVarAssignment(isVar: Boolean?, lv: LVInfo, typ: String?, value: String?): String = when(lv) {
        is LVName -> {
            val type = if (typ == null) "" else ": $typ"
            val initV = value?.let { " := $it" } ?: ""
            if (isVar == null) "${lv.name}$type$initV;" else "var ${lv.name}$type$initV;"
        }
        is LVIndex -> {
            genVarAssignment(null, lv.e,null, toLoad(lv.e).expr + "[" + lv.index.expr + " := " + value + "]")
        }
        is LVAttr -> {
            genVarAssignment(null, lv.e, null, toLoad(lv.e).expr + ".(" + lv.attr + " := " + value + ")")
        }
        is LVSlice -> {
            genVarAssignment(null,lv.e, null,toLoad(lv.e).expr + "[" + lv.start?.expr + ".." + lv.upper?.expr + " := " + value + "]")
        }
        else -> TODO("$lv")
/*
        is LVTuple -> fail("not supported $lv")
*/
    }

    override fun genAugAssignment(lhs: LVInfo, op: EBinOp, rhs: PExpr): String = genVarAssignment(
            null, lhs, null, genBinOp(toLoad(lhs), op, rhs).expr)

    override fun genExprStmt(e: String): String = e + ";"

    override fun genAssertStmt(e: String, m: String?): String = "assert(" + e + (m?.let { ", " + it } ?: "") + ");"

    override fun genForHead(t: LVInfo, i: String): String {
        val i = freshName("i")
        val coll = freshName("coll")
        val itInit = genVarAssignment(false, LVName(i), null, "0")
        val collInit = genVarAssignment(false, LVName(coll), null, i)
        val variant = "  decreases |$coll| - i"
        val whileHead = genWhileHead("$i < |$coll|", variant)
        val iter = genVarAssignment(false, t, null,
                genIndexSubscript(LVIndex(LVName(coll), TPyObject, atomic(i)), TPyObject).expr)
        return "$itInit\n$collInit\n$whileHead\n$iter"
    }

    override fun genWhileHead(t: String): String = genWhileHead(t, null)
    fun genWhileHead(t: String, variant: String? = null): String {
        if (variant == null) {
            return "while $t {"
        } else {
            return "while $t\n$variant\n{"
        }
    }

    override fun genIfHead(t: String): String = "if ${t} {"

    override fun typeToStr(t: RTType): String = when(t) {
        is NamedType -> if (t.tParams.isEmpty()) t.name else t.name + "<" + t.tParams.map { typeToStr(it) }.joinToString(",") + ">"
        else -> TODO()
    }

    override val destructLValTuples = true

    override fun genComment(comment: String) {
        print("/*")
        print(comment.split("\\n").joinToString("\n"))
        println("*/")
    }

    override fun genFunBegin(n: String, args: List<Pair<Arg, String?>>, typ: String): String {
        val args = args.map { genArg(it.first) }.joinToString(", ")
        return "method $n($args) returns (res_: $typ) {"
    }

    override fun genToplevel(n: String, e: TExpr): String {
        return "const $n := " + genExpr(e, TypeResolver.topLevelTyper).expr + ";"
    }

    override fun getDefaultValueForBase(base: String): String? = run {
        when(base) {
            "boolean" -> "false"
            "uint8" -> "0"
            "uint64" -> ""
            else -> "${base}()"
        }
    }

    override fun genOptionalType(typ: String): String {
        TODO("Not yet implemented")
    }

    override fun genClsField(name: String, typ: String, init: String?): String {
        return "  ${name}: $typ"
    }

    override fun genValueClass(name: String, base: String) {
        println("type $name = $base")
    }

    override fun genContainerClass(name: String, fields: List<Triple<String, String, String?>>) {
        println("datatype $name = $name(")
        println(fields.joinToString(",\n") {
            genClsField(it.first, it.second, it.third)
        })
        println(")")
    }

}