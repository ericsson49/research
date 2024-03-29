package beacon_java.altair.data;

import lombok.*;
import beacon_java.pylib.*;
import beacon_java.phase0.data.BLSSignature;
import beacon_java.phase0.data.BeaconBlockHeader;
import beacon_java.ssz.SSZBitvector;
import beacon_java.ssz.Bytes32;
import beacon_java.ssz.Container;
import static beacon_java.altair.Constants.FINALIZED_ROOT_INDEX;
import static beacon_java.altair.Constants.NEXT_SYNC_COMMITTEE_INDEX;
import static beacon_java.altair.Constants.SYNC_COMMITTEE_SIZE;
import beacon_java.altair.data.SyncCommittee;
import beacon_java.ssz.SSZVector;
import beacon_java.phase0.data.Version;

@Data @NoArgsConstructor @AllArgsConstructor
public class LightClientUpdate extends Container {
  public static BeaconBlockHeader header_default = new BeaconBlockHeader();
  public static SyncCommittee next_sync_committee_default = new SyncCommittee();
  public static SSZVector<Bytes32> next_sync_committee_branch_default = new SSZVector<Bytes32>();
  public static BeaconBlockHeader finality_header_default = new BeaconBlockHeader();
  public static SSZVector<Bytes32> finality_branch_default = new SSZVector<Bytes32>();
  public static SSZBitvector sync_committee_bits_default = new SSZBitvector();
  public static BLSSignature sync_committee_signature_default = new BLSSignature();
  public static Version fork_version_default = new Version();
  public BeaconBlockHeader header = header_default;
  public SyncCommittee next_sync_committee = next_sync_committee_default;
  public SSZVector<Bytes32> next_sync_committee_branch = next_sync_committee_branch_default;
  public BeaconBlockHeader finality_header = finality_header_default;
  public SSZVector<Bytes32> finality_branch = finality_branch_default;
  public SSZBitvector sync_committee_bits = sync_committee_bits_default;
  public BLSSignature sync_committee_signature = sync_committee_signature_default;
  public Version fork_version = fork_version_default;
  public LightClientUpdate copy() { return this; }
}
