package onotole

import kotlin.math.min

fun <N> kosaraju(g: Graph<N>): Map<N,N> {
  val transitions = g.transitions
  val reverse = g.reverse
  return kosaraju(transitions, reverse)
}

fun <N> kosaraju(forward: Map<N, Collection<N>>, backward: Map<N, Collection<N>>): Map<N, N> {
  val visited = mutableSetOf<N>()
  val lst = mutableListOf<N>()
  fun visit(n: N) {
    if (n !in visited) {
      visited.add(n)
      (forward[n] ?: emptyList()).forEach(::visit)
      lst.add(n)
    }
  }
  forward.keys.forEach(::visit)
  val componentOf = mutableMapOf<N, N>()
  fun assign(u: N, root: N) {
    if (u !in componentOf) {
      componentOf[u] = root
      (backward[u] ?: emptyList()).forEach { v -> assign(v, root) }
    }
  }
  lst.reversed().forEach { u -> assign(u, u) }
  return componentOf
}

@OptIn(ExperimentalStdlibApi::class)
fun <N> tarjan(g: Graph<N>) {
  var index = 0
  val stack = mutableListOf<N>()

  val indexOf = mutableMapOf<N,Int>()
  val lowLink = mutableMapOf<N,Int>()
  val onstack = mutableSetOf<N>()

  val components = mutableListOf<MutableList<N>>()
  fun strongConnect(v: N) {
    indexOf[v] = index
    lowLink[v] = index
    index += 1
    stack.add(v)
    onstack.add(v)

    (g.transitions[v] ?: emptyList()).forEach { w ->
      if (w !in indexOf) {
        strongConnect(w)
        lowLink[v] = min(lowLink[v]!!, lowLink[w]!!)
      } else if (w in onstack) {
        lowLink[v] = min(lowLink[v]!!, indexOf[w]!!)
      }
    }

    if (lowLink[v]!! == indexOf[v]!!) {
      val comp = mutableListOf<N>()
      do {
        val w = stack.removeLast()
        onstack.remove(w)
        comp.add(w)
      } while (w != v)
      components.add(comp)
    }
  }

  g.transitions.keys.forEach { v ->
    if (v !in indexOf) {
      strongConnect(v)
    }
  }
}

fun main() {
  val edeges = listOf(0 to 1, 1 to 2, 2 to 0, 3 to 4, 4 to 5, 4 to 6)
  val trans = edeges.groupBy { it.first }.mapValues { it.value.map { it.second } }
  val g = object : Graph<Int> {
    override val transitions: Map<Int,List<Int>> = trans
    override val reverse: Map<Int, List<Int>> = reverse(transitions)
  }

  val res = kosaraju(g)
  res.forEach {
    println(it.key.toString() + " " + it.value)
  }
  val newEdges = edeges.map { res[it.first]!! to res[it.second]!! }.toSet()
  newEdges.forEach {
    println("${it.first} -> ${it.second}")
  }
  tarjan(g)
}