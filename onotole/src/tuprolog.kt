package onotole

import it.unibo.tuprolog.core.*
import it.unibo.tuprolog.core.List
import it.unibo.tuprolog.dsl.prolog
import it.unibo.tuprolog.solve.MutableSolver
import it.unibo.tuprolog.solve.Solution
import it.unibo.tuprolog.solve.classic.classicWithDefaultBuiltins
import it.unibo.tuprolog.theory.Theory
import it.unibo.tuprolog.unify.Unificator
import kotlin.collections.List as KList

class TPVars() {
  val vars = mutableMapOf<String,Var>()
  fun getVar(v: String) = vars.getOrPut(v) { Var.of(v) }
}

object TuPrologSolver {
  val baseTheory = prolog {
    ktListOf(
            fact { "swappable_op"("add", atomOf("__add__"), atomOf("__radd__")) },
            fact { "swappable_op"("floordiv", atomOf("__floordiv__"), atomOf("__rfloordiv__")) },

            fact { "swappable_op"("lt", atomOf("__lt__"), atomOf("__gte__")) },
            fact { "swappable_op"("lte", atomOf("__lte__"), atomOf("__gt__")) },
            fact { "swappable_op"("gt", atomOf("__gt__"), atomOf("__lte__")) },
            fact { "swappable_op"("gte", atomOf("__gte__"), atomOf("__lt__")) },

            rule { "op_type"("noteq", X, Y, Z) `if` "op_type"("eq", X, Y, Z) },
            fact { "op_type"("eq", X, X, "pybool") },
            fact { "op_type"("is", X, Y, "pybool") },
            rule { "op_type"("lt", X, X, "pybool") `if` (X `==` "pyint") },
            rule { "op_type"("gt", X, X, "pybool") `if` (X `==` "pyint") },

            rule { "op_type"("mult", X, X, X) `if`  (X `=` "pyint") },
            rule { "op_type"("floordiv", X, X, X) `if`  (X `=` "pyint") },
            rule { "op_type"("mod", X, X, X) `if`  (X `=` "pyint") },

            fact { "fun_type"("map", list("pycallable"(list(X),Y), "pysequence"(X)), "pysequence"(Y)) },
            fact { "fun_type"("len", list("pylist"(`_`)), "pyint") },
            fact { "fun_type"("range", list("pyint"), "pysequence"("pyint")) },
            fact { "fun_type"("zip", list("pysequence"(X),"pysequence"(Y)), "pysequence"("pytuple"(X,Y)))},


            fact { "fun_type"("elem_of", list("pysequence"(X)), X) },

            rule { "attr_type"(C, A, T) `if` "class_attr"(C, A, T) },
            rule { "attr_type"(C, A, T) `if` ("super_class"(C, S) and "class_attr"(S, A, T)) },

            rule { "attr_ftype"(C, A, P, T) `if` "class_fattr"(C,A,P,T) },
            rule { "attr_ftype"(C, A, P, T) `if` ("super_class"(C, S) and "class_fattr"(S, A, P, T)) },

            rule { "attr_fh"(C, A, H) `if` "class_method"(C, A, H) },
            rule { "attr_fh"(C, A, H) `if` ("super_class"(C, S) and "class_method"(S, A, H)) },

            rule { "super_class"(A, B) `if` "base_class"(A,B)},
            rule { "super_class"(A, B) `if` ("base_class"(A,C) and "super_class"(C,B)) },
    )
  }
  val pylibThrory = prolog {
    ktListOf(
            fact { "type_vars"("pysequence"(X), list(X), list(), list()) },
            fact { "type_vars"("pycallable"(A, R), list(R), list(), A) },
            rule { "type_vars"(T, R, list(), list()) `if` (T univ consOf("pytuple", R)) },

            fact { "fun_type"("pylist::append", list("pylist"(X), X), "void") },

            fact { "type_vars"("pylist"(X), list(), list(X), list()) },
            fact { "base_class"("pylist"(X), "pysequence"(X)) },
            fact { "class_method"("pylist"(X), "append", "pylist::append") },

            fact { "class_fattr"("pyint", atomOf("__add__"), list("pyint"), "pyint") },
            fact { "class_fattr"("pyint", atomOf("__floordiv__"), list("pyint"), "pyint") },

            fact { "class_fattr"("pyint", atomOf("__lt__"), list("pyint"), "pybool") },
            fact { "class_fattr"("pyint", atomOf("__gt__"), list("pyint"), "pybool") },
            fact { "class_fattr"("pyint", atomOf("__lte__"), list("pyint"), "pybool") },
            fact { "class_fattr"("pyint", atomOf("__gte__"), list("pyint"), "pybool") },
    )
  }

