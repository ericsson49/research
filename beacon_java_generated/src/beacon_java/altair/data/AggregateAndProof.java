package beacon_java.altair.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AggregateAndProof {
  public static ValidatorIndex aggregator_index_default = new ValidatorIndex();
  public static Attestation aggregate_default = new Attestation();
  public static BLSSignature selection_proof_default = new BLSSignature();
  public ValidatorIndex aggregator_index = aggregator_index_default;
  public Attestation aggregate = aggregate_default;
  public BLSSignature selection_proof = selection_proof_default;
  public AggregateAndProof copy() { return this; }
}
