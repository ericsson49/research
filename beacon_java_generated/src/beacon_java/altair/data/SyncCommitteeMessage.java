package beacon_java.altair.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Root;
import beacon_java.phase0.data.Slot;
import beacon_java.phase0.data.ValidatorIndex;

@Data @NoArgsConstructor @AllArgsConstructor
public class SyncCommitteeMessage extends Container {
  public static Slot slot_default = new Slot();
  public static Root beacon_block_root_default = new Root();
  public static ValidatorIndex validator_index_default = new ValidatorIndex();
  public static BLSSignature signature_default = new BLSSignature();
  public Slot slot = slot_default;
  public Root beacon_block_root = beacon_block_root_default;
  public ValidatorIndex validator_index = validator_index_default;
  public BLSSignature signature = signature_default;
  public SyncCommitteeMessage copy() { return this; }
}
