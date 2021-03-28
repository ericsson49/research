package beacon_java.phase1.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class LightClientVote {
  public static LightClientVoteData data_default = new LightClientVoteData();
  public static SSZBitvector aggregation_bits_default = new SSZBitvector();
  public static BLSSignature signature_default = new BLSSignature();
  public LightClientVoteData data = data_default;
  public SSZBitvector aggregation_bits = aggregation_bits_default;
  public BLSSignature signature = signature_default;
  public LightClientVote copy() { return this; }
}
