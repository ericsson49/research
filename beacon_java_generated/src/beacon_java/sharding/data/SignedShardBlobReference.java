package beacon_java.sharding.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.ssz.Container;
import beacon_java.sharding.data.ShardBlobReference;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedShardBlobReference extends Container {
  public static ShardBlobReference message_default = new ShardBlobReference();
  public static BLSSignature signature_default = new BLSSignature();
  public ShardBlobReference message = message_default;
  public BLSSignature signature = signature_default;
  public SignedShardBlobReference copy() { return this; }
}
