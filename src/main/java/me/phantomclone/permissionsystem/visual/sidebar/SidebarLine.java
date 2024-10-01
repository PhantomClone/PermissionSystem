package me.phantomclone.permissionsystem.visual.sidebar;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;

@AllArgsConstructor
@Getter
@Setter
@Accessors(fluent = true)
public class SidebarLine {

    private int identifier;
    private Component component;
    private int score;

}
