package beacon_java.merge.data;

import lombok.*;
import beacon_java.pylib.*;
import static beacon_java.merge.Constants.BYTES_PER_LOGS_BLOOM;
import beacon_java.ssz.SSZByteVector;
import beacon_java.ssz.Bytes20;
import beacon_java.ssz.Bytes32;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Hash32;
import beacon_java.ssz.SSZList;
import static beacon_java.merge.Constants.MAX_TRANSACTIONS_PER_PAYLOAD;
import beacon_java.merge.data.Transaction;
import beacon_java.ssz.uint64;

@Data @NoArgsConstructor @AllArgsConstructor
public class ExecutionPayload extends Container {
  public static Hash32 parent_hash_default = new Hash32();
  public static Bytes20 coinbase_default = new Bytes20();
  public static Bytes32 state_root_default = new Bytes32();
  public static Bytes32 receipt_root_default = new Bytes32();
  public static SSZByteVector logs_bloom_default = new SSZByteVector();
  public static Bytes32 random_default = new Bytes32();
  public static uint64 block_number_default = uint64.ZERO;
  public static uint64 gas_limit_default = uint64.ZERO;
  public static uint64 gas_used_default = uint64.ZERO;
  public static uint64 timestamp_default = uint64.ZERO;
  public static Bytes32 base_fee_per_gas_default = new Bytes32();
  public static Hash32 block_hash_default = new Hash32();
  public static SSZList<Transaction> transactions_default = new SSZList<Transaction>();
  public Hash32 parent_hash = parent_hash_default;
  public Bytes20 coinbase = coinbase_default;
  public Bytes32 state_root = state_root_default;
  public Bytes32 receipt_root = receipt_root_default;
  public SSZByteVector logs_bloom = logs_bloom_default;
  public Bytes32 random = random_default;
  public uint64 block_number = block_number_default;
  public uint64 gas_limit = gas_limit_default;
  public uint64 gas_used = gas_used_default;
  public uint64 timestamp = timestamp_default;
  public Bytes32 base_fee_per_gas = base_fee_per_gas_default;
  public Hash32 block_hash = block_hash_default;
  public SSZList<Transaction> transactions = transactions_default;
  public ExecutionPayload copy() { return this; }
}
