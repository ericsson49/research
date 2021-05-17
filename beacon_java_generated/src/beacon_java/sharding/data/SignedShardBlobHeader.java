package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.ssz.Container;
import beacon_java.sharding.data.ShardBlobHeader;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedShardBlobHeader extends Container {
  public static ShardBlobHeader message_default = new ShardBlobHeader();
  public static BLSSignature signature_default = new BLSSignature();
  public ShardBlobHeader message = message_default;
  public BLSSignature signature = signature_default;
  public SignedShardBlobHeader copy() { return this; }
}
