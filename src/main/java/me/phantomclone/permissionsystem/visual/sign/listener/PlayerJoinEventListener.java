package me.phantomclone.permissionsystem.visual.sign.listener;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.cache.PlayerPermissionRankUserCacheListener;
import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.visual.tablist.TabListService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public class PlayerJoinEventListener implements Listener {

    
    private final TabListService tabListService;
    private final PlayerPermissionRankUserCacheListener playerPermissionRankUserCacheListener;
    private final Logger logger;
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        CompletableFuture<PermissionRankUser> permissionRankUserCompletableFuture =
                playerPermissionRankUserCacheListener.getPermissionRankUser(
                        event.getPlayer().getUniqueId());

        if (!permissionRankUserCompletableFuture.isDone()) {
            logger.log(
                    Level.WARNING,
                    "Permission data of player [{}] is not loaded.",
                    event.getPlayer().getName());
        }

        tabListService.setPlayerTeams(event.getPlayer(), permissionRankUserCompletableFuture.join());
    }

    
}
