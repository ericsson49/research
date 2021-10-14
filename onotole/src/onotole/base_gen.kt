package onotole

import java.io.PrintStream

sealed class ArgRef
data class PositionalRef(val idx: Int): ArgRef()
data class KeywordRef(val idx: Int): ArgRef()
data class DefaultRef(val idx: Int): ArgRef()


sealed class LVInfo
data class LVName(val name: String): LVInfo()
data class LVAttr(val e: LVInfo, val attr: identifier): LVInfo()
data class LVIndex(val e: LVInfo, val t: RTType, val index: RExpr): LVInfo()
data class LVSlice(val e: LVInfo, val t: RTType, val start: RExpr?, val upper: RExpr?, val step: RExpr?): LVInfo()
data class LVTuple(val elts: List<LVInfo>): LVInfo()

abstract class ModuleRef(val pw: PrintStream) {
  abstract fun finish()
  abstract fun genTopLevel(name: String, value: TExpr)
  abstract fun genClass(cls: ClassDef)
  abstract fun genFunc(func: FunctionDef)
}

abstract class BaseGen(val currPkg: String, val importedPkgs: Set<String>) {
  open fun toModules(defs: Collection<TopLevelDef>): List<Pair<String, Collection<TopLevelDef>>> {
    val consts = defs.filterIsInstance<ConstTLDef>()
    val classes = defs.filterIsInstance<ClassTLDef>()
    val methods = defs.filterIsInstance<FuncTLDef>()
    if (consts.size + classes.size + methods.size != defs.size) fail()
    return listOf(
        "constants" to consts,
        "classes" to classes,
        "methods" to methods,
    )
  }


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

  abstract fun applyMapFilter(e: RExpr, a: String, l: String): RExpr

  fun wrapLambda(f: String, l: String) = if (parenthesesAroundLambda) "$f($l)" else "$f $l"
  fun genComprehension(e: TExpr, gs: List<Comprehension>, exprTypes: ExprTypes): RExpr {
    if (gs.size != 1)
      fail("too many generators")
    val c = gs[0]
    val vtPairs = matchVarsAndTypes(c.target, getIterableElemType(exprTypes[c.iter].asType()))
    val newCtx = exprTypes.ctx.copy(vtPairs.map { it.first.id to it.second })
    val newExprTypes = exprTypes.new(newCtx)
    val ifs = c.ifs.map {
      val filterTarget = genLValExpr(c.target, exprTypes, liveVarAnalysis(it))
      "filter" to genLambda(listOf(genLambdaArg(filterTarget.exprHolder)), genDestructors(filterTarget), genExpr(it, newExprTypes))
    }
    val mapTarget = genLValExpr(c.target, exprTypes, liveVarAnalysis(e))
    val list = ifs + ("map" to genLambda(listOf(genLambdaArg(mapTarget.exprHolder)), genDestructors(mapTarget), genExpr(e, newExprTypes)))
    return list.fold(genExpr(c.iter, exprTypes), { pe, a -> applyMapFilter(pe, a.first, a.second) })
  }

  fun splitClassName(n: String): Pair<String, String> {
    val idx = n.lastIndexOf('.')
    if (idx < 0)
      return "" to n
    else
      return n.substring(0, idx) to n.substring(idx+1, n.length)
  }

  abstract val parenthesesAroundLambda: Boolean
  abstract fun genNum(n: Num): String
  abstract fun genNameConstant(e: NameConstant): RExpr
  abstract fun genLambda(args: List<String>, preBody: List<String>, body: RExpr): String
  abstract fun genLambdaArg(a: LVInfo): String

