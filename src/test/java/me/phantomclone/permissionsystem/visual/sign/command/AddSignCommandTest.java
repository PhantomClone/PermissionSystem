package me.phantomclone.permissionsystem.visual.sign.command;

import me.phantomclone.permissionsystem.command.AbstractCommandTest;
import me.phantomclone.permissionsystem.visual.sign.AddSignInteraction;
import me.phantomclone.permissionsystem.visual.sign.PermissionSignPacketAdapterListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
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

class AddSignCommandTest extends AbstractCommandTest<AddSignCommand> {

  @Mock Player player;
  @Mock PermissionSignPacketAdapterListener permissionSignPacketAdapterListener;
  @Mock Server server;
  @Mock PluginManager pluginManager;

  protected AddSignCommandTest() {
    super("permission");
  }

  @Override
  protected AddSignCommand getCommand() {
    return new AddSignCommand(javaPlugin, languageService, permissionSignPacketAdapterListener);
  }

  @Test
  void testSuccessfullyActiveInteractionListener() {
    when(languageService.getMessageComponent("permission_click_sign_to_add", player))
        .thenReturn(Optional.of(Component.text("click a sign")));

    when(player.hasPermission("pl.p_sign.add")).thenReturn(true);
    when(javaPlugin.getServer()).thenReturn(server);
    when(server.getPluginManager()).thenReturn(pluginManager);

    assertTrue(
        commandExecutor.onCommand(
            player, pluginCommand, "permission", new String[] {"sign", "register"}));
    verify(pluginManager).registerEvents(any(AddSignInteraction.class), eq(javaPlugin));
    verify(languageService)
        .getMessageComponent(matches("permission_click_sign_to_add"), eq(player));
  }

  @Test
  void testNoPermission() {
    when(player.hasPermission("pl.p_sign.add")).thenReturn(false);

    assertTrue(
        commandExecutor.onCommand(
            player, pluginCommand, "permission", new String[] {"sign", "register"}));
    verify(languageService, never()).getMessageComponent(any(String.class), any(Player.class));
  }

  @Test
  void testTabCompilation() {
    when(player.hasPermission("pl.p_sign.add")).thenReturn(true);
    List<String> tabCompletions = commandExecutor.onTabComplete(player, pluginCommand, "permission", new String[]{"s"});

    assertNotNull(tabCompletions);
    assertEquals(1, tabCompletions.size());
    assertEquals("sign", tabCompletions.getFirst());
  }

  @Test
  void testTabCompilationWithPermission() {
    when(player.hasPermission("pl.p_sign.add")).thenReturn(false);
    List<String> tabCompletions = commandExecutor.onTabComplete(player, pluginCommand, "permission", new String[]{"s"});

    assertNotNull(tabCompletions);
    assertTrue(tabCompletions.isEmpty());
  }
}
