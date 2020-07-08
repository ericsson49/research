package pylib

import ssz.Bitlist
import ssz.Bytes32
import ssz.uint64
import java.lang.Long
import java.math.BigInteger
import java.nio.ByteOrder
import java.util.*
import java.util.stream.StreamSupport
import kotlin.streams.toList

typealias pybytes = Bytes
typealias pybool = Boolean
typealias PyList<T> = MutableList<T>
typealias PyDict<K,V> = MutableMap<K,V>

inline class pyint(val value: BigInteger) {
  constructor(x: uint64) : this(x.toLong().toBigInteger())

  operator fun compareTo(b: pyint) = value.compareTo(b.value)
  operator fun compareTo(b: uint64) = value.compareTo(b.toLong().toBigInteger())
  operator fun rem(b: pyint) = pyint(value.rem(b.value))
  operator fun rem(b: uint64) = rem(pyint(b))
  operator fun div(b: pyint) = pyint(value.div(b.value))
  operator fun div(b: uint64) = rem(pyint(b))
  fun pow(b: uint64) = pyint(value.pow(b.toLong().toInt()))
  fun pow(b: Int) = pyint(value.pow(b))
  operator fun times(b: pyint) = pyint(value.times(b.value))
  operator fun times(b: uint64) = pyint(value.times(b.toLong().toBigInteger()))
  operator fun plus(b: pyint) = pyint(value.plus(b.value))
  operator fun plus(b: uint64) = pyint(value.plus(b.toLong().toBigInteger()))
  operator fun minus(b: pyint) = pyint(value.minus(b.value))
  operator fun minus(b: uint64) = pyint(value.minus(b.toLong().toBigInteger()))
}

