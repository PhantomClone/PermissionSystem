package me.phantomclone.permissionsystem.language;

import me.phantomclone.permissionsystem.command.CommandRegistry;
import me.phantomclone.permissionsystem.language.command.LanguageChangeLocaleCommand;
import me.phantomclone.permissionsystem.language.listener.PlayerLoginEventListener;
import org.apache.commons.lang3.LocaleUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Optional;

import static java.util.Locale.GERMANY;

public class LanguageUserService {

  private final NamespacedKey namespacedKey;

  public LanguageUserService(JavaPlugin javaPlugin) {
    this.namespacedKey = NamespacedKey.fromString("language_identifier", javaPlugin);

  }

  public void registerListener(JavaPlugin javaPlugin, CommandRegistry commandRegistry, LanguageService languageService) {
    javaPlugin.getServer().getPluginManager().registerEvents(new PlayerLoginEventListener(this), javaPlugin);
    commandRegistry.registerCommand(new LanguageChangeLocaleCommand(languageService, this));
  }

  public void storeLanguageOnlinePlayer(Player player, Locale locale) {
    player
        .getPersistentDataContainer()
        .set(namespacedKey, PersistentDataType.STRING, locale.toString());
  }

  public Locale getLanguageOfOnlinePlayer(Player player) {
    return Optional.ofNullable(
            player.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING))
        .map(LocaleUtils::toLocale)
        .orElse(GERMANY);
  }
}
