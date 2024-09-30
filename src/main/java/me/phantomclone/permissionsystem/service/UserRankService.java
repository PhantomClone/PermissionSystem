package me.phantomclone.permissionsystem.service;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.entity.rank.UserRank;
import me.phantomclone.permissionsystem.repository.UserRankRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class UserRankService {

  private final UserRankRepository userRankRepository;

  public CompletableFuture<List<UserRank>> getUserRanksOf(UUID uuid) {
    return userRankRepository.getAllUserRankOf(uuid);
  }

  public CompletableFuture<Boolean> addUserRank(UUID uuid, Rank rank) {
    return addUserRank(uuid, rank, null);
  }

  public CompletableFuture<Boolean> addUserRank(UUID uuid, Rank rank, LocalDateTime until) {
    return userRankRepository.storeEntity(new UserRank(uuid, rank, null, until));
  }

  public CompletableFuture<Boolean> removeUserRank(UUID uuid, Rank rank) {
    return userRankRepository.deleteUserRank(uuid, rank.id());
  }
}
