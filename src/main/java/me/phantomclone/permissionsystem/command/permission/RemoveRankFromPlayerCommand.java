package me.phantomclone.permissionsystem.command.permission;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.command.annotation.CommandArgument;
import me.phantomclone.permissionsystem.command.annotation.CommandInfo;
import me.phantomclone.permissionsystem.command.annotation.CommandTabArgument;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.language.util.MessageUtil;
import me.phantomclone.permissionsystem.service.UserPermissionRankService;
import me.phantomclone.permissionsystem.service.rank.RankService;
import me.phantomclone.permissionsystem.service.rank.UserRankService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public class RemoveRankFromPlayerCommand {

  private static final String HELP_IDENTIFIER = "permission_command_remove_rank_from_player_help";
  private static final String COULD_NOT_REMOVE_IDENTIFIER =
      "permission_command_remove_rank_from_player_could_not_remove";
  private static final String COULD_NOT_FIND_UUID_IDENTIFIER =
      "permission_command_remove_rank_from_player_could_not_find_uuid";
  private static final String REMOVED_PERMISSION_IDENTIFIER =
      "permission_command_remove_rank_from_player_removed";
  private static final String COULD_NOT_FIND_RANK_IDENTIFIER =
      "permission_command_remove_rank_from_player_could_not_be_found";

  private final JavaPlugin plugin;
  private final RankService rankService;
  private final UserRankService userRankService;
  private final UserPermissionRankService userPermissionRankService;
  private final LanguageService languageService;

  @CommandInfo(
      commandSyntax = {"rank", "rankName", "remove", "user", "playerUuid"},
      permission = "pl.permission.rank.remove",
      helpMessageIdentifier = HELP_IDENTIFIER)
  public void execute(CommandSender commandSender, Rank rank, UUID playerUuid) {
    userRankService
        .removeUserRank(playerUuid, rank)
        .whenComplete(
            (result, throwable) ->
                onSuccessUpdatePlayerWithRank(commandSender, playerUuid, throwable, result));
  }

  private void onSuccessUpdatePlayerWithRank(
      CommandSender commandSender, UUID playerUuid, Throwable throwable, boolean result) {
    if (throwable != null || !result) {
      MessageUtil.sendMessage(
          languageService, commandSender, COULD_NOT_REMOVE_IDENTIFIER, component -> component);
      return;
    }

    MessageUtil.sendMessage(
        languageService, commandSender, REMOVED_PERMISSION_IDENTIFIER, component -> component);

    plugin.getServer().getOnlinePlayers().stream()
        .map(Entity::getUniqueId)
        .filter(uuid -> uuid.equals(playerUuid))
        .findFirst()
        .ifPresent(userPermissionRankService::callUpdate);
  }

  @CommandArgument(
      value = "playerUuid",
      parseErrorMessageIdentifier = COULD_NOT_FIND_UUID_IDENTIFIER)
  public CompletableFuture<UUID> parseUUID(String argument) {
    return plugin.getServer().getOnlinePlayers().stream()
        .filter(player -> player.getName().equalsIgnoreCase(argument))
        .findFirst()
        .map(Entity::getUniqueId)
        .map(CompletableFuture::completedFuture)
        .orElseGet(() -> fetchUUID(argument));
  }

  @CommandTabArgument("playerUuid")
  public List<String> tabCompletionPlayerUuid(String argument) {
    return plugin.getServer().getOnlinePlayers().stream()
        .map(Player::getName)
        .filter(playerName -> playerName.startsWith(argument))
        .toList();
  }

  private CompletableFuture<UUID> fetchUUID(String playerName) {
    CompletableFuture<UUID> completableFuture = new CompletableFuture<>();

    plugin
        .getServer()
        .getAsyncScheduler()
        .runNow(
            plugin,
            task ->
                completableFuture.complete(
                    plugin.getServer().getOfflinePlayer(playerName).getUniqueId()));

    return completableFuture;
  }

  @CommandArgument(value = "rankName", parseErrorMessageIdentifier = COULD_NOT_FIND_RANK_IDENTIFIER)
  public CompletableFuture<Rank> parseRank(String argument) {
    return rankService
        .getRank(argument)
        .thenCompose(
            optionalRank ->
                optionalRank
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(RemoveRankFromPlayerCommand::getFailedRankFuture));
  }

  @CommandTabArgument("rankName")
  public List<String> tabCompletionPermission(String argument) {
    return List.of("RANK_NAME");
  }

  private static CompletableFuture<Rank> getFailedRankFuture() {
    return CompletableFuture.failedFuture(new RuntimeException());
  }
}
