package me.phantomclone.permissionsystem.command.permission;

import me.phantomclone.permissionsystem.command.AbstractCommandTest;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.service.rank.RankService;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateRankCommandTest extends AbstractCommandTest<CreateRankCommand> {

  @Mock RankService rankService;
  @Mock CommandSender commandSender;

  protected CreateRankCommandTest() {
    super("rank");
  }

  @Override
  protected CreateRankCommand getCommand() {
    return new CreateRankCommand(rankService, languageService);
  }

  @Test
  void createRankSuccessfully() {
    Rank defaultRank = new Rank(1, "default", 1, "default", List.of(), null);
    Rank admin = new Rank(2, "admin", 1, "<red>ADMIN", List.of(), defaultRank);

    when(commandSender.hasPermission("pl.permission.rank.create")).thenReturn(true);
    when(rankService.getRank("default"))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(defaultRank)));
    when(rankService.createOrUpdateRank("admin", 1, "<red>ADMIN", defaultRank))
        .thenReturn(CompletableFuture.completedFuture(admin));
    when(rankService.rankExists("admin")).thenReturn(CompletableFuture.completedFuture(false));

    setUpTimer();

    assertTrue(
        commandExecutor.onCommand(
            commandSender,
            pluginCommand,
            "rank",
            new String[] {"create", "admin", "1", "<red>ADMIN", "default"}));
    verifyTimer();

    verify(rankService).getRank("default");
    verify(rankService).createOrUpdateRank("admin", 1, "<red>ADMIN", defaultRank);

    verify(languageService)
        .getMessageComponent("permission_command_create_rank_successfully", Locale.GERMANY);
  }

  @Test
  void createRankSuccessfullyWithNoBaseRank() {
    Rank admin = new Rank(2, "admin", 1, "<red>ADMIN", List.of(), null);

    when(commandSender.hasPermission("pl.permission.rank.create")).thenReturn(true);
    when(rankService.createOrUpdateRank("admin", 1, "<red>ADMIN", null))
        .thenReturn(CompletableFuture.completedFuture(admin));
    when(rankService.rankExists("admin")).thenReturn(CompletableFuture.completedFuture(false));

    setUpTimer();

    assertTrue(
        commandExecutor.onCommand(
            commandSender,
            pluginCommand,
            "rank",
            new String[] {"create", "admin", "1", "<red>ADMIN"}));
    verifyTimer();

    verify(rankService, never()).getRank(anyString());
    verify(rankService).createOrUpdateRank("admin", 1, "<red>ADMIN", null);

    verify(languageService)
        .getMessageComponent("permission_command_create_rank_successfully", Locale.GERMANY);
  }

  @Test
  void createRankNotSuccessfully() {
    Rank defaultRank = new Rank(1, "default", 1, "default", List.of(), null);

    when(commandSender.hasPermission("pl.permission.rank.create")).thenReturn(true);
    when(rankService.getRank("default"))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(defaultRank)));
    when(rankService.createOrUpdateRank("admin", 1, "<red>ADMIN", defaultRank))
        .thenReturn(CompletableFuture.failedFuture(new SQLException()));
    when(rankService.rankExists("admin")).thenReturn(CompletableFuture.completedFuture(false));

    setUpTimer();

    assertTrue(
        commandExecutor.onCommand(
            commandSender,
            pluginCommand,
            "rank",
            new String[] {"create", "admin", "1", "<red>ADMIN", "default"}));
    verifyTimer();

    verify(rankService).getRank("default");
    verify(rankService).createOrUpdateRank("admin", 1, "<red>ADMIN", defaultRank);
    verify(languageService)
        .getMessageComponent("permission_command_create_rank_not_successfully", Locale.GERMANY);
  }

  @Test
  void createRankNotSuccessfullyNameTaken() {
    Rank defaultRank = new Rank(1, "default", 1, "default", List.of(), null);

    when(commandSender.hasPermission("pl.permission.rank.create")).thenReturn(true);
    when(rankService.getRank("default"))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(defaultRank)));
    when(rankService.rankExists("admin")).thenReturn(CompletableFuture.completedFuture(true));

    setUpTimer();

    assertTrue(
        commandExecutor.onCommand(
            commandSender,
            pluginCommand,
            "rank",
            new String[] {"create", "admin", "1", "<red>ADMIN", "default"}));
    verifyTimer();

    verify(rankService).getRank("default");
    verify(rankService, never())
        .createOrUpdateRank(anyString(), anyInt(), anyString(), any(Rank.class));

    verify(languageService)
        .getMessageComponent("permission_command_create_rank_name_taken", Locale.GERMANY);
  }

  @Test
  void createRankNotSuccessfullyBaseRankNotFound() {
    when(commandSender.hasPermission("pl.permission.rank.create")).thenReturn(true);
    when(rankService.getRank("default"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
    when(rankService.rankExists("admin")).thenReturn(CompletableFuture.completedFuture(false));

    setUpTimer();

    assertTrue(
        commandExecutor.onCommand(
            commandSender,
            pluginCommand,
            "rank",
            new String[] {"create", "admin", "1", "<red>ADMIN", "default"}));
    verifyTimer();

    verify(rankService).getRank("default");
    verify(rankService, never())
        .createOrUpdateRank(anyString(), anyInt(), anyString(), any(Rank.class));

    verify(languageService)
        .getMessageComponent("permission_command_create_rank_name_taken", Locale.GERMANY);
  }

  @Test
  void testTabCompletionOnCreate() {
    when(commandSender.hasPermission("pl.permission.rank.create")).thenReturn(true);

    List<String> tabCompletion =
        commandExecutor.onTabComplete(commandSender, pluginCommand, "rank", new String[] {"creat"});

    assertNotNull(tabCompletion);
    assertEquals(1, tabCompletion.size());
    assertEquals("create", tabCompletion.getFirst());
  }

  @Test
  void testTabCompletionOnCreateWithNoPermission() {
    when(commandSender.hasPermission("pl.permission.rank.create")).thenReturn(false);

    List<String> tabCompletion =
        commandExecutor.onTabComplete(commandSender, pluginCommand, "rank", new String[] {"creat"});

    assertNotNull(tabCompletion);
    assertTrue(tabCompletion.isEmpty());
  }

  @Test
  void testTabCompletionOnRankName() {
    when(commandSender.hasPermission("pl.permission.rank.create")).thenReturn(true);

    List<String> tabCompletion =
        commandExecutor.onTabComplete(
            commandSender, pluginCommand, "rank", new String[] {"create", "ANYTHING"});

    assertNotNull(tabCompletion);
    assertEquals(1, tabCompletion.size());
    assertEquals("RANK_NAME", tabCompletion.getFirst());
  }

  @Test
  void testTabCompletionOnPriority() {
    when(commandSender.hasPermission("pl.permission.rank.create")).thenReturn(true);

    List<String> tabCompletion =
        commandExecutor.onTabComplete(
            commandSender, pluginCommand, "rank", new String[] {"create", "ANYTHING", "99"});

    assertNotNull(tabCompletion);
    assertEquals(3, tabCompletion.size());
    assertTrue(tabCompletion.contains("1"));
    assertTrue(tabCompletion.contains("2"));
    assertTrue(tabCompletion.contains("10"));
  }

  @Test
  void testTabCompletionOnPrefix() {
    when(commandSender.hasPermission("pl.permission.rank.create")).thenReturn(true);

    List<String> tabCompletion =
        commandExecutor.onTabComplete(
            commandSender,
            pluginCommand,
            "rank",
            new String[] {"create", "ANYTHING", "99", "anyprefix"});

    assertNotNull(tabCompletion);
    assertEquals(1, tabCompletion.size());
    assertEquals("<red>ADMIN", tabCompletion.getFirst());
  }

  @Test
  void testTabCompletionOnBaseRank() {
    when(commandSender.hasPermission("pl.permission.rank.create")).thenReturn(true);

    List<String> tabCompletion =
        commandExecutor.onTabComplete(
            commandSender,
            pluginCommand,
            "rank",
            new String[] {"create", "ANYTHING", "99", "anyprefix", "MAXMUSTER"});

    assertNotNull(tabCompletion);
    assertEquals(2, tabCompletion.size());
    assertTrue(tabCompletion.contains("BaseRank"));
    assertTrue(tabCompletion.contains("empty"));
  }
}
