package phase1

import pylib.pyint
import ssz.Bytes32
import ssz.boolean

fun hash_tree_root(a: Any): Root = TODO()
fun hash(a: Any): Bytes32 = TODO()

data class FQ2(val coeffs: Pair<pyint, pyint>)

object bls {
  fun Sign(privkey: Int, message: Root): BLSSignature {
    TODO("Not yet implemented")
  }

  fun Verify(pubkey: BLSPubkey, message: Root, signature: BLSSignature): Boolean {
    TODO("Not yet implemented")
  }

  fun Aggregate(signatures: Collection<BLSSignature>): BLSSignature {
    TODO("Not yet implemented")
  }

  fun FastAggregateVerify(pubkeys: Collection<BLSPubkey>, root: Root, signature: BLSSignature): Boolean {
    TODO("Not yet implemented")
  }

  fun AggregateVerify(pairs: List<Pair<BLSPubkey, Root>>, signature: BLSSignature): boolean {
    TODO("Not yet implemented")
  }

  fun signature_to_G2(signature: BLSSignature): Triple<FQ2, FQ2, FQ2> {
    TODO("Not yet implemented")
  }
}
