package beacon_java.merge;

import beacon_java.merge.data.*;
import beacon_java.phase0.data.Hash32;
import beacon_java.pylib.pybool;
import beacon_java.ssz.Bytes32;
import beacon_java.ssz.uint64;

public class Utils {
  public static BeaconState copy(BeaconState s) { return s; }
  public static ExecutionPayload produce_execution_payload(Hash32 parent_hash, uint64 timestamp) { return null; }
  public static pybool verify_execution_state_transition(ExecutionPayload payload) { return null; }
  public static PowBlock get_pow_chain_head() { return null; }
  public static PowBlock get_pow_block(Hash32 block_hash) { return null; }

  public interface ExecutionEngine {
    pybool new_block(ExecutionPayload execution_payload);
    ExecutionPayload assemble_block(Hash32 block_hash, uint64 timestamp, Bytes32 random);
    pybool on_payload(ExecutionPayload executionPayload);
  }

  public final static ExecutionEngine EXECUTION_ENGINE = new ExecutionEngine() {
    @Override
    public pybool new_block(ExecutionPayload execution_payload) {
      return null;
    }

    @Override
    public ExecutionPayload assemble_block(Hash32 block_hash, uint64 timestamp, Bytes32 random) {
      return null;
    }

    @Override
    public pybool on_payload(ExecutionPayload execution_payload) {
      return null;
    }
  };
}
