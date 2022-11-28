package onotole.type_inference

import onotole.*
import onotole.typelib.*
import onotole.util.toClassVal
import onotole.util.toFAtom


abstract class FAtomTypeCalculator(): TypeCalculator<FAtom> {
  override val boolT = FAtom("pylib.bool")
  override val bytesT = FAtom("pylib.bytes")
  override val numT = FAtom("pylib.int")
  override val strT = FAtom("pylib.str")
  override val noneT = FAtom("pylib.None")
  override fun parseType(e: TExpr): FAtom {
    return TODO()
  }
  override fun isSubType(a: FAtom, b: FAtom): Boolean {
    with(TypingContext) {
      return st(a, b).first
    }
  }

  override fun join(a: FAtom, b: FAtom): FAtom {
    with(TypingContext) {
      val res = tryJoin(a, b)
      return if (res.first == res.second)
        res.first
      else fail("can't calculate join of $a nd $b")
    }
  }

  override fun mkListType(t: FAtom): FAtom = FAtom("pylib.PyList", listOf(t))
  override fun mkSetType(t: FAtom): FAtom = FAtom("pylib.Set", listOf(t))
  override fun mkDictType(k: FAtom, v: FAtom): FAtom = FAtom("pylib.Dict", listOf(k, v))
  override fun mkTupleType(elts: List<FAtom>): FAtom = FAtom("pylib.Tuple", elts)
  override fun resolveAttrGet(t: FAtom, attr: identifier): FAtom {
    with(TypingContext) {
      return resolveAttributeGet(t, attr) as FAtom
    }
  }
}

object TypingContext: TypingCtx {
  var classes: MutableMap<String,TLClassDecl> = mutableMapOf()
  var constTypes = mutableMapOf<String,FAtom>()

  override fun getClassDecl(cls: String) = classes[cls]
      ?: fail()
  fun initConstants(consts: Map<String,FAtom>) {
    constTypes.putAll(consts)
  }

  fun registerModules(res: List<TLModule>) {
    classes.putAll((res.flatMap { it.declarations }).filterIsInstance<TLClassDecl>().associateBy { it.head.name })
  }

  override fun getBase(cls: FAtom): FAtom? {
    return when(cls.n) {
      "pylib.object" -> null
      "pylib.Tuple" -> FAtom("pylib.object")
      in classes -> {
        val classDescr = classes[cls.n]!!
        val cd = classDescr.head
        if (cls.ps.size != cd.noTParams) fail()
        val tvAssgn = cd.tvars.zip(cls.ps).toMap()
        classDescr.parent?.let { it.toFAtom(tvAssgn) as FAtom }
      }
      else -> FAtom("pylib.object")
    }
  }

  override fun getTypeParams(a: FAtom): Triple<List<Int>,List<Int>,List<Int>> {
    return when(a.n) {
      "pylib.Set" -> Triple(listOf(0), emptyList(), emptyList())
      "pylib.Iterator" -> Triple(listOf(0), emptyList(), emptyList())
      "pylib.Iterable" -> Triple(listOf(0), emptyList(), emptyList())
      "pylib.Collection" -> Triple(listOf(0), emptyList(), emptyList())
      "pylib.Sequence" -> Triple(listOf(0), emptyList(), emptyList())
      "pylib.PyList" -> Triple(emptyList(), listOf(0), emptyList())
      "pylib.Callable" ->
        Triple(listOf(a.ps.size-1), emptyList(), (0 until (a.ps.size-1)).toList())
      "pylib.Dict" -> Triple(emptyList(), listOf(0,1), emptyList())
      "pylib.Tuple" -> Triple(a.ps.indices.toList(), emptyList(), emptyList())
      "ssz.List" -> Triple(emptyList(), listOf(0), emptyList())
      "ssz.Vector" -> Triple(emptyList(), listOf(0), emptyList())
      else -> if (a.ps.isEmpty())
        Triple(listOf(), listOf(), listOf())
      else
        TODO()
    }
  }

}

