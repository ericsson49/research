package beacon_java.merge;

import beacon_java.phase0.data.Epoch;
import beacon_java.phase0.data.Version;
import beacon_java.pylib.*;
import beacon_java.ssz.*;

import static beacon_java.pylib.Exports.pow;

public interface Constants {
  pyint TRANSITION_TOTAL_DIFFICULTY = pow(pyint.create(2L), pyint.create(32L));
  uint64 MAX_BYTES_PER_OPAQUE_TRANSACTION = new uint64(pow(pyint.create(2L), pyint.create(20L)));
  uint64 MAX_EXECUTION_TRANSACTIONS = new uint64(pow(pyint.create(2L), pyint.create(14L)));
  uint64 BYTES_PER_LOGS_BLOOM = new uint64(pow(pyint.create(2L), pyint.create(8L)));
  Version MERGE_FORK_VERSION = new Version("0x02000000");
  Epoch MERGE_FORK_EPOCH = new Epoch(pyint.create("18446744073709551615"));
}
