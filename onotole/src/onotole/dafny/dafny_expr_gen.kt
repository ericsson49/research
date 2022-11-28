package onotole.dafny

import onotole.Arg
import onotole.Attribute
import onotole.BinOp
import onotole.BoolOp
import onotole.Bytes
import onotole.CTV
import onotole.Call
import onotole.ClassVal
import onotole.Clazz
import onotole.Compare
import onotole.Comprehension
import onotole.ConstExpr
import onotole.DafnyClassKind
import onotole.EBinOp
import onotole.EBoolOp
import onotole.ECmpOp
import onotole.EUnaryOp
import onotole.ExprTyper
import onotole.FunType
import onotole.FuncInst
import onotole.GeneratorExp
import onotole.IfExp
import onotole.Index
import onotole.LVAttr
import onotole.LVInfo
import onotole.LVName
import onotole.Lambda
import onotole.Let
import onotole.ListComp
import onotole.MAX_PRIO
import onotole.MIN_PRIO
import onotole.MemoryModel
import onotole.Name
import onotole.NameConstant
import onotole.NamedType
import onotole.Num
import onotole.PExpr
import onotole.PyDict
import onotole.PyList
import onotole.RExpr
import onotole.RInfix
import onotole.RLit
import onotole.RName
import onotole.RPExpr
import onotole.RPrefix
import onotole.RSeq
import onotole.RTType
import onotole.Slice
import onotole.SpecialFuncRef
import onotole.Starred
import onotole.Str
import onotole.Subscript
import onotole.TExpr
import onotole.TPyBytes
import onotole.Tuple
import onotole.TypeResolver
import onotole.UnaryOp
import onotole.asType
import onotole.`class`
import onotole.convertCompareSimple
import onotole.dafnyClassKinds
import onotole.dafnyCollectionTypes
import onotole.dafnyFreshAttrs
import onotole.fail
import onotole.findFuncDescr
import onotole.format_binop_expr
import onotole.format_commutative_op_expr
import onotole.format_unop_expr
import onotole.getIterableElemType
import onotole.identifier
import onotole.isDafnyClassKind
import onotole.isExceptionCheck
import onotole.isSubType_new
import onotole.liveVarAnalysis
import onotole.matchNamesAndTypes
import onotole.matchVarsAndTypes
import onotole.mkAttribute
import onotole.mkCall
import onotole.mkName
import onotole.toInstance

