package onotole.type_inference

import onotole.FreshNames
import onotole.fail
import onotole.kosaraju

class VarDeps {
  val fwd = mutableMapOf<FVar,MutableSet<FVar>>()
  val bwd = mutableMapOf<FVar,MutableSet<FVar>>()
  fun addST(a: FVar, b: FVar) {
    fwd.getOrPut(a) { mutableSetOf() }.add(b)
    bwd.getOrPut(b) { mutableSetOf() }.add(a)
  }
}

class CoSto() {
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
              ?: fail())
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
  fun getConstraints(): List<Constraint> {
    val a = lbs.flatMap { it.value.map { v -> v to it.key } }
    val b = ubs.flatMap { it.value.map { v -> it.key to v } }
    return a.plus(b).plus(vars)
  }
}

fun partitionConstraints(cs: Collection<Constraint>): Triple<Set<Pair<FAtom,FVar>>,Set<Pair<FVar,FAtom>>,Set<Pair<FVar,FVar>>> {
  var curr = cs
  val lbs = mutableSetOf<Pair<FAtom,FVar>>()
  val ubs = mutableSetOf<Pair<FVar,FAtom>>()
  val vs = mutableSetOf<Pair<FVar,FVar>>()
  while (curr.isNotEmpty()) {
    val derived = mutableSetOf<Constraint>()
    curr.forEach { (a,b) ->
      when {
        a is FAtom && b is FAtom -> derived.addAll(checkST(a, b) ?: fail())
        a is FAtom && b is FVar -> lbs.add(a to b)
        a is FVar && b is FAtom -> ubs.add(a to b)
        a is FVar && b is FVar -> vs.add(a to b)
      }
    }
    curr = derived
  }
  return Triple(lbs, ubs, vs)
}

fun mergeLowerBounds(lbs: Collection<Pair<FAtom,FVar>>, fn: FreshNames, newCS: MutableSet<Constraint>): Map<FVar,FAtom> {
  return lbs.groupBy { it.second }.mapValues {
    mergeLBS(it.value.map { it.first }, fn, newCS)
  }
}
fun mergeUpperBounds(ubs: Collection<Pair<FVar,FAtom>>, fn: FreshNames, newCS: MutableSet<Constraint>): Map<FVar,FAtom> {
  return ubs.groupBy { it.first }.mapValues { mergeUBS(it.value.map { it.second }, fn, newCS) }
}

fun norm2(cs: Collection<Constraint>, fn: FreshNames): Triple<Map<String,FTerm>,Map<String,FTerm>,Map<String,FTerm>> {
  val eqs = mutableMapOf<FVar,FTerm>()
  var curr = cs
  while (true) {
    val prev = curr
    val (_lbs, _ubs, vs) = partitionConstraints(curr)
    val (comps, fwd, bwd) = makeDAG(vs)
    val varSub = makeSubst(comps)
    if (varSub.isNotEmpty()) {
      eqs.putAll(varSub)
      eqs.putAll(applySubst(eqs, varSub))
      curr = applySubst(curr, varSub)
      continue
    } else {
      val newCs = mutableSetOf<Constraint>()
      val lbs = mergeLowerBounds(_lbs, fn, newCs).toMutableMap()
      val ubs = mergeUpperBounds(_ubs, fn, newCs).toMutableMap()
      val vars = postorder(bwd)
      vars.forEach { v ->
        val vals = (bwd[v] ?: emptySet()).mapNotNull { lbs[it] }.plus(lbs[v]?.let { listOf(it) } ?: emptyList())
        if (vals.isNotEmpty()) {
          lbs[v] = mergeLBS(vals, fn, newCs)
        }
      }
      vars.reversed().forEach { v ->
        val vals = (fwd[v] ?: emptySet()).mapNotNull { ubs[it] }.plus(ubs[v]?.let { listOf(it) } ?: emptyList())
        if (vals.isNotEmpty()) {
          ubs[v] = mergeUBS(vals, fn, newCs)
        }
      }
      val sub = mutableMapOf<FVar,FTerm>()
      lbs.keys.intersect(ubs.keys).forEach { v ->
        val lb = lbs[v]!!
        val ub = ubs[v]!!
        if (lb == ub)
          sub[v] = lb
        else
          newCs.addAll(checkST(lb, ub) ?: fail())
      }
      curr = lbs.map { it.value to it.key }.plus(ubs.toList()).plus(vs).plus(newCs)
      if (sub.isNotEmpty()) {
        eqs.putAll(sub)
        eqs.putAll(applySubst(eqs, sub))
        curr = applySubst(curr, sub)
      }
      if (curr == prev)
        return Triple(eqs.mapKeys { it.key.v }, lbs.mapKeys { it.key.v }, ubs.mapKeys { it.key.v })
    }
  }
}

