package beacon_java.sharding.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconBlockBody extends beacon_java.merge.data.BeaconBlockBody {
  public static SSZList<ShardProposerSlashing> shard_proposer_slashings_default = new SSZList<ShardProposerSlashing>();
  public static SSZList<SignedShardBlobHeader> shard_headers_default = new SSZList<SignedShardBlobHeader>();
  public SSZList<ShardProposerSlashing> shard_proposer_slashings = shard_proposer_slashings_default;
  public SSZList<SignedShardBlobHeader> shard_headers = shard_headers_default;
  public BeaconBlockBody copy() { return this; }
}
