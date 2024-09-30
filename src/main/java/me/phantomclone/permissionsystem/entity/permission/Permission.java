package me.phantomclone.permissionsystem.entity.permission;

import net.kyori.adventure.text.Component;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public record Permission(long id, String permission, String descriptionString) {

  public Component description() {
    return miniMessage().deserialize(descriptionString);
  }
}
