package me.phantomclone.permissionsystem.visual.sign.command;

import me.phantomclone.permissionsystem.command.AbstractCommandTest;
import me.phantomclone.permissionsystem.visual.sign.PermissionSignPacketAdapterListener;
import me.phantomclone.permissionsystem.visual.sign.RemoveSignInteraction;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoveSignCommandTest extends AbstractCommandTest<RemoveSignCommand> {

  @Mock Player player;
  @Mock PermissionSignPacketAdapterListener permissionSignPacketAdapterListener;
  @Mock PluginManager pluginManager;

  protected RemoveSignCommandTest() {
    super("permission");
  }

  @Override
  protected RemoveSignCommand getCommand() {
    return new RemoveSignCommand(javaPlugin, languageService, permissionSignPacketAdapterListener);
  }

  @Test
  void testSuccessfullyActiveInteractionListener() {
    when(languageService.getMessageComponent("permission_click_sign_to_remove", player))
        .thenReturn(Optional.of(Component.text("click a sign")));

    when(player.hasPermission("pl.p_sign.remove")).thenReturn(true);
    when(javaPlugin.getServer()).thenReturn(server);
    when(server.getPluginManager()).thenReturn(pluginManager);

    assertTrue(
        commandExecutor.onCommand(
            player, pluginCommand, "permission", new String[] {"sign", "unregister"}));
    verify(pluginManager).registerEvents(any(RemoveSignInteraction.class), eq(javaPlugin));
    verify(languageService)
        .getMessageComponent(matches("permission_click_sign_to_remove"), eq(player));
  }

  @Test
  void testNoPermission() {
    when(player.hasPermission("pl.p_sign.remove")).thenReturn(false);

    assertTrue(
        commandExecutor.onCommand(
            player, pluginCommand, "permission", new String[] {"sign", "unregister"}));
    verify(languageService, never()).getMessageComponent(any(String.class), any(Player.class));
  }

  @Test
  void testTabCompilation() {
    when(player.hasPermission("pl.p_sign.remove")).thenReturn(true);
    List<String> tabCompletions =
        commandExecutor.onTabComplete(player, pluginCommand, "permission", new String[] {"s"});

    assertNotNull(tabCompletions);
    assertEquals(1, tabCompletions.size());
    assertEquals("sign", tabCompletions.getFirst());
  }

  @Test
  void testTabCompilationWithPermission() {
    when(player.hasPermission("pl.p_sign.remove")).thenReturn(false);
    List<String> tabCompletions =
        commandExecutor.onTabComplete(player, pluginCommand, "permission", new String[] {"s"});

    assertNotNull(tabCompletions);
    assertTrue(tabCompletions.isEmpty());
  }
}
