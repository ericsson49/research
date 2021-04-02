const CONFIG_NAME := "mainnet";
const GENESIS_SLOT := new Slot(0);
const GENESIS_EPOCH := new Epoch(0);
const FAR_FUTURE_EPOCH := new Epoch(2.pow(64) - 1);
const BASE_REWARDS_PER_EPOCH := new uint64(4);
const DEPOSIT_CONTRACT_TREE_DEPTH := new uint64(2.pow(5));
const JUSTIFICATION_BITS_LENGTH := new uint64(4);
const ENDIANNESS := "little";
const ETH1_FOLLOW_DISTANCE := new uint64(2.pow(11));
const MAX_COMMITTEES_PER_SLOT := new uint64(2.pow(6));
const TARGET_COMMITTEE_SIZE := new uint64(2.pow(7));
const MAX_VALIDATORS_PER_COMMITTEE := new uint64(2.pow(11));
const MIN_PER_EPOCH_CHURN_LIMIT := new uint64(2.pow(2));
const CHURN_LIMIT_QUOTIENT := new uint64(2.pow(16));
const SHUFFLE_ROUND_COUNT := new uint64(90);
const MIN_GENESIS_ACTIVE_VALIDATOR_COUNT := new uint64(2.pow(14));
const MIN_GENESIS_TIME := new uint64(1606824000);
const HYSTERESIS_QUOTIENT := new uint64(4);
const HYSTERESIS_DOWNWARD_MULTIPLIER := new uint64(1);
const HYSTERESIS_UPWARD_MULTIPLIER := new uint64(5);
const MIN_DEPOSIT_AMOUNT := new Gwei(2.pow(0) * 10.pow(9));
const MAX_EFFECTIVE_BALANCE := new Gwei(2.pow(5) * 10.pow(9));
const EJECTION_BALANCE := new Gwei(2.pow(4) * 10.pow(9));
const EFFECTIVE_BALANCE_INCREMENT := new Gwei(2.pow(0) * 10.pow(9));
const GENESIS_FORK_VERSION := new Version("0x00000000");
const BLS_WITHDRAWAL_PREFIX := new Bytes1("0x00");
const ETH1_ADDRESS_WITHDRAWAL_PREFIX := new Bytes1("0x01");
const GENESIS_DELAY := new uint64(604800);
const SECONDS_PER_SLOT := new uint64(12);
const SECONDS_PER_ETH1_BLOCK := new uint64(14);
const MIN_ATTESTATION_INCLUSION_DELAY := new uint64(2.pow(0));
const SLOTS_PER_EPOCH := new uint64(2.pow(5));
const MIN_SEED_LOOKAHEAD := new uint64(2.pow(0));
const MAX_SEED_LOOKAHEAD := new uint64(2.pow(2));
const MIN_EPOCHS_TO_INACTIVITY_PENALTY := new uint64(2.pow(2));
const EPOCHS_PER_ETH1_VOTING_PERIOD := new uint64(2.pow(6));
const SLOTS_PER_HISTORICAL_ROOT := new uint64(2.pow(13));
const MIN_VALIDATOR_WITHDRAWABILITY_DELAY := new uint64(2.pow(8));
const SHARD_COMMITTEE_PERIOD := new uint64(2.pow(8));
const EPOCHS_PER_HISTORICAL_VECTOR := new uint64(2.pow(16));
const EPOCHS_PER_SLASHINGS_VECTOR := new uint64(2.pow(13));
const HISTORICAL_ROOTS_LIMIT := new uint64(2.pow(24));
const VALIDATOR_REGISTRY_LIMIT := new uint64(2.pow(40));
const BASE_REWARD_FACTOR := new uint64(2.pow(6));
const WHISTLEBLOWER_REWARD_QUOTIENT := new uint64(2.pow(9));
const PROPOSER_REWARD_QUOTIENT := new uint64(2.pow(3));
const INACTIVITY_PENALTY_QUOTIENT := new uint64(2.pow(26));
const MIN_SLASHING_PENALTY_QUOTIENT := new uint64(2.pow(7));
const PROPORTIONAL_SLASHING_MULTIPLIER := new uint64(1);
const MAX_PROPOSER_SLASHINGS := 2.pow(4);
const MAX_ATTESTER_SLASHINGS := 2.pow(1);
const MAX_ATTESTATIONS := 2.pow(7);
const MAX_DEPOSITS := 2.pow(4);
const MAX_VOLUNTARY_EXITS := 2.pow(4);
const DOMAIN_BEACON_PROPOSER := new DomainType("0x00000000");
const DOMAIN_BEACON_ATTESTER := new DomainType("0x01000000");
const DOMAIN_RANDAO := new DomainType("0x02000000");
const DOMAIN_DEPOSIT := new DomainType("0x03000000");
const DOMAIN_VOLUNTARY_EXIT := new DomainType("0x04000000");
const DOMAIN_SELECTION_PROOF := new DomainType("0x05000000");
const DOMAIN_AGGREGATE_AND_PROOF := new DomainType("0x06000000");
const SAFE_SLOTS_TO_UPDATE_JUSTIFIED := 2.pow(3);
const TARGET_AGGREGATORS_PER_COMMITTEE := 2.pow(4);
const RANDOM_SUBNETS_PER_VALIDATOR := 2.pow(0);
const EPOCHS_PER_RANDOM_SUBNET_SUBSCRIPTION := 2.pow(8);
const ATTESTATION_SUBNET_COUNT := 64;
const ETH_TO_GWEI := new uint64(10.pow(9));
const SAFETY_DECAY := new uint64(10);
type Slot = uint64
type Epoch = uint64
type CommitteeIndex = uint64
type ValidatorIndex = uint64
type Gwei = uint64
type Root = Bytes32
type Version = Bytes4
type DomainType = Bytes4
type ForkDigest = Bytes4
type Domain = Bytes32
type BLSPubkey = Bytes48
type BLSSignature = Bytes96
type Ether = uint64
datatype Fork = Fork(
  previous_version: Version,
  current_version: Version,
  epoch: Epoch
)
datatype ForkData = ForkData(
  current_version: Version,
  genesis_validators_root: Root
)
datatype Checkpoint = Checkpoint(
  epoch: Epoch,
  root: Root
)
datatype Validator = Validator(
  pubkey: BLSPubkey,
  withdrawal_credentials: Bytes32,
  effective_balance: Gwei,
  slashed: boolean,
  activation_eligibility_epoch: Epoch,
  activation_epoch: Epoch,
  exit_epoch: Epoch,
  withdrawable_epoch: Epoch
)
datatype AttestationData = AttestationData(
  slot: Slot,
  index: CommitteeIndex,
  beacon_block_root: Root,
  source: Checkpoint,
  target: Checkpoint
)
datatype IndexedAttestation = IndexedAttestation(
  attesting_indices: List<ValidatorIndex>,
  data: AttestationData,
  signature: BLSSignature
)
datatype PendingAttestation = PendingAttestation(
  aggregation_bits: Bitlist,
  data: AttestationData,
  inclusion_delay: Slot,
  proposer_index: ValidatorIndex
)
datatype Eth1Data = Eth1Data(
  deposit_root: Root,
  deposit_count: uint64,
  block_hash: Bytes32
)
datatype HistoricalBatch = HistoricalBatch(
  block_roots: Vector<Root>,
  state_roots: Vector<Root>
)
datatype DepositMessage = DepositMessage(
  pubkey: BLSPubkey,
  withdrawal_credentials: Bytes32,
  amount: Gwei
)
datatype DepositData = DepositData(
  pubkey: BLSPubkey,
  withdrawal_credentials: Bytes32,
  amount: Gwei,
  signature: BLSSignature
)
datatype BeaconBlockHeader = BeaconBlockHeader(
  slot: Slot,
  proposer_index: ValidatorIndex,
  parent_root: Root,
  state_root: Root,
  body_root: Root
)
datatype SigningData = SigningData(
  object_root: Root,
  domain: Domain
)
datatype AttesterSlashing = AttesterSlashing(
  attestation_1: IndexedAttestation,
  attestation_2: IndexedAttestation
)
datatype Attestation = Attestation(
  aggregation_bits: Bitlist,
  data: AttestationData,
  signature: BLSSignature
)
datatype Deposit = Deposit(
  proof: Vector<Bytes32>,
  data: DepositData
)
datatype VoluntaryExit = VoluntaryExit(
  epoch: Epoch,
  validator_index: ValidatorIndex
)
datatype BeaconState = BeaconState(
  genesis_time: uint64,
  genesis_validators_root: Root,
  slot: Slot,
  fork: Fork,
  latest_block_header: BeaconBlockHeader,
  block_roots: Vector<Root>,
  state_roots: Vector<Root>,
  historical_roots: List<Root>,
  eth1_data: Eth1Data,
  eth1_data_votes: List<Eth1Data>,
  eth1_deposit_index: uint64,
  validators: List<Validator>,
  balances: List<Gwei>,
  randao_mixes: Vector<Bytes32>,
  slashings: Vector<Gwei>,
  previous_epoch_attestations: List<PendingAttestation>,
  current_epoch_attestations: List<PendingAttestation>,
  justification_bits: Bitvector,
  previous_justified_checkpoint: Checkpoint,
  current_justified_checkpoint: Checkpoint,
  finalized_checkpoint: Checkpoint
)
datatype SignedVoluntaryExit = SignedVoluntaryExit(
  message: VoluntaryExit,
  signature: BLSSignature
)
datatype SignedBeaconBlockHeader = SignedBeaconBlockHeader(
  message: BeaconBlockHeader,
  signature: BLSSignature
)
datatype ProposerSlashing = ProposerSlashing(
  signed_header_1: SignedBeaconBlockHeader,
  signed_header_2: SignedBeaconBlockHeader
)
datatype BeaconBlockBody = BeaconBlockBody(
  randao_reveal: BLSSignature,
  eth1_data: Eth1Data,
  graffiti: Bytes32,
  proposer_slashings: List<ProposerSlashing>,
  attester_slashings: List<AttesterSlashing>,
  attestations: List<Attestation>,
  deposits: List<Deposit>,
  voluntary_exits: List<SignedVoluntaryExit>
)
datatype BeaconBlock = BeaconBlock(
  slot: Slot,
  proposer_index: ValidatorIndex,
  parent_root: Root,
  state_root: Root,
  body: BeaconBlockBody
)
datatype SignedBeaconBlock = SignedBeaconBlock(
  message: BeaconBlock,
  signature: BLSSignature
)
datatype Eth1Block = Eth1Block(
  timestamp: uint64,
  deposit_root: Root,
  deposit_count: uint64
)
datatype AggregateAndProof = AggregateAndProof(
  aggregator_index: ValidatorIndex,
  aggregate: Attestation,
  selection_proof: BLSSignature
)
datatype SignedAggregateAndProof = SignedAggregateAndProof(
  message: AggregateAndProof,
  signature: BLSSignature
)
datatype LatestMessage = LatestMessage(
  epoch: Epoch,
  root: Root
)
datatype Store = Store(
  time: uint64,
  genesis_time: uint64,
  justified_checkpoint: Checkpoint,
  finalized_checkpoint: Checkpoint,
  best_justified_checkpoint: Checkpoint,
  blocks: Dict<Root,BeaconBlock>,
  block_states: Dict<Root,BeaconState>,
  checkpoint_states: Dict<Checkpoint,BeaconState>,
  latest_messages: Dict<ValidatorIndex,LatestMessage>
)
/*
    Return the largest integer ``x`` such that ``x**2 <= n``.
    */
