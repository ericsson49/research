package onotole.type_inference

import kotlin.random.Random

class WeightedUF {
  val parent = mutableListOf<Int>()
  val sizes = mutableListOf<Int>()
  fun mk(): Int {
    val n = parent.size
    parent.add(n)
    sizes.add(1)
    return n
  }
  fun find(n: Int): Int {
    var p = n
    while (p != parent[p]) {
      parent[p] = parent[parent[p]]
      p = parent[p]
    }
    return p
  }
  fun union(a: Int, b: Int) {
    val ap = find(a)
    val bp = find(b)
    if (ap != bp) {
      if (sizes[ap] < sizes[bp]) {
        parent[ap] = bp
        sizes[bp] += sizes[ap]
      } else {
        parent[bp] = ap
        sizes[ap] += sizes[bp]
      }
    }
  }
}
fun main() {
  val len = 100000
  val t = System.currentTimeMillis()
  for(i in 0 until 100) {
    val uf = WeightedUF()
    for (i in 0 until len) {
      uf.mk()
    }
    val rand = Random(10)
    for(i in 0 until len) {
      uf.union(i, (i+i)%len)
    }
    //val sz = (0 until len).map { it to uf.find(it) }.groupBy { it.second }.mapValues { it.value.map { it.first } }
    //println()
  }
  println(Math.round((System.currentTimeMillis()-t)/1.0))
}
