package beacon_java.altair.data;

import beacon_java.phase0.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SyncCommitteeSignature {
  public static Slot slot_default = new Slot();
  public static Root beacon_block_root_default = new Root();
  public static ValidatorIndex validator_index_default = new ValidatorIndex();
  public static BLSSignature signature_default = new BLSSignature();
  public Slot slot = slot_default;
  public Root beacon_block_root = beacon_block_root_default;
  public ValidatorIndex validator_index = validator_index_default;
  public BLSSignature signature = signature_default;
  public SyncCommitteeSignature copy() { return this; }
}
