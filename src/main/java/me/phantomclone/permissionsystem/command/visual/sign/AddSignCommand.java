package me.phantomclone.permissionsystem.command.visual.sign;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.command.annotation.CommandInfo;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.language.util.MessageUtil;
import me.phantomclone.permissionsystem.visual.sign.AddSignInteraction;
import me.phantomclone.permissionsystem.visual.sign.PermissionSignPacketAdapterListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public class AddSignCommand {

  private static final String CLICK_SIGN_IDENTIFIER = "permission_click_sign_to_add";
  private static final String CLICK_SIGN_HELP_IDENTIFIER = "permission_click_sign_help_to_add";

  private final JavaPlugin plugin;
  private final LanguageService languageService;
  private final PermissionSignPacketAdapterListener permissionSignPacketAdapterListener;

  @CommandInfo(
      commandSyntax = {"permission", "sign", "register"},
      permission = "pl.p_sign.add",
      helpMessageIdentifier = CLICK_SIGN_HELP_IDENTIFIER)
  public void execute(Player player) {
    plugin
        .getServer()
        .getPluginManager()
        .registerEvents(
            new AddSignInteraction(player, languageService, permissionSignPacketAdapterListener),
            plugin);
    MessageUtil.sendMessage(languageService, player, CLICK_SIGN_IDENTIFIER, component -> component);
  }
}
