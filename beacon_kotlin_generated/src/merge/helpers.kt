package merge

import phase0.Hash32
import pylib.pybool
import ssz.uint64

fun produce_execution_payload(parent_hash: Hash32, timestamp: uint64): ExecutionPayload = TODO()
fun verify_execution_state_transition(payload: ExecutionPayload): pybool = TODO()
fun get_pow_block(block_hash: Hash32): PowBlock = TODO()
fun get_pow_chain_head(): PowBlock = TODO()