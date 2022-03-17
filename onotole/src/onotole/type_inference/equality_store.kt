package onotole.type_inference

import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Term
import it.unibo.tuprolog.core.Var
import onotole.FreshNames
import onotole.fail
import onotole.type_inference.EQCStore
import kotlin.jvm.functions.FunctionN

interface IEQStore {
  fun unif(a: FTerm, b: FTerm)
  fun find(t: FTerm): FTerm
  operator fun contains(a: FTerm): Boolean
}

class UFEQStore2(val fn: FreshNames): IEQStore {
  val repr = mutableMapOf<FTerm,FTerm>()
  val classList = mutableMapOf<FTerm,MutableSet<FTerm>>()

  override fun contains(a: FTerm) = a in repr

  override fun unif(a: FTerm, b: FTerm) {
    merge(a, b)
  }

  override fun find(t: FTerm) = t.rep

  var FTerm.rep
    get() = repr.getOrPut(this) { this }
    set(v) {
      repr[this] = v
    }
  val FTerm.peers get() = classList.getOrPut(this) { mutableSetOf(this) }
  fun union(a: FTerm, b: FTerm) {
    val ar = a.rep
    val br = b.rep
    if (ar != br) {
      val (from, to) = if (ar.peers.size < br.peers.size) ar to br else br to ar
      from.peers.forEach {
        it.rep = to
      }
      to.peers.addAll(from.peers)
      from.peers.clear()
    }
  }

  fun findCongruentPredecessors(t: FTerm): Set<FAtom> {
    fun findIn(c: Collection<FTerm>): List<FAtom> {
      val funcs = c.filterIsInstance<FAtom>().filter { it.ps.isNotEmpty() }
      val matches = funcs.filter { t in it.ps }
      return matches.plus(funcs.flatMap { findIn(it.ps)})
    }
    return findIn(repr.keys.toSet()).toSet()
  }
  fun congruent(a: FAtom, b: FAtom): Boolean {
    if (a.n != b.n || a.ps.size != b.ps.size) return false
    return a.ps.zip(b.ps).all { (a,b) -> a.rep == b.rep }
  }
  fun merge(a: FTerm, b: FTerm) {
    if (a.rep != b.rep) {
      union(a, b)
      // trigger bijective merges
      val elems = a.rep.peers.filterIsInstance<FAtom>()
      elems.zipWithNext().forEach { (a, b) ->
        if (a.n != b.n || a.ps.size != b.ps.size) fail("could not unify $a and $b")
        a.ps.zip(b.ps).forEach { merge(it.first, it.second) }
      }
      val xs = findCongruentPredecessors(a)
      val ys = findCongruentPredecessors(b)
      for (x in xs) {
        for (y in ys) {
          if (x.rep != y.rep && congruent(x, y)) {
            merge(x, y)
          }
        }
      }
    }
  }
}

class UFEQStore(val fn: FreshNames): IEQStore {
  data class FT(val n: String, val ps: List<String>) {
    override fun toString() = n + (if (ps.isEmpty()) "" else ps.joinToString(",","[","]"))
  }
  val repr = mutableMapOf<String,String>()
  val classVars = mutableMapOf<String,MutableSet<String>>()
  val classAtoms = mutableMapOf<String,FT>()
  val atomLookup = mutableMapOf<FT,String>()
  val uses = mutableMapOf<String,MutableSet<Pair<FT,String>>>()

  private fun new(n: String) {
    if (n in repr) fail("already exist")
    repr[n] = n
    classVars[n] = mutableSetOf(n)
    uses[n] = mutableSetOf()
  }
  private fun merge(a: String, b: String) {
    val ar = find(a)
    val br = find(b)
    if (ar != br) {
      val (from, to) = if (classVars[ar]!!.size < classVars[br]!!.size) {
        ar to br
      } else br to ar
      classVars[from]!!.forEach {
        repr[it] = to
      }
      classVars[to]!!.addAll(classVars[from]!!)
      val toMerge = mutableListOf<Pair<String,String>>()
      if (classAtoms[from] != null) {
        val fromA = classAtoms[from]!!
        if (classAtoms[to] != null) {
          val toA = classAtoms[to]!!
          if (fromA.n != toA.n || fromA.ps.size != toA.ps.size) fail()
          toMerge.addAll(fromA.ps.zip(toA.ps))
        }
        classAtoms[to] = fromA
      }
      classVars.remove(from)
      classAtoms.remove(from)
      uses[from]!!.forEach { (ft,r) ->
        val nft = ft.copy(ps = ft.ps.map(::find))
        if (atomLookup[nft] != null) {
          toMerge.add(r to atomLookup[nft]!!)
        } else {
          atomLookup[nft] = r
          uses[to]!!.add(ft to r)
        }
      }
      uses.remove(from)
      toMerge.forEach {
        merge(it.first, it.second)
      }
    }
  }