function method integer_squareroot(n_0: uint64): uint64 {
  var x_0 := n_0;
  var y_0 := (x_0 + 1) / 2;
  var x_2 := x_0;
  var y_2 := y_0;
  while y_2 < x_2 {
    var x_1 := y_2;
    var y_1 := (x_1 + (n_0 / x_1)) / 2;
    x_2 := x_1;
    y_2 := y_1;
  }
  return x_2;
}

/*
    Return the exclusive-or of two 32-byte strings.
    */
function method xor(bytes_1_0: Bytes32,bytes_2_0: Bytes32): Bytes32 {
  return new Bytes32(zip(bytes_1_0, bytes_2_0).map((tmp_0) => var a := tmp_0.first; var b := tmp_0.second; a xor b));
}

/*
    Return the integer deserialization of ``data`` interpreted as ``ENDIANNESS``-endian.
    */
function method bytes_to_uint64(data_0: bytes): uint64 {
  return new uint64(int.from_bytes(data_0, ENDIANNESS));
}

/*
    Check if ``validator`` is active.
    */
function method is_active_validator(validator_0: Validator,epoch_0: Epoch): bool {
  return (validator_0.activation_epoch <= epoch_0) && (epoch_0 < validator_0.exit_epoch);
}

/*
    Check if ``validator`` is eligible to be placed into the activation queue.
    */
function method is_eligible_for_activation_queue(validator_0: Validator): bool {
  return (validator_0.activation_eligibility_epoch == FAR_FUTURE_EPOCH) && (validator_0.effective_balance == MAX_EFFECTIVE_BALANCE);
}

/*
    Check if ``validator`` is eligible for activation.
    */
function method is_eligible_for_activation(state_0: BeaconState,validator_0: Validator): bool {
  return (validator_0.activation_eligibility_epoch <= state_0.finalized_checkpoint.epoch) && (validator_0.activation_epoch == FAR_FUTURE_EPOCH);
}

/*
    Check if ``validator`` is slashable.
    */
function method is_slashable_validator(validator_0: Validator,epoch_0: Epoch): bool {
  return !validator_0.slashed && ((validator_0.activation_epoch <= epoch_0) && (epoch_0 < validator_0.withdrawable_epoch));
}

/*
    Check if ``data_1`` and ``data_2`` are slashable according to Casper FFG rules.
    */
function method is_slashable_attestation_data(data_1_0: AttestationData,data_2_0: AttestationData): bool {
  return ((data_1_0 != data_2_0) && (data_1_0.target.epoch == data_2_0.target.epoch)) || ((data_1_0.source.epoch < data_2_0.source.epoch) && (data_2_0.target.epoch < data_1_0.target.epoch));
}

/*
    Check if ``indexed_attestation`` is not empty, has sorted and unique indices and has a valid aggregate signature.
    */
function method is_valid_indexed_attestation(state_0: BeaconState,indexed_attestation_0: IndexedAttestation): bool {
  var indices_0 := indexed_attestation_0.attesting_indices;
  if (len(indices_0) == 0) || !(indices_0 == sorted(set(indices_0))) {
    return false;
  }
  var pubkeys_0 := indices_0.map((i) => state_0.validators[i].pubkey);
  var domain_0 := get_domain(state_0, DOMAIN_BEACON_ATTESTER, indexed_attestation_0.data.target.epoch);
  var signing_root_0 := compute_signing_root(indexed_attestation_0.data, domain_0);
  return bls.FastAggregateVerify(pubkeys_0, signing_root_0, indexed_attestation_0.signature);
}

/*
    Check if ``leaf`` at ``index`` verifies against the Merkle ``root`` and ``branch``.
    */
function method is_valid_merkle_branch(leaf_0: Bytes32,branch_0: Sequence<Bytes32>,depth_0: uint64,index_0: uint64,root_0: Root): bool {
  var value_0 := leaf_0;
  var value_3 := value_0;
  var i1 := 0;
var coll2 := i1;
while i1 < |coll2|
  decreases |coll2| - i
{
var i_0 := coll2[i1];
    if pybool(index_0 / 2.pow(i_0) % 2) {
      var value_1 := hash(branch_0[i_0] + value_3);
      value_3 := value_1;
    } else {
      var value_2 := hash(value_3 + branch_0[i_0]);
      value_3 := value_2;
    }
  }
  return value_3 == root_0;
}

/*
    Return the shuffled index corresponding to ``seed`` (and ``index_count``).
    */
function method compute_shuffled_index(index_0: uint64,index_count_0: uint64,seed_0: Bytes32): uint64 {
  assert(index_0 < index_count_0)
  var index_2 := index_0;
  var i3 := 0;
var coll4 := i3;
while i3 < |coll4|
  decreases |coll4| - i
{
var current_round_0 := coll4[i3];
    var pivot_0 := bytes_to_uint64(hash(seed_0 + uint_to_bytes(new uint8(current_round_0)))[0..8]) % index_count_0;
    var flip_0 := (pivot_0 + index_count_0 - index_2) % index_count_0;
    var position_0 := max(index_2, flip_0);
    var source_0 := hash(seed_0 + uint_to_bytes(new uint8(current_round_0)) + uint_to_bytes(new uint32(position_0 / 256)));
    var byte_0 := new uint8(source_0[position_0 % 256 / 8]);
    var bit_0 := (byte_0 shr (position_0 % 8)) % 2;
    var index_1 := if (pybool(bit_0)) then flip_0 else index_2;
    index_2 := index_1;
  }
  return index_2;
}

/*
    Return from ``indices`` a random index sampled by effective balance.
    */
function method compute_proposer_index(state_0: BeaconState,indices_0: Sequence<ValidatorIndex>,seed_0: Bytes32): ValidatorIndex {
  assert(len(indices_0) > 0)
  var MAX_RANDOM_BYTE_0 := 2.pow(8) - 1;
  var i_0 := new uint64(0);
  var total_0 := new uint64(len(indices_0));
  var i_2 := i_0;
  while true {
    var candidate_index_0 := indices_0[compute_shuffled_index(i_2 % total_0, total_0, seed_0)];
    var random_byte_0 := hash(seed_0 + uint_to_bytes(new uint64(i_2 / 32)))[i_2 % 32];
    var effective_balance_0 := state_0.validators[candidate_index_0].effective_balance;
    if (effective_balance_0 * MAX_RANDOM_BYTE_0) >= (MAX_EFFECTIVE_BALANCE * random_byte_0) {
      return candidate_index_0;
    }
    var i_1 := i_2 + 1;
    i_2 := i_1;
  }
}

/*
    Return the committee corresponding to ``indices``, ``seed``, ``index``, and committee ``count``.
    */
function method compute_committee(indices_0: Sequence<ValidatorIndex>,seed_0: Bytes32,index_0: uint64,count_0: uint64): Sequence<ValidatorIndex> {
  var start_0 := len(indices_0) * index_0 / count_0;
  var end_0 := len(indices_0) * new uint64(index_0 + 1) / count_0;
  return range(start_0, end_0).map((i) => indices_0[compute_shuffled_index(new uint64(i), new uint64(len(indices_0)), seed_0)]);
}

/*
    Return the epoch number at ``slot``.
    */
function method compute_epoch_at_slot(slot_0: Slot): Epoch {
  return new Epoch(slot_0 / SLOTS_PER_EPOCH);
}

/*
    Return the start slot of ``epoch``.
    */
function method compute_start_slot_at_epoch(epoch_0: Epoch): Slot {
  return new Slot(epoch_0 * SLOTS_PER_EPOCH);
}

/*
    Return the epoch during which validator activations and exits initiated in ``epoch`` take effect.
    */
function method compute_activation_exit_epoch(epoch_0: Epoch): Epoch {
  return new Epoch(epoch_0 + 1 + MAX_SEED_LOOKAHEAD);
}

/*
    Return the 32-byte fork data root for the ``current_version`` and ``genesis_validators_root``.
    This is used primarily in signature domains to avoid collisions across forks/chains.
    */
function method compute_fork_data_root(current_version_0: Version,genesis_validators_root_0: Root): Root {
  return hash_tree_root(new ForkData(current_version_0, genesis_validators_root_0));
}

/*
    Return the 4-byte fork digest for the ``current_version`` and ``genesis_validators_root``.
    This is a digest primarily used for domain separation on the p2p layer.
    4-bytes suffices for practical separation of forks/chains.
    */
function method compute_fork_digest(current_version_0: Version,genesis_validators_root_0: Root): ForkDigest {
  return new ForkDigest(compute_fork_data_root(current_version_0, genesis_validators_root_0)[0..4]);
}

/*
    Return the domain for the ``domain_type`` and ``fork_version``.
    */
function method compute_domain(domain_type_0: DomainType,fork_version_0: Version,genesis_validators_root_0: Root): Domain {
  Version fork_version_2;
  if fork_version_0 == null {
    var fork_version_1 := GENESIS_FORK_VERSION;
    fork_version_2 := fork_version_1;
  } else {
    fork_version_2 := fork_version_0;
  }
  Root genesis_validators_root_2;
  if genesis_validators_root_0 == null {
    var genesis_validators_root_1 := new Root();
    genesis_validators_root_2 := genesis_validators_root_1;
  } else {
    genesis_validators_root_2 := genesis_validators_root_0;
  }
  var fork_data_root_0 := compute_fork_data_root(fork_version_2, genesis_validators_root_2);
  return new Domain(domain_type_0 + fork_data_root_0[0..28]);
}

/*
    Return the signing root for the corresponding signing data.
    */
function method compute_signing_root(ssz_object_0: object,domain_0: Domain): Root {
  return hash_tree_root(new SigningData(hash_tree_root(ssz_object_0), domain_0));
}

/*
    Return the current epoch.
    */
function method get_current_epoch(state_0: BeaconState): Epoch {
  return compute_epoch_at_slot(state_0.slot);
}

/*`
    Return the previous epoch (unless the current epoch is ``GENESIS_EPOCH``).
    */
function method get_previous_epoch(state_0: BeaconState): Epoch {
  var current_epoch_0 := get_current_epoch(state_0);
  return if (current_epoch_0 == GENESIS_EPOCH) then GENESIS_EPOCH else new Epoch(current_epoch_0 - 1);
}

/*
    Return the block root at the start of a recent ``epoch``.
    */
function method get_block_root(state_0: BeaconState,epoch_0: Epoch): Root {
  return get_block_root_at_slot(state_0, compute_start_slot_at_epoch(epoch_0));
}

/*
    Return the block root at a recent ``slot``.
    */
function method get_block_root_at_slot(state_0: BeaconState,slot_0: Slot): Root {
  assert((slot_0 < state_0.slot) && (state_0.slot <= (slot_0 + SLOTS_PER_HISTORICAL_ROOT)))
  return state_0.block_roots[slot_0 % SLOTS_PER_HISTORICAL_ROOT];
}

