package me.phantomclone.permissionsystem.visual.sign;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

@RequiredArgsConstructor
public class AddSignInteraction implements Listener {

    private final Player player;
    private final PermissionSignPacketAdapterListener signIDK;

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (!event.getPlayer().equals(player)) {
            return;
        }
        Block block = player.getTargetBlockExact(5);

        if (block == null || !(block.getState() instanceof Sign sign)) {
            return;
        }

        player.sendMessage(Component.text("Block found."));

        signIDK.setKeyForPermissionSign(block);
        signIDK.addSignToKnownSign(sign);
        sign.update();

        HandlerList.unregisterAll(this);
    }

}
