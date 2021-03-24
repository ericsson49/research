package beacon_java.altair.data;

import beacon_java.data.BLSSignature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