context (TypingCtx)
fun resolveAttributeGet(cls: FAtom, attr: String): FTerm {
  if (cls.n == "pylib.Optional") {
    return resolveAttributeGet(cls.ps[0] as FAtom, attr)
  }
  if (cls.n == "phase0.Store" && attr == "updated") {
    return TLTCallable(emptyList(), classValToTLType(cls.toClassVal())).toFAtom(emptyMap())
  }
  val classesWithCopy = setOf("phase0.BeaconState", "altair.BeaconState", "bellatrix.BeaconState")
  if (cls.n in classesWithCopy && attr == "copy") {
    return TLTCallable(emptyList(), classValToTLType(cls.toClassVal())).toFAtom(emptyMap())
  }

  if (attr in int_funcs && st(cls,FAtom("ssz.uint")).first) {
    return TLTCallable(listOf(TLTClass("pylib.int", emptyList())), classValToTLType(cls.toClassVal())).toFAtom(emptyMap())
  }
  when(cls.n) {
    in TypingContext.classes -> {
      val classDescr = TypingContext.classes[cls.n]!!
      val cd = classDescr.head
      if (cls.ps.size != cd.noTParams) fail()
      val tvAssgn = cd.tvars.zip(cls.ps).toMap()
      val attrs = classDescr.attrs
      if (attr in attrs) {
        val f = attrs[attr]!!
        if (f !is TLTClass && f !is TLTCallable)
          fail()
        return f.toFAtom(tvAssgn)
      } else if (classDescr.parent != null)
        return resolveAttributeGet(classDescr.parent.toFAtom(tvAssgn) as FAtom, attr)
      TODO()
    }
  }
  TODO()
}

context (TypingCtx)
fun resolveIndexGet(cls: FAtom, idx: FTerm): FTerm {
  return resolveAttributeCall(cls, "__getitem__").first
}

fun resolveSliceGet(cls: FAtom, lower: FTerm?, upper: FTerm?, step: FTerm?): FTerm {
  return when(cls.n) {
    "ssz.Bitlist" -> FAtom("pylib.Sequence", listOf(FAtom("pylib.int")))
    "ssz.Bitvector" -> FAtom("pylib.Sequence", listOf(FAtom("pylib.int")))
    "ssz.Bytes32","phase0.Root" -> FAtom("pylib.bytes")
    "pylib.Sequence" -> cls
    "pylib.PyList" -> FAtom("pylib.Sequence", cls.ps)
    else -> TODO()
  }
}


val int_funcs = setOf(
    "add", "sub","mult","floordiv","mod"
).flatMap { setOf(it, "r$it") }.map { "__${it}__" }.toSet()
context (TypingCtx)
fun resolveAttributeCall(cls: FAtom, attr: String): Pair<FTerm,List<FTerm>> {
  val attr = attr.toLowerCase()
  val res = resolveAttributeGet(cls, attr) as FAtom
  if (res.n != "pylib.Callable") fail()
  return res.ps[res.ps.size-1] to res.ps.subList(0, res.ps.size-1)
//  val classesWithCopy = setOf("phase0.BeaconState", "altair.BeaconState", "bellatrix.BeaconState")
//  if (cls.n in classesWithCopy && attr == "copy") {
//    return cls to emptyList()
//  }
//  if (attr in int_funcs && st(cls,FAtom("ssz.uint")).first) {
//    return cls to listOf(FAtom("pylib.int"))
//  }
}

context (TypingCtx)
fun resolveSpecialOpType(op: String, args: List<FAtom>): FTerm {
  return when(op) {
    in TypeResolver.binOps, in TypeResolver.cmpOps -> {
      if (args.size != 2) fail()
      val (aType, bType) = args[0] to args[1]
      if (op in TypeResolver.binOps) {
        if (aType.n != bType.n && st(bType, aType).first)
          resolveAttributeCall(bType, "__r" + op + "__").first
        else
          resolveAttributeCall(aType, "__" + op + "__").first
      } else if (op in TypeResolver.cmpOps) {
        FAtom("pylib.bool")
      } else TODO("shouldn't happen")
    }
    in TypeResolver.boolOps -> TODO()
    in TypeResolver.unaryOp -> {
      if (args.size != 1) fail()
      val aType = args[0]
      if (op == "Not") {
        FAtom("pylib.bool")
      } else if (op == "USub") {
        resolveAttributeCall(aType, "__neg__").first
      } else TODO()
    }
    else -> fail()
  }
}

