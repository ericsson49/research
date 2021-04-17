import java.util.*

sealed class VarDef
class ParamVarDef(val name: String): VarDef()
data class StmtVarDef(val s: Stmt): VarDef() {
  override fun equals(other: Any?): Boolean {
    return this === other || other is StmtVarDef && this.s === other.s
  }
  override fun hashCode(): Int {
    return System.identityHashCode(this.s)
  }
}
data class PhiVarDef(val defs: List<VarDef>): VarDef()
class CopyVarDef(val vd: VarDef): VarDef()

class PhiWeb(val v: String) {
  val defs = mutableSetOf<VarDef>()
  var lastDef: VarSlot? = null
  var defStmt: VarDef? = null

  fun getNonPhiDefs(): Set<VarDef> {
    fun proc(vd: VarDef): List<VarDef> = when(vd) {
      is StmtVarDef -> listOf(vd)
      is PhiVarDef -> vd.defs.flatMap(::proc)
      is ParamVarDef -> listOf(vd)
      is CopyVarDef -> proc(vd.vd)
    }
    val mp = IdentityHashMap<VarDef, Unit>()
    defs.flatMap(::proc).forEach { mp[it] = Unit }
    return mp.keys.toSet()
  }
}

fun getNonPhiDefs(def: VarDef): List<VarDef> {
  return when(def) {
    is ParamVarDef -> listOf(def)
    is StmtVarDef -> listOf(def)
    is PhiVarDef -> def.defs.flatMap(::getNonPhiDefs)
    is CopyVarDef -> getNonPhiDefs(def.vd)
  }
}

fun getStmtDefs(def: VarDef): List<StmtVarDef> {
  return when(def) {
    is ParamVarDef -> emptyList()
    is StmtVarDef -> listOf(def)
    is PhiVarDef -> def.defs.flatMap(::getStmtDefs)
    is CopyVarDef -> emptyList()
  }
}

fun <K,V> Map<K,V>.merge(pairs: Collection<Pair<K,V>>): Map<K,V> {
  val res = this.toMutableMap()
  pairs.forEach {
    res[it.first] = it.second
  }
  return res
}

fun isSame(a: VarDef, b: VarDef): Boolean {
  return when(a) {
    is StmtVarDef -> b is StmtVarDef && a.s === b.s
    is ParamVarDef -> b is ParamVarDef
    is PhiVarDef -> b is PhiVarDef
        && a.defs.size == b.defs.size
        && a.defs.zip(b.defs).map { isSame(it.first, it.second) }.all { it }
    is CopyVarDef -> b is CopyVarDef && isSame(a.vd, b.vd)
  }
}
fun newDefs(a: Map<String, VarDef>, b: Map<String, VarDef>): Map<String,VarDef> {
  val res = mutableMapOf<String,VarDef>()
  for(k in b.keys) {
    if (k !in a || !isSame(b[k]!!, a[k]!!)) {
      res[k] = b[k]!!
    }
  }
  return res
}

fun newDefsAnalysis(stmts: Iterable<Stmt>, newDefsAnno: StmtAnnoMap<Set<String>>): Set<String> {
  val acc = mutableSetOf<String>()
  for(s in stmts) {
    acc.addAll(newDefsAnalysis(s, newDefsAnno))
  }
  return acc
}
fun newDefsAnalysis(s: Stmt, newDefsAnno: StmtAnnoMap<Set<String>>): Set<String> {
  if (s !in newDefsAnno) {
    newDefsAnno[s] = when (s) {
      is Return -> emptySet()
      is Assign -> getVarNamesInStoreCtx(s.target).toSet()
      is AnnAssign -> getVarNamesInStoreCtx(s.target).toSet()
      is AugAssign -> getVarNamesInStoreCtx(s.target).toSet()
      is For -> newDefsAnalysis(s.body, newDefsAnno)
      is While -> newDefsAnalysis(s.body, newDefsAnno)
      is If -> newDefsAnalysis(s.body, newDefsAnno).plus(newDefsAnalysis(s.orelse, newDefsAnno)).toSet()
      is Try -> newDefsAnalysis(s.body, newDefsAnno).plus(newDefsAnalysis(s.orelse, newDefsAnno))
          .plus(s.handlers.flatMap { newDefsAnalysis(it.body, newDefsAnno) })
          .plus(newDefsAnalysis(s.finalbody, newDefsAnno)).toSet()
      is Assert -> emptySet()
      is Expr -> emptySet()
      is Pass -> emptySet()
      is Break -> emptySet()
      is Continue -> emptySet()
      else -> fail("Not implemented yet $s")
    }
  }
  return newDefsAnno[s]!!
}

