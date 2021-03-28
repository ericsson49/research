package beacon_java.altair.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SyncCommitteeContribution {
  public static Slot slot_default = new Slot();
  public static Root beacon_block_root_default = new Root();
  public static uint64 subcommittee_index_default = uint64.ZERO;
  public static SSZBitvector aggregation_bits_default = new SSZBitvector();
  public static BLSSignature signature_default = new BLSSignature();
  public Slot slot = slot_default;
  public Root beacon_block_root = beacon_block_root_default;
  public uint64 subcommittee_index = subcommittee_index_default;
  public SSZBitvector aggregation_bits = aggregation_bits_default;
  public BLSSignature signature = signature_default;
  public SyncCommitteeContribution copy() { return this; }
}
