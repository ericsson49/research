package beacon_java.phase1.data;

import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CustodySlashing {
  public static uint64 data_index_default = uint64.ZERO;
  public static ValidatorIndex malefactor_index_default = new ValidatorIndex();
  public static BLSSignature malefactor_secret_default = new BLSSignature();
  public static ValidatorIndex whistleblower_index_default = new ValidatorIndex();
  public static ShardTransition shard_transition_default = new ShardTransition();
  public static Attestation attestation_default = new Attestation();
  public static SSZByteList data_default = new SSZByteList();
  public uint64 data_index = data_index_default;
  public ValidatorIndex malefactor_index = malefactor_index_default;
  public BLSSignature malefactor_secret = malefactor_secret_default;
  public ValidatorIndex whistleblower_index = whistleblower_index_default;
  public ShardTransition shard_transition = shard_transition_default;
  public Attestation attestation = attestation_default;
  public SSZByteList data = data_default;
  public CustodySlashing copy() { return this; }
}