class VarDefAnalysis(val liveVars: Pair<StmtAnnoMap<Set<String>>,StmtAnnoMap<Set<String>>>) {
  val anno = StmtAnnoMap<Map<String,VarDef>>()
  val newDefs = StmtAnnoMap<Map<String,VarDef>>()
  val phiWebs = StmtAnnoMap<PhiWeb>()

  fun varDefAnalysis(c: List<Stmt>, _in: Map<String,VarDef>): Map<String,VarDef>
      = c.fold(_in, { acc, s ->  varDefAnalysis(s, acc) })
  fun varDefAnalysis(
      s: Stmt, _in: Map<String,VarDef>): Map<String,VarDef> {
    anno[s] = _in
    val res: List<Pair<String,VarDef>> = when (s) {
      is Return -> emptyList()
      is Assign -> {
        val names = getVarNamesInStoreCtx(s.target)
        names.map { it to StmtVarDef(s) }
      }
      is AnnAssign -> {
        val names = getVarNamesInStoreCtx(s.target)
        names.map { it to StmtVarDef(s) }
      }
      is AugAssign -> {
        val names = getVarNamesInStoreCtx(s.target)
        names.map { it to StmtVarDef(s) }
      }
      is For -> {
        val liveVarsBefore = liveVars.first[s]!!

        val names = getVarNamesInStoreCtx(s.target)
        val bodyIn = _in.merge(names.map { it to StmtVarDef(s) })
        val bodyOut = varDefAnalysis(s.body, bodyIn)
        val bodyUpds = newDefs(bodyIn, bodyOut).filter { it.key in liveVarsBefore }

        val newVars = bodyUpds.keys
        val mergedUpds = mutableMapOf<String,VarDef>()
        for (v in newVars) {
          val a = bodyUpds[v] ?: _in[v]!!
          val b = _in[v]!!
          mergedUpds[v] = PhiVarDef(listOf(a,b))
        }

        mergedUpds.forEach { (k, v) ->
          val defs = getStmtDefs(v)
          val existing = defs.filter { it.s in phiWebs }.map { phiWebs[it.s]!! }.toSet()
          if (existing.size > 1)
            fail("unexpected")
          val phiWeb = if (existing.isNotEmpty()) existing.first() else PhiWeb(k)
          phiWeb.defStmt = (v as PhiVarDef).defs[1]
          defs.filter { it.s !in phiWebs }.forEach {
            phiWebs[it.s] = phiWeb
            phiWeb.defs.add(it)
          }
        }
        mergedUpds.toList()
      }
      is While -> {
        val liveVarsBefore = liveVars.first[s]!!
        //val bodyVars = newDefsAnalysis(s.body, newDefsAnno)
        //val phiVars = bodyVars.filter { it in liveVarsBefore }

        val bodyIn = _in
        val bodyOut = varDefAnalysis(s.body, bodyIn)
        val bodyUpds = newDefs(_in, bodyOut).filter { it.key in liveVarsBefore }

        val newVars = bodyUpds.keys
        val mergedUpds = mutableMapOf<String,VarDef>()
        for (v in newVars) {
          val a = bodyUpds[v] ?: _in[v]!!
          val b = _in[v]!!
          mergedUpds[v] = PhiVarDef(listOf(a,b))
        }

        mergedUpds.forEach { (k, v) ->
          val defs = getStmtDefs(v)
          val existing = defs.filter { it.s in phiWebs }.map { phiWebs[it.s]!! }.toSet()
          if (existing.size > 1)
            fail("unexpected")
          val phiWeb = if (existing.isNotEmpty()) existing.first() else PhiWeb(k)
          phiWeb.defStmt = (v as PhiVarDef).defs[1]
          defs.filter { it.s !in phiWebs }.forEach {
            phiWebs[it.s] = phiWeb
            phiWeb.defs.add(it)
          }
        }
        mergedUpds.toList()
      }
      is If -> {
        val liveVarsAfter = liveVars.second[s]!!
        val bodyOut = varDefAnalysis(s.body, _in)
        val elseOut = varDefAnalysis(s.orelse, _in)
        val bodyUpds = newDefs(_in, bodyOut).filter { it.key in liveVarsAfter }
        val elseUpds = newDefs(_in, elseOut).filter { it.key in liveVarsAfter }

        val newVars = bodyUpds.keys.union(elseUpds.keys)
        val mergedUpds = mutableMapOf<String,VarDef>()
        for (v in newVars) {
          val a = bodyUpds[v] ?: _in[v]!!
          val b = elseUpds[v] ?: _in[v]!!
          mergedUpds[v] = PhiVarDef(listOf(a,b))
        }

        mergedUpds.forEach { (k, v) ->
          val defs = getStmtDefs(v)
          val existing = defs.filter { it.s in phiWebs }.map { phiWebs[it.s]!! }.toSet()
          if (existing.size > 1)
            fail("unexpected")
          val phiWeb = if (existing.isNotEmpty()) existing.first() else PhiWeb(k)
          phiWeb.defStmt = StmtVarDef(s)
          defs.filter { it.s !in phiWebs }.forEach {
            phiWebs[it.s] = phiWeb
            phiWeb.defs.add(it)
          }
        }
        mergedUpds.toList()
      }
      is Try -> {
        if (s.finalbody.size > 0)
          fail("not yet implemented")
        val liveVarsAfter = liveVars.second[s]!!
        val bodyOut = varDefAnalysis(s.body, _in)
        val bodyElseOut = varDefAnalysis(s.orelse, bodyOut)
        val handlerOuts = s.handlers.map {
          val handlerIn = _in.merge(it.name?.let { listOf(it to StmtVarDef(s)) } ?: emptyList())
          varDefAnalysis(it.body, handlerIn)
        }



        val bodyElseUpds = newDefs(_in, bodyElseOut).filter { it.key in liveVarsAfter }
        val handlerUpds = handlerOuts.map { newDefs(_in, it).filter { it.key in liveVarsAfter } }

        val newVars = bodyElseUpds.keys.union(handlerUpds.flatMap { it.keys })
        val mergedUpds = mutableMapOf<String,VarDef>()
        for (v in newVars) {
          val a = bodyElseUpds[v] ?: _in[v]!!
          val bs = handlerUpds.map { it[v] ?: _in[v]!! }
          mergedUpds[v] = PhiVarDef(listOf(a).plus(bs))
        }

        mergedUpds.forEach { (k, v) ->
          val defs = getStmtDefs(v)
          val existing = defs.filter { it.s in phiWebs }.map { phiWebs[it.s]!! }.toSet()
          if (existing.size > 1)
            fail("unexpected")
          val phiWeb = if (existing.isNotEmpty()) existing.first() else PhiWeb(k)
          phiWeb.defStmt = StmtVarDef(s)
          defs.filter { it.s !in phiWebs }.forEach {
            phiWebs[it.s] = phiWeb
            phiWeb.defs.add(it)
          }
        }

        mergedUpds.toList()
      }
      is Assert -> emptyList()
      is Expr -> emptyList()
      is Pass -> emptyList()
      is Break -> emptyList()
      is Continue -> emptyList()
      else -> fail("Not implemented yet $s")
    }
    val upds = res.toMap()
    newDefs[s] = upds
    return _in.merge(res)
  }

}

fun varDefAnalysis(f: FunctionDef): Triple<StmtAnnoMap<Map<String,VarDef>>,StmtAnnoMap<Map<String,VarDef>>,StmtAnnoMap<PhiWeb>> {
  val liveVars = liveVarAnalysis(f)
  val params = f.args.args.map { it.arg to ParamVarDef(it.arg) }.toMap()
  val vda = VarDefAnalysis(liveVars)
  vda.varDefAnalysis(f.body, params)
  return Triple(vda.anno, vda.newDefs, vda.phiWebs)
}