/*
    Return the randao mix at a recent ``epoch``.
    */
function method get_randao_mix(state_0: BeaconState,epoch_0: Epoch): Bytes32 {
  return state_0.randao_mixes[epoch_0 % EPOCHS_PER_HISTORICAL_VECTOR];
}

/*
    Return the sequence of active validator indices at ``epoch``.
    */
function method get_active_validator_indices(state_0: BeaconState,epoch_0: Epoch): Sequence<ValidatorIndex> {
  return enumerate(state_0.validators).filter((tmp_5) => var i := tmp_5.first; var v := tmp_5.second; is_active_validator(v, epoch_0)).map((tmp_6) => var i := tmp_6.first; var v := tmp_6.second; new ValidatorIndex(i));
}

/*
    Return the validator churn limit for the current epoch.
    */
function method get_validator_churn_limit(state_0: BeaconState): uint64 {
  var active_validator_indices_0 := get_active_validator_indices(state_0, get_current_epoch(state_0));
  return max(MIN_PER_EPOCH_CHURN_LIMIT, new uint64(len(active_validator_indices_0)) / CHURN_LIMIT_QUOTIENT);
}

/*
    Return the seed at ``epoch``.
    */
function method get_seed(state_0: BeaconState,epoch_0: Epoch,domain_type_0: DomainType): Bytes32 {
  var mix_0 := get_randao_mix(state_0, new Epoch(epoch_0 + EPOCHS_PER_HISTORICAL_VECTOR - MIN_SEED_LOOKAHEAD - 1));
  return hash(domain_type_0 + uint_to_bytes(epoch_0) + mix_0);
}

/*
    Return the number of committees in each slot for the given ``epoch``.
    */
function method get_committee_count_per_slot(state_0: BeaconState,epoch_0: Epoch): uint64 {
  return max(new uint64(1), min(MAX_COMMITTEES_PER_SLOT, new uint64(len(get_active_validator_indices(state_0, epoch_0))) / SLOTS_PER_EPOCH / TARGET_COMMITTEE_SIZE));
}

/*
    Return the beacon committee at ``slot`` for ``index``.
    */
function method get_beacon_committee(state_0: BeaconState,slot_0: Slot,index_0: CommitteeIndex): Sequence<ValidatorIndex> {
  var epoch_0 := compute_epoch_at_slot(slot_0);
  var committees_per_slot_0 := get_committee_count_per_slot(state_0, epoch_0);
  return compute_committee(get_active_validator_indices(state_0, epoch_0), get_seed(state_0, epoch_0, DOMAIN_BEACON_ATTESTER), (slot_0 % SLOTS_PER_EPOCH * committees_per_slot_0) + index_0, committees_per_slot_0 * SLOTS_PER_EPOCH);
}

/*
    Return the beacon proposer index at the current slot.
    */
function method get_beacon_proposer_index(state_0: BeaconState): ValidatorIndex {
  var epoch_0 := get_current_epoch(state_0);
  var seed_0 := hash(get_seed(state_0, epoch_0, DOMAIN_BEACON_PROPOSER) + uint_to_bytes(state_0.slot));
  var indices_0 := get_active_validator_indices(state_0, epoch_0);
  return compute_proposer_index(state_0, indices_0, seed_0);
}

/*
    Return the combined effective balance of the ``indices``.
    ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
    Math safe up to ~10B ETH, afterwhich this overflows uint64.
    */
function method get_total_balance(state_0: BeaconState,indices_0: Set<ValidatorIndex>): Gwei {
  return new Gwei(max(EFFECTIVE_BALANCE_INCREMENT, sum(indices_0.map((index) => state_0.validators[index].effective_balance))));
}

/*
    Return the combined effective balance of the active validators.
    Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
    */
function method get_total_active_balance(state_0: BeaconState): Gwei {
  return get_total_balance(state_0, set(get_active_validator_indices(state_0, get_current_epoch(state_0))));
}

/*
    Return the signature domain (fork version concatenated with domain type) of a message.
    */
function method get_domain(state_0: BeaconState,domain_type_0: DomainType,epoch_0: Epoch): Domain {
  var epoch_1 := if (epoch_0 == null) then get_current_epoch(state_0) else epoch_0;
  var fork_version_0 := if (epoch_1 < state_0.fork.epoch) then state_0.fork.previous_version else state_0.fork.current_version;
  return compute_domain(domain_type_0, fork_version_0, state_0.genesis_validators_root);
}

/*
    Return the indexed attestation corresponding to ``attestation``.
    */
function method get_indexed_attestation(state_0: BeaconState,attestation_0: Attestation): IndexedAttestation {
  var attesting_indices_0 := get_attesting_indices(state_0, attestation_0.data, attestation_0.aggregation_bits);
  return new IndexedAttestation(sorted(attesting_indices_0), attestation_0.data, attestation_0.signature);
}

/*
    Return the set of attesting indices corresponding to ``data`` and ``bits``.
    */
function method get_attesting_indices(state_0: BeaconState,data_0: AttestationData,bits_0: Bitlist): Set<ValidatorIndex> {
  var committee_0 := get_beacon_committee(state_0, data_0.slot, data_0.index);
  return set(enumerate(committee_0).filter((tmp_7) => var i := tmp_7.first; var index := tmp_7.second; bits_0[i]).map((tmp_8) => var i := tmp_8.first; var index := tmp_8.second; index));
}

/*
    Increase the validator balance at index ``index`` by ``delta``.
    */
function method increase_balance(state_0: BeaconState,index_0: ValidatorIndex,delta_0: Gwei): Unit {
  state_0 := state_0.(balances := state_0.balances[index_0 := state_0.balances[index_0] + delta_0]]);
}

/*
    Decrease the validator balance at index ``index`` by ``delta``, with underflow protection.
    */
function method decrease_balance(state_0: BeaconState,index_0: ValidatorIndex,delta_0: Gwei): Unit {
  state_0 := state_0.(balances := state_0.balances[index_0 := new Gwei(if (delta_0 > state_0.balances[index_0]) then 0 else state_0.balances[index_0] - delta_0)]]);
}

/*
    Initiate the exit of the validator with index ``index``.
    */
function method initiate_validator_exit(state_0: BeaconState,index_0: ValidatorIndex): Unit {
  var validator_0 := state_0.validators[index_0];
  if validator_0.exit_epoch != FAR_FUTURE_EPOCH {
    return;
  }
  var exit_epochs_0 := state_0.validators.filter((v) => v.exit_epoch != FAR_FUTURE_EPOCH).map((v) => v.exit_epoch);
  var exit_queue_epoch_0 := max(exit_epochs_0 + PyList(compute_activation_exit_epoch(get_current_epoch(state_0))));
  var exit_queue_churn_0 := len(state_0.validators.filter((v) => v.exit_epoch == exit_queue_epoch_0).map((v) => v));
  Epoch exit_queue_epoch_2;
  if exit_queue_churn_0 >= get_validator_churn_limit(state_0) {
    var exit_queue_epoch_1 := exit_queue_epoch_0 + new Epoch(1);
    exit_queue_epoch_2 := exit_queue_epoch_1;
  } else {
    exit_queue_epoch_2 := exit_queue_epoch_0;
  }
  validator_0 := validator_0.(exit_epoch := exit_queue_epoch_2]);
  validator_0 := validator_0.(withdrawable_epoch := new Epoch(validator_0.exit_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY)]);
}

/*
    Slash the validator with index ``slashed_index``.
    */
function method slash_validator(state_0: BeaconState,slashed_index_0: ValidatorIndex,whistleblower_index_0: ValidatorIndex): Unit {
  var epoch_0 := get_current_epoch(state_0);
  initiate_validator_exit(state_0, slashed_index_0);
  var validator_0 := state_0.validators[slashed_index_0];
  validator_0 := validator_0.(slashed := new boolean(true)]);
  validator_0 := validator_0.(withdrawable_epoch := max(validator_0.withdrawable_epoch, new Epoch(epoch_0 + EPOCHS_PER_SLASHINGS_VECTOR))]);
  state_0 := state_0.(slashings := state_0.slashings[epoch_0 % EPOCHS_PER_SLASHINGS_VECTOR := state_0.slashings[epoch_0 % EPOCHS_PER_SLASHINGS_VECTOR] + validator_0.effective_balance]]);
  decrease_balance(state_0, slashed_index_0, validator_0.effective_balance / MIN_SLASHING_PENALTY_QUOTIENT);
  var proposer_index_0 := get_beacon_proposer_index(state_0);
  ValidatorIndex whistleblower_index_2;
  if whistleblower_index_0 == null {
    var whistleblower_index_1 := proposer_index_0;
    whistleblower_index_2 := whistleblower_index_1;
  } else {
    whistleblower_index_2 := whistleblower_index_0;
  }
  var whistleblower_reward_0 := new Gwei(validator_0.effective_balance / WHISTLEBLOWER_REWARD_QUOTIENT);
  var proposer_reward_0 := new Gwei(whistleblower_reward_0 / PROPOSER_REWARD_QUOTIENT);
  increase_balance(state_0, proposer_index_0, proposer_reward_0);
  increase_balance(state_0, whistleblower_index_2, new Gwei(whistleblower_reward_0 - proposer_reward_0));
}

function method initialize_beacon_state_from_eth1(eth1_block_hash_0: Bytes32,eth1_timestamp_0: uint64,deposits_0: Sequence<Deposit>): BeaconState {
  var fork_0 := new Fork(GENESIS_FORK_VERSION, GENESIS_FORK_VERSION, GENESIS_EPOCH);
  var state_0 := new BeaconState(eth1_timestamp_0 + GENESIS_DELAY, BeaconState.genesis_validators_root_default, BeaconState.slot_default, fork_0, new BeaconBlockHeader(BeaconBlockHeader.slot_default, BeaconBlockHeader.proposer_index_default, BeaconBlockHeader.parent_root_default, BeaconBlockHeader.state_root_default, hash_tree_root(new BeaconBlockBody(BeaconBlockBody.randao_reveal_default, BeaconBlockBody.eth1_data_default, BeaconBlockBody.graffiti_default, BeaconBlockBody.proposer_slashings_default, BeaconBlockBody.attester_slashings_default, BeaconBlockBody.attestations_default, BeaconBlockBody.deposits_default, BeaconBlockBody.voluntary_exits_default))), BeaconState.block_roots_default, BeaconState.state_roots_default, BeaconState.historical_roots_default, new Eth1Data(Eth1Data.deposit_root_default, new uint64(len(deposits_0)), eth1_block_hash_0), BeaconState.eth1_data_votes_default, BeaconState.eth1_deposit_index_default, BeaconState.validators_default, BeaconState.balances_default, PyList(eth1_block_hash_0) * EPOCHS_PER_HISTORICAL_VECTOR, BeaconState.slashings_default, BeaconState.previous_epoch_attestations_default, BeaconState.current_epoch_attestations_default, BeaconState.justification_bits_default, BeaconState.previous_justified_checkpoint_default, BeaconState.current_justified_checkpoint_default, BeaconState.finalized_checkpoint_default);
  var leaves_0 := list(map((deposit) => deposit.data, deposits_0));
  var i10 := 0;
var coll11 := i10;
while i10 < |coll11|
  decreases |coll11| - i
{
var tmp_9 := coll11[i10];
  var index_0 := tmp_9.first;
  var deposit_0 := tmp_9.second;
    var deposit_data_list_0 := new List<DepositData>(*leaves_0[0..index_0 + 1]);
    state_0 := state_0.(eth1_data := state_0.eth1_data.(deposit_root := hash_tree_root(deposit_data_list_0)])]);
    process_deposit(state_0, deposit_0);
  }
  var i13 := 0;