class DafnyExprGen(
    val nativeTypeFunc: (TExpr) -> String, val insideMethod: Boolean,
    val heapVars: Set<String> = emptySet(),
    val methRename: Map<String, String> = emptyMap()
) {
  fun genNativeType(t: TExpr): String {
    return nativeTypeFunc.invoke(t)
  }

  fun with(insideMethod: Boolean? = null, heapVars: Set<String>? = null): DafnyExprGen {
    return DafnyExprGen(nativeTypeFunc, insideMethod ?: this.insideMethod, heapVars ?: this.heapVars, methRename)
  }

  fun isNonFresh(e: TExpr, typer: ExprTyper): Boolean {
    return when(e) {
      is Attribute -> {
        val vt = typer[e.value] as NamedType
        e.attr !in (dafnyFreshAttrs[vt.name] ?: emptySet())
      }
      else -> TODO()
    }
  }

  fun canReferToClass(e: TExpr, typer: ExprTyper): Boolean {
    return isDafnyClassKind(e, typer, setOf(DafnyClassKind.Class, DafnyClassKind.Datatype)) && when(e) {
      is Name -> e.id in heapVars
      is Attribute -> canReferToClass(e.value, typer)
      is Subscript -> canReferToClass(e.value, typer)
      is Call -> {
        if (e.func is CTV && e.func.v is FuncInst && e.func.v.name == "<check>") {
          canReferToClass(e.args[0], typer)
        } else false
      }
      else -> false
    }
  }

  fun isNonLocal(e: TExpr, typer: ExprTyper): Boolean {
    return isDafnyClassKind(e, typer, DafnyClassKind.Class) && when(e) {
      is Name -> canReferToClass(e, typer)
      is Attribute -> canReferToClass(e.value, typer) && isNonFresh(e, typer)
      is Subscript ->
        TODO()
      is Call -> {
        if (e.func is CTV && e.func.v is FuncInst && e.func.v.name == "<check>") {
          isNonLocal(e.args[0], typer)
        } else false
      }
      else -> false
    }
  }

  fun genNum(n: Num): RExpr {
    return atomic(when(n.n) {
      is Int -> n.n.toString()
      else -> fail("not supported yet")
    })
  }

  fun genName(n: String): RExpr {
    return atomic(when {
      n.startsWith("phase0.") -> n.substring("phase0.".length)
      n.startsWith("ssz.") -> n.substring("ssz.".length)
      n.startsWith("pylib.") -> n.substring("pylib.".length)
      n == "<Result>" -> "Result"
      else -> n
    })
  }


  fun genNameConstant(e: NameConstant): RExpr = if (e.value == null)
    atomic("()") else atomic(e.value.toString())

  fun genPyDict(elts: List<Pair<RExpr, RExpr>>): RExpr {
    val res = elts.map { "${render(it.first)} := ${render(it.second)}" }.joinToString(", ", "map[", "]")
    return if (insideMethod) wrapFreshValue("pylib.Dict", atomic(res)) else atomic(res)
  }

  fun genPyList(elts: List<RExpr>): RExpr {
    val res = elts.joinToString(", ", "[", "]") { render(it) }
    return if (insideMethod) {
      wrapFreshValue("pylib.List", atomic(res))
    } else atomic(res)
  }

  fun genTuple(elts: List<RExpr>): RExpr {
    return atomic("(" + elts.joinToString(", ") { render(it) } + ")")
  }

  fun genCtorCall(className: String, args: List<RExpr>): RExpr {
    val cn = (genName(className) as RPExpr).expr
    return when {
      dafnyClassKinds[className] == DafnyClassKind.Class ->
        when {
          className in dafnyCollectionTypes && insideMethod ->
            genFunCall(RLit("${cn}_new"), args)
          className in dafnyCollectionTypes && !insideMethod -> when(className) {
            "pylib.Set" -> atomic("{" + args.joinToString(", ") + "}")
            "pylib.PyList", "ssz.List" -> atomic("[" + args.joinToString(", ") + "]")
            "pylib.Dict" -> atomic("map[" + args.joinToString(", ") + "]")
            else -> TODO()
          }
          insideMethod ->
            genFunCall(RLit("new $cn.Init"), args)
          else ->
            genFunCall(RLit(cn + "_dt"), args)
        }

      dafnyClassKinds[className] == DafnyClassKind.Primitive ->
        genFunCall(RLit("${cn}_new"), args)

      else -> genFunCall(RLit(cn), args)
    }
  }

  fun genCall_FuncInst(e: Call, typer: ExprTyper): RExpr {
    if (e.func !is CTV || e.func.v !is FuncInst) fail()
    if (e.keywords.isNotEmpty()) TODO()
    fun genExpr(e: TExpr) = genExpr(e, typer)
    fun getExprType(e: TExpr) = typer[e]

    val resArgs: List<RExpr> = e.args.map { genExpr(it) }

    if (e.func.v.name.endsWith("::new")) {
      val className = e.func.v.name.substring(0, e.func.v.name.length - "::new".length)
      return genCtorCall(className, resArgs)
    }

    return if (e.func.v.name == "pylib.copy") {
      if (resArgs.size != 1) fail()
      genFunCall(genAttrBase(resArgs[0], "copy"), emptyList())
    } else if (e.func.v.name == "pylib.len" && !insideMethod) {
      if (e.args.size != 1 || e.keywords.isNotEmpty()) fail()
      atomic("|" + render(genExpr(e.args[0])) + "|")
    } else {
      fun renameCollMethod(fn: String, coll: TExpr, imPrefix: String = ""): RExpr {
        if (!fn.startsWith("pylib.")) TODO()
        val fn = fn.substring("pylib.".length)
        return if (insideMethod)
          RLit(imPrefix + fn)
        else {
          val collType = typer[coll].asType() as NamedType
          val prefix = when (collType.name) {
            "pylib.PyList", "pylib.Sequence" -> "seq_"
            "pylib.Set" -> "set_"
            else -> TODO()
          }
          RLit(prefix + fn)
        }
      }
      val funcName = when {
        e.func.v.name.startsWith("pylib.map") -> {
          renameCollMethod(e.func.v.name, e.args[1], "py")
        }
        e.func.v.name.startsWith("pylib.filter") -> {
          renameCollMethod(e.func.v.name, e.args[1])
        }
        e.func.v.name.startsWith("pylib.sum") -> {
          renameCollMethod(e.func.v.name, e.args[0])
        }
        e.func.v.name.startsWith("pylib.any") -> {
          renameCollMethod(e.func.v.name, e.args[0])
        }
        e.func.v.name.startsWith("pylib.max") -> {
          renameCollMethod(e.func.v.name, e.args[0])
        }
        e.func.v.name == "<assert>" -> RLit("pyassert")
        e.func.v.name == "<check>" -> fail()
        e.func.v.name == "pylib.list" -> {
          if (insideMethod)
            RLit("pylist")
          else {
            if (e.args.size != 1) TODO()
            val collType = typer[e.args[0]].asType() as NamedType
            when(collType.name) {
              "pylib.PyList", "pylib.Sequence" -> return genExpr(e.args[0])
              "pylib.Set" -> RLit("set_to_seq")
              else -> TODO()
            }
          }
        }
        e.func.v.name == "pylib.set" -> {
          if (insideMethod)
            RLit("pyset")
          else {
            if (e.args.size != 1) TODO()
            val collType = typer[e.args[0]].asType() as NamedType
            when(collType.name) {
              "pylib.Set" -> return genExpr(e.args[0])
              "pylib.PyList", "pylib.Sequence", "ssz.List" -> RLit("seq_to_set")
              else -> TODO()
            }
          }
        }
        e.func.v.name == "pylib.dict" -> {
          if (resArgs.isEmpty())
            return genCtorCall("pylib.Dict", emptyList())
          else
            RLit("pydict")
        }
        e.func.v.name == "pylib.iter" -> {
          if (e.args.size != 1 || e.keywords.isNotEmpty()) fail()
          if (insideMethod)
            RLit("iter")
          else
            return genExpr(e.args[0])
        }
        else -> {
          if (e.keywords.isNotEmpty()) fail()
          val funcName = if (e.func.v.name in methRename) {
            val classArgExists = e.args.any { canReferToClass(it, typer) }
            if (!insideMethod && !classArgExists)
              methRename[e.func.v.name]!!
            else
              e.func.v.name
          } else
            e.func.v.name
          genName(funcName)
          //genExpr(mkName(e.func.v.name), typer)
        }
      }
      if (insideMethod && findFuncDescr(e.func.v.name)?.memoryModel == MemoryModel.FRESH) {
        val t = getExprType(e).asType() as NamedType
        wrapFreshValue(t.name, genFunCall(funcName, resArgs))
      } else
        genFunCall(funcName, resArgs)
    }
  }

  fun genCall_Attribute(e: Call, typer: ExprTyper): RExpr {
    if (e.func !is Attribute) fail()
    fun getExprType(e: TExpr) = typer[e]
    fun genExpr(e: TExpr) = genExpr(e, typer)
    return when {
      !insideMethod && e.func.attr == "keys" -> {
        if (e.args.isNotEmpty() || e.keywords.isNotEmpty()) fail()
        val tgt = genExpr(e.func.value, typer)
        val t = canReferToClass(e.func.value, typer)
        genAttrBase(tgt, "Keys")
      }
      e.func.attr == "updated" -> {
        if (e.args.isNotEmpty()) TODO()
        val tgt = genExpr(e.func.value, typer)
        val args = e.keywords.map { atomic(it.arg!! + " := " + render(genExpr(it.value))) }
        atomic(render(tgt) + ".(" + args.joinToString(", ") { render(it) } + ")")
      }
      e.func.attr == "updated_at" -> {
        if (e.keywords.isNotEmpty() || e.args.size != 2) TODO()
        val tgt = genExpr(e.func.value, typer)
        val idx = genExpr(e.args[0])
        val value = genExpr(e.args[1])
        atomic(render(tgt) + "[" + render(idx) + " := " + render(value) + "]")
      }
      e.func.attr == "add_pure" -> {
        if (e.keywords.isNotEmpty() || e.args.size != 1) TODO()
        val tgt = genExpr(e.func.value, typer)
        val value = genExpr(e.args[0])
        atomic(render(tgt) + " + {" + render(value) + "}")
      }
      !insideMethod && e.func.attr == "intersection" -> {
        if (e.keywords.isNotEmpty() || e.args.size != 1) TODO()
        val tgt = genExpr(e.func.value, typer)
        val value = genExpr(e.args[0])
        val valueType = typer[e.args[0]].asType() as NamedType
        val value_ = when (valueType.name) {
          "pylib.Set" -> value
          "pylib.PyList", "pylib.Sequence", "ssz.List" -> genFunCall(RLit("seq_to_set"), listOf(value))
          else -> TODO()
        }
        atomic(render(tgt) + " * " + render(value_))
      }
      e.func.attr == "append_pure" -> {
        if (e.keywords.isNotEmpty() || e.args.size != 1) TODO()
        val tgt = genExpr(e.func.value, typer)
        val value = genExpr(e.args[0])
        atomic(render(tgt) + " + [" + render(value) + "]")
      }
      else -> {
        if (e.keywords.isNotEmpty())
          fail()
        val resArgs_: List<RExpr> = e.args.map { genExpr(it) }
        val resArgs = resArgs_.plus(e.keywords.map { atomic(it.arg!! + " := " + render(genExpr(it.value))) })
        val res = genFunCall(genAttrBase(genExpr(e.func.value, typer), e.func.attr), resArgs)
        val valType = getExprType(e.func.value).asType() as NamedType
        if (insideMethod && (e.func.attr + "()") in (dafnyFreshAttrs[valType.name] ?: emptySet())) {
          val t = getExprType(e).asType() as NamedType
          wrapFreshValue(t.name, res)
        } else res
      }
    }
  }

  fun genExpr(e: TExpr, typer: ExprTyper): RExpr {
    fun getExprType(e: TExpr) = typer[e]
    fun genExpr(e: TExpr) = genExpr(e, typer)
    return when (e) {
      is NameConstant -> genNameConstant(e)
      is Num -> genNum(e)
      is Str -> atomic(e.s)
      is Bytes -> genFunCall(RLit("pybytes.create"), listOf(atomic(e.s)))

      is Name -> genName(e.id)
      is Attribute -> {
        val valType = typer[e.value]
        val res = mkAttrLoad(genExpr(e.value), e.attr)
        if (insideMethod && valType is NamedType && e.attr in (dafnyFreshAttrs[valType.name] ?: emptySet())) {
          val exprType = typer[e]
          if (exprType !is NamedType) TODO()
          wrapFreshValue("pylib.List", res)
        } else res
      }
      is Subscript -> {
        val valueExpr = genExpr(e.value)
        when (e.slice) {
          is Index -> mkIndexLoad(e.value, e.slice.value, typer)
          is Slice -> {
            val lower = e.slice.lower?.let { genExpr(it) }
            val upper = e.slice.upper?.let { genExpr(it) }
            val step = e.slice.step?.let { genExpr(it) }
            mkSliceLoad(valueExpr, lower, upper, step)
          }
          else -> fail(e.slice.toString())
        }
      }

      is BinOp -> genExpr(mkCall("<${e.op}>", listOf(e.left, e.right)))
      is BoolOp -> genExpr(mkCall("<${e.op}>", e.values))
      is Compare -> genExpr(convertCompareSimple(e))
      is UnaryOp -> genExpr(mkCall("<${e.op}>", listOf(e.operand)))

      is Call -> {
        val type = getExprType(e.func).let { type ->
          if (type is NamedType) {
            if (e.func !is CTV || e.func.v !is ClassVal) TODO()
            type.`class`
          } else type
        }
        val resArgs: List<RExpr> = e.args.map { genExpr(it) }
        when {
          type is SpecialFuncRef -> {
            if (e.keywords.isNotEmpty()) TODO()
            val op = type.name.substring(1, type.name.length-1)
            genSpecialOpCall(op, e, typer)
          }
          type is Clazz -> {
            if (e.keywords.isNotEmpty()) TODO()
            val t = type.toInstance()
            val fh = RLit(typeToStr(t.copy(name = t.name + "_new")))
            val resArgs = if (isSubType_new(t, TPyBytes) && resArgs.isNotEmpty()) {
              if (resArgs.size > 1) fail()
              val resArg = if (render(resArgs[0]).startsWith("\""))
                atomic(render(resArgs[0]).substring(1, render(resArgs[0]).length-1))
              else resArgs[0]
              listOf(resArg)
            } else resArgs
            genCtorCall(t.name, resArgs)
          }
          e.func is Attribute -> {
            genCall_Attribute(e, typer)
          }
          e.func is CTV && e.func.v is FuncInst -> {
            genCall_FuncInst(e, typer)
          }
          e.func is Name -> {
            genFunCall(genExpr(e.func), resArgs)
          }
          else -> TODO()
        }
      }

      is IfExp -> when {
        e.body == NameConstant(true) -> genBoolOp(listOf(genExpr(e.test), genExpr(e.orelse)), EBoolOp.Or)
        e.orelse == NameConstant(false) -> genBoolOp(listOf(genExpr(e.test), genExpr(e.body)), EBoolOp.And)
        else -> genIfExpr(genExpr(e.test), genExpr(e.body), genExpr(e.orelse))
      }

      is Tuple -> genTuple(e.elts.map { genExpr(it) })
      is PyList -> genPyList(e.elts.map { genExpr(it) })
      is PyDict -> genPyDict(e.keys.zip(e.values).map { genExpr(it.first) to genExpr(it.second) })
      is GeneratorExp -> genComprehension(e.elt, e.generators, typer)
      is ListComp -> genExpr(mkCall("list", listOf(GeneratorExp(e.elt, e.generators))))
      is Lambda -> {
        val lamType = typer[e] as FunType
        val newExprTypes = typer.updated(e.args.args.zip(lamType.argTypes).map { it.first.arg to it.second })
        val lvs = liveVarAnalysis(e)
        val liveVars = lvs.filter { newExprTypes.ctx.contains(it) && !it.startsWith("<") }
        if (insideMethod && e.args.args.any { isDafnyClassKind(mkName(it.arg), newExprTypes, DafnyClassKind.Class) })
          TODO()
        val prevHVs = heapVars.intersect(liveVars)
        val newHVs = if (insideMethod)
          liveVars.minus(heapVars).filter { isDafnyClassKind(mkName(it), newExprTypes, DafnyClassKind.Class) }
        else emptyList()
        val hvs = prevHVs.union(newHVs)
        atomic(genLambda(e.args.args.map(::genArg), this.with(insideMethod = false, heapVars = hvs).genExpr(e.body, newExprTypes)))
      }
      is Starred -> RPrefix("*", genExpr(e.value))
      is CTV -> when(e.v) {
        is ConstExpr -> genExpr(e.v.e)
        else -> TODO()
      }
      is Let -> genLet(e, typer)
      else -> fail(e.toString())
    }
  }


  private fun wrapFreshValue(exprClass: String, res: RExpr): RInfix {
    val fn = when(exprClass) {
      "ssz.List" -> "PyList_new"
      "pylib.Set" -> "Set_new"
      "pylib.Dict" -> "Dict_new"
      "pylib.List" -> "PyList_new"
      "pylib.Sequence" -> "Sequence_new"
      else -> TODO()
    }
    return genFunCall(atomic(fn), listOf(res))
  }


  fun genComprehension(e: TExpr, gs: List<Comprehension>, typer: ExprTyper): RExpr {
    if (gs.size != 1)
      fail("too many generators")
    val c = gs[0]
    val vtPairs = matchVarsAndTypes(c.target, getIterableElemType(typer[c.iter].asType()))
    val newExprTypes = typer.updated(vtPairs.map { it.first.id to it.second })
    if (c.target !is Name) TODO()
    val lamArgName = c.target.id
    val ifs = c.ifs.map {
      "filter" to genLambda(listOf(lamArgName), genExpr(it, newExprTypes))
    }
    val list = ifs + ("map" to genLambda(listOf(lamArgName), genExpr(e, newExprTypes)))
    return list.fold(genExpr(c.iter, typer), { pe, a -> applyMapFilter(pe, a.first, a.second) })
  }

  fun applyMapFilter(e: RExpr, a: String, l: String): RExpr {
    val n = if (a == "map") "map_" else a
    return RInfix("", atomic(n), RSeq("(", ")", ",", listOf(atomic(l), e)))
  }

  fun LVInfo.toTExpr(): TExpr = when(this) {
    is LVName -> mkName(this.name)
    is LVAttr -> mkAttribute(this.e.toTExpr(), this.attr)
    else -> TODO()
  }

  fun mkAttrLoad(e: RExpr, a: identifier): RExpr {
    return atomic("${render(e)}.$a")
  }

  fun mkIndexLoad(value_: TExpr, index_: TExpr, typer: ExprTyper): RExpr {
    val value = genExpr(value_, typer)
    val index = genExpr(index_, typer)
    return if (insideMethod || isNonLocal(value_, typer))
      mkAttrCall(value, "get", listOf(index))
    else {
      val t = typer[value_].asType() as NamedType
      when(t.name) {
        "pylib.Dict" -> genFunCall(atomic("map_get"), listOf(value, index))
        "ssz.List" -> genFunCall(atomic("seq_get"), listOf(value, index))
        else -> TODO()
      }
    }
  }

  fun mkSliceLoad(value: RExpr, lower: RExpr?, upper: RExpr?, step: RExpr?): RExpr = TODO()

  fun genBinOp(a: RExpr, o: EBinOp, b: RExpr): RExpr {
    val op = genOperator(o)
    return if (op[0].isLetter()) {
      atomic("$op(${render(a)}, ${render(b)})")
    } else {
      RInfix(op, a, b)
    }
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

  fun genBoolOp(exprs: List<RExpr>, op: EBoolOp): RExpr = RInfix(genTBoolop(op), exprs)

  fun genUnaryOp(a: RExpr, op: EUnaryOp): RExpr = RPrefix(genUnaryop(op), a)

  fun genOperator(o: EBinOp): String {
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

  private fun genSpecialOpCall(op: String, e: Call, typer: ExprTyper): RExpr {
    fun genExpr(e: TExpr) = genExpr(e, typer)
    return when (op) {
      in TypeResolver.binOps -> {
        if (e.args.size != 2) fail()
        genBinOp(genExpr(e.args[0]), EBinOp.valueOf(op), genExpr(e.args[1]))
      }

      in TypeResolver.boolOps -> {
        genBoolOp(e.args.map(::genExpr), EBoolOp.valueOf(op))
      }

      in TypeResolver.cmpOps -> {
        if (e.args.size != 2) fail()
        genCmpOp(e.args[0], ECmpOp.valueOf(op), e.args[1], typer)
      }

      in TypeResolver.unaryOp -> {
        if (e.args.size != 1) fail()
        genUnaryOp(genExpr(e.args[0]), EUnaryOp.valueOf(op))
      }

      in setOf("dict") -> {
        val (keys, values) = e.args.map {
          val elts = (it as Tuple).elts
          if (it.elts.size != 2) fail()
          elts[0] to elts[1]
        }.unzip()
        genExpr(PyDict(keys, values))
      }
      else -> TODO(op)
    }
  }

  fun genCmpOp(ae: TExpr, op: ECmpOp, be: TExpr, typer: ExprTyper): RExpr {
    val a = genExpr(ae, typer)
    val b = genExpr(be, typer)
    return if (op == ECmpOp.Is && render(b) == "null")
      RInfix("==", a, b)
    else if (op == ECmpOp.IsNot && render(b) == "null")
      RInfix("!=", a, b )
    else if (op == ECmpOp.In || op == ECmpOp.NotIn) {
      genInOp(ae, be, op == ECmpOp.NotIn, typer)
    } else
      RInfix(genCmpop(op), a, b)
  }

  fun genInOp(a: TExpr, b: TExpr, invert: Boolean, typer: ExprTyper): RExpr {
    return if (!insideMethod && !isNonLocal(b, typer)) {
      val op = if (invert) "!in" else "in"
      atomic(render(genExpr(a, typer)) + " $op " + render(genExpr(b, typer)))
    } else {
      val res = mkAttrCall(genExpr(b, typer), "contains", listOf(genExpr(a, typer)))
      if (invert)
        genUnaryOp(res, EUnaryOp.Not)
      else
        res
    }
  }


  fun genIfExpr(t: RExpr, b: RExpr, e: RExpr): RExpr = atomic("if (${render(t)}) then ${render(b)} else ${render(e)}")

  fun genLet(e: Let, typer: ExprTyper): RExpr {
    var ctx = typer
    var exprGen = this
    val bindings = e.bindings.map {
      val va = genVarAssignment(it.names, it.value, ctx)
      ctx = ctx.updated(matchNamesAndTypes(it.names, ctx[it.value].asType()))
      if (exprGen.canReferToClass(it.value, ctx)) {
        if (it.names.size != 1) TODO()
        exprGen = exprGen.with(heapVars = exprGen.heapVars.plus(it.names))
      }
      va
    }
    return RPExpr(MIN_PRIO, bindings.joinToString(" ") + " " + render(exprGen.genExpr(e.value, ctx)))
  }

  fun genVarAssignment(lv: List<String>, value: TExpr, typer: ExprTyper): String {
    val tgt = when {
      lv.isEmpty() -> fail()
      lv.size == 1 -> lv[0]
      else -> lv.joinToString(", ", "(", ")")
    }
    if (isExceptionCheck(value)) {
      (((value as Call).func as CTV).v as FuncInst)
      if (value.args.size != 1 || value.keywords.isNotEmpty()) fail()
      val valueE = render(genExpr(value.args[0], typer))
      return "var ${tgt} :- " + valueE + ";"
    } else {
      val valueE = render(genExpr(value, typer))
      return "var ${tgt} := $valueE;"
    }
  }
  fun genVarAssignment(lv: Name, value: TExpr, typer: ExprTyper): String {
    return genVarAssignment(listOf(lv.id), value, typer)
  }


  fun toShortName(tn: String): String {
    val idx = tn.lastIndexOf(".")
    return if (idx == -1) tn else tn.substring(idx + 1)
  }
  fun typeToStr(t: RTType): String = when {
    t is NamedType && (t.name == "None" || t.name == "pylib.None") -> "()"
    t is NamedType && t.name == "<Outcome>" -> typeToStr(t.copy(name = "Outcome"))
    t is NamedType && t.name == "Tuple" -> t.tParams.map { typeToStr(it) }.joinToString(", ", "(", ")")
    t is NamedType -> {
      val clsName = toShortName(t.name)
      if (t.tParams.isEmpty()) clsName else clsName + "<" + t.tParams.map { typeToStr(it) }.joinToString(",") + ">"
    }
    else -> TODO()
  }

  fun genArg(a: Arg): String {
    return a.arg + (a.annotation?.let { ": " + genNativeType(it) } ?: "")
  }

  fun genLambda(args: List<String>, body: RExpr): String {
    return "(" + args.joinToString(", ") + ") => ${render(body)}"
  }


  fun atomic(s: String) = RPExpr(MAX_PRIO, s)

  fun genAttrBase(e: RExpr, a: identifier): RExpr = RInfix(".", e, RLit(a))
  fun mkAttrCall(e: RExpr, attr: identifier, args: List<RExpr>) = genFunCall(genAttrBase(e, attr), args)
  fun genFunCall(funcExpr: RExpr, args: List<RExpr>) =
      RInfix("", funcExpr, RSeq("(", ")", ",", args.map { it }))

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
}