package phase1

import ssz.Bytes32
import ssz.Bytes4
import ssz.Bytes48
import ssz.Bytes96
import ssz.CBitlist
import ssz.CBitvector
import ssz.CByteList
import ssz.CDict
import ssz.CList
import ssz.CVector
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
    var max_reveal_lateness: Epoch = Epoch()
)

data class AttestationData(
    var slot: Slot = Slot(),
    var index: CommitteeIndex = CommitteeIndex(),
    var beacon_block_root: Root = Root(),
    var source: Checkpoint = Checkpoint(),
    var target: Checkpoint = Checkpoint(),
    var head_shard_root: Root = Root(),
    var shard_transition_root: Root = Root()
)

data class PendingAttestation(
    var aggregation_bits: CBitlist = CBitlist(),
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
    var block_roots: CVector<Root> = CVector<Root>(),
    var state_roots: CVector<Root> = CVector<Root>()
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
    var aggregation_bits: CBitlist = CBitlist(),
    var data: AttestationData = AttestationData(),
    var custody_bits_blocks: CList<CBitlist> = CList<CBitlist>(),
    var signature: BLSSignature = BLSSignature()
)

data class IndexedAttestation(
    var committee: CList<ValidatorIndex> = CList<ValidatorIndex>(),
    var attestation: Attestation = Attestation()
)

data class AttesterSlashing(
    var attestation_1: IndexedAttestation = IndexedAttestation(),
    var attestation_2: IndexedAttestation = IndexedAttestation()
)

data class Deposit(
    var proof: CVector<Bytes32> = CVector<Bytes32>(),
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
    var proposer_index: ValidatorIndex = ValidatorIndex(),
    var body: CByteList = CByteList()
)

data class SignedShardBlock(
    var message: ShardBlock = ShardBlock(),
    var signature: BLSSignature = BLSSignature()
)

data class ShardBlockHeader(
    var shard_parent_root: Root = Root(),
    var beacon_parent_root: Root = Root(),
    var slot: Slot = Slot(),
    var proposer_index: ValidatorIndex = ValidatorIndex(),
    var body_root: Root = Root()
)

data class ShardState(
    var slot: Slot = Slot(),
    var gasprice: Gwei = Gwei(),
    var transition_digest: Bytes32 = Bytes32(),
    var latest_block_root: Root = Root()
)

data class ShardTransition(
    var start_slot: Slot = Slot(),
    var shard_block_lengths: CList<uint64> = CList<uint64>(),
    var shard_data_roots: CList<Bytes32> = CList<Bytes32>(),
    var shard_states: CList<ShardState> = CList<ShardState>(),
    var proposer_signature_aggregate: BLSSignature = BLSSignature()
)

data class CustodySlashing(
    var data_index: uint64 = 0uL,
    var malefactor_index: ValidatorIndex = ValidatorIndex(),
    var malefactor_secret: BLSSignature = BLSSignature(),
    var whistleblower_index: ValidatorIndex = ValidatorIndex(),
    var shard_transition: ShardTransition = ShardTransition(),
    var attestation: Attestation = Attestation(),
    var data: CByteList = CByteList()
)

data class SignedCustodySlashing(
    var message: CustodySlashing = CustodySlashing(),
    var signature: BLSSignature = BLSSignature()
)

data class BeaconBlockBody(
    var randao_reveal: BLSSignature = BLSSignature(),
    var eth1_data: Eth1Data = Eth1Data(),
    var graffiti: Bytes32 = Bytes32(),
    var proposer_slashings: CList<ProposerSlashing> = CList<ProposerSlashing>(),
    var attester_slashings: CList<AttesterSlashing> = CList<AttesterSlashing>(),
    var attestations: CList<Attestation> = CList<Attestation>(),
    var deposits: CList<Deposit> = CList<Deposit>(),
    var voluntary_exits: CList<SignedVoluntaryExit> = CList<SignedVoluntaryExit>(),
    var custody_slashings: CList<SignedCustodySlashing> = CList<SignedCustodySlashing>(),
    var custody_key_reveals: CList<CustodyKeyReveal> = CList<CustodyKeyReveal>(),
    var early_derived_secret_reveals: CList<EarlyDerivedSecretReveal> = CList<EarlyDerivedSecretReveal>(),
    var shard_transitions: CVector<ShardTransition> = CVector<ShardTransition>(),
    var light_client_signature_bitfield: CBitvector = CBitvector(),
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
    var pubkeys: CList<BLSPubkey> = CList<BLSPubkey>(),
    var compact_validators: CList<uint64> = CList<uint64>()
)

data class BeaconState(
    var genesis_time: uint64 = 0uL,
    var genesis_validators_root: Root = Root(),
    var slot: Slot = Slot(),
    var fork: Fork = Fork(),
    var latest_block_header: BeaconBlockHeader = BeaconBlockHeader(),
    var block_roots: CVector<Root> = CVector<Root>(),
    var state_roots: CVector<Root> = CVector<Root>(),
    var historical_roots: CList<Root> = CList<Root>(),
    var eth1_data: Eth1Data = Eth1Data(),
    var eth1_data_votes: CList<Eth1Data> = CList<Eth1Data>(),
    var eth1_deposit_index: uint64 = 0uL,
    var validators: CList<Validator> = CList<Validator>(),
    var balances: CList<Gwei> = CList<Gwei>(),
    var randao_mixes: CVector<Root> = CVector<Root>(),
    var slashings: CVector<Gwei> = CVector<Gwei>(),
    var previous_epoch_attestations: CList<PendingAttestation> = CList<PendingAttestation>(),
    var current_epoch_attestations: CList<PendingAttestation> = CList<PendingAttestation>(),
    var justification_bits: CBitvector = CBitvector(),
    var previous_justified_checkpoint: Checkpoint = Checkpoint(),
    var current_justified_checkpoint: Checkpoint = Checkpoint(),
    var finalized_checkpoint: Checkpoint = Checkpoint(),
    var shard_states: CList<ShardState> = CList<ShardState>(),
    var online_countdown: CList<OnlineEpochs> = CList<OnlineEpochs>(),
    var current_light_committee: CompactCommittee = CompactCommittee(),
    var next_light_committee: CompactCommittee = CompactCommittee(),
    var exposed_derived_secrets: CVector<CList<ValidatorIndex>> = CVector<CList<ValidatorIndex>>()
)

data class AttestationCustodyBitWrapper(
    var attestation_data_root: Root = Root(),
    var block_index: uint64 = 0uL,
    var bit: boolean = false
)

data class LatestMessage(
    var epoch: Epoch = Epoch(),
    var root: Root = Root()
)

data class Store(
    var time: uint64 = 0uL,
    var genesis_time: uint64 = 0uL,
    var justified_checkpoint: Checkpoint = Checkpoint(),
    var finalized_checkpoint: Checkpoint = Checkpoint(),
    var best_justified_checkpoint: Checkpoint = Checkpoint(),
    var blocks: CDict<Root, BeaconBlockHeader> = CDict<Root, BeaconBlockHeader>(),
    var block_states: CDict<Root, BeaconState> = CDict<Root, BeaconState>(),
    var checkpoint_states: CDict<Checkpoint, BeaconState> = CDict<Checkpoint, BeaconState>(),
    var latest_messages: CDict<ValidatorIndex, LatestMessage> = CDict<ValidatorIndex, LatestMessage>()
)
