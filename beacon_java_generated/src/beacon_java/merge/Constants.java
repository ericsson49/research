package beacon_java.merge;

import beacon_java.pylib.*;
import beacon_java.ssz.*;

import static beacon_java.pylib.Exports.power;

public interface Constants {
  pyint TRANSITION_TOTAL_DIFFICULTY = power(pyint.create(2L), pyint.create(32L));
  uint64 MAX_BYTES_PER_OPAQUE_TRANSACTION = new uint64(power(pyint.create(2L), pyint.create(20L)));
  uint64 MAX_APPLICATION_TRANSACTIONS = new uint64(power(pyint.create(2L), pyint.create(14L)));
  uint64 BYTES_PER_LOGS_BLOOM = new uint64(power(pyint.create(2L), pyint.create(8L)));
}
