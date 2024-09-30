package me.phantomclone.permissionsystem.event;

import lombok.Getter;
import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PermissionRankUserUpdateEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    @Getter
    private final PermissionRankUser permissionRankUser;

    public PermissionRankUserUpdateEvent(PermissionRankUser permissionRankUser) {
        super(true);
        this.permissionRankUser = permissionRankUser;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

}