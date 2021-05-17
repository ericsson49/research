package beacon_java.phase0.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BLSPubkey;
import beacon_java.ssz.Bytes32;
import beacon_java.ssz.Container;
import beacon_java.phase0.data.Gwei;

@Data @NoArgsConstructor @AllArgsConstructor
public class DepositMessage extends Container {
  public static BLSPubkey pubkey_default = new BLSPubkey();
  public static Bytes32 withdrawal_credentials_default = new Bytes32();
  public static Gwei amount_default = new Gwei();
  public BLSPubkey pubkey = pubkey_default;
  public Bytes32 withdrawal_credentials = withdrawal_credentials_default;
  public Gwei amount = amount_default;
  public DepositMessage copy() { return this; }
}