data class Tuple2<A : Comparable<A>, B : Comparable<B>>(val a: A, val b: B) : Comparable<Tuple2<A, B>> {
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
fun len(c: pybytes) = c.size().toULong()

fun <T> any(c: Collection<T>) = c.any()
fun all(c: Collection<Boolean>) = c.all { it }

fun sum(c: Collection<ULong>) = run {
  var res: ULong = 0u
  for (e in c) {
    res += e
  }
  res
}

fun sum(c: Iterable<pyint>): pyint = run {
  var res: BigInteger = BigInteger.ZERO
  for (e in c) {
    res += e.value
  }
  return pyint(res)
}

fun range(start: uint64, end: uint64) = start until end
fun range(start: uint64, end: uint64, st: uint64) = start until end step st.toLong()
fun range(n: uint64) = range(0uL, n)

fun max(a: uint64, b: uint64) = kotlin.math.max(a, b)
fun max(a: uint64?, b: uint64) = if (a == null) b else kotlin.math.max(a, b)
fun <T : Comparable<T>> max(c: Iterable<T>): T = c.max()!!
fun <T : Comparable<T>> max(a: T, b: T): T = if (a > b) a else b
fun <T, K : Comparable<K>> max(c: Iterable<T>, key: (T) -> K): T = c.maxBy(key)!!
fun <T, K : Comparable<K>> max(c: Iterable<T>, key: (T) -> K, default: T) = c.maxBy(key) ?: default

fun min(a: uint64, b: uint64) = kotlin.math.min(a, b)
fun <T : Comparable<T>> min(c: Iterable<T>): T = c.min()!!
fun <T, K : Comparable<K>> min(c: Iterable<T>, key: (T) -> K): T = c.minBy(key)!!
fun <T : Comparable<T>> min(a: T, b: T): T = if (a < b) a else b

fun <T> enumerate(c: Collection<T>) = c.mapIndexed { a, b -> Pair(a, b) }

fun <T> Iterable<T>.intersection(b: Iterable<T>) = this.intersect(b)

fun BitSet.slice(from: Int, to: Int) = (from until to).map { this[it] }
fun Bytes.slice(a: uint64, b: uint64) = this.slice(a.toInt(), Math.min(b.toInt(), this.size()) - a.toInt())
fun <T> List<T>.slice(a: uint64, b: uint64) = this.subList(a.toInt(), Math.min(b.toInt(), this.size))
fun <T> MutableList<T>.updateSlice(f: uint64, t: uint64, x: List<T>) {
  for (i in f until t) {
    this[i] = x[i - f]
  }
}

operator fun <T> List<T>.get(index: uint64) = get(index.toInt())
operator fun <A> Pair<A, A>.get(i: uint64) = if (i == 0uL) first else if (i == 1uL) second else throw IllegalArgumentException("bad index " + i)
operator fun pybytes.get(index: uint64) = get(index.toInt())

operator fun <T> MutableList<T>.set(index: uint64, value: T) = set(index.toInt(), value)
operator fun MutableList<Boolean>.set(i: uint64, v: uint64) = this.set(i, pybool(v))

fun <T> Iterable<T>.count(x: T): uint64 {
  return this.filter { it == x }.count().toULong()
}

operator fun Bytes.plus(b: Bytes) = Bytes.concatenate(this, b)

operator fun uint64.unaryMinus(): Int = -this.toInt()

infix fun uint64.shl(a: uint64): uint64 = this.shl(a.toInt())
infix fun Byte.shl(a: uint64): uint64 = this.toULong().and(0xFFuL).shl(a.toInt())
infix fun pybool.shl(a: uint64): uint64 = if (this) 1uL.shl(a) else 0uL

infix fun uint64.shr(a: uint64): uint64 = this.shr(a.toInt())
infix fun Byte.shr(a: uint64): uint64 = this.toULong().and(0xFFuL).shr(a.toInt())
infix fun pybool.shr(a: uint64): uint64 = if (this) 1uL.shr(a) else 0uL

fun pybool(a: uint64) = a != 0uL
fun pybool(a: Boolean) = a

fun <T, U> zip(a: Collection<T>, b: Collection<U>) = a.zip(b)
fun <T, U, V> zip(a: Collection<T>, b: Collection<U>, c: Collection<V>): Collection<Triple<T, U, V>> = a.zip(b).zip(c).map { Triple(it.first.first, it.first.second, it.second) }
fun zip(b1: Bytes32, b2: Bytes32): List<Pair<Byte, Byte>> = b1.toArray().toList().zip(b2.toArray().toList())

fun <T> set(c: Collection<T>) = c.toSet()
fun <T> set() = setOf<T>()
fun <T> list(c: Iterable<T>) = c.toList()
fun <T : Comparable<T>> sorted(c: Iterable<T>) = c.sorted().toMutableList()
fun <T, K : Comparable<K>> sorted(c: Iterable<T>, key: (T) -> K) = c.sortedBy(key)

fun <T> filter(f: (T) -> Boolean, c: Iterable<T>) = c.filter(f)
fun <T, V> map(f: (T) -> V, c: Iterable<T>) = c.map(f)


fun <T> MutableList<T>.append(a: T) {
  this.add(a)
}

operator fun <T> PyList<T>.times(dup: uint64): PyList<T> = List(dup.toInt()) { this }.flatten().toMutableList()

fun <T> List<T>.index(a: T) = this.indexOf(a)

fun <T> List<T>.toPyList(): PyList<T> = this.toMutableList()
fun <T> PyList() = mutableListOf<T>()
fun <T> PyList(vararg elts: T) = mutableListOf(*elts)

fun <K,V> List<Pair<K,V>>.toPyDict(): PyDict<K,V> = this.toMap().toMutableMap()
fun <K,V> PyDict() = mutableMapOf<K,V>()
fun <K,V> PyDict(vararg pairs: Pair<K, V>) = mutableMapOf(*pairs)

fun <K, V> Map<K, V>.keys() = this.keys
fun <K, V> Map<K, V>.items() = this.entries

fun uint64.pow(b: uint64): uint64 = Math.pow(this.toDouble(), b.toDouble()).toULong()
fun uint64.bit_length(): pyint {
  return pyint((64 - Long.numberOfLeadingZeros(this.toLong())).toBigInteger())
}

fun uint64.to_bytes(length: uint64, endiannes: String): pybytes {
  return Bytes.ofUnsignedLong(
    this.toLong(), if (endiannes == "little") ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
  ).slice(0, length.toInt())
}

fun from_bytes(data: pybytes, endiannes: String): uint64 {
  return data.toLong(if (endiannes == "little") ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
    .toULong()
}

fun pyint.to_bytes(length: uint64, endiannes: String): pybytes = {
  val bytes = Bytes.wrap(this.value.toByteArray())
  return if (endiannes == "little") {
    bytes.reverse().slice(0, length.toInt())
  } else {
    bytes.slice(0, length.toInt())
  }
}

operator fun <T> Pair<T, T>.contains(a: T) = this.first == a || this.second == a
fun <T,U> Pair<T,T>.map(f: (T) -> U): List<U> = listOf(f(first), f(second))

fun pybytes(s: String): pybytes = Bytes.fromHexString("0x" + s)
fun pybytes.join(c: Iterable<pybytes>): pybytes = Bytes.concatenate(*Streams.stream(c).toList().toTypedArray())
