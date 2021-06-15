package altair

import pylib.PyDict
import pylib.pyint
import phase0.*
import ssz.*

typealias GeneralizedIndex = pyint
typealias ParticipationFlags = uint8
fun ParticipationFlags(x: ULong): ParticipationFlags = uint8(x)
fun ParticipationFlags(x: uint8): ParticipationFlags = x
fun ParticipationFlags() = ParticipationFlags(0u.toUByte())
data class BeaconBlockBody(
    var randao_reveal: BLSSignature = BLSSignature(),
    var eth1_data: Eth1Data = Eth1Data(),
    var graffiti: Bytes32 = Bytes32(),
    var proposer_slashings: SSZList<ProposerSlashing> = SSZList<ProposerSlashing>(),
    var attester_slashings: SSZList<AttesterSlashing> = SSZList<AttesterSlashing>(),
    var attestations: SSZList<Attestation> = SSZList<Attestation>(),
    var deposits: SSZList<Deposit> = SSZList<Deposit>(),
    var voluntary_exits: SSZList<SignedVoluntaryExit> = SSZList<SignedVoluntaryExit>(),
    var sync_aggregate: SyncAggregate = SyncAggregate()
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
    var next_sync_committee: SyncCommittee = SyncCommittee()
)
data class SyncAggregate(
    var sync_committee_bits: SSZBitvector = SSZBitvector(),
    var sync_committee_signature: BLSSignature = BLSSignature()
)
data class SyncCommittee(
    var pubkeys: SSZVector<BLSPubkey> = SSZVector<BLSPubkey>(),
    var aggregate_pubkey: BLSPubkey = BLSPubkey()
)
data class SyncCommitteeMessage(
    var slot: Slot = Slot(),
    var beacon_block_root: Root = Root(),
    var validator_index: ValidatorIndex = ValidatorIndex(),
    var signature: BLSSignature = BLSSignature()
)
data class SyncCommitteeContribution(
    var slot: Slot = Slot(),
    var beacon_block_root: Root = Root(),
    var subcommittee_index: uint64 = 0uL,
    var aggregation_bits: SSZBitvector = SSZBitvector(),
    var signature: BLSSignature = BLSSignature()
)
data class ContributionAndProof(
    var aggregator_index: ValidatorIndex = ValidatorIndex(),
    var contribution: SyncCommitteeContribution = SyncCommitteeContribution(),
    var selection_proof: BLSSignature = BLSSignature()
)
data class SignedContributionAndProof(
    var message: ContributionAndProof = ContributionAndProof(),
    var signature: BLSSignature = BLSSignature()
)
data class SyncAggregatorSelectionData(
    var slot: Slot = Slot(),
    var subcommittee_index: uint64 = 0uL
)
data class LightClientSnapshot(
    var header: BeaconBlockHeader = BeaconBlockHeader(),
    var current_sync_committee: SyncCommittee = SyncCommittee(),
    var next_sync_committee: SyncCommittee = SyncCommittee()
)
data class LightClientUpdate(
    var header: BeaconBlockHeader = BeaconBlockHeader(),
    var next_sync_committee: SyncCommittee = SyncCommittee(),
    var next_sync_committee_branch: SSZVector<Bytes32> = SSZVector<Bytes32>(),
    var finality_header: BeaconBlockHeader = BeaconBlockHeader(),
    var finality_branch: SSZVector<Bytes32> = SSZVector<Bytes32>(),
    var sync_committee_bits: SSZBitvector = SSZBitvector(),
    var sync_committee_signature: BLSSignature = BLSSignature(),
    var fork_version: Version = Version()
)
data class LightClientStore(
    var snapshot: LightClientSnapshot = LightClientSnapshot(),
    var valid_updates: MutableSet<LightClientUpdate> = mutableSetOf<LightClientUpdate>()
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
