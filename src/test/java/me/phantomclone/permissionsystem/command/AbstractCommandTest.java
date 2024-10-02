package me.phantomclone.permissionsystem.command;

import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.language.LanguageUserService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractCommandTest<T> {

  private final String commandLabel;
  protected T command;

  @Mock protected JavaPlugin javaPlugin;
  @Mock protected Logger logger;
  @Mock protected LanguageService languageService;
  @Mock protected LanguageUserService languageUserService;
  @Mock protected PluginCommand pluginCommand;

  protected CommandExecutor commandExecutor;
  protected CommandRegistry commandRegistry;

  protected AbstractCommandTest(String commandLabel) {
    this.commandLabel = commandLabel;
  }

  protected abstract T getCommand();

  @BeforeEach
  void beforeEach() {
    commandExecutor = new CommandExecutor(javaPlugin, logger, languageService);
    commandRegistry = new CommandRegistry(javaPlugin, commandExecutor);

    when(javaPlugin.getCommand(commandLabel)).thenReturn(pluginCommand);
    command = getCommand();
    assertNotNull(command);
    commandRegistry.registerCommand(command);
  }
}
