package beacon_java.phase1.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PendingAttestation {
  public static SSZBitlist aggregation_bits_default = new SSZBitlist();
  public static AttestationData data_default = new AttestationData();
  public static Slot inclusion_delay_default = new Slot();
  public static ValidatorIndex proposer_index_default = new ValidatorIndex();
  public static SSZBoolean crosslink_success_default = new SSZBoolean();
  public SSZBitlist aggregation_bits = aggregation_bits_default;
  public AttestationData data = data_default;
  public Slot inclusion_delay = inclusion_delay_default;
  public ValidatorIndex proposer_index = proposer_index_default;
  public SSZBoolean crosslink_success = crosslink_success_default;
  public PendingAttestation copy() { return this; }
}
