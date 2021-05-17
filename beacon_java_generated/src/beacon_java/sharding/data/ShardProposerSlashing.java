package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.sharding.data.SignedShardBlobReference;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShardProposerSlashing extends Container {
  public static SignedShardBlobReference signed_reference_1_default = new SignedShardBlobReference();
  public static SignedShardBlobReference signed_reference_2_default = new SignedShardBlobReference();
  public SignedShardBlobReference signed_reference_1 = signed_reference_1_default;
  public SignedShardBlobReference signed_reference_2 = signed_reference_2_default;
  public ShardProposerSlashing copy() { return this; }
}