var coll14 := i13;
while i13 < |coll14|
  decreases |coll14| - i
{
var tmp_12 := coll14[i13];
  var index_1 := tmp_12.first;
  var validator_0 := tmp_12.second;
    var balance_0 := state_0.balances[index_1];
    validator_0 := validator_0.(effective_balance := min(balance_0 - (balance_0 % EFFECTIVE_BALANCE_INCREMENT), MAX_EFFECTIVE_BALANCE)]);
    if validator_0.effective_balance == MAX_EFFECTIVE_BALANCE {
      validator_0 := validator_0.(activation_eligibility_epoch := GENESIS_EPOCH]);
      validator_0 := validator_0.(activation_epoch := GENESIS_EPOCH]);
    }
  }
  state_0 := state_0.(genesis_validators_root := hash_tree_root(state_0.validators)]);
  return state_0;
}

function method is_valid_genesis_state(state_0: BeaconState): bool {
  if state_0.genesis_time < MIN_GENESIS_TIME {
    return false;
  }
  if len(get_active_validator_indices(state_0, GENESIS_EPOCH)) < MIN_GENESIS_ACTIVE_VALIDATOR_COUNT {
    return false;
  }
  return true;
}

function method state_transition(state_0: BeaconState,signed_block_0: SignedBeaconBlock,validate_result_0: bool): Unit {
  var block_0 := signed_block_0.message;
  process_slots(state_0, block_0.slot);
  if validate_result_0 {
    assert(verify_block_signature(state_0, signed_block_0))
  }
  process_block(state_0, block_0);
  if validate_result_0 {
    assert(block_0.state_root == hash_tree_root(state_0))
  }
}

function method verify_block_signature(state_0: BeaconState,signed_block_0: SignedBeaconBlock): bool {
  var proposer_0 := state_0.validators[signed_block_0.message.proposer_index];
  var signing_root_0 := compute_signing_root(signed_block_0.message, get_domain(state_0, DOMAIN_BEACON_PROPOSER, null));
  return bls.Verify(proposer_0.pubkey, signing_root_0, signed_block_0.signature);
}

function method process_slots(state_0: BeaconState,slot_0: Slot): Unit {
  assert(state_0.slot < slot_0)
  while state_0.slot < slot_0 {
    process_slot(state_0);
    if ((state_0.slot + 1) % SLOTS_PER_EPOCH) == 0 {
      process_epoch(state_0);
    }
    state_0 := state_0.(slot := new Slot(state_0.slot + 1)]);
  }
}

function method process_slot(state_0: BeaconState): Unit {
  var previous_state_root_0 := hash_tree_root(state_0);
  state_0 := state_0.(state_roots := state_0.state_roots[state_0.slot % SLOTS_PER_HISTORICAL_ROOT := previous_state_root_0]]);
  if state_0.latest_block_header.state_root == new Bytes32() {
    state_0 := state_0.(latest_block_header := state_0.latest_block_header.(state_root := previous_state_root_0])]);
  }
  var previous_block_root_0 := hash_tree_root(state_0.latest_block_header);
  state_0 := state_0.(block_roots := state_0.block_roots[state_0.slot % SLOTS_PER_HISTORICAL_ROOT := previous_block_root_0]]);
}

function method process_epoch(state_0: BeaconState): Unit {
  process_justification_and_finalization(state_0);
  process_rewards_and_penalties(state_0);
  process_registry_updates(state_0);
  process_slashings(state_0);
  process_eth1_data_reset(state_0);
  process_effective_balance_updates(state_0);
  process_slashings_reset(state_0);
  process_randao_mixes_reset(state_0);
  process_historical_roots_update(state_0);
  process_participation_record_updates(state_0);
}

function method get_matching_source_attestations(state_0: BeaconState,epoch_0: Epoch): Sequence<PendingAttestation> {
  assert(epoch_0 in Pair(get_previous_epoch(state_0), get_current_epoch(state_0)))
  return if (epoch_0 == get_current_epoch(state_0)) then state_0.current_epoch_attestations else state_0.previous_epoch_attestations;
}

function method get_matching_target_attestations(state_0: BeaconState,epoch_0: Epoch): Sequence<PendingAttestation> {
  return get_matching_source_attestations(state_0, epoch_0).filter((a) => a.data.target.root == get_block_root(state_0, epoch_0)).map((a) => a);
}

function method get_matching_head_attestations(state_0: BeaconState,epoch_0: Epoch): Sequence<PendingAttestation> {
  return get_matching_target_attestations(state_0, epoch_0).filter((a) => a.data.beacon_block_root == get_block_root_at_slot(state_0, a.data.slot)).map((a) => a);
}

function method get_unslashed_attesting_indices(state_0: BeaconState,attestations_0: Sequence<PendingAttestation>): Set<ValidatorIndex> {
  var output_0 := set();
  var output_2 := output_0;
  var i15 := 0;
var coll16 := i15;
while i15 < |coll16|
  decreases |coll16| - i
{
var a_0 := coll16[i15];
    var output_1 := output_2.union(get_attesting_indices(state_0, a_0.data, a_0.aggregation_bits));
    output_2 := output_1;
  }
  return set(filter((index) => !state_0.validators[index].slashed, output_2));
}

/*
    Return the combined effective balance of the set of unslashed validators participating in ``attestations``.
    Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
    */
function method get_attesting_balance(state_0: BeaconState,attestations_0: Sequence<PendingAttestation>): Gwei {
  return get_total_balance(state_0, get_unslashed_attesting_indices(state_0, attestations_0));
}

function method process_justification_and_finalization(state_0: BeaconState): Unit {
  if get_current_epoch(state_0) <= (GENESIS_EPOCH + 1) {
    return;
  }
  var previous_attestations_0 := get_matching_target_attestations(state_0, get_previous_epoch(state_0));
  var current_attestations_0 := get_matching_target_attestations(state_0, get_current_epoch(state_0));
  var total_active_balance_0 := get_total_active_balance(state_0);
  var previous_target_balance_0 := get_attesting_balance(state_0, previous_attestations_0);
  var current_target_balance_0 := get_attesting_balance(state_0, current_attestations_0);
  weigh_justification_and_finalization(state_0, total_active_balance_0, previous_target_balance_0, current_target_balance_0);
}

function method weigh_justification_and_finalization(state_0: BeaconState,total_active_balance_0: Gwei,previous_epoch_target_balance_0: Gwei,current_epoch_target_balance_0: Gwei): Unit {
  var previous_epoch_0 := get_previous_epoch(state_0);
  var current_epoch_0 := get_current_epoch(state_0);
  var old_previous_justified_checkpoint_0 := state_0.previous_justified_checkpoint;
  var old_current_justified_checkpoint_0 := state_0.current_justified_checkpoint;
  state_0 := state_0.(previous_justified_checkpoint := state_0.current_justified_checkpoint]);
  state_0 := state_0.(justification_bits := state_0.justification_bits[1..null := state_0.justification_bits[0..JUSTIFICATION_BITS_LENGTH - 1]]]);
  state_0 := state_0.(justification_bits := state_0.justification_bits[0 := new boolean(0)]]);
  if (previous_epoch_target_balance_0 * 3) >= (total_active_balance_0 * 2) {
    state_0 := state_0.(current_justified_checkpoint := new Checkpoint(previous_epoch_0, get_block_root(state_0, previous_epoch_0))]);
    state_0 := state_0.(justification_bits := state_0.justification_bits[1 := new boolean(1)]]);
  }
  if (current_epoch_target_balance_0 * 3) >= (total_active_balance_0 * 2) {
    state_0 := state_0.(current_justified_checkpoint := new Checkpoint(current_epoch_0, get_block_root(state_0, current_epoch_0))]);
    state_0 := state_0.(justification_bits := state_0.justification_bits[0 := new boolean(1)]]);
  }
  var bits_0 := state_0.justification_bits;
  if all(bits_0[1..4]) && ((old_previous_justified_checkpoint_0.epoch + 3) == current_epoch_0) {
    state_0 := state_0.(finalized_checkpoint := old_previous_justified_checkpoint_0]);
  }
  if all(bits_0[1..3]) && ((old_previous_justified_checkpoint_0.epoch + 2) == current_epoch_0) {
    state_0 := state_0.(finalized_checkpoint := old_previous_justified_checkpoint_0]);
  }
  if all(bits_0[0..3]) && ((old_current_justified_checkpoint_0.epoch + 2) == current_epoch_0) {
    state_0 := state_0.(finalized_checkpoint := old_current_justified_checkpoint_0]);
  }
  if all(bits_0[0..2]) && ((old_current_justified_checkpoint_0.epoch + 1) == current_epoch_0) {
    state_0 := state_0.(finalized_checkpoint := old_current_justified_checkpoint_0]);
  }
}

function method get_base_reward(state_0: BeaconState,index_0: ValidatorIndex): Gwei {
  var total_balance_0 := get_total_active_balance(state_0);
  var effective_balance_0 := state_0.validators[index_0].effective_balance;
  return new Gwei(effective_balance_0 * BASE_REWARD_FACTOR / integer_squareroot(total_balance_0) / BASE_REWARDS_PER_EPOCH);
}

function method get_proposer_reward(state_0: BeaconState,attesting_index_0: ValidatorIndex): Gwei {
  return new Gwei(get_base_reward(state_0, attesting_index_0) / PROPOSER_REWARD_QUOTIENT);
}

function method get_finality_delay(state_0: BeaconState): uint64 {
  return get_previous_epoch(state_0) - state_0.finalized_checkpoint.epoch;
}

function method is_in_inactivity_leak(state_0: BeaconState): bool {
  return get_finality_delay(state_0) > MIN_EPOCHS_TO_INACTIVITY_PENALTY;
}

function method get_eligible_validator_indices(state_0: BeaconState): Sequence<ValidatorIndex> {
  var previous_epoch_0 := get_previous_epoch(state_0);
  return enumerate(state_0.validators).filter((tmp_17) => var index := tmp_17.first; var v := tmp_17.second; is_active_validator(v, previous_epoch_0) || (v.slashed && ((previous_epoch_0 + 1) < v.withdrawable_epoch))).map((tmp_18) => var index := tmp_18.first; var v := tmp_18.second; new ValidatorIndex(index));
}

/*
    Helper with shared logic for use by get source, target, and head deltas functions
    */
