package phase0

import ssz.Bytes
import ssz.Bytes1
import ssz.Bytes32
import ssz.Bytes4
import ssz.uint64

fun ValidatorIndex(x: Int): ValidatorIndex = x.toULong()
fun Domain(x: Bytes): Domain = Domain(Bytes32.wrap(x))
fun DomainType(x: String): DomainType = Bytes4.fromHexString(x)
fun Version(x: String): Version = Bytes4.fromHexString(x)
fun Bytes1(x: String): Bytes1 = Bytes.fromHexString(x)[0]

fun <T> List<T>.updateAt(i: uint64, v: T): List<T> = TODO()
fun <T> List<T>.updateSlice(f: uint64, u: uint64, vs: List<T>): List<T> = TODO()