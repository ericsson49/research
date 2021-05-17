package merge

import phase0.Epoch
import phase0.Version
import pylib.pow
import ssz.uint64

val MAX_BYTES_PER_OPAQUE_TRANSACTION = uint64(2uL.pow(20uL))
val MAX_EXECUTION_TRANSACTIONS = uint64(2uL.pow(14uL))
val BYTES_PER_LOGS_BLOOM = uint64(2uL.pow(8uL))
val MERGE_FORK_VERSION = Version("0x02000000")
val MERGE_FORK_EPOCH = Epoch(18446744073709551615uL)
val TRANSITION_TOTAL_DIFFICULTY = 2uL.pow(32uL)
