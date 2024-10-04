package me.phantomclone.permissionsystem.command.permission;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.command.annotation.CommandArgument;
import me.phantomclone.permissionsystem.command.annotation.CommandInfo;
import me.phantomclone.permissionsystem.command.annotation.CommandTabArgument;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.language.util.MessageUtil;
import me.phantomclone.permissionsystem.service.rank.RankService;
import org.bukkit.command.CommandSender;

@RequiredArgsConstructor
public class CreateRankCommand {

  private static final String HELP_IDENTIFIER = "permission_command_create_rank_help";
  private static final String INVALID_NAME_IDENTIFIER = "permission_command_create_rank_name_taken";
  private static final String INVALID_PRIORITY_IDENTIFIER =
      "permission_command_create_rank_priority_nan";
  private static final String RANK_CREATED_SUCCESSFULLY =
      "permission_command_create_rank_successfully";
  private static final String RANK_CREATED_NOT_SUCCESSFULLY =
      "permission_command_create_rank_not_successfully";

  private final RankService rankService;
  private final LanguageService languageService;

  @CommandInfo(
      commandSyntax = {"rank", "create", "name", "priority", "prefix", "?baserank"},
      permission = "pl.permission.rank.create",
      helpMessageIdentifier = HELP_IDENTIFIER)
  public void execute(
      CommandSender commandSender, String name, int priority, String prefix, Rank nullableRank) {
    rankService
        .createOrUpdateRank(name, priority, prefix, nullableRank)
        .whenComplete(
            (rank, throwable) ->
                MessageUtil.sendMessage(
                    languageService,
                    commandSender,
                    throwable == null ? RANK_CREATED_SUCCESSFULLY : RANK_CREATED_NOT_SUCCESSFULLY,
                    component -> component));
  }

  @CommandArgument(value = "name", parseErrorMessageIdentifier = INVALID_NAME_IDENTIFIER)
  public CompletableFuture<String> checkRankName(String argument) {
    return rankService
        .rankExists(argument)
        .thenCompose(
            rankExists ->
                rankExists == null || rankExists ? getFailedStringFuture() : CompletableFuture.completedFuture(argument));
  }

  @CommandTabArgument("name")
  public List<String> nameTabCompletion(String argument) {
    return List.of("RANK_NAME");
  }

  @CommandArgument(value = "priority", parseErrorMessageIdentifier = INVALID_PRIORITY_IDENTIFIER)
  public int parsePriority(String argument) {
    return Integer.parseInt(argument);
  }

  @CommandTabArgument("priority")
  public List<String> priorityTabCompletion(String argument) {
    return List.of("1", "2", "10");
  }

  @CommandArgument(value = "prefix", parseErrorMessageIdentifier = HELP_IDENTIFIER)
  public String parsePrefix(String argument) {
    return argument;
  }

  @CommandTabArgument("prefix")
  public List<String> prefixTabCompletion(String argument) {
    return List.of("<red>ADMIN");
  }

  @CommandArgument(value = "?baserank", parseErrorMessageIdentifier = INVALID_NAME_IDENTIFIER)
  public CompletableFuture<Rank> lookUpForBaseRank(String argument) {
    return rankService
        .getRank(argument)
        .thenCompose(
            optionalRank ->
                optionalRank
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(CreateRankCommand::getFailedRankFuture));
  }

  @CommandTabArgument("?baserank")
  public List<String> baseRankTabCompletion(String argument) {
    return List.of("BaseRank", "empty");
  }

  private static CompletableFuture<String> getFailedStringFuture() {
    return CompletableFuture.failedFuture(new RuntimeException());
  }

  private static CompletableFuture<Rank> getFailedRankFuture() {
    return CompletableFuture.failedFuture(new RuntimeException());
  }
}
