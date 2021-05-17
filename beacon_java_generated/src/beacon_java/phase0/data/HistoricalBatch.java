package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Root;
import static beacon_java.phase0.Constants.SLOTS_PER_HISTORICAL_ROOT;
import beacon_java.ssz.SSZVector;

@Data @NoArgsConstructor @AllArgsConstructor
public class HistoricalBatch extends Container {
  public static SSZVector<Root> block_roots_default = new SSZVector<Root>();
  public static SSZVector<Root> state_roots_default = new SSZVector<Root>();
  public SSZVector<Root> block_roots = block_roots_default;
  public SSZVector<Root> state_roots = state_roots_default;
  public HistoricalBatch copy() { return this; }
}
