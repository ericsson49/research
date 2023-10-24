package onotole.type_inference

import onotole.AnnAssign
import onotole.Assert
import onotole.Assign
import onotole.Attribute
import onotole.Break
import onotole.CTV
import onotole.Call
import onotole.ClassVal
import onotole.ConstExpr
import onotole.ExTypeVar
import onotole.Expr
import onotole.FuncInst
import onotole.FunctionDef
import onotole.GeneratorExp
import onotole.If
import onotole.IfExp
import onotole.Index
import onotole.Lambda
import onotole.Let
import onotole.Name
import onotole.NameConstant
import onotole.Num
import onotole.PyDict
import onotole.PyList
import onotole.Return
import onotole.Slice
import onotole.Starred
import onotole.Stmt
import onotole.Str
import onotole.Subscript
import onotole.TExpr
import onotole.Tuple
import onotole.TypeResolver
import onotole.While
import onotole.asTypeVar
import onotole.fail
import onotole.namesToTExpr
import onotole.typelib.TLTVar
import onotole.typelib.parseTypeDecl
import onotole.util.toFAtom
import onotole.util.toTLTClass

sealed class CConstr
sealed interface SimpleCon
data class ConEQ(val a: FTerm, val b: FTerm): CConstr(), SimpleCon
data class ConST(val a: FTerm, val b: FTerm): CConstr(), SimpleCon
sealed class CallHandle
data class CallOp(val op: String): CallHandle()
data class GetAttr(val tgt: FTerm, val attr: String): CallHandle()
data class SetAttr(val tgt: FTerm, val attr: String): CallHandle()
data class CallAttr(val tgt: FTerm, val attr: String): CallHandle()
data class GetIdx(val tgt: FTerm): CallHandle()
data class GetIdxConst(val tgt: FTerm, val idx: Int): CallHandle()
data class SetIdx(val tgt: FTerm): CallHandle()
data class GetSlice(val tgt: FTerm): CallHandle()
data class SetSlice(val tgt: FTerm): CallHandle()
data class ConCall(val res: FTerm, val call: CallHandle,
                   val args: Collection<FTerm>, val kwds: Collection<Pair<String,FTerm>> = emptyList()): CConstr()

