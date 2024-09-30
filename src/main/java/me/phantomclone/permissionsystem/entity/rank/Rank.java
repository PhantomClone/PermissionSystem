package me.phantomclone.permissionsystem.entity.rank;

import me.phantomclone.permissionsystem.entity.permission.Permission;
import net.kyori.adventure.text.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public record Rank(
    long id,
    String name,
    int priority,
    String prefixString,
    List<Permission> permissionList,
    Rank nullableBaseRank) {

  public Component prefix() {
    return miniMessage().deserialize(prefixString);
  }

  public Optional<Rank> baseRank() {
    return Optional.ofNullable(nullableBaseRank);
  }

  public List<Permission> getAllPermissionList() {
    return Stream.concat(
            permissionList.stream(),
            baseRank().stream().map(Rank::getAllPermissionList).flatMap(Collection::stream))
        .toList();
  }
}
