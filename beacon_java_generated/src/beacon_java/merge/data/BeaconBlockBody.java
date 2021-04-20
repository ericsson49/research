package beacon_java.merge.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconBlockBody extends beacon_java.phase0.data.BeaconBlockBody {
  public static ExecutionPayload execution_payload_default = new ExecutionPayload();
  public ExecutionPayload execution_payload = execution_payload_default;
  public BeaconBlockBody copy() { return this; }
}
