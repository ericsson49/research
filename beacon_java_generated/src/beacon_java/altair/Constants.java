package beacon_java.altair;

import beacon_java.altair.data.*;
import beacon_java.altair.data.BeaconState;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import beacon_java.phase0.data.*;

import static beacon_java.altair.Utils.get_generalized_index;
import static beacon_java.pylib.Exports.*;

public interface Constants {
  pyint TIMELY_HEAD_FLAG_INDEX = pyint.create(0L);
  pyint TIMELY_SOURCE_FLAG_INDEX = pyint.create(1L);
  pyint TIMELY_TARGET_FLAG_INDEX = pyint.create(2L);
  uint64 TIMELY_HEAD_WEIGHT = new uint64(pyint.create(12L));
  uint64 TIMELY_SOURCE_WEIGHT = new uint64(pyint.create(12L));
  uint64 TIMELY_TARGET_WEIGHT = new uint64(pyint.create(24L));
  uint64 SYNC_REWARD_WEIGHT = new uint64(pyint.create(8L));
  uint64 PROPOSER_WEIGHT = new uint64(pyint.create(8L));
  uint64 WEIGHT_DENOMINATOR = new uint64(pyint.create(64L));
  BLSSignature G2_POINT_AT_INFINITY = new BLSSignature(plus(pybytes.create("\\c0"), multiply(pybytes.create("\\00"), pyint.create(95L))));
  uint64 INACTIVITY_PENALTY_QUOTIENT_ALTAIR = new uint64(multiply(pyint.create(3L), power(pyint.create(2L), pyint.create(24L))));
  uint64 MIN_SLASHING_PENALTY_QUOTIENT_ALTAIR = new uint64(power(pyint.create(2L), pyint.create(6L)));
  uint64 PROPORTIONAL_SLASHING_MULTIPLIER_ALTAIR = new uint64(pyint.create(2L));
  uint64 SYNC_COMMITTEE_SIZE = new uint64(power(pyint.create(2L), pyint.create(10L)));
  uint64 SYNC_PUBKEYS_PER_AGGREGATE = new uint64(power(pyint.create(2L), pyint.create(6L)));
  uint64 INACTIVITY_SCORE_BIAS = new uint64(pyint.create(4L));
  Epoch EPOCHS_PER_SYNC_COMMITTEE_PERIOD = new Epoch(power(pyint.create(2L), pyint.create(8L)));
  DomainType DOMAIN_SYNC_COMMITTEE = new DomainType("0x07000000");
  DomainType DOMAIN_SYNC_COMMITTEE_SELECTION_PROOF = new DomainType("0x08000000");
  DomainType DOMAIN_CONTRIBUTION_AND_PROOF = new DomainType("0x09000000");
  Version ALTAIR_FORK_VERSION = new Version("0x01000000");
  Slot ALTAIR_FORK_SLOT = new Slot(pyint.create("18446744073709551615"));
  pyint TARGET_AGGREGATORS_PER_SYNC_SUBCOMMITTEE = power(pyint.create(2L), pyint.create(2L));
  pyint SYNC_COMMITTEE_SUBNET_COUNT = pyint.create(8L);
  pyint MIN_SYNC_COMMITTEE_PARTICIPANTS = pyint.create(1L);
  uint64 MAX_VALID_LIGHT_CLIENT_UPDATES = new uint64(minus(power(pyint.create(2L), pyint.create(64L)), pyint.create(1L)));
  Slot LIGHT_CLIENT_UPDATE_TIMEOUT = new Slot(power(pyint.create(2L), pyint.create(13L)));
  GeneralizedIndex FINALIZED_ROOT_INDEX = get_generalized_index(BeaconState.class, "finalized_checkpoint", "root");
  GeneralizedIndex NEXT_SYNC_COMMITTEE_INDEX = get_generalized_index(BeaconState.class, "next_sync_committee");
}
