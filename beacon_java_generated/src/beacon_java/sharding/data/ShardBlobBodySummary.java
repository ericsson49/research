package beacon_java.sharding.data;

import beacon_java.phase0.data.Root;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShardBlobBodySummary {
  public static DataCommitment commitment_default = new DataCommitment();
  public static BLSCommitment degree_proof_default = new BLSCommitment();
  public static Root data_root_default = new Root();
  public static Root beacon_block_root_default = new Root();
  public DataCommitment commitment = commitment_default;
  public BLSCommitment degree_proof = degree_proof_default;
  public Root data_root = data_root_default;
  public Root beacon_block_root = beacon_block_root_default;
  public ShardBlobBodySummary copy() { return this; }
}
