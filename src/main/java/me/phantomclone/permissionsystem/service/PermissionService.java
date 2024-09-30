package me.phantomclone.permissionsystem.service;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.entity.permission.Permission;
import me.phantomclone.permissionsystem.repository.PermissionRepository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class PermissionService {

  private final PermissionRepository permissionRepository;

  public CompletableFuture<Permission> createOrUpdatePermission(
      String permission, String descriptionAsMiniString) {
    return permissionRepository.storeEntity(
        new Permission(-1, permission, descriptionAsMiniString));
  }

  public CompletableFuture<Optional<Permission>> getPermission(String permission) {
    return permissionRepository.getEntityBy(permission);
  }

  public CompletableFuture<Boolean> deletePermission(long permissionId) {
    return permissionRepository.removeEntity(permissionId);
  }
}
