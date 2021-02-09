package beacon_java.phase1.data;

import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CustodyChunkChallenge {
  public static ValidatorIndex responder_index_default = new ValidatorIndex();
  public static ShardTransition shard_transition_default = new ShardTransition();
  public static Attestation attestation_default = new Attestation();
  public static uint64 data_index_default = uint64.ZERO;
  public static uint64 chunk_index_default = uint64.ZERO;
  public ValidatorIndex responder_index = responder_index_default;
  public ShardTransition shard_transition = shard_transition_default;
  public Attestation attestation = attestation_default;
  public uint64 data_index = data_index_default;
  public uint64 chunk_index = chunk_index_default;
  public CustodyChunkChallenge copy() { return this; }
}
