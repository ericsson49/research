package beacon_java.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class LightAggregateAndProof {
  public static ValidatorIndex aggregator_index_default = new ValidatorIndex();
  public static LightClientVote aggregate_default = new LightClientVote();
  public static BLSSignature selection_proof_default = new BLSSignature();
  public ValidatorIndex aggregator_index = aggregator_index_default;
  public LightClientVote aggregate = aggregate_default;
  public BLSSignature selection_proof = selection_proof_default;
  public LightAggregateAndProof copy() { return this; }
}
