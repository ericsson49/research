package beacon_java.altair;

import beacon_java.altair.data.BeaconBlock;
import beacon_java.altair.data.BeaconState;
import beacon_java.altair.data.GeneralizedIndex;
import beacon_java.phase0.data.BLSPubkey;
import beacon_java.phase0.data.Root;

public class Utils {
  public static BLSPubkey copy(BLSPubkey s) { return s; }
  public static BeaconBlock copy(BeaconBlock s) { return s; }
  public static BeaconState copy(BeaconState s) { return s; }
  public static Root hash_tree_root(Object o) { return null; }
  public static GeneralizedIndex get_generalized_index(Class<?> ssz_class, String... p) { return null; }
}
