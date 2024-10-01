package me.phantomclone.permissionsystem.language.entity;

import net.kyori.adventure.text.Component;

import java.util.Locale;

public record MessageEntity(String identifier, Locale locale, Component messageComponent) {}