context (TypingCtx)
fun resolveConstExprType(ce: ConstExpr): FAtom {
  return calcConstExprType(ce.e, TypingContext.constTypes, emptyMap())
}

context (TypingCtx)
fun resolveExprType(e: TExpr, vars: Map<String, FTerm>, cs: IConstrStore<FTerm>): FTerm? {
  fun resolveExprType(e: TExpr) = resolveExprType(e, vars, cs)
  return when(e) {
    is CTV -> if (e.v is ConstExpr) resolveConstExprType(e.v) else TODO()
    is NameConstant -> when(e.value) {
      null -> FAtom("pylib.None")
      true, false -> FAtom("pylib.bool")
      else -> TODO()
    }
    is Str -> FAtom("pylib.str")
    is Num -> FAtom("pylib.int")
    is Name -> vars["T" + e.id] ?: FVar("T" + e.id)
    is Attribute -> {
      when(val valType = resolveExprType(e.value)) {
        null, is FVar -> null
        is FAtom -> resolveAttributeGet(valType, e.attr)
      }
    }
    is Subscript -> {
      when(val valType = resolveExprType(e.value)) {
        null, is FVar -> null
        is FAtom -> {
          when(e.slice) {
            is Index -> {
              if (valType.n == "pylib.Tuple" && e.slice.value is CTV
                  && e.slice.value.v is ConstExpr && e.slice.value.v.e is Num) {
                valType.ps[e.slice.value.v.e.n.toInt()]
              } else {
                val idxType = resolveExprType(e.slice.value)
                when (idxType) {
                  null, is FVar -> null
                  is FAtom -> resolveIndexGet(valType, idxType)
                }
              }
            }
            is Slice -> {
              if (e.slice.lower != null) {
                resolveExprType(e.slice.lower)
              }
              if (e.slice.upper != null) {
                resolveExprType(e.slice.upper)
              }
              if (e.slice.step != null) {
                resolveExprType(e.slice.step)
              }
              resolveSliceGet(valType, null, null, null)
            }
            else -> fail()
          }
        }
      }
    }
    is Call -> {
      if (e.func is CTV) {
        val argTypes = e.args.map { resolveExprType(it) }
        val kwdTypes = e.keywords.map { it.arg!! to resolveExprType(it.value) }
        val fh = if (e.func.v is ClassVal) {
          toFAtom(parseTypeDecl(e.func.v))
        } else if (e.func.v is FuncInst) {
          if (argTypes.plus(kwdTypes.map { it.second }).all { it != null }) {
            val funcParams = e.func.v.sig.args.toMap()
            val argParams = e.func.v.sig.args.subList(0, argTypes.size).map { it.second.toFAtom(emptyMap()) }
            val kwdParams = kwdTypes.map { funcParams[it.first]!!.toFAtom(emptyMap()) }
            argTypes.zip(argParams).forEach {
              cs.addST(it.first!!, it.second)
            }
            kwdTypes.zip(kwdParams).forEach {
              cs.addST(it.first.second!!, it.second)
            }
          }
          e.func.v.sig.tParams.filterIsInstance<TLTVar>().forEach { v -> vars[v.name]?.let { lb ->
            cs.addST(lb, FVar(v.name))
          } }
          e.func.v.sig.ret.toFAtom(emptyMap())
          //toFTerm(e.func.v.sig.ret)
        } else fail()
        fh
      } else if (e.func is Attribute) {
        if (e.keywords.isNotEmpty()) TODO()
        when(val valType = resolveExprType(e.func.value)) {
          null, is FVar -> null
          is FAtom -> {
            val argTypes = e.args.map { resolveExprType(it) }
            val (retType, paramTypes) = resolveAttributeCall(valType, e.func.attr)
            if (argTypes.size != paramTypes.size) fail()
            if (argTypes.all { it != null }) {
              argTypes.zip(paramTypes).forEach {
                cs.addST(it.first!!, it.second)
              }
            }
            retType
          }
        }
      } else if (e.func is Name && e.func.id in TypeResolver.specialFuncNames) {
        if (e.keywords.isNotEmpty()) fail()
        val op = e.func.id.substring(1, e.func.id.length-1)
        when(op) {
          in TypeResolver.binOps, in TypeResolver.cmpOps -> {
            if (e.args.size != 2) fail()
            val (a, b) = e.args[0] to e.args[1]
            val (aType, bType) = resolveExprType(a) to resolveExprType(b)
            if (op in TypeResolver.binOps) {
              if (aType != null && aType is FAtom && bType != null && bType is FAtom) {
                if (aType.n != bType.n && st(bType, aType).first)
                  resolveAttributeCall(bType, "__r" + op + "__").first
                else
                  resolveAttributeCall(aType, "__" + op + "__").first
              } else null
            } else if (op in TypeResolver.cmpOps) {
              FAtom("pylib.bool")
            } else TODO("shouldn't happen")
          }
          in TypeResolver.boolOps -> TODO()
          in TypeResolver.unaryOp -> {
            if (e.args.size != 1) fail()
            val aType = resolveExprType(e.args[0])
            if (op == "Not") {
              FAtom("pylib.bool")
            } else if (op == "USub") {
              if (aType != null && aType is FAtom)
                resolveAttributeCall(aType, "__neg__").first
              else null
            } else TODO()
          }
          else -> fail()
        }
      } else fail()
    }
    is IfExp -> {
      val testVal = resolveExprType(e.test)
      //if (testVal != null) {
      //  cs.addST(testVal, FAtom("pylib.bool"))
      //}
      val bodyVal = resolveExprType(e.body)
      val orelseVal = resolveExprType(e.orelse)
      if (bodyVal != null || orelseVal != null) {
        val v = cs.freshVar("Iif")
        bodyVal?.let { cs.addST(it, v) }
        orelseVal?.let { cs.addST(it, v) }
        v
      } else null
    }
    is Let -> {
      if (e.bindings.flatMap { it.names }.intersect(vars.keys).isNotEmpty()) fail()
      e.bindings.forEach {
        processStmt(Assign(namesToTExpr(it.names, true), it.value), vars, cs)
      }
      resolveExprType(e.value)
    }
    is Tuple -> {
      val eltTypes = e.elts.map { resolveExprType(it) }
      if (eltTypes.any { it == null }) null
      else FAtom("pylib.Tuple", eltTypes.map { it!! })
    }
    is GeneratorExp -> {
      if (e.generators.size != 1) fail()
      val g = e.generators[0]
      val localVars = when (g.target) {
        is Name -> listOf(g.target.id)
        is Tuple -> g.target.elts.map { (it as Name).id }
        else -> fail()
      }.map { "T$it" }
      val iterType = resolveExprType(g.iter)
      val elemType = iterType?.let {
        if (it is FAtom) resolveAttributeCall(it, "__getitem__").first else null
      }
      if (elemType != null) {
        val valueTypes = when (elemType) {
          is FVar -> if (localVars.size == 1) listOf(elemType) else TODO()
          is FAtom -> if (localVars.size == 1)
              listOf(elemType)
            else if (elemType.n == "pylib.Tuple" && elemType.ps.size == localVars.size)
              elemType.ps
            else fail()
        }
        val newVars = vars.plus(localVars.zip(valueTypes))
        g.ifs.forEach {
          resolveExprType(it, newVars, cs)
        }
        resolveExprType(e.elt, newVars, cs)?.let {
          FAtom("pylib.Sequence", listOf(it))
        }
      } else null
    }
    is Lambda -> {
      fun convert(t: TExpr): FTerm {
        val v = ((t as CTV).v as ExTypeVar).v
        return vars[v] ?: FVar(v)
      }
      val argTypes = e.args.args.map { convert(it.annotation!!) }
      val retType = convert(e.returns!!)

      if (argTypes.all { it !is FVar }) {
        val localVars = e.args.args.map { "T${it.arg}" }
        val newVars = vars.plus(localVars.zip(argTypes))
        val resType = resolveExprType(e.body, newVars, cs)
        if (resType != null)
          cs.addEQ(retType, resType)
      }
      FAtom("pylib.Callable", argTypes.plus(retType))
    }
    is Starred -> null // TODO
    else -> TODO()
  }
}
context (TypingCtx)
private fun processStmt(s: Stmt, vars: Map<String, FTerm>, cs: IConstrStore<FTerm>) {
  when(s) {
    is Expr -> resolveExprType(s.value, vars, cs)
    is Assert -> {
      val testType = resolveExprType(s.test, vars, cs)
      //if (testType != null) {
      //  cs.addST(testType, FAtom("pylib.bool"))
      //}
      if (s.msg != null) {
        val msgType = resolveExprType(s.test, vars, cs)
        //if (msgType != null) {
        //  cs.addST(msgType, FAtom("pylib.str"))
        //}
      }
    }
    is Assign -> {
      val valType = resolveExprType(s.value, vars, cs)
      when(s.target) {
        is Name -> {
          val vn = s.target.id
          if (valType != null) {
            cs.addST(valType, cs.mkVar("T$vn"))
          }
        }
        is Subscript -> {
          val tgtType = resolveExprType(s.target, vars, cs)
          val subscrValType = resolveExprType(s.target.value, vars, cs)
          when(s.target.slice) {
            is Index -> {
              val idxType = resolveExprType(s.target.slice.value, vars, cs)
            }
            is Slice -> {
              if (s.target.slice.lower != null) {
                resolveExprType(s.target.slice.lower, vars, cs)
              }
              if (s.target.slice.upper != null) {
                resolveExprType(s.target.slice.upper, vars, cs)
              }
              if (s.target.slice.step != null) {
                resolveExprType(s.target.slice.step, vars, cs)
              }
            }
            else -> TODO()
          }
        }
        is Attribute -> {
          val tgtType = resolveExprType(s.target, vars, cs)
          val tgtValType = resolveExprType(s.target.value, vars, cs)
          val valType = resolveExprType(s.value, vars, cs)
        }
        is Tuple -> {
          if (valType is FVar) {
            val vs = s.target.elts.map { e -> cs.mkVar("T${(e as Name).id}") }
            cs.addST(valType, FAtom("pylib.Tuple", vs))
          } else
            TODO()
        }
        else -> TODO()
      }
    }
    is AnnAssign -> {
      val vn = (s.target as Name).id
      cs.addEQ(cs.mkVar("T$vn"), toFAtom(parseTypeDecl(s.annotation as CTV)))
      if (s.value != null) {
        val valType = resolveExprType(s.value, vars, cs)
        if (valType != null) {
          cs.addST(valType, cs.mkVar("T$vn"))
        }
      }
    }
    is If -> {
      val testType = resolveExprType(s.test, vars, cs)
      //if (testType != null) {
      //  cs.addST(testType, FAtom("pylib.bool"))
      //}
      s.body.forEach { processStmt(it, vars, cs) }
      s.orelse.forEach { processStmt(it, vars, cs) }
    }
    is While -> {
      val testType = resolveExprType(s.test, vars, cs)
      //if (testType != null) {
      //  cs.addST(testType, FAtom("pylib.bool"))
      //}
      s.body.forEach { processStmt(it, vars, cs) }
    }
    is Return -> {
      val valType = s.value?.let { resolveExprType(it, vars, cs) } ?: FAtom("pylib.None")
      cs.addST(valType, cs.mkVar("Treturn"))
    }
    is Break -> {}
    else -> TODO()
  }
}