  private fun find(a: String) = repr[a]!!
  override fun find(t: FTerm) = when (t) {
    is FVar -> FVar(repr.getOrPut(t.v) { new(t.v); t.v })
    is FAtom -> {
      val ft = toFT(t)
      if (ft in atomLookup) {
        FVar(find(atomLookup[ft]!!))
      } else {
        val n = fn.fresh("R")
        new(n)
        classAtoms[n] = ft
        atomLookup[ft] = n
        ft.ps.forEach {
          uses[it]!!.add(ft to n)
        }
        FVar(n)
      }
    }
  }

  override fun contains(a: FTerm): Boolean {
    return toFT(a as FAtom) in atomLookup
  }

  private fun toFT(a: FAtom) = FT(a.n, a.ps.map { if (it !is FVar) fail("should be flat") else find(it.v) })
  override fun unif(a: FTerm, b: FTerm) {
    when {
      a is FVar && b is FVar -> merge(a.v, b.v)
      a is FAtom && b is FAtom -> TODO()
      a is FAtom && b is FVar -> unif(b, a)
      a is FVar && b is FAtom -> {
        val ft = toFT(b)
        if (ft in atomLookup) {
          merge(a.v, atomLookup[ft]!!)
        } else {
          atomLookup[ft] = a.v
          ft.ps.forEach {
            uses[it]!!.add(ft to a.v)
          }
        }
      }
    }
  }
}

class TUPEQStore2(val fn: FreshNames): IEQStore {
  val varMap = mutableMapOf<String, Var>()
  val eqs = EQCStore()
  fun convert(t: FTerm): Term = when(t) {
    is FVar -> varMap.getOrPut(t.v) { Var.of(t.v) }
    is FAtom -> {
      val nps = t.ps.map(::convert)
      Struct.of(t.n, nps)
    }
  }
  fun convertBack(t: Term): FTerm = when {
    t.isVariable -> FVar((t as Var).name)
    t.isStruct -> FAtom((t as Struct).functor, t.argsList.map(::convertBack))
    else -> TODO()
  }
  override fun unif(a: FTerm, b: FTerm) {
    eqs.unif(convert(a), convert(b))
  }

  override fun find(t: FTerm): FTerm {
    return convertBack(eqs.sub.applyTo(convert(t)))
  }

  override fun contains(a: FTerm): Boolean {
    TODO("Not yet implemented")
  }

}
class TUPEQStore(val fn: FreshNames): IEQStore {
  val varMap = mutableMapOf<String, Var>()
  val atomMap = mutableMapOf<FAtom,FVar>()
  val eqs = EQCStore()
  fun convert(t: FTerm): Term = when(t) {
    is FVar -> varMap.getOrPut(t.v) { Var.of(t.v) }
    is FAtom -> {
      val nps = t.ps.map(::convert)
      if (nps.any { !it.isVariable }) fail("should be flate")
      Struct.of(t.n, nps)
    }
  }
  fun convertBack(t: Term): FTerm = when {
    t.isVariable -> FVar((t as Var).name)
    t.isStruct -> FAtom((t as Struct).functor, t.argsList.map(::convertBack))
    else -> TODO()
  }
  override fun unif(a: FTerm, b: FTerm) {
    eqs.unif(convert(a), convert(b))
  }

  private fun findInEQs(a: FAtom): Var? {
    val res = eqs.sub.entries.find { (v,t) ->
      t.isStruct && (t as Struct).functor == a.n
          && t.argsList == a.ps.map { convert(it as FVar) } }
    return res?.key
  }
  override fun contains(a: FTerm): Boolean {
    val ar = internAtomParams(a as FAtom)
    return ar in atomMap || findInEQs(ar) != null
  }

  private fun internAtomParams(a: FAtom): FAtom {
    if (a.ps.any { it !is FVar }) fail("should be flat")
    return a.copy(ps = a.ps.map { internVar((it as FVar).v) })
  }

  private fun postIntern(r: FTerm): FTerm {
    return if (r is FAtom) {
      atomMap.getOrPut(r) {
        val res = findInEQs(r)
        if (res != null)
          convertBack(res)
        else {
          val newV = convert(FVar(fn.fresh("R")))
          eqs.unif(newV, convert(r))
          convertBack(newV)
        } as FVar
      }
    } else r

  }
  override fun find(t: FTerm) = when(t) {
    is FVar -> internVar(t.v)
    is FAtom -> internAtom(t)
  }
  fun internVar(v: String): FVar {
    return postIntern(convertBack(eqs.sub.applyTo(convert(FVar(v))))) as FVar
  }

  fun internAtom(a: FAtom): FVar {
    return postIntern(internAtomParams(a)) as FVar
  }
}
