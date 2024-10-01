package me.phantomclone.permissionsystem.listener.chat;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.cache.PlayerPermissionRankUserCacheListener;
import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class AsyncChatEventListener implements Listener, ChatRenderer {

    private final PlayerPermissionRankUserCacheListener playerPermissionRankUserCacheListener;

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        event.renderer(this);
    }

    @Override
    public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer) {
        PermissionRankUser permissionRankUser = playerPermissionRankUserCacheListener.getPermissionRankUser(source.getUniqueId()).join();

        return permissionRankUser.highestRank().map(Rank::prefix)
                .orElse(Component.text().color(TextColor.color(255, 255, 255)).asComponent())
                .append(Component.text(" | "))
                .append(sourceDisplayName)
                .append(Component.text(": ").color(TextColor.color(255, 255, 255)))
                .append(message.color(TextColor.color(255, 255, 255)));
    }
}
