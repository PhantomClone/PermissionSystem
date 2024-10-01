package me.phantomclone.permissionsystem.visual.sidebar;

import static org.bukkit.scoreboard.Criteria.DUMMY;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class SidebarService {

  public void updateTitle(Player player, Component title) {
    Scoreboard scoreboard = player.getScoreboard();
    Objective objective = getObjective(scoreboard);

    objective.displayName(title);
  }

  public void updateLine(Player player, SidebarLine sidebarLine) {
    Scoreboard scoreboard = player.getScoreboard();
    Objective objective = getObjective(scoreboard);
    Team team = getTeam(scoreboard, objective, sidebarLine.identifier(), sidebarLine.score());
    team.prefix(sidebarLine.component());
  }

  public void removeLine(Player player, int identifier) {
    getObjective(player.getScoreboard()).getScore(getTeamEntryKey(identifier)).resetScore();
  }

  public SidebarLine createLine(int identifier, Component component, int score) {
    return new SidebarLine(identifier, component, score);
  }

  private Team getTeam(Scoreboard scoreboard, Objective objective, int identifier, int score) {
    String teamName = getTeamName(identifier);
    Team team = scoreboard.getTeam(teamName);

    return team != null
        ? validateScore(team, objective, identifier, score)
        : createNewTeam(scoreboard, objective, teamName, identifier, score);
  }

  private static String getTeamName(int identifier) {
    return String.format("sidebar-%d", identifier);
  }

  private Team validateScore(Team team, Objective objective, int identifier, int score) {
    objective.getScore(getTeamEntryKey(identifier)).setScore(score);

    return team;
  }

  private Team createNewTeam(
      Scoreboard scoreboard, Objective objective, String teamName, int identifier, int score) {
    Team team = scoreboard.registerNewTeam(teamName);

    String key = getTeamEntryKey(identifier);
    team.addEntry(key);
    objective.getScore(key).setScore(score);

    return team;
  }

  private static String getTeamEntryKey(int identifier) {
    return "§0".repeat(identifier + 1);
  }

  private Objective getObjective(Scoreboard scoreboard) {
    Objective sidebar = scoreboard.getObjective("sidebar");

    return sidebar != null ? sidebar : createObjective(scoreboard);
  }

  private Objective createObjective(Scoreboard scoreboard) {
    Objective objective = scoreboard.registerNewObjective("sidebar", DUMMY, Component.text().asComponent());
    objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);

    return objective;
  }
}
