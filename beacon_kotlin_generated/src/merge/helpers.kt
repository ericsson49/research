package merge

import phase0.Hash32
import pylib.pybool
import ssz.uint64

fun produce_execution_payload(parent_hash: Hash32, timestamp: uint64): ExecutionPayload = TODO()
fun verify_execution_state_transition(payload: ExecutionPayload): pybool = TODO()
fun get_pow_block(block_hash: Hash32): PowBlock = TODO()
fun get_pow_chain_head(): PowBlock = TODO()

interface ExecutionEngine {
  fun new_block(executionPayload: ExecutionPayload): Boolean
  fun assemble_block(blockHash: Hash32, timestamp: uint64): ExecutionPayload
}

val EXECUTION_ENGINE = object : ExecutionEngine {
  override fun new_block(executionPayload: ExecutionPayload): Boolean = TODO("Not yet implemented")
  override fun assemble_block(blockHash: Hash32, timestamp: uint64): ExecutionPayload = TODO("Not yet implemented")
}