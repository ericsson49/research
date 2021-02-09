package beacon_java.phase1.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class DepositData {
  public static BLSPubkey pubkey_default = new BLSPubkey();
  public static Bytes32 withdrawal_credentials_default = new Bytes32();
  public static Gwei amount_default = new Gwei();
  public static BLSSignature signature_default = new BLSSignature();
  public BLSPubkey pubkey = pubkey_default;
  public Bytes32 withdrawal_credentials = withdrawal_credentials_default;
  public Gwei amount = amount_default;
  public BLSSignature signature = signature_default;
  public DepositData copy() { return this; }
}
