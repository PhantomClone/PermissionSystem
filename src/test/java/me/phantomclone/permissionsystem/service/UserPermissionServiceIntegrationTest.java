package me.phantomclone.permissionsystem.service;

import me.phantomclone.permissionsystem.entity.permission.Permission;
import me.phantomclone.permissionsystem.entity.permission.UserPermission;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserPermissionServiceIntegrationTest extends AbstractServiceIntegrationTest {

  @Test
  void testPersistUserPermission() {
    UUID uuid = UUID.randomUUID();
    assertTrue(userPermissionService.getUserPermissionOf(uuid).join().isEmpty());

    Permission maxPermission = permissionService.createOrUpdatePermission("max.max", "Hall").join();

    assertTrue(userPermissionService.addUserPermission(uuid, maxPermission).join());

    List<UserPermission> userPermissions = userPermissionService.getUserPermissionOf(uuid).join();

    assertEquals(1, userPermissions.size());
    UserPermission actualPermission = userPermissions.get(0);
    assertEquals(maxPermission, actualPermission.permission());
    assertNotNull(actualPermission.since());
    assertNull(actualPermission.nullableUntil());
    assertEquals(uuid, actualPermission.uuid());

    Permission max2Permission =
        permissionService.createOrUpdatePermission("max.max2", "Halloo").join();

    LocalDateTime until = LocalDateTime.of(2024, 10, 10, 22, 0);
    assertTrue(userPermissionService.addUserPermission(uuid, max2Permission, until).join());

    List<UserPermission> userPermissions2 = userPermissionService.getUserPermissionOf(uuid).join();

    assertEquals(2, userPermissions2.size());

    UserPermission actualPermission1 =
        userPermissions2.stream()
            .filter(userPermission -> userPermission.permission().permission().equals("max.max"))
            .findFirst()
            .orElseThrow();

    assertEquals(maxPermission, actualPermission1.permission());
    assertNotNull(actualPermission1.since());
    assertNull(actualPermission1.nullableUntil());
    assertEquals(uuid, actualPermission1.uuid());

    UserPermission actualPermission2 =
        userPermissions2.stream()
            .filter(userPermission -> userPermission.permission().permission().equals("max.max2"))
            .findFirst()
            .orElseThrow();

    assertEquals(max2Permission, actualPermission2.permission());
    assertNotNull(actualPermission2.since());
    assertEquals(until, actualPermission2.nullableUntil());
    assertEquals(uuid, actualPermission2.uuid());

    assertTrue(userPermissionService.removeUserPermission(uuid, max2Permission).join());
    assertFalse(userPermissionService.removeUserPermission(uuid, max2Permission).join());

    List<UserPermission> allPermsAfterDel = userPermissionService.getUserPermissionOf(uuid).join();
    assertEquals(1, allPermsAfterDel.size());
    UserPermission actualAllPermission = allPermsAfterDel.get(0);
    assertEquals(maxPermission, actualAllPermission.permission());
    assertNotNull(actualAllPermission.since());
    assertNull(actualAllPermission.nullableUntil());
    assertEquals(uuid, actualAllPermission.uuid());
  }
}
