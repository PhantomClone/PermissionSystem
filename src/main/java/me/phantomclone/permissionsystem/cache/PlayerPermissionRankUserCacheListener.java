package me.phantomclone.permissionsystem.cache;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.event.PermissionRankUserUpdateEvent;
import me.phantomclone.permissionsystem.service.UserPermissionRankService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

  private PermissionRankUser updateCache(UUID uuid, PermissionRankUser userPermissions) {
    cacheMap.put(uuid, userPermissions);
    return userPermissions;
  }
}
