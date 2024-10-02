package me.phantomclone.permissionsystem.language.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.phantomclone.permissionsystem.language.LanguageService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.UnaryOperator;

import static java.util.Locale.GERMANY;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageUtil {

  public static void sendMessage(
      LanguageService languageService,
      CommandSender commandSender,
      String messageIdentifier,
      UnaryOperator<Component> modifyComponent) {
    Component message =
        modifyComponent.apply(
            (commandSender instanceof Player player
                ? languageService
                    .getMessageComponent(messageIdentifier, player)
                    .orElseGet(
                        () ->
                            languageService
                                .getMessageComponent(messageIdentifier, GERMANY)
                                .orElseThrow())
                : languageService.getMessageComponent(messageIdentifier, GERMANY).orElseThrow()));

    commandSender.sendMessage(message);
  }
}
