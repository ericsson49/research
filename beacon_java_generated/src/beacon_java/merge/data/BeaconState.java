package beacon_java.merge.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.merge.data.ExecutionPayloadHeader;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconState extends beacon_java.phase0.data.BeaconState {
  public static ExecutionPayloadHeader latest_execution_payload_header_default = new ExecutionPayloadHeader();
  public ExecutionPayloadHeader latest_execution_payload_header = latest_execution_payload_header_default;
  public BeaconState copy() { return this; }
}
