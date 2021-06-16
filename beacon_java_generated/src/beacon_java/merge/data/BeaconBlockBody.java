package beacon_java.merge.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.merge.data.ExecutionPayload;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconBlockBody extends beacon_java.phase0.data.BeaconBlockBody {
  public static ExecutionPayload execution_payload_default = new ExecutionPayload();
  public ExecutionPayload execution_payload = execution_payload_default;
  public BeaconBlockBody copy() { return this; }
  public BeaconBlockBody(beacon_java.phase0.data.BLSSignature randao_reveal, beacon_java.phase0.data.Eth1Data eth1_data, beacon_java.ssz.Bytes32 graffiti, beacon_java.ssz.SSZList<beacon_java.phase0.data.ProposerSlashing> proposer_slashings, beacon_java.ssz.SSZList<beacon_java.phase0.data.AttesterSlashing> attester_slashings, beacon_java.ssz.SSZList<beacon_java.phase0.data.Attestation> attestations, beacon_java.ssz.SSZList<beacon_java.phase0.data.Deposit> deposits, beacon_java.ssz.SSZList<beacon_java.phase0.data.SignedVoluntaryExit> voluntary_exits, ExecutionPayload execution_payload) {
    super(randao_reveal, eth1_data, graffiti, proposer_slashings, attester_slashings, attestations, deposits, voluntary_exits);
    this.execution_payload = execution_payload;
  }
}
