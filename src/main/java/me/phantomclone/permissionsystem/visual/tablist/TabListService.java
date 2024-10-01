package me.phantomclone.permissionsystem.visual.tablist;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Optional;

@RequiredArgsConstructor
public class TabListService {

    private final Rank defaultRank;

    public void setPlayerTeams(Player player, PermissionRankUser permissionRankUser) {
        Scoreboard scoreboard = player.getScoreboard();
        Rank rank = permissionRankUser.highestRank().orElse(defaultRank);

        Team team = getOrCreateTeam(scoreboard, "%d".formatted(1000 - rank.priority()));

        team.prefix(rank.prefix().append(Component.text(" | ")));
        team.color(NamedTextColor.GRAY);

        team.addEntry(player.getName());
    }
    private Team getOrCreateTeam(Scoreboard scoreboard, String teamName) {
        return Optional.ofNullable(scoreboard.getTeam(teamName))
                .orElseGet(() -> scoreboard.registerNewTeam(teamName));
    }

}
