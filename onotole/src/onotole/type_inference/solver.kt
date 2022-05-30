package onotole.type_inference

import onotole.FreshNames
import onotole.Graph
import onotole.fail
import onotole.kosaraju
import onotole.reverse
import java.lang.RuntimeException

sealed class FTerm
data class FVar(val v: String): FTerm() {
  override fun toString(): String = v
}
data class FAtom(val n: String, val ps: List<FTerm> = emptyList()): FTerm() {
  override fun toString(): String = if (ps.isEmpty()) n else n + ps.joinToString(",", "[", "]")
}

typealias Constraint = Pair<FTerm, FTerm>

context (TypingCtx)
fun getAncestors(a: FAtom): List<FAtom> = listOf(a).plus(getBase(a)?.let { getAncestors(it) } ?: emptyList())

context (TypingCtx)
fun getAncestorByClassName(a: FAtom, cn: String): FAtom? {
  val ancestorsOfA = getAncestors(a)
  return ancestorsOfA.find { it.n == cn }
}

context (TypingCtx)
fun tryMeet(elems: Set<FAtom>): Set<FAtom> {
  if (elems.size == 1)
    return elems
  else {
    for((a,b) in elems.zipWithNext()) {
      val (a1,b1) = tryMeet(a, b)
      if (a1 == b1)
        return tryMeet(elems.minus(setOf(a,b)).plus(a1))
    }
    return elems
  }
}
context (TypingCtx)
fun tryMeet(a: FAtom, b: FAtom): Pair<FAtom,FAtom> {
  val a = if (a.n == "pylib.Optional") a.ps[0] as FAtom else a
  val b = if (b.n == "pylib.Optional") b.ps[0] as FAtom else b
  return when {
    a == b -> a to b
    else -> {
      val r1 = tryCheckST(a, b)
      val r2 = tryCheckST(b, a)
      when {
        r1 == true && r2 == true -> TODO() // shouldn't happen as it means a==b
        r1 == true -> a to a
        r2 == true -> b to b
        r1 == false && r2 == false -> a to b
        else -> a to b
      }
    }
  }
}
context (TypingCtx)
fun tryJoin(elems: Set<FAtom>): Set<FAtom> {
  if (elems.size == 1)
    return elems
  else {
    for((a,b) in elems.zipWithNext()) {
      val (a1, b1) = tryJoin(a, b)
      if (a1 == b1)
        return tryJoin(elems.minus(setOf(a,b)).plus(a1))
    }
    return elems
  }
}
context (TypingCtx)
fun tryJoin(a: FAtom, b: FAtom): Pair<FAtom,FAtom> {
  class ContinueException: RuntimeException()
  val OBJ = FAtom("pylib.object")
  return when {
    a == OBJ || b == OBJ -> OBJ to OBJ
    a.n == "pylib.None" -> b to b
    b.n == "pylib.None" -> a to a
    a == b -> a to b
    else -> {
      val ancestorsOfA = getAncestors(a).associateBy { it.n }
      val ancestorsOfB = getAncestors(b).associateBy { it.n }
      val commonAncestors = ancestorsOfA.keys.intersect(ancestorsOfB.keys)
      val bases = ancestorsOfA.keys.filter { it in commonAncestors }
      for (lub in bases) {
        val baseOfA = ancestorsOfA[lub]!!
        val baseOfB = ancestorsOfB[lub]!!
        val (coP, inP, conP) = getTypeParams(baseOfA)
        if (conP.isNotEmpty()) TODO()
        try {
          val coR = coP.map {
            val (a, b) = baseOfA.ps[it] to baseOfB.ps[it]
            if (a == OBJ || b == OBJ)
              OBJ
            else if (a == b)
              a
            else if (a is FAtom && b is FAtom) {
              val (a1, b1) = tryJoin(a, b)
              if (a1 == b1)
                a1
              else
                return baseOfA to baseOfB
            } else
              return baseOfA to baseOfB
          }
          val inR = inP.map {
            val (a, b) = baseOfA.ps[it] to baseOfB.ps[it]
            if (a == b)
              a
            else if (a is FAtom && b is FAtom)
              throw ContinueException()
            else
              return baseOfA to baseOfB
          }
          val res = FAtom(lub, baseOfA.ps.indices.map {
            if (it in coP) coR[coP.indexOf(it)]
            else if (it in inP) inR[inP.indexOf(it)]
            else TODO()
          })
          return res to res
        } catch (e: ContinueException) {
          println()
        }
      }
      TODO()
    }
  }
}
context (TypingCtx)
fun join(a: FAtom?, b: FAtom?, fn: FreshNames): Pair<FAtom?,List<Constraint>> {
  return when {
    a == b -> a to emptyList()
    a == null -> b to emptyList()
    b == null -> a to emptyList()
    else -> {
      val ancestorsOfA = getAncestors(a).map { it.n to it }.toMap()
      val ancestorsOfB = getAncestors(b).map { it.n to it }.toMap()
      val commonAncestors = ancestorsOfA.keys.intersect(ancestorsOfB.keys)
      ancestorsOfA.keys.filter { it in commonAncestors }.forEach { lub ->
        val baseOfA = ancestorsOfA[lub]!!
        val baseOfB = ancestorsOfB[lub]!!
        val (coP, inP, conP) = getTypeParams(baseOfA)
        if (conP.isNotEmpty()) TODO()
        try {
          val newCs = mutableListOf<Constraint>()
          val coR = coP.map { baseOfA.ps[it] }
              .zip(coP.map { baseOfB.ps[it] })
              .map {
                val a = it.first
                val b = it.second
                if (a is FAtom && b is FAtom) {
                  val (res, cs) = join(a, b, fn)
                  newCs.addAll(cs)
                  res!!
                } else if (a == b)
                  a
                else {
                  val v = FVar(fn.fresh("J"))
                  newCs.add(a to v)
                  newCs.add(b to v)
                  v
                }
              }
          val inR = inP.map { baseOfA.ps[it] }
              .zip(inP.map { baseOfB.ps[it] })
              .map {
                val a = it.first
                val b = it.second
                if (a != b)
                  fail()
                else
                  a
              }
          return FAtom(lub, baseOfA.ps.indices.map {
            if (it in coP) coR[coP.indexOf(it)]
            else if (it in inP) inR[inP.indexOf(it)]
            else TODO()
          }) to newCs
        } catch(e: RuntimeException) {
          println()
        }
      }
      TODO()
    }
  }
}
context (TypingCtx)
fun tryCheckST(a: FAtom, b: FAtom): Boolean? {
  val (res, cs) = st(a, b)
  return if (!res) false
  else {
    val (withVars, withoutVars) = cs.partition { it.first is FVar || it.second is FVar }
    if (withVars.isNotEmpty())
      null
    else {
      withoutVars.forEach {
        val r = tryCheckST(it.first as FAtom, it.second as FAtom)
        if (r != true)
          return r
      }
      return true
    }
  }
}
context (TypingCtx)
fun st(a: FAtom, b: FAtom): Pair<Boolean,Set<Constraint>> {
  return when {
    a == b -> true to emptySet()
    a.n == "pylib.None" -> true to emptySet()
    (a.n == "Tuple" || a.n == "pylib.Tuple") && (b.n == "pylib.Sequence" || b.n == "pylib.Iterable") -> {
      true to a.ps.map { it to b.ps[0] }.toSet()
    }
    a.n == "pylib.bytes" && b.n == "phase0.BLSPubkey" -> true to emptySet()
    a.n == "ssz.uint8" && b.n == "altair.ParticipationFlags" -> true to emptySet()
    a.n == "pylib.int" && b.n == "pylib.bool" -> true to emptySet()
    a.n == "ssz.Hash32" && b.n == "phase0.Root" -> true to emptySet()
    a.n == "ssz.Bytes96" && b.n == "phase0.BLSSignature" -> true to emptySet()
    else -> {
      val am = getAncestorByClassName(a, b.n)
      if (am != null) {
        if (am.ps.size != b.ps.size)
          fail()
        val (coP,inP,conP) = getTypeParams(am)
        val cs = am.ps.zip(b.ps)
        val coCS = coP.map { cs[it] }
        val inCS = inP.flatMap { listOf(cs[it], cs[it].second to cs[it].first) }
        val conCS = conP.map { cs[it].second to cs[it].first }

        val newConstraints = coCS.plus(inCS).plus(conCS)
        true to newConstraints.toSet()
      } else false to emptySet()
    }
  }
}

