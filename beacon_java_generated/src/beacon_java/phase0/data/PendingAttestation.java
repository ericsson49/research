package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.AttestationData;
import beacon_java.ssz.SSZBitlist;
import beacon_java.ssz.Container;
import static beacon_java.phase0.Constants.MAX_VALIDATORS_PER_COMMITTEE;
import beacon_java.phase0.data.Slot;
import beacon_java.phase0.data.ValidatorIndex;

@Data @NoArgsConstructor @AllArgsConstructor
public class PendingAttestation extends Container {
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
