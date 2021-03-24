package beacon_java.altair.data;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class VoluntaryExit {
  public static Epoch epoch_default = new Epoch();
  public static ValidatorIndex validator_index_default = new ValidatorIndex();
  public Epoch epoch = epoch_default;
  public ValidatorIndex validator_index = validator_index_default;
  public VoluntaryExit copy() { return this; }
}