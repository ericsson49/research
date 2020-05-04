import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.bytes.Bytes48
import java.util.*

typealias uint32 = UInt
typealias uint64 = ULong
typealias Slot = uint64
typealias Epoch = uint64
typealias Root = Bytes32
typealias ValidatorIndex = uint64
typealias Gwei = uint64
typealias BLSPubkey = Bytes48
typealias BLSSignature = Bytes // 96
typealias Version = Bytes // 4
typealias CommitteeIndex = uint64
typealias Bitlist = BooleanArray
typealias Bitvector = BooleanArray
typealias Domain = Bytes // 8
typealias DomainType = Bytes // 4
typealias Bytes1 = Byte
typealias Sequence<T> = List<T>
typealias bytes = Bytes
typealias Vector<T> = MutableList<T>

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
interface Constants {
  val GENESIS_SLOT: Slot get() = 0uL
  val GENESIS_EPOCH: Epoch get() = 0uL
  val FAR_FUTURE_EPOCH: Epoch get() = (1uL shl 64) - 1u
  val BASE_REWARDS_PER_EPOCH: uint32 get() = 4u
  val DEPOSIT_CONTRACT_TREE_DEPTH: uint64 get() = 1uL shl 5
  val JUSTIFICATION_BITS_LENGTH: Int get() = 4
  val ENDIANNESS: String get() = "little"
  val MAX_COMMITTEES_PER_SLOT: uint64 get() = 1uL shl 6
  val TARGET_COMMITTEE_SIZE: uint32 get() = 1u shl 7
  val MAX_VALIDATORS_PER_COMMITTEE: uint64 get() = 1uL shl 11
  val MIN_PER_EPOCH_CHURN_LIMIT: ULong get() = 1uL shl 2
  val CHURN_LIMIT_QUOTIENT: uint32 get() = 1u shl 16
  val SHUFFLE_ROUND_COUNT: uint64 get() = 90u
  val MIN_GENESIS_ACTIVE_VALIDATOR_COUNT: uint32 get() = 1u shl 14
  val MIN_GENESIS_TIME: uint64 get() = 1578009600uL
  val MIN_DEPOSIT_AMOUNT: Gwei get() = (1uL shl 0) * 1000_000_000uL
  val MAX_EFFECTIVE_BALANCE: Gwei get() = (1uL shl 5) * 1000_000_000uL
  val EJECTION_BALANCE: Gwei get() = (1uL shl 4) * 1000_000_000uL
  val EFFECTIVE_BALANCE_INCREMENT: Gwei get() = (1uL shl 0) * 1000_000_000uL
  val GENESIS_FORK_VERSION: Version get() = Bytes.fromHexString("0x00000000")
  val BLS_WITHDRAWAL_PREFIX: Bytes1 get() = 0x00
  val MIN_GENESIS_DELAY: uint32 get() = 86400u
  val SECONDS_PER_SLOT: uint32 get() = 12u
  val MIN_ATTESTATION_INCLUSION_DELAY: uint32 get() = 1u shl 0
  val SLOTS_PER_EPOCH: uint32 get() = 1u shl 5
  val MIN_SEED_LOOKAHEAD: uint32 get() = 1u shl 0
  val MAX_SEED_LOOKAHEAD: uint32 get() = 1u shl 2
  val MIN_EPOCHS_TO_INACTIVITY_PENALTY: uint32 get() = 1u shl 2
  val SLOTS_PER_ETH1_VOTING_PERIOD: uint32 get() = 1u shl 10
  val SLOTS_PER_HISTORICAL_ROOT: uint32 get() = 1u shl 13
  val MIN_VALIDATOR_WITHDRAWABILITY_DELAY: uint32 get() = 1u shl 8
  val PERSISTENT_COMMITTEE_PERIOD: uint32 get() = 1u shl 11
  val EPOCHS_PER_HISTORICAL_VECTOR: uint32 get() = 1u shl 16
  val EPOCHS_PER_SLASHINGS_VECTOR: uint32 get() = 1u shl 13
  val HISTORICAL_ROOTS_LIMIT: uint32 get() = 1u shl 24
  val VALIDATOR_REGISTRY_LIMIT: uint32 get() = 1u shl 40
  val BASE_REWARD_FACTOR: uint32 get() = 1u shl 6
  val WHISTLEBLOWER_REWARD_QUOTIENT: uint32 get() = 1u shl 9
  val PROPOSER_REWARD_QUOTIENT: uint32 get() = 1u shl 3
  val INACTIVITY_PENALTY_QUOTIENT: uint32 get() = 1u shl 25
  val MIN_SLASHING_PENALTY_QUOTIENT: uint32 get() = 1u shl 5
  val MAX_PROPOSER_SLASHINGS: uint32 get() = 1u shl 4
  val MAX_ATTESTER_SLASHINGS: uint32 get() = 1u shl 0
  val MAX_ATTESTATIONS: uint32 get() = 1u shl 7
  val MAX_DEPOSITS: uint64 get() = 1uL shl 4
  val MAX_VOLUNTARY_EXITS: uint32 get() = 1u shl 4
  val DOMAIN_BEACON_PROPOSER: DomainType get() = Bytes.fromHexString("0x00000000")
  val DOMAIN_BEACON_ATTESTER: DomainType get() = Bytes.fromHexString("0x01000000")
  val DOMAIN_RANDAO: DomainType get() = Bytes.fromHexString("0x02000000")
  val DOMAIN_DEPOSIT: DomainType get() = Bytes.fromHexString("0x03000000")
  val DOMAIN_VOLUNTARY_EXIT: DomainType get() = Bytes.fromHexString("0x04000000")
  val SAFE_SLOTS_TO_UPDATE_JUSTIFIED: uint32 get() = 1u shl 3
  val ETH1_FOLLOW_DISTANCE: uint32 get() = 1u shl 10
  val TARGET_AGGREGATORS_PER_COMMITTEE: uint32 get() = 1u shl 4
  val RANDOM_SUBNETS_PER_VALIDATOR: uint32 get() = 1u shl 0
  val EPOCHS_PER_RANDOM_SUBNET_SUBSCRIPTION: uint32 get() = 1u shl 8
  val SECONDS_PER_ETH1_BLOCK: uint32 get() = 14u
}

data class Fork(
    val previous_version: Version = Bytes.fromHexString("0x00000000"),
    val current_version: Version = Bytes.fromHexString("0x00000000"),
    val epoch: Epoch = 0uL
)

data class Checkpoint(val epoch: Epoch = 0uL, val root: Root = Bytes32.ZERO)

