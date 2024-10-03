package me.phantomclone.permissionsystem.entity;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import me.phantomclone.permissionsystem.entity.permission.Permission;
import me.phantomclone.permissionsystem.entity.permission.UserPermission;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.entity.rank.UserRank;

public record PermissionRankUser(
    UUID uuid, List<UserRank> ranks, List<UserPermission> userPermissions) {

  public Optional<Rank> highestRank() {
    LocalDateTime currentTime = LocalDateTime.now();
    return ranks().stream()
        .filter(userRank -> userRank.until().filter(currentTime::isAfter).isEmpty())
        .map(UserRank::rank)
        .max(Comparator.comparingInt(Rank::priority));
  }

  public List<Permission> allPermissions() {
    return Stream.concat(
            userPermissions().stream().map(UserPermission::permission),
            ranks().stream()
                .map(UserRank::rank)
                .map(Rank::getAllPermissionList)
                .flatMap(Collection::stream))
        .toList();
  }
}
