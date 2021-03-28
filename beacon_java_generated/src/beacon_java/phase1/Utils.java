package beacon_java.phase1;

import beacon_java.phase1.data.BeaconBlock;
import beacon_java.phase1.data.BeaconState;
import beacon_java.phase1.data.ShardState;
import beacon_java.phase1.data.Root;

public class Utils {
  public static BeaconBlock copy(BeaconBlock s) { return s; }
  public static BeaconState copy(BeaconState s) { return s; }
  public static ShardState copy(ShardState s) { return s; }
  public static Root hash_tree_root(Object o) { return null; }
}