data class IValidator(
    val pubkey: BLSPubkey = Bytes48.ZERO,
    val withdrawal_credentials: Bytes32 = Bytes32.ZERO,
    var effective_balance: Gwei = 0uL,
    var slashed: Boolean = false,
    var activation_eligibility_epoch: Epoch = 0uL,
    var activation_epoch: Epoch = Epoch.MAX_VALUE,
    var exit_epoch: Epoch = Epoch.MAX_VALUE,
    var withdrawable_epoch: Epoch = Epoch.MAX_VALUE
)

data class AttestationData(
    val slot: Slot,
    val index: CommitteeIndex,
    // LMD GHOST vote
    val beacon_block_root: Root,
    // FFG vote
    val source: Checkpoint,
    val target: Checkpoint
)

data class IndexedAttestation(
    val attesting_indices: List<ValidatorIndex>, // getMAX_VALIDATORS_PER_COMMITTEE]
    val data: AttestationData,
    val signature: BLSSignature
)

data class PendingAttestation(
    val aggregation_bits: Bitlist, //[getMAX_VALIDATORS_PER_COMMITTEE],
    val data: AttestationData,
    val inclusion_delay: Slot,
    val proposer_index: ValidatorIndex
)

data class Eth1Data(
    var deposit_root: Root = Bytes32.ZERO,
    val deposit_count: uint64 = 0uL,
    val block_hash: Bytes32 = Bytes32.ZERO
)

data class HistoricalBatch(
    val block_roots: Vector<Root>, //, getSLOTS_PER_HISTORICAL_ROOT],
    val state_roots: Vector<Root> //, getSLOTS_PER_HISTORICAL_ROOT],
)

data class DepositMessage(
    val pubkey: BLSPubkey,
    val withdrawal_credentials: Bytes32,
    val amount: Gwei
)

data class DepositData(
    val pubkey: BLSPubkey,
    val withdrawal_credentials: Bytes32,
    val amount: Gwei,
    val signature: BLSSignature  // Signing over DepositMessage
)

data class BeaconBlockHeader(
    val slot: Slot = 0uL,
    val parent_root: Root = Bytes32.ZERO,
    var state_root: Root = Bytes32.ZERO,
    val body_root: Root = Bytes32.ZERO
)

data class SigningRoot(
    val object_root: Root,
    val domain: Domain
)

data class AttesterSlashing(
    val attestation_1: IndexedAttestation,
    val attestation_2: IndexedAttestation
)

data class Attestation(
    val aggregation_bits: Bitlist, //[getMAX_VALIDATORS_PER_COMMITTEE],
    val data: AttestationData,
    val signature: BLSSignature
)

data class Deposit(
    val proof: Vector<Bytes32>, //, getDEPOSIT_CONTRACT_TREE_DEPTH + 1],  // Merkle path to deposit root
    val data: DepositData
)

data class VoluntaryExit(
    val epoch: Epoch,  // Earliest epoch when voluntary exit can be processed
    val validator_index: ValidatorIndex
)

data class BeaconState (
    val c: Constants,
    // Versioning
    val genesis_time: uint64 = 0uL,
    var slot: Slot = 0uL,
    val fork: Fork = Fork(),
    // History
    var latest_block_header: BeaconBlockHeader = BeaconBlockHeader(),
    val block_roots: Vector<Root> = mutableListOf(), // getSLOTS_PER_HISTORICAL_ROOT],
    //val state_roots: Vector<Root> = mutableListOf(), // getSLOTS_PER_HISTORICAL_ROOT],
    //val historical_roots: MutableList<Root> = mutableListOf(), //, HISTORICAL_ROOTS_LIMIT],
    // Eth1
    var eth1_data: Eth1Data = Eth1Data(),
    var eth1_data_votes: MutableList<Eth1Data> = mutableListOf(), //, getSLOTS_PER_ETH1_VOTING_PERIOD],
    var eth1_deposit_index: uint64 = 0uL,
    // Registry
    val validators: Vector<IValidator> = mutableListOf(), //, VALIDATOR_REGISTRY_LIMIT],
    val balances: MutableList<Gwei> = mutableListOf(), //, VALIDATOR_REGISTRY_LIMIT],
    // Randomness
    val randao_mixes: Vector<Bytes32> = mutableListOf(), //, getEPOCHS_PER_HISTORICAL_VECTOR],
    // Slashings
    val slashings: Vector<Gwei> = mutableListOf(), //, getEPOCHS_PER_SLASHINGS_VECTOR],  // Per-epoch sums of slashed effective balances
    // Attestations
    var previous_epoch_attestations: MutableList<PendingAttestation> = mutableListOf(), //, MAX_ATTESTATIONS * SLOTS_PER_EPOCH],
    var current_epoch_attestations: MutableList<PendingAttestation> = mutableListOf(), //, MAX_ATTESTATIONS * SLOTS_PER_EPOCH],
    // Finality
    val justification_bits: Bitvector = BooleanArray(c.JUSTIFICATION_BITS_LENGTH), //[JUSTIFICATION_BITS_LENGTH], // Bit set for (every recent justified epoch
    var previous_justified_checkpoint: Checkpoint = Checkpoint(),  // Previous epoch snapshot
    var current_justified_checkpoint: Checkpoint = Checkpoint(),
    var finalized_checkpoint: Checkpoint = Checkpoint()
)

data class SignedVoluntaryExit(
    val message: VoluntaryExit,
    val signature: BLSSignature
)

data class SignedBeaconBlockHeader(
    val message: BeaconBlockHeader,
    val signature: BLSSignature
)

data class ProposerSlashing(
    val proposer_index: ValidatorIndex,
    val signed_header_1: SignedBeaconBlockHeader,
    val signed_header_2: SignedBeaconBlockHeader
)

data class BeaconBlockBody(
    val randao_reveal: BLSSignature = Bytes.concatenate(Bytes48.ZERO, Bytes48.ZERO),
    val eth1_data: Eth1Data = Eth1Data(),  // Eth1 data vote
    val graffiti: Bytes32 = Bytes32.ZERO,  // Arbitrary data
    // Operations
    val proposer_slashings: List<ProposerSlashing> = listOf(), //, MAX_PROPOSER_SLASHINGS
    val attester_slashings: List<AttesterSlashing> = listOf(), //, MAX_ATTESTER_SLASHINGS
    val attestations: List<Attestation> = listOf(), //, MAX_ATTESTATIONS
    val deposits: List<Deposit> = listOf(), // getMAX_DEPOSITS],
    val voluntary_exits: List<SignedVoluntaryExit> = listOf() //, MAX_VOLUNTARY_EXITS]
)

data class BeaconBlock(
    val slot: Slot,
    val parent_root: Root,
    val state_root: Root,
    val body: BeaconBlockBody
)

data class SignedBeaconBlock(
    val message: BeaconBlock,
    val signature: BLSSignature
)

