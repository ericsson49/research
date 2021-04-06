import java.lang.IllegalArgumentException

sealed class ArgRef
data class PositionalRef(val idx: Int): ArgRef()
data class KeywordRef(val idx: Int): ArgRef()
data class DefaultRef(val idx: Int): ArgRef()


sealed class LVInfo
data class LVName(val name: String): LVInfo()
data class LVAttr(val e: LVInfo, val attr: identifier): LVInfo()
data class LVIndex(val e: LVInfo, val t: RTType, val index: PExpr): LVInfo()
data class LVSlice(val e: LVInfo, val t: RTType, val start: PExpr?, val upper: PExpr?): LVInfo()
data class LVTuple(val elts: List<LVInfo>): LVInfo()

abstract class BaseGen {
  open fun genCmpop(o: ECmpOp): String {
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

  fun genUnaryop(o: EUnaryOp): String {
    return when (o) {
      EUnaryOp.Invert -> "~"
      EUnaryOp.Not -> "!"
      EUnaryOp.UAdd -> "+"
      EUnaryOp.USub -> "-"
    }
  }

  fun wrapLambda(f: String, l: String) = if (parenthesesAroundLambda) "$f($l)" else "$f $l"
  fun genComprehension(e: TExpr, gs: List<Comprehension>, exprTypes: ExprTypes): PExpr {
    if (gs.size != 1)
      fail("too many generators")
    val c = gs[0]
    val vtPairs = matchVarsAndTypes(c.target, getIterableElemType(exprTypes[c.iter].asType()))
    val newCtx = exprTypes.ctx.copy(vtPairs.map { it.first.id to it.second })
    val newExprTypes = exprTypes.new(newCtx)
    val ifs = c.ifs.map {
      val filterTarget = genLValExpr(c.target, exprTypes, liveVarAnalysis(it))
      wrapLambda("filter", genLambda(listOf(genLambdaArg(filterTarget.exprHolder)), genDestructors(filterTarget), genExpr(it, newExprTypes).expr))
    }
    val mapTarget = genLValExpr(c.target, exprTypes, liveVarAnalysis(e))
    val list = ifs + wrapLambda("map", genLambda(listOf(genLambdaArg(mapTarget.exprHolder)), genDestructors(mapTarget), genExpr(e, newExprTypes).expr))
    return list.fold(genExpr(c.iter, exprTypes), { pe, a -> genAttrBase(pe, a) })
  }

  abstract val parenthesesAroundLambda: Boolean
  abstract fun genNum(n: Num): String
  abstract fun genNameConstant(e: NameConstant): PExpr
  abstract fun genLambda(args: List<String>, preBody: List<String>, body: String): String
  abstract fun genLambdaArg(a: LVInfo): String

  abstract fun genIfExpr(t: PExpr, b: PExpr, e: PExpr): PExpr
  abstract fun genCmpOp(a: PExpr, op: ECmpOp, b: PExpr): PExpr
  abstract fun genIndexSubscript(e: LVInfo, typ: RTType): PExpr
  abstract fun genBinOp(a: PExpr, op: EBinOp, b: PExpr): PExpr
  abstract fun genBoolOp(exprs: List<PExpr>, op: EBoolOp): PExpr
  abstract fun genUnaryOp(a: PExpr, op: EUnaryOp): PExpr
  abstract fun genOperator(op: EBinOp): String
  abstract fun genPyDict(elts: List<Pair<String,String>>): String
  abstract fun genPyList(elts: List<String>): PExpr
  abstract fun genTuple(elts: List<String>): PExpr
  abstract fun genAttrLoad(e: PExpr, a: identifier): PExpr
  abstract fun genFunHandle(
          e: TExpr, type: Sort, fh: FunSignature, argRefs: List<ArgRef>,
          args: List<String>, kwdArgs: List<Pair<String,String>>, exprTypes: ExprTypes
  ): Pair<String, List<String>>

  fun genFunCall(funcExpr: String, args: List<String>) = PExpr(MAX_PRIO, funcExpr + "(" + args.joinToString(", ") + ")")
  fun genAttrBase(e: PExpr, a: identifier): PExpr {
    val we = if (e.prio < MAX_PRIO) "(${e.expr})" else e.expr
    return PExpr(MAX_PRIO, we + "." + a)
  }
  fun genAttrCall(e: PExpr, attr: identifier, args: List<String>) = genFunCall(genAttrBase(e, attr).expr, args)

  val tmpNames = mutableSetOf<String>()
  fun freshName(prefix: String = "tmp_"): String {
    val res = prefix + tmpNames.size
    tmpNames.add(res)
    return res
  }

  class VInfo(val isName: Boolean, val expr: String)
  class DestructionInfo(val exprHolder: LVInfo, val type: RTType?, val destructors: List<Pair<LVInfo,String>> = emptyList())

  fun genLValExpr_NoTuple(e: TExpr, exprTypes: ExprTypes, liveVars: Set<String>? = null): Pair<LVInfo,RTType?> {
    fun genExpr(e: TExpr) = genExpr(e,exprTypes)
    fun genExpr_NoTuple(e: TExpr) = genLValExpr_NoTuple(e,exprTypes, liveVars)
    return when(e) {
      is Name -> LVName(if (true || liveVars == null || e.id in liveVars) e.id else "_") to null
      is Attribute -> LVAttr(genLValExpr_NoTuple(e.value, exprTypes, liveVars).first, e.attr) to exprTypes[e].asType()
      is Subscript -> {
        val lvType = exprTypes[e].asType()
        val t = exprTypes[e.value].asType()
        val (pe, _) = genLValExpr_NoTuple(e.value, exprTypes, liveVars)
        when (e.slice) {
          is Index -> {
            LVIndex(pe, t, genExpr(e.slice.value)) to lvType
          }
          is Slice -> {
            val sliceArgs = mutableListOf<PExpr>()
            val start = e.slice.lower?.let { genExpr(it) } ?: atomic(genNum(Num(0)))
            sliceArgs.add(start)
            val upper = e.slice.upper?.let { genExpr(it) }
            if (e.slice.upper != null)
              sliceArgs.add(genExpr(e.slice.upper))
            if (e.slice.step != null)
              fail("slice step is not supported")

            LVSlice(pe, t, start, upper) to lvType
          }
          else -> fail(e.slice.toString())
        }
      }
      else -> fail("unsupported $e")
    }
  }

  fun genLValExpr(e: TExpr, exprTypes: ExprTypes, liveVars: Set<String>? = null): DestructionInfo {
    fun genExpr(e: TExpr) = genExpr(e,exprTypes)
    return when(e) {
      is Name -> DestructionInfo(genLValExpr_NoTuple(e, exprTypes, liveVars).first, null)
      is Attribute -> {
        val (lv, t) = genLValExpr_NoTuple(e, exprTypes, liveVars)
        DestructionInfo(lv, t)
      }
      is Subscript -> {
        val (lv, t) = genLValExpr_NoTuple(e, exprTypes, liveVars)
        DestructionInfo(lv, t)
      }
      is Tuple -> {
        if (!destructLValTuples) {
          //val exprHolder = genTuple(e.elts.map { genLValExpr(it, exprTypes, liveVars).exprHolder.expr }, true)
          DestructionInfo(LVTuple(e.elts.map { genLValExpr(it, exprTypes, liveVars).exprHolder }), null)
        } else {
          val exprHolder = freshName()
          val res = mutableListOf<Pair<LVInfo,String>>()
          val indexers = listOf("first", "second", "third", "fourth")
          e.elts.forEachIndexed { i, ex ->
            val a = genLValExpr(ex, exprTypes, liveVars)
            res.add(a.exprHolder to exprHolder + "." + indexers[i])
            res.addAll(a.destructors)
          }
          DestructionInfo(LVName(exprHolder), null, res)
        }
      }
      else -> fail("unsupported $e")
    }
  }

  fun placeArgs(formalArgs: List<String>, defaults: Int, positional: Int, keywords: List<String>): List<ArgRef> {
    val slots = Array<ArgRef?>(formalArgs.size) { null }
    for(i in 0 until positional) {
      slots[i] = PositionalRef(i)
    }
    keywords.forEachIndexed { i, arg ->
      val idx = formalArgs.indexOf(arg)
      if (idx < 0)
        fail("unknown parameter $arg")
      if (slots[idx] != null)
        fail("$arg value is already defined")
      slots[idx] = KeywordRef(i)
    }
    val offset = formalArgs.size - defaults
    for(i in 0 until defaults) {
      if (slots[i + offset] == null)
        slots[i + offset] = DefaultRef(i)
    }
    val res = mutableListOf<ArgRef>()
    slots.forEachIndexed { i, e ->
      if (e == null)
        fail("cannot find a value for ${formalArgs[i]} parameter")
      else
        res.add(e)
    }
    return res
  }

  fun atomic(s: String) = PExpr(MAX_PRIO, s)
  fun genExpr(e: TExpr, exprTypes: ExprTypes): PExpr {
    val ctx = exprTypes.ctx
    fun getExprType(e: TExpr) = exprTypes[e]
    fun genExpr(e: TExpr) = genExpr(e, exprTypes)
    return when (e) {
      is Str -> atomic(e.s)
      is Name -> atomic(e.id)
      is Compare -> {
        val left = genExpr(e.left)
        val rights = e.comparators.map { genExpr(it) }
        val lefts = listOf(left) + rights.subList(0, rights.size - 1)
        val parts = lefts.zip(e.ops).zip(rights).map {
          genCmpOp(it.first.first, it.first.second, it.second)
        }
        genBoolOp(parts, EBoolOp.And)
      }
      is Attribute -> {
        genAttrLoad(genExpr(e.value), e.attr)
      }
      is BoolOp -> genBoolOp(e.values.map(::genExpr), e.op)
      is BinOp -> genBinOp(genExpr(e.left), e.op, genExpr(e.right))
      is Call -> {
        val resType = getExprType(e)
        val type = getExprType(e.func)
        val a1 = e.args.map { genExpr(it).expr }
        val a2 = e.keywords.map { it.arg!! to genExpr(it.value).expr }

        val argTypes = e.args.map { exprTypes[it].asType() }
        val kwdTypes = e.keywords.map { it.arg!! to exprTypes[it.value].asType() }
        val (fh, argRefs) = type.resolveReturnType(argTypes, kwdTypes)

        val (funcExpr, args) = genFunHandle(e.func, type, fh, argRefs, a1, a2, exprTypes)

        genFunCall(funcExpr, args)
      }
      is Num -> atomic(genNum(e))
      is UnaryOp -> genUnaryOp(genExpr(e.operand), e.op)
      is NameConstant -> genNameConstant(e)
      is Subscript -> {
        val t = getExprType(e.value).asType()
        val pe = genExpr(e.value)
        when (e.slice) {
          is Index -> {
            genIndexSubscript(LVIndex(LVName(pe.expr), t, genExpr(e.slice.value)), t)
          }
          is Slice -> {
            val sliceArgs = mutableListOf<PExpr>()
            val start = e.slice.lower?.let { genExpr(it) } ?: atomic(genNum(Num(0)))
            sliceArgs.add(start)
            if (e.slice.upper != null)
              sliceArgs.add(genExpr(e.slice.upper))
            if (e.slice.step != null)
              fail("slice step is not supported")

            genIndexSubscript(LVSlice(LVName(pe.expr), t, start, e.slice.upper?.let { genExpr(it) } ?: atomic("-1")), t)
            //genAttrCall(genExpr(e.value), "slice", sliceArgs.map { it.expr })
          }
          else -> fail(e.slice.toString())
        }
      }

      is IfExp -> {
        val type = getExprType(e.test).asType()
        val testExpr = if (type == TPyBool) genExpr(e.test) else atomic("pybool(${genExpr(e.test)})")
        genIfExpr(testExpr, genExpr(e.body), genExpr(e.orelse))
      }
      is ListComp -> {
        val c = genComprehension(e.elt, e.generators, exprTypes)
        if (true) c else genAttrCall(c, "toPyList", emptyList())
      }
      is DictComp -> {
        val c = genComprehension(Tuple(elts = listOf(e.key, e.value), ctx = ExprContext.Load), e.generators, exprTypes)
        if (true) c else genAttrCall(c, "toPyDict", emptyList())
      }
      is Tuple -> atomic(genTuple(e.elts.map { genExpr(it).expr }).expr)
      is PyDict -> atomic(genPyDict(e.keys.zip(e.values).map { Pair(genExpr(it.first).expr, genExpr(it.second).expr) }))
      is PyList -> genPyList(e.elts.map { genExpr(it).expr })
      is GeneratorExp -> genComprehension(e.elt, e.generators, exprTypes)
      is Bytes -> genFunCall("pybytes.create", listOf(e.s))
      is Lambda -> {
        val lamType = exprTypes[e] as FunType
        val newCtx = exprTypes.ctx.copy(e.args.args.zip(lamType.argTypes).map { it.first.arg to it.second })
        val newExprTypes = exprTypes.new(newCtx)
        atomic(genLambda(e.args.args.map(::genArg), emptyList(), genExpr(e.body, newExprTypes).expr))
      }
      is Starred -> format_unop_expr(genExpr(e.value), "*")
      else -> fail(e.toString())
    }
  }

  fun genArg(a: Arg): String {
    return a.arg + (a.annotation?.let { ": " + genNativeType(it) } ?: "")
  }

  var phiVarMap = mutableMapOf<String,StmtAnnoMap<String>>()

  abstract fun genReturn(v: String?, t: RTType): String
  abstract fun genVarAssignment(isVar: Boolean?, lv: LVInfo, typ: String?, value: String?): String
  abstract fun genAugAssignment(lhs: LVInfo, op: EBinOp, rhs: PExpr): String
  abstract fun genExprStmt(e: String): String
  abstract fun genAssertStmt(e: String, m: String?): String
  abstract fun genForHead(t: LVInfo, i: String): String
  abstract fun genWhileHead(t: String): String
  abstract fun genIfHead(t: String): String
  abstract fun typeToStr(t: RTType): String
  abstract val destructLValTuples: Boolean

  fun genStmt(s: Stmt): List<String> {
    val ctx = analyses.varTypings[s]!!
    val exprTypes = exprTypes.new(ctx)
    val defsBefore = varDefs.first[s]!!
    val defs = varDefs.second[s]!!

    val res = mutableListOf<String>()

    fun genExpr(e: TExpr) = genExpr(e, exprTypes)
    fun getExprType(e: TExpr): RTType = exprTypes[e].asType()
    fun getVarDecl(vs: VarSlot, s: Stmt): Pair<Boolean?, String> {
      val decl: Boolean?
      val name: String
      when(vs.def) {
        is StmtVarDef -> {
          val defStmt = vs.def.s
          if (defStmt === s) {
            decl = vs.refs != null && !vs.refs.isEmpty()
            name = vs.varName.toString()
          } else {
            val tt = varInfo.newSlots[defStmt]!![vs.name]!!
            decl = null
            name = tt.varName.toString()
          }
        }
        is PhiVarDef -> {
          decl = null
          name = vs.varName.toString()
        }
        else -> fail("gggg")
      }
      return Pair(decl, name)
    }

    when (s) {
      is Return -> {
        val exprType = s.value?.let { getExprType(it) } ?: TPyNone
        if (!canAssignOrCoerceTo(exprType, returnType))
          fail("return expr type doesn't match func return type")
        res.add(genReturn(s.value?.let { genExpr(it).expr }, returnType))
      }
      is Expr -> res.add(genExprStmt(genExpr(s.value).expr))
      is Assign -> {
        if (s.targets.size != 1)
          fail("not implemented")
        val target = s.targets[0]

        when(target) {
          is Name -> {
            varInfo.newSlots[s]?.values?.forEach {
              val vd = getVarDecl(it, s)
              res.add(genVarAssignment(vd.first, LVName(vd.second), null, genExpr(s.value).expr))
            }
          }
          else -> {
            val lhs: Pair<Boolean?, String>
            val tupleTarget = target is Tuple
            val destructInfo = genLValExpr(target, exprTypes)
            val vh = destructInfo.exprHolder
            val exprType = getExprType(s.value)
            var expr: String = genExpr(s.value).expr
            if (destructInfo.type != null && destructInfo.type != exprType) {
              expr = coerceExprToType(s.value, destructInfo.type, exprTypes)
            }
            if (!destructLValTuples) {
              //lhs = Pair(if (tupleTarget) false else null, destructInfo.exprHolder.expr)
              res.add(genVarAssignment(false, vh, null, expr))
            } else {
              res.add(genVarAssignment(false, vh, null, expr))
              res.addAll(genDestructors(destructInfo))
            }
          }
        }
      }
      is While -> {
        val phhh = varInfo.newSlots[s]
        phhh?.values?.forEach {vs ->
          val phiVar = vs.def as PhiVarDef
          val phiWeb = varInfo.phiWebMap[phiVar]!!
          val defS = (phiWeb.defStmt!! as StmtVarDef).s
          /*val types = phiWeb.getNonPhiDefs().map { vd ->
            when (vd) {
              is StmtVarDef -> {
                val a = analyses.varTypingsAfter[vd.s]!![vs.name]!!.asType()
                a
              }
              is ParamVarDef -> { analyses.funcArgs.find { it.name == vs.name }!!.type }
              else -> fail("shouldn't happen")
            }
          }*/
          //val phiVarType = getCommonSuperType(types)
          if (defS == s) {
            val info = varInfo.ifAnnoMap[s]!![vs.name]!!
            val phiWeb = varInfo.phiWebMap[info.target.def as PhiVarDef]!!
            val targetName = phiWeb.lastDef!!.varName
            res.add(genVarAssignment(true, LVName(targetName.toString())/*vs.valName*/, null/*"$phiVarType"*/, info.elseCopy!!.varName.toString()))
          }
        }

        if (s.orelse.isNotEmpty())
          fail("not implemented")
        else {
          val type = getExprType(s.test)
          val testExpr = if (type == TPyBool) genExpr(s.test).expr else "pybool(${genExpr(s.test)})"
          res.add(genWhileHead(testExpr))
          s.body.forEach {
            res.addAll(genStmt(it).map { "  $it" })
          }
          res.add("}")
        }
      }
      is If -> {
        val phhh = varInfo.newSlots[s]
        phhh?.values?.forEach {vs ->
          val phiVar = vs.def as PhiVarDef
          val phiWeb = varInfo.phiWebMap[phiVar]!!
          val defS = (phiWeb.defStmt!! as StmtVarDef).s
          val types = phiWeb.getNonPhiDefs().map { vd ->
            when (vd) {
              is StmtVarDef -> {
                val a = analyses.varTypingsAfter[vd.s]!![vs.name]!!.asType()
                a
              }
              is ParamVarDef -> { analyses.funcArgs.find { it.name == vs.name }!!.type }
              else -> fail("shouldn't happen")
            }
          }
          val phiVarType = getCommonSuperType(types)
          if (defS == s) {
            res.add(genVarAssignment(false, LVName(vs.varName.toString()), typeToStr(phiVarType), null))
          }
        }
        val bodyCopies = mutableListOf<String>()
        val elseCopies = mutableListOf<String>()
        varInfo.ifAnnoMap[s]?.forEach { v, info ->
          val phiWeb = varInfo.phiWebMap[info.target.def as PhiVarDef]!!
          val targetName = phiWeb.lastDef!!.varName
          if (info.bodyCopy != null) {
            bodyCopies.add("$targetName = ${info.bodyCopy.varName}")
          }
          if (info.elseCopy != null) {
            elseCopies.add("$targetName = ${info.elseCopy.varName}")
          }
        }
        val type = getExprType(s.test)
        val testExpr = if (type == TPyBool) genExpr(s.test).expr else "pybool(${genExpr(s.test).expr})"
        res.add(genIfHead(testExpr))
        s.body.forEach { res.addAll(genStmt(it).map { "  $it" }) }

        bodyCopies.forEach { res.add("  $it") }
        val newBodyDefs = mutableListOf<String>()
        defs.forEach {
          val bodyDefs = (it.value as PhiVarDef).defs[0]
          if (it.key in defsBefore && isSame(defsBefore[it.key]!!, bodyDefs)) {
            newBodyDefs.add("  " + it.key + " = " + it.key)
          }
        }

        val newElseDefs = mutableListOf<String>()
        defs.forEach {
          val elseDefs = (it.value as PhiVarDef).defs[1]
          if (it.key in defsBefore && isSame(defsBefore[it.key]!!, elseDefs)) {
            newElseDefs.add("  " + it.key + " = " + it.key)
          }
        }

        if (s.orelse.size > 0 || newElseDefs.size > 0 || elseCopies.size > 0) {
          res.add("} else {")
          s.orelse.forEach { res.addAll(genStmt(it).map { "  $it" }) }
          elseCopies.forEach { res.add("  $it") }
        }
        res.add("}")
      }
      is For -> {
        val phhh = varInfo.newSlots[s]
        phhh?.values?.forEach {vs ->
          val phiVar = vs.def as PhiVarDef
          val phiWeb = varInfo.phiWebMap[phiVar]!!
          val defS = (phiWeb.defStmt!! as StmtVarDef).s
          /*val types = phiWeb.getNonPhiDefs().map { vd ->
            when (vd) {
              is StmtVarDef -> {
                val a = analyses.varTypingsAfter[vd.s]!![vs.name]!!.asType()
                a
              }
              is ParamVarDef -> { analyses.funcArgs.find { it.name == vs.name }!!.type }
              else -> fail("shouldn't happen")
            }
          }
          val phiVarType = getCommonSuperType(types)*/
          if (defS == s) {
            val info = varInfo.ifAnnoMap[s]!![vs.name]!!
            val phiWeb = varInfo.phiWebMap[info.target.def as PhiVarDef]!!
            val targetName = phiWeb.lastDef!!.varName
            res.add(genVarAssignment(true, LVName(targetName.toString())/*vs.valName*/, null/*"$phiVarType"*/, info.elseCopy!!.varName.toString()))
          }
        }

        if (s.orelse.isNotEmpty())
          fail("not implemented")
        else {
          val dInfo = genLValExpr(s.target, exprTypes)
          res.add(genForHead(dInfo.exprHolder, genExpr(s.iter).expr))
          res.addAll(genDestructors(dInfo))
          s.body.forEach { res.addAll(genStmt(it).map { "  $it" }) }
          res.add("}")
        }
      }
      is Assert -> {
        res.add(genAssertStmt(genExpr(s.test).expr, s.msg?.let { genExpr(it).expr }))
      }
      is AugAssign -> res.add(genAugAssignment(genLValExpr(s.target, exprTypes).exprHolder, s.op, genExpr(s.value)))
      is AnnAssign -> {
        varInfo.newSlots[s]?.values?.forEach {
          val vd = getVarDecl(it, s)
          res.add(genVarAssignment(vd.first, LVName(vd.second), genNativeType(s.annotation), genExpr(s.value!!).expr))
        }
        /*val lhs: Pair<Boolean?, String>
        if (phiWeb != null) {
          if (phiWeb.defStmt == StmtVarDef(s)) {
            lhs = Pair(true, convertName(phiWeb.v, true))
          } else {
            lhs = Pair(null, convertName(phiWeb.v, true))
          }
        } else {
          lhs = Pair(false, genExpr(s.target, true))
        }
        res.add(genAssignment(lhs.first, lhs.second, genNativeType(s.annotation), genExpr(s.value!!)))*/
      }
      is FunctionDef -> res.addAll(genFunc(s))
      is Pass -> res.add("// pass")
      is Continue -> res.add("continue")
      is Break -> res.add("break")
      is Try -> {
        res.add("try {")
        s.body.forEach { res.addAll(genStmt(it).map { "  $it" }) }
        s.handlers.forEach {
          res.add("} catch(${it.name?:'_'}: ${genExpr(it.typ)}) {")
          it.body.forEach { res.addAll(genStmt(it).map { "  $it" }) }
        }
        if (s.orelse.isNotEmpty() && s.finalbody.isNotEmpty()) {
          fail("try/else/finally is not yet implemented")
        } else {
          if (s.orelse.isNotEmpty()) {
            res.add("}")
            res.add("run { // else")
            s.orelse.forEach { res.addAll(genStmt(it).map { "  $it" }) }
          } else if (s.finalbody.isNotEmpty()) {
            res.add("} finally {")
            s.finalbody.forEach { res.addAll(genStmt(it).map { "  $it" }) }
          }
          res.add("}")
        }
      }
      else -> fail(s.toString())
    }
    return res
  }

  private fun coerceExprToType(ex: TExpr, type: RTType, exprTypes: ExprTypes): String {
    val expr = genExpr(ex, exprTypes).expr
    val clazz = (type as NamedType).clazz
    val (fh, argRefs) = clazz.resolveReturnType(listOf(exprTypes[ex].asType()), emptyList())
    val (funcExpr, args) = genFunHandle(ex, clazz, fh, argRefs, listOf(expr), emptyList(), exprTypes)
    return genFunCall(funcExpr, args).expr
  }

  private fun genDestructors(destructInfo: DestructionInfo) = destructInfo.destructors.map {
    genVarAssignment(false, it.first, null, it.second)
  }

  var _analyses: Analyses? = null
  val analyses: Analyses
    get() = _analyses!!

  var _returnType: RTType = TPyNone
  val returnType: RTType
    get() = _returnType

  var _varDefs: Triple<StmtAnnoMap<Map<String,VarDef>>,StmtAnnoMap<Map<String,VarDef>>,StmtAnnoMap<PhiWeb>>? = null
  val varDefs: Triple<StmtAnnoMap<Map<String,VarDef>>,StmtAnnoMap<Map<String,VarDef>>,StmtAnnoMap<PhiWeb>>
    get() = _varDefs!!

  var _varInfo: VarInfo? = null
  val varInfo: VarInfo
    get() = _varInfo!!

  var _exprTypes: ExprTypes? = null
  val exprTypes: ExprTypes get() = _exprTypes!!

  abstract fun genComment(comment: String)
  abstract fun genFunBegin(n: String, args: List<Pair<Arg,String?>>, typ: String): String

  fun genFunc(f: FunctionDef): List<String> {
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
      genComment(comment.substring(1, comment.length-1))
    }

    _analyses = inferenceVarTypes(f)
    _varDefs = varDefAnalysis(f)
    _returnType = parseType(f.returns!!)
    _varInfo = varSlotAnalysis(f)

    _exprTypes = TypeResolver.topLevelTyper
    val defautls = List(f.args.args.size - f.args.defaults.size) { null }.plus(f.args.defaults.map { genExpr(it, exprTypes).expr })
    val args = f.args.args.zip(defautls)
    val typ = genNativeType(f.returns!!)
    val res = mutableListOf<String>()
    res.add(genFunBegin(f.name, args, typ))
    for (s in body) {
      res.addAll(genStmt(s).map { "  $it" })
    }
    _exprTypes = null
    res.add("}")
    return res
  }

