package onotole

import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Term
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.dsl.prolog
import it.unibo.tuprolog.theory.Theory
import java.nio.file.Files
import java.nio.file.Paths

sealed class PTerm
data class PAtom(val a: String): PTerm() {
  override fun toString() = a
}
data class PVar(val v: String): PTerm() {
  override fun toString() = v
}
data class PStruct(val f: String, val ps: List<PTerm>): PTerm() {
  override fun toString() = "$f(" + ps.joinToString(",") + ")"
}
data class PList(val elts: List<PTerm>): PTerm() {
  override fun toString() = elts.joinToString(",", prefix = "[", postfix = "]")
}

sealed class Constr
data class EqConstr(val a: PTerm, val b: PTerm): Constr() {
  override fun toString() = "$a = $b"
}
data class STConstr(val a: PTerm, val b: PTerm): Constr() {
  override fun toString() = "$a <: $b"
}
data class PredConstr(val f: String, val args: List<PTerm>, val res: PTerm): Constr() {
  override fun toString() = "$res = \$$f(" + args.joinToString(",") + ")"
}

class ConstraintStore {
  val history = mutableListOf<Constr>()
  val cs = CStore()

  fun add(c: Constr) {
    history.add(c)
    cs.add(c)
  }

  fun getValue(v: PVar): PTerm = convert(cs.getValue(cs.convert(v) as Var))

  fun bindToLeastBound(v: PVar) {
    val pv = cs.convert(v)
    val r = cs.leastSolution(pv as Var)
    if (r.isVariable) fail()
    history.add(EqConstr(v, convert(r)))
    cs.unif(pv, r)
  }

  fun convert(t: Term): PTerm = when(t) {
    is Var -> PVar(t.name)
    is Atom -> PAtom(t.value)
    is Struct -> PStruct(t.functor, t.argsList.map(::convert))
    is it.unibo.tuprolog.core.List -> PList(t.toList().map(::convert))
    else -> TODO()
  }
}

class TypeConstraintGen {
  val constraints = ConstraintStore()
  val tmpVars = mutableListOf<String>()
  fun newPVar(prefix: String = "T_tmp"): PVar {
    val n = prefix + tmpVars.size
    tmpVars.add(n)
    return PVar(n)
  }
  fun genTVar() = newPVar("TV_")
  fun genPVar(v: String) = PVar("T_$v")
  fun tunif(a: PTerm, b: PTerm) = EqConstr(a, b)
  fun subType(a: PTerm, b: PTerm) = STConstr(a, b)

  fun genPPRed(p: String, args: List<PTerm>, res: PVar): PVar {
    constraints.add(PredConstr(p, args, res))
    return res
  }
  fun genPPRed(p: String, args: List<PTerm>) = genPPRed(p, args, newPVar("R"))
  fun genTOp(op: String, a: PTerm, b: PTerm) = genPPRed("op_type", listOf(PAtom(op), a, b))
  fun genTOp(op: EBinOp, a: PTerm, b: PTerm) = genTOp(op.toString().toLowerCase(), a, b)
  fun genTOp(op: EBoolOp, a: PTerm, b: PTerm) = genTOp(op.toString().toLowerCase(), a, b)
  fun genTOp(op: ECmpOp, a: PTerm, b: PTerm) = genTOp(op.toString().toLowerCase(), a, b)

