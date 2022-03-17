package onotole.type_inference

import onotole.*
import onotole.typelib.*

object TypingContext {
  var classes: MutableMap<String,TLClassDecl> = mutableMapOf()
  var constTypes = mutableMapOf<String,FAtom>()
  fun initConstants(consts: Map<String,FAtom>) {
    constTypes.putAll(consts)
  }

  fun registerModules(res: List<TLModule>) {
    classes.putAll((res.flatMap { it.declarations }).filterIsInstance<TLClassDecl>().map { it.head.name to it }.toMap())
  }
}
fun resolveAttributeGet(cls: FAtom, attr: String): FTerm {
  if (cls.n == "pylib.Optional") {
    return resolveAttributeGet(cls.ps[0] as FAtom, attr)
  }
  when(cls.n) {
    in TypingContext.classes -> {
      val classDescr = TypingContext.classes[cls.n]!!
      val cd = classDescr.head
      if (cls.ps.size != cd.tvars.size) fail()
      val tvAssgn = cd.tvars.zip(cls.ps).toMap()
      val attrs = classDescr.attrs
      if (attr in attrs) {
        val f = attrs[attr]!!
        if (f !is TLTClass) fail()
        return f.toFAtom(emptyMap())
      }
      TODO()
    }
  }
  TODO()
}

fun resolveIndexGet(cls: FAtom, idx: FTerm): FTerm {
  return resolveAttributeCall(cls, "__getitem__").first
}

fun resolveSliceGet(cls: FAtom, lower: FTerm?, upper: FTerm?, step: FTerm?): FTerm {
  return when(cls.n) {
    "ssz.Bitlist" -> FAtom("pylib.Sequence", listOf(FAtom("pylib.int")))
    "ssz.Bitvector" -> FAtom("pylib.Sequence", listOf(FAtom("pylib.int")))
    "ssz.Hash32","phase0.Root" -> FAtom("pylib.bytes")
    "pylib.Sequence" -> cls
    "pylib.PyList" -> FAtom("pylib.Sequence", cls.ps)
    else -> TODO()
  }
}


fun TLType.toFAtom(tvm: Map<String,FTerm>): FTerm = when(this) {
  is TLTVar -> tvm[this.name] ?: FVar(this.name)
  is TLTClass -> FAtom(this.name, this.params.filter { it !is TLTConst }.map { it.toFAtom(tvm) })
  is TLTCallable -> FAtom("pylib.Callable", this.args.plus(this.ret).map { it.toFAtom(tvm) })
  else -> TODO()
}
val int_funcs = setOf(
    "add", "sub","mult","floordiv","mod"
).flatMap { setOf(it, "r$it") }.map { "__${it}__" }.toSet()
fun resolveAttributeCall(cls: FAtom, attr: String): Pair<FTerm,List<FTerm>> {
  val attr = attr.toLowerCase()

  val classesWithCopy = setOf("phase0.BeaconState", "altair.BeaconState", "bellatrix.BeaconState")
  if (cls.n in classesWithCopy && attr == "copy") {
    return cls to emptyList()
  }

  if (attr in int_funcs && st(cls,FAtom("ssz.uint")).first) {
    return cls to listOf(FAtom("pylib.int"))
  }

  when(cls.n) {
    in TypingContext.classes -> {
      val classDescr = TypingContext.classes[cls.n]!!
      val cd = classDescr.head
      if (cls.ps.size != cd.noTParams)
        fail()
      val tvAssgn = cd.tvars.zip(cls.ps).toMap()
      val attrs = classDescr.attrs
      if (attr in attrs) {
        val f = attrs[attr]!!
        if (f !is TLTCallable) fail()
        val args = f.args
        val res = f.ret
        return res.toFAtom(tvAssgn) to args.map { it.toFAtom(tvAssgn) }
      } else if (classDescr.parent != null)
        return resolveAttributeCall(classDescr.parent.toFAtom(tvAssgn) as FAtom, attr)
    }
    else -> TODO()
  }
  fail()
}

