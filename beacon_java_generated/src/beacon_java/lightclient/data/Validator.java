package beacon_java.lightclient.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class Validator {
  public static BLSPubkey pubkey_default = new BLSPubkey();
  public static Bytes32 withdrawal_credentials_default = new Bytes32();
  public static Gwei effective_balance_default = new Gwei();
  public static SSZBoolean slashed_default = new SSZBoolean();
  public static Epoch activation_eligibility_epoch_default = new Epoch();
  public static Epoch activation_epoch_default = new Epoch();
  public static Epoch exit_epoch_default = new Epoch();
  public static Epoch withdrawable_epoch_default = new Epoch();
  public BLSPubkey pubkey = pubkey_default;
  public Bytes32 withdrawal_credentials = withdrawal_credentials_default;
  public Gwei effective_balance = effective_balance_default;
  public SSZBoolean slashed = slashed_default;
  public Epoch activation_eligibility_epoch = activation_eligibility_epoch_default;
  public Epoch activation_epoch = activation_epoch_default;
  public Epoch exit_epoch = exit_epoch_default;
  public Epoch withdrawable_epoch = withdrawable_epoch_default;
  public Validator copy() { return this; }
}