fun <N> postorder(graph: Map<N,Collection<N>>): List<N> {
  val visited = mutableSetOf<N>()
  val res = mutableListOf<N>()
  fun walk(n: N) {
    if (n !in visited) {
      visited.add(n)
      (graph[n] ?: emptySet()).forEach(::walk)
      res.add(n)
    }
  }
  graph.keys.forEach(::walk)
  return res
}
context (TypingCtx)
fun solve(cs: Collection<Constraint>, fn: FreshNames): Map<FVar,FAtom> {
  val lowerBounds = mutableMapOf<FVar, FAtom>()
  val tested = mutableSetOf<Pair<FAtom,FAtom>>()
  val constraints = cs.toMutableList()
  do {
    var lbUpdated = false
    val toDrop = mutableListOf<Constraint>()
    val toAdd = mutableListOf<Constraint>()
    for((a,b) in constraints) {
      val av = when(a) { is FVar -> lowerBounds[a]; is FAtom -> a }
      when(b) {
        is FVar -> {
          val bv = lowerBounds[b]
          val (nbv, ncs) = join(av, bv, fn)
          if (nbv != bv) {
            lowerBounds[b] = nbv!!
            lbUpdated = true
          }
          if (a is FAtom) {
            toDrop.add(a to b)
          }
          toAdd.addAll(ncs)
        }
        is FAtom -> {
          if (av != null && av to b !in tested) {
            val (r, ncs) = st(av, b)
            if (!r)
              fail()
            tested.add(av to b)
            if (ncs.isNotEmpty()) {
              toAdd.addAll(ncs)
            }
          }
        }
      }
    }
    val constrAdded = constraints.addAll(toAdd)
    constraints.removeAll(toDrop)
  } while (lbUpdated or constrAdded)
  return lowerBounds
}