  fun genProlog(e: TExpr): PTerm {
    return when(e) {
      is NameConstant -> when (e.value) {
        null -> PAtom("pynone")
        is Boolean -> PAtom("pybool")
        else -> fail("not implemented $e")
      }
      is Num -> PAtom("pyint")
      is Str -> PAtom("pystr")
      is Name -> genPVar(e.id)
      is BinOp -> {
        val lr = genProlog(e.left)
        val rr = genProlog(e.right)
        genTOp(e.op,lr,rr)
      }
      is Compare -> {
        val lr = genProlog(e.left)
        val rrs = e.comparators.map { genProlog(it) }
        listOf(lr).plus(rrs.subList(0, rrs.size - 1)).zip(e.ops).zip(rrs).forEach {
          val r = genTOp(it.first.second, it.first.first, it.second)
          constraints.add(tunif(r, PAtom("pybool")))
        }
        PAtom("pybool")
      }
      is Call -> {
        if (e.keywords.isNotEmpty()) fail()
        val (fh, preArgs) = when(e.func) {
          is Name -> PAtom(e.func.id) to emptyList<PTerm>()
          is Attribute -> {
            val self = genProlog(e.func.value)
            genPPRed("attr_fh", listOf(self, PAtom(e.func.attr))) to listOf(self)
          }
          else -> TODO()
        }
        val args = e.args.map { genProlog(it) }
        val fArgs = e.args.indices.map { newPVar("P") }
        args.zip(fArgs).forEach { (a, b) ->
          constraints.add(STConstr(a, b))
        }
        genPPRed("fun_type", listOf(fh).plus(PList(preArgs.plus(fArgs))))
      }
      is Subscript -> {
        val slice = when(e.slice) {
          is Index -> genProlog(e.slice.value)
          else -> fail("not implemented ${e.slice}")
        }
        val coll = newPVar("P")
        constraints.add(subType(genProlog(e.value), coll))
        genPPRed("fun_type", listOf(PAtom("elem_of")).plus(PList(listOf(coll))))
      }
      is Tuple -> {
        PStruct("pytuple", e.elts.map { genProlog(it) })
      }
      is PyList -> {
        if (e.elts.isNotEmpty()) TODO()
        PStruct("pylist", listOf(genTVar()))
      }
      is Lambda -> {
        PStruct("pycallable", listOf(PList(e.args.args.map { genTVar() }), genTVar()))
      }
      is Attribute -> {
        // load ctx
        genPPRed("attr_type", listOf(genProlog(e.value), PAtom(e.attr)))
      }
      is GeneratorExp -> {
        if (e.generators.size != 1) fail()
        if (e.generators[0].ifs.size != 0) TODO()
        val gen = e.generators[0]

        fun toArgs(t: TExpr): List<Arg> = when(t) {
          is Name -> listOf(Arg(arg = t.id))
          is Tuple -> t.elts.flatMap { toArgs(it as Name) }
          else -> fail("unsupported $t")
        }

        val mapLam = Lambda(
                args = Arguments(args = toArgs(gen.target)),
                body = e.elt
        )
        val res = Call(
                func = Name(id = "map", ctx = ExprContext.Load),
                args = listOf(mapLam, gen.iter), keywords = emptyList())
        genProlog(res)
      }
      else -> fail("not implemented $e")
    }
  }

  fun genLVal(e: TExpr): PTerm = when(e) {
    is Name -> genPVar(e.id)
    is Tuple -> {
      val elts = e.elts.map { genLVal(it) }
      PStruct("pytuple", elts)
    }
    else -> fail("not implemented $e")
  }

  val varsDefined = mutableSetOf<PVar>()

  fun genProlog(s: Stmt) {
    when(s) {
      is Assign -> {
        fun getVars(l: PTerm): Set<PVar> = when {
          l is PVar -> setOf(l)
          l is PStruct && l.f == "pytuple" -> l.ps.flatMap(::getVars).toSet()
          else -> TODO()
        }

        val l = genLVal(s.target)
        val r = genProlog(s.value)
        constraints.add(subType(r, l))
        val vars = getVars(l)
        vars.minus(varsDefined).forEach {
          constraints.bindToLeastBound(it)
        }
        varsDefined.addAll(vars)
      }
      is If -> {
        val r = genProlog(s.test)
        constraints.add(tunif(PAtom("pybool"), r))
        s.body.forEach { genProlog(it) }
        s.orelse.forEach { genProlog(it) }
      }
      is While -> {
        val r = genProlog(s.test)
        constraints.add(tunif(PAtom("pybool"), r))
        s.body.forEach { genProlog(it) }
      }
      is For -> {
        val v = genPVar((s.target as Name).id)
        val r = genProlog(s.iter)
        val coll = newPVar("PP")
        constraints.add(subType(r, coll))
        genPPRed("fun_type", listOf(PAtom("elem_of")).plus(PList(listOf(coll))), v)
        constraints.bindToLeastBound(v)
        s.body.forEach { genProlog(it) }
      }
      is Return -> if (s.value != null) {
        val r = genProlog(s.value)
        constraints.add(subType(r, PVar("Tret")))
      }
      is Expr -> {
        genProlog(s.value)
      }
      else -> fail("not implemented $s")
    }
  }

