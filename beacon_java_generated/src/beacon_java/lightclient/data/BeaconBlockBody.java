package beacon_java.lightclient.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconBlockBody {
  public static BLSSignature randao_reveal_default = new BLSSignature();
  public static Eth1Data eth1_data_default = new Eth1Data();
  public static Bytes32 graffiti_default = new Bytes32();
  public static SSZList<ProposerSlashing> proposer_slashings_default = new SSZList<ProposerSlashing>();
  public static SSZList<AttesterSlashing> attester_slashings_default = new SSZList<AttesterSlashing>();
  public static SSZList<Attestation> attestations_default = new SSZList<Attestation>();
  public static SSZList<Deposit> deposits_default = new SSZList<Deposit>();
  public static SSZList<SignedVoluntaryExit> voluntary_exits_default = new SSZList<SignedVoluntaryExit>();
  public static SSZBitvector sync_committee_bits_default = new SSZBitvector();
  public static BLSSignature sync_committee_signature_default = new BLSSignature();
  public BLSSignature randao_reveal = randao_reveal_default;
  public Eth1Data eth1_data = eth1_data_default;
  public Bytes32 graffiti = graffiti_default;
  public SSZList<ProposerSlashing> proposer_slashings = proposer_slashings_default;
  public SSZList<AttesterSlashing> attester_slashings = attester_slashings_default;
  public SSZList<Attestation> attestations = attestations_default;
  public SSZList<Deposit> deposits = deposits_default;
  public SSZList<SignedVoluntaryExit> voluntary_exits = voluntary_exits_default;
  public SSZBitvector sync_committee_bits = sync_committee_bits_default;
  public BLSSignature sync_committee_signature = sync_committee_signature_default;
  public BeaconBlockBody copy() { return this; }
}
