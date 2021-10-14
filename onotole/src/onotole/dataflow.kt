package onotole

import java.util.*

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

fun <K,V> reverse(deps: Map<K,Collection<V>>): Map<V,List<K>> {
  val res = mutableMapOf<V,MutableSet<K>>()
  deps.forEach { (n, succ) ->
    succ.forEach { m -> res.getOrPut(m, ::mutableSetOf).add(n) }
  }
  return res.mapValues { it.value.toList() }
}

interface Graph<T> {
  val transitions: Map<T,List<T>>
  val reverse: Map<T,List<T>>
}

interface CFGraph<L,N: IInstr>: Graph<L> {
  fun get(l: L): N
}

class CFG<T: IInstr>(override val transitions: Map<T,List<T>> = emptyMap()): CFGraph<T, T> {
  override val reverse: Map<T,List<T>> = reverse(transitions)
  override fun get(l: T) = l
  val first: T = transitions.keys.minus(reverse.keys).toList()[0]
  val labeling: Map<T,String> = transitions.toList().reversed().mapIndexed { i, it -> it.first to "s_$i" }.toMap()
}

fun <T, U, A> List<T>.foldRightWithAcc(init: U, acc: A, f: (T, U, A) -> U): U {
  return this.foldRight(init) { a, b -> f(a, b, acc) }
}

interface NodeProcessor<T> {
  fun mkInstr(targets: Set<identifier> = emptySet(), refs: Set<String> = emptySet()): T
}

sealed class EdgeSource
data class BlockIndex(val block: List<Stmt>, val index: Int): EdgeSource() {
  override fun equals(other: Any?): Boolean {
    return this === other || other is BlockIndex && block === other.block && index == other.index
  }
}
class Undef: EdgeSource()

class CFGBuilder<T: IInstr>(val proc: NodeProcessor<T>) {
  val transitions = mutableMapOf<T,List<T>>()
  val freshNames = FreshNames()
  val stmtToInstrMap = IdentityHashMap<Stmt,Pair<T,T>>()
  val edgeLabeling = mutableMapOf<Pair<T,T>,EdgeSource>()
  val loopTargets = mutableListOf<Pair<T,T>>()
  companion object {
    fun <T: IInstr> makeCFG(f: FunctionDef, proc: NodeProcessor<T>): Triple<CFGraph<T,T>,Map<Stmt,Pair<T,T>>,Map<Pair<T,T>,EdgeSource>> {
      val builder = CFGBuilder(proc)
      val transitions = builder.transitions
      val exit = proc.mkInstr()
      transitions[exit] = emptyList()
      val first = builder.processStmts(f.body, exit)
      val pre = builder.addInstr(proc.mkInstr(f.args.args.map { it.arg }.toSet(), emptySet()), first, BlockIndex(f.body, 0))
      val entry = proc.mkInstr()
      transitions[entry] = listOf(pre)
      return Triple(CFG(transitions), builder.stmtToInstrMap, builder.edgeLabeling)
    }
  }

  fun addInstr(i: T, next: T, l: EdgeSource): T {
    transitions[i] = listOf(next)
    edgeLabeling[i to next] = l
    return i
  }

  fun addInstr(i: T, next: List<T>, labels: List<EdgeSource>): T {
    transitions[i] = next
    next.zip(labels).forEach { (n, l) ->
      edgeLabeling[i to n] = l
    }
    return i
  }

  fun pushLoop(head: T, exit: T) {
    loopTargets.add(Pair(head, exit))
  }
  fun popLoop() {
    loopTargets.removeAt(loopTargets.size-1)
  }

  fun processStmt(block: List<Stmt>, i: Int, next: T): T {
    val s = block[i]
    val index = BlockIndex(block,i+1)
    val res = when(s) {
      is Expr -> {
        addInstr(proc.mkInstr(emptySet(), liveVarAnalysis(s.value)), next, index)
      }
      is Assert -> {
        val refs = liveVarAnalysis(s.test).plus(s.msg?.let(::liveVarAnalysis) ?: emptySet())
        addInstr(proc.mkInstr(emptySet(), refs), next, index)
      }
      is Assign -> {
        val target = s.target
        val vars = getVarNamesInStoreCtx(target).toSet()
        val refs = liveVarAnalysis(s.value).plus(getVarNamesInLoadCtx(target))
        addInstr(proc.mkInstr(vars, refs), next, index)
      }
      is AnnAssign -> {
        val target = s.target
        val vars = getVarNamesInStoreCtx(target).toSet()
        val refs = liveVarAnalysis(s.value!!).plus(getVarNamesInLoadCtx(target))
        addInstr(proc.mkInstr(vars, refs), next, index)
      }
      is AugAssign -> {
        val target = s.target
        val vars = getVarNamesInStoreCtx(target).toSet()
        val refs = liveVarAnalysis(s.value).plus(getVarNamesInLoadCtx(target)).plus(vars)
        addInstr(proc.mkInstr(vars, refs), next, index)
      }
      is If -> {
        val elseBegin = processStmts(s.orelse, next)
        val bodyBegin = processStmts(s.body, next)
        addInstr(
            proc.mkInstr(targets = emptySet(), refs = liveVarAnalysis(s.test)),
            listOf(bodyBegin, elseBegin), listOf(BlockIndex(s.body, 0), BlockIndex(s.orelse, 0)))
      }
      is While -> {
        val whileHead = proc.mkInstr(targets = emptySet(), refs = liveVarAnalysis(s.test))
        pushLoop(whileHead, next)
        val bodyBegin = processStmts(s.body, whileHead)
        popLoop()
        addInstr(whileHead, listOf(bodyBegin, next), listOf(BlockIndex(s.body, 0), BlockIndex(block, i+1)))
      }
      is For -> {
        val newTmpVar = freshNames.fresh()
        val preHead = proc.mkInstr(setOf(newTmpVar), liveVarAnalysis(s.iter))
        val forHead = proc.mkInstr(targets = emptySet(), refs = setOf(newTmpVar))
        pushLoop(forHead, next)
        val bodyBegin = processStmts(s.body, forHead)
        popLoop()
        if (!(s.target is Name || s.target is Tuple && s.target.elts.all { it is Name }))
          fail("not implemented yet")
        val names = getVarNamesInStoreCtx(s.target)
        val preBody = addInstr(proc.mkInstr(names.toSet(), setOf(newTmpVar)), bodyBegin, /*Undef()*/BlockIndex(s.body, 0))
        addInstr(forHead, listOf(preBody, next), listOf(/*BlockIndex(s.body, 0)*/Undef(), BlockIndex(block, i+1)))
        addInstr(preHead, forHead, BlockIndex(block, i))
      }
      is Return -> {
        s.value?.let { addInstr(proc.mkInstr(setOf("<return>"), liveVarAnalysis(it)), next, BlockIndex(block, i)) } ?: next
      }
      is Continue -> {
        loopTargets[loopTargets.size-1].first
      }
      is Break -> {
        loopTargets[loopTargets.size-1].second
      }
      else -> fail("not implemented $s")
    }
    stmtToInstrMap[s] = Pair(res, next)
    return res
  }

  fun processStmts(block: List<Stmt>, next: T): T {
    var currNext = next
    block.indices.reversed().forEach { i ->
      currNext = processStmt(block, i, currNext)
    }
    return currNext
  }
}

fun <T> makeGenKill(gen: Set<T>, kill: Set<T>): RealFFunc<Set<T>> {
  return RealFFunc { out -> (out - kill) + gen }
}
fun <N,T> makeSetDFAProblem(
    cfg: Graph<N>,
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
