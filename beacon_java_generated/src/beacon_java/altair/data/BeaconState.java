package beacon_java.altair.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;
import beacon_java.phase0.data.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class BeaconState {
  public static uint64 genesis_time_default = uint64.ZERO;
  public static Root genesis_validators_root_default = new Root();
  public static Slot slot_default = new Slot();
  public static Fork fork_default = new Fork();
  public static BeaconBlockHeader latest_block_header_default = new BeaconBlockHeader();
  public static SSZVector<Root> block_roots_default = new SSZVector<Root>();
  public static SSZVector<Root> state_roots_default = new SSZVector<Root>();
  public static SSZList<Root> historical_roots_default = new SSZList<Root>();
  public static Eth1Data eth1_data_default = new Eth1Data();
  public static SSZList<Eth1Data> eth1_data_votes_default = new SSZList<Eth1Data>();
  public static uint64 eth1_deposit_index_default = uint64.ZERO;
  public static SSZList<Validator> validators_default = new SSZList<Validator>();
  public static SSZList<Gwei> balances_default = new SSZList<Gwei>();
  public static SSZVector<Bytes32> randao_mixes_default = new SSZVector<Bytes32>();
  public static SSZVector<Gwei> slashings_default = new SSZVector<Gwei>();
  public static SSZList<ParticipationFlags> previous_epoch_participation_default = new SSZList<ParticipationFlags>();
  public static SSZList<ParticipationFlags> current_epoch_participation_default = new SSZList<ParticipationFlags>();
  public static SSZBitvector justification_bits_default = new SSZBitvector();
  public static Checkpoint previous_justified_checkpoint_default = new Checkpoint();
  public static Checkpoint current_justified_checkpoint_default = new Checkpoint();
  public static Checkpoint finalized_checkpoint_default = new Checkpoint();
  public static SSZList<uint64> inactivity_scores_default = new SSZList<uint64>();
  public static SyncCommittee current_sync_committee_default = new SyncCommittee();
  public static SyncCommittee next_sync_committee_default = new SyncCommittee();
  public uint64 genesis_time = genesis_time_default;
  public Root genesis_validators_root = genesis_validators_root_default;
  public Slot slot = slot_default;
  public Fork fork = fork_default;
  public BeaconBlockHeader latest_block_header = latest_block_header_default;
  public SSZVector<Root> block_roots = block_roots_default;
  public SSZVector<Root> state_roots = state_roots_default;
  public SSZList<Root> historical_roots = historical_roots_default;
  public Eth1Data eth1_data = eth1_data_default;
  public SSZList<Eth1Data> eth1_data_votes = eth1_data_votes_default;
  public uint64 eth1_deposit_index = eth1_deposit_index_default;
  public SSZList<Validator> validators = validators_default;
  public SSZList<Gwei> balances = balances_default;
  public SSZVector<Bytes32> randao_mixes = randao_mixes_default;
  public SSZVector<Gwei> slashings = slashings_default;
  public SSZList<ParticipationFlags> previous_epoch_participation = previous_epoch_participation_default;
  public SSZList<ParticipationFlags> current_epoch_participation = current_epoch_participation_default;
  public SSZBitvector justification_bits = justification_bits_default;
  public Checkpoint previous_justified_checkpoint = previous_justified_checkpoint_default;
  public Checkpoint current_justified_checkpoint = current_justified_checkpoint_default;
  public Checkpoint finalized_checkpoint = finalized_checkpoint_default;
  public SSZList<uint64> inactivity_scores = inactivity_scores_default;
  public SyncCommittee current_sync_committee = current_sync_committee_default;
  public SyncCommittee next_sync_committee = next_sync_committee_default;
  public BeaconState copy() { return this; }
}
