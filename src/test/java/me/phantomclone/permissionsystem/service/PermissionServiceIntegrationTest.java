package me.phantomclone.permissionsystem.service;

import me.phantomclone.permissionsystem.entity.permission.Permission;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PermissionServiceIntegrationTest extends AbstractServiceIntegrationTest {

  @Test
  void testPersistPermission() {
    Permission maxPermission =
        permissionService.createOrUpdatePermission("max.max", "Hallo").join();

    Optional<Permission> optionalPermission = permissionService.getPermission("max.max").join();

    assertTrue(optionalPermission.isPresent());
    Permission permission = optionalPermission.get();
    assertEquals("max.max", permission.permission());
    assertEquals("Hallo", permission.descriptionString());
    assertEquals(maxPermission.id(), permission.id());
    assertNotEquals(-1, permission.id());

    Permission max2Permission =
        permissionService.createOrUpdatePermission("max.max", "Hallo2").join();
    assertEquals(maxPermission.id(), max2Permission.id());
    assertEquals("max.max", max2Permission.permission());
    assertEquals("Hallo2", max2Permission.descriptionString());

    Optional<Permission> optionalPermissionMax2 = permissionService.getPermission("max.max").join();
    assertTrue(optionalPermissionMax2.isPresent());
    Permission permissionMax2 = optionalPermissionMax2.get();
    assertEquals(maxPermission.id(), permissionMax2.id());
    assertEquals("max.max", permissionMax2.permission());
    assertEquals("Hallo2", permissionMax2.descriptionString());

    assertTrue(permissionService.deletePermission(permissionMax2.id()).join());
    assertFalse(permissionService.deletePermission(permissionMax2.id()).join());

    assertTrue(permissionService.getPermission("max.max").join().isEmpty());
  }
}
