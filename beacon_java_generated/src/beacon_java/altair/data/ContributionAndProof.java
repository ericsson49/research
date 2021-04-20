package beacon_java.altair.data;

import beacon_java.phase0.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ContributionAndProof {
  public static ValidatorIndex aggregator_index_default = new ValidatorIndex();
  public static SyncCommitteeContribution contribution_default = new SyncCommitteeContribution();
  public static BLSSignature selection_proof_default = new BLSSignature();
  public ValidatorIndex aggregator_index = aggregator_index_default;
  public SyncCommitteeContribution contribution = contribution_default;
  public BLSSignature selection_proof = selection_proof_default;
  public ContributionAndProof copy() { return this; }
}
