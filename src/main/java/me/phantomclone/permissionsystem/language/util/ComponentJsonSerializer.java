package me.phantomclone.permissionsystem.language.util;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import net.kyori.adventure.text.Component;

public class ComponentJsonSerializer extends JsonSerializer<Component> {

    @Override
    public void serialize(Component component, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(miniMessage().serialize(component));
    }
}

