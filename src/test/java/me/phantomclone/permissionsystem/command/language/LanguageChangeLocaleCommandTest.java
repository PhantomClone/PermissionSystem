package me.phantomclone.permissionsystem.command.language;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import me.phantomclone.permissionsystem.command.AbstractCommandTest;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

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

  @Test
  void testTabCompletionOnCreate() {
    List<String> tabCompletion =
            commandExecutor.onTabComplete(player, pluginCommand, "language", new String[] {"de"});

    assertNotNull(tabCompletion);
    assertEquals(1, tabCompletion.size());
    assertEquals("de_DE", tabCompletion.getFirst());
  }

}
