package sharding

import phase0.Checkpoint
import phase0.CommitteeIndex
import phase0.PendingAttestation
import phase0.Root
import phase0.Slot
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
data class AttestationData(
    var shard_header_root: Root = Root()
): phase0.AttestationData()
data class BeaconBlockBody(
    var shard_proposer_slashings: SSZList<ShardProposerSlashing> = SSZList<ShardProposerSlashing>(),
    var shard_headers: SSZList<SignedShardBlobHeader> = SSZList<SignedShardBlobHeader>()
): merge.BeaconBlockBody()
data class BeaconState(
    override var previous_epoch_attestations: SSZList<PendingAttestation> = SSZList<PendingAttestation>(),
    override var current_epoch_attestations: SSZList<PendingAttestation> = SSZList<PendingAttestation>(),
    var previous_epoch_pending_shard_headers: SSZList<PendingShardHeader> = SSZList<PendingShardHeader>(),
    var current_epoch_pending_shard_headers: SSZList<PendingShardHeader> = SSZList<PendingShardHeader>(),
    var grandparent_epoch_confirmed_commitments: SSZVector<SSZVector<DataCommitment>> = SSZVector<SSZVector<DataCommitment>>(),
    var shard_gasprice: uint64 = 0uL,
    var current_epoch_start_shard: Shard = Shard()
): merge.BeaconState()
data class DataCommitment(
    var point: BLSCommitment = BLSCommitment(),
    var length: uint64 = 0uL
)
data class ShardBlobBodySummary(
    var commitment: DataCommitment = DataCommitment(),
    var degree_proof: BLSCommitment = BLSCommitment(),
    var data_root: phase0.Root = phase0.Root(),
    var beacon_block_root: phase0.Root = phase0.Root()
)
data class ShardBlobHeader(
    var slot: phase0.Slot = phase0.Slot(),
    var shard: Shard = Shard(),
    var body_summary: ShardBlobBodySummary = ShardBlobBodySummary(),
    var proposer_index: phase0.ValidatorIndex = phase0.ValidatorIndex()
)
data class SignedShardBlobHeader(
    var message: ShardBlobHeader = ShardBlobHeader(),
    var signature: phase0.BLSSignature = phase0.BLSSignature()
)
data class PendingShardHeader(
    var slot: phase0.Slot = phase0.Slot(),
    var shard: Shard = Shard(),
    var commitment: DataCommitment = DataCommitment(),
    var root: phase0.Root = phase0.Root(),
    var votes: SSZBitlist = SSZBitlist(),
    var confirmed: boolean = false
)
data class ShardBlobReference(
    var slot: phase0.Slot = phase0.Slot(),
    var shard: Shard = Shard(),
    var body_root: phase0.Root = phase0.Root(),
    var proposer_index: phase0.ValidatorIndex = phase0.ValidatorIndex()
)
data class SignedShardBlobReference(
    var message: ShardBlobReference = ShardBlobReference(),
    var signature: phase0.BLSSignature = phase0.BLSSignature()
)
data class ShardProposerSlashing(
    var signed_reference_1: SignedShardBlobReference = SignedShardBlobReference(),
    var signed_reference_2: SignedShardBlobReference = SignedShardBlobReference()
)
//data class IndexedAttestation(
//    var attesting_indices: SSZList<phase0.ValidatorIndex> = SSZList<phase0.ValidatorIndex>(),
//    var data: AttestationData = AttestationData(),
//    var signature: phase0.BLSSignature = phase0.BLSSignature()
//)
//data class PendingAttestation(
//    var aggregation_bits: SSZBitlist = SSZBitlist(),
//    var data: AttestationData = AttestationData(),
//    var inclusion_delay: phase0.Slot = phase0.Slot(),
//    var proposer_index: phase0.ValidatorIndex = phase0.ValidatorIndex()
//)
//class Attestation(): phase0.Attestation()
//data class BeaconBlock(
//    var slot: phase0.Slot = phase0.Slot(),
//    var proposer_index: phase0.ValidatorIndex = phase0.ValidatorIndex(),
//    var parent_root: phase0.Root = phase0.Root(),
//    var state_root: phase0.Root = phase0.Root(),
//    var body: BeaconBlockBody = BeaconBlockBody()
//)
//data class Store(
//    var time: uint64 = 0uL,
//    var genesis_time: uint64 = 0uL,
//    var justified_checkpoint: phase0.Checkpoint = phase0.Checkpoint(),
//    var finalized_checkpoint: phase0.Checkpoint = phase0.Checkpoint(),
//    var best_justified_checkpoint: phase0.Checkpoint = phase0.Checkpoint(),
//    var blocks: PyDict<phase0.Root, BeaconBlock> = PyDict<phase0.Root, BeaconBlock>(),
//    var block_states: PyDict<phase0.Root, BeaconState> = PyDict<phase0.Root, BeaconState>(),
//    var checkpoint_states: PyDict<phase0.Checkpoint, BeaconState> = PyDict<phase0.Checkpoint, BeaconState>(),
//    var latest_messages: PyDict<phase0.ValidatorIndex, phase0.LatestMessage> = PyDict<phase0.ValidatorIndex, phase0.LatestMessage>()
//)
//data class AttesterSlashing(
//    var attestation_1: IndexedAttestation = IndexedAttestation(),
//    var attestation_2: IndexedAttestation = IndexedAttestation()
//)
//data class AggregateAndProof(
//    var aggregator_index: phase0.ValidatorIndex = phase0.ValidatorIndex(),
//    var aggregate: phase0.Attestation = phase0.Attestation(),
//    var selection_proof: phase0.BLSSignature = phase0.BLSSignature()
//)
//data class SignedBeaconBlock(
//    var message: BeaconBlock = BeaconBlock(),
//    var signature: phase0.BLSSignature = phase0.BLSSignature()
//)
//data class SignedAggregateAndProof(
//    var message: AggregateAndProof = AggregateAndProof(),
//    var signature: phase0.BLSSignature = phase0.BLSSignature()
//)
