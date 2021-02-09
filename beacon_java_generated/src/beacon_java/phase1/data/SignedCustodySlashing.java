package beacon_java.phase1.data;

import beacon_java.data.*;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignedCustodySlashing {
  public static CustodySlashing message_default = new CustodySlashing();
  public static BLSSignature signature_default = new BLSSignature();
  public CustodySlashing message = message_default;
  public BLSSignature signature = signature_default;
  public SignedCustodySlashing copy() { return this; }
}
