package beacon_java.merge;

import beacon_java.pylib.*;
import beacon_java.ssz.Bytes32;
import beacon_java.phase0.data.Epoch;
import beacon_java.phase0.data.Version;
import beacon_java.ssz.uint64;

import static beacon_java.pylib.Exports.*;


public interface Constants {
  uint64 MAX_BYTES_PER_OPAQUE_TRANSACTION = new uint64(pow(pyint.create(2L), pyint.create(20L)));
  uint64 MAX_TRANSACTIONS_PER_PAYLOAD = new uint64(pow(pyint.create(2L), pyint.create(14L)));
  uint64 BYTES_PER_LOGS_BLOOM = new uint64(pow(pyint.create(2L), pyint.create(8L)));
  uint64 GAS_LIMIT_DENOMINATOR = new uint64(pow(pyint.create(2L), pyint.create(10L)));
  uint64 MIN_GAS_LIMIT = new uint64(pyint.create(5000L));
  uint64 GENESIS_GAS_LIMIT = new uint64(pyint.create(30000000L));
  Bytes32 GENESIS_BASE_FEE_PER_GAS = new Bytes32("0x00ca9a3b00000000000000000000000000000000000000000000000000000000");
  Version MERGE_FORK_VERSION = new Version("0x02000000");
  Epoch MERGE_FORK_EPOCH = new Epoch(pyint.create("18446744073709551615"));
  pyint MIN_ANCHOR_POW_BLOCK_DIFFICULTY = pyint.create(0L);
  uint64 TARGET_SECONDS_TO_MERGE = new uint64(multiply(pyint.create(7L), pyint.create(86400L)));
}
