package merge

import pylib.PyDict
import phase0.*
import ssz.*

typealias OpaqueTransaction = SSZByteList
fun OpaqueTransaction(x: SSZByteList): OpaqueTransaction = x
fun OpaqueTransaction() = OpaqueTransaction(SSZByteList())
open class BeaconBlockBody(
    var execution_payload: ExecutionPayload = ExecutionPayload(),
    randao_reveal: BLSSignature = BLSSignature(),
    eth1_data: Eth1Data = Eth1Data(),
    graffiti: Bytes32 = Bytes32(),
    proposer_slashings: SSZList<ProposerSlashing> = SSZList<ProposerSlashing>(),
    attester_slashings: SSZList<AttesterSlashing> = SSZList<AttesterSlashing>(),
    attestations: SSZList<Attestation> = SSZList<Attestation>(),
    deposits: SSZList<Deposit> = SSZList<Deposit>(),
    voluntary_exits: SSZList<SignedVoluntaryExit> = SSZList<SignedVoluntaryExit>()
): phase0.BeaconBlockBody(randao_reveal = randao_reveal, eth1_data = eth1_data, graffiti = graffiti, proposer_slashings = proposer_slashings, attester_slashings = attester_slashings, attestations = attestations, deposits = deposits, voluntary_exits = voluntary_exits)
open class BeaconState(
    var latest_execution_payload_header: ExecutionPayloadHeader = ExecutionPayloadHeader(),
    genesis_time: uint64 = 0uL,
    genesis_validators_root: Root = Root(),
    slot: Slot = Slot(),
    fork: Fork = Fork(),
    latest_block_header: BeaconBlockHeader = BeaconBlockHeader(),
    block_roots: SSZVector<Root> = SSZVector<Root>(),
    state_roots: SSZVector<Root> = SSZVector<Root>(),
    historical_roots: SSZList<Root> = SSZList<Root>(),
    eth1_data: Eth1Data = Eth1Data(),
    eth1_data_votes: SSZList<Eth1Data> = SSZList<Eth1Data>(),
    eth1_deposit_index: uint64 = 0uL,
    validators: SSZList<Validator> = SSZList<Validator>(),
    balances: SSZList<Gwei> = SSZList<Gwei>(),
    randao_mixes: SSZVector<Bytes32> = SSZVector<Bytes32>(),
    slashings: SSZVector<Gwei> = SSZVector<Gwei>(),
    previous_epoch_attestations: SSZList<PendingAttestation> = SSZList<PendingAttestation>(),
    current_epoch_attestations: SSZList<PendingAttestation> = SSZList<PendingAttestation>(),
    justification_bits: SSZBitvector = SSZBitvector(),
    previous_justified_checkpoint: Checkpoint = Checkpoint(),
    current_justified_checkpoint: Checkpoint = Checkpoint(),
    finalized_checkpoint: Checkpoint = Checkpoint()
): phase0.BeaconState(genesis_time = genesis_time, genesis_validators_root = genesis_validators_root, slot = slot, fork = fork, latest_block_header = latest_block_header, block_roots = block_roots, state_roots = state_roots, historical_roots = historical_roots, eth1_data = eth1_data, eth1_data_votes = eth1_data_votes, eth1_deposit_index = eth1_deposit_index, validators = validators, balances = balances, randao_mixes = randao_mixes, slashings = slashings, previous_epoch_attestations = previous_epoch_attestations, current_epoch_attestations = current_epoch_attestations, justification_bits = justification_bits, previous_justified_checkpoint = previous_justified_checkpoint, current_justified_checkpoint = current_justified_checkpoint, finalized_checkpoint = finalized_checkpoint)
open class ExecutionPayload(
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
open class ExecutionPayloadHeader(
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
open class TransitionStore(
    var transition_total_difficulty: uint256 = uint256()
)
open class PowBlock(
    var block_hash: Hash32 = Hash32(),
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
