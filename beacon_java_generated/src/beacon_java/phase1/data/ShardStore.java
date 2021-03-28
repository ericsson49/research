package beacon_java.phase1.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShardStore {
  public static Shard shard_default = new Shard();
  public static PyDict<Root,SignedShardBlock> signed_blocks_default = new PyDict<Root,SignedShardBlock>();
  public static PyDict<Root,ShardState> block_states_default = new PyDict<Root,ShardState>();
  public static PyDict<ValidatorIndex,ShardLatestMessage> latest_messages_default = new PyDict<ValidatorIndex,ShardLatestMessage>();
  public Shard shard = shard_default;
  public PyDict<Root,SignedShardBlock> signed_blocks = signed_blocks_default;
  public PyDict<Root,ShardState> block_states = block_states_default;
  public PyDict<ValidatorIndex,ShardLatestMessage> latest_messages = latest_messages_default;
  public ShardStore copy() { return this; }
}