  abstract fun genIfExpr(t: RExpr, b: RExpr, e: RExpr): RExpr
  abstract fun genCmpOp(a: RExpr, op: ECmpOp, b: RExpr): RExpr
  abstract fun genIndexSubscript(e: LVInfo, typ: RTType): RExpr
  abstract fun genBinOp(a: RExpr, op: EBinOp, b: RExpr): RExpr
  abstract fun genBoolOp(exprs: List<RExpr>, op: EBoolOp): RExpr
  abstract fun genUnaryOp(a: RExpr, op: EUnaryOp): RExpr
  abstract fun genOperator(op: EBinOp): String
  abstract fun genPyDict(elts: List<Pair<RExpr,RExpr>>): RExpr
  abstract fun genPyList(elts: List<RExpr>): RExpr
  abstract fun genTuple(elts: List<RExpr>): RExpr
  abstract fun genAttrLoad(e: RExpr, a: identifier, isStatic: Boolean): RExpr
  abstract fun genFunHandle(
          e: TExpr, type: Sort, fh: FunSignature, argRefs: List<ArgRef>,
          args: List<RExpr>, kwdArgs: List<Pair<String,RExpr>>, exprTypes: ExprTypes
  ): Pair<RExpr, List<String>>

  fun genFunCall(funcExpr: RExpr, args: List<String>) =
      RInfix("", funcExpr, RSeq("(", ")", ",", args.map { RLit(it) }))
  fun genAttrBase(e: RExpr, a: identifier): RExpr = RInfix(".", e, RLit(a))
  fun genAttrCall(e: RExpr, attr: identifier, args: List<String>) = genFunCall(genAttrBase(e, attr), args)

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
            val start = e.slice.lower?.let { genExpr(it) } ?: atomic(genNum(Num(0)))
            val upper = e.slice.upper?.let { genExpr(it) }
            val step = e.slice.upper?.let { genExpr(it) }
            LVSlice(pe, t, start, upper, step) to lvType
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

