package me.phantomclone.permissionsystem.language.command;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.command.annotation.CommandArgument;
import me.phantomclone.permissionsystem.command.annotation.CommandInfo;
import me.phantomclone.permissionsystem.command.annotation.CommandTabArgument;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.language.LanguageUserService;
import me.phantomclone.permissionsystem.language.util.MessageUtil;
import org.apache.commons.lang3.LocaleUtils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class LanguageChangeLocaleCommand {

  private static final String LANGUAGE_HELP_IDENTIFIER = "command_language_locale_help";
  private static final String LANGUAGE_CHANGED_SUCCESSFULLY_IDENTIFIER =
      "command_language_change_successfully";

  private final LanguageService languageService;
  private final LanguageUserService languageUserService;

  @CommandInfo(
      commandSyntax = {"language", "locale"},
      helpMessageIdentifier = LANGUAGE_HELP_IDENTIFIER)
  public void execute(Player player, Locale locale) {
    languageUserService.storeLanguageOnlinePlayer(player, locale);
    MessageUtil.sendMessage(
        languageService, player, LANGUAGE_CHANGED_SUCCESSFULLY_IDENTIFIER, component -> component);
  }

  @CommandArgument(
      value = "locale",
      parseErrorMessageIdentifier = "command_language_locale_wrong_locale_format")
  public Locale locale(String argument) {
    return LocaleUtils.toLocale(argument);
  }

  @CommandTabArgument(value = "locale")
  public List<String> localeTab(String argument) {
    return Stream.of(Locale.GERMANY, Locale.US)
        .map(Locale::toString)
        .filter(localeString -> localeString.startsWith(argument))
        .toList();
  }
}
