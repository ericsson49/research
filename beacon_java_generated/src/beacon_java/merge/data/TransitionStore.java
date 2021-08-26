package beacon_java.merge.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.uint256;

@Data @NoArgsConstructor @AllArgsConstructor
public class TransitionStore extends Object {
  public static uint256 transition_total_difficulty_default = new uint256();
  public uint256 transition_total_difficulty = transition_total_difficulty_default;
  public TransitionStore copy() { return this; }
}