  fun atomic(s: String) = RPExpr(MAX_PRIO, s)
  fun genExpr(e: TExpr, exprTypes: ExprTypes): RExpr {
    fun getExprType(e: TExpr) = exprTypes[e]
    fun genExpr(e: TExpr) = genExpr(e, exprTypes)
    return when (e) {
      is Str -> atomic(e.s)
      is Name -> {
        val exprType = getExprType(e)
        if (exprType is Clazz || exprType is MetaClass)
          atomic(genNativeType(e))
        else
          atomic(e.id)
      }
      is Attribute -> {
        genAttrLoad(genExpr(e.value), e.attr, getExprType(e.value) is Clazz)
      }
      is Call -> {
        val resType = getExprType(e)
        val type = getExprType(e.func)
        if (type is SpecialFuncRef) {
          val op = type.name.substring(1, type.name.length-1)
          when(op) {
            in TypeResolver.binOps -> {
              if (e.args.size != 2) fail()
              genBinOp(genExpr(e.args[0]), EBinOp.valueOf(op), genExpr(e.args[1]))
            }
            in TypeResolver.boolOps -> {
              genBoolOp(e.args.map(::genExpr), EBoolOp.valueOf(op))
            }
            in TypeResolver.cmpOps -> {
              if (e.args.size != 2) fail()
              genCmpOp(genExpr(e.args[0]), ECmpOp.valueOf(op), genExpr(e.args[1]))
            }
            in TypeResolver.unaryOp -> {
              if (e.args.size != 1) fail()
              genUnaryOp(genExpr(e.args[0]), EUnaryOp.valueOf(op))
            }
            in setOf("list") -> {
              genPyList(e.args.map { genExpr(it) })
            }
            in setOf("dict") -> {
              genPyDict(e.args.map {
                val t = it as Tuple
                if (t.elts.size != 2) fail()
                genExpr(t.elts[0]) to genExpr(t.elts[1])
              })
            }
            else -> TODO(type.name)
          }
        } else {
          val a1 = e.args.map { genExpr(it) }
          val a2 = e.keywords.map { it.arg!! to genExpr(it.value) }

          val argTypes = e.args.map { exprTypes[it].asType() }
          val kwdTypes = e.keywords.map { it.arg!! to exprTypes[it.value].asType() }
          val (fh, argRefs) = type.resolveReturnType(argTypes, kwdTypes)

          val (funcExpr, args) = genFunHandle(e.func, type, fh, argRefs, a1, a2, exprTypes)

          genFunCall(funcExpr, args)
        }
      }
      is Num -> atomic(genNum(e))
      is NameConstant -> genNameConstant(e)
      is Subscript -> {
        val t = getExprType(e.value).asType()
        val pe = genExpr(e.value)
        when (e.slice) {
          is Index -> {
            genIndexSubscript(LVIndex(LVName(render(pe)), t, genExpr(e.slice.value)), t)
          }
          is Slice -> {
            val lower = e.slice.lower?.let { genExpr(it) }// ?: atomic(genNum(Num(0)))
            val upper = e.slice.upper?.let { genExpr(it) }// ?: atomic(genNum(Num(-1)))
            val step = e.slice.step?.let { genExpr(it) }// ?: atomic(getNum(Num(1)))
            genIndexSubscript(LVSlice(LVName(render(pe)), t, lower, upper, step), t)
          }
          else -> fail(e.slice.toString())
        }
      }

      is IfExp -> {
        val type = getExprType(e.test).asType()
        val testExpr = if (type == TPyBool)
          genExpr(e.test)
        else
          RInfix("", atomic("pybool"), RSeq("(", ")", ",", listOf(genExpr(e.test))))
        genIfExpr(testExpr, genExpr(e.body), genExpr(e.orelse))
      }
      is Tuple -> genTuple(e.elts.map { genExpr(it) })
      is GeneratorExp -> genComprehension(e.elt, e.generators, exprTypes)
      is Bytes -> genFunCall(RLit("pybytes.create"), listOf(e.s))
      is Lambda -> {
        val lamType = exprTypes[e] as FunType
        val newCtx = exprTypes.ctx.copy(e.args.args.zip(lamType.argTypes).map { it.first.arg to it.second })
        val newExprTypes = exprTypes.new(newCtx)
        atomic(genLambda(e.args.args.map(::genArg), emptyList(), genExpr(e.body, newExprTypes)))
      }
      is Starred -> RPrefix("*", genExpr(e.value))
      else -> fail(e.toString())
    }
  }

  fun genArg(a: Arg): String {
    return a.arg + (a.annotation?.let { ": " + genNativeType(it) } ?: "")
  }

  fun renderP(e: RExpr): PExpr = when(e) {
    is RLit -> PExpr(MAX_PRIO, e.expr)
    is RName -> PExpr(MAX_PRIO, e.name)
    is RPrefix -> format_unop_expr(renderP(e.e), e.op)
    is RInfix -> if (e.op.isEmpty() || e.op == ".")
      e.args.map { renderP(it) }.reduce { a, b ->
        val _a = if (a.prio < MAX_PRIO) "(${a.expr})" else a.expr
        PExpr(MAX_PRIO, "$_a${e.op}${b.expr}")
      }
    else if (e.args.size == 2)
      format_binop_expr(renderP(e.args[0]), renderP(e.args[1]), e.op)
    else
      format_commutative_op_expr(e.args.map { renderP(it) }, e.op)
    is RSeq -> PExpr(MAX_PRIO, e.args.joinToString(e.delim + " ", e.open, e.close) { renderP(it).expr })
    is RPExpr -> PExpr(e.prio, e.expr)
  }
  fun render(e: RExpr) = renderP(e).expr

  abstract fun genReturn(v: String?, t: RTType): String
  abstract fun genVarAssignment(isVar: Boolean?, lv: LVInfo, typ: String?, value: String?): String
  abstract fun genAugAssignment(lhs: LVInfo, op: EBinOp, rhs: RExpr): String
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

