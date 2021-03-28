package beacon_java.phase1.data;

import beacon_java.pylib.*;
import beacon_java.ssz.*;
import lombok.*;

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
  public static SSZVector<Root> randao_mixes_default = new SSZVector<Root>();
  public static SSZVector<Gwei> slashings_default = new SSZVector<Gwei>();
  public static SSZList<PendingAttestation> previous_epoch_attestations_default = new SSZList<PendingAttestation>();
  public static SSZList<PendingAttestation> current_epoch_attestations_default = new SSZList<PendingAttestation>();
  public static SSZBitvector justification_bits_default = new SSZBitvector();
  public static Checkpoint previous_justified_checkpoint_default = new Checkpoint();
  public static Checkpoint current_justified_checkpoint_default = new Checkpoint();
  public static Checkpoint finalized_checkpoint_default = new Checkpoint();
  public static Shard current_epoch_start_shard_default = new Shard();
  public static SSZList<ShardState> shard_states_default = new SSZList<ShardState>();
  public static SSZList<OnlineEpochs> online_countdown_default = new SSZList<OnlineEpochs>();
  public static CompactCommittee current_light_committee_default = new CompactCommittee();
  public static CompactCommittee next_light_committee_default = new CompactCommittee();
  public static SSZVector<SSZList<ValidatorIndex>> exposed_derived_secrets_default = new SSZVector<SSZList<ValidatorIndex>>();
  public static SSZList<CustodyChunkChallengeRecord> custody_chunk_challenge_records_default = new SSZList<CustodyChunkChallengeRecord>();
  public static uint64 custody_chunk_challenge_index_default = uint64.ZERO;
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
  public SSZVector<Root> randao_mixes = randao_mixes_default;
  public SSZVector<Gwei> slashings = slashings_default;
  public SSZList<PendingAttestation> previous_epoch_attestations = previous_epoch_attestations_default;
  public SSZList<PendingAttestation> current_epoch_attestations = current_epoch_attestations_default;
  public SSZBitvector justification_bits = justification_bits_default;
  public Checkpoint previous_justified_checkpoint = previous_justified_checkpoint_default;
  public Checkpoint current_justified_checkpoint = current_justified_checkpoint_default;
  public Checkpoint finalized_checkpoint = finalized_checkpoint_default;
  public Shard current_epoch_start_shard = current_epoch_start_shard_default;
  public SSZList<ShardState> shard_states = shard_states_default;
  public SSZList<OnlineEpochs> online_countdown = online_countdown_default;
  public CompactCommittee current_light_committee = current_light_committee_default;
  public CompactCommittee next_light_committee = next_light_committee_default;
  public SSZVector<SSZList<ValidatorIndex>> exposed_derived_secrets = exposed_derived_secrets_default;
  public SSZList<CustodyChunkChallengeRecord> custody_chunk_challenge_records = custody_chunk_challenge_records_default;
  public uint64 custody_chunk_challenge_index = custody_chunk_challenge_index_default;
  public BeaconState copy() { return this; }
}
