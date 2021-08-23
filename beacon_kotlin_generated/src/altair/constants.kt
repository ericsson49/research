package altair

import pylib.plus
import pylib.pow
import pylib.pybytes
import pylib.pyint
import pylib.times
import phase0.*
import pylib.PyList
import ssz.*

val TIMELY_SOURCE_FLAG_INDEX = pyint(0uL)
val TIMELY_TARGET_FLAG_INDEX = pyint(1uL)
val TIMELY_HEAD_FLAG_INDEX = pyint(2uL)
val TIMELY_SOURCE_WEIGHT = uint64(14uL)
val TIMELY_TARGET_WEIGHT = uint64(26uL)
val TIMELY_HEAD_WEIGHT = uint64(14uL)
val SYNC_REWARD_WEIGHT = uint64(2uL)
val PROPOSER_WEIGHT = uint64(8uL)
val WEIGHT_DENOMINATOR = uint64(64uL)
val G2_POINT_AT_INFINITY = BLSSignature(pybytes("xc0") + (pybytes("x00") * 95uL))
val PARTICIPATION_FLAG_WEIGHTS = PyList(TIMELY_SOURCE_WEIGHT, TIMELY_TARGET_WEIGHT, TIMELY_HEAD_WEIGHT)
val INACTIVITY_PENALTY_QUOTIENT_ALTAIR = uint64(3uL * 2uL.pow(24uL))
val MIN_SLASHING_PENALTY_QUOTIENT_ALTAIR = uint64(2uL.pow(6uL))
val PROPORTIONAL_SLASHING_MULTIPLIER_ALTAIR = uint64(2uL)
val SYNC_COMMITTEE_SIZE = uint64(2uL.pow(9uL))
val EPOCHS_PER_SYNC_COMMITTEE_PERIOD = uint64(2uL.pow(8uL))
val INACTIVITY_SCORE_BIAS = uint64(2uL.pow(2uL))
val INACTIVITY_SCORE_RECOVERY_RATE = uint64(2uL.pow(4uL))
val DOMAIN_SYNC_COMMITTEE = DomainType("0x07000000")
val DOMAIN_SYNC_COMMITTEE_SELECTION_PROOF = DomainType("0x08000000")
val DOMAIN_CONTRIBUTION_AND_PROOF = DomainType("0x09000000")
val ALTAIR_FORK_VERSION = Version("0x01000000")
val ALTAIR_FORK_EPOCH = Epoch(18446744073709551615uL)
val TARGET_AGGREGATORS_PER_SYNC_SUBCOMMITTEE = 2uL.pow(4uL)
val SYNC_COMMITTEE_SUBNET_COUNT = 4uL
val MIN_SYNC_COMMITTEE_PARTICIPANTS = 1uL
val FINALIZED_ROOT_INDEX = get_generalized_index(BeaconState::class, "finalized_checkpoint", "root")
val NEXT_SYNC_COMMITTEE_INDEX = get_generalized_index(BeaconState::class, "next_sync_committee")