function method get_attestation_component_deltas(state_0: BeaconState,attestations_0: Sequence<PendingAttestation>): Tuple<Sequence<Gwei>,Sequence<Gwei>> {
  var rewards_0 := PyList(new Gwei(0)) * len(state_0.validators);
  var penalties_0 := PyList(new Gwei(0)) * len(state_0.validators);
  var total_balance_0 := get_total_active_balance(state_0);
  var unslashed_attesting_indices_0 := get_unslashed_attesting_indices(state_0, attestations_0);
  var attesting_balance_0 := get_total_balance(state_0, unslashed_attesting_indices_0);
  var i19 := 0;
var coll20 := i19;
while i19 < |coll20|
  decreases |coll20| - i
{
var index_0 := coll20[i19];
    if index_0 in unslashed_attesting_indices_0 {
      var increment_0 := EFFECTIVE_BALANCE_INCREMENT;
      if is_in_inactivity_leak(state_0) {
        rewards_0 := rewards_0[index_0 := rewards_0[index_0] + get_base_reward(state_0, index_0)];
      } else {
        var reward_numerator_0 := get_base_reward(state_0, index_0) * (attesting_balance_0 / increment_0);
        rewards_0 := rewards_0[index_0 := rewards_0[index_0] + (reward_numerator_0 / (total_balance_0 / increment_0))];
      }
    } else {
      penalties_0 := penalties_0[index_0 := penalties_0[index_0] + get_base_reward(state_0, index_0)];
    }
  }
  return Pair(rewards_0, penalties_0);
}

/*
    Return attester micro-rewards/penalties for source-vote for each validator.
    */
function method get_source_deltas(state_0: BeaconState): Tuple<Sequence<Gwei>,Sequence<Gwei>> {
  var matching_source_attestations_0 := get_matching_source_attestations(state_0, get_previous_epoch(state_0));
  return get_attestation_component_deltas(state_0, matching_source_attestations_0);
}

/*
    Return attester micro-rewards/penalties for target-vote for each validator.
    */
function method get_target_deltas(state_0: BeaconState): Tuple<Sequence<Gwei>,Sequence<Gwei>> {
  var matching_target_attestations_0 := get_matching_target_attestations(state_0, get_previous_epoch(state_0));
  return get_attestation_component_deltas(state_0, matching_target_attestations_0);
}

/*
    Return attester micro-rewards/penalties for head-vote for each validator.
    */
function method get_head_deltas(state_0: BeaconState): Tuple<Sequence<Gwei>,Sequence<Gwei>> {
  var matching_head_attestations_0 := get_matching_head_attestations(state_0, get_previous_epoch(state_0));
  return get_attestation_component_deltas(state_0, matching_head_attestations_0);
}

/*
    Return proposer and inclusion delay micro-rewards/penalties for each validator.
    */
function method get_inclusion_delay_deltas(state_0: BeaconState): Tuple<Sequence<Gwei>,Sequence<Gwei>> {
  var rewards_0 := range(len(state_0.validators)).map((_0) => new Gwei(0));
  var matching_source_attestations_0 := get_matching_source_attestations(state_0, get_previous_epoch(state_0));
  var i21 := 0;
var coll22 := i21;
while i21 < |coll22|
  decreases |coll22| - i
{
var index_0 := coll22[i21];
    var attestation_0 := min(matching_source_attestations_0.filter((a) => index_0 in get_attesting_indices(state_0, a.data, a.aggregation_bits)).map((a) => a), (a) => a.inclusion_delay);
    rewards_0 := rewards_0[attestation_0.proposer_index := rewards_0[attestation_0.proposer_index] + get_proposer_reward(state_0, index_0)];
    var max_attester_reward_0 := new Gwei(get_base_reward(state_0, index_0) - get_proposer_reward(state_0, index_0));
    rewards_0 := rewards_0[index_0 := rewards_0[index_0] + new Gwei(max_attester_reward_0 / attestation_0.inclusion_delay)];
  }
  var penalties_0 := range(len(state_0.validators)).map((_0) => new Gwei(0));
  return Pair(rewards_0, penalties_0);
}

/*
    Return inactivity reward/penalty deltas for each validator.
    */
function method get_inactivity_penalty_deltas(state_0: BeaconState): Tuple<Sequence<Gwei>,Sequence<Gwei>> {
  var penalties_0 := range(len(state_0.validators)).map((_0) => new Gwei(0));
  if is_in_inactivity_leak(state_0) {
    var matching_target_attestations_0 := get_matching_target_attestations(state_0, get_previous_epoch(state_0));
    var matching_target_attesting_indices_0 := get_unslashed_attesting_indices(state_0, matching_target_attestations_0);
    var i23 := 0;
var coll24 := i23;
while i23 < |coll24|
  decreases |coll24| - i
{
var index_0 := coll24[i23];
      var base_reward_0 := get_base_reward(state_0, index_0);
      penalties_0 := penalties_0[index_0 := penalties_0[index_0] + new Gwei((BASE_REWARDS_PER_EPOCH * base_reward_0) - get_proposer_reward(state_0, index_0))];
      if index_0 !in matching_target_attesting_indices_0 {
        var effective_balance_0 := state_0.validators[index_0].effective_balance;
        penalties_0 := penalties_0[index_0 := penalties_0[index_0] + new Gwei(effective_balance_0 * get_finality_delay(state_0) / INACTIVITY_PENALTY_QUOTIENT)];
      }
    }
  }
  var rewards_0 := range(len(state_0.validators)).map((_0) => new Gwei(0));
  return Pair(rewards_0, penalties_0);
}

/*
    Return attestation reward/penalty deltas for each validator.
    */
function method get_attestation_deltas(state_0: BeaconState): Tuple<Sequence<Gwei>,Sequence<Gwei>> {
  var tmp_25 := get_source_deltas(state_0);
  var source_rewards_0 := tmp_25.first;
  var source_penalties_0 := tmp_25.second;
  var tmp_26 := get_target_deltas(state_0);
  var target_rewards_0 := tmp_26.first;
  var target_penalties_0 := tmp_26.second;
  var tmp_27 := get_head_deltas(state_0);
  var head_rewards_0 := tmp_27.first;
  var head_penalties_0 := tmp_27.second;
  var tmp_28 := get_inclusion_delay_deltas(state_0);
  var inclusion_delay_rewards_0 := tmp_28.first;
  var __0 := tmp_28.second;
  var tmp_29 := get_inactivity_penalty_deltas(state_0);
  var __1 := tmp_29.first;
  var inactivity_penalties_0 := tmp_29.second;
  var rewards_0 := range(len(state_0.validators)).map((i) => source_rewards_0[i] + target_rewards_0[i] + head_rewards_0[i] + inclusion_delay_rewards_0[i]);
  var penalties_0 := range(len(state_0.validators)).map((i) => source_penalties_0[i] + target_penalties_0[i] + head_penalties_0[i] + inactivity_penalties_0[i]);
  return Pair(rewards_0, penalties_0);
}

function method process_rewards_and_penalties(state_0: BeaconState): Unit {
  if get_current_epoch(state_0) == GENESIS_EPOCH {
    return;
  }
  var tmp_30 := get_attestation_deltas(state_0);
  var rewards_0 := tmp_30.first;
  var penalties_0 := tmp_30.second;
  var i31 := 0;
var coll32 := i31;
while i31 < |coll32|
  decreases |coll32| - i
{
var index_0 := coll32[i31];
    increase_balance(state_0, new ValidatorIndex(index_0), rewards_0[index_0]);
    decrease_balance(state_0, new ValidatorIndex(index_0), penalties_0[index_0]);
  }
}

function method process_registry_updates(state_0: BeaconState): Unit {
  var i34 := 0;
var coll35 := i34;
while i34 < |coll35|
  decreases |coll35| - i
{
var tmp_33 := coll35[i34];
  var index_0 := tmp_33.first;
  var validator_0 := tmp_33.second;
    if is_eligible_for_activation_queue(validator_0) {
      validator_0 := validator_0.(activation_eligibility_epoch := get_current_epoch(state_0) + 1]);
    }
    if is_active_validator(validator_0, get_current_epoch(state_0)) && (validator_0.effective_balance <= EJECTION_BALANCE) {
      initiate_validator_exit(state_0, new ValidatorIndex(index_0));
    }
  }
  var activation_queue_0 := sorted(enumerate(state_0.validators).filter((tmp_36) => var index := tmp_36.first; var validator := tmp_36.second; is_eligible_for_activation(state_0, validator)).map((tmp_37) => var index := tmp_37.first; var validator := tmp_37.second; index), (index) => Pair(state_0.validators[index].activation_eligibility_epoch, index));
  var i38 := 0;
var coll39 := i38;
while i38 < |coll39|
  decreases |coll39| - i
{
var index_1 := coll39[i38];
    var validator_1 := state_0.validators[index_1];
    validator_1 := validator_1.(activation_epoch := compute_activation_exit_epoch(get_current_epoch(state_0))]);
  }
}

function method process_slashings(state_0: BeaconState): Unit {
  var epoch_0 := get_current_epoch(state_0);
  var total_balance_0 := get_total_active_balance(state_0);
  var adjusted_total_slashing_balance_0 := min(sum(state_0.slashings) * PROPORTIONAL_SLASHING_MULTIPLIER, total_balance_0);
  var i41 := 0;
var coll42 := i41;
while i41 < |coll42|
  decreases |coll42| - i
{
var tmp_40 := coll42[i41];
  var index_0 := tmp_40.first;
  var validator_0 := tmp_40.second;
    if validator_0.slashed && ((epoch_0 + (EPOCHS_PER_SLASHINGS_VECTOR / 2)) == validator_0.withdrawable_epoch) {
      var increment_0 := EFFECTIVE_BALANCE_INCREMENT;
      var penalty_numerator_0 := validator_0.effective_balance / increment_0 * adjusted_total_slashing_balance_0;
      var penalty_0 := penalty_numerator_0 / total_balance_0 * increment_0;
      decrease_balance(state_0, new ValidatorIndex(index_0), penalty_0);
    }
  }
}

function method process_eth1_data_reset(state_0: BeaconState): Unit {
  var next_epoch_0 := new Epoch(get_current_epoch(state_0) + 1);
  if (next_epoch_0 % EPOCHS_PER_ETH1_VOTING_PERIOD) == 0 {
    state_0 := state_0.(eth1_data_votes := new List<Eth1Data>()]);
  }
}

function method process_effective_balance_updates(state_0: BeaconState): Unit {
  var i44 := 0;
var coll45 := i44;
while i44 < |coll45|
  decreases |coll45| - i
{
var tmp_43 := coll45[i44];
  var index_0 := tmp_43.first;
  var validator_0 := tmp_43.second;
    var balance_0 := state_0.balances[index_0];
    var HYSTERESIS_INCREMENT_0 := new uint64(EFFECTIVE_BALANCE_INCREMENT / HYSTERESIS_QUOTIENT);
    var DOWNWARD_THRESHOLD_0 := HYSTERESIS_INCREMENT_0 * HYSTERESIS_DOWNWARD_MULTIPLIER;
    var UPWARD_THRESHOLD_0 := HYSTERESIS_INCREMENT_0 * HYSTERESIS_UPWARD_MULTIPLIER;
    if ((balance_0 + DOWNWARD_THRESHOLD_0) < validator_0.effective_balance) || ((validator_0.effective_balance + UPWARD_THRESHOLD_0) < balance_0) {
      validator_0 := validator_0.(effective_balance := min(balance_0 - (balance_0 % EFFECTIVE_BALANCE_INCREMENT), MAX_EFFECTIVE_BALANCE)]);
    }
  }
}