  val currTheory = prolog {
    ktListOf(
            fact { "fun_type"("pyContainer::copy", list(X), X) },
            fact { "class_method"("pyContainer", "copy", "pyContainer::copy")},

            fact { "base_class"("uint64", "pyint") },
            fact { "class_fattr"("uint64", atomOf("__add__"), list("pyint"), "uint64") },
            fact { "class_fattr"("uint64", atomOf("__floordiv__"), list("pyint"), "uint64") },

            fact { "base_class"(atomOf("Bytes32"), "pysequence"("pyint")) },
            fact { "fun_type"(atomOf("Bytes32"), list(), atomOf("Bytes32")) },
            fact { "fun_type"(atomOf("Bytes32"), list("pysequence"("pyint")), atomOf("Bytes32")) },
    )
  }


  val solver =
    MutableSolver.classicWithDefaultBuiltins(
            staticKb = Theory.of(baseTheory.plus(pylibThrory)),
            dynamicKb = Theory.Companion.of(currTheory)
    )

  fun solve(s: Struct): Solution {
    val s = solveNF(s)
    if (s is Solution.Yes)
      return s
    else
      fail("failed $s")
  }

  fun solveNF(s: Struct): Solution {
    return solver.solve(s).first()
  }

  fun solveAndCorrectVars(s: Struct): Substitution {
    val sol = solve(s)
    val origSubst = sol.substitution
    return if (sol.isYes) {
      val (newVars, rest) = origSubst.toList().partition { it.second is Var }
      if (newVars.isEmpty()) {
        origSubst
      } else {
        val flipped = newVars.map { it.second as Var to it.first }
        Substitution.of(Substitution.of(rest), Substitution.of(flipped))
      }
    } else {
      origSubst
    }
  }

  fun convert(t: PTerm): Term = when(t) {
    is PAtom -> Atom.of(t.a)
    is PStruct -> Struct.of(t.f, t.ps.map { convert(it) })
    is PList -> List.from(t.elts.map { convert(it) })
    else -> TODO()
  }

}

open class CStoreBase {
  val vars = TPVars()
  fun convert(t: PTerm): Term = when(t) {
    is PAtom -> Atom.of(t.a)
    is PVar -> vars.getVar(t.v)
    is PStruct -> Struct.of(t.f, t.ps.map { convert(it) })
    is PList -> List.from(t.elts.map { convert(it) })
    else -> TODO()
  }

}

class EQCStore {
  var sub: Substitution = Substitution.empty()
  var version: Int = 0

  fun unif(a: Term, b: Term) {
    //println("unif $a with $b")
    val a = sub.applyTo(a)
    val b = sub.applyTo(b)
    val s = Unificator.strict().mgu(a, b)
    if (s.isFailed)
      fail()
    applySubst(s)
  }

  fun applySubst(s: Substitution) {
    if (s.isNotEmpty()) {
      val s2 = Substitution.of(sub, s)
      if (s2.isFailed) fail()
      version += 1
      sub = s2
    }
  }
}

class STCStore(val eqs: EQCStore) {
  val sub: Substitution get() = eqs.sub
  fun unif(a: Term, b: Term) = eqs.unif(a,b)

  var latestEqsVersion = eqs.version
  val allSubstsApplied get() = latestEqsVersion == eqs.version
  var constraints = mutableSetOf<Pair<Term,Term>>()
  val pendingConstraints = mutableSetOf<Pair<Term,Term>>()

  var version: Int = 0

  // - input events processed:
  // -- all substs applied
  // -- queue is empty
  // - store consistency
  // -- reflexivity + symmetry
  // -- idempotence automatic (set of constraints)
  // -- tranitivity applied (ensured when stsQueue is empty)
  // -- reductions applied

  fun addSTConstr(a: Term, b: Term) {
    pendingConstraints.add(a to b)
  }

  private fun checkReflexivity() {
    val refl = constraints.filter { (a,b) -> a == b }
    constraints.removeAll(refl)
  }
  private fun checkSymmetry() {
    val toUnif = constraints.filter { (a,b) -> b to a in constraints }
    toUnif.forEach { (a,b) -> unif(a,b) }
  }
  private fun shrinkStore() {
    while (!allSubstsApplied) {
      val subst = constraints.map { (a, b) -> sub.applyTo(a) to sub.applyTo(b) }
      constraints.clear()
      constraints.addAll(subst)
      latestEqsVersion = eqs.version
      // try shrink store
      checkReflexivity()
      checkSymmetry()
      // which may result in new substitutions
      version++
    }
    // properties:
    // all substs are applied
    // reflexivity checks applied
    // symmetry checks applied
    // but queue can contain pending stuff
    // but there can be opportunities for reductions
  }

