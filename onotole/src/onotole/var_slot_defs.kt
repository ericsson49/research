package onotole

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

data class IfPhiInfo(val target: VarSlot, val bodyCopy: VarSlot?, val elseCopy: VarSlot?)

class VarInfo {
  val slots = mutableListOf<VarSlot>()
  val ifAnnoMap = StmtAnnoMap<MutableMap<String,IfPhiInfo>>()
  val newSlots = StmtAnnoMap<MutableMap<String,VarSlot>>()
  val phiWebMap: MutableMap<PhiVarDef, PhiWeb> = IdentityHashMap<PhiVarDef, PhiWeb>()

  val values = mutableMapOf<String,MutableSet<String>>()

  private fun newValueId(v: String): String {
    val g = values.getOrPut(v, ::mutableSetOf)
    val prefix = if (g.isEmpty()) "" else if (g.size == 1) "_" else "_${g.size}"
    val res = v + prefix
    g.add(res)
    return res
  }
  private fun addSlot(vs: VarSlot, s: Stmt?): VarSlot {
    slots.add(vs)
    if (s != null) {
      if (newSlots[s] == null)
        newSlots[s] = mutableMapOf(vs.name to vs)
      else
        newSlots[s]!![vs.name] = vs
    }
    return vs
  }
  fun newParamSlot(param: String) = addSlot(VarSlot(param, newValueId(param), VarNameVersion(param, 0), ParamVarDef(param)), null)
  fun newVarSlot(v: String, varName: VarNameVersion, s: Stmt) = addSlot(VarSlot(v, newValueId(v), varName, StmtVarDef(s), mutableListOf()), s)
  fun newPhiSlot(v: String, varName: VarNameVersion, s: Stmt, defs: List<VarDef>): VarSlot {
    val phiVars = defs.flatMap { if (it is PhiVarDef) listOf(it) else emptyList() }
    val phiWebs = phiVars.map { phiWebMap[it]!! }.toSet()
    val phiWeb: PhiWeb
    if (phiWebs.isEmpty()) {
      phiWeb = PhiWeb(v)
    } else if (phiWebs.size > 1) {
      fail("too many phi webs")
    } else {
      phiWeb = phiWebs.toList()[0]
    }
    val def = PhiVarDef(defs)
    phiWebMap[def] = phiWeb
    phiWeb.defs.add(def)
    val vs = VarSlot(v, newValueId(v), varName, def)
    phiWeb.lastDef = vs
    phiWeb.defStmt = StmtVarDef(s)
    return addSlot(vs, s)
  }
}

data class VarSlot(val name: String, val valName: String, val varName: VarNameVersion, val def: VarDef, val refs: MutableList<Stmt>? = null)

typealias VarSlots = MutableMap<String,VarSlot>

fun VarSlots.copy(): VarSlots = this.toMutableMap()

data class VarNameVersion(val v: String, val n: Int) {
  override fun toString(): String = if (n == 0) v else if (n == 1) v + "_" else v + "_" + (n-1)
}
class Vars(val versions: MutableMap<String, Int> = mutableMapOf(),
           val vars: MutableMap<VarNameVersion,VarSlot> = mutableMapOf()) {
  fun newVarName(v: String) = VarNameVersion(v, (versions[v] ?: -1) + 1)
  operator fun get(v: String): VarSlot? = vars[VarNameVersion(v, versions[v] ?: 0)]
  operator fun set(v: String, vs: VarSlot) {
    val nextVersion = (versions[v] ?: -1) + 1
    versions[v] = nextVersion
    vars[VarNameVersion(v, nextVersion)] = vs
  }
  fun copy(): Vars = Vars(this.versions.toMutableMap(), this.vars.toMutableMap())
  val values: Collection<VarSlot>
      get() = versions.map { vars[VarNameVersion(it.key, it.value)]!! }
  fun filterValues(p: (VarSlot) -> Boolean) = versions.filter { p(vars[VarNameVersion(it.key, it.value)]!!) }
}

