package sharding

import phase0.Attestation
import phase0.AttesterSlashing
import phase0.BLSSignature
import phase0.BeaconBlockHeader
import phase0.Checkpoint
import phase0.CommitteeIndex
import phase0.Deposit
import phase0.Eth1Data
import phase0.Fork
import phase0.Gwei
import phase0.LatestMessage
import phase0.PendingAttestation
import phase0.ProposerSlashing
import phase0.Root
import phase0.SignedVoluntaryExit
import phase0.Slot
import phase0.Validator
import phase0.ValidatorIndex
import pylib.PyDict
import ssz.*

typealias Shard = uint64
fun Shard(x: uint64): Shard = x
fun Shard() = Shard(0uL)
typealias BLSCommitment = Bytes48
fun BLSCommitment(x: Bytes48): BLSCommitment = x
fun BLSCommitment() = BLSCommitment(Bytes48())
typealias BLSPoint = uint256
fun BLSPoint(x: uint256): BLSPoint = x
fun BLSPoint() = BLSPoint(uint256())
open class AttestationData(
    var shard_header_root: Root = Root(),
    slot: Slot = Slot(),
    index: CommitteeIndex = CommitteeIndex(),
    beacon_block_root: Root = Root(),
    source: Checkpoint = Checkpoint(),
    target: Checkpoint = Checkpoint()
): phase0.AttestationData(slot = slot, index = index, beacon_block_root = beacon_block_root, source = source, target = target)
open class BeaconBlockBody(
    var shard_proposer_slashings: SSZList<ShardProposerSlashing> = SSZList<ShardProposerSlashing>(),
    var shard_headers: SSZList<SignedShardBlobHeader> = SSZList<SignedShardBlobHeader>(),
    execution_payload: merge.ExecutionPayload = merge.ExecutionPayload(),
    randao_reveal: BLSSignature = BLSSignature(),
    eth1_data: Eth1Data = Eth1Data(),
    graffiti: Bytes32 = Bytes32(),
    proposer_slashings: SSZList<ProposerSlashing> = SSZList<ProposerSlashing>(),
    attester_slashings: SSZList<AttesterSlashing> = SSZList<AttesterSlashing>(),
    attestations: SSZList<Attestation> = SSZList<Attestation>(),
    deposits: SSZList<Deposit> = SSZList<Deposit>(),
    voluntary_exits: SSZList<SignedVoluntaryExit> = SSZList<SignedVoluntaryExit>()
): merge.BeaconBlockBody(execution_payload = execution_payload, randao_reveal = randao_reveal, eth1_data = eth1_data, graffiti = graffiti, proposer_slashings = proposer_slashings, attester_slashings = attester_slashings, attestations = attestations, deposits = deposits, voluntary_exits = voluntary_exits)
open class BeaconState(
    override var previous_epoch_attestations: SSZList<PendingAttestation> = SSZList<PendingAttestation>(),
    override var current_epoch_attestations: SSZList<PendingAttestation> = SSZList<PendingAttestation>(),
    var shard_buffer: SSZVector<SSZList<ShardWork>> = SSZVector<SSZList<ShardWork>>(),
    var shard_gasprice: uint64 = 0uL,
    var current_epoch_start_shard: Shard = Shard(),
    latest_execution_payload_header: merge.ExecutionPayloadHeader = merge.ExecutionPayloadHeader(),
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
    justification_bits: SSZBitvector = SSZBitvector(),
    previous_justified_checkpoint: Checkpoint = Checkpoint(),
    current_justified_checkpoint: Checkpoint = Checkpoint(),
    finalized_checkpoint: Checkpoint = Checkpoint()
): merge.BeaconState(latest_execution_payload_header = latest_execution_payload_header, genesis_time = genesis_time, genesis_validators_root = genesis_validators_root, slot = slot, fork = fork, latest_block_header = latest_block_header, block_roots = block_roots, state_roots = state_roots, historical_roots = historical_roots, eth1_data = eth1_data, eth1_data_votes = eth1_data_votes, eth1_deposit_index = eth1_deposit_index, validators = validators, balances = balances, randao_mixes = randao_mixes, slashings = slashings, previous_epoch_attestations = previous_epoch_attestations, current_epoch_attestations = current_epoch_attestations, justification_bits = justification_bits, previous_justified_checkpoint = previous_justified_checkpoint, current_justified_checkpoint = current_justified_checkpoint, finalized_checkpoint = finalized_checkpoint)
data class DataCommitment(
    var point: BLSCommitment = BLSCommitment(),
    var length: uint64 = 0uL
)
data class ShardBlobBodySummary(
    var commitment: DataCommitment = DataCommitment(),
    var degree_proof: BLSCommitment = BLSCommitment(),
    var data_root: Root = Root(),
    var beacon_block_root: Root = Root()
)
data class ShardBlobHeader(
    var slot: Slot = Slot(),
    var shard: Shard = Shard(),
    var body_summary: ShardBlobBodySummary = ShardBlobBodySummary(),
    var proposer_index: ValidatorIndex = ValidatorIndex()
)
data class SignedShardBlobHeader(
    var message: ShardBlobHeader = ShardBlobHeader(),
    var signature: BLSSignature = BLSSignature()
)
data class PendingShardHeader(
    var commitment: DataCommitment = DataCommitment(),
    var root: Root = Root(),
    var votes: SSZBitlist = SSZBitlist(),
    var weight: Gwei = Gwei(),
    var update_slot: Slot = Slot()
)
data class ShardBlobReference(
    var slot: Slot = Slot(),
    var shard: Shard = Shard(),
    var body_root: Root = Root(),
    var proposer_index: ValidatorIndex = ValidatorIndex()
)
data class SignedShardBlobReference(
    var message: ShardBlobReference = ShardBlobReference(),
    var signature: BLSSignature = BLSSignature()
)
data class ShardProposerSlashing(
    var signed_reference_1: SignedShardBlobReference = SignedShardBlobReference(),
    var signed_reference_2: SignedShardBlobReference = SignedShardBlobReference()
)
sealed class ShardWorkStatus(
    var selector: uint8 = uint8()
)
open class ShardWorkStatus_0(
    var value: Unit = Unit
): ShardWorkStatus(selector = uint8(0uL))
open class ShardWorkStatus_1(
    var value: DataCommitment = DataCommitment()
): ShardWorkStatus(selector = uint8(1uL))
open class ShardWorkStatus_2(
    var value: SSZList<PendingShardHeader> = SSZList<PendingShardHeader>()
): ShardWorkStatus(selector = uint8(2uL))
open class ShardWork(
    var status: ShardWorkStatus = ShardWorkStatus_0()
)