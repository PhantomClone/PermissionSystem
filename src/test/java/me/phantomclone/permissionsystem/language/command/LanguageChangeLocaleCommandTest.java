package me.phantomclone.permissionsystem.language.command;

import me.phantomclone.permissionsystem.command.AbstractCommandTest;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LanguageChangeLocaleCommandTest extends AbstractCommandTest<LanguageChangeLocaleCommand> {

  @Mock Player player;

  protected LanguageChangeLocaleCommandTest() {
    super("language");
  }

  @Override
  protected LanguageChangeLocaleCommand getCommand() {
    return new LanguageChangeLocaleCommand(languageService, languageUserService);
  }

  @Test
  void testChangeLanguageSuccessfully() {
    when(languageService.getMessageComponent("command_language_change_successfully", player))
        .thenReturn(Optional.of(Component.text("changed")));
    assertTrue(
        commandExecutor.onCommand(player, pluginCommand, "language", new String[] {"de_DE"}));
    verify(languageService)
        .getMessageComponent(matches("command_language_change_successfully"), eq(player));
  }

  @Test
  void testChangeLanguageInvalidLocale() {
    when(languageService.getMessageComponent("command_language_locale_wrong_locale_format", player))
        .thenReturn(Optional.of(Component.text("invalid locale")));

    assertTrue(
        commandExecutor.onCommand(player, pluginCommand, "language", new String[] {"de_MMMM"}));
    verify(languageService)
        .getMessageComponent(matches("command_language_locale_wrong_locale_format"), eq(player));
  }

  @Test
  void testChangeLanguageHelpBecauseNoArgs() {
    when(languageService.getMessageComponent("command_language_locale_help", player))
        .thenReturn(Optional.of(Component.text("use /language <locale>")));

    assertTrue(commandExecutor.onCommand(player, pluginCommand, "language", new String[] {}));
    verify(languageService)
        .getMessageComponent(matches("command_language_locale_help"), eq(player));
  }

  @Test
  void testChangeLanguageHelpBecauseTooManyArgs() {
    when(languageService.getMessageComponent("command_language_locale_help", player))
        .thenReturn(Optional.of(Component.text("use /language <locale>")));

    assertTrue(
        commandExecutor.onCommand(player, pluginCommand, "language", new String[] {"a", "b"}));
    verify(languageService)
        .getMessageComponent(matches("command_language_locale_help"), eq(player));
  }
}
