package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.Bytes32;
import beacon_java.ssz.Container;
import static beacon_java.phase0.Constants.DEPOSIT_CONTRACT_TREE_DEPTH;
import beacon_java.phase0.data.DepositData;
import beacon_java.ssz.SSZVector;

@Data @NoArgsConstructor @AllArgsConstructor
public class Deposit extends Container {
  public static SSZVector<Bytes32> proof_default = new SSZVector<Bytes32>();
  public static DepositData data_default = new DepositData();
  public SSZVector<Bytes32> proof = proof_default;
  public DepositData data = data_default;
  public Deposit copy() { return this; }
}