data class Eth1Block(
    val timestamp: uint64
    // All other eth1 block fields
)

data class AggregateAndProof(
    val aggregator_index: ValidatorIndex,
    val aggregate: Attestation,
    val selection_proof: BLSSignature
)

interface BLS {
  fun Sign(privkey: Int, message: Root): BLSSignature
  fun Verify(pubkey: BLSPubkey, message: Root, signature: BLSSignature): Boolean
  fun FastAggregateVerify(pubkeys: Collection<BLSPubkey>, root: Root, signature: BLSSignature): Boolean
}

interface BeaconChain: Constants, BLS {
  fun hash(a: Bytes): Root
  fun hash_tree_root(a: Any): Root

  fun compute_epoch_at_slot(slot: Slot): Epoch = slot / SLOTS_PER_EPOCH
  fun compute_start_slot_at_epoch(epoch: Epoch): Slot = epoch * SLOTS_PER_EPOCH

  fun get_current_epoch(state: BeaconState): Epoch = compute_epoch_at_slot(state.slot)
  fun get_previous_epoch(state: BeaconState): Epoch {
    /*`
    Return the previous epoch (unless the current epoch is ``GENESIS_EPOCH``).
    */
    val current_epoch = get_current_epoch(state)
    return if (current_epoch == GENESIS_EPOCH) GENESIS_EPOCH else current_epoch - 1u
  }
  fun get_randao_mix(state: BeaconState, epoch: Epoch): Bytes32 =
      state.randao_mixes[epoch % EPOCHS_PER_HISTORICAL_VECTOR]
  fun get_block_root(state: BeaconState, epoch: Epoch): Root =
      get_block_root_at_slot(state, compute_start_slot_at_epoch(epoch))

  fun get_block_root_at_slot(state: BeaconState, slot: Slot): Root {
    assert(slot < state.slot && state.slot <= slot + SLOTS_PER_HISTORICAL_ROOT)
    return state.block_roots[slot % SLOTS_PER_HISTORICAL_ROOT]
  }

  fun compute_signing_root(ssz_object: Any, domain: Domain): Root {
    val domain_wrapped_object = SigningRoot(
        object_root = hash_tree_root(ssz_object),
        domain = domain
    )
    return hash_tree_root(domain_wrapped_object)
  }
  fun compute_domain(domain_type: DomainType, fork_version: Version? = null): Domain {
    var fork_version = fork_version
    if (fork_version == null) {
      fork_version = GENESIS_FORK_VERSION
    }
    return domain_type + fork_version
  }
  fun get_domain(state: BeaconState, domain_type: DomainType, epoch: Epoch? = null): Domain {
    val epoch = if (epoch == null) get_current_epoch(state) else epoch
    val fork_version = if (epoch < state.fork.epoch) state.fork.previous_version else state.fork.current_version
    return compute_domain(domain_type, fork_version)
  }
  fun get_block_signature(state: BeaconState, header: BeaconBlockHeader, privkey: Int): BLSSignature {
    val domain = get_domain(state, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(header.slot))
    val signing_root = compute_signing_root(header, domain)
    return Sign(privkey, signing_root)
  }
  fun get_attestation_signature(state: BeaconState, attestation_data: AttestationData, privkey: Int): BLSSignature {
    val domain = get_domain(state, DOMAIN_BEACON_ATTESTER, attestation_data.target.epoch)
    val signing_root = compute_signing_root(attestation_data, domain)
    return Sign(privkey, signing_root)
  }


  fun is_active_validator(validator: IValidator, epoch: Epoch): Boolean {
    return validator.activation_epoch <= epoch && epoch < validator.exit_epoch
  }

  fun is_eligible_for_activation_queue(validator: IValidator): Boolean {
    return (
        validator.activation_eligibility_epoch == FAR_FUTURE_EPOCH
            && validator.effective_balance == MAX_EFFECTIVE_BALANCE
        )
  }

  fun is_eligible_for_activation(state: BeaconState, validator: IValidator): Boolean {
    return (
        // Placement in queue is finalized
        validator.activation_eligibility_epoch <= state.finalized_checkpoint.epoch
            // Has not yet been activated
            && validator.activation_epoch == FAR_FUTURE_EPOCH
        )
  }

  fun is_slashable_validator(validator: IValidator, epoch: Epoch): Boolean {
    return (!validator.slashed) and (validator.activation_epoch <= epoch && epoch < validator.withdrawable_epoch)
  }

  fun get_active_validator_indices(state: BeaconState, epoch: Epoch): Sequence<ValidatorIndex> =
      enumerate(state.validators).filter { (_, validator) -> is_active_validator(validator, epoch) }.map { it.first.toULong() }

  fun compute_activation_exit_epoch(epoch: Epoch): Epoch = epoch + 1u + MAX_SEED_LOOKAHEAD
  fun get_validator_churn_limit(state: BeaconState): uint64 {
    /*
    Return the validator churn limit for (the current epoch.
    */
    val active_validator_indices = get_active_validator_indices(state, get_current_epoch(state))
    return max(MIN_PER_EPOCH_CHURN_LIMIT, len(active_validator_indices) / CHURN_LIMIT_QUOTIENT)
  }


  fun int_to_bytes(n: uint64, length: uint64): bytes {
    val str = when(ENDIANNESS) {
      "little" -> n.toULong().toString(16).substring(0, length.toInt()*2)
      "big" -> java.lang.Long.reverseBytes(n.toLong()).toULong().toString(16).substring((8-length.toInt())*2, 16)
      else -> throw IllegalArgumentException("Unsupported " + ENDIANNESS)
    }
    return Bytes.fromHexString("0x" + str)
  }

  fun bytes_to_int(data: bytes): uint64 {
    val res = java.lang.Long.parseUnsignedLong(data.toUnprefixedHexString(), 16)
    return when(ENDIANNESS) {
      "little" -> res
      "big" -> java.lang.Long.reverseBytes(res)
      else -> throw IllegalArgumentException("Unsupported " + ENDIANNESS)
    }.toULong()
  }

  fun get_seed(state: BeaconState, epoch: Epoch, domain_type: DomainType): Bytes32 {
    /*
      Return the seed at ``epoch``.
      */
    val mix = get_randao_mix(state, epoch + EPOCHS_PER_HISTORICAL_VECTOR - MIN_SEED_LOOKAHEAD - 1u)  // Avoid underflow
    return hash(domain_type + int_to_bytes(epoch, length = 8u) + mix)
  }

