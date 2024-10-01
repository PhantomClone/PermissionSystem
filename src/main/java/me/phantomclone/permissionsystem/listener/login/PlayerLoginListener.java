package me.phantomclone.permissionsystem.listener.login;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.cache.PlayerPermissionRankUserCacheListener;
import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.inject.PermissibleBaseListener;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.service.UserPermissionRankService;
import me.phantomclone.permissionsystem.service.rank.UserRankService;
import me.phantomclone.permissionsystem.visual.sidebar.SidebarService;
import me.phantomclone.permissionsystem.visual.tablist.TabListService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class PlayerLoginListener implements Listener {

  private static final String SIDEBAR_TITLE_IDENTIFIER = "sidebar_title";
  private static final String ROW_1_IDENTIFIER = "sidebar_row_1";
  private static final String PLAYER_JOIN_IDENTIFIER = "player_join_message";

  private final JavaPlugin javaPlugin;
  private final Logger logger;
  private final PlayerPermissionRankUserCacheListener playerPermissionRankUserCacheListener;
  private final UserRankService userRankService;
  private final UserPermissionRankService userPermissionRankService;
  private final Rank defaultRank;
  private final SidebarService sidebarService;
  private final TabListService tabListService;
  private final LanguageService languageService;

  @EventHandler
  public void onLogin(AsyncPlayerPreLoginEvent event) {
    try {
      PermissionRankUser permissionRankUser =
          playerPermissionRankUserCacheListener.getPermissionRankUser(event.getUniqueId()).join();

      if (permissionRankUser.ranks().isEmpty()) {
        userRankService.addUserRank(permissionRankUser.uuid(), defaultRank).join();
        userPermissionRankService.callUpdate(permissionRankUser.uuid()).join();
      }

    } catch (Exception exception) {
      event.disallow(
          AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
          Component.text("Encounter internal server error"));
      logger.log(
          Level.SEVERE,
          "Encounter exception while loading player permissions [PreLogin].",
          exception);
    }
  }

  @EventHandler
  public void onLogin(PlayerLoginEvent event) {
    CompletableFuture<PermissionRankUser> permissionRankUserCompletableFuture =
        playerPermissionRankUserCacheListener.getPermissionRankUser(
            event.getPlayer().getUniqueId());
    if (!permissionRankUserCompletableFuture.isDone()) {
      logger.log(Level.INFO, "Player login not done.");
      event.disallow(
          PlayerLoginEvent.Result.KICK_OTHER, Component.text("Encounter internal server error"));
    }

    try {
      PermissionRankUser permissionRankUser = permissionRankUserCompletableFuture.join();
      PermissibleBaseListener.inject(javaPlugin, event.getPlayer(), permissionRankUser);
    } catch (Exception exception) {
      exception.printStackTrace();
      event.disallow(
          PlayerLoginEvent.Result.KICK_OTHER, Component.text("Encounter internal server error"));
      logger.log(
          Level.SEVERE, "Encounter exception while loading player permissions [Login].", exception);
    }
  }

  @EventHandler
  public void onPlayerJoinEvent(PlayerJoinEvent event) {
    CompletableFuture<PermissionRankUser> permissionRankUserCompletableFuture =
        playerPermissionRankUserCacheListener.getPermissionRankUser(
            event.getPlayer().getUniqueId());

    if (!permissionRankUserCompletableFuture.isDone()) {
      logger.log(
          Level.WARNING,
          "Permission data of player [%s] is not loaded.",
          event.getPlayer().getName());
    }

    PermissionRankUser permissionRankUser = permissionRankUserCompletableFuture.join();
    Rank rank = permissionRankUser.highestRank().orElseThrow();
    Component joinMessage =
        rank.prefix()
            .append(
                languageService
                    .getMessageComponent(PLAYER_JOIN_IDENTIFIER, event.getPlayer())
                    .orElse(Component.text(" {player_name} has joined the Server!"))
                    .replaceText(
                        builder ->
                            builder
                                .match("\\{player_name\\}")
                                .replacement(event.getPlayer().getName())));

    event.joinMessage(joinMessage);

    setSidebarForPlayer(event.getPlayer(), rank);
    tabListService.setPlayerTeams(event.getPlayer(), permissionRankUser);
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

  public void register() {
    javaPlugin.getServer().getPluginManager().registerEvents(this, javaPlugin);
  }
}
