package me.phantomclone.permissionsystem.entity.permission;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public record UserPermission(
    UUID uuid, Permission permission, LocalDateTime since, LocalDateTime nullableUntil) {

  public Optional<LocalDateTime> until() {
    return Optional.ofNullable(nullableUntil);
  }
}
