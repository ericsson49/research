package beacon_java.phase1.data;

import beacon_java.data.Root;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CustodyChunkResponse {
  public static uint64 challenge_index_default = uint64.ZERO;
  public static uint64 chunk_index_default = uint64.ZERO;
  public static SSZByteVector chunk_default = new SSZByteVector();
  public static SSZVector<Root> branch_default = new SSZVector<Root>();
  public uint64 challenge_index = challenge_index_default;
  public uint64 chunk_index = chunk_index_default;
  public SSZByteVector chunk = chunk_default;
  public SSZVector<Root> branch = branch_default;
  public CustodyChunkResponse copy() { return this; }
}
