package me.phantomclone.permissionsystem.language.listener;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.language.LanguageUserService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

@RequiredArgsConstructor
public class PlayerLoginEventListener implements Listener {

    private final LanguageUserService languageUserService;

    @EventHandler
    public void onPlayerJoinEvent(PlayerLoginEvent event) {
        languageUserService.storeLanguageOnlinePlayer(event.getPlayer(), event.getPlayer().locale());
    }

}