class VarSlotAnalysis(
    private val liveVarsBefore: StmtAnnoMap<Set<String>>,
    private val liveVarsAfter: StmtAnnoMap<Set<String>>) {
  val varInfo =  VarInfo()
  fun procStmt(s: Stmt, vars: Vars) {
    fun addVarRef(name: String, stmt: Stmt, newVarOnly: Boolean) {
      val vs = vars[name]
      if (vs?.refs == null) {
        vars[name] = varInfo.newVarSlot(name, vars.newVarName(name), stmt)
      } else {
        if (!newVarOnly)
          vs.refs.add(stmt)
        else
          fail("AnnAssign")
      }
    }
    when(s) {
      is Assign -> {
        val target = s.target
        when(target) {
          is Name -> addVarRef(target.id, s, false)
          is Subscript -> {}
          is Attribute -> {}
          else -> {}
        }
      }
      is AnnAssign -> {
        when(s.target) {
          is Name -> addVarRef(s.target.id, s, true)
          else -> TODO()
        }
      }
      is AugAssign -> {
        when (s.target) {
          is Name -> {
            val vName = s.target.id
            if (vars[vName] == null) fail("$vName is undefined")
            addVarRef(vName, s, false)
          }
          is Subscript -> {}
          else -> TODO()
        }
      }
      is If -> {
        val bodyVars = vars.copy()
        s.body.forEach { procStmt(it, bodyVars) }
        val newBodyVals = bodyVars.values.map { it.valName }.toSet().minus(vars.values.map { it.valName })
        val newBodyVars = bodyVars.filterValues { it.valName in newBodyVals }
        val liveBodyVarNames = newBodyVars.keys.intersect(liveVarsAfter[s]!!)

        val elseVars = vars.copy()
        s.orelse.forEach { procStmt(it, elseVars) }
        val newElseVals = elseVars.values.map { it.valName }.toSet().minus(vars.values.map { it.valName })
        val newElseVars = elseVars.filterValues { it.valName in newElseVals }
        val liveElseVarNames = newElseVars.keys.intersect(liveVarsAfter[s]!!)

        for (v in liveBodyVarNames.union(liveElseVarNames)) {
          val bodyDef = if (v in liveBodyVarNames) bodyVars[v]!!.def else CopyVarDef(vars[v]!!.def)
          val elseDef = if (v in liveElseVarNames) elseVars[v]!!.def else CopyVarDef(vars[v]!!.def)
          val phiSlot = varInfo.newPhiSlot(v, vars.newVarName(v), s, listOf(bodyDef, elseDef))
          val bodyCopy = if (v !in liveBodyVarNames && v in liveElseVarNames) {
            vars[v]
          } else {
            null
          }
          val elseCopy = if (v in liveBodyVarNames && v !in liveElseVarNames) {
            vars[v]
          } else {
            null
          }
          vars[v] = phiSlot
          if (varInfo.ifAnnoMap[s] == null)
            varInfo.ifAnnoMap[s] = mutableMapOf()
          varInfo.ifAnnoMap[s]!![v] = IfPhiInfo(phiSlot, bodyCopy, elseCopy)
        }
      }
      is While -> {
        val bodyVars = vars.copy()
        s.body.forEach { procStmt(it, bodyVars) }
        val newBodyVals = bodyVars.values.map { it.valName }.toSet().minus(vars.values.map { it.valName })
        val newBodyVars = bodyVars.filterValues { it.valName in newBodyVals }
        // vars live after while or before block body
        val liveVarsAfterBody = liveVarsAfter[s]!!.union(liveVarsBefore[s.body[0]]!!)
        val liveBodyVarNames = newBodyVars.keys.intersect(liveVarsAfterBody)
        for(v in liveBodyVarNames) {
          val before = vars[v]
          if (before == null) {
            fail("$v is undefined before while")
          } else {
            if (before.refs == null) {
              // make copy before while
              val phiSlot = varInfo.newPhiSlot(v, vars.newVarName(v), s, listOf(bodyVars[v]!!.def, before.def))
              vars[v] = phiSlot
              if (varInfo.ifAnnoMap[s] == null)
                varInfo.ifAnnoMap[s] = mutableMapOf()
              varInfo.ifAnnoMap[s]!![v] = IfPhiInfo(phiSlot, null, before)
            }
          }
        }
      }
      is For -> {
        val bodyVars = vars.copy()
        s.body.forEach { procStmt(it, bodyVars) }
        val newBodyVals = bodyVars.values.map { it.valName }.toSet().minus(vars.values.map { it.valName })
        val newBodyVars = bodyVars.filterValues { it.valName in newBodyVals }
        // vars live after while or before block body
        val liveVarsAfterBody = liveVarsAfter[s]!!.union(liveVarsBefore[s.body[0]]!!)
        val liveBodyVarNames = newBodyVars.keys.intersect(liveVarsAfterBody)
        for (v in liveBodyVarNames) {
          val before = vars[v]
          if (before == null) {
            fail("$v is undefined before for")
          } else {
            if (before.refs == null) {
              // make copy before for
              val phiSlot = varInfo.newPhiSlot(v, vars.newVarName(v), s, listOf(bodyVars[v]!!.def, before.def))
              vars[v] = phiSlot
              if (varInfo.ifAnnoMap[s] == null)
                varInfo.ifAnnoMap[s] = mutableMapOf()
              varInfo.ifAnnoMap[s]!![v] = IfPhiInfo(phiSlot, null, before)
            }
          }
        }
      }
      is Return -> { }
      is Expr -> {}
      is Assert -> {}
      is Continue -> {}
      is Break -> {}
      else -> fail("not supported $s")
    }
  }
}