    fun addVarDecl(vs: VarSlot, loop: Boolean) {
      val phiVar = vs.def as PhiVarDef
      val phiWeb = varInfo.phiWebMap[phiVar]!!
      val defS = (phiWeb.defStmt!! as StmtVarDef).s
      val varAssignment: String
      if (defS == s) {
        if (loop) {
          val info = varInfo.ifAnnoMap[s]!![vs.name]!!
          val phiWeb = varInfo.phiWebMap[info.target.def as PhiVarDef]!!
          val targetName = phiWeb.lastDef!!.varName
          varAssignment = genVarAssignment(true, LVName(targetName.toString()), null, info.elseCopy!!.varName.toString())
        } else {
          val types = phiWeb.getNonPhiDefs().map { vd ->
            when (vd) {
              is StmtVarDef -> analyses.varTypingsAfter[vd.s]!![vs.name]!!.asType()
              is ParamVarDef -> analyses.funcArgs.find { it.name == vs.name }!!.type
              else -> fail("shouldn't happen")
            }
          }
          val phiVarType = getCommonSuperType(types)
          varAssignment = genVarAssignment(false, LVName(vs.varName.toString()), typeToStr(phiVarType), null)
        }
        res.add(varAssignment)
      } else {
        //TODO()
      }
    }