interface IConstrStore<T> {
  fun freshVar(p: String? = null): T
  fun mkVar(n: String): T
  fun mkAtom(a: String, ps: List<T>): T
  fun mkAtom(a: String) = mkAtom(a, emptyList())
  fun mkAtom(a: String, vararg ps: T) = mkAtom(a, ps.toList())
  fun mkAtom(a: String, vararg ps: String) = mkAtom(a, ps.toList().map(::mkVar))
  fun addST(a: T, b: T)
  fun addEQ(a: T, b: T)
}

context (TypingCtx)
class ConstrStore: IConstrStore<FTerm> {
  val fn = FreshNames()
  val eqs: TUPEQStore2 = TUPEQStore2(fn)
  override fun freshVar(p: String?): FTerm = FVar(fn.fresh(p ?: "C"))
  override fun mkVar(n: String): FVar = FVar(n)
  override fun mkAtom(a: String, ps: List<FTerm>) = FAtom(a, ps)

  fun getBaseClass(a: FAtom): FAtom? {
    return getBase(a)
  }
  fun flatten(t: FTerm): FTerm {
    return when (t) {
      is FVar -> eqs.find(t)
      is FAtom -> {
        if (t.n == "pylib.Optional") {
          flatten(t.ps[0])
        } else {
          val ps = t.ps.map {
            val p = flatten(it)
            if (p !is FVar) {
              val nv = FVar(fn.fresh("C"))
              addEQ(nv, p)
              nv
            } else p
          }
          val a = t.copy(ps = ps)
          if (a !in bcProcessed) {
            // add superclasses
            getBaseClass(a)?.let { facts.add(a to (flatten(it) as FAtom)) }
            eqs.find(a)
          } else eqs.find(a)
        }
      }
    }
  }
  val bcProcessed = mutableSetOf<FAtom>()
  val facts = mutableSetOf<Pair<FAtom,FAtom>>()
  val stConstraints = mutableSetOf<Pair<FTerm,FTerm>>()
  override fun addST(a: FTerm, b: FTerm) {
    val af = flatten(a)
    val bf = flatten(b)
    if (af != bf)
      stConstraints.add(af to bf)
  }
  override fun addEQ(a: FTerm, b: FTerm) {
    val a1 = flatten(a)
    val b1 = flatten(b)
    if (a1 != b1) {
      if (a1 is FAtom && b1 is FAtom)
        TODO("split equality")
      eqs.unif(a1, b1)
    }
  }

