package me.phantomclone.permissionsystem.command.permission;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.cache.PlayerPermissionRankUserCacheListener;
import me.phantomclone.permissionsystem.command.annotation.CommandArgument;
import me.phantomclone.permissionsystem.command.annotation.CommandInfo;
import me.phantomclone.permissionsystem.command.annotation.CommandTabArgument;
import me.phantomclone.permissionsystem.entity.permission.Permission;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.language.util.MessageUtil;
import me.phantomclone.permissionsystem.service.UserPermissionRankService;
import me.phantomclone.permissionsystem.service.permission.PermissionService;
import me.phantomclone.permissionsystem.service.rank.RankService;
import org.bukkit.command.CommandSender;

@RequiredArgsConstructor
public class AddPermissionToRankCommand {

  private static final String HELP_IDENTIFIER = "permission_command_add_permission_to_rank_help";
  private static final String INVALID_RANK_NOT_FOUND_IDENTIFIER =
      "permission_command_add_permission_to_rank_not_found";
  private static final String COULD_NOT_CREATE_PERMISSION_IDENTIFIER =
      "permission_command_add_permission_could_not_be_created";
  private static final String COULD_NOT_ADD_IDENTIFIER =
      "permission_command_add_permission_could_not_add";
  private static final String ADDED_PERMISSION_IDENTIFIER =
      "permission_command_add_permission_added";

  private final RankService rankService;
  private final PermissionService permissionService;
  private final PlayerPermissionRankUserCacheListener playerPermissionRankUserCacheListener;
  private final UserPermissionRankService userPermissionRankService;
  private final LanguageService languageService;

  @CommandInfo(
      commandSyntax = {"rank", "name", "permission", "add", "permissionToAdd"},
      permission = "pl.permission.rank.permission.add",
      helpMessageIdentifier = HELP_IDENTIFIER)
  public void execute(CommandSender commandSender, Rank rank, Permission permission) {
    rankService
        .addRankPermission(rank, permission)
        .whenComplete(
            (optionalRank, throwable) ->
                onSuccessUpdatePlayerWithRank(commandSender, rank, throwable));
  }

  private void onSuccessUpdatePlayerWithRank(
      CommandSender commandSender, Rank rank, Throwable throwable) {
    if (throwable != null) {
      MessageUtil.sendMessage(
          languageService, commandSender, COULD_NOT_ADD_IDENTIFIER, component -> component);
      return;
    }

    MessageUtil.sendMessage(
        languageService, commandSender, ADDED_PERMISSION_IDENTIFIER, component -> component);

    playerPermissionRankUserCacheListener
        .findUUIDsWithRank(rank)
        .forEach(userPermissionRankService::callUpdate);
  }

  @CommandArgument(value = "name", parseErrorMessageIdentifier = INVALID_RANK_NOT_FOUND_IDENTIFIER)
  public CompletableFuture<Rank> parseRank(String argument) {
    return rankService
        .getRank(argument)
        .thenCompose(
            optionalRank ->
                optionalRank
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(AddPermissionToRankCommand::getFailedRankFuture));
  }

  @CommandTabArgument("name")
  public List<String> tabCompletionName(String argument) {
    return List.of("RANK_NAME");
  }

  @CommandArgument(
      value = "permissionToAdd",
      parseErrorMessageIdentifier = COULD_NOT_CREATE_PERMISSION_IDENTIFIER)
  public CompletableFuture<Permission> parsePermission(String argument) {
    return permissionService
        .getPermission(argument)
        .thenCompose(
            optionalPermission ->
                optionalPermission
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(() -> permissionService.createOrUpdatePermission(argument, "")));
  }

  @CommandTabArgument("permissionToAdd")
  public List<String> tabCompletionPermission(String argument) {
    return List.of("pl.command.heal");
  }

  private static CompletableFuture<Rank> getFailedRankFuture() {
    return CompletableFuture.failedFuture(new RuntimeException());
  }
}