function method process_slashings_reset(state_0: BeaconState): Unit {
  var next_epoch_0 := new Epoch(get_current_epoch(state_0) + 1);
  state_0 := state_0.(slashings := state_0.slashings[next_epoch_0 % EPOCHS_PER_SLASHINGS_VECTOR := new Gwei(0)]]);
}

function method process_randao_mixes_reset(state_0: BeaconState): Unit {
  var current_epoch_0 := get_current_epoch(state_0);
  var next_epoch_0 := new Epoch(current_epoch_0 + 1);
  state_0 := state_0.(randao_mixes := state_0.randao_mixes[next_epoch_0 % EPOCHS_PER_HISTORICAL_VECTOR := get_randao_mix(state_0, current_epoch_0)]]);
}

function method process_historical_roots_update(state_0: BeaconState): Unit {
  var next_epoch_0 := new Epoch(get_current_epoch(state_0) + 1);
  if (next_epoch_0 % (SLOTS_PER_HISTORICAL_ROOT / SLOTS_PER_EPOCH)) == 0 {
    var historical_batch_0 := new HistoricalBatch(state_0.block_roots, state_0.state_roots);
    state_0.historical_roots.append(hash_tree_root(historical_batch_0));
  }
}

function method process_participation_record_updates(state_0: BeaconState): Unit {
  state_0 := state_0.(previous_epoch_attestations := state_0.current_epoch_attestations]);
  state_0 := state_0.(current_epoch_attestations := new List<PendingAttestation>()]);
}

function method process_block(state_0: BeaconState,block_0: BeaconBlock): Unit {
  process_block_header(state_0, block_0);
  process_randao(state_0, block_0.body);
  process_eth1_data(state_0, block_0.body);
  process_operations(state_0, block_0.body);
}

function method process_block_header(state_0: BeaconState,block_0: BeaconBlock): Unit {
  assert(block_0.slot == state_0.slot)
  assert(block_0.slot > state_0.latest_block_header.slot)
  assert(block_0.proposer_index == get_beacon_proposer_index(state_0))
  assert(block_0.parent_root == hash_tree_root(state_0.latest_block_header))
  state_0 := state_0.(latest_block_header := new BeaconBlockHeader(block_0.slot, block_0.proposer_index, block_0.parent_root, new Root(), hash_tree_root(block_0.body))]);
  var proposer_0 := state_0.validators[block_0.proposer_index];
  assert(!proposer_0.slashed)
}

function method process_randao(state_0: BeaconState,body_0: BeaconBlockBody): Unit {
  var epoch_0 := get_current_epoch(state_0);
  var proposer_0 := state_0.validators[get_beacon_proposer_index(state_0)];
  var signing_root_0 := compute_signing_root(epoch_0, get_domain(state_0, DOMAIN_RANDAO, null));
  assert(bls.Verify(proposer_0.pubkey, signing_root_0, body_0.randao_reveal))
  var mix_0 := xor(get_randao_mix(state_0, epoch_0), hash(body_0.randao_reveal));
  state_0 := state_0.(randao_mixes := state_0.randao_mixes[epoch_0 % EPOCHS_PER_HISTORICAL_VECTOR := mix_0]]);
}

function method process_eth1_data(state_0: BeaconState,body_0: BeaconBlockBody): Unit {
  state_0.eth1_data_votes.append(body_0.eth1_data);
  if (state_0.eth1_data_votes.count(body_0.eth1_data) * 2) > (EPOCHS_PER_ETH1_VOTING_PERIOD * SLOTS_PER_EPOCH) {
    state_0 := state_0.(eth1_data := body_0.eth1_data]);
  }
}

function method process_operations(state_0: BeaconState,body_0: BeaconBlockBody): Unit {
  assert(len(body_0.deposits) == min(MAX_DEPOSITS, state_0.eth1_data.deposit_count - state_0.eth1_deposit_index))
  var i46 := 0;
var coll47 := i46;
while i46 < |coll47|
  decreases |coll47| - i
{
var operation_0 := coll47[i46];
    process_proposer_slashing(state_0, operation_0);
  }
  var i48 := 0;
var coll49 := i48;
while i48 < |coll49|
  decreases |coll49| - i
{
var operation_1 := coll49[i48];
    process_attester_slashing(state_0, operation_1);
  }
  var i50 := 0;
var coll51 := i50;
while i50 < |coll51|
  decreases |coll51| - i
{
var operation_2 := coll51[i50];
    process_attestation(state_0, operation_2);
  }
  var i52 := 0;
var coll53 := i52;
while i52 < |coll53|
  decreases |coll53| - i
{
var operation_3 := coll53[i52];
    process_deposit(state_0, operation_3);
  }
  var i54 := 0;
var coll55 := i54;
while i54 < |coll55|
  decreases |coll55| - i
{
var operation_4 := coll55[i54];
    process_voluntary_exit(state_0, operation_4);
  }
}

function method process_proposer_slashing(state_0: BeaconState,proposer_slashing_0: ProposerSlashing): Unit {
  var header_1_0 := proposer_slashing_0.signed_header_1.message;
  var header_2_0 := proposer_slashing_0.signed_header_2.message;
  assert(header_1_0.slot == header_2_0.slot)
  assert(header_1_0.proposer_index == header_2_0.proposer_index)
  assert(header_1_0 != header_2_0)
  var proposer_0 := state_0.validators[header_1_0.proposer_index];
  assert(is_slashable_validator(proposer_0, get_current_epoch(state_0)))
  var i56 := 0;
var coll57 := i56;
while i56 < |coll57|
  decreases |coll57| - i
{
var signed_header_0 := coll57[i56];
    var domain_0 := get_domain(state_0, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(signed_header_0.message.slot));
    var signing_root_0 := compute_signing_root(signed_header_0.message, domain_0);
    assert(bls.Verify(proposer_0.pubkey, signing_root_0, signed_header_0.signature))
  }
  slash_validator(state_0, header_1_0.proposer_index, null);
}

function method process_attester_slashing(state_0: BeaconState,attester_slashing_0: AttesterSlashing): Unit {
  var attestation_1_0 := attester_slashing_0.attestation_1;
  var attestation_2_0 := attester_slashing_0.attestation_2;
  assert(is_slashable_attestation_data(attestation_1_0.data, attestation_2_0.data))
  assert(is_valid_indexed_attestation(state_0, attestation_1_0))
  assert(is_valid_indexed_attestation(state_0, attestation_2_0))
  var slashed_any_0 := false;
  var indices_0 := set(attestation_1_0.attesting_indices).intersection(attestation_2_0.attesting_indices);
  var slashed_any_2 := slashed_any_0;
  var i58 := 0;
var coll59 := i58;
while i58 < |coll59|
  decreases |coll59| - i
{
var index_0 := coll59[i58];
    if is_slashable_validator(state_0.validators[index_0], get_current_epoch(state_0)) {
      slash_validator(state_0, index_0, null);
      var slashed_any_1 := true;
      slashed_any_2 := slashed_any_1;
    } else {
      slashed_any_2 := slashed_any_2;
    }
  }
  assert(slashed_any_2)
}

function method process_attestation(state_0: BeaconState,attestation_0: Attestation): Unit {
  var data_0 := attestation_0.data;
  assert(data_0.target.epoch in Pair(get_previous_epoch(state_0), get_current_epoch(state_0)))
  assert(data_0.target.epoch == compute_epoch_at_slot(data_0.slot))
  assert(((data_0.slot + MIN_ATTESTATION_INCLUSION_DELAY) <= state_0.slot) && (state_0.slot <= (data_0.slot + SLOTS_PER_EPOCH)))
  assert(data_0.index < get_committee_count_per_slot(state_0, data_0.target.epoch))
  var committee_0 := get_beacon_committee(state_0, data_0.slot, data_0.index);
  assert(len(attestation_0.aggregation_bits) == len(committee_0))
  var pending_attestation_0 := new PendingAttestation(attestation_0.aggregation_bits, data_0, state_0.slot - data_0.slot, get_beacon_proposer_index(state_0));
  if data_0.target.epoch == get_current_epoch(state_0) {
    assert(data_0.source == state_0.current_justified_checkpoint)
    state_0.current_epoch_attestations.append(pending_attestation_0);
  } else {
    assert(data_0.source == state_0.previous_justified_checkpoint)
    state_0.previous_epoch_attestations.append(pending_attestation_0);
  }
  assert(is_valid_indexed_attestation(state_0, get_indexed_attestation(state_0, attestation_0)))
}

function method get_validator_from_deposit(state_0: BeaconState,deposit_0: Deposit): Validator {
  var amount_0 := deposit_0.data.amount;
  var effective_balance_0 := min(amount_0 - (amount_0 % EFFECTIVE_BALANCE_INCREMENT), MAX_EFFECTIVE_BALANCE);
  return new Validator(deposit_0.data.pubkey, deposit_0.data.withdrawal_credentials, effective_balance_0, Validator.slashed_default, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH, FAR_FUTURE_EPOCH);
}

function method process_deposit(state_0: BeaconState,deposit_0: Deposit): Unit {
  assert(is_valid_merkle_branch(hash_tree_root(deposit_0.data), deposit_0.proof, DEPOSIT_CONTRACT_TREE_DEPTH + 1, state_0.eth1_deposit_index, state_0.eth1_data.deposit_root))
  state_0 := state_0.(eth1_deposit_index := state_0.eth1_deposit_index + 1]);
  var pubkey_0 := deposit_0.data.pubkey;
  var amount_0 := deposit_0.data.amount;
  var validator_pubkeys_0 := state_0.validators.map((v) => v.pubkey);
  if pubkey_0 !in validator_pubkeys_0 {
    var deposit_message_0 := new DepositMessage(deposit_0.data.pubkey, deposit_0.data.withdrawal_credentials, deposit_0.data.amount);
    var domain_0 := compute_domain(DOMAIN_DEPOSIT, null, null);
    var signing_root_0 := compute_signing_root(deposit_message_0, domain_0);
    if !bls.Verify(pubkey_0, signing_root_0, deposit_0.data.signature) {
      return;
    }
    state_0.validators.append(get_validator_from_deposit(state_0, deposit_0));
    state_0.balances.append(amount_0);
  } else {
    var index_0 := new ValidatorIndex(validator_pubkeys_0.index(pubkey_0));
    increase_balance(state_0, index_0, amount_0);
  }
}

function method process_voluntary_exit(state_0: BeaconState,signed_voluntary_exit_0: SignedVoluntaryExit): Unit {
  var voluntary_exit_0 := signed_voluntary_exit_0.message;
  var validator_0 := state_0.validators[voluntary_exit_0.validator_index];
  assert(is_active_validator(validator_0, get_current_epoch(state_0)))
  assert(validator_0.exit_epoch == FAR_FUTURE_EPOCH)
  assert(get_current_epoch(state_0) >= voluntary_exit_0.epoch)
  assert(get_current_epoch(state_0) >= (validator_0.activation_epoch + SHARD_COMMITTEE_PERIOD))
  var domain_0 := get_domain(state_0, DOMAIN_VOLUNTARY_EXIT, voluntary_exit_0.epoch);
  var signing_root_0 := compute_signing_root(voluntary_exit_0, domain_0);
  assert(bls.Verify(validator_0.pubkey, signing_root_0, signed_voluntary_exit_0.signature))
  initiate_validator_exit(state_0, voluntary_exit_0.validator_index);
}

