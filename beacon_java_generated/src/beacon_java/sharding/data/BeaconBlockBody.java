package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.SSZList;
import static beacon_java.sharding.Constants.MAX_SHARDS;
import static beacon_java.sharding.Constants.MAX_SHARD_HEADERS_PER_SHARD;
import static beacon_java.sharding.Constants.MAX_SHARD_PROPOSER_SLASHINGS;
import beacon_java.sharding.data.ShardProposerSlashing;
import beacon_java.sharding.data.SignedShardBlobHeader;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconBlockBody extends beacon_java.merge.data.BeaconBlockBody {
  public static SSZList<ShardProposerSlashing> shard_proposer_slashings_default = new SSZList<ShardProposerSlashing>();
  public static SSZList<SignedShardBlobHeader> shard_headers_default = new SSZList<SignedShardBlobHeader>();
  public SSZList<ShardProposerSlashing> shard_proposer_slashings = shard_proposer_slashings_default;
  public SSZList<SignedShardBlobHeader> shard_headers = shard_headers_default;
  public BeaconBlockBody copy() { return this; }
  public BeaconBlockBody(beacon_java.phase0.data.BLSSignature randao_reveal, beacon_java.phase0.data.Eth1Data eth1_data, beacon_java.ssz.Bytes32 graffiti, SSZList<beacon_java.phase0.data.ProposerSlashing> proposer_slashings, SSZList<beacon_java.phase0.data.AttesterSlashing> attester_slashings, SSZList<beacon_java.phase0.data.Attestation> attestations, SSZList<beacon_java.phase0.data.Deposit> deposits, SSZList<beacon_java.phase0.data.SignedVoluntaryExit> voluntary_exits, beacon_java.merge.data.ExecutionPayload execution_payload, SSZList<ShardProposerSlashing> shard_proposer_slashings, SSZList<SignedShardBlobHeader> shard_headers) {
    super(randao_reveal, eth1_data, graffiti, proposer_slashings, attester_slashings, attestations, deposits, voluntary_exits, execution_payload);
    this.shard_proposer_slashings = shard_proposer_slashings;
    this.shard_headers = shard_headers;
  }
}
