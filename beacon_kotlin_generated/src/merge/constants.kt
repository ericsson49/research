package merge

import phase0.Epoch
import phase0.Version
import pylib.pow
import pylib.pyint
import ssz.Bytes32
import ssz.uint256
import ssz.uint64

val MAX_BYTES_PER_OPAQUE_TRANSACTION = uint64(2uL.pow(20uL))
val MAX_TRANSACTIONS_PER_PAYLOAD = uint64(2uL.pow(14uL))
val BYTES_PER_LOGS_BLOOM = uint64(2uL.pow(8uL))
val GAS_LIMIT_DENOMINATOR = uint64(2uL.pow(10uL))
val MIN_GAS_LIMIT = uint64(5000uL)
val GENESIS_GAS_LIMIT = uint64(30000000uL)
val GENESIS_BASE_FEE_PER_GAS = Bytes32("0x00ca9a3b00000000000000000000000000000000000000000000000000000000")
val MERGE_FORK_VERSION = Version("0x02000000")
val MERGE_FORK_EPOCH = Epoch(18446744073709551615uL)
val MIN_ANCHOR_POW_BLOCK_DIFFICULTY: uint256 = pyint(0uL)
val TARGET_SECONDS_TO_MERGE = uint64(7uL * 86400uL)