fun norm3(cs: Collection<Constraint>, fn: FreshNames): Triple<Map<FVar,FTerm>,Map<FVar,FTerm>,Map<FVar,FTerm>> {
  val eqs = mutableMapOf<FVar,FTerm>()
  var curr = cs
  while (true) {
    val prev = curr
    val co = CoSto()
    co.addAll(curr)
    val (comps, fwd, bwd) = makeDAG(co.vars)
    val varSub = makeSubst(comps)
    if (varSub.isNotEmpty()) {
      eqs.putAll(varSub)
      val sub2 = applySubst(eqs, varSub)
      eqs.putAll(sub2)
      curr = applySubst(curr, sub2)
      continue
    } else {
      val newCs = mutableSetOf<Constraint>()
      val vars = postorder(bwd)
      vars.forEach { v ->
        bwd[v]?.mapNotNull(co.lbs::get)?.let {
          val vals = it.flatten().toSet()
          co.addAll(tryJoin(vals).map { l -> l to v })
        }
      }
      vars.reversed().forEach { v ->
        fwd[v]?.mapNotNull(co.ubs::get)?.let {
          val vals = it.flatten().toSet()
          co.addAll(tryMeet(vals).map { u -> v to u })
        }
      }
      val sub = mutableMapOf<FVar,FTerm>()
      vars.union(co.lbs.keys).union(co.ubs.keys).forEach { v ->
        val lb = co.lbs[v] ?: emptySet()
        val ub = co.ubs[v] ?: emptySet()
        val common = lb.intersect(ub)
        if (common.isNotEmpty()) {
          if (common.size > 1)
            TODO()
          sub[v] = common.first()
        } else {
          lb.forEach { l ->
            ub.forEach { u ->
              newCs.addAll(checkST(l, u) ?: fail())
            }
          }
        }
      }
      curr = co.getConstraints().plus(newCs)
      if (sub.isNotEmpty()) {
        eqs.putAll(sub)
        val sub2 = applySubst(eqs, sub)
        eqs.putAll(sub2)
        curr = applySubst(curr, sub2)
      }
      if (curr == prev) {
        val lbs = co.lbs.filterValues { it.size == 1 }.mapValues { it.value.first() }
        val ubs = co.ubs.filterValues { it.size == 1 }.mapValues { it.value.first() }
        return Triple(eqs.toMap(), lbs, ubs)
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
fun mergeLBS(lbs: Collection<FAtom>, fn: FreshNames, newCS: MutableSet<Constraint>): FAtom {
  return lbs.reduce { a,b ->
    val (res, ncs) = join(a,b, fn)
    newCS.addAll(ncs)
    res!!
  }
}

fun lub(a: FAtom, b: FAtom, fn: FreshNames): Pair<FAtom,Collection<Constraint>> {
  return when {
    a == b -> a to emptyList()
    a.n == b.n -> {
      if (a.ps.size != b.ps.size) fail()
      val (coP, inP, conP) = getTypeParams(a)
      if (conP.isNotEmpty()) TODO()
      val newCS = mutableSetOf<Constraint>()
      val coR = coP.map {
        val a = a.ps[it]
        val b = b.ps[it]
        if (a is FAtom && b is FAtom) {
          val (res, ncs) = lub(a, b, fn)
          newCS.addAll(ncs)
          res
        } else if (a == b) {
          a
        } else {
          val v = FVar(fn.fresh("L"))
          newCS.add(v to a)
          newCS.add(v to b)
          v
        }
      }
      val inR = inP.map { if (a.ps[it] != b.ps[it]) fail() else a.ps[it] }
      return FAtom(a.n, a.ps.indices.map {
        if (it in coP) coR[coP.indexOf(it)]
        else if (it in inP) inR[inP.indexOf(it)]
        else TODO()
      }) to newCS
    }
    else -> {
      val ancestorsOfA = getAncestors(a).map { it.n to it }.toMap()
      val ancestorsOfB = getAncestors(b).map { it.n to it }.toMap()
      if (a.n == b.n) fail()
      else if (a.n in ancestorsOfB) {
        // b's class is the result
        val (coP,inP,conP) = getTypeParams(b)
        if (conP.isNotEmpty()) TODO()
        val newCovars = coP.map { FVar(fn.fresh("L$it")) }
        val newPS = b.ps.indices.map {
          if (it in coP)
            newCovars[coP.indexOf(it)]
          else if (it in inP)
            b.ps[inP.indexOf(it)]
          else
            b.ps[conP.indexOf(it)]
        }
        val res = FAtom(b.n, newPS)
        val (r1, ncs1) = st(res, a)
        val (r2, ncs2) = st(res, b)
        if (!r1 || !r2) fail()
        res to ncs1.plus(ncs2)
      } else {
        lub(b, a, fn)
      }
//      val (r1, ncs1) = st(a, b)
//      val (r2, ncs2) = st(b, a)
//      if (r1 && r2)
//        TODO()
//      else if (!r1 && !r2)
//        fail()
//      else if (r1) {
//        a to ncs1
//      } else {
//        b to ncs2
//      }
    }
  }
}
fun mergeUBS(ubs: Collection<FAtom>, fn: FreshNames, newCS: MutableSet<Constraint>): FAtom {
  return ubs.reduce { a,b ->
    val (res, ncs) = lub(a,b, fn)
    newCS.addAll(ncs)
    res
  }
}
fun makeDAG(v2v: Collection<Pair<FVar,FVar>>): Triple<Collection<List<FVar>>, Map<FVar,List<FVar>>, Map<FVar,List<FVar>>> {
  val fwd = v2v.groupBy { it.first }.mapValues { it.value.map { it.second } }.toMap()
  val bwd = v2v.groupBy { it.second }.mapValues { it.value.map { it.first } }.toMap()
  val scc = kosaraju(bwd, fwd).toList().groupBy { it.second }.mapValues { it.value.map { it.first } }.values
  return Triple(scc, fwd, bwd)
}
fun <N> findSCC(fwd: Map<N,List<N>>, bwd: Map<N,List<N>>): Collection<Collection<N>> {
  return kosaraju(bwd, fwd).toList().groupBy { it.second }.mapValues { it.value.map { it.first } }.values
}
