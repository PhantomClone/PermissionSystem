package me.phantomclone.permissionsystem.entity.rank;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public record UserRank(UUID uuid, Rank rank, LocalDateTime since, LocalDateTime nullableUntil) {

  public Optional<LocalDateTime> until() {
    return Optional.ofNullable(nullableUntil);
  }
}
