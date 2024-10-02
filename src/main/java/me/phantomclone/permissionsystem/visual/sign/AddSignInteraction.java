package me.phantomclone.permissionsystem.visual.sign;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.language.util.MessageUtil;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

@RequiredArgsConstructor
public class AddSignInteraction implements Listener {

  private static final String ADD_SIGN_SUCCESSFULLY_IDENTIFIER = "permission_add_sign_successful";

  private final Player player;
  private final LanguageService languageService;
  private final PermissionSignPacketAdapterListener permissionSignPacketAdapterListener;

  @EventHandler
  public void onClick(PlayerInteractEvent event) {
    if (!event.getPlayer().equals(player)) {
      return;
    }
    Block block = player.getTargetBlockExact(5);

    if (block == null || !(block.getState() instanceof Sign sign)) {
      return;
    }

    MessageUtil.sendMessage(
        languageService, player, ADD_SIGN_SUCCESSFULLY_IDENTIFIER, component -> component);

    permissionSignPacketAdapterListener.setKeyForPermissionSign(block);
    permissionSignPacketAdapterListener.addSignToKnownSign(sign);
    sign.update();

    event.setCancelled(true);

    HandlerList.unregisterAll(this);
  }
}
