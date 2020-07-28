package ssz

import pylib.pybytes
import pylib.pyint
import org.apache.tuweni.bytes.Bytes as TuweniBytes
import org.apache.tuweni.bytes.Bytes32 as TuweniBytes32
import org.apache.tuweni.bytes.Bytes48 as TuweniBytes48

typealias SSZObject = Any

typealias bit = Boolean
typealias boolean = Boolean
typealias uint8 = UByte
typealias uint32 = UInt
typealias uint64 = ULong

typealias Bytes = TuweniBytes
typealias Bytes1 = Byte
typealias Bytes4 = TuweniBytes
typealias Bytes32 = TuweniBytes32
typealias Bytes48 = TuweniBytes48
typealias Bytes96 = TuweniBytes

typealias Bitlist = BooleanArray
typealias SSZBitlist = List<Boolean>
typealias SSZBitvector = MutableList<Boolean>
typealias SSZByteList = List<Byte>
typealias SSZByteVector = List<Byte>
typealias SSZList<T> = MutableList<T>
typealias SSZVector<T> = MutableList<T>
typealias CSequence<T> = List<T>
typealias SSZDict<K, V> = MutableMap<K, V>

typealias Sequence<T> = List<T>
typealias Vector<T> = MutableList<T>

fun uint8(v: pyint): uint8 = v.value.toByte().toUByte()
fun uint8(v: uint64): uint8 = v.toUByte()
fun uint32(v: pyint): uint32 = v.value.toInt().toUInt()
fun uint32(v: uint64): uint32 = v.toUInt()
fun uint64(v: pyint): uint64 = v.value.toLong().toULong()
fun uint64(v: ULong): uint64 = v
fun Bytes4(): Bytes4 = TuweniBytes.fromHexString("0x00000000")
fun Bytes32(): Bytes32 = Bytes32.ZERO
fun Bytes32(x: List<Byte>): Bytes32 = Bytes32.wrap(x.toByteArray())
fun Bytes48(): Bytes48 = Bytes48.ZERO
fun Bytes96(): Bytes96 = TuweniBytes.concatenate(Bytes48.ZERO, Bytes48.ZERO)

fun SSZBitlist() = listOf<Boolean>()
fun SSZBitvector(): SSZBitvector = mutableListOf<Boolean>()
fun SSZByteList() = listOf<Byte>()
fun SSZByteVector() = listOf<Byte>()
fun <T> SSZList() = mutableListOf<T>()
fun <T> SSZList(vararg elts: T) = mutableListOf<T>(*elts)
inline fun <reified T> SSZList(elts: Collection<T>) = SSZList(*elts.toTypedArray())
fun <T> SSZVector() = mutableListOf<T>()
fun <K, V> SSZDict() = mutableMapOf<K, V>()

interface TreeNode {
  fun get_left(): TreeNode
  fun get_right(): TreeNode
  fun merkle_root(): Bytes32
}

fun SSZByteList.get_backing(): TreeNode = TODO()

fun SSZByteList.toPyBytes(): pybytes = TODO()

fun uint_to_bytes(b: uint8): pybytes = TODO()
fun uint_to_bytes(b: uint32): pybytes = TODO()
fun uint_to_bytes(b: uint64): pybytes = TODO()