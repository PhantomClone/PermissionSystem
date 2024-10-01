package me.phantomclone.permissionsystem.command;

import java.lang.reflect.Method;

import me.phantomclone.permissionsystem.command.annotation.CommandArgument;
import me.phantomclone.permissionsystem.command.annotation.CommandInfo;
import me.phantomclone.permissionsystem.command.annotation.CommandTabArgument;

public record CommandInformation(Object commandObject,
                                 CommandInfo commandInfo,
                                 int minArgsLength,
                                 int maxArgsLength,
                                 Method commandMethod,
                                 CommandArgument[] commandArguments,
                                 Method[] commandArgumentsMethods,
                                 CommandTabArgument[] commandTabArgument,
                                 Method[] commandTabArgumentsMethods) { }
