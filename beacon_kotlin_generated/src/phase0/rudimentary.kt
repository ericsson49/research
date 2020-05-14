package phase0

import phase1.BeaconBlockHeader
import phase1.Checkpoint
import phase1.Epoch
import phase1.Eth1Data
import phase1.Fork
import phase1.Gwei
import phase1.PendingAttestation
import phase1.Root
import phase1.Slot
import phase1.Validator
import phase1.compute_epoch_at_slot
import ssz.Bytes32
import ssz.CBitvector

/**
 * Minimal code to make phase1 compile.
 * Full phase0 in near future.
 */

typealias uint64 = ULong
typealias Vector<T> = MutableList<T>

fun get_current_epoch(state: BeaconState): Epoch {
  return compute_epoch_at_slot(state.slot)
}

data class BeaconState(
    // Versioning
    val genesis_time: uint64 = 0uL,
    var slot: Slot = 0uL,
    val fork: Fork = Fork(),
    // History
    var latest_block_header: BeaconBlockHeader = BeaconBlockHeader(),
    val block_roots: Vector<Root> = mutableListOf(), // getSLOTS_PER_HISTORICAL_ROOT],
    val state_roots: Vector<Root> = mutableListOf(), // getSLOTS_PER_HISTORICAL_ROOT],
    val historical_roots: MutableList<Root> = mutableListOf(), //, HISTORICAL_ROOTS_LIMIT],
    // Eth1
    var eth1_data: Eth1Data = Eth1Data(),
    var eth1_data_votes: MutableList<Eth1Data> = mutableListOf(), //, getSLOTS_PER_ETH1_VOTING_PERIOD],
    var eth1_deposit_index: uint64 = 0uL,
    // Registry
    val validators: Vector<Validator> = mutableListOf(), //, VALIDATOR_REGISTRY_LIMIT],
    val balances: MutableList<Gwei> = mutableListOf(), //, VALIDATOR_REGISTRY_LIMIT],
    // Randomness
    val randao_mixes: Vector<Bytes32> = mutableListOf(), //, getEPOCHS_PER_HISTORICAL_VECTOR],
    // Slashings
    val slashings: Vector<Gwei> = mutableListOf(), //, getEPOCHS_PER_SLASHINGS_VECTOR],  // Per-epoch sums of slashed effective balances
    // Attestations
    var previous_epoch_attestations: MutableList<PendingAttestation> = mutableListOf(), //, MAX_ATTESTATIONS * SLOTS_PER_EPOCH],
    var current_epoch_attestations: MutableList<PendingAttestation> = mutableListOf(), //, MAX_ATTESTATIONS * SLOTS_PER_EPOCH],
    // Finality
    val justification_bits: CBitvector = CBitvector(), //[JUSTIFICATION_BITS_LENGTH], // Bit pylib.set for (every recent justified epoch
    var previous_justified_checkpoint: Checkpoint = Checkpoint(),  // Previous epoch snapshot
    var current_justified_checkpoint: Checkpoint = Checkpoint(),
    var finalized_checkpoint: Checkpoint = Checkpoint()
)
