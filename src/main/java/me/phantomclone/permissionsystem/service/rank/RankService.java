package me.phantomclone.permissionsystem.service.rank;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.entity.permission.Permission;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.repository.rank.RankRepository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class RankService {

  private final RankRepository rankRepository;

  public CompletableFuture<Rank> createOrUpdateRank(
      String name, int priority, String prefixString, Rank nullableBaseRank) {
    return rankRepository.createOrUpdateRank(
        new Rank(-1, name, priority, prefixString, null, nullableBaseRank));
  }

  public CompletableFuture<Boolean> rankExists(String name) {
    return rankRepository.rankExists(name);
  }
  public CompletableFuture<Optional<Rank>> getRank(String name) {
    return rankRepository.getRank(name);
  }

  public CompletableFuture<Optional<Rank>> getRank(long rankId) {
    return rankRepository.getRank(rankId);
  }

  public CompletableFuture<Optional<Rank>> addRankPermission(Rank rank, Permission permission) {
    return rankRepository.addRankPermission(rank.id(), permission);
  }

  public CompletableFuture<Optional<Rank>> removeRankPermission(Rank rank, Permission permission) {
    return rankRepository.removeRankPermission(rank.id(), permission);
  }

  public CompletableFuture<Boolean> deleteRank(Rank rank) {
    return rankRepository.deleteRank(rank.id());
  }
}
