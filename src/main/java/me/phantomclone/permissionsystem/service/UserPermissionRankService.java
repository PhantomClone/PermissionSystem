package me.phantomclone.permissionsystem.service;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.entity.permission.UserPermission;
import me.phantomclone.permissionsystem.entity.rank.UserRank;
import me.phantomclone.permissionsystem.service.permission.UserPermissionService;
import me.phantomclone.permissionsystem.service.rank.UserRankService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class UserPermissionRankService {

  private final UserRankService userRankService;
  private final UserPermissionService userPermissionService;

  public CompletableFuture<PermissionRankUser> getPermissionRankUser(UUID uuid) {
    CompletableFuture<List<UserPermission>> allUserPermissionsOf =
        userPermissionService.getUserPermissionOf(uuid);
    CompletableFuture<List<UserRank>> allUserRankOf = userRankService.getUserRanksOf(uuid);

    return CompletableFuture.allOf(allUserPermissionsOf, allUserRankOf)
        .thenApply(
            unused ->
                new PermissionRankUser(uuid, allUserRankOf.join(), allUserPermissionsOf.join()));
  }
}