  fun flushPendingConstraints(): Boolean {
    val currVersion = version
    shrinkStore()
    while (pendingConstraints.isNotEmpty()) {
      val unifs = mutableSetOf<Pair<Term, Term>>()
      val scan = mutableListOf<Pair<Term, Term>>()
      for ((a, b) in pendingConstraints) {
        val a = sub.applyTo(a)
        val b = sub.applyTo(b)

        if (a == b || a to b in constraints) {
          continue
        }
        if (b to a in constraints) {
          unifs.add(a to b)
        } else {
          scan.add(a to b)
        }
      }
      pendingConstraints.clear()
      if (scan.isNotEmpty()) {
        constraints.addAll(scan)
        version++
        checkSymmetry()
      }
      unifs.forEach { (a, b) -> unif(a, b) }
      shrinkStore()

      val toAdd = mutableSetOf<Pair<Term, Term>>()
      for (p in scan) {
        val a = sub.applyTo(p.first)
        val b = sub.applyTo(p.second)
        constraints.forEach { (a2, b2) ->
          if (b == a2 && a != b2) {
            toAdd.add(a to b2)
          }
          if (b2 == a && a2 != b) {
            toAdd.add(a2 to b)
          }
        }
      }
      pendingConstraints.addAll(toAdd)
      shrinkStore()
    }
    // properties:
    // all substs are applied
    // reflexivity checks applied
    // symmetry checks applied
    // queue is empty
    // but there can be opportunities for reductions, which are handled on another level currently
    return currVersion != version
  }

  /*fun getArgVarianceIndices(f: String, arity: Int): Triple<KList<Int>,KList<Int>,KList<Int>> = when(f) {
    "pysequence" -> Triple(listOf(0), emptyList(), emptyList())
    "pylist" -> Triple(emptyList(), listOf(0), emptyList())
    "pytuple" -> Triple((0 until arity).toList(), emptyList(), emptyList())
    "pycallable" -> Triple(listOf(arity-1), emptyList(), (0 until (arity-1)).toList())
    else -> TODO("getArgVariances(\"$f\")")
  }*/

  fun getArgVariances(s: Struct): Triple<List,List,List> {
    val coVars = Var.of("Co")
    val inVars = Var.of("In")
    val contraVars = Var.of("Contra")
    val sub = TuPrologSolver.solveAndCorrectVars(Struct.of("type_vars", s, coVars, inVars, contraVars))
    if (sub.isFailed) fail()
    return Triple(coVars.apply(sub) as List, inVars.apply(sub) as List, contraVars.apply(sub) as List)
  }

  private val boundSTCache = mutableSetOf<Pair<Term,Term>>()
  val bstCacheVersion = eqs.version
  val bstAlreadyChecked: Set<Pair<Term,Term>> get() {
    if (bstCacheVersion < eqs.version) {
      val n = boundSTCache.map { it.first.apply(eqs.sub) to it.second.apply(eqs.sub) }
      boundSTCache.clear()
      boundSTCache.addAll(n)
    }
    return boundSTCache
  }

