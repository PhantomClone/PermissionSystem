package me.phantomclone.permissionsystem.visual.sign.command;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.command.annotation.CommandInfo;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.language.util.MessageUtil;
import me.phantomclone.permissionsystem.visual.sign.PermissionSignPacketAdapterListener;
import me.phantomclone.permissionsystem.visual.sign.RemoveSignInteraction;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public class RemoveSignCommand {

  private static final String CLICK_SIGN_IDENTIFIER = "permission_click_sign_to_remove";
  private static final String CLICK_SIGN_HELP_IDENTIFIER = "permission_click_sign_help_to_remove";

  private final JavaPlugin plugin;
  private final LanguageService languageService;
  private final PermissionSignPacketAdapterListener permissionSignPacketAdapterListener;

  @CommandInfo(
      commandSyntax = {"permission", "sign", "unregister"},
      permission = "pl.p_sign.remove",
      helpMessageIdentifier = CLICK_SIGN_HELP_IDENTIFIER)
  public void execute(Player player) {
    plugin
        .getServer()
        .getPluginManager()
        .registerEvents(
            new RemoveSignInteraction(player, languageService, permissionSignPacketAdapterListener),
            plugin);
    MessageUtil.sendMessage(languageService, player, CLICK_SIGN_IDENTIFIER, component -> component);
  }
}
