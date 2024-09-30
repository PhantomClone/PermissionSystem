package me.phantomclone.permissionsystem.service;

import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.entity.permission.Permission;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserPermissionRankServiceTest extends AbstractServiceIntegrationTest {

  @Test
  void testGetPermissionRankUser() {
    UUID uuid = UUID.randomUUID();

    Permission suppPerm1 =
        permissionService.createOrUpdatePermission("team.supp1", "SuppPerm1").join();
    Permission suppPerm2 =
        permissionService.createOrUpdatePermission("team.supp2", "SuppPerm2").join();
    Permission adminPerm1 =
        permissionService.createOrUpdatePermission("team.admin1", "AdminPerm1").join();
    Permission adminPerm2 =
        permissionService.createOrUpdatePermission("team.admin2", "AdminPerm").join();

    Rank suppRank = rankService.createOrUpdateRank("supp", 7, "<red>Supp", null).join();
    assertNotEquals(-1, suppRank.id());
    Rank modRank = rankService.createOrUpdateRank("mod", 8, "<red>Mod", suppRank).join();
    assertNotEquals(-1, modRank.id());
    Rank adminRank = rankService.createOrUpdateRank("admin", 10, "<red>ADMIN", modRank).join();
    assertNotEquals(-1, adminRank.id());
    Rank subAdminRank = rankService.createOrUpdateRank("subadmin", 9, "<red>SUBADMIN", null).join();
    assertNotEquals(-1, subAdminRank.id());

    assertEquals(
        1,
        rankService
            .addRankPermission(adminRank, adminPerm1)
            .join()
            .orElseThrow()
            .permissionList()
            .size());
    assertEquals(
        2,
        rankService
            .addRankPermission(adminRank, adminPerm2)
            .join()
            .orElseThrow()
            .permissionList()
            .size());
    assertEquals(
        1,
        rankService
            .addRankPermission(suppRank, suppPerm1)
            .join()
            .orElseThrow()
            .permissionList()
            .size());
    assertEquals(
        2,
        rankService
            .addRankPermission(suppRank, suppPerm2)
            .join()
            .orElseThrow()
            .permissionList()
            .size());

    assertTrue(userRankService.addUserRank(uuid, subAdminRank).join());
    assertTrue(userRankService.addUserRank(uuid, adminRank).join());

    Permission maxPermission = permissionService.createOrUpdatePermission("max.max", "Hall").join();

    assertTrue(userPermissionService.addUserPermission(uuid, maxPermission).join());

    PermissionRankUser permissionRankUser =
        userPermissionRankService.getPermissionRankUser(uuid).join();

    assertEquals(5, permissionRankUser.allPermissions().size());
    assertEquals(1, permissionRankUser.userPermissions().size());
    assertEquals(2, permissionRankUser.ranks().size());
    Optional<Rank> optionalAdminRank = rankService.getRank("admin").join();
    assertTrue(optionalAdminRank.isPresent());
    assertEquals(optionalAdminRank.get(), permissionRankUser.highestRank().orElseThrow());
  }
}