fun resolveConstExprType(ce: ConstExpr): FAtom {
  return calcConstExprType(ce.e, TypingContext.constTypes, emptyMap())
}

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
          toFAtom(parseTypeDecl(e.func.v.toTExpr(), classParseCassInfo))
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
        val v = cs.freshVar("Tif")
        bodyVal?.let { cs.addST(it, v) }
        orelseVal?.let { cs.addST(it, v) }
        v
      } else null
    }
    is Let -> {
      if (e.bindings.map { it.arg!! }.intersect(vars.keys).isNotEmpty()) fail()
      e.bindings.forEach {
        processStmt(Assign(mkName(it.arg!!, true), it.value), vars, cs)
      }
      resolveExprType(e.value)
    }
    is Tuple -> {
      val eltTypes = e.elts.map { resolveExprType(it) }
      if (eltTypes.any { it == null }) null
      else FAtom("Tuple", eltTypes.map { it!! })
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
      cs.addEQ(cs.mkVar("T$vn"), toFAtom(parseTypeDecl(s.annotation, classParseCassInfo)))
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

fun toFTerm(t: TLType): FTerm = when(t) {
  is TLTClass -> toFAtom(t)
  is TLTVar -> FVar(t.name)
  else -> TODO()
}
fun toFAtom(cls: TLTClass): FAtom = FAtom(cls.name, cls.params.filter { it !is TLTConst }.map { toFTerm(it) })
fun inferTypes(fd: FunctionDef): Map<String,ClassVal> {
  val f = convertToAndOutOfSSA(fd)
  pyPrintFunc(f)
  println()

  val args = f.args.args.map { "T" + it.arg to toFAtom(parseTypeDecl(it.annotation!!, classParseCassInfo)) }.toMap()
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
  f.body.forEach { s ->
    tc.processStmt(s, args, ccs)
  }
  tc_solve(constrs, fn)

  var curr: Map<String,FAtom> = args
  var currEqs: List<Pair<String,FAtom>> = emptyList()
  do {
    val constraints = ConstrStore()
    val currVars = curr.plus(currEqs)
    f.body.forEach { s ->
      processStmt(s, currVars, constraints)
    }
    //val (eqs, lbs, ubs) = norm2(constraints.stConstraints, constraints.fn)
    constraints.simplify()
    currEqs = constraints.eqs.eqs.sub.map { it.key.name to constraints.eqs.convertBack(it.value) }.filter { it.second is FAtom }.map { it.first to it.second as FAtom }
    val res = solve(constraints.stConstraints, constraints.fn).mapKeys { it.key.v }
    val res2 = res.mapValues { replaceTypeVars(it.value, res) as FAtom }
//    fun cmp() {
//      val r1 = lbs.plus(eqs).filterKeys { !it.startsWith("L_") }.filterValues { it is FAtom }
//      val r2 = res.plus(currEqs).filterKeys { !it.startsWith("C_") }
//      if (r1 != r2)
//        println()
//    }
//    cmp()
    val prev = curr
    curr = curr.plus(res2)
  } while (prev != curr)

  val res1 = (curr + currEqs).mapValues { replaceTypeVars(it.value, curr) }
  return res1.mapValues { it.value.toClassVal() }
}

fun replaceTypeVars(t: FTerm, vars: Map<String,FAtom>): FTerm = when(t) {
  is FVar -> vars[t.v] ?: t
  is FAtom -> t.copy(ps = t.ps.map { replaceTypeVars(it, vars) })
}
fun FTerm.toClassVal(): ClassVal = when(this) {
  is FVar ->
    fail()
  is FAtom -> this.toClassVal()
}
fun FAtom.toClassVal(): ClassVal {
  return ClassVal(this.n, this.ps.map { it.toClassVal() })
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