fun varSlotAnalysis(f: FunctionDef): VarInfo {
  val (liveVarsBefore, liveVarsAfter) = liveVarAnalysis(f)
  val args = f.allArgs.map { it.arg }
  val a = VarSlotAnalysis(liveVarsBefore, liveVarsAfter)
  val varInfo = a.varInfo
  val vars = Vars()
  args.forEach {
    vars[it] = varInfo.newParamSlot(it)
  }
  f.body.forEach { a.procStmt(it, vars)}

  varInfo.phiWebMap.values.toSet().forEach {
    val ld = it.lastDef!!

    fun process(vd: VarDef): List<Stmt> = when(vd) {
      is StmtVarDef -> listOf(vd.s)
      is PhiVarDef -> vd.defs.flatMap(::process)
      is ParamVarDef -> emptyList()
      is CopyVarDef -> emptyList()
    }
    it.defs.flatMap(::process).forEach { s ->
      if (varInfo.newSlots[s] == null)
        varInfo.newSlots[s] = mutableMapOf(ld.name to ld)
      else
        varInfo.newSlots[s]!![ld.name] = ld
    }
  }
  varInfo.slots.forEach { vs ->
    vs.refs?.forEach { s ->
      if (varInfo.newSlots[s] != null && vs.name in varInfo.newSlots[s]!!)
        fail("ffff")
      if (varInfo.newSlots[s] == null)
        varInfo.newSlots[s] = mutableMapOf()
      varInfo.newSlots[s]!![vs.name] = vs
    }
  }
  return varInfo
}

fun main() {
  val defs = parseDefs("../eth2.0-specs/tests/fork_choice/defs_test.txt").filterIsInstance<FunctionDef>()
  val funcs = defs//.subList(0,1)

  funcs.forEach {
    println(it.name)
    val varInfo = varSlotAnalysis(desugar(it))

    //varInfo.slots.forEach(::println)
    varInfo.newSlots.values.forEach(::println)
  }
}