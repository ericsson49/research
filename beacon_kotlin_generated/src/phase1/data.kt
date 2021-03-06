package phase1

import ssz.Bytes32
import ssz.Bytes4
import ssz.Bytes48
import ssz.Bytes96
import ssz.SSZBitlist
import ssz.SSZBitvector
import ssz.SSZByteList
import ssz.SSZByteVector
import ssz.SSZDict
import ssz.SSZList
import ssz.SSZVector
import ssz.boolean
import ssz.uint64
import ssz.uint8

typealias Slot = uint64

fun Slot(x: uint64): Slot = x
fun Slot() = Slot(0uL)

typealias Epoch = uint64

fun Epoch(x: uint64): Epoch = x
fun Epoch() = Epoch(0uL)

typealias CommitteeIndex = uint64

fun CommitteeIndex(x: uint64): CommitteeIndex = x
fun CommitteeIndex() = CommitteeIndex(0uL)

typealias ValidatorIndex = uint64

fun ValidatorIndex(x: uint64): ValidatorIndex = x
fun ValidatorIndex() = ValidatorIndex(0uL)

typealias Gwei = uint64

fun Gwei(x: uint64): Gwei = x
fun Gwei() = Gwei(0uL)

typealias Root = Bytes32

fun Root(x: Bytes32): Root = x
fun Root() = Root(Bytes32())

typealias Version = Bytes4

fun Version(x: Bytes4): Version = x
fun Version() = Version(Bytes4())

typealias DomainType = Bytes4

fun DomainType(x: Bytes4): DomainType = x
fun DomainType() = DomainType(Bytes4())

typealias ForkDigest = Bytes4

fun ForkDigest(x: Bytes4): ForkDigest = x
fun ForkDigest() = ForkDigest(Bytes4())

typealias Domain = Bytes32

fun Domain(x: Bytes32): Domain = x
fun Domain() = Domain(Bytes32())

typealias BLSPubkey = Bytes48

fun BLSPubkey(x: Bytes48): BLSPubkey = x
fun BLSPubkey() = BLSPubkey(Bytes48())

typealias BLSSignature = Bytes96

fun BLSSignature(x: Bytes96): BLSSignature = x
fun BLSSignature() = BLSSignature(Bytes96())

typealias Shard = uint64

fun Shard(x: uint64): Shard = x
fun Shard() = Shard(0uL)

typealias OnlineEpochs = uint8

fun OnlineEpochs(x: uint8): OnlineEpochs = x
fun OnlineEpochs() = OnlineEpochs(0u.toUByte())

data class Fork(
    var previous_version: Version = Version(),
    var current_version: Version = Version(),
    var epoch: Epoch = Epoch()
)

data class ForkData(
    var current_version: Version = Version(),
    var genesis_validators_root: Root = Root()
)

data class Checkpoint(
    var epoch: Epoch = Epoch(),
    var root: Root = Root()
)

data class Validator(
    var pubkey: BLSPubkey = BLSPubkey(),
    var withdrawal_credentials: Bytes32 = Bytes32(),
    var effective_balance: Gwei = Gwei(),
    var slashed: boolean = false,
    var activation_eligibility_epoch: Epoch = Epoch(),
    var activation_epoch: Epoch = Epoch(),
    var exit_epoch: Epoch = Epoch(),
    var withdrawable_epoch: Epoch = Epoch(),
    var next_custody_secret_to_reveal: uint64 = 0uL,
    var all_custody_secrets_revealed_epoch: Epoch = Epoch()
)

data class AttestationData(
    var slot: Slot = Slot(),
    var index: CommitteeIndex = CommitteeIndex(),
    var beacon_block_root: Root = Root(),
    var source: Checkpoint = Checkpoint(),
    var target: Checkpoint = Checkpoint(),
    var shard: Shard = Shard(),
    var shard_head_root: Root = Root(),
    var shard_transition_root: Root = Root()
)

data class PendingAttestation(
    var aggregation_bits: SSZBitlist = SSZBitlist(),
    var data: AttestationData = AttestationData(),
    var inclusion_delay: Slot = Slot(),
    var proposer_index: ValidatorIndex = ValidatorIndex(),
    var crosslink_success: boolean = false
)

data class Eth1Data(
    var deposit_root: Root = Root(),
    var deposit_count: uint64 = 0uL,
    var block_hash: Bytes32 = Bytes32()
)

