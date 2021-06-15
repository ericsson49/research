package merge

import phase0.Epoch
import phase0.Version
import pylib.pow
import pylib.pyint
import ssz.uint256
import ssz.uint64

val MAX_BYTES_PER_OPAQUE_TRANSACTION = uint64(2uL.pow(20uL))
val MAX_EXECUTION_TRANSACTIONS = uint64(2uL.pow(14uL))
val BYTES_PER_LOGS_BLOOM = uint64(2uL.pow(8uL))
val MERGE_FORK_VERSION = Version("0x02000000")
val MERGE_FORK_EPOCH = Epoch(18446744073709551615uL)
val MIN_ANCHOR_POW_BLOCK_DIFFICULTY: uint256 = pyint(0uL)
val TARGET_SECONDS_TO_MERGE = uint64(7uL * 86400uL)
