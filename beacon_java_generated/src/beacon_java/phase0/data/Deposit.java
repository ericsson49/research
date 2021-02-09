package beacon_java.phase0.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class Deposit {
  public static SSZVector<Bytes32> proof_default = new SSZVector<Bytes32>();
  public static DepositData data_default = new DepositData();
  public SSZVector<Bytes32> proof = proof_default;
  public DepositData data = data_default;
  public Deposit copy() { return this; }
}
