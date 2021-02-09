package beacon_java.phase1.data;

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
  public static SSZList<CustodyChunkChallenge> chunk_challenges_default = new SSZList<CustodyChunkChallenge>();
  public static SSZList<CustodyChunkResponse> chunk_challenge_responses_default = new SSZList<CustodyChunkResponse>();
  public static SSZList<CustodyKeyReveal> custody_key_reveals_default = new SSZList<CustodyKeyReveal>();
  public static SSZList<EarlyDerivedSecretReveal> early_derived_secret_reveals_default = new SSZList<EarlyDerivedSecretReveal>();
  public static SSZList<SignedCustodySlashing> custody_slashings_default = new SSZList<SignedCustodySlashing>();
  public static SSZVector<ShardTransition> shard_transitions_default = new SSZVector<ShardTransition>();
  public static SSZBitvector light_client_bits_default = new SSZBitvector();
  public static BLSSignature light_client_signature_default = new BLSSignature();
  public BLSSignature randao_reveal = randao_reveal_default;
  public Eth1Data eth1_data = eth1_data_default;
  public Bytes32 graffiti = graffiti_default;
  public SSZList<ProposerSlashing> proposer_slashings = proposer_slashings_default;
  public SSZList<AttesterSlashing> attester_slashings = attester_slashings_default;
  public SSZList<Attestation> attestations = attestations_default;
  public SSZList<Deposit> deposits = deposits_default;
  public SSZList<SignedVoluntaryExit> voluntary_exits = voluntary_exits_default;
  public SSZList<CustodyChunkChallenge> chunk_challenges = chunk_challenges_default;
  public SSZList<CustodyChunkResponse> chunk_challenge_responses = chunk_challenge_responses_default;
  public SSZList<CustodyKeyReveal> custody_key_reveals = custody_key_reveals_default;
  public SSZList<EarlyDerivedSecretReveal> early_derived_secret_reveals = early_derived_secret_reveals_default;
  public SSZList<SignedCustodySlashing> custody_slashings = custody_slashings_default;
  public SSZVector<ShardTransition> shard_transitions = shard_transitions_default;
  public SSZBitvector light_client_bits = light_client_bits_default;
  public BLSSignature light_client_signature = light_client_signature_default;
  public BeaconBlockBody copy() { return this; }
}
