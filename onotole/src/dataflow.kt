typealias FlowFun = (VarSet) -> VarSet

sealed class FFunc<T>
class IdentityFFunc<T>: FFunc<T>()
data class ConstFFunc<T>(val c: T): FFunc<T>()
data class RealFFunc<T>(val f: (T) -> T): FFunc<T>()

data class DFAProblem<N,T>(
    val bottom: () -> T,
    val confluence: (T,T) -> T,
    val deps: Map<N,Collection<Pair<N, FFunc<T>>>>,
    val init: ((N) -> T?)? = null
) {
  fun confluence(c: Collection<T>): T = c.fold(bottom(), confluence)
}

operator fun <T> FFunc<T>.invoke(p: T): T = when(this) {
  is IdentityFFunc -> p
  is ConstFFunc -> c
  is RealFFunc -> f(p)
}

fun <N,T> solve(p: DFAProblem<N,T>): Map<N,T> {
  fun init(n: N): T = p.init?.invoke(n) ?: p.bottom()
  val results = p.deps.mapValues { init(it.key) }.toMutableMap()
  var changed = true
  while (changed) {
    changed = false
    for ((n, deps) in p.deps) {
      val res = p.confluence(deps.map { (m: N, f: FFunc<T>) -> f(results.getOrPut(m) { init(m) }) })
      if (results[n] != res) {
        changed = true
        results[n] = res
      }
    }
  }
  return results.toMap()
}

class CFG<T> {
  private var completed = false
  val transitions = mutableMapOf<T,List<T>>()
  private var _labeling: Map<T,String>? = null
  val labeling: Map<T,String>
    get() {
      if (!completed)
        fail("should be completed first")
      if (_labeling == null) {
        val res = mutableMapOf<T,String>()
        transitions.toList().reversed().forEach {
          res[it.first] = "s_" + res.size
        }
        _labeling = res.toMap()
      }
      return _labeling!!
    }
  private var _reverse: Map<T,List<T>>? = null
  val reverse: Map<T,List<T>>
    get() {
      if (!completed)
        fail("should be completed first")
      if (_reverse == null) {
        val res = mutableMapOf<T,MutableSet<T>>()
        transitions.toList().reversed().forEach { (n, succ) ->
          succ.forEach { m -> res.getOrPut(m, ::mutableSetOf).add(n) }
        }
        _reverse = res.mapValues { it.value.toList() }
      }
      return _reverse!!
    }
  fun addInstr(i: T, next: T) = addInstr(i, listOf(next))
  fun addInstr(i: T, next: List<T>): T {
    transitions[i] = next
    return i
  }
  fun complete() { completed = true }
}

fun <T, U, A> List<T>.foldRightWithAcc(init: U, acc: A, f: (T, U, A) -> U): U {
  return this.foldRight(init, { a,b -> f(a, b, acc) })
}

interface NodeProcessor<T> {
  fun enterInstr(cfg: CFG<T>): T
  fun assignInstr(cfg: CFG<T>, targets: Set<identifier>, refs: Set<String>): T
  fun branchInstr(cfg: CFG<T>, refs: Set<String>): T
  fun exitInstr(cfg: CFG<T>): T
}

class CFGBuilder<T>(val proc: NodeProcessor<T>) {
  val cfg = CFG<T>()
  val tmpVars = mutableSetOf<String>()
  companion object {
    fun <T> makeCFG(f: FunctionDef, proc: NodeProcessor<T>): CFG<T> {
      val builder = CFGBuilder(proc)
      val cfg = builder.cfg
      val exit = builder.cfg.addInstr(proc.exitInstr(builder.cfg), emptyList())
      val first = builder.processStmts(f.body, exit)
      val pre = cfg.addInstr(proc.assignInstr(cfg, f.args.args.map { it.arg }.toSet(), emptySet()), first)
      cfg.addInstr(proc.enterInstr(cfg), pre)
      cfg.complete()
      return cfg
    }
  }

