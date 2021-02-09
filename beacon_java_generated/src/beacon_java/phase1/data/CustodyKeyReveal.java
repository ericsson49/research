package beacon_java.phase1.data;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CustodyKeyReveal {
  public static ValidatorIndex revealer_index_default = new ValidatorIndex();
  public static BLSSignature reveal_default = new BLSSignature();
  public ValidatorIndex revealer_index = revealer_index_default;
  public BLSSignature reveal = reveal_default;
  public CustodyKeyReveal copy() { return this; }
}