  abstract fun genToplevel(n: String, e: TExpr): String
  abstract fun getDefaultValueForBase(base: String): String?
  abstract fun genOptionalType(typ: String): String

  fun genNativeType(t: TExpr): String {
    return when (t) {
      is Name -> typeToStr(NamedType(t.id))
      is Subscript -> {
        typeToStr((_getExprType(t) as Clazz).toInstance())
      }
      is NameConstant -> typeToStr(NamedType(t.value?.toString() ?: "Unit"))
      else -> fail("not supported $t")
    }
  }

  fun genClsField(f: Stmt): Triple<String,String,String?> {
    val annAssign = f as AnnAssign
    val fName = (annAssign.target as Name).id
    val fTyp = genNativeType(annAssign.annotation)
    val init = getDefaultValueForBase(fTyp) ?: "$fTyp()"
    return Triple(fName, fTyp, init)
  }

  abstract fun genValueClass(name: String, base: String)
  abstract fun genClsField(name: String, typ: String, init: String?): String
  abstract fun genContainerClass(name: String, fields: List<Triple<String,String,String?>>)

  fun genClass(c: ClassDef) {
    if (c.bases.size > 1)
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
    val exprTypes = TypeResolver.topLevelTyper
    val names = a.targets.map{genExpr(it, exprTypes)}.joinToString(", ")
    println("val " + names + " = " + genExpr(a.value, exprTypes).expr)
  }
}