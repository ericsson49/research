package beacon_java.phase1.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CustodyChunkChallengeRecord {
  public static uint64 challenge_index_default = uint64.ZERO;
  public static ValidatorIndex challenger_index_default = new ValidatorIndex();
  public static ValidatorIndex responder_index_default = new ValidatorIndex();
  public static Epoch inclusion_epoch_default = new Epoch();
  public static Root data_root_default = new Root();
  public static uint64 chunk_index_default = uint64.ZERO;
  public uint64 challenge_index = challenge_index_default;
  public ValidatorIndex challenger_index = challenger_index_default;
  public ValidatorIndex responder_index = responder_index_default;
  public Epoch inclusion_epoch = inclusion_epoch_default;
  public Root data_root = data_root_default;
  public uint64 chunk_index = chunk_index_default;
  public CustodyChunkChallengeRecord copy() { return this; }
}
