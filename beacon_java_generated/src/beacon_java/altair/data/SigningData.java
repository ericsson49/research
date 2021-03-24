package beacon_java.altair.data;

import beacon_java.data.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SigningData {
  public static Root object_root_default = new Root();
  public static Domain domain_default = new Domain();
  public Root object_root = object_root_default;
  public Domain domain = domain_default;
  public SigningData copy() { return this; }
}
