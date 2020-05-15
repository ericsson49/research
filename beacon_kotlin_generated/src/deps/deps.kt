package deps

import pylib.pyint
import ssz.Bytes
import ssz.Bytes32
import ssz.Bytes48
import ssz.Bytes96
import ssz.boolean

fun hash_tree_root(a: Any): Bytes32 = TODO()
fun hash(a: Any): Bytes32 = TODO()

data class FQ2(val coeffs: Pair<pyint, pyint>)

object bls {
  fun Sign(privkey: pyint, message: Bytes): Bytes96 {
    TODO("Not yet implemented")
  }

  fun Verify(pubkey: Bytes48, message: Bytes, signature: Bytes96): Boolean {
    TODO("Not yet implemented")
  }

  fun Aggregate(signatures: Collection<Bytes96>): Bytes96 {
    TODO("Not yet implemented")
  }

  fun FastAggregateVerify(pubkeys: Collection<Bytes48>, root: Bytes, signature: Bytes96): Boolean {
    TODO("Not yet implemented")
  }

  fun AggregateVerify(pairs: List<Pair<Bytes48, Bytes>>, signature: Bytes96): boolean {
    TODO("Not yet implemented")
  }

  fun signature_to_G2(signature: Bytes96): Triple<FQ2, FQ2, FQ2> {
    TODO("Not yet implemented")
  }
}