    when (s) {
      is Return -> {
        val exprType = s.value?.let { getExprType(it) } ?: TPyNone
        if (!canAssignOrCoerceTo(exprType, returnType))
          fail("return expr type doesn't match func return type")
        res.add(genReturn(s.value?.let { render(genExpr(it)) }, returnType))
      }
      is Expr -> res.add(genExprStmt(render(genExpr(s.value))))
      is Assign -> {
        val target = s.target

        when(target) {
          is Name -> {
            varInfo.newSlots[s]?.values?.forEach {
              val vd = getVarDecl(it, s)
              res.add(genVarAssignment(vd.first, LVName(vd.second), null, render(genExpr(s.value))))
            }
          }
          else -> {
            val destructInfo = genLValExpr(target, exprTypes)
            val vh = destructInfo.exprHolder
            val exprType = getExprType(s.value)
            var expr: RExpr = genExpr(s.value)
            if (destructInfo.type != null && destructInfo.type != exprType) {
              expr = coerceExprToType(s.value, destructInfo.type, exprTypes)
            }
            if (!destructLValTuples) {
              res.add(genVarAssignment(false, vh, null, render(expr)))
            } else {
              res.add(genVarAssignment(false, vh, null, render(expr)))
              res.addAll(genDestructors(destructInfo))
            }
          }
        }
      }
      is While -> {
        (varInfo.newSlots[s] ?: emptyMap()).values.forEach { vs ->
          addVarDecl(vs, true)
        }
        val type = getExprType(s.test)
        val testExpr = if (type == TPyBool) render(genExpr(s.test)) else "pybool(${render(genExpr(s.test))})"
        res.add(genWhileHead(testExpr))
        s.body.forEach {
          res.addAll(genStmt(it).map { "  $it" })
        }
        res.add("}")
      }
      is If -> {
        (varInfo.newSlots[s] ?: emptyMap()).values.forEach { vs ->
          addVarDecl(vs, false)
        }
        val type = getExprType(s.test)
        val testExpr = if (type == TPyBool) render(genExpr(s.test)) else "pybool(${render(genExpr(s.test))})"
        res.add(genIfHead(testExpr))
        s.body.forEach { res.addAll(genStmt(it).map { "  $it" }) }

        if (s.orelse.isNotEmpty()) {
          res.add("} else {")
          s.orelse.forEach { res.addAll(genStmt(it).map { "  $it" }) }
        }
        res.add("}")
      }
      is For -> {
        (varInfo.newSlots[s] ?: emptyMap()).values.forEach { vs ->
          addVarDecl(vs, true)
        }
        val dInfo = genLValExpr(s.target, exprTypes)
        res.add(genForHead(dInfo.exprHolder, render(genExpr(s.iter))))
        res.addAll(genDestructors(dInfo))
        s.body.forEach { res.addAll(genStmt(it).map { "  $it" }) }
        res.add("}")
      }
      is Assert -> {
        res.add(genAssertStmt(render(genExpr(s.test)), s.msg?.let { render(genExpr(it)) }))
      }
      is AugAssign -> res.add(genAugAssignment(genLValExpr(s.target, exprTypes).exprHolder, s.op, genExpr(s.value)))
      is AnnAssign -> {
        varInfo.newSlots[s]?.values?.forEach {
          val vd = getVarDecl(it, s)
          res.add(genVarAssignment(vd.first, LVName(vd.second), genNativeType(s.annotation), render(genExpr(s.value!!))))
        }
      }
      is FunctionDef -> res.addAll(genFunc(s))
      is Pass -> res.add("// pass")
      is Continue -> res.add("continue")
      is Break -> res.add("break")
      is Try -> TODO()
      else -> fail(s.toString())
    }
    return res
  }

  private fun coerceExprToType(ex: TExpr, type: RTType, exprTypes: ExprTypes): RExpr {
    val expr = genExpr(ex, exprTypes)
    val clazz = (type as NamedType).clazz
    val (fh, argRefs) = clazz.resolveReturnType(listOf(exprTypes[ex].asType()), emptyList())
    val (funcExpr, args) = genFunHandle(ex, clazz, fh, argRefs, listOf(expr), emptyList(), exprTypes)
    return genFunCall(funcExpr, args)
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

  var _varInfo: VarInfo? = null
  val varInfo: VarInfo
    get() = _varInfo!!

  var _exprTypes: ExprTypes? = null
  val exprTypes: ExprTypes get() = _exprTypes ?: TypeResolver.topLevelTyper

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

    _exprTypes = TypeResolver.topLevelTyper
    _analyses = inferVarTypes(exprTypes, f)
    _returnType = parseType(exprTypes, f.returns!!)
    _varInfo = varSlotAnalysis(f)

    val defautls = List(f.args.args.size - f.args.defaults.size) { null }.plus(f.args.defaults.map { render(genExpr(it, exprTypes)) })
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

  abstract fun genToplevel(n: String, e: TExpr)
  abstract fun getDefaultValueForBase(base: String): String?
  abstract fun genOptionalType(typ: String): String

  fun genNativeType(t: TExpr): String {
    return when (t) {
      is NameConstant, is Name, is Subscript, is Attribute -> typeToStr(parseType(exprTypes, t))
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

  abstract fun genValueClass(name: String, base: TExpr)
  abstract fun genClsField(name: String, typ: String, init: String?): String
  abstract fun genContainerClass(name: String, base: TExpr, fields: List<Triple<String,String,String?>>)

  fun genClass(c: ClassDef) {
    if (c.bases.size > 1)
      fail("")
    if (c.body.size == 1 && c.body[0] is Pass) {
      if (c.bases.size > 1)
        fail("too many bases classes for a Value type")
      genValueClass(c.name, c.bases[0])
    } else {
      if (c.bases.size > 1)
        fail("too many bases classes")
      genContainerClass(c.name, c.bases[0], c.body.map(::genClsField))
    }
  }

  inner class DefaultModuleRef(): ModuleRef(System.out) {
    override fun finish() {}
    override fun genTopLevel(name: String, value: TExpr) {
      this@BaseGen.genToplevel(name, value)
    }
    override fun genClass(cls: ClassDef) {
      this@BaseGen.genClass(cls)
    }
    override fun genFunc(func: FunctionDef) {
      this@BaseGen.genFunc(func).forEach(pw::println)
      pw.println()
    }
  }

  open fun beginModule(name: String, defs: Collection<TopLevelDef>): ModuleRef = DefaultModuleRef()
  open fun endModule(mod: ModuleRef) {
    mod.finish()
  }
}