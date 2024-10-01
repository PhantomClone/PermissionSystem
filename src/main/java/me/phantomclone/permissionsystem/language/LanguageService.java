package me.phantomclone.permissionsystem.language;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.language.entity.MessageEntity;
import me.phantomclone.permissionsystem.language.util.ComponentJsonDeserializer;
import me.phantomclone.permissionsystem.language.util.ComponentJsonSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public class LanguageService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    SimpleModule simpleModule = new SimpleModule();
    simpleModule.addSerializer(Component.class, new ComponentJsonSerializer());
    simpleModule.addDeserializer(Component.class, new ComponentJsonDeserializer());
    OBJECT_MAPPER.registerModule(simpleModule);
  }

  private final List<MessageEntity> messageEntityList = new LinkedList<>();
  private final LanguageUserService languageUserService;

  public void registerMessages(Locale locale, File jsonMessageFile) throws IOException {
    OBJECT_MAPPER.readValue(jsonMessageFile, new TypeReference<List<MessageEntity>>() {}).stream()
        .map(
            messageEntity ->
                new MessageEntity(
                    messageEntity.identifier(), locale, messageEntity.messageComponent()))
        .forEach(this::addOrReplaceMessageEntity);
  }

  public Optional<Component> getMessageComponent(String identifier, Player player) {
    Locale locale = languageUserService.getLanguageOfOnlinePlayer(player);

    return getMessageComponent(identifier, locale);
  }

  public Optional<Component> getMessageComponent(String identifier, Locale locale) {
    return messageEntityList.stream()
        .filter(messageEntity -> compareMessageEntity(messageEntity, locale, identifier))
        .findFirst()
        .map(MessageEntity::messageComponent);
  }

  private synchronized void addOrReplaceMessageEntity(MessageEntity messageEntity) {
    messageEntityList.removeIf(
        allMessageEntity ->
            compareMessageEntity(
                allMessageEntity, messageEntity.locale(), messageEntity.identifier()));
    messageEntityList.add(messageEntity);
  }

  private boolean compareMessageEntity(
      MessageEntity messageEntity, Locale locale, String identifier) {
    return compareLocale(
            locale, messageEntity.locale().getLanguage(), messageEntity.locale().getCountry())
        && messageEntity.identifier().equals(identifier);
  }

  private boolean compareLocale(Locale locale, String language, String country) {
    return locale.getLanguage().equals(language) && locale.getCountry().equals(country);
  }
}
