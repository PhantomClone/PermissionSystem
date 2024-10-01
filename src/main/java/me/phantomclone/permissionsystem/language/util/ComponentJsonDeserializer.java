package me.phantomclone.permissionsystem.language.util;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import net.kyori.adventure.text.Component;

public class ComponentJsonDeserializer extends JsonDeserializer<Component> {
    @Override
    public Component deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return miniMessage().deserialize(jsonParser.readValueAs(String.class));
    }
}
