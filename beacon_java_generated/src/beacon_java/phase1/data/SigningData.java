package beacon_java.phase1.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SigningData {
  public static Root object_root_default = new Root();
  public static Domain domain_default = new Domain();
  public Root object_root = object_root_default;
  public Domain domain = domain_default;
  public SigningData copy() { return this; }
}
