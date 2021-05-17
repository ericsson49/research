package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.Attestation;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.ValidatorIndex;

@Data @NoArgsConstructor @AllArgsConstructor
public class AggregateAndProof extends Container {
  public static ValidatorIndex aggregator_index_default = new ValidatorIndex();
  public static Attestation aggregate_default = new Attestation();
  public static BLSSignature selection_proof_default = new BLSSignature();
  public ValidatorIndex aggregator_index = aggregator_index_default;
  public Attestation aggregate = aggregate_default;
  public BLSSignature selection_proof = selection_proof_default;
  public AggregateAndProof copy() { return this; }
}