fun inferTypes2(fd: FunctionDef, ssa: Boolean = false): Map<String,ClassVal> {
  val f = if (ssa) fd else convertToAndOutOfSSA(fd)

  with(TypingContext) {
    val tc = TypeChecker()
    val fn = FreshNames()
    val constrs = mutableListOf<CConstr>()
    val delayedConstrs = mutableListOf<CConstr>()
    val ccs = object : CConstrStore {
      override fun newVar(n: String) = FVar(fn.fresh(n))
      override fun mkVar(n: String) = FVar(n)
      override fun addConstr(c: CConstr, delayed: Boolean) {
        if (delayed)
          delayedConstrs.add(c)
        else
          constrs.add(c)
      }
    }
    tc.processFunc(f, ccs)
    val coStore = tc_solve(constrs)
    tc_check_delayed(coStore, delayedConstrs)

    val vars = coStore.eqs.keys.plus(coStore.lbs.keys).plus(coStore.ubs.keys)
    val varValues = vars.associate { v ->
      val value = coStore.eqs[v]
          ?: coStore.lbs[v]?.let { if (it.size != 1) fail() else it.first() }
          ?: coStore.ubs[v]?.let { if (it.size != 1) fail() else it.first() }
          ?: TODO()
      v.v to value
    }

    fun getVars(t: FTerm): Set<FVar> = when (t) {
      is FVar -> setOf(t)
      is FAtom -> t.ps.flatMap(::getVars).toSet()
    }

    val vs = postorder(varValues.mapValues { getVars(it.value) }.mapKeys { FVar(it.key) })
    val varValues2 = mutableMapOf<String, FTerm>()
    vs.forEach { v ->
      varValues2[v.v] = replaceTypeVars(varValues[v.v]!!, varValues2)
    }
    return varValues2.mapValues { it.value.toClassVal() }
  }
}

