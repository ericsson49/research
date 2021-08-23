package merge

import phase0.Hash32
import pylib.pybool
import ssz.Bytes32
import ssz.uint64
import java.util.function.BinaryOperator

fun produce_execution_payload(parent_hash: Hash32, timestamp: uint64): ExecutionPayload = TODO()
fun verify_execution_state_transition(payload: ExecutionPayload): pybool = TODO()
fun get_pow_block(block_hash: Hash32): PowBlock = TODO()
fun get_pow_chain_head(): PowBlock = TODO()

interface ExecutionEngine {
  fun new_block(executionPayload: ExecutionPayload): Boolean
  fun assemble_block(blockHash: Hash32, timestamp: uint64, random: Bytes32): ExecutionPayload
  fun on_payload(payload: ExecutionPayload): Boolean
}

val EXECUTION_ENGINE = object : ExecutionEngine {
  override fun new_block(executionPayload: ExecutionPayload): Boolean = TODO("Not yet implemented")
  override fun assemble_block(blockHash: Hash32, timestamp: uint64, random: Bytes32): ExecutionPayload = TODO("Not yet implemented")
  override fun on_payload(payload: ExecutionPayload): Boolean = TODO("Not yet implemented")
}