data class HistoricalBatch(
    var block_roots: SSZVector<Root> = SSZVector<Root>(),
    var state_roots: SSZVector<Root> = SSZVector<Root>()
)

data class DepositMessage(
    var pubkey: BLSPubkey = BLSPubkey(),
    var withdrawal_credentials: Bytes32 = Bytes32(),
    var amount: Gwei = Gwei()
)

data class DepositData(
    var pubkey: BLSPubkey = BLSPubkey(),
    var withdrawal_credentials: Bytes32 = Bytes32(),
    var amount: Gwei = Gwei(),
    var signature: BLSSignature = BLSSignature()
)

data class BeaconBlockHeader(
    var slot: Slot = Slot(),
    var proposer_index: ValidatorIndex = ValidatorIndex(),
    var parent_root: Root = Root(),
    var state_root: Root = Root(),
    var body_root: Root = Root()
)

data class SigningData(
    var object_root: Root = Root(),
    var domain: Domain = Domain()
)

data class Attestation(
    var aggregation_bits: SSZBitlist = SSZBitlist(),
    var data: AttestationData = AttestationData(),
    var custody_bits_blocks: SSZList<SSZBitlist> = SSZList<SSZBitlist>(),
    var signature: BLSSignature = BLSSignature()
)

data class IndexedAttestation(
    var attesting_indices: SSZList<ValidatorIndex> = SSZList<ValidatorIndex>(),
    var data: AttestationData = AttestationData(),
    var signature: BLSSignature = BLSSignature()
)

data class AttesterSlashing(
    var attestation_1: IndexedAttestation = IndexedAttestation(),
    var attestation_2: IndexedAttestation = IndexedAttestation()
)

data class Deposit(
    var proof: SSZVector<Bytes32> = SSZVector<Bytes32>(),
    var data: DepositData = DepositData()
)

data class VoluntaryExit(
    var epoch: Epoch = Epoch(),
    var validator_index: ValidatorIndex = ValidatorIndex()
)

data class SignedVoluntaryExit(
    var message: VoluntaryExit = VoluntaryExit(),
    var signature: BLSSignature = BLSSignature()
)

data class SignedBeaconBlockHeader(
    var message: BeaconBlockHeader = BeaconBlockHeader(),
    var signature: BLSSignature = BLSSignature()
)

data class ProposerSlashing(
    var signed_header_1: SignedBeaconBlockHeader = SignedBeaconBlockHeader(),
    var signed_header_2: SignedBeaconBlockHeader = SignedBeaconBlockHeader()
)

data class Eth1Block(
    var timestamp: uint64 = 0uL,
    var deposit_root: Root = Root(),
    var deposit_count: uint64 = 0uL
)

data class AggregateAndProof(
    var aggregator_index: ValidatorIndex = ValidatorIndex(),
    var aggregate: Attestation = Attestation(),
    var selection_proof: BLSSignature = BLSSignature()
)

data class SignedAggregateAndProof(
    var message: AggregateAndProof = AggregateAndProof(),
    var signature: BLSSignature = BLSSignature()
)

data class CustodyChunkChallengeRecord(
    var challenge_index: uint64 = 0uL,
    var challenger_index: ValidatorIndex = ValidatorIndex(),
    var responder_index: ValidatorIndex = ValidatorIndex(),
    var inclusion_epoch: Epoch = Epoch(),
    var data_root: Root = Root(),
    var chunk_index: uint64 = 0uL
)

data class CustodyChunkResponse(
    var challenge_index: uint64 = 0uL,
    var chunk_index: uint64 = 0uL,
    var chunk: SSZByteVector = SSZByteVector(),
    var branch: SSZVector<Root> = SSZVector<Root>()
)

data class CustodyKeyReveal(
    var revealer_index: ValidatorIndex = ValidatorIndex(),
    var reveal: BLSSignature = BLSSignature()
)

data class EarlyDerivedSecretReveal(
    var revealed_index: ValidatorIndex = ValidatorIndex(),
    var epoch: Epoch = Epoch(),
    var reveal: BLSSignature = BLSSignature(),
    var masker_index: ValidatorIndex = ValidatorIndex(),
    var mask: Bytes32 = Bytes32()
)

data class ShardBlock(
    var shard_parent_root: Root = Root(),
    var beacon_parent_root: Root = Root(),
    var slot: Slot = Slot(),
    var shard: Shard = Shard(),
    var proposer_index: ValidatorIndex = ValidatorIndex(),
    var body: SSZByteList = SSZByteList()
)

data class SignedShardBlock(
    var message: ShardBlock = ShardBlock(),
    var signature: BLSSignature = BLSSignature()
)

data class ShardBlockHeader(
    var shard_parent_root: Root = Root(),
    var beacon_parent_root: Root = Root(),
    var slot: Slot = Slot(),
    var shard: Shard = Shard(),
    var proposer_index: ValidatorIndex = ValidatorIndex(),
    var body_root: Root = Root()
)

data class ShardState(
    var slot: Slot = Slot(),
    var gasprice: Gwei = Gwei(),
    var latest_block_root: Root = Root()
)

data class ShardTransition(
    var start_slot: Slot = Slot(),
    var shard_block_lengths: SSZList<uint64> = SSZList<uint64>(),
    var shard_data_roots: SSZList<Bytes32> = SSZList<Bytes32>(),
    var shard_states: SSZList<ShardState> = SSZList<ShardState>(),
    var proposer_signature_aggregate: BLSSignature = BLSSignature()
)

data class CustodySlashing(
    var data_index: uint64 = 0uL,
    var malefactor_index: ValidatorIndex = ValidatorIndex(),
    var malefactor_secret: BLSSignature = BLSSignature(),
    var whistleblower_index: ValidatorIndex = ValidatorIndex(),
    var shard_transition: ShardTransition = ShardTransition(),
    var attestation: Attestation = Attestation(),
    var data: SSZByteList = SSZByteList()
)

data class SignedCustodySlashing(
    var message: CustodySlashing = CustodySlashing(),
    var signature: BLSSignature = BLSSignature()
)

data class CustodyChunkChallenge(
    var responder_index: ValidatorIndex = ValidatorIndex(),
    var shard_transition: ShardTransition = ShardTransition(),
    var attestation: Attestation = Attestation(),
    var data_index: uint64 = 0uL,
    var chunk_index: uint64 = 0uL
)

data class BeaconBlockBody(
    var randao_reveal: BLSSignature = BLSSignature(),
    var eth1_data: Eth1Data = Eth1Data(),
    var graffiti: Bytes32 = Bytes32(),
    var proposer_slashings: SSZList<ProposerSlashing> = SSZList<ProposerSlashing>(),
    var attester_slashings: SSZList<AttesterSlashing> = SSZList<AttesterSlashing>(),
    var attestations: SSZList<Attestation> = SSZList<Attestation>(),
    var deposits: SSZList<Deposit> = SSZList<Deposit>(),
    var voluntary_exits: SSZList<SignedVoluntaryExit> = SSZList<SignedVoluntaryExit>(),
    var chunk_challenges: SSZList<CustodyChunkChallenge> = SSZList<CustodyChunkChallenge>(),
    var chunk_challenge_responses: SSZList<CustodyChunkResponse> = SSZList<CustodyChunkResponse>(),
    var custody_key_reveals: SSZList<CustodyKeyReveal> = SSZList<CustodyKeyReveal>(),
    var early_derived_secret_reveals: SSZList<EarlyDerivedSecretReveal> = SSZList<EarlyDerivedSecretReveal>(),
    var custody_slashings: SSZList<SignedCustodySlashing> = SSZList<SignedCustodySlashing>(),
    var shard_transitions: SSZVector<ShardTransition> = SSZVector<ShardTransition>(),
    var light_client_bits: SSZBitvector = SSZBitvector(),
    var light_client_signature: BLSSignature = BLSSignature()
)

data class BeaconBlock(
    var slot: Slot = Slot(),
    var proposer_index: ValidatorIndex = ValidatorIndex(),
    var parent_root: Root = Root(),
    var state_root: Root = Root(),
    var body: BeaconBlockBody = BeaconBlockBody()
)

data class SignedBeaconBlock(
    var message: BeaconBlock = BeaconBlock(),
    var signature: BLSSignature = BLSSignature()
)

data class CompactCommittee(
    var pubkeys: SSZList<BLSPubkey> = SSZList<BLSPubkey>(),
    var compact_validators: SSZList<uint64> = SSZList<uint64>()
)