fun inferTypes(fd: FunctionDef): Map<String,ClassVal> {
  val f = convertToAndOutOfSSA(fd)
  pyPrintFunc(f)
  println()

  val args = f.args.args.associate { "T" + it.arg to toFAtom(parseTypeDecl(it.annotation!! as CTV)) }

  with(TypingContext) {
    var curr: Map<String, FAtom> = args
    var currEqs: List<Pair<String, FAtom>> = emptyList()
    do {
      val constraints = ConstrStore()
      val currVars = curr.plus(currEqs)
      f.body.forEach { s ->
        processStmt(s, currVars, constraints)
      }
      constraints.simplify()
      currEqs = constraints.eqs.eqs.sub.map { it.key.name to constraints.eqs.convertBack(it.value) }.filter { it.second is FAtom }.map { it.first to it.second as FAtom }
      val res = solve(constraints.stConstraints, constraints.fn).mapKeys { it.key.v }
      val res2 = res.mapValues { replaceTypeVars(it.value, res) as FAtom }
      val prev = curr
      curr = curr.plus(res2)
    } while (prev != curr)

    val res1 = (curr + currEqs).mapValues { replaceTypeVars(it.value, curr) }
    return res1.mapValues { it.value.toClassVal() }
  }
}

fun replaceTypeVars(t: FTerm, vars: Map<String,FTerm>): FTerm = when(t) {
  is FVar -> vars[t.v] ?: t
  is FAtom -> t.copy(ps = t.ps.map { replaceTypeVars(it, vars) })
}
fun main() {
  val f = FunctionDef(name = "test",
      args = Arguments(args = listOf(Arg("n", Name("pylib.int", ExprContext.Load)))),
      returns = Name("pylib.int", ExprContext.Load),
      body = listOf(
          Assign(mkName("a", true), Num(0)),
          While(NameConstant(true), body = listOf(
              Assign(mkName("a", true), BinOp(mkName("a"), EBinOp.Add, mkName("n")))
          ))
      )
  )
  val fd = desugarExprs(f)
  inferTypes(fd)
}