package ssz

import org.apache.tuweni.bytes.Bytes as TuweniBytes
import org.apache.tuweni.bytes.Bytes32 as TuweniBytes32
import org.apache.tuweni.bytes.Bytes48 as TuweniBytes48

typealias SSZObject = Any

typealias boolean = Boolean
typealias uint8 = UByte
typealias uint64 = ULong

typealias Bytes = TuweniBytes
typealias Bytes1 = Byte
typealias Bytes4 = TuweniBytes
typealias Bytes32 = TuweniBytes32
typealias Bytes48 = TuweniBytes48
typealias Bytes96 = TuweniBytes

typealias Bitlist = BooleanArray
typealias CBitlist = List<Boolean>
typealias CBitvector = MutableList<Boolean>
typealias CByteList = List<Byte>
typealias CList<T> = MutableList<T>
typealias CVector<T> = MutableList<T>
typealias CSequence<T> = List<T>
typealias CDict<K, V> = MutableMap<K, V>

typealias Sequence<T> = List<T>
typealias Vector<T> = MutableList<T>

fun Bytes4(): Bytes4 = TuweniBytes.fromHexString("0x00000000")
fun Bytes32(): Bytes32 = Bytes32.ZERO
fun Bytes32(x: List<Byte>): Bytes32 = Bytes32.wrap(x.toByteArray())
fun Bytes48(): Bytes48 = Bytes48.ZERO
fun Bytes96(): Bytes96 = TuweniBytes.concatenate(Bytes48.ZERO, Bytes48.ZERO)

fun CBitlist() = listOf<Boolean>()
fun CBitvector(): CBitvector = mutableListOf<Boolean>()
fun CByteList() = listOf<Byte>()
fun <T> CList() = mutableListOf<T>()
fun <T> CVector() = mutableListOf<T>()
fun <K, V> CDict() = mutableMapOf<K, V>()