function method get_forkchoice_store(anchor_state_0: BeaconState,anchor_block_0: BeaconBlock): Store {
  assert(anchor_block_0.state_root == hash_tree_root(anchor_state_0))
  var anchor_root_0 := hash_tree_root(anchor_block_0);
  var anchor_epoch_0 := get_current_epoch(anchor_state_0);
  var justified_checkpoint_0 := new Checkpoint(anchor_epoch_0, anchor_root_0);
  var finalized_checkpoint_0 := new Checkpoint(anchor_epoch_0, anchor_root_0);
  return new Store(new uint64(anchor_state_0.genesis_time + (SECONDS_PER_SLOT * anchor_state_0.slot)), anchor_state_0.genesis_time, justified_checkpoint_0, finalized_checkpoint_0, justified_checkpoint_0, PyDict(anchor_root_0 to copy(anchor_block_0)), PyDict(anchor_root_0 to copy(anchor_state_0)), PyDict(justified_checkpoint_0 to copy(anchor_state_0)), Store.latest_messages_default);
}

function method get_slots_since_genesis(store_0: Store): int {
  return (store_0.time - store_0.genesis_time) / SECONDS_PER_SLOT;
}

function method get_current_slot(store_0: Store): Slot {
  return new Slot(GENESIS_SLOT + get_slots_since_genesis(store_0));
}

function method compute_slots_since_epoch_start(slot_0: Slot): int {
  return slot_0 - compute_start_slot_at_epoch(compute_epoch_at_slot(slot_0));
}

function method get_ancestor(store_0: Store,root_0: Root,slot_0: Slot): Root {
  var block_0 := store_0.blocks[root_0];
  if block_0.slot > slot_0 {
    return get_ancestor(store_0, block_0.parent_root, slot_0);
  } else {
    if block_0.slot == slot_0 {
      return root_0;
    } else {
      return root_0;
    }
  }
}

function method get_latest_attesting_balance(store_0: Store,root_0: Root): Gwei {
  var state_0 := store_0.checkpoint_states[store_0.justified_checkpoint];
  var active_indices_0 := get_active_validator_indices(state_0, get_current_epoch(state_0));
  return new Gwei(sum(active_indices_0.filter((i) => (i in store_0.latest_messages) && (get_ancestor(store_0, store_0.latest_messages[i].root, store_0.blocks[root_0].slot) == root_0)).map((i) => state_0.validators[i].effective_balance)));
}

function method filter_block_tree(store_0: Store,block_root_0: Root,blocks_0: Dict<Root,BeaconBlock>): bool {
  var block_0 := store_0.blocks[block_root_0];
  var children_0 := store_0.blocks.keys().filter((root) => store_0.blocks[root].parent_root == block_root_0).map((root) => root);
  if any(children_0) {
    var filter_block_tree_result_0 := children_0.map((child) => filter_block_tree(store_0, child, blocks_0));
    if any(filter_block_tree_result_0) {
      blocks_0 := blocks_0[block_root_0 := block_0];
      return true;
    }
    return false;
  }
  var head_state_0 := store_0.block_states[block_root_0];
  var correct_justified_0 := (store_0.justified_checkpoint.epoch == GENESIS_EPOCH) || (head_state_0.current_justified_checkpoint == store_0.justified_checkpoint);
  var correct_finalized_0 := (store_0.finalized_checkpoint.epoch == GENESIS_EPOCH) || (head_state_0.finalized_checkpoint == store_0.finalized_checkpoint);
  if correct_justified_0 && correct_finalized_0 {
    blocks_0 := blocks_0[block_root_0 := block_0];
    return true;
  }
  return false;
}

/*
    Retrieve a filtered block tree from ``store``, only returning branches
    whose leaf state's justified/finalized info agrees with that in ``store``.
    */
function method get_filtered_block_tree(store_0: Store): Dict<Root,BeaconBlock> {
  var base_0 := store_0.justified_checkpoint.root;
  Dict<Root,BeaconBlock> blocks_0 := PyDict();
  filter_block_tree(store_0, base_0, blocks_0);
  return blocks_0;
}

function method get_head(store_0: Store): Root {
  var blocks_0 := get_filtered_block_tree(store_0);
  var head_0 := store_0.justified_checkpoint.root;
  var head_2 := head_0;
  while true {
    var children_0 := blocks_0.keys().filter((root) => blocks_0[root].parent_root == head_2).map((root) => root);
    if len(children_0) == 0 {
      return head_2;
    }
    var head_1 := max(children_0, (root) => Pair(get_latest_attesting_balance(store_0, root), root));
    head_2 := head_1;
  }
}

/*
    To address the bouncing attack, only update conflicting justified
    checkpoints in the fork choice if in the early slots of the epoch.
    Otherwise, delay incorporation of new justified checkpoint until next epoch boundary.

    See https://ethresear.ch/t/prevention-of-bouncing-attack-on-ffg/6114 for more detailed analysis and discussion.
    */
function method should_update_justified_checkpoint(store_0: Store,new_justified_checkpoint_0: Checkpoint): bool {
  if compute_slots_since_epoch_start(get_current_slot(store_0)) < SAFE_SLOTS_TO_UPDATE_JUSTIFIED {
    return true;
  }
  var justified_slot_0 := compute_start_slot_at_epoch(store_0.justified_checkpoint.epoch);
  if !(get_ancestor(store_0, new_justified_checkpoint_0.root, justified_slot_0) == store_0.justified_checkpoint.root) {
    return false;
  }
  return true;
}

function method validate_on_attestation(store_0: Store,attestation_0: Attestation): Unit {
  var target_0 := attestation_0.data.target;
  var current_epoch_0 := compute_epoch_at_slot(get_current_slot(store_0));
  var previous_epoch_0 := if (current_epoch_0 > GENESIS_EPOCH) then current_epoch_0 - 1 else GENESIS_EPOCH;
  assert(target_0.epoch in PyList(current_epoch_0, previous_epoch_0))
  assert(target_0.epoch == compute_epoch_at_slot(attestation_0.data.slot))
  assert(target_0.root in store_0.blocks)
  assert(attestation_0.data.beacon_block_root in store_0.blocks)
  assert(store_0.blocks[attestation_0.data.beacon_block_root].slot <= attestation_0.data.slot)
  var target_slot_0 := compute_start_slot_at_epoch(target_0.epoch);
  assert(target_0.root == get_ancestor(store_0, attestation_0.data.beacon_block_root, target_slot_0))
  assert(get_current_slot(store_0) >= (attestation_0.data.slot + 1))
}

function method store_target_checkpoint_state(store_0: Store,target_0: Checkpoint): Unit {
  if target_0 !in store_0.checkpoint_states {
    var base_state_0 := copy(store_0.block_states[target_0.root]);
    if base_state_0.slot < compute_start_slot_at_epoch(target_0.epoch) {
      process_slots(base_state_0, compute_start_slot_at_epoch(target_0.epoch));
    }
    store_0 := store_0.(checkpoint_states := store_0.checkpoint_states[target_0 := base_state_0]]);
  }
}

function method update_latest_messages(store_0: Store,attesting_indices_0: Sequence<ValidatorIndex>,attestation_0: Attestation): Unit {
  var target_0 := attestation_0.data.target;
  var beacon_block_root_0 := attestation_0.data.beacon_block_root;
  var i60 := 0;
var coll61 := i60;
while i60 < |coll61|
  decreases |coll61| - i
{
var i_0 := coll61[i60];
    if (i_0 !in store_0.latest_messages) || (target_0.epoch > store_0.latest_messages[i_0].epoch) {
      store_0 := store_0.(latest_messages := store_0.latest_messages[i_0 := new LatestMessage(target_0.epoch, beacon_block_root_0)]]);
    }
  }
}

function method on_tick(store_0: Store,time_0: uint64): Unit {
  var previous_slot_0 := get_current_slot(store_0);
  store_0 := store_0.(time := time_0]);
  var current_slot_0 := get_current_slot(store_0);
  if !((current_slot_0 > previous_slot_0) && (compute_slots_since_epoch_start(current_slot_0) == 0)) {
    return;
  }
  if store_0.best_justified_checkpoint.epoch > store_0.justified_checkpoint.epoch {
    store_0 := store_0.(justified_checkpoint := store_0.best_justified_checkpoint]);
  }
}

function method on_block(store_0: Store,signed_block_0: SignedBeaconBlock): Unit {
  var block_0 := signed_block_0.message;
  assert(block_0.parent_root in store_0.block_states)
  var pre_state_0 := copy(store_0.block_states[block_0.parent_root]);
  assert(get_current_slot(store_0) >= block_0.slot)
  var finalized_slot_0 := compute_start_slot_at_epoch(store_0.finalized_checkpoint.epoch);
  assert(block_0.slot > finalized_slot_0)
  assert(get_ancestor(store_0, block_0.parent_root, finalized_slot_0) == store_0.finalized_checkpoint.root)
  var state_0 := pre_state_0.copy();
  state_transition(state_0, signed_block_0, true);
  store_0 := store_0.(blocks := store_0.blocks[hash_tree_root(block_0) := block_0]]);
  store_0 := store_0.(block_states := store_0.block_states[hash_tree_root(block_0) := state_0]]);
  if state_0.current_justified_checkpoint.epoch > store_0.justified_checkpoint.epoch {
    if state_0.current_justified_checkpoint.epoch > store_0.best_justified_checkpoint.epoch {
      store_0 := store_0.(best_justified_checkpoint := state_0.current_justified_checkpoint]);
    }
    if should_update_justified_checkpoint(store_0, state_0.current_justified_checkpoint) {
      store_0 := store_0.(justified_checkpoint := state_0.current_justified_checkpoint]);
    }
  }
  if state_0.finalized_checkpoint.epoch > store_0.finalized_checkpoint.epoch {
    store_0 := store_0.(finalized_checkpoint := state_0.finalized_checkpoint]);
    if store_0.justified_checkpoint != state_0.current_justified_checkpoint {
      if state_0.current_justified_checkpoint.epoch > store_0.justified_checkpoint.epoch {
        store_0 := store_0.(justified_checkpoint := state_0.current_justified_checkpoint]);
        return;
      }
      var finalized_slot_1 := compute_start_slot_at_epoch(store_0.finalized_checkpoint.epoch);
      var ancestor_at_finalized_slot_0 := get_ancestor(store_0, store_0.justified_checkpoint.root, finalized_slot_1);
      if ancestor_at_finalized_slot_0 != store_0.finalized_checkpoint.root {
        store_0 := store_0.(justified_checkpoint := state_0.current_justified_checkpoint]);
      }
    }
  }
}

/*
    Run ``on_attestation`` upon receiving a new ``attestation`` from either within a block or directly on the wire.

    An ``attestation`` that is asserted as invalid may be valid at a later time,
    consider scheduling it for later processing in such case.
    */
