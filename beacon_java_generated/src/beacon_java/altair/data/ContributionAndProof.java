package beacon_java.altair.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.ssz.Container;
import beacon_java.altair.data.SyncCommitteeContribution;
import beacon_java.phase0.data.ValidatorIndex;

@Data @NoArgsConstructor @AllArgsConstructor
public class ContributionAndProof extends Container {
  public static ValidatorIndex aggregator_index_default = new ValidatorIndex();
  public static SyncCommitteeContribution contribution_default = new SyncCommitteeContribution();
  public static BLSSignature selection_proof_default = new BLSSignature();
  public ValidatorIndex aggregator_index = aggregator_index_default;
  public SyncCommitteeContribution contribution = contribution_default;
  public BLSSignature selection_proof = selection_proof_default;
  public ContributionAndProof copy() { return this; }
}
