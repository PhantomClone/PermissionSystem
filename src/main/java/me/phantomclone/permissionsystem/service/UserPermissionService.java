package me.phantomclone.permissionsystem.service;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.entity.permission.Permission;
import me.phantomclone.permissionsystem.entity.permission.UserPermission;
import me.phantomclone.permissionsystem.repository.UserPermissionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class UserPermissionService {

  private final UserPermissionRepository userPermissionRepository;

  public CompletableFuture<List<UserPermission>> getUserPermissionOf(UUID uuid) {
    return userPermissionRepository.getAllUserPermissionsOf(uuid);
  }

  public CompletableFuture<Boolean> addUserPermission(UUID uuid, Permission permission) {
    return addUserPermission(uuid, permission, null);
  }

  public CompletableFuture<Boolean> addUserPermission(
      UUID uuid, Permission permission, LocalDateTime until) {
    return userPermissionRepository.storeEntity(new UserPermission(uuid, permission, null, until));
  }

  public CompletableFuture<Boolean> removeUserPermission(UUID uuid, Permission permission) {
    return userPermissionRepository.removeEntity(uuid, permission.id());
  }
}
