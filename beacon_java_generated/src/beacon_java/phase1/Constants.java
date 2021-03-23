package beacon_java.phase1;

import beacon_java.data.BLSSignature;
import beacon_java.phase1.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;

import static beacon_java.pylib.Exports.*;
import static beacon_java.util.Exports.ceillog2;

public interface Constants {
  String CONFIG_NAME = "mainnet";
  Slot GENESIS_SLOT = new Slot(pyint.create(0L));
  Epoch GENESIS_EPOCH = new Epoch(pyint.create(0L));
  Epoch FAR_FUTURE_EPOCH = new Epoch(minus(power(pyint.create(2L), pyint.create(64L)), pyint.create(1L)));
  uint64 BASE_REWARDS_PER_EPOCH = new uint64(pyint.create(4L));
  uint64 DEPOSIT_CONTRACT_TREE_DEPTH = new uint64(power(pyint.create(2L), pyint.create(5L)));
  uint64 JUSTIFICATION_BITS_LENGTH = new uint64(pyint.create(4L));
  String ENDIANNESS = "little";
  uint64 ETH1_FOLLOW_DISTANCE = new uint64(power(pyint.create(2L), pyint.create(11L)));
  uint64 MAX_COMMITTEES_PER_SLOT = new uint64(power(pyint.create(2L), pyint.create(6L)));
  uint64 TARGET_COMMITTEE_SIZE = new uint64(power(pyint.create(2L), pyint.create(7L)));
  uint64 MAX_VALIDATORS_PER_COMMITTEE = new uint64(power(pyint.create(2L), pyint.create(11L)));
  uint64 MIN_PER_EPOCH_CHURN_LIMIT = new uint64(power(pyint.create(2L), pyint.create(2L)));
  uint64 CHURN_LIMIT_QUOTIENT = new uint64(power(pyint.create(2L), pyint.create(16L)));
  uint64 SHUFFLE_ROUND_COUNT = new uint64(pyint.create(90L));
  uint64 MIN_GENESIS_ACTIVE_VALIDATOR_COUNT = new uint64(power(pyint.create(2L), pyint.create(14L)));
  uint64 MIN_GENESIS_TIME = new uint64(pyint.create(1606824000L));
  uint64 HYSTERESIS_QUOTIENT = new uint64(pyint.create(4L));
  uint64 HYSTERESIS_DOWNWARD_MULTIPLIER = new uint64(pyint.create(1L));
  uint64 HYSTERESIS_UPWARD_MULTIPLIER = new uint64(pyint.create(5L));
  Gwei MIN_DEPOSIT_AMOUNT = new Gwei(multiply(power(pyint.create(2L), pyint.create(0L)), power(pyint.create(10L), pyint.create(9L))));
  Gwei MAX_EFFECTIVE_BALANCE = new Gwei(multiply(power(pyint.create(2L), pyint.create(5L)), power(pyint.create(10L), pyint.create(9L))));
  Gwei EJECTION_BALANCE = new Gwei(multiply(power(pyint.create(2L), pyint.create(4L)), power(pyint.create(10L), pyint.create(9L))));
  Gwei EFFECTIVE_BALANCE_INCREMENT = new Gwei(multiply(power(pyint.create(2L), pyint.create(0L)), power(pyint.create(10L), pyint.create(9L))));
  Version GENESIS_FORK_VERSION = new Version("0x00000000");
  Bytes1 BLS_WITHDRAWAL_PREFIX = new Bytes1("0x00");
  Bytes1 ETH1_ADDRESS_WITHDRAWAL_PREFIX = new Bytes1("0x01");
  uint64 GENESIS_DELAY = new uint64(pyint.create(604800L));
  uint64 SECONDS_PER_SLOT = new uint64(pyint.create(12L));
  uint64 SECONDS_PER_ETH1_BLOCK = new uint64(pyint.create(14L));
  uint64 MIN_ATTESTATION_INCLUSION_DELAY = new uint64(power(pyint.create(2L), pyint.create(0L)));
  uint64 SLOTS_PER_EPOCH = new uint64(power(pyint.create(2L), pyint.create(5L)));
  uint64 MIN_SEED_LOOKAHEAD = new uint64(power(pyint.create(2L), pyint.create(0L)));
  uint64 MAX_SEED_LOOKAHEAD = new uint64(power(pyint.create(2L), pyint.create(2L)));
  uint64 MIN_EPOCHS_TO_INACTIVITY_PENALTY = new uint64(power(pyint.create(2L), pyint.create(2L)));
  uint64 EPOCHS_PER_ETH1_VOTING_PERIOD = new uint64(power(pyint.create(2L), pyint.create(6L)));
  uint64 SLOTS_PER_HISTORICAL_ROOT = new uint64(power(pyint.create(2L), pyint.create(13L)));
  uint64 MIN_VALIDATOR_WITHDRAWABILITY_DELAY = new uint64(power(pyint.create(2L), pyint.create(8L)));
  uint64 SHARD_COMMITTEE_PERIOD = new uint64(power(pyint.create(2L), pyint.create(8L)));
  uint64 EPOCHS_PER_HISTORICAL_VECTOR = new uint64(power(pyint.create(2L), pyint.create(16L)));
  uint64 EPOCHS_PER_SLASHINGS_VECTOR = new uint64(power(pyint.create(2L), pyint.create(13L)));
  uint64 HISTORICAL_ROOTS_LIMIT = new uint64(power(pyint.create(2L), pyint.create(24L)));
  uint64 VALIDATOR_REGISTRY_LIMIT = new uint64(power(pyint.create(2L), pyint.create(40L)));
  uint64 BASE_REWARD_FACTOR = new uint64(power(pyint.create(2L), pyint.create(6L)));
  uint64 WHISTLEBLOWER_REWARD_QUOTIENT = new uint64(power(pyint.create(2L), pyint.create(9L)));
  uint64 PROPOSER_REWARD_QUOTIENT = new uint64(power(pyint.create(2L), pyint.create(3L)));
  uint64 INACTIVITY_PENALTY_QUOTIENT = new uint64(power(pyint.create(2L), pyint.create(26L)));
  uint64 MIN_SLASHING_PENALTY_QUOTIENT = new uint64(power(pyint.create(2L), pyint.create(7L)));
  uint64 PROPORTIONAL_SLASHING_MULTIPLIER = new uint64(pyint.create(1L));
  pyint MAX_PROPOSER_SLASHINGS = power(pyint.create(2L), pyint.create(4L));
  pyint MAX_ATTESTER_SLASHINGS = power(pyint.create(2L), pyint.create(1L));
  pyint MAX_ATTESTATIONS = power(pyint.create(2L), pyint.create(7L));
  pyint MAX_DEPOSITS = power(pyint.create(2L), pyint.create(4L));
  pyint MAX_VOLUNTARY_EXITS = power(pyint.create(2L), pyint.create(4L));
  DomainType DOMAIN_BEACON_PROPOSER = new DomainType("0x00000000");
  DomainType DOMAIN_BEACON_ATTESTER = new DomainType("0x01000000");
  DomainType DOMAIN_RANDAO = new DomainType("0x02000000");
  DomainType DOMAIN_DEPOSIT = new DomainType("0x03000000");
  DomainType DOMAIN_VOLUNTARY_EXIT = new DomainType("0x04000000");
  DomainType DOMAIN_SELECTION_PROOF = new DomainType("0x05000000");
  DomainType DOMAIN_AGGREGATE_AND_PROOF = new DomainType("0x06000000");
  pyint SAFE_SLOTS_TO_UPDATE_JUSTIFIED = power(pyint.create(2L), pyint.create(3L));
  pyint TARGET_AGGREGATORS_PER_COMMITTEE = power(pyint.create(2L), pyint.create(4L));
  pyint RANDOM_SUBNETS_PER_VALIDATOR = power(pyint.create(2L), pyint.create(0L));
  pyint EPOCHS_PER_RANDOM_SUBNET_SUBSCRIPTION = power(pyint.create(2L), pyint.create(8L));
  pyint ATTESTATION_SUBNET_COUNT = pyint.create(64L);
  uint64 ETH_TO_GWEI = new uint64(power(pyint.create(10L), pyint.create(9L)));
  uint64 SAFETY_DECAY = new uint64(pyint.create(10L));
  pyint CUSTODY_PRIME = pyint.create(minus(power(pyint.create(2L), pyint.create(256L)), pyint.create(189L)));
  uint64 CUSTODY_SECRETS = new uint64(pyint.create(3L));
  uint64 BYTES_PER_CUSTODY_ATOM = new uint64(pyint.create(32L));
  uint64 CUSTODY_PROBABILITY_EXPONENT = new uint64(pyint.create(10L));
  uint64 RANDAO_PENALTY_EPOCHS = new uint64(power(pyint.create(2L), pyint.create(1L)));
  uint64 EARLY_DERIVED_SECRET_PENALTY_MAX_FUTURE_EPOCHS = new uint64(power(pyint.create(2L), pyint.create(15L)));
  uint64 EPOCHS_PER_CUSTODY_PERIOD = new uint64(power(pyint.create(2L), pyint.create(14L)));
  uint64 CUSTODY_PERIOD_TO_RANDAO_PADDING = new uint64(power(pyint.create(2L), pyint.create(11L)));
  uint64 MAX_CHUNK_CHALLENGE_DELAY = new uint64(power(pyint.create(2L), pyint.create(15L)));
  uint64 MAX_CUSTODY_CHUNK_CHALLENGE_RECORDS = new uint64(power(pyint.create(2L), pyint.create(20L)));
  uint64 MAX_CUSTODY_KEY_REVEALS = new uint64(power(pyint.create(2L), pyint.create(8L)));
  uint64 MAX_EARLY_DERIVED_SECRET_REVEALS = new uint64(power(pyint.create(2L), pyint.create(0L)));
  uint64 MAX_CUSTODY_CHUNK_CHALLENGES = new uint64(power(pyint.create(2L), pyint.create(2L)));
  uint64 MAX_CUSTODY_CHUNK_CHALLENGE_RESPONSES = new uint64(power(pyint.create(2L), pyint.create(4L)));
  uint64 MAX_CUSTODY_SLASHINGS = new uint64(power(pyint.create(2L), pyint.create(0L)));
  uint64 EARLY_DERIVED_SECRET_REVEAL_SLOT_REWARD_MULTIPLE = new uint64(power(pyint.create(2L), pyint.create(1L)));
  uint64 MINOR_REWARD_QUOTIENT = new uint64(power(pyint.create(2L), pyint.create(8L)));
  uint64 MAX_SHARDS = new uint64(power(pyint.create(2L), pyint.create(10L)));
  uint64 INITIAL_ACTIVE_SHARDS = new uint64(power(pyint.create(2L), pyint.create(6L)));
  uint64 LIGHT_CLIENT_COMMITTEE_SIZE = new uint64(power(pyint.create(2L), pyint.create(7L)));
  uint64 GASPRICE_ADJUSTMENT_COEFFICIENT = new uint64(power(pyint.create(2L), pyint.create(3L)));
  uint64 MAX_SHARD_BLOCK_SIZE = new uint64(power(pyint.create(2L), pyint.create(20L)));
  uint64 TARGET_SHARD_BLOCK_SIZE = new uint64(power(pyint.create(2L), pyint.create(18L)));
  SSZList<uint64> SHARD_BLOCK_OFFSETS = new SSZList<>(PyList.of(
      new uint64(pyint.create(1L)), new uint64(pyint.create(2L)), new uint64(pyint.create(3L)), new uint64(pyint.create(5L)),
      new uint64(pyint.create(8L)), new uint64(pyint.create(13L)), new uint64(pyint.create(21L)), new uint64(pyint.create(34L)),
      new uint64(pyint.create(55L)), new uint64(pyint.create(89L)), new uint64(pyint.create(144L)), new uint64(pyint.create(233L))));
  pyint MAX_SHARD_BLOCKS_PER_ATTESTATION = len(SHARD_BLOCK_OFFSETS);
  uint64 BYTES_PER_CUSTODY_CHUNK = new uint64(power(pyint.create(2L), pyint.create(12L)));
  uint64 CUSTODY_RESPONSE_DEPTH = ceillog2(divide(MAX_SHARD_BLOCK_SIZE, BYTES_PER_CUSTODY_CHUNK));
  Gwei MAX_GASPRICE = new Gwei(power(pyint.create(2L), pyint.create(14L)));
  Gwei MIN_GASPRICE = new Gwei(power(pyint.create(2L), pyint.create(3L)));
  BLSSignature NO_SIGNATURE = new BLSSignature(multiply("\\x00", pyint.create(96L)));
  OnlineEpochs ONLINE_PERIOD = new OnlineEpochs(power(pyint.create(2L), pyint.create(3L)));
  Epoch LIGHT_CLIENT_COMMITTEE_PERIOD = new Epoch(power(pyint.create(2L), pyint.create(8L)));
  DomainType DOMAIN_SHARD_PROPOSAL = new DomainType("0x80000000");
  DomainType DOMAIN_SHARD_COMMITTEE = new DomainType("0x81000000");
  DomainType DOMAIN_LIGHT_CLIENT = new DomainType("0x82000000");
  DomainType DOMAIN_CUSTODY_BIT_SLASHING = new DomainType("0x83000000");
  DomainType DOMAIN_LIGHT_SELECTION_PROOF = new DomainType("0x84000000");
  DomainType DOMAIN_LIGHT_AGGREGATE_AND_PROOF = new DomainType("0x85000000");
  Version PHASE_1_FORK_VERSION = new Version("0x01000000");
  Slot PHASE_1_FORK_SLOT = new Slot(pyint.create(0L));
  pyint TARGET_LIGHT_CLIENT_AGGREGATORS_PER_SLOT = power(pyint.create(2L), pyint.create(3L));
  pyint LIGHT_CLIENT_PREPARATION_EPOCHS = power(pyint.create(2L), pyint.create(2L));
}
