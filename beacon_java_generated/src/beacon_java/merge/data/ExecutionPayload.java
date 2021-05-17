package beacon_java.merge.data;

import beacon_java.phase0.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ExecutionPayload {
  public static Hash32 block_hash_default = new Hash32();
  public static Hash32 parent_hash_default = new Hash32();
  public static Bytes20 coinbase_default = new Bytes20();
  public static Bytes32 state_root_default = new Bytes32();
  public static uint64 number_default = uint64.ZERO;
  public static uint64 gas_limit_default = uint64.ZERO;
  public static uint64 gas_used_default = uint64.ZERO;
  public static uint64 timestamp_default = uint64.ZERO;
  public static Bytes32 receipt_root_default = new Bytes32();
  public static SSZByteVector logs_bloom_default = new SSZByteVector();
  public static SSZList<OpaqueTransaction> transactions_default = new SSZList<OpaqueTransaction>();
  public Hash32 block_hash = block_hash_default;
  public Hash32 parent_hash = parent_hash_default;
  public Bytes20 coinbase = coinbase_default;
  public Bytes32 state_root = state_root_default;
  public uint64 number = number_default;
  public uint64 gas_limit = gas_limit_default;
  public uint64 gas_used = gas_used_default;
  public uint64 timestamp = timestamp_default;
  public Bytes32 receipt_root = receipt_root_default;
  public SSZByteVector logs_bloom = logs_bloom_default;
  public SSZList<OpaqueTransaction> transactions = transactions_default;
  public ExecutionPayload copy() { return this; }
}