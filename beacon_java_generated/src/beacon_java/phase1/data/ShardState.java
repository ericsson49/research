package beacon_java.phase1.data;

import beacon_java.data.Root;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShardState {
  public static Slot slot_default = new Slot();
  public static Gwei gasprice_default = new Gwei();
  public static Root latest_block_root_default = new Root();
  public Slot slot = slot_default;
  public Gwei gasprice = gasprice_default;
  public Root latest_block_root = latest_block_root_default;
  public ShardState copy() { return this; }
}