function method on_attestation(store_0: Store,attestation_0: Attestation): Unit {
  validate_on_attestation(store_0, attestation_0);
  store_target_checkpoint_state(store_0, attestation_0.data.target);
  var target_state_0 := store_0.checkpoint_states[attestation_0.data.target];
  var indexed_attestation_0 := get_indexed_attestation(target_state_0, attestation_0);
  assert(is_valid_indexed_attestation(target_state_0, indexed_attestation_0))
  update_latest_messages(store_0, indexed_attestation_0.attesting_indices, attestation_0);
}

function method check_if_validator_active(state_0: BeaconState,validator_index_0: ValidatorIndex): bool {
  var validator_0 := state_0.validators[validator_index_0];
  return is_active_validator(validator_0, get_current_epoch(state_0));
}

/*
    Return the committee assignment in the ``epoch`` for ``validator_index``.
    ``assignment`` returned is a tuple of the following form:
        * ``assignmentlistOf(0)`` is the list of validators in the committee
        * ``assignmentlistOf(1)`` is the index to which the committee is assigned
        * ``assignmentlistOf(2)`` is the slot at which the committee is assigned
    Return None if no assignment.
    */
function method get_committee_assignment(state_0: BeaconState,epoch_0: Epoch,validator_index_0: ValidatorIndex): Optional<Tuple<Sequence<ValidatorIndex>,CommitteeIndex,Slot>> {
  var next_epoch_0 := new Epoch(get_current_epoch(state_0) + 1);
  assert(epoch_0 <= next_epoch_0)
  var start_slot_0 := compute_start_slot_at_epoch(epoch_0);
  var committee_count_per_slot_0 := get_committee_count_per_slot(state_0, epoch_0);
  var i62 := 0;
var coll63 := i62;
while i62 < |coll63|
  decreases |coll63| - i
{
var slot_0 := coll63[i62];
    var i64 := 0;
var coll65 := i64;
while i64 < |coll65|
  decreases |coll65| - i
{
var index_0 := coll65[i64];
      var committee_0 := get_beacon_committee(state_0, new Slot(slot_0), new CommitteeIndex(index_0));
      if validator_index_0 in committee_0 {
        return Triple(committee_0, new CommitteeIndex(index_0), new Slot(slot_0));
      }
    }
  }
  return null;
}

function method is_proposer(state_0: BeaconState,validator_index_0: ValidatorIndex): bool {
  return get_beacon_proposer_index(state_0) == validator_index_0;
}

function method get_epoch_signature(state_0: BeaconState,block_0: BeaconBlock,privkey_0: int): BLSSignature {
  var domain_0 := get_domain(state_0, DOMAIN_RANDAO, compute_epoch_at_slot(block_0.slot));
  var signing_root_0 := compute_signing_root(compute_epoch_at_slot(block_0.slot), domain_0);
  return bls.Sign(privkey_0, signing_root_0);
}

function method compute_time_at_slot(state_0: BeaconState,slot_0: Slot): uint64 {
  return new uint64(state_0.genesis_time + (slot_0 * SECONDS_PER_SLOT));
}

function method voting_period_start_time(state_0: BeaconState): uint64 {
  var eth1_voting_period_start_slot_0 := new Slot(state_0.slot - (state_0.slot % (EPOCHS_PER_ETH1_VOTING_PERIOD * SLOTS_PER_EPOCH)));
  return compute_time_at_slot(state_0, eth1_voting_period_start_slot_0);
}

function method is_candidate_block(block_0: Eth1Block,period_start_0: uint64): bool {
  return ((block_0.timestamp + (SECONDS_PER_ETH1_BLOCK * ETH1_FOLLOW_DISTANCE)) <= period_start_0) && ((block_0.timestamp + (SECONDS_PER_ETH1_BLOCK * ETH1_FOLLOW_DISTANCE * 2)) >= period_start_0);
}

function method get_eth1_vote(state_0: BeaconState,eth1_chain_0: Sequence<Eth1Block>): Eth1Data {
  var period_start_0 := voting_period_start_time(state_0);
  var votes_to_consider_0 := eth1_chain_0.filter((block) => is_candidate_block(block, period_start_0) && (get_eth1_data(block).deposit_count >= state_0.eth1_data.deposit_count)).map((block) => get_eth1_data(block));
  var valid_votes_0 := state_0.eth1_data_votes.filter((vote) => vote in votes_to_consider_0).map((vote) => vote);
  Eth1Data state_eth1_data_0 := state_0.eth1_data;
  var default_vote_0 := if (any(votes_to_consider_0)) then votes_to_consider_0[len(votes_to_consider_0) - 1] else state_eth1_data_0;
  return max(valid_votes_0, (v) => Pair(valid_votes_0.count(v), -valid_votes_0.index(v)), default_vote_0);
}

function method compute_new_state_root(state_0: BeaconState,block_0: BeaconBlock): Root {
  BeaconState temp_state_0 := state_0.copy();
  var signed_block_0 := new SignedBeaconBlock(block_0, SignedBeaconBlock.signature_default);
  state_transition(temp_state_0, signed_block_0, false);
  return hash_tree_root(temp_state_0);
}

function method get_block_signature(state_0: BeaconState,block_0: BeaconBlock,privkey_0: int): BLSSignature {
  var domain_0 := get_domain(state_0, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(block_0.slot));
  var signing_root_0 := compute_signing_root(block_0, domain_0);
  return bls.Sign(privkey_0, signing_root_0);
}

function method get_attestation_signature(state_0: BeaconState,attestation_data_0: AttestationData,privkey_0: int): BLSSignature {
  var domain_0 := get_domain(state_0, DOMAIN_BEACON_ATTESTER, attestation_data_0.target.epoch);
  var signing_root_0 := compute_signing_root(attestation_data_0, domain_0);
  return bls.Sign(privkey_0, signing_root_0);
}

/*
    Compute the correct subnet for an attestation for Phase 0.
    Note, this mimics expected Phase 1 behavior where attestations will be mapped to their shard subnet.
    */
function method compute_subnet_for_attestation(committees_per_slot_0: uint64,slot_0: Slot,committee_index_0: CommitteeIndex): uint64 {
  var slots_since_epoch_start_0 := new uint64(slot_0 % SLOTS_PER_EPOCH);
  var committees_since_epoch_start_0 := committees_per_slot_0 * slots_since_epoch_start_0;
  return new uint64((committees_since_epoch_start_0 + committee_index_0) % ATTESTATION_SUBNET_COUNT);
}

function method get_slot_signature(state_0: BeaconState,slot_0: Slot,privkey_0: int): BLSSignature {
  var domain_0 := get_domain(state_0, DOMAIN_SELECTION_PROOF, compute_epoch_at_slot(slot_0));
  var signing_root_0 := compute_signing_root(slot_0, domain_0);
  return bls.Sign(privkey_0, signing_root_0);
}

function method is_aggregator(state_0: BeaconState,slot_0: Slot,index_0: CommitteeIndex,slot_signature_0: BLSSignature): bool {
  var committee_0 := get_beacon_committee(state_0, slot_0, index_0);
  var modulo_0 := max(1, len(committee_0) / TARGET_AGGREGATORS_PER_COMMITTEE);
  return (bytes_to_uint64(hash(slot_signature_0)[0..8]) % modulo_0) == 0;
}

function method get_aggregate_signature(attestations_0: Sequence<Attestation>): BLSSignature {
  var signatures_0 := attestations_0.map((attestation) => attestation.signature);
  return bls.Aggregate(signatures_0);
}

function method get_aggregate_and_proof(state_0: BeaconState,aggregator_index_0: ValidatorIndex,aggregate_0: Attestation,privkey_0: int): AggregateAndProof {
  return new AggregateAndProof(aggregator_index_0, aggregate_0, get_slot_signature(state_0, aggregate_0.data.slot, privkey_0));
}

function method get_aggregate_and_proof_signature(state_0: BeaconState,aggregate_and_proof_0: AggregateAndProof,privkey_0: int): BLSSignature {
  var aggregate_0 := aggregate_and_proof_0.aggregate;
  var domain_0 := get_domain(state_0, DOMAIN_AGGREGATE_AND_PROOF, compute_epoch_at_slot(aggregate_0.data.slot));
  var signing_root_0 := compute_signing_root(aggregate_and_proof_0, domain_0);
  return bls.Sign(privkey_0, signing_root_0);
}

/*
    Returns the weak subjectivity period for the current ``state``.
    This computation takes into account the effect of:
        - validator set churn (bounded by ``get_validator_churn_limit()`` per epoch), and
        - validator balance top-ups (bounded by ``MAX_DEPOSITS * SLOTS_PER_EPOCH`` per epoch).
    A detailed calculation can be found at:
    https://github.com/runtimeverification/beacon-chain-verification/blob/master/weak-subjectivity/weak-subjectivity-analysis.pdf
    */
function method compute_weak_subjectivity_period(state_0: BeaconState): uint64 {
  var ws_period_0 := MIN_VALIDATOR_WITHDRAWABILITY_DELAY;
  var N_0 := len(get_active_validator_indices(state_0, get_current_epoch(state_0)));
  var t_0 := get_total_active_balance(state_0) / N_0 / ETH_TO_GWEI;
  var T_0 := MAX_EFFECTIVE_BALANCE / ETH_TO_GWEI;
  var delta_0 := get_validator_churn_limit(state_0);
  var Delta_0 := MAX_DEPOSITS * SLOTS_PER_EPOCH;
  var D_0 := SAFETY_DECAY;
  uint64 ws_period_3;
  if (T_0 * (200 + (3 * D_0))) < (t_0 * (200 + (12 * D_0))) {
    var epochs_for_validator_set_churn_0 := N_0 * ((t_0 * (200 + (12 * D_0))) - (T_0 * (200 + (3 * D_0)))) / (600 * delta_0 * ((2 * t_0) + T_0));
    var epochs_for_balance_top_ups_0 := N_0 * (200 + (3 * D_0)) / (600 * Delta_0);
    var ws_period_1 := ws_period_0 + max(epochs_for_validator_set_churn_0, epochs_for_balance_top_ups_0);
    ws_period_3 := ws_period_1;
  } else {
    var ws_period_2 := ws_period_0 + (3 * N_0 * D_0 * t_0 / (200 * Delta_0 * (T_0 - t_0)));
    ws_period_3 := ws_period_2;
  }
  return ws_period_3;
}

function method is_within_weak_subjectivity_period(store_0: Store,ws_state_0: BeaconState,ws_checkpoint_0: Checkpoint): bool {
  assert(ws_state_0.latest_block_header.state_root == ws_checkpoint_0.root)
  assert(compute_epoch_at_slot(ws_state_0.slot) == ws_checkpoint_0.epoch)
  var ws_period_0 := compute_weak_subjectivity_period(ws_state_0);
  var ws_state_epoch_0 := compute_epoch_at_slot(ws_state_0.slot);
  var current_epoch_0 := compute_epoch_at_slot(get_current_slot(store_0));
  return current_epoch_0 <= (ws_state_epoch_0 + ws_period_0);
}

/*
    A stub function return mocking Eth1Data.
    */
function method get_eth1_data(block_0: Eth1Block): Eth1Data {
  return new Eth1Data(block_0.deposit_root, block_0.deposit_count, hash_tree_root(block_0));
}
