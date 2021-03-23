package beacon_java.deps;

import beacon_java.data.BLSPubkey;
import beacon_java.data.BLSSignature;
import beacon_java.data.Root;
import beacon_java.pylib.Sequence;
import beacon_java.pylib.Triple;
import beacon_java.pylib.pybool;
import beacon_java.pylib.pyint;
import beacon_java.ssz.Bytes32;

import static beacon_java.util.Exports.TODO;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public interface BLS {
  BLS bls = new BLS() {
    @Override
    public BLSSignature Sign(pyint privkey_0, Root signing_root_0) {
      return TODO(BLSSignature.class);
    }

    @Override
    public BLSSignature Aggregate(Iterable<BLSSignature> signatures_0) {
      return TODO(BLSSignature.class);
    }

    @Override
    public BLSPubkey AggregatePKs(Iterable<BLSPubkey> pubkeys) {
      return TODO(BLSPubkey.class);
    }

    @Override
    public pybool Verify(BLSPubkey pubkey, Root signing_root_0, BLSSignature signature) {
      return TODO(pybool.class);
    }

    @Override
    public pybool FastAggregateVerify(Sequence<BLSPubkey> pubkeys_0, Bytes32 message_0, BLSSignature signature_0) {
      return TODO(pybool.class);
    }

    @Override
    public pybool AggregateVerify(Sequence<BLSPubkey> pubkeys_0, Sequence<? extends Bytes32> messages_0, BLSSignature signature_0) {
      return TODO(pybool.class);
    }

    @Override
    public Triple<FQ2, FQ2, FQ2> signature_to_G2(BLSSignature key_0) {
      return TODO(Triple.class);
    }
  };

  BLSSignature Sign(pyint privkey_0, Root signing_root_0);

  BLSSignature Aggregate(Iterable<BLSSignature> signatures_0);

  BLSPubkey AggregatePKs(Iterable<BLSPubkey> pks);

  pybool Verify(BLSPubkey pubkey, Root signing_root_0, BLSSignature signature);

  pybool FastAggregateVerify(Sequence<BLSPubkey> pubkeys_0, Bytes32 message_0, BLSSignature signature_0);

  pybool AggregateVerify(Sequence<BLSPubkey> pubkeys_0, Sequence<? extends Bytes32> messages_0, BLSSignature signature_0);

  Triple<FQ2, FQ2, FQ2> signature_to_G2(BLSSignature key_0);
}
