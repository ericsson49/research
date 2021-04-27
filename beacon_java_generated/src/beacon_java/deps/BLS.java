package beacon_java.deps;

import beacon_java.pylib.Sequence;
import beacon_java.pylib.Triple;
import beacon_java.pylib.pybool;
import beacon_java.pylib.pyint;
import beacon_java.sharding.data.BLSCommitment;
import beacon_java.ssz.Bytes32;
import beacon_java.ssz.Bytes48;
import beacon_java.ssz.Bytes96;

import static beacon_java.util.Exports.TODO;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public interface BLS {
  BLS bls = new BLS() {
    @Override
    public <T extends Bytes96> T  Sign(pyint privkey_0, Bytes32 signing_root_0) {
      throw new RuntimeException("TODO: not yet implemented");
    }

    @Override
    public <T extends Bytes96> T Aggregate(Iterable<T> signatures_0) {
      throw new RuntimeException("TODO: not yet implemented");
    }

    @Override
    public <T extends Bytes48> T AggregatePKs(Iterable<T> pubkeys) {
      throw new RuntimeException("TODO: not yet implemented");
    }

    @Override
    public pybool Verify(Bytes48 pubkey, Bytes32 signing_root_0, Bytes96 signature) {
      return TODO(pybool.class);
    }

    @Override
    public pybool FastAggregateVerify(Sequence<? extends Bytes48> pubkeys_0, Bytes32 message_0, Bytes96 signature_0) {
      return TODO(pybool.class);
    }

    @Override
    public pybool AggregateVerify(Sequence<? extends Bytes48> pubkeys_0, Sequence<? extends Bytes32> messages_0, Bytes96 signature_0) {
      return TODO(pybool.class);
    }

    @Override
    public Triple<FQ2, FQ2, FQ2> signature_to_G2(Bytes96 key_0) {
      return TODO(Triple.class);
    }

    @Override
    public Object Pairing(Bytes48 p1, Sequence<FQ2> p2) { return TODO(Object.class); }
  };

  <T extends Bytes96> T Sign(pyint privkey_0, Bytes32 signing_root_0);

  <T extends Bytes96> T Aggregate(Iterable<T> signatures_0);

  <T extends Bytes48> T AggregatePKs(Iterable<T> pubkeys);

  pybool Verify(Bytes48 pubkey, Bytes32 signing_root_0, Bytes96 signature);

  pybool FastAggregateVerify(Sequence<? extends Bytes48> pubkeys_0, Bytes32 message_0, Bytes96 signature_0);

  pybool AggregateVerify(Sequence<? extends Bytes48> pubkeys_0, Sequence<? extends Bytes32> messages_0, Bytes96 signature_0);

  Triple<FQ2, FQ2, FQ2> signature_to_G2(Bytes96 key_0);

  Object Pairing(Bytes48 p1, Sequence<FQ2> p2);
}
