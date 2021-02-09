package beacon_java.phase1.data;

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
  public static uint64 next_custody_secret_to_reveal_default = uint64.ZERO;
  public static Epoch all_custody_secrets_revealed_epoch_default = new Epoch();
  public BLSPubkey pubkey = pubkey_default;
  public Bytes32 withdrawal_credentials = withdrawal_credentials_default;
  public Gwei effective_balance = effective_balance_default;
  public SSZBoolean slashed = slashed_default;
  public Epoch activation_eligibility_epoch = activation_eligibility_epoch_default;
  public Epoch activation_epoch = activation_epoch_default;
  public Epoch exit_epoch = exit_epoch_default;
  public Epoch withdrawable_epoch = withdrawable_epoch_default;
  public uint64 next_custody_secret_to_reveal = next_custody_secret_to_reveal_default;
  public Epoch all_custody_secrets_revealed_epoch = all_custody_secrets_revealed_epoch_default;
  public Validator copy() { return this; }
}