  fun flush() {
    flushPendingConstraints()
    while (true) {
      val toRemove = mutableSetOf<Pair<Term,Term>>()
      for ((a, b) in constraints.minus(bstAlreadyChecked)) {
        if (a is Struct && b is Struct) {
          when {
            a.functor == b.functor -> {
              // same class, apply variance subtyping rules
              val (aCo, aIn, aCon) = getArgVariances(a)
              val (bCo, bIn, bCon) = getArgVariances(b)
              aCo.toList().zip(bCo.toList()).forEach { (a,b) -> addSTConstr(a, b) }
              aIn.toList().zip(bIn.toList()).forEach { (a,b) -> unif(a, b) }
              aCon.toList().zip(bCon.toList()).forEach { (a,b) -> addSTConstr(b, a) }
              toRemove.add(a to b)

              /*fun flattenArgs(args: KList<Term>): KList<Term> = args.flatMap {
                when (it) {
                  is List -> it.toList()
                  else -> listOf(it)
                }
              }

              val aArgs = flattenArgs(a.argsList)
              val bArgs = flattenArgs(b.argsList)
              val aArity = aArgs.size
              val bArity = bArgs.size
              if (aArity != bArity) fail()
              val (cos, ins, contras) = getArgVarianceIndices(a.functor, aArity)
              if (cos.plus(ins).plus(contras).sorted() != (0 until aArity).sorted()) fail()
              cos.forEach { addSTConstr(aArgs[it], bArgs[it]) }
              ins.forEach { unif(aArgs[it], bArgs[it]) }
              contras.forEach { addSTConstr(bArgs[it], aArgs[it]) }
              toRemove.add(a to b)*/
            }
            else -> {
              // different classes, check subclassing
              val s = TuPrologSolver.solve(Struct.of("super_class", a, b)).substitution
              eqs.applySubst(s)
              toRemove.add(a to b)
            }
          }
        }
      }
      if (toRemove.isNotEmpty()) {
        boundSTCache.addAll(toRemove)
        flushPendingConstraints()
      } else {
        break
      }
    }
  }

}

class CStore: CStoreBase() {
  val eqs = EQCStore()
  val sts = STCStore(eqs)

  val sub: Substitution get() = eqs.sub
  fun unif(a: Term, b: Term) = eqs.unif(a,b)

  fun unif(ts: Iterable<Term>) {
    ts.zipWithNext().forEach { unif(it.first, it.second)}
  }

  var preds: MutableSet<Pair<String,KList<Term>>> = linkedSetOf()

  fun add(c: Constr) {
    when (c) {
      is EqConstr -> unif(convert(c.a), convert(c.b))
      is STConstr -> {
        sts.addSTConstr(convert(c.a), convert(c.b))
      }
      is PredConstr -> processPred(c.f, c.args.plus(c.res).map(::convert))
    }
  }

  fun getValue(v: Var): Term {
    sts.flush()
    return v.apply(sub)
  }

  fun leastSolution(_v: Var): Term {
    val v = getValue(_v)
    return if (v is Var) {
      glb(sts.constraints.filter { it.second == v }.map { it.first })
    } else {
      v
    }
  }

  fun glb(a: Term, b: Term): Term = when {
    a == Atom.of("nothing") -> b
    b == Atom.of("nothing") -> a
    a == b -> a
    a == Atom.of("pyint") && b == Atom.of("uint64") -> a
    else -> TODO()
  }
  fun glb(ts: Collection<Term>): Term = ts.fold(Atom.of("nothing"), this::glb)

  fun solve(s: Struct): Substitution = TuPrologSolver.solveAndCorrectVars(s)

  fun checkOpSwappable(op: String): Pair<Atom,Atom>? {
    val a = Var.of("A")
    val b = Var.of("B")
    val s = TuPrologSolver.solveNF(Struct.of("swappable_op", Atom.of(op), a, b)).substitution
    return if (s.isFailed)
      null
    else {
      a.apply(s) as Atom to b.apply(s) as Atom
    }
  }

  fun processPred(p: String, ps: KList<Term>) {
    val ps = ps.map(sub::applyTo)
    if (p == "fun_type") {
      val s = solve(Struct.of(p, ps))
      eqs.applySubst(s)
    } else if (p == "op_type") {
      sts.flush()
      val nonGrounds = ps.subList(0, ps.size - 1).filter { it.isVariable }
      if (nonGrounds.any()) {
        fail()
      }
      val sw = checkOpSwappable((ps[0] as Atom).value)
      if (sw != null) {
        val tmp = Var.of("_")
        val s = solve(Struct.of("attr_ftype", ps[1], sw.first, List.of(tmp), ps[3]))
        eqs.applySubst(s)
        sts.addSTConstr(ps[2].apply(s), tmp.apply(s))
      } else {
        val s = solve(Struct.of(p, ps))
        eqs.applySubst(s)
      }
    } else if (p == "attr_type") {
      val nonGrounds = ps.subList(0, ps.size - 1).filter { it.isVariable }
      if (nonGrounds.any()) {
        fail()
      }
      val s = solve(Struct.of(p, ps))
      eqs.applySubst(s)
    } else if (p == "attr_fh") {
      val nonGrounds = ps.subList(0, ps.size - 1).filter { it.isVariable }
      if (nonGrounds.any()) {
        fail()
      }
      val s = solve(Struct.of(p, ps))
      eqs.applySubst(s)
    } else {
      TODO()
      preds.add(p to ps)
    }
    sts.flush()
  }

}
