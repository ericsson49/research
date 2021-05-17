package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Domain;
import beacon_java.phase0.data.Root;

@Data @NoArgsConstructor @AllArgsConstructor
public class SigningData extends Container {
  public static Root object_root_default = new Root();
  public static Domain domain_default = new Domain();
  public Root object_root = object_root_default;
  public Domain domain = domain_default;
  public SigningData copy() { return this; }
}
