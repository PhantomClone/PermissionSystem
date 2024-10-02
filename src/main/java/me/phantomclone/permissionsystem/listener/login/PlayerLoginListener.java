package me.phantomclone.permissionsystem.listener.login;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.cache.PlayerPermissionRankUserCacheListener;
import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.inject.PermissibleBaseListener;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.service.UserPermissionRankService;
import me.phantomclone.permissionsystem.service.rank.UserRankService;
import net.kyori.adventure.text.Component;
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

  private static final String PLAYER_JOIN_IDENTIFIER = "player_join_message";

  private final JavaPlugin javaPlugin;
  private final Logger logger;
  private final PlayerPermissionRankUserCacheListener playerPermissionRankUserCacheListener;
  private final UserRankService userRankService;
  private final UserPermissionRankService userPermissionRankService;
  private final Rank defaultRank;
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
          "Permission data of player [{}] is not loaded.",
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
  }

  public void register() {
    javaPlugin.getServer().getPluginManager().registerEvents(this, javaPlugin);
  }
}
