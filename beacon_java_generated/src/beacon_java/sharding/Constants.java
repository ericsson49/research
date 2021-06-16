package beacon_java.sharding;

import beacon_java.deps.FQ2;
import beacon_java.pylib.PyList;
import beacon_java.pylib.Sequence;
import beacon_java.pylib.pyint;
import beacon_java.sharding.data.BLSCommitment;
import beacon_java.phase0.data.DomainType;
import beacon_java.phase0.data.Epoch;
import beacon_java.phase0.data.Gwei;

import static beacon_java.pylib.Exports.*;
import static beacon_java.sharding.Constants.MAX_SAMPLES_PER_BLOCK;
import static beacon_java.sharding.Constants.MODULUS;
import static beacon_java.sharding.Constants.POINTS_PER_SAMPLE;
import static beacon_java.sharding.Constants.PRIMITIVE_ROOT_OF_UNITY;
import beacon_java.ssz.uint64;

public interface Constants {
  pyint PRIMITIVE_ROOT_OF_UNITY = pyint.create(5L);
  pyint DATA_AVAILABILITY_INVERSE_CODING_RATE = pow(pyint.create(2L), pyint.create(1L));
  uint64 POINTS_PER_SAMPLE = new uint64(pow(pyint.create(2L), pyint.create(3L)));
  pyint MODULUS = pyint.create("52435875175126190479447740508185965837690552500527637822603658699938581184513");
  uint64 MAX_SHARDS = new uint64(pow(pyint.create(2L), pyint.create(10L)));
  uint64 INITIAL_ACTIVE_SHARDS = new uint64(pow(pyint.create(2L), pyint.create(6L)));
  uint64 GASPRICE_ADJUSTMENT_COEFFICIENT = new uint64(pow(pyint.create(2L), pyint.create(3L)));
  pyint MAX_SHARD_HEADERS_PER_SHARD = pyint.create(4L);
  pyint MAX_SHARD_PROPOSER_SLASHINGS = pow(pyint.create(2L), pyint.create(4L));
  uint64 MAX_SAMPLES_PER_BLOCK = new uint64(pow(pyint.create(2L), pyint.create(11L)));
  uint64 TARGET_SAMPLES_PER_BLOCK = new uint64(pow(pyint.create(2L), pyint.create(10L)));
  PyList<BLSCommitment> G1_SETUP = new PyList<BLSCommitment>();
  PyList<Sequence<FQ2>> G2_SETUP = new PyList<Sequence<FQ2>>();
  pyint ROOT_OF_UNITY = pow(PRIMITIVE_ROOT_OF_UNITY, divide(minus(MODULUS, pyint.create(1L)), pyint.create(multiply(MAX_SAMPLES_PER_BLOCK, POINTS_PER_SAMPLE))), MODULUS);
  Gwei MAX_GASPRICE = new Gwei(pow(pyint.create(2L), pyint.create(33L)));
  Gwei MIN_GASPRICE = new Gwei(pow(pyint.create(2L), pyint.create(3L)));
  DomainType DOMAIN_SHARD_PROPOSER = new DomainType("0x80000000");
  DomainType DOMAIN_SHARD_COMMITTEE = new DomainType("0x81000000");
  pyint SHARD_WORK_UNCONFIRMED = pyint.create(0L);
  pyint SHARD_WORK_CONFIRMED = pyint.create(1L);
  pyint SHARD_WORK_PENDING = pyint.create(2L);
  uint64 SHARD_STATE_MEMORY_SLOTS = new uint64(pow(pyint.create(2L), pyint.create(8L)));
}
