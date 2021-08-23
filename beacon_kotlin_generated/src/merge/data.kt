package merge

import altair.*
import pylib.PyDict
import phase0.*
import ssz.*

typealias OpaqueTransaction = SSZByteList
fun OpaqueTransaction(x: SSZByteList): OpaqueTransaction = x
fun OpaqueTransaction() = OpaqueTransaction(SSZByteList())
typealias Transaction = OpaqueTransaction
fun Transaction(x: OpaqueTransaction): Transaction = x
fun Transaction() = Transaction(OpaqueTransaction())
open class BeaconBlockBody(
    var randao_reveal: BLSSignature = BLSSignature(),
    var eth1_data: Eth1Data = Eth1Data(),
    var graffiti: Bytes32 = Bytes32(),
    var proposer_slashings: SSZList<ProposerSlashing> = SSZList<ProposerSlashing>(),
    var attester_slashings: SSZList<AttesterSlashing> = SSZList<AttesterSlashing>(),
    var attestations: SSZList<Attestation> = SSZList<Attestation>(),
    var deposits: SSZList<Deposit> = SSZList<Deposit>(),
    var voluntary_exits: SSZList<SignedVoluntaryExit> = SSZList<SignedVoluntaryExit>(),
    var sync_aggregate: SyncAggregate = SyncAggregate(),
    var execution_payload: ExecutionPayload = ExecutionPayload()
)
open class BeaconState(
    var genesis_time: uint64 = 0uL,
    var genesis_validators_root: Root = Root(),
    var slot: Slot = Slot(),
    var fork: Fork = Fork(),
    var latest_block_header: BeaconBlockHeader = BeaconBlockHeader(),
    var block_roots: SSZVector<Root> = SSZVector<Root>(),
    var state_roots: SSZVector<Root> = SSZVector<Root>(),
    var historical_roots: SSZList<Root> = SSZList<Root>(),
    var eth1_data: Eth1Data = Eth1Data(),
    var eth1_data_votes: SSZList<Eth1Data> = SSZList<Eth1Data>(),
    var eth1_deposit_index: uint64 = 0uL,
    var validators: SSZList<Validator> = SSZList<Validator>(),
    var balances: SSZList<Gwei> = SSZList<Gwei>(),
    var randao_mixes: SSZVector<Bytes32> = SSZVector<Bytes32>(),
    var slashings: SSZVector<Gwei> = SSZVector<Gwei>(),
    var previous_epoch_participation: SSZList<ParticipationFlags> = SSZList<ParticipationFlags>(),
    var current_epoch_participation: SSZList<ParticipationFlags> = SSZList<ParticipationFlags>(),
    var justification_bits: SSZBitvector = SSZBitvector(),
    var previous_justified_checkpoint: Checkpoint = Checkpoint(),
    var current_justified_checkpoint: Checkpoint = Checkpoint(),
    var finalized_checkpoint: Checkpoint = Checkpoint(),
    var inactivity_scores: SSZList<uint64> = SSZList<uint64>(),
    var current_sync_committee: SyncCommittee = SyncCommittee(),
    var next_sync_committee: SyncCommittee = SyncCommittee(),
    var latest_execution_payload_header: ExecutionPayloadHeader = ExecutionPayloadHeader()
)
open class ExecutionPayload(
    var parent_hash: Hash32 = Hash32(),
    var coinbase: Bytes20 = Bytes20(),
    var state_root: Bytes32 = Bytes32(),
    var receipt_root: Bytes32 = Bytes32(),
    var logs_bloom: SSZByteVector = SSZByteVector(),
    var random: Bytes32 = Bytes32(),
    var block_number: uint64 = 0uL,
    var gas_limit: uint64 = 0uL,
    var gas_used: uint64 = 0uL,
    var timestamp: uint64 = 0uL,
    var base_fee_per_gas: Bytes32 = Bytes32(),
    var block_hash: Hash32 = Hash32(),
    var transactions: SSZList<Transaction> = SSZList<Transaction>()
)
open class ExecutionPayloadHeader(
    var parent_hash: Hash32 = Hash32(),
    var coinbase: Bytes20 = Bytes20(),
    var state_root: Bytes32 = Bytes32(),
    var receipt_root: Bytes32 = Bytes32(),
    var logs_bloom: SSZByteVector = SSZByteVector(),
    var random: Bytes32 = Bytes32(),
    var block_number: uint64 = 0uL,
    var gas_limit: uint64 = 0uL,
    var gas_used: uint64 = 0uL,
    var timestamp: uint64 = 0uL,
    var base_fee_per_gas: Bytes32 = Bytes32(),
    var block_hash: Hash32 = Hash32(),
    var transactions_root: Root = Root()
)
open class TransitionStore(
    var transition_total_difficulty: uint256 = uint256()
)
open class PowBlock(
    var block_hash: Hash32 = Hash32(),
    var parent_hash: Hash32 = Hash32(),
    var is_processed: boolean = false,
    var is_valid: boolean = false,
    var total_difficulty: uint256 = uint256(),
    var difficulty: uint256 = uint256()
)
open class BeaconBlock(
    var slot: Slot = Slot(),
    var proposer_index: ValidatorIndex = ValidatorIndex(),
    var parent_root: Root = Root(),
    var state_root: Root = Root(),
    var body: BeaconBlockBody = BeaconBlockBody()
)
open class Store(
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
open class SignedBeaconBlock(
    var message: BeaconBlock = BeaconBlock(),
    var signature: BLSSignature = BLSSignature()
)
