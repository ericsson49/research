package beacon_java.sharding.data;

import beacon_java.phase0.data.Gwei;
import beacon_java.phase0.data.PendingAttestation;
import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconState extends beacon_java.merge.data.BeaconState {
  public static SSZList<PendingAttestation> previous_epoch_attestations_default = new SSZList<PendingAttestation>();
  public static SSZList<PendingAttestation> current_epoch_attestations_default = new SSZList<PendingAttestation>();
  public static SSZList<PendingShardHeader> previous_epoch_pending_shard_headers_default = new SSZList<PendingShardHeader>();
  public static SSZList<PendingShardHeader> current_epoch_pending_shard_headers_default = new SSZList<PendingShardHeader>();
  public static SSZVector<SSZVector<DataCommitment>> grandparent_epoch_confirmed_commitments_default = new SSZVector<SSZVector<DataCommitment>>();
  public static Gwei shard_gasprice_default = new Gwei(pyint.create(0));
  public static Shard current_epoch_start_shard_default = new Shard();
  public SSZList<PendingAttestation> previous_epoch_attestations = previous_epoch_attestations_default;
  public SSZList<PendingAttestation> current_epoch_attestations = current_epoch_attestations_default;
  public SSZList<PendingShardHeader> previous_epoch_pending_shard_headers = previous_epoch_pending_shard_headers_default;
  public SSZList<PendingShardHeader> current_epoch_pending_shard_headers = current_epoch_pending_shard_headers_default;
  public SSZVector<SSZVector<DataCommitment>> grandparent_epoch_confirmed_commitments = grandparent_epoch_confirmed_commitments_default;
  public Gwei shard_gasprice = shard_gasprice_default;
  public Shard current_epoch_start_shard = current_epoch_start_shard_default;
  public BeaconState copy() { return this; }
}