  fun compute_shuffled_index(index: ValidatorIndex, index_count: uint64, seed: Bytes32): ValidatorIndex {
    /*
    Return the shuffled validator index corresponding to ``seed`` (and ``index_count``).
    */
    assert(index < index_count)

    var index = index
    // Swap or not (https://link.springer.com/content/pdf/10.1007%2F978-3-642-32009-5_1.pdf)
    // See the 'generalized domain' algorithm on page 3
    for (current_round in range(SHUFFLE_ROUND_COUNT)) {
      val pivot = bytes_to_int(hash(seed + int_to_bytes(current_round, length = 1u)).slice(0, 8)) % index_count
      val flip = (pivot + index_count - index) % index_count
      val position = max(index, flip)
      val source = hash(seed + int_to_bytes(current_round, length = 1u) + int_to_bytes(position / 256u, length = 4u))
      val byte = source[((position % 256u) / 8u)]
      val bit = (byte.toByte().toInt() shr (position % 8u).toInt()) % 2 == 1
      index = if (bit) flip else index
    }

    return index
  }
  fun compute_proposer_index(state: BeaconState, indices: Sequence<ValidatorIndex>, seed: Bytes32): ValidatorIndex {
    /*
    Return from ``indices`` a random index sampled by effective balance.
    */
    assert(len(indices) > 0u)
    val MAX_RANDOM_BYTE = (1u shl 8) - 1u
    var i = 0u
    while (true) {
      val candidate_index = indices[compute_shuffled_index(i % len(indices), len(indices), seed)]
      val random_byte = hash(seed + int_to_bytes(i / 32uL, length = 8u))[(i % 32u)]
      val effective_balance = state.validators[candidate_index].effective_balance
      if (effective_balance * MAX_RANDOM_BYTE >= MAX_EFFECTIVE_BALANCE * random_byte.toInt().toUInt()) {
        return candidate_index
      }
      i += 1u
    }
  }
  fun get_beacon_proposer_index(state: BeaconState): ValidatorIndex {
    /*
      Return the beacon proposer index at the current slot.
      */
    val epoch = get_current_epoch(state)
    val seed = hash(get_seed(state, epoch, DOMAIN_BEACON_PROPOSER) + int_to_bytes(state.slot, length = 8u))
    val indices = get_active_validator_indices(state, epoch)
    return compute_proposer_index(state, indices, seed)
  }

  fun compute_committee(indices: Sequence<ValidatorIndex>, seed: Bytes32, index: uint64, count: uint64): Sequence<ValidatorIndex> {
    val start = (len(indices) * index) / count
    val end = (len(indices) * (index + 1u)) / count
    return range(start, end).map { i -> indices[compute_shuffled_index(i, len(indices), seed)] }
  }
  fun get_committee_count_at_slot(state: BeaconState, slot: Slot): uint64 {
    /*
      Return the number of committees at ``slot``.
      */
    val epoch = compute_epoch_at_slot(slot)
    return max(
        1u, min(
        MAX_COMMITTEES_PER_SLOT,
        len(get_active_validator_indices(state, epoch)) / SLOTS_PER_EPOCH / TARGET_COMMITTEE_SIZE
    )
    )
  }
  fun get_beacon_committee(state: BeaconState, slot: Slot, index: CommitteeIndex): Sequence<ValidatorIndex> {
    /*
      Return the beacon committee at ``slot`` for ``index``.
      */
    val epoch = compute_epoch_at_slot(slot)
    val committees_per_slot = get_committee_count_at_slot(state, slot)
    return compute_committee(
        indices = get_active_validator_indices(state, epoch),
        seed = get_seed(state, epoch, DOMAIN_BEACON_ATTESTER),
        index = (slot % SLOTS_PER_EPOCH) * committees_per_slot + index,
        count = committees_per_slot * SLOTS_PER_EPOCH
    )
  }


  fun get_total_balance(state: BeaconState, indices: Set<ValidatorIndex>): Gwei {
    /*
    Return the combined effective balance of the ``indices``. (1 Gwei minimum to avoid divisions by zero.)
    */
    return max(1u, indices.map { index -> state.validators[index].effective_balance }.sum())
  }

  fun get_total_active_balance(state: BeaconState): Gwei {
    /*
      Return the combined effective balance of the active validators.
      */
    return get_total_balance(state, get_active_validator_indices(state, get_current_epoch(state)).toSet())
  }

