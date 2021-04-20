package merge

import pylib.pow
import ssz.uint64

val TRANSITION_TOTAL_DIFFICULTY = 2uL.pow(32uL)
val MAX_BYTES_PER_OPAQUE_TRANSACTION = uint64(2uL.pow(20uL))
val MAX_APPLICATION_TRANSACTIONS = uint64(2uL.pow(14uL))
val BYTES_PER_LOGS_BLOOM = uint64(2uL.pow(8uL))
