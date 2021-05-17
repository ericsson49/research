package beacon_java.altair.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.Attestation;
import beacon_java.phase0.data.AttesterSlashing;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.ssz.Bytes32;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Deposit;
import beacon_java.phase0.data.Eth1Data;
import beacon_java.ssz.SSZList;
import static beacon_java.phase0.Constants.MAX_ATTESTATIONS;
import static beacon_java.phase0.Constants.MAX_ATTESTER_SLASHINGS;
import static beacon_java.phase0.Constants.MAX_DEPOSITS;
import static beacon_java.phase0.Constants.MAX_PROPOSER_SLASHINGS;
import static beacon_java.phase0.Constants.MAX_VOLUNTARY_EXITS;
import beacon_java.phase0.data.ProposerSlashing;
import beacon_java.phase0.data.SignedVoluntaryExit;
import beacon_java.altair.data.SyncAggregate;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconBlockBody extends Container {
  public static BLSSignature randao_reveal_default = new BLSSignature();
  public static Eth1Data eth1_data_default = new Eth1Data();
  public static Bytes32 graffiti_default = new Bytes32();
  public static SSZList<ProposerSlashing> proposer_slashings_default = new SSZList<ProposerSlashing>();
  public static SSZList<AttesterSlashing> attester_slashings_default = new SSZList<AttesterSlashing>();
  public static SSZList<Attestation> attestations_default = new SSZList<Attestation>();
  public static SSZList<Deposit> deposits_default = new SSZList<Deposit>();
  public static SSZList<SignedVoluntaryExit> voluntary_exits_default = new SSZList<SignedVoluntaryExit>();
  public static SyncAggregate sync_aggregate_default = new SyncAggregate();
  public BLSSignature randao_reveal = randao_reveal_default;
  public Eth1Data eth1_data = eth1_data_default;
  public Bytes32 graffiti = graffiti_default;
  public SSZList<ProposerSlashing> proposer_slashings = proposer_slashings_default;
  public SSZList<AttesterSlashing> attester_slashings = attester_slashings_default;
  public SSZList<Attestation> attestations = attestations_default;
  public SSZList<Deposit> deposits = deposits_default;
  public SSZList<SignedVoluntaryExit> voluntary_exits = voluntary_exits_default;
  public SyncAggregate sync_aggregate = sync_aggregate_default;
  public BeaconBlockBody copy() { return this; }
}
