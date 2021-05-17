package beacon_java.altair.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.ssz.uint8;

public class ParticipationFlags extends uint8 {
  public ParticipationFlags(uint8 value) { super(value); }
  public ParticipationFlags() { super(uint8.ZERO); }
  public ParticipationFlags(pyint value) { super(value); }
}
