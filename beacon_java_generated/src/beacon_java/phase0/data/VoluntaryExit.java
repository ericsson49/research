package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Epoch;
import beacon_java.phase0.data.ValidatorIndex;

@Data @NoArgsConstructor @AllArgsConstructor
public class VoluntaryExit extends Container {
  public static Epoch epoch_default = new Epoch();
  public static ValidatorIndex validator_index_default = new ValidatorIndex();
  public Epoch epoch = epoch_default;
  public ValidatorIndex validator_index = validator_index_default;
  public VoluntaryExit copy() { return this; }
}
