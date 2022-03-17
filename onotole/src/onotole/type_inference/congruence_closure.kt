package onotole.type_inference

import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Substitution
import it.unibo.tuprolog.core.Term
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.unify.Unificator
import onotole.fail

fun unify(cs: Collection<Collection<FTerm>>): Map<FVar, FTerm> {
  val eq = EQCStore()
  fun unify(ts: Collection<FTerm>) = ts.zipWithNext().forEach { (a, b) -> eq.unif(convert(a), convert(b))}
  cs.forEach { unify(it) }
  return eq.sub.map { FVar(it.key.name) to convertBack(it.value) }.toMap()
}
fun convert(t: FTerm): Term = when(t) {
  is FVar -> Var.of(t.v)
  is FAtom -> Struct.of(t.n, t.ps.map { convert(it) })
}
fun convertBack(t: Term): FTerm = if (t.isVariable) {
  FVar((t as Var).name)
} else if (t.isStruct) {
  val s = t as Struct
  FAtom(s.functor, s.argsList.map { convertBack(it) })
} else fail()

class EQS {
  var sub: Substitution = Substitution.empty()

  fun unif(a: FTerm, b: FTerm) {
    val s = Unificator.strict(sub).mgu(convert(a), convert(b))
    if (s.isFailed) fail()
    applySubst(s)
  }
  fun applySubst(s: Substitution) {
    if (s.isNotEmpty()) {
      val s2 = Substitution.of(sub, s)
      if (s2.isFailed) fail()
      sub = s2
    }
  }
}

typealias C = FTerm
typealias F = FAtom
data class TT(val f: F, val r: C)
class CC() {
  @JvmInline
  value class R(val c: C)
  val pending = mutableListOf<Pair<C,C>>()
  val _repr = mutableMapOf<C,R>()
  val _classList = mutableMapOf<R,MutableSet<C>>()
  val _useList = mutableMapOf<R,MutableSet<TT>>()
  val lookup = mutableMapOf<Pair<String,List<R>>,C>()

  var C.r
    get() = _repr.getOrPut(this) { R(this) }
    set(c: R) { _repr[this] = c }

  val R.uses: MutableSet<TT>
    get() = _useList.getOrPut(this) { mutableSetOf() }

  val R.cls: MutableSet<C>
    get() = _classList.getOrPut(this) { mutableSetOf(this.c) }

  fun merge(s: C, t: C) {
    pending.add(s to t)
    propagate()
  }
  fun merge(f: F, a: C) {
    val fr = f.n to f.ps.map { it.r }
    val t = lookup[fr]
    if (t != null) {
      pending.add(a to t)
      propagate()
    } else {
      lookup[fr] = a
      val t = TT(f, a)
      fr.second.forEach {
        it.uses.add(t)
      }
    }
  }
  fun propagate() {
    while (pending.isNotEmpty()) {
      val (a, b) = pending.removeAt(0)
      if (a.r != b.r) {
        val oldRep = a.r
        oldRep.cls.forEach { c ->
          c.r = b.r
          b.r.cls.add(c)
        }
        oldRep.cls.clear()
        oldRep.uses.forEach { t1 ->
          val r = t1.f.n to t1.f.ps.map { it.r }
          val t2 = lookup[r]
          if (t2 != null) {
            pending.add(t1.r to t2)
          } else {
            lookup[r] = t1.r
            b.r.uses.add(t1)
          }
        }
        oldRep.uses.clear()
      }
    }
  }
  fun normalize(c: C) {

  }
}