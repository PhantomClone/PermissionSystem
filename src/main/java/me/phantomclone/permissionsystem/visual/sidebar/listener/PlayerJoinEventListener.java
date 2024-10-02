package me.phantomclone.permissionsystem.visual.sidebar.listener;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.cache.PlayerPermissionRankUserCacheListener;
import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.visual.sidebar.SidebarService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class PlayerJoinEventListener implements Listener {

    private static final String SIDEBAR_TITLE_IDENTIFIER = "sidebar_title";
    private static final String ROW_1_IDENTIFIER = "sidebar_row_1";

    private final SidebarService sidebarService;
    private final LanguageService languageService;
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

        PermissionRankUser permissionRankUser = permissionRankUserCompletableFuture.join();
        Rank rank = permissionRankUser.highestRank().orElseThrow();

        setSidebarForPlayer(event.getPlayer(), rank);
    }


    private void setSidebarForPlayer(Player player, Rank rank) {
        sidebarService.updateTitle(
                player,
                languageService
                        .getMessageComponent(SIDEBAR_TITLE_IDENTIFIER, player)
                        .orElse(Component.text("Permission Sidebar").color(TextColor.color(255, 0, 0))));
        sidebarService.updateLine(player, sidebarService.createLine(0, Component.text(""), 0));
        sidebarService.updateLine(player, sidebarService.createLine(1, rank.prefix(), 1));
        sidebarService.updateLine(
                player,
                sidebarService.createLine(
                        2,
                        languageService
                                .getMessageComponent(ROW_1_IDENTIFIER, player)
                                .orElse(Component.text("Your Rank: ")),
                        2));
        sidebarService.updateLine(player, sidebarService.createLine(3, Component.text(""), 3));
    }

}
