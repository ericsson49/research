package merge

import pylib.PyDict
import phase0.*
import ssz.*

typealias OpaqueTransaction = SSZByteList
fun OpaqueTransaction(x: SSZByteList): OpaqueTransaction = x
fun OpaqueTransaction() = OpaqueTransaction(SSZByteList())
open class BeaconBlockBody(
    var execution_payload: ExecutionPayload = ExecutionPayload()
): phase0.BeaconBlockBody()
open class BeaconState(
    var latest_execution_payload_header: ExecutionPayloadHeader = ExecutionPayloadHeader()
): phase0.BeaconState()
data class ExecutionPayload(
    var block_hash: Hash32 = Hash32(),
    var parent_hash: Hash32 = Hash32(),
    var coinbase: Bytes20 = Bytes20(),
    var state_root: Bytes32 = Bytes32(),
    var number: uint64 = 0uL,
    var gas_limit: uint64 = 0uL,
    var gas_used: uint64 = 0uL,
    var timestamp: uint64 = 0uL,
    var receipt_root: Bytes32 = Bytes32(),
    var logs_bloom: SSZByteVector = SSZByteVector(),
    var transactions: SSZList<OpaqueTransaction> = SSZList<OpaqueTransaction>()
)
data class ExecutionPayloadHeader(
    var block_hash: Hash32 = Hash32(),
    var parent_hash: Hash32 = Hash32(),
    var coinbase: Bytes20 = Bytes20(),
    var state_root: Bytes32 = Bytes32(),
    var number: uint64 = 0uL,
    var gas_limit: uint64 = 0uL,
    var gas_used: uint64 = 0uL,
    var timestamp: uint64 = 0uL,
    var receipt_root: Bytes32 = Bytes32(),
    var logs_bloom: SSZByteVector = SSZByteVector(),
    var transactions_root: Root = Root()
)
data class PowBlock(
    var block_hash: Hash32 = Hash32(),
    var is_processed: boolean = false,
    var is_valid: boolean = false,
    var total_difficulty: uint256 = uint256()
)
data class BeaconBlock(
    var slot: Slot = Slot(),
    var proposer_index: ValidatorIndex = ValidatorIndex(),
    var parent_root: Root = Root(),
    var state_root: Root = Root(),
    var body: BeaconBlockBody = BeaconBlockBody()
)
data class Store(
    var time: uint64 = 0uL,
    var genesis_time: uint64 = 0uL,
    var justified_checkpoint: Checkpoint = Checkpoint(),
    var finalized_checkpoint: Checkpoint = Checkpoint(),
    var best_justified_checkpoint: Checkpoint = Checkpoint(),
    var blocks: PyDict<Root, BeaconBlock> = PyDict<Root, BeaconBlock>(),
    var block_states: PyDict<Root, BeaconState> = PyDict<Root, BeaconState>(),
    var checkpoint_states: PyDict<Checkpoint, BeaconState> = PyDict<Checkpoint, BeaconState>(),
    var latest_messages: PyDict<ValidatorIndex, LatestMessage> = PyDict<ValidatorIndex, LatestMessage>()
)
data class SignedBeaconBlock(
    var message: BeaconBlock = BeaconBlock(),
    var signature: BLSSignature = BLSSignature()
)