interface CConstrStore {
  fun newVar(n: String): FVar
  fun mkVar(n: String): FVar
  fun addConstr(c: CConstr, delayed: Boolean = false)
  fun addEQ(a: FTerm, b: FTerm, delayed: Boolean = false) = addConstr(ConEQ(a, b), delayed)
  fun addST(a: FTerm, b: FTerm, delayed: Boolean = false) = addConstr(ConST(a, b), delayed)
}
context (TypingCtx)
class TypeChecker() {
  val NONE = FAtom("pylib.None")
  fun resolveExprType(e: TExpr, vars: Map<String, FTerm>, cs: CConstrStore): FTerm {
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
        val v = cs.newVar("A_" + e.attr)
        cs.addConstr(ConCall(v, GetAttr(resolveExprType(e.value), e.attr), emptyList()))
        v
      }
      is Subscript -> {
        val valType = resolveExprType(e.value)
        when(e.slice) {
          is Index -> {
            val v = cs.newVar("I")
            val sliceValue = if (e.slice.value is CTV && e.slice.value.v is ConstExpr)
              e.slice.value.v.e else e.slice.value
            if (sliceValue is Num) {
              val idx = sliceValue.n.toInt()
              cs.addConstr(ConCall(v, GetIdxConst(valType, idx), listOf()))
            } else {
              val idxType = resolveExprType(e.slice.value)
              cs.addConstr(ConCall(v, GetIdx(valType), listOf(idxType)))
            }
            v
          }
          is Slice -> {
            val v = cs.newVar("S")
            val lower = if (e.slice.lower != null) {
              resolveExprType(e.slice.lower)
            } else NONE
            val upper = if (e.slice.upper != null) {
              resolveExprType(e.slice.upper)
            } else NONE
            val step = if (e.slice.step != null) {
              resolveExprType(e.slice.step)
            } else NONE
            cs.addConstr(ConCall(v, GetSlice(valType), listOf(lower, upper, step)))
            v
          }
          else -> fail()
        }
      }
      is Call -> {
        if (e.func is CTV) {
          val argTypes = e.args.map { resolveExprType(it) }
          val kwdTypes = e.keywords.map { it.arg!! to resolveExprType(it.value) }
          val fh = if (e.func.v is ClassVal) {
            toFAtom(parseTypeDecl(e.func.v))
          } else if (e.func.v is FuncInst) {
            val funcParams = e.func.v.sig.args.toMap()
            val argParams = e.func.v.sig.args.subList(0, argTypes.size).map { it.second.toFAtom(emptyMap()) }
            val kwdParams = kwdTypes.map { funcParams[it.first]!!.toFAtom(emptyMap()) }
            argTypes.zip(argParams).forEach {
              cs.addST(it.first, it.second)
            }
            kwdTypes.zip(kwdParams).forEach {
              cs.addST(it.first.second, it.second)
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
          val valType = resolveExprType(e.func.value)
          val res = cs.newVar("C")
          val argTypes = e.args.map { resolveExprType(it) }
          cs.addConstr(ConCall(res, CallAttr(valType, e.func.attr), argTypes))
          res
        } else if (e.func is Name && e.func.id in TypeResolver.specialFuncNames) {
          if (e.keywords.isNotEmpty()) fail()
          val res = cs.newVar("C")
          val argTypes = e.args.map { resolveExprType(it) }
          cs.addConstr(ConCall(res, CallOp(e.func.id), argTypes))
          res
        } else fail()
      }
      is IfExp -> {
        cs.addST(resolveExprType(e.test), FAtom("pylib.bool"), true)
        val v = cs.newVar("Iif")
        cs.addST(resolveExprType(e.body), v)
        cs.addST(resolveExprType(e.orelse), v)
        v
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
        FAtom("pylib.Tuple", eltTypes)
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
        val elemType = cs.newVar("E")
        cs.addConstr(ConST(iterType, FAtom("pylib.Iterable", listOf(elemType))))
        val valueTypes = if (localVars.size == 1) {
          val p = cs.mkVar(((g.targetAnno!! as CTV).v as ExTypeVar).v)
          cs.addST(elemType, p)
          listOf(elemType)
        } else {
          val anno = g.targetAnno
          if (anno == null || anno !is CTV || anno.v !is ClassVal || anno.v.name != "pylib.Tuple") fail()
          val elems = anno.v.tParams.map { cs.mkVar((it as ExTypeVar).v) }   //localVars.mapIndexed { i,_ -> cs.mkVar("GEarg_$i") }
          cs.addST(elemType, FAtom("pylib.Tuple", elems))
          elems
        }
        val newVars = vars.plus(localVars.zip(valueTypes))
        g.ifs.forEach {
          cs.addConstr(ConST(resolveExprType(it, newVars, cs), FAtom("pylib.bool")), true)
        }
        val resType = resolveExprType(e.elt, newVars, cs)
        FAtom("pylib.Sequence", listOf(resType))
      }
      is Lambda -> {
        fun convert(t: TExpr): FTerm {
          val v = ((t as CTV).v as ExTypeVar).v
          return vars[v] ?: FVar(v)
        }
        val argTypes = e.args.args.map { convert(it.annotation!!) }
        val retType = convert(e.returns!!)

        val localVars = e.args.args.map { "T${it.arg}" }
        val newVars = vars.plus(localVars.zip(argTypes))
        val resType = resolveExprType(e.body, newVars, cs)
        cs.addEQ(retType, resType)
        FAtom("pylib.Callable", argTypes.plus(retType))
      }
      is Starred -> resolveExprType(e.value)
      is PyList -> {
        val vv = FVar(e.valueAnno!!.asTypeVar().v)
        e.elts.forEach {
          cs.addConstr(ConST(resolveExprType(it), vv))
        }
        FAtom("pylib.PyList", listOf(vv))
      }
      is PyDict -> {
        val kv = FVar(e.keyAnno!!.asTypeVar().v)
        val vv = FVar(e.valueAnno!!.asTypeVar().v)
        e.keys.forEach {
          cs.addConstr(ConST(resolveExprType(it), kv))
        }
        e.values.forEach {
          cs.addConstr(ConST(resolveExprType(it), vv))
        }
        FAtom("pylib.Dict", listOf(kv, vv))
      }
      else -> TODO()
    }
  }

  fun processFunc(f: FunctionDef, cs: CConstrStore) {
    fun toFAtom(t: TExpr): FAtom = when((t as CTV).v) {
      is ClassVal -> toFAtom((t.v as ClassVal).toTLTClass())
      else -> TODO()
    }

    val args = f.args.args.map { "T" + it.arg to toFAtom(parseTypeDecl(it.annotation!! as CTV)) }.toMap()
    args.forEach { (v, t) ->
      cs.addEQ(cs.mkVar(v), t)
    }
    f.body.forEach { s ->
      processStmt(s, args, cs)
    }
    cs.addST(cs.mkVar("Treturn"), toFAtom(f.returns!!))
  }

  fun processStmt(s: Stmt, vars: Map<String, FTerm>, cs: CConstrStore) {
    when(s) {
      is Expr -> resolveExprType(s.value, vars, cs)
      is Assert -> {
        val testType = resolveExprType(s.test, vars, cs)
        cs.addST(testType, FAtom("pylib.bool"), true)
        if (s.msg != null) {
          val msgType = resolveExprType(s.test, vars, cs)
          cs.addST(msgType, FAtom("pylib.str"), true)
        }
      }
      is Assign -> {
        val valType = resolveExprType(s.value, vars, cs)
        when(s.target) {
          is Name -> {
            val vn = s.target.id
            cs.addST(valType, cs.mkVar("T$vn"))
          }
          is Subscript -> {
            val subscrValType = resolveExprType(s.target.value, vars, cs)
            when(s.target.slice) {
              is Index -> {
                val idxType = resolveExprType(s.target.slice.value, vars, cs)
                cs.addConstr(ConCall(NONE, SetIdx(subscrValType), listOf(idxType, valType)), true)
              }
              is Slice -> {
                val lower = if (s.target.slice.lower != null) {
                  resolveExprType(s.target.slice.lower, vars, cs)
                } else NONE
                val upper = if (s.target.slice.upper != null) {
                  resolveExprType(s.target.slice.upper, vars, cs)
                } else NONE
                val step = if (s.target.slice.step != null) {
                  resolveExprType(s.target.slice.step, vars, cs)
                } else NONE
                cs.addConstr(ConCall(NONE, SetSlice(subscrValType), listOf(lower, upper, step, valType)), true)
              }
              else -> TODO()
            }
          }
          is Attribute -> {
            val tgtValType = resolveExprType(s.target.value, vars, cs)
            cs.addConstr(ConCall(NONE, SetAttr(tgtValType, s.target.attr), listOf(valType)), true)
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
          cs.addST(valType, cs.mkVar("T$vn"))
        }
      }
      is If -> {
        val testType = resolveExprType(s.test, vars, cs)
        cs.addST(testType, FAtom("pylib.bool"), true)
        s.body.forEach { processStmt(it, vars, cs) }
        s.orelse.forEach { processStmt(it, vars, cs) }
      }
      is While -> {
        val testType = resolveExprType(s.test, vars, cs)
        cs.addST(testType, FAtom("pylib.bool"), true)
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
}

fun convertOptionalType(t: FTerm): FTerm = when(t) {
  is FVar -> t
  is FAtom -> if (t.n == "pylib.Optional") {
    if (t.ps.size != 1) fail()
    convertOptionalType(t.ps[0])
  } else t.copy(ps = t.ps.map(::convertOptionalType))
}
fun convertOptionalType(c: CConstr): CConstr = convert(c, ::convertOptionalType)
fun convert(ch: CallHandle, f: (FTerm) -> FTerm): CallHandle = when(ch) {
  is CallOp -> ch
  is GetAttr -> ch.copy(tgt = f(ch.tgt))
  is SetAttr -> ch.copy(tgt = f(ch.tgt))
  is CallAttr -> ch.copy(tgt = f(ch.tgt))
  is GetIdxConst -> ch.copy(tgt = f(ch.tgt))
  is GetIdx -> ch.copy(tgt = f(ch.tgt))
  is SetIdx -> ch.copy(tgt = f(ch.tgt))
  is GetSlice -> ch.copy(tgt = f(ch.tgt))
  is SetSlice -> ch.copy(tgt = f(ch.tgt))
}

fun convert(c: CConstr, f: (FTerm) -> FTerm): CConstr = when(c) {
  is ConEQ -> c.copy(a = f(c.a), b = f(c.b))
  is ConST -> c.copy(a = f(c.a), b = f(c.b))
  is ConCall -> c.copy(res = f(c.res),
      call = convert(c.call, f),
      args = c.args.map(f),
      kwds = c.kwds.map { it.copy(second = f(it.second)) }
  )
}
context (TypingCtx)
fun tc_check_delayed(cs: CoStore, delayedConstraints: Collection<CConstr>) {
  delayedConstraints.forEach { c ->
    when (c) {
      is ConEQ -> cs.addAll(listOf(c))
      is ConST -> cs.addAll(listOf(c))
      is ConCall -> {
        tc_solve(cs, listOf(c))
      }
    }
  }
}
context (TypingCtx)
fun tc_solve(cs: Collection<CConstr>): CoStore {
  val cs = cs.map(::convertOptionalType)
  val coStore = CoStore()
  coStore.addAll(cs.filterIsInstance<ConEQ>())
  coStore.addAll(cs.filterIsInstance<ConST>())
  val calls2 = cs.filterIsInstance<ConCall>()
  tc_solve(coStore, calls2)
  return coStore
}
context (TypingCtx)
fun tc_solve(store: CoStore, calls: List<ConCall>) {
  var currCalls = calls.toSet()
  do {
    store.checkSat()
    val calls2 = currCalls.map { convert(it) { t -> applySubst(t, store.eqs) } as ConCall }
    val vars = store.lbs.filterValues { it.size == 1 }.mapValues { it.value.first() }
    val (derived, inactive) = checkCallConstraints(calls2, vars)
    val derived2 = derived.map(::convertOptionalType)
    store.addAll(derived2.filterIsInstance<ConEQ>())
    store.addAll(derived2.filterIsInstance<ConST>())
    currCalls = inactive.plus(derived2.filterIsInstance<ConCall>()).toSet()
  } while (derived.isNotEmpty())
  if (currCalls.isNotEmpty())
    fail()
}
context (TypingCtx)
fun checkCallConstraints(calls: Collection<ConCall>, vars: Map<FVar,FTerm>): Pair<Collection<CConstr>,Collection<ConCall>> {
  val derived = mutableListOf<CConstr>()
  val inactive = mutableListOf<ConCall>()
  calls.forEach { c ->
    val guards = c.args.plus(c.kwds.map { it.second })
    fun remap(t: FTerm): FTerm = when(t) {
      is FAtom -> t
      is FVar -> vars[t] ?: t
    }
    fun remap(ch: CallHandle): CallHandle = when(ch) {
      is CallOp -> ch
      is GetAttr -> ch.copy(tgt = remap(ch.tgt))
      is SetAttr -> ch.copy(tgt = remap(ch.tgt))
      is CallAttr -> ch.copy(tgt = remap(ch.tgt))
      is GetIdxConst -> ch.copy(tgt = remap(ch.tgt))
      is GetIdx -> ch.copy(tgt = remap(ch.tgt))
      is SetIdx -> ch.copy(tgt = remap(ch.tgt))
      is GetSlice -> ch.copy(tgt = remap(ch.tgt))
      is SetSlice -> ch.copy(tgt = remap(ch.tgt))
    }
    fun isGround(t: FTerm): Boolean = remap(t) is FAtom
    fun isGround(ch: CallHandle): Boolean = when(ch) {
      is CallOp -> true
      is GetAttr -> isGround(ch.tgt)
      is SetAttr -> isGround(ch.tgt)
      is CallAttr -> isGround(ch.tgt)
      is GetIdxConst -> isGround(ch.tgt)
      is GetIdx -> isGround(ch.tgt)
      is SetIdx -> isGround(ch.tgt)
      is GetSlice -> isGround(ch.tgt)
      is SetSlice -> isGround(ch.tgt)
    }
    if (isGround(c.call) && guards.all(::isGround)) {
      val args = c.args.map { remap(it) as FAtom }
      val kwds = c.kwds.map { it.first to (remap(it.second) as FAtom) }
      derived.addAll(applyCallConstraint(c.res, remap(c.call), args, kwds))
    } else
      inactive.add(c)
  }
  return derived to inactive
}
context (TypingCtx)
fun applyCallConstraint(rr: FTerm, c: CallHandle, args: List<FAtom>, kwds: List<Pair<String,FAtom>>): Collection<CConstr> {
  val res = mutableSetOf<CConstr>()
  val r = when(c) {
    is CallOp -> {
      if (kwds.isNotEmpty()) fail()
      val op = c.op.substring(1, c.op.length-1)
      when(op) {
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
    is CallAttr -> {
      val valType = c.tgt
      val (retType, paramTypes) = resolveAttributeCall(valType as FAtom, c.attr)
      if (args.size != paramTypes.size)
        fail()
      args.zip(paramTypes).forEach {
        res.add(ConST(it.first, it.second))
      }
      retType
    }
    is GetAttr -> resolveAttributeGet(c.tgt as FAtom, c.attr)
    is SetAttr -> {
      val valType = resolveAttributeGet(c.tgt as FAtom, c.attr)
      res.addAll(tryConvert(args[0], valType))
      FAtom("pylib.None")
    }
    is GetIdxConst -> {
      val tgt = c.tgt as FAtom
      if (tgt.n == "pylib.Tuple")
        tgt.ps[c.idx]
      else
        resolveIndexGet(tgt, FAtom("pylib.int"))
    }
    is GetIdx -> resolveIndexGet(c.tgt as FAtom, args[0])
    is SetIdx -> {
      val valType = resolveIndexGet(c.tgt as FAtom, args[0])
      res.addAll(tryConvert(args[1], valType))
      FAtom("pylib.None")
    }
    is GetSlice -> resolveSliceGet(c.tgt as FAtom, null, null, null)
    is SetSlice -> {
      val valType = resolveSliceGet(c.tgt as FAtom, null, null, null)
      res.addAll(tryConvert(args[3], valType))
      FAtom("pylib.None")
    }
  }
  res.add(ConST(r, rr))
  return res
}

context (TypingCtx)
fun tryConvert(a: FTerm, b: FTerm): List<CConstr> {
  fun checkST(a: FAtom, b: FAtom) = tryCheckST(a, b) == true
  return when {
    a is FAtom && b is FAtom -> {
      when {
        a.n == "pylib.int" && b.n == "pylib.bool" -> emptyList()
        a.n == "pylib.int" && checkST(b, FAtom("ssz.uint")) -> emptyList()
        a.n == "pylib.bool" && checkST(b, FAtom("ssz.uint")) -> emptyList()
        a.n == "pylib.PyList" && b.n == "ssz.List" -> listOf(ConST(a.ps[0], b.ps[0]))
        else -> null
      }
    }
    else -> null
  } ?: listOf(ConST(a, b))
}