  fun genPrologForType(t: TExpr): PTerm = when(t) {
    is NameConstant -> when(t.value) {
      null -> PAtom("pynone")
      else -> TODO()
    }
    is Name -> when(t.id) {
      "int", "bool", "str", "bytes" -> PAtom("py" + t.id)
      else -> PAtom(t.id)
    }
    is Subscript -> when {
      t.value is Name && t.value.id == "PyList" && t.slice is Index -> {
        PStruct("pylist", listOf(genPrologForType(t.slice.value)))
      }
      t.value is Name && t.value.id == "Dict" && t.slice is Index && t.slice.value is Tuple -> {
        PStruct("pydict", listOf(
                genPrologForType(t.slice.value.elts[0]),
                genPrologForType(t.slice.value.elts[1]),
        ))
      }
      t.value is Name && t.value.id == "Sequence" && t.slice is Index -> {
        PStruct("pysequence", listOf(genPrologForType(t.slice.value)))
      }
      t.value is Name && t.value.id == "List" && t.slice is Index && t.slice.value is Tuple -> {
        PStruct("SSZList", listOf(genPrologForType(t.slice.value.elts[0])))
      }
      t.value is Name && t.value.id == "Vector" && t.slice is Index && t.slice.value is Tuple -> {
        PStruct("SSZVector", listOf(genPrologForType(t.slice.value.elts[0])))
      }
      t.value is Name && t.value.id == "Bitlist" && t.slice is Index -> {
        PStruct("SSZBitlist", listOf(genPrologForType(t.slice.value)))
      }
      t.value is Name && t.value.id == "Bitvector" && t.slice is Index -> {
        PStruct("SSZBitvector", listOf(genPrologForType(t.slice.value)))
      }
      else -> fail("not implemented $t")
    }
    else -> fail("not implemented $t")
  }
  fun genProlog(a: Arg) {
    constraints.add(tunif(genPVar(a.arg), genPrologForType(a.annotation!!)))
  }
  fun genPrologReturn(t: TExpr?) {
    constraints.add(tunif(PVar("Tret"), genPrologForType(t!!)))
  }
}





fun main() {
  val ignoredFuncs = setOf("cache_this", "hash", "ceillog2")
  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_phase0_dev.txt")
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  val defs = parsed.map { toStmt(it) }

  defs.forEach {
    if (it is ClassDef) {
      val pg = TypeConstraintGen()
      if (it.bases.size != 1) fail()
      val cls = "py" + it.name
      val base = TuPrologSolver.convert(pg.genPrologForType(it.bases[0]))

      val fields = it.body.filterIsInstance<AnnAssign>().map { f ->
        val attrName = (f.target as Name).id
        val attrType = TuPrologSolver.convert(pg.genPrologForType(f.annotation))
        prolog { fact { "class_attr"(cls, attrName, attrType) } }
      }

      TuPrologSolver.solver.appendDynamicKb(Theory.of(
              prolog {
                ktListOf(
                        fact { "base_class"(cls, base) },
                        fact { "fun_type"(it.name, list(), cls) }
                )
              }.plus(fields)
      ))
      println()
    }
    if (it is FunctionDef && it.name !in ignoredFuncs) {
      precessFunctionDef(it)
    }
  }
}

private fun precessFunctionDef(it: FunctionDef) {
  val f = convertToAndOutOfSSA(desugarExprs(it))
  val pg = TypeConstraintGen()
  println("func " + f.name)
  f.args.args.forEach {
    pg.genProlog(it)
  }
  pg.genPrologReturn(f.returns)
  f.body.forEach {
    pg.genProlog(it)
  }
  /*pg.acc.forEach {
      println("  $it")
    }*/
  println(pg.constraints.history.joinToString("\n"))
  println()

  val cs = CStore()
  pg.constraints.history.forEach {
    cs.add(it)
  }
  println(cs.sts.constraints)
  println(cs.sts.pendingConstraints)
  println(cs.preds)
  cs.sts.flush()
  println("----")
  println(cs.sts.constraints.minus(cs.sts.bstAlreadyChecked))
  println(cs.sts.pendingConstraints)
  println(cs.preds)

  println()
}