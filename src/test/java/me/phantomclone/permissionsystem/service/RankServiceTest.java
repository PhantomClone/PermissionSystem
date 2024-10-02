package me.phantomclone.permissionsystem.service;

import me.phantomclone.permissionsystem.entity.permission.Permission;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RankServiceTest extends AbstractServiceIntegrationTest {

  @Test
  void testPersistRank() {
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

    assertTrue(rankService.rankExists("admin").join());
    assertTrue(rankService.rankExists("mod").join());
    assertTrue(rankService.rankExists("supp").join());
    assertFalse(rankService.rankExists("dev").join());

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

    Rank getAdminRankByName = rankService.getRank("admin").join().orElseThrow();
    assertNotEquals(-1, getAdminRankByName.id());
    assertNotNull(getAdminRankByName.prefix());
    List<Permission> allPermissionList = getAdminRankByName.getAllPermissionList();
    assertEquals(4, allPermissionList.size());
    assertTrue(allPermissionList.contains(suppPerm1));
    assertTrue(allPermissionList.contains(suppPerm2));
    assertTrue(allPermissionList.contains(adminPerm1));
    assertTrue(allPermissionList.contains(adminPerm2));
    Rank getAdminRankById = rankService.getRank(getAdminRankByName.id()).join().orElseThrow();
    assertEquals(getAdminRankByName, getAdminRankById);

    Optional<Rank> optionalAdminBaseRank = getAdminRankById.baseRank();
    assertTrue(optionalAdminBaseRank.isPresent());

    Rank shouldBeModRank = optionalAdminBaseRank.get();
    assertEquals("mod", shouldBeModRank.name());

    Optional<Rank> optionalModBaseRank = shouldBeModRank.baseRank();
    assertTrue(optionalModBaseRank.isPresent());

    Rank shouldBeSuppRank = optionalModBaseRank.get();
    assertEquals("supp", shouldBeSuppRank.name());

    rankService.removeRankPermission(adminRank, suppPerm1).join(); // NO ERROR
    rankService.removeRankPermission(adminRank, adminPerm1).join();

    Optional<Rank> optionalAdminRank = rankService.getRank("admin").join();
    assertTrue(optionalAdminRank.isPresent());
    Rank admin = optionalAdminRank.get();
    List<Permission> allPerms = admin.getAllPermissionList();
    assertEquals(3, allPerms.size());
    assertTrue(allPerms.contains(suppPerm1));
    assertTrue(allPerms.contains(suppPerm2));
    assertFalse(allPerms.contains(adminPerm1));
    assertTrue(allPerms.contains(adminPerm2));

    assertTrue(rankService.deleteRank(modRank).join());

    Optional<Rank> optionalModRank = rankService.getRank("mod").join();
    assertTrue(optionalModRank.isEmpty());

    Optional<Rank> optionalAdminRank2 = rankService.getRank("admin").join();
    assertTrue(optionalAdminRank2.isPresent());
    Rank rank = optionalAdminRank2.get();
    assertNull(rank.nullableBaseRank());
    List<Permission> allPermissionList2 = rank.getAllPermissionList();
    assertEquals(1, allPermissionList2.size());
    assertFalse(allPermissionList2.contains(suppPerm1));
    assertFalse(allPermissionList2.contains(suppPerm2));
    assertFalse(allPermissionList2.contains(adminPerm1));
    assertTrue(allPermissionList2.contains(adminPerm2));
  }
}