  fun simplify() {
    var curr: Set<Pair<FTerm,FTerm>> = proc1(stConstraints)
    var currF: Set<Pair<FAtom,FAtom>> = proc2(facts)
    do {
      val comps = calcSCC(curr.union(currF))
      comps.forEach { it.zipWithNext(eqs::unif) }
      val prev = curr
      val prevF = currF
      curr = proc1(curr)
      currF = proc2(currF)
    } while (curr != prev && currF != prevF)
    stConstraints.clear()
    stConstraints.addAll(curr)
    facts.clear()
    facts.addAll(currF)
  }

  private fun proc2(currF: Set<Pair<FAtom, FAtom>>) =
      currF.map {
        it.first.copy(ps = it.first.ps.map(eqs::find)) to it.second.copy(ps = it.second.ps.map(eqs::find))
      }.filter { it.first != it.second }.toSet()

  private fun proc(a: FTerm): FTerm = when(a) {
    is FVar -> eqs.find(a)
    is FAtom -> eqs.find(a.copy(ps = a.ps.map(::proc)))
  }
  private fun proc1(curr: Set<Pair<FTerm, FTerm>>) =
      curr.map {
        proc(it.first) to proc(it.second) }.filter { it.first != it.second }.toSet()

  private fun calcSCC(cs: Collection<Pair<FTerm,FTerm>>): Collection<List<FTerm>> {
    val trans = cs.groupBy { it.first }.mapValues { it.value.map { it.second } }
    val g = object : Graph<FTerm> {
      override val transitions = trans
      override val reverse = reverse(transitions)
    }
    return kosaraju(g).toList().groupBy { it.second }.mapValues { it.value.map { it.first } }.values
  }

}

fun main() {
  with(TypingContext) {
    val b = ConstrStore()
/*
    b.addST(b.mkAtom("seq","T"), b.mkVar("B"))
    b.addST(b.mkVar("B"), b.mkAtom("seq", "U"))
    b.addST(b.mkAtom("seq", "U"), b.mkAtom("seq", "T"))
    b.addST(b.mkAtom("list", "U"), b.mkVar("A"))
    b.addST(b.mkVar("A"), b.mkAtom("list", "T"))
*/
/*
    b.addST(b.mkAtom("uint"), b.mkVar("T0"))
    b.addST(b.mkVar("T0"), b.mkVar("T2"))
    b.addST(b.mkVar("T1"), b.mkVar("T2"))
    b.addST(b.mkAtom("int"), b.mkVar("T1"))
*/
    b.addST(b.mkAtom("list", b.mkAtom("list", "V")), b.mkVar("A"))
    b.addST(b.mkAtom("list", b.mkAtom("list", "U")), b.mkVar("A"))
    //b.addEQ(b.mkVar("U"), b.mkVar("V"))

    b.simplify()
    solve(b.stConstraints, b.fn)
    println()
  }
}