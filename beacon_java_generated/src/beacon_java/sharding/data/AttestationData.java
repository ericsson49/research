package beacon_java.sharding.data;

import beacon_java.phase0.data.Root;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AttestationData extends beacon_java.phase0.data.AttestationData {
  public static Root shard_header_root_default = new Root();
  public Root shard_header_root = shard_header_root_default;
  public AttestationData copy() { return this; }
}
