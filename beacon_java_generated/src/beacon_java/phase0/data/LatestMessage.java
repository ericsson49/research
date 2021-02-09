package beacon_java.phase0.data;

import beacon_java.data.Root;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class LatestMessage {
  public static Epoch epoch_default = new Epoch();
  public static Root root_default = new Root();
  public Epoch epoch = epoch_default;
  public Root root = root_default;
  public LatestMessage copy() { return this; }
}
