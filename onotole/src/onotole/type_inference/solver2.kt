package onotole.type_inference

import onotole.fail
import onotole.kosaraju

class CoStore() {
  val eqs = mutableMapOf<FVar,FTerm>()
  val lbs = mutableMapOf<FVar,Set<FAtom>>()
  val ubs = mutableMapOf<FVar,Set<FAtom>>()
  val vars = mutableSetOf<Pair<FVar,FVar>>()
  fun addAll(cs: Collection<Constraint>) {
    var curr = cs
    while (curr.isNotEmpty()) {
      val derived = mutableSetOf<Constraint>()
      curr.forEach { (a, b) ->
        when {
          a is FAtom && b is FAtom -> derived.addAll(checkST(a, b)
              ?: fail("cannot assign $a to $b"))
          a is FVar && b is FVar -> vars.add(a to b)
          a is FAtom && b is FVar -> lbs.merge(b, setOf(a)) { a, b ->
            tryJoin(a + b)
          }
          a is FVar && b is FAtom -> ubs.merge(a, setOf(b)) { a, b ->
            tryMeet(a + b)
          }
        }
      }
      curr = derived
    }
  }
  @JvmName("addAll1")
  fun addAll(cs: Collection<ConST>) {
    addAll(cs.map { it.a to it.b })
  }
  @JvmName("addAll2")
  fun addAll(cs: Collection<ConEQ>) {
    addAll(cs.flatMap { listOf(it.a to it.b, it.b to it.a) })
  }
  fun getConstraints(): List<Constraint> {
    val a = lbs.flatMap { it.value.map { v -> v to it.key } }
    val b = ubs.flatMap { it.value.map { v -> it.key to v } }
    return a.plus(b).plus(vars)
  }
  fun applySubst(sub: Map<FVar, FTerm>) {
    val newEqs = applySubst(eqs.plus(sub), sub)
    eqs.putAll(newEqs)
    val cs = getConstraints()
    lbs.clear()
    ubs.clear()
    vars.clear()
    addAll(applySubst(cs, newEqs))
  }
  fun checkSat() {
    while (true) {
      val prev = getConstraints()
      val (comps, fwd, bwd) = makeDAG(vars)
      val varSub = makeSubst(comps)
      val sub = if (varSub.isNotEmpty()) {
        varSub
      } else {
        val vars = postorder(bwd)
        vars.forEach { v ->
          bwd[v]?.mapNotNull(lbs::get)?.let {
            val vals = it.flatten().toSet()
            addAll(tryJoin(vals).map { l -> l to v })
          }
        }
        vars.reversed().forEach { v ->
          fwd[v]?.mapNotNull(ubs::get)?.let {
            val vals = it.flatten().toSet()
            addAll(tryMeet(vals).map { u -> v to u })
          }
        }
        val sub = mutableMapOf<FVar, FTerm>()
        lbs.keys.intersect(ubs.keys).forEach { v ->
          val lb = lbs[v]!!
          val ub = ubs[v]!!
          val common = lb.intersect(ub)
          if (common.isNotEmpty()) {
            if (common.size > 1)
              TODO()
            sub[v] = common.first()
          } else {
            lb.forEach { l ->
              ub.forEach { u ->
                addAll(listOf(l to u))
              }
            }
          }
        }
        sub
      }
      if (sub.isNotEmpty()) {
        applySubst(sub)
      }
      if (getConstraints() == prev) {
        return
      }
    }
  }
}

fun applySubst(eqs: Map<FVar,FTerm>, sub: Map<FVar, FTerm>): Map<FVar,FTerm> = eqs.mapValues { applySubst(it.value, sub) }
fun applySubst(cs: Collection<Constraint>, sub: Map<FVar,FTerm>): Collection<Constraint> {
  return cs.map { applySubst(it.first, sub) to applySubst(it.second, sub) }
}
fun applySubst(a: FTerm, sub: Map<FVar,FTerm>): FTerm = when(a) {
  is FVar -> sub[a] ?: a
  is FAtom -> a.copy(ps = a.ps.map { applySubst(it, sub) })
}

fun makeSubst(comps: Collection<Collection<FVar>>): Map<FVar,FVar> {
  return comps.flatMap { it.zipWithNext() }.toMap()
}

fun checkST(a: FAtom, b: FAtom): Collection<Constraint>? {
  val (ok, newCS) = st(a, b)
  return if (ok) newCS else null
}

fun makeDAG(v2v: Collection<Pair<FVar,FVar>>): Triple<Collection<List<FVar>>, Map<FVar,List<FVar>>, Map<FVar,List<FVar>>> {
  val fwd = v2v.groupBy { it.first }.mapValues { it.value.map { it.second } }.toMap()
  val bwd = v2v.groupBy { it.second }.mapValues { it.value.map { it.first } }.toMap()
  val scc = findSCC(fwd, bwd)
  return Triple(scc, fwd, bwd)
}

fun <N> findSCC(fwd: Map<N, List<N>>, bwd: Map<N, List<N>>): Collection<List<N>> {
  return kosaraju(bwd, fwd).toList().groupBy { it.second }.mapValues { it.value.map { it.first } }.values
}
