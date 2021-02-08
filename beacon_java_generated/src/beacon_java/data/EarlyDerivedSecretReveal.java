package beacon_java.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class EarlyDerivedSecretReveal {
  public static ValidatorIndex revealed_index_default = new ValidatorIndex();
  public static Epoch epoch_default = new Epoch();
  public static BLSSignature reveal_default = new BLSSignature();
  public static ValidatorIndex masker_index_default = new ValidatorIndex();
  public static Bytes32 mask_default = new Bytes32();
  public ValidatorIndex revealed_index = revealed_index_default;
  public Epoch epoch = epoch_default;
  public BLSSignature reveal = reveal_default;
  public ValidatorIndex masker_index = masker_index_default;
  public Bytes32 mask = mask_default;
  public EarlyDerivedSecretReveal copy() { return this; }
}
