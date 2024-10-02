package me.phantomclone.permissionsystem.cache;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.entity.rank.UserRank;
import me.phantomclone.permissionsystem.event.PermissionRankUserUpdateEvent;
import me.phantomclone.permissionsystem.service.UserPermissionRankService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public class PlayerPermissionRankUserCacheListener implements Listener {

  private final UserPermissionRankService userPermissionRankService;
  private final ConcurrentMap<UUID, PermissionRankUser> cacheMap = new ConcurrentHashMap<>();

  @EventHandler
  public void onPermissionRankUserUpdateEvent(PermissionRankUserUpdateEvent event) {
    cacheMap.put(event.getPermissionRankUser().uuid(), event.getPermissionRankUser());
  }

  public CompletableFuture<PermissionRankUser> getPermissionRankUser(UUID uuid) {
    if (cacheMap.containsKey(uuid)) {
      return CompletableFuture.completedFuture(cacheMap.get(uuid));
    }

    return userPermissionRankService
        .getPermissionRankUser(uuid)
        .thenApply(userPermissions -> updateCache(uuid, userPermissions));
  }

  public List<UUID> findUUIDsWithRank(Rank rank) {
    return cacheMap.entrySet().stream()
        .filter(
            uuidPermissionRankUserEntry ->
                uuidPermissionRankUserEntry.getValue().ranks().stream()
                    .map(UserRank::rank)
                    .anyMatch(r -> r.id() == rank.id()))
        .map(Map.Entry::getKey)
        .toList();
  }

  private PermissionRankUser updateCache(UUID uuid, PermissionRankUser userPermissions) {
    cacheMap.put(uuid, userPermissions);
    return userPermissions;
  }
}
