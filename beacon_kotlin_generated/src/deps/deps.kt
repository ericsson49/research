package deps

import phase1.ShardState
import pylib.pyint
import ssz.Bytes
import ssz.Bytes32
import ssz.Bytes48
import ssz.Bytes96
import ssz.boolean

fun hash_tree_root(a: Any): Bytes32 = TODO()
fun hash(a: Any): Bytes32 = TODO()
fun copy(v: phase0.BeaconBlockHeader) = v.copy()
fun copy(v: phase0.BeaconBlock) = v.copy()
fun copy(v: phase0.BeaconState) = v.copy()
fun copy(v: phase1.BeaconBlockHeader) = v.copy()
fun copy(v: phase1.BeaconBlock) = v.copy()
fun copy(v: phase1.BeaconState) = v.copy()
fun copy(v: ShardState) = v.copy()

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

  fun AggregateVerify(pubkyes: List<Bytes48>, messages: List<Bytes>, signature: Bytes96): boolean {
    TODO("Not yet implemented")
  }

  fun signature_to_G2(signature: Bytes96): Triple<FQ2, FQ2, FQ2> {
    TODO("Not yet implemented")
  }
}
