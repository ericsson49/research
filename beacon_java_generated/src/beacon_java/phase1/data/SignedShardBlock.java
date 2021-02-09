package beacon_java.phase1.data;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedShardBlock {
  public static ShardBlock message_default = new ShardBlock();
  public static BLSSignature signature_default = new BLSSignature();
  public ShardBlock message = message_default;
  public BLSSignature signature = signature_default;
  public SignedShardBlock copy() { return this; }
}