data class BeaconState(
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
    var randao_mixes: SSZVector<Root> = SSZVector<Root>(),
    var slashings: SSZVector<Gwei> = SSZVector<Gwei>(),
    var previous_epoch_attestations: SSZList<PendingAttestation> = SSZList<PendingAttestation>(),
    var current_epoch_attestations: SSZList<PendingAttestation> = SSZList<PendingAttestation>(),
    var justification_bits: SSZBitvector = SSZBitvector(),
    var previous_justified_checkpoint: Checkpoint = Checkpoint(),
    var current_justified_checkpoint: Checkpoint = Checkpoint(),
    var finalized_checkpoint: Checkpoint = Checkpoint(),
    var current_epoch_start_shard: Shard = Shard(),
    var shard_states: SSZList<ShardState> = SSZList<ShardState>(),
    var online_countdown: SSZList<OnlineEpochs> = SSZList<OnlineEpochs>(),
    var current_light_committee: CompactCommittee = CompactCommittee(),
    var next_light_committee: CompactCommittee = CompactCommittee(),
    var exposed_derived_secrets: SSZVector<SSZList<ValidatorIndex>> = SSZVector<SSZList<ValidatorIndex>>(),
    var custody_chunk_challenge_records: SSZList<CustodyChunkChallengeRecord> = SSZList<CustodyChunkChallengeRecord>(),
    var custody_chunk_challenge_index: uint64 = 0uL
)

data class FullAttestationData(
    var slot: Slot = Slot(),
    var index: CommitteeIndex = CommitteeIndex(),
    var beacon_block_root: Root = Root(),
    var source: Checkpoint = Checkpoint(),
    var target: Checkpoint = Checkpoint(),
    var shard_head_root: Root = Root(),
    var shard_transition: ShardTransition = ShardTransition()
)

data class FullAttestation(
    var aggregation_bits: SSZBitlist = SSZBitlist(),
    var data: FullAttestationData = FullAttestationData(),
    var signature: BLSSignature = BLSSignature()
)

data class LightClientVoteData(
    var slot: Slot = Slot(),
    var beacon_block_root: Root = Root()
)

data class LightClientVote(
    var data: LightClientVoteData = LightClientVoteData(),
    var aggregation_bits: SSZBitvector = SSZBitvector(),
    var signature: BLSSignature = BLSSignature()
)

data class LightAggregateAndProof(
    var aggregator_index: ValidatorIndex = ValidatorIndex(),
    var aggregate: LightClientVote = LightClientVote(),
    var selection_proof: BLSSignature = BLSSignature()
)

data class SignedLightAggregateAndProof(
    var message: LightAggregateAndProof = LightAggregateAndProof(),
    var signature: BLSSignature = BLSSignature()
)

data class LatestMessage(
    var epoch: Epoch = Epoch(),
    var root: Root = Root()
)

data class ShardLatestMessage(
    var epoch: Epoch = Epoch(),
    var root: Root = Root()
)

data class ShardStore(
    var shard: Shard = Shard(),
    var signed_blocks: SSZDict<Root, SignedShardBlock> = SSZDict<Root, SignedShardBlock>(),
    var block_states: SSZDict<Root, ShardState> = SSZDict<Root, ShardState>(),
    var latest_messages: SSZDict<ValidatorIndex, ShardLatestMessage> = SSZDict<ValidatorIndex, ShardLatestMessage>()
)

data class Store(
    var time: uint64 = 0uL,
    var genesis_time: uint64 = 0uL,
    var justified_checkpoint: Checkpoint = Checkpoint(),
    var finalized_checkpoint: Checkpoint = Checkpoint(),
    var best_justified_checkpoint: Checkpoint = Checkpoint(),
    var blocks: SSZDict<Root, BeaconBlock> = SSZDict<Root, BeaconBlock>(),
    var block_states: SSZDict<Root, BeaconState> = SSZDict<Root, BeaconState>(),
    var checkpoint_states: SSZDict<Checkpoint, BeaconState> = SSZDict<Checkpoint, BeaconState>(),
    var latest_messages: SSZDict<ValidatorIndex, LatestMessage> = SSZDict<ValidatorIndex, LatestMessage>(),
    var shard_stores: SSZDict<Shard, ShardStore> = SSZDict<Shard, ShardStore>()
)
