package phase0

import phase1.Version
import pylib.pow
import ssz.Bytes
import ssz.Bytes1
import ssz.Bytes32
import ssz.Bytes4
import ssz.uint64

val CONFIG_NAME = "mainnet"
val GENESIS_SLOT = Slot(0uL)
val GENESIS_EPOCH = Epoch(0uL)
val FAR_FUTURE_EPOCH = Epoch(2uL.pow(64uL) - 1uL)
val BASE_REWARDS_PER_EPOCH = uint64(4uL)
val DEPOSIT_CONTRACT_TREE_DEPTH = uint64(2uL.pow(5uL))
val JUSTIFICATION_BITS_LENGTH = uint64(4uL)
val ENDIANNESS = "little"
val ETH1_FOLLOW_DISTANCE = uint64(2uL.pow(11uL))
val MAX_COMMITTEES_PER_SLOT = uint64(2uL.pow(6uL))
val TARGET_COMMITTEE_SIZE = uint64(2uL.pow(7uL))
val MAX_VALIDATORS_PER_COMMITTEE = uint64(2uL.pow(11uL))
val MIN_PER_EPOCH_CHURN_LIMIT = uint64(2uL.pow(2uL))
val CHURN_LIMIT_QUOTIENT = uint64(2uL.pow(16uL))
val SHUFFLE_ROUND_COUNT = uint64(90uL)
val MIN_GENESIS_ACTIVE_VALIDATOR_COUNT = uint64(2uL.pow(14uL))
val MIN_GENESIS_TIME = uint64(1606824000uL)
val HYSTERESIS_QUOTIENT = uint64(4uL)
val HYSTERESIS_DOWNWARD_MULTIPLIER = uint64(1uL)
val HYSTERESIS_UPWARD_MULTIPLIER = uint64(5uL)
val MIN_DEPOSIT_AMOUNT = Gwei(2uL.pow(0uL) * 10uL.pow(9uL))
val MAX_EFFECTIVE_BALANCE = Gwei(2uL.pow(5uL) * 10uL.pow(9uL))
val EJECTION_BALANCE = Gwei(2uL.pow(4uL) * 10uL.pow(9uL))
val EFFECTIVE_BALANCE_INCREMENT = Gwei(2uL.pow(0uL) * 10uL.pow(9uL))
val GENESIS_FORK_VERSION = Version("0x00000000")
val BLS_WITHDRAWAL_PREFIX = Bytes1("0x00")
val ETH1_ADDRESS_WITHDRAWAL_PREFIX = Bytes1("0x01")
val GENESIS_DELAY = uint64(604800uL)
val SECONDS_PER_SLOT = uint64(12uL)
val SECONDS_PER_ETH1_BLOCK = uint64(14uL)
val MIN_ATTESTATION_INCLUSION_DELAY = uint64(2uL.pow(0uL))
val SLOTS_PER_EPOCH = uint64(2uL.pow(5uL))
val MIN_SEED_LOOKAHEAD = uint64(2uL.pow(0uL))
val MAX_SEED_LOOKAHEAD = uint64(2uL.pow(2uL))
val MIN_EPOCHS_TO_INACTIVITY_PENALTY = uint64(2uL.pow(2uL))
val EPOCHS_PER_ETH1_VOTING_PERIOD = uint64(2uL.pow(6uL))
val SLOTS_PER_HISTORICAL_ROOT = uint64(2uL.pow(13uL))
val MIN_VALIDATOR_WITHDRAWABILITY_DELAY = uint64(2uL.pow(8uL))
val SHARD_COMMITTEE_PERIOD = uint64(2uL.pow(8uL))
val EPOCHS_PER_HISTORICAL_VECTOR = uint64(2uL.pow(16uL))
val EPOCHS_PER_SLASHINGS_VECTOR = uint64(2uL.pow(13uL))
val HISTORICAL_ROOTS_LIMIT = uint64(2uL.pow(24uL))
val VALIDATOR_REGISTRY_LIMIT = uint64(2uL.pow(40uL))
val BASE_REWARD_FACTOR = uint64(2uL.pow(6uL))
val WHISTLEBLOWER_REWARD_QUOTIENT = uint64(2uL.pow(9uL))
val PROPOSER_REWARD_QUOTIENT = uint64(2uL.pow(3uL))
val INACTIVITY_PENALTY_QUOTIENT = uint64(2uL.pow(26uL))
val MIN_SLASHING_PENALTY_QUOTIENT = uint64(2uL.pow(7uL))
val PROPORTIONAL_SLASHING_MULTIPLIER = uint64(1uL)
val MAX_PROPOSER_SLASHINGS = 2uL.pow(4uL)
val MAX_ATTESTER_SLASHINGS = 2uL.pow(1uL)
val MAX_ATTESTATIONS = 2uL.pow(7uL)
val MAX_DEPOSITS = 2uL.pow(4uL)
val MAX_VOLUNTARY_EXITS = 2uL.pow(4uL)
val DOMAIN_BEACON_PROPOSER = DomainType("0x00000000")
val DOMAIN_BEACON_ATTESTER = DomainType("0x01000000")
val DOMAIN_RANDAO = DomainType("0x02000000")
val DOMAIN_DEPOSIT = DomainType("0x03000000")
val DOMAIN_VOLUNTARY_EXIT = DomainType("0x04000000")
val DOMAIN_SELECTION_PROOF = DomainType("0x05000000")
val DOMAIN_AGGREGATE_AND_PROOF = DomainType("0x06000000")
val SAFE_SLOTS_TO_UPDATE_JUSTIFIED = 2uL.pow(3uL)
val TARGET_AGGREGATORS_PER_COMMITTEE = 2uL.pow(4uL)
val RANDOM_SUBNETS_PER_VALIDATOR = 2uL.pow(0uL)
val EPOCHS_PER_RANDOM_SUBNET_SUBSCRIPTION = 2uL.pow(8uL)
val ATTESTATION_SUBNET_COUNT = 64uL
val ETH_TO_GWEI = uint64(10uL.pow(9uL))
val SAFETY_DECAY = uint64(10uL)
