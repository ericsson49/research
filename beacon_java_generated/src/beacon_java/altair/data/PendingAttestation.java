package beacon_java.altair.data;

import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PendingAttestation {
  public static SSZBitlist aggregation_bits_default = new SSZBitlist();
  public static AttestationData data_default = new AttestationData();
  public static Slot inclusion_delay_default = new Slot();
  public static ValidatorIndex proposer_index_default = new ValidatorIndex();
  public SSZBitlist aggregation_bits = aggregation_bits_default;
  public AttestationData data = data_default;
  public Slot inclusion_delay = inclusion_delay_default;
  public ValidatorIndex proposer_index = proposer_index_default;
  public PendingAttestation copy() { return this; }
}
