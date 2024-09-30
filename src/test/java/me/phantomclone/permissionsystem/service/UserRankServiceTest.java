package me.phantomclone.permissionsystem.service;

import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.entity.rank.UserRank;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class UserRankServiceTest extends AbstractServiceIntegrationTest {

  @Test
  void testPersistUserRank() {
    UUID uuid = UUID.randomUUID();

    assertTrue(userRankService.getUserRanksOf(uuid).join().isEmpty());

    Rank adminRank = rankService.createOrUpdateRank("admin", 10, "<red>ADMIN", null).join();
    assertNotEquals(-1, adminRank.id());

    assertTrue(userRankService.addUserRank(uuid, adminRank).join());

    List<UserRank> userRanks = userRankService.getUserRanksOf(uuid).join();
    assertEquals(1, userRanks.size());

    assertEquals(adminRank, userRanks.get(0).rank());
    assertEquals(uuid, userRanks.get(0).uuid());
    assertNotNull(userRanks.get(0).since());
    assertTrue(userRanks.get(0).until().isEmpty());

    assertThrows(
        CompletionException.class,
        () ->
            userRankService
                .addUserRank(uuid, adminRank)
                .whenComplete((aBoolean, throwable) -> assertNotNull(throwable))
                .join());

    Rank modRank = rankService.createOrUpdateRank("mod", 8, "<red>Mod", null).join();
    assertNotEquals(-1, modRank.id());

    LocalDateTime until = LocalDateTime.of(2024, 10, 1, 22, 0);
    assertTrue(userRankService.addUserRank(uuid, modRank, until).join());

    List<UserRank> userRanks2 = userRankService.getUserRanksOf(uuid).join();
    assertEquals(2, userRanks2.size());

    assertTrue(userRanks2.stream().anyMatch(userRank -> userRank.rank().id() == modRank.id()));
    assertTrue(userRanks2.stream().anyMatch(userRank -> userRank.rank().id() == adminRank.id()));

    UserRank modUserRank =
        userRanks2.stream()
            .filter(userRank -> userRank.rank().id() == modRank.id())
            .findFirst()
            .orElseThrow();
    assertEquals(modRank, modUserRank.rank());
    assertEquals(uuid, modUserRank.uuid());
    assertNotNull(modUserRank.since());
    assertTrue(modUserRank.until().isPresent());
    assertEquals(until, modUserRank.until().get());

    assertTrue(userRankService.removeUserRank(uuid, modRank).join());

    List<UserRank> userRanks3 = userRankService.getUserRanksOf(uuid).join();
    assertEquals(1, userRanks3.size());

    assertEquals(adminRank, userRanks3.get(0).rank());
    assertEquals(uuid, userRanks3.get(0).uuid());
    assertNotNull(userRanks3.get(0).since());
    assertTrue(userRanks3.get(0).until().isEmpty());

    assertFalse(userRankService.removeUserRank(uuid, modRank).join());
  }
}
