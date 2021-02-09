package beacon_java.phase1.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class HistoricalBatch {
  public static SSZVector<Root> block_roots_default = new SSZVector<Root>();
  public static SSZVector<Root> state_roots_default = new SSZVector<Root>();
  public SSZVector<Root> block_roots = block_roots_default;
  public SSZVector<Root> state_roots = state_roots_default;
  public HistoricalBatch copy() { return this; }
}