  fun processStmt(s: Stmt, next: T): T {
    return when(s) {
      is Assign -> {
        if (s.targets.size != 1)
          fail("unsupported")
        val target = s.targets[0]
        val vars = getVarNamesInStoreCtx(target).toSet()
        val refs = liveVarAnalysis(s.value)
        cfg.addInstr(proc.assignInstr(cfg, vars, refs), next)
      }
      is AnnAssign -> {
        val target = s.target
        val vars = getVarNamesInStoreCtx(target).toSet()
        val refs = liveVarAnalysis(s.value!!)
        cfg.addInstr(proc.assignInstr(cfg, vars, refs), next)
      }
      is AugAssign -> {
        val target = s.target
        val vars = getVarNamesInStoreCtx(target).toSet()
        val refs = liveVarAnalysis(s.value).plus(vars)
        cfg.addInstr(proc.assignInstr(cfg, vars, refs), next)
      }
      is If -> {
        val elseBegin = processStmts(s.orelse, next)
        val bodyBegin = processStmts(s.body, next)
        cfg.addInstr(proc.branchInstr(cfg, liveVarAnalysis(s.test)), listOf(bodyBegin, elseBegin))
      }
      is While -> {
        val whileHead = proc.branchInstr(cfg, liveVarAnalysis(s.test))
        val bodyBegin = processStmts(s.body, whileHead)
        cfg.addInstr(whileHead, listOf(bodyBegin, next))
      }
      is For -> {
        val newTmpVar = "tmp_" + tmpVars.size
        tmpVars.add(newTmpVar)
        val preHead = proc.assignInstr(cfg, setOf(newTmpVar), liveVarAnalysis(s.iter))
        val forHead = proc.branchInstr(cfg, setOf(newTmpVar))
        val bodyBegin = processStmts(s.body, forHead)
        if (s.target !is Name)
          fail("not implemented yet")
        val preBody = cfg.addInstr(proc.assignInstr(cfg, setOf(s.target.id), setOf(newTmpVar)), bodyBegin)
        cfg.addInstr(forHead, listOf(preBody, next))
        cfg.addInstr(preHead, forHead)
      }
      is Return -> {
        s.value?.let { cfg.addInstr(proc.assignInstr(cfg, setOf("<return>"), liveVarAnalysis(it)), next) } ?: next
      }
      else -> fail("not implemented $s")
    }
  }
  fun processStmts(c: List<Stmt>, next: T) = c.foldRight(next, ::processStmt)
}

fun <T> makeGenKill(gen: Set<T>, kill: Set<T>): RealFFunc<Set<T>> {
  return RealFFunc { out -> (out - kill) + gen }
}
fun <N,T> makeSetDFAProblem(
    cfg: CFG<N>,
    forward: Boolean,
    must: Boolean,
    init: () -> Set<T>,
    genKillMaker: (N) -> Pair<Set<T>,Set<T>>
): DFAProblem<Pair<N,String>,Set<T>> {
  val deps = mutableMapOf<Pair<N,String>, Collection<Pair<Pair<N,String>, FFunc<Set<T>>>>>()
  cfg.transitions.keys.forEach { n ->
    val ds = (if (forward) cfg.reverse else cfg.transitions)[n] ?: emptyList()
    val (c1,c2) = if (forward) Pair("in", "out") else Pair("out", "in")
    deps[Pair(n, c1)] = ds.map { Pair(it, c2) to IdentityFFunc<Set<T>>() }
    val (gen, kill) = genKillMaker(n)
    deps[Pair(n, c2)] = listOf(Pair(n, c1) to makeGenKill(gen, kill))
  }
  return if (must) {
    DFAProblem(init, { a,b -> a.intersect(b) }, deps)
  } else {
    DFAProblem(init, { a,b -> a.union(b) }, deps)
  }
}
