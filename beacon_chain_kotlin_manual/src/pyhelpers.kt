import org.apache.tuweni.bytes.Bytes
import java.util.*

data class Tuple2<A: Comparable<A>,B: Comparable<B>>(val a: A, val b: B): Comparable<Tuple2<A, B>> {
  override fun compareTo(other: Tuple2<A, B>): Int {
    val res = a.compareTo(other.a)
    return if (res == 0) {
      b.compareTo(other = b)
    } else {
      res
    }
  }
}

fun <T> len(c: Collection<T>) = c.size.toULong()
fun len(c: BitSet) = c.size().toLong()
fun len(c: Bitlist) = c.size

fun range(start: uint32, end: uint32) = start until end
fun range(n: uint32) = range(0u, n)
fun range(start: uint64, end: uint64) = start until end
fun range(n: uint64) = range(0uL, n)

fun max(a: uint64, b: uint64) = kotlin.math.max(a, b)
fun max(a: uint64?, b: uint64) = if (a == null) b else kotlin.math.max(a, b)
fun min(a: uint64, b: uint64) = kotlin.math.min(a, b)

fun <T> enumerate(c: List<T>): List<Pair<Int, T>> = c.mapIndexed { i, t ->  Pair(i, t) }
fun all(c: Collection<Boolean>) = c.all { it }

fun BitSet.slice(from: Int, to: Int) = (from until to).map { this[it] }

operator fun Bytes.plus(b: Bytes) = Bytes.concatenate(this, b)

operator fun <T> List<T>.get(index: uint64) = get(index.toInt())
operator fun <T> MutableList<T>.get(index: uint64) = get(index.toInt())
operator fun <T> MutableList<T>.set(index: uint64, value: T) = set(index.toInt(), value)
operator fun bytes.get(index: uint32) = get(index.toInt())
operator fun bytes.get(index: uint64) = get(index.toInt())