  fun increase_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei) {
    /*
      Increase the validator balance at index ``index`` by ``delta``.
      */
    state.balances[index] += delta
  }

  fun decrease_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei) {
    /*
    Decrease the validator balance at index ``index`` by ``delta``, with underflow protection.
    */
    state.balances[index] =
        if (delta > state.balances[index]) 0u else state.balances[index] - delta
  }


  fun initiate_validator_exit(state: BeaconState, index: ValidatorIndex) {
    /*
    Initiate the exit of the validator with index ``index``.
    */
    // Return if (validator already initiated exit
    val validator = state.validators[index]
    if (validator.exit_epoch != FAR_FUTURE_EPOCH) {
      return
    }
    // Compute exit queue epoch
    val exit_epochs = state.validators.filter { v -> v.exit_epoch != FAR_FUTURE_EPOCH }.map { v -> v.exit_epoch }

    var exit_queue_epoch = max(exit_epochs.max(), compute_activation_exit_epoch(get_current_epoch(state)))
    val exit_queue_churn = len(state.validators.filter { v -> v.exit_epoch == exit_queue_epoch })
    if (exit_queue_churn >= get_validator_churn_limit(state)) {
      exit_queue_epoch += 1u
    }
    // Set validator exit epoch and withdrawable epoch
    validator.exit_epoch = exit_queue_epoch
    validator.withdrawable_epoch = validator.exit_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY
  }
  fun slash_validator(state: BeaconState, slashed_index: ValidatorIndex, whistleblower_index: ValidatorIndex? = null) {
    val epoch = get_current_epoch(state)
    initiate_validator_exit(state, slashed_index)
    val validator = state.validators[slashed_index]
    validator.slashed = true
    validator.withdrawable_epoch = max(validator.withdrawable_epoch, epoch + EPOCHS_PER_SLASHINGS_VECTOR)
    state.slashings[(epoch % EPOCHS_PER_SLASHINGS_VECTOR)] += validator.effective_balance
    decrease_balance(state, slashed_index, validator.effective_balance / MIN_SLASHING_PENALTY_QUOTIENT)

    // Apply proposer and whistleblower rewards
    val proposer_index = get_beacon_proposer_index(state)
    var whistleblower_index = whistleblower_index
    if (whistleblower_index == null) {
      whistleblower_index = proposer_index
    }
    val whistleblower_reward = validator.effective_balance / WHISTLEBLOWER_REWARD_QUOTIENT
    val proposer_reward = whistleblower_reward / PROPOSER_REWARD_QUOTIENT
    increase_balance(state, proposer_index, proposer_reward)
    increase_balance(state, whistleblower_index, whistleblower_reward - proposer_reward)
  }


  fun integer_squareroot(n: uint64): uint64 {
    /*
    Return the largest integer ``x`` such that ``x**2 <= n``.
    */
    var x = n
    val y = (x + 1u) / 2u
    while (y < x) {
      x = y
      val y = (x + n / x) / 2u
    }
    return x
  }

  fun initialize_beacon_state_from_eth1(eth1_block_hash: Bytes32, eth1_timestamp: uint64, deposits: Sequence<Deposit>): BeaconState {
    val fork = Fork(
        previous_version = GENESIS_FORK_VERSION,
        current_version = GENESIS_FORK_VERSION,
        epoch = GENESIS_EPOCH
    )

    val state = BeaconState(
        c = this,
        genesis_time = eth1_timestamp - eth1_timestamp % MIN_GENESIS_DELAY + 2u * MIN_GENESIS_DELAY,
        fork = fork,
        eth1_data = Eth1Data(block_hash = eth1_block_hash, deposit_count = len(deposits)),
        latest_block_header = BeaconBlockHeader(body_root = hash_tree_root(BeaconBlockBody())),
        randao_mixes = (0 until EPOCHS_PER_HISTORICAL_VECTOR.toInt()).map { eth1_block_hash } as Vector<Bytes32>  // Seed RANDAO with Eth1 entropy
    )

    // Process deposits
    val leaves = deposits.map { deposit -> deposit.data }
    for ((_, deposit) in enumerate(deposits)) {
      val deposit_data_list =
          listOf<DepositData>() // [DepositData, 1 shl getDEPOSIT_CONTRACT_TREE_DEPTH](*leaves[:index +1])
      state.eth1_data.deposit_root = hash_tree_root(deposit_data_list)
      process_deposit(state, deposit)
    }

    // Process activations
    for ((index, validator) in enumerate(state.validators)) {
      val balance = state.balances[index]
      validator.effective_balance = min(balance - balance % EFFECTIVE_BALANCE_INCREMENT, MAX_EFFECTIVE_BALANCE)
      if (validator.effective_balance == MAX_EFFECTIVE_BALANCE) {
        validator.activation_eligibility_epoch = GENESIS_EPOCH
        validator.activation_epoch = GENESIS_EPOCH
      }
    }

    return state
  }

  fun is_valid_genesis_state(state: BeaconState): Boolean {
    if (state.genesis_time < MIN_GENESIS_TIME) {
      return false
    }
    if (len(get_active_validator_indices(state, GENESIS_EPOCH)) < MIN_GENESIS_ACTIVE_VALIDATOR_COUNT) {
      return false
    }
    return true
  }

  fun state_transition(
      state: BeaconState,
      signed_block: SignedBeaconBlock,
      validate_result: Boolean = true
  ): BeaconState {
    val block = signed_block.message
    // Process slots (including those with no blocks) since block
    process_slots(state, block.slot)
    // Verify signature
    if (validate_result) {
      assert(verify_block_signature(state, signed_block))
    }
    // Process block
    process_block(state, block)
    // Verify state root
    if (validate_result) {
      assert(block.state_root == hash_tree_root(state))
    }
    // Return post-state
    return state
  }

  fun verify_block_signature(state: BeaconState, signed_block: SignedBeaconBlock): Boolean {
    val proposer = state.validators[get_beacon_proposer_index(state)]
    val signing_root = compute_signing_root(signed_block.message, get_domain(state, DOMAIN_BEACON_PROPOSER))
    return Verify(proposer.pubkey, signing_root, signed_block.signature)
  }

  fun process_slots(state: BeaconState, slot: Slot) {
    assert(state.slot <= slot)
    while (state.slot < slot) {
      process_slot(state)
      // Process epoch on the start slot of the next epoch
      if ((state.slot + 1u) % SLOTS_PER_EPOCH == 0uL) {
        process_epoch(state)
      }
      state.slot += 1u
    }
  }

  fun process_slot(state: BeaconState) {
    // Cache state root
    val previous_state_root = hash_tree_root(state)
    //state.state_roots[(state.slot % SLOTS_PER_HISTORICAL_ROOT)] = previous_state_root
    // Cache latest block header state root
    if (state.latest_block_header.state_root == Bytes32.ZERO) {
      state.latest_block_header.state_root = previous_state_root
    }
    // Cache block root
    val previous_block_root = hash_tree_root(state.latest_block_header)
    state.block_roots[(state.slot % SLOTS_PER_HISTORICAL_ROOT)] = previous_block_root
  }

  fun process_epoch(state: BeaconState) {
    process_justification_and_finalization(state)
    process_rewards_and_penalties(state)
    process_registry_updates(state)
    // @process_reveal_deadlines
    // @process_challenge_deadlines
    process_slashings(state)
    // @update_period_committee
    process_final_updates(state)
    // @after_process_final_updates
  }

  fun get_matching_source_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
    assert(epoch in listOf(get_previous_epoch(state), get_current_epoch(state)))
    return if (epoch == get_current_epoch(state)) state.current_epoch_attestations else state.previous_epoch_attestations
  }

  fun get_matching_target_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
    return get_matching_source_attestations(state, epoch).filter { a ->
      a.data.target.root == get_block_root(
          state,
          epoch
      )
    }
  }

  fun get_matching_head_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
    return get_matching_source_attestations(
        state,
        epoch
    ).filter { a -> a.data.beacon_block_root == get_block_root_at_slot(state, a.data.slot) }
  }

  fun get_attesting_indices(state: BeaconState, data: AttestationData, bits: Bitlist /*MAX_VALIDATORS_PER_COMMITTEE*/): Set<ValidatorIndex> {
    val committee = get_beacon_committee(state, data.slot, data.index)
    return enumerate(committee).filter { (i, _) -> bits[i] }.map { (_,validator_index) -> validator_index }.toSet()
  }

  fun get_unslashed_attesting_indices(
      state: BeaconState,
      attestations: Sequence<PendingAttestation>
  ): Set<ValidatorIndex> {
    var output = setOf<ValidatorIndex>()
    for (a in attestations) {
      output = output.union(get_attesting_indices(state, a.data, a.aggregation_bits))
    }
    return output.filter { index -> !state.validators[index].slashed }.toSet()
  }

  fun get_attesting_balance(state: BeaconState, attestations: Sequence<PendingAttestation>): Gwei {
    return get_total_balance(state, get_unslashed_attesting_indices(state, attestations))
  }

  fun process_justification_and_finalization(state: BeaconState) {
    if (get_current_epoch(state) <= GENESIS_EPOCH + 1u) {
      return
    }

    val previous_epoch = get_previous_epoch(state)
    val current_epoch = get_current_epoch(state)
    val old_previous_justified_checkpoint = state.previous_justified_checkpoint
    val old_current_justified_checkpoint = state.current_justified_checkpoint

    // Process justifications
    state.previous_justified_checkpoint = state.current_justified_checkpoint

    for (i in 0 until len(state.justification_bits) - 1) {
      state.justification_bits[i + 1] = state.justification_bits[i]
    }
    state.justification_bits[0] = false
    var matching_target_attestations = get_matching_target_attestations(state, previous_epoch)  // Previous epoch
    if (get_attesting_balance(state, matching_target_attestations) * 3u >= get_total_active_balance(state) * 2u) {
      state.current_justified_checkpoint = Checkpoint(
          epoch = previous_epoch,
          root = get_block_root(state, previous_epoch)
      )
      state.justification_bits[1] = true
    }
    matching_target_attestations = get_matching_target_attestations(state, current_epoch)  // Current epoch
    if (get_attesting_balance(state, matching_target_attestations) * 3u >= get_total_active_balance(state) * 2u) {
      state.current_justified_checkpoint = Checkpoint(
          epoch = current_epoch,
          root = get_block_root(state, current_epoch)
      )
      state.justification_bits[0] = true
    }

    // Process finalizations
    val bits = state.justification_bits
    // The 2nd/3rd/4th most recent epochs are justified, the 2nd using the 4th as source
    if (all(bits.slice(1 until 4)) && old_previous_justified_checkpoint.epoch + 3u == current_epoch) {
      state.finalized_checkpoint = old_previous_justified_checkpoint
    }
    // The 2nd/3rd most recent epochs are justified, the 2nd using the 3rd as source
    if (all(bits.slice(1 until 3)) && old_previous_justified_checkpoint.epoch + 2u == current_epoch) {
      state.finalized_checkpoint = old_previous_justified_checkpoint
    }
    // The 1st/2nd/3rd most recent epochs are justified, the 1st using the 3rd as source
    if (all(bits.slice(0 until 3)) && old_current_justified_checkpoint.epoch + 2u == current_epoch) {
      state.finalized_checkpoint = old_current_justified_checkpoint
    }
    // The 1st/2nd most recent epochs are justified, the 1st using the 2nd as source
    if (all(bits.slice(0 until 2)) && old_current_justified_checkpoint.epoch + 1u == current_epoch) {
      state.finalized_checkpoint = old_current_justified_checkpoint
    }
  }

  fun get_base_reward(state: BeaconState, index: ValidatorIndex): Gwei {
    val total_balance = get_total_active_balance(state)
    val effective_balance = state.validators[index].effective_balance
    return effective_balance * BASE_REWARD_FACTOR / integer_squareroot(total_balance) / BASE_REWARDS_PER_EPOCH
  }

  fun get_attestation_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
    val previous_epoch = get_previous_epoch(state)
    val total_balance = get_total_active_balance(state)
    val rewards = state.validators.map { 0uL }.toMutableList()
    val penalties = state.validators.map { 0uL }.toMutableList()

    val eligible_validator_indices = enumerate(state.validators).filter { (_, validator) ->
      is_active_validator(validator, previous_epoch)
          || (validator.slashed && previous_epoch + 1u < validator.withdrawable_epoch)
    }
        .map { it.first.toULong() }

    // Micro-incentives for (matching FFG source, FFG target, and head
    val matching_source_attestations = get_matching_source_attestations(state, previous_epoch)
    val matching_target_attestations = get_matching_target_attestations(state, previous_epoch)
    val matching_head_attestations = get_matching_head_attestations(state, previous_epoch)
    for (attestations in listOf(
        matching_source_attestations,
        matching_target_attestations,
        matching_head_attestations
    )) {
      val unslashed_attesting_indices = get_unslashed_attesting_indices(state, attestations)
      val attesting_balance = get_total_balance(state, unslashed_attesting_indices)
      for (index in eligible_validator_indices) {
        if (index in unslashed_attesting_indices) {
          rewards[index] += get_base_reward(state, index) * attesting_balance / total_balance
        } else {
          penalties[index] += get_base_reward(state, index)
        }
      }
    }

    // Proposer and inclusion delay micro-rewards
    for (index in get_unslashed_attesting_indices(state, matching_source_attestations)) {
      val attestation = matching_source_attestations
          .filter { a -> index in get_attesting_indices(state, a.data, a.aggregation_bits) }
          .minBy { a -> a.inclusion_delay }!!
      val proposer_reward = get_base_reward(state, index) / PROPOSER_REWARD_QUOTIENT
      rewards[attestation.proposer_index] += proposer_reward
      val max_attester_reward = get_base_reward(state, index) - proposer_reward
      rewards[index] += max_attester_reward / attestation.inclusion_delay
    }
    // Inactivity penalty
    val finality_delay = previous_epoch - state.finalized_checkpoint.epoch
    if (finality_delay > MIN_EPOCHS_TO_INACTIVITY_PENALTY) {
      val matching_target_attesting_indices = get_unslashed_attesting_indices(state, matching_target_attestations)
      for (index in eligible_validator_indices) {
        penalties[index] += BASE_REWARDS_PER_EPOCH * get_base_reward(state, index)
        if (index !in matching_target_attesting_indices) {
          val effective_balance = state.validators[index].effective_balance
          penalties[index] += effective_balance * finality_delay / INACTIVITY_PENALTY_QUOTIENT
        }
      }
    }
    return Pair(rewards, penalties)
  }

  fun process_rewards_and_penalties(state: BeaconState) {
    if (get_current_epoch(state) == GENESIS_EPOCH) {
      return
    }

    val p = get_attestation_deltas(state)
    val rewards = p.first
    val penalties = p.second
    for (index in range(len(state.validators))) {
      increase_balance(state, index, rewards[index])
      decrease_balance(state, index, penalties[index])
    }
  }

  fun process_registry_updates(state: BeaconState) {
    // Process activation eligibility and ejections
    for ((index, validator) in enumerate(state.validators)) {
      if (is_eligible_for_activation_queue(validator)) {
        validator.activation_eligibility_epoch = get_current_epoch(state) + 1u
      }
      if (is_active_validator(validator, get_current_epoch(state)) && validator.effective_balance <= EJECTION_BALANCE) {
        initiate_validator_exit(state, index.toULong())
      }
    }

    // Queue validators eligible for (activation and not yet dequeued for (activation
    val activation_queue = enumerate(state.validators).filter { (_,validator) -> is_eligible_for_activation(state, validator) }
        .map { it.first }.sortedBy { index -> Tuple2(state.validators[index].activation_eligibility_epoch, index) }

    // Dequeued validators for (activation up to churn limit
    for (index in activation_queue.subList(0, get_validator_churn_limit(state).toInt())) {
      val validator = state.validators[index]
      validator.activation_epoch = compute_activation_exit_epoch(get_current_epoch(state))
    }
  }


  fun process_slashings(state: BeaconState) {
    val epoch = get_current_epoch(state)
    val total_balance = get_total_active_balance(state)
    state.validators.forEachIndexed { index, validator ->
      if (validator.slashed && epoch + EPOCHS_PER_SLASHINGS_VECTOR / 2u == validator.withdrawable_epoch) {
        val increment = EFFECTIVE_BALANCE_INCREMENT  // Factored out from penalty numerator to avoid uint64 overflow
        val penalty_numerator = validator.effective_balance / increment * min(state.slashings.sum() * 3u, total_balance)
        val penalty = penalty_numerator / total_balance * increment
        decrease_balance(state, index.toULong(), penalty)
      }
    }
  }

  fun process_final_updates(state: BeaconState) {
    val current_epoch = get_current_epoch(state)
    val next_epoch = current_epoch + 1u
    // Reset eth1 data votes
    if ((state.slot + 1u) % SLOTS_PER_ETH1_VOTING_PERIOD == 0uL) {
      state.eth1_data_votes = mutableListOf()
    }
    // Update effective balances with hysteresis
    state.validators.forEachIndexed { index, validator ->
      val balance = state.balances[index]
      val HALF_INCREMENT = EFFECTIVE_BALANCE_INCREMENT / 2u
      if (balance < validator.effective_balance || validator.effective_balance + 3u * HALF_INCREMENT < balance) {
        validator.effective_balance = min(balance - balance % EFFECTIVE_BALANCE_INCREMENT, MAX_EFFECTIVE_BALANCE)
      }
    }
    // Reset slashings
    state.slashings[(next_epoch % EPOCHS_PER_SLASHINGS_VECTOR)] = 0u
    // Set randao mix
    state.randao_mixes[(next_epoch % EPOCHS_PER_HISTORICAL_VECTOR)] = get_randao_mix(state, current_epoch)
    // Set historical root accumulator
    //if (next_epoch % (SLOTS_PER_HISTORICAL_ROOT / SLOTS_PER_EPOCH) == 0L) {
    //  val historical_batch = HistoricalBatch(block_roots = state.block_roots, state_roots = state.state_roots)
    //  state.historical_roots.add(hash_tree_root(historical_batch))
    //}
    // Rotate current/previous epoch attestations
    state.previous_epoch_attestations = state.current_epoch_attestations
    state.current_epoch_attestations = mutableListOf()
  }


  fun xor(bytes_1: Bytes32, bytes_2: Bytes32): Bytes32 = bytes_1.xor(bytes_2)

  fun process_block(state: BeaconState, block: BeaconBlock) {
    process_block_header(state, block)
    process_randao(state, block.body)
    process_eth1_data(state, block.body)
    process_operations(state, block.body)
  }

  fun process_block_header(state: BeaconState, block: BeaconBlock) {
    // Verify that the slots match
    assert(block.slot == state.slot)
    // Verify that the parent matches
    assert(block.parent_root == hash_tree_root(state.latest_block_header))
    // Cache current block as the new latest block
    state.latest_block_header = BeaconBlockHeader(
        slot = block.slot,
        parent_root = block.parent_root,
        state_root = Bytes32.ZERO,  // Overwritten in the next process_slot call
        body_root = hash_tree_root(block.body)
    )

    // Verify proposer is not slashed
    val proposer = state.validators[get_beacon_proposer_index(state)]
    assert(!proposer.slashed)
  }

  fun process_randao(state: BeaconState, body: BeaconBlockBody) {
    val epoch = get_current_epoch(state)
    // Verify RANDAO reveal
    val proposer = state.validators[get_beacon_proposer_index(state)]
    val signing_root = compute_signing_root(epoch, get_domain(state, DOMAIN_RANDAO))
    assert(Verify(proposer.pubkey, signing_root, body.randao_reveal))
    // Mix in RANDAO reveal
    val mix = xor(get_randao_mix(state, epoch), hash(body.randao_reveal))
    state.randao_mixes[(epoch % EPOCHS_PER_HISTORICAL_VECTOR)] = mix
  }

  fun process_eth1_data(state: BeaconState, body: BeaconBlockBody) {
    state.eth1_data_votes.add(body.eth1_data)

    if (state.eth1_data_votes.count { it == body.eth1_data }.toULong() * 2u > SLOTS_PER_ETH1_VOTING_PERIOD) {
      state.eth1_data = body.eth1_data
    }
  }

  fun process_operations(state: BeaconState, body: BeaconBlockBody) {
    // Verify that outstanding deposits are processed up to the maximum number of deposits
    assert(len(body.deposits) == min(MAX_DEPOSITS, state.eth1_data.deposit_count - state.eth1_deposit_index))

    for (operation in body.proposer_slashings) {
      process_proposer_slashing(state, operation)
    }
    for (operation in body.attester_slashings) {
      process_attester_slashing(state, operation)
    }
    for (operation in body.attestations) {
      process_attestation(state, operation)
    }
    for (operation in body.deposits) {
      process_deposit(state, operation)
    }
    for (operation in body.voluntary_exits) {
      process_voluntary_exit(state, operation)
    }
  }

  fun process_proposer_slashing(state: BeaconState, proposer_slashing: ProposerSlashing) {
    // Verify header slots match
    assert(proposer_slashing.signed_header_1.message.slot == proposer_slashing.signed_header_2.message.slot)
    // Verify the headers are different
    assert(proposer_slashing.signed_header_1 != proposer_slashing.signed_header_2)
    // Verify the proposer is slashable
    val proposer = state.validators[proposer_slashing.proposer_index]
    assert(is_slashable_validator(proposer, get_current_epoch(state)))
    // Verify signatures
    for (signed_header in listOf(proposer_slashing.signed_header_1, proposer_slashing.signed_header_2)) {
      val domain = get_domain(state, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(signed_header.message.slot))
      val signing_root = compute_signing_root(signed_header.message, domain)
      assert(Verify(proposer.pubkey, signing_root, signed_header.signature))
    }

    slash_validator(state, proposer_slashing.proposer_index)
  }

  fun get_indexed_attestation(state: BeaconState, attestation: Attestation): IndexedAttestation {
    /*
      Return the indexed attestation corresponding to ``attestation``.
      */
    val attesting_indices = get_attesting_indices(state, attestation.data, attestation.aggregation_bits)

    return IndexedAttestation(
        attesting_indices = attesting_indices.sorted(),
        data = attestation.data,
        signature = attestation.signature
    )
  }

  fun is_valid_indexed_attestation(state: BeaconState, indexed_attestation: IndexedAttestation): Boolean {
    val indices = indexed_attestation.attesting_indices

    if (!(len(indices) <= MAX_VALIDATORS_PER_COMMITTEE)) {
      return false
    }
    if (!(indices == indices.toSortedSet())) {
      return false
    }
    val pubkeys = indices.map { i -> state.validators[i].pubkey }
    val domain = get_domain(state, DOMAIN_BEACON_ATTESTER, indexed_attestation.data.target.epoch)
    val signing_root = compute_signing_root(indexed_attestation.data, domain)
    return FastAggregateVerify(pubkeys, signing_root, indexed_attestation.signature)
  }

  fun is_slashable_attestation_data(data_1: AttestationData, data_2: AttestationData): Boolean {
    return (
        // Double vote
        (data_1 != data_2 && data_1.target.epoch == data_2.target.epoch) or
            // Surround vote
            (data_1.source.epoch < data_2.source.epoch && data_2.target.epoch < data_1.target.epoch)
        )
  }

  fun process_attester_slashing(state: BeaconState, attester_slashing: AttesterSlashing) {
    val attestation_1 = attester_slashing.attestation_1
    val attestation_2 = attester_slashing.attestation_2
    assert(is_slashable_attestation_data(attestation_1.data, attestation_2.data))
    assert(is_valid_indexed_attestation(state, attestation_1))
    assert(is_valid_indexed_attestation(state, attestation_2))

    var slashed_any = false
    val indices = attestation_1.attesting_indices.toSet().intersect(attestation_2.attesting_indices)
    for (index in indices.toSortedSet()) {
      if (is_slashable_validator(state.validators[index], get_current_epoch(state))) {
        slash_validator(state, index)
        slashed_any = true
      }
    }
    assert(slashed_any)
  }

  fun process_attestation(state: BeaconState, attestation: Attestation) {
    val data = attestation.data
    assert(data.index < get_committee_count_at_slot(state, data.slot))
    assert(data.target.epoch in listOf(get_previous_epoch(state), get_current_epoch(state)))
    assert(data.target.epoch == compute_epoch_at_slot(data.slot))
    assert(data.slot + MIN_ATTESTATION_INCLUSION_DELAY <= state.slot && state.slot <= data.slot + SLOTS_PER_EPOCH)

    val committee = get_beacon_committee(state, data.slot, data.index)
    assert(len(attestation.aggregation_bits).toULong() == len(committee))

    val pending_attestation = PendingAttestation(
        data = data,
        aggregation_bits = attestation.aggregation_bits,
        inclusion_delay = state.slot - data.slot,
        proposer_index = get_beacon_proposer_index(state)
    )

    if (data.target.epoch == get_current_epoch(state)) {
      assert(data.source == state.current_justified_checkpoint)
      state.current_epoch_attestations.add(pending_attestation)
    } else {
      assert(data.source == state.previous_justified_checkpoint)
      state.previous_epoch_attestations.add(pending_attestation)
    }
    // Verify signature
    assert(is_valid_indexed_attestation(state, get_indexed_attestation(state, attestation)))
  }

  fun is_valid_merkle_branch(
      leaf: Bytes32,
      branch: Sequence<Bytes32>,
      depth: uint64,
      index: uint64,
      root: Root
  ): Boolean {
    /*
    Check if ``leaf`` at ``index`` verifies against the Merkle ``root`` and ``branch``.
    */
    var value = leaf
    for (i in 0 until depth.toInt()) {
      value = if ((index / (1u shl i)) % 2u == 1uL) {
        hash(branch[i] + value)
      } else {
        hash(value + branch[i])
      }
    }
    return value == root
  }

  fun process_deposit(state: BeaconState, deposit: Deposit) {
    // Verify the Merkle branch
    assert(
        is_valid_merkle_branch(
            leaf = hash_tree_root(deposit.data),
            branch = deposit.proof,
            depth = DEPOSIT_CONTRACT_TREE_DEPTH + 1u,  // Add 1 for (the List length mix-in
            index = state.eth1_deposit_index,
            root = state.eth1_data.deposit_root
        )
    )

    // Deposits must be processed in order
    state.eth1_deposit_index += 1u

    val pubkey = deposit.data.pubkey
    val amount = deposit.data.amount
    val validator_pubkeys = state.validators.map { v -> v.pubkey }
    if (pubkey !in validator_pubkeys) {
      // Verify the deposit signature (proof of possession) which is not checked by the deposit contract
      val deposit_message = DepositMessage(
          pubkey = deposit.data.pubkey,
          withdrawal_credentials = deposit.data.withdrawal_credentials,
          amount = deposit.data.amount
      )
      val domain = compute_domain(DOMAIN_DEPOSIT)  // Fork-agnostic domain since deposits are valid across forks
      val signing_root = compute_signing_root(deposit_message, domain)
      if (!(Verify(pubkey, signing_root, deposit.data.signature))) {
        return
      }

      // Add validator and balance entries
      state.validators.add(
          IValidator(
              pubkey = pubkey,
              withdrawal_credentials = deposit.data.withdrawal_credentials,
              activation_eligibility_epoch = FAR_FUTURE_EPOCH,
              activation_epoch = FAR_FUTURE_EPOCH,
              exit_epoch = FAR_FUTURE_EPOCH,
              withdrawable_epoch = FAR_FUTURE_EPOCH,
              effective_balance = min(amount - amount % EFFECTIVE_BALANCE_INCREMENT, MAX_EFFECTIVE_BALANCE),
              slashed = false
          )
      )
      state.balances.add(amount)
    } else {
      // Increase balance by deposit amount
      val index = validator_pubkeys.indexOf(pubkey).toULong()
      increase_balance(state, index, amount)
    }
  }

  fun process_voluntary_exit(state: BeaconState, signed_voluntary_exit: SignedVoluntaryExit) {
    val voluntary_exit = signed_voluntary_exit.message
    val validator = state.validators[voluntary_exit.validator_index]
    // Verify the validator is active
    assert(is_active_validator(validator, get_current_epoch(state)))
    // Verify exit has not been initiated
    assert(validator.exit_epoch == FAR_FUTURE_EPOCH)
    // Exits must specify an epoch when they become valid; they are not valid before then
    assert(get_current_epoch(state) >= voluntary_exit.epoch)
    // Verify the validator has been active long enough
    assert(get_current_epoch(state) >= validator.activation_epoch + PERSISTENT_COMMITTEE_PERIOD)
    // Verify signature
    val domain = get_domain(state, DOMAIN_VOLUNTARY_EXIT, voluntary_exit.epoch)
    val signing_root = compute_signing_root(voluntary_exit, domain)
    assert(Verify(validator.pubkey, signing_root, signed_voluntary_exit.signature))
    // Initiate exit
    initiate_validator_exit(state, voluntary_exit.validator_index)
  }

}
