package me.phantomclone.permissionsystem.command;

import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.command.annotation.CommandArgument;
import me.phantomclone.permissionsystem.command.annotation.CommandInfo;
import me.phantomclone.permissionsystem.command.annotation.CommandTabArgument;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class CommandRegistry {

    private final JavaPlugin javaPlugin;
    private final CommandExecutor commandExecutor;

    public void registerCommand(Object command) {
        Arrays.stream(command.getClass().getMethods())
                .map(method -> methodToCommand(command, method, method.getAnnotation(CommandInfo.class)))
                .filter(Objects::nonNull)
                .forEach(commandExecutor::addCommand);
    }

    public void removeCommand(Object command) {
        commandExecutor.removeCommand(command);
    }

    private CommandInformation methodToCommand(Object object, Method commandMethod, CommandInfo commandInfo) {
        if (commandInfo == null) {
            return null;
        }

        registerPluginCommand(commandInfo);

        String[] commandArgs = Arrays.copyOfRange(commandInfo.commandSyntax(), 1, commandInfo.commandSyntax().length);

        CommandArgumentRecord[] commandArguments = findCommandArguments(object, commandInfo, commandArgs);
        CommandTabArgumentRecord[] commandTabArguments = findCommandTapArguments(object, commandInfo, commandArgs);

        int length = commandInfo.commandSyntax().length;
        return new CommandInformation(object, commandInfo, commandInfo.commandSyntax()[length - 1].startsWith("?") ? length - 2 : length - 1, length - 1, commandMethod,
                Arrays.stream(commandArguments).map(commandTabArgument -> commandTabArgument == null ? null : commandTabArgument.info).toArray(CommandArgument[]::new),
                Arrays.stream(commandArguments).map(commandTabArgument -> commandTabArgument == null ? null : commandTabArgument.parseMethod).toArray(Method[]::new),
                Arrays.stream(commandTabArguments).map(commandTabArgument -> commandTabArgument == null ? null : commandTabArgument.info).toArray(CommandTabArgument[]::new),
                Arrays.stream(commandTabArguments).map(commandTabArgument -> commandTabArgument == null ? null : commandTabArgument.parseMethod).toArray(Method[]::new));
    }

    private void registerPluginCommand(CommandInfo commandInfo) {
        PluginCommand command = javaPlugin.getCommand(commandInfo.commandSyntax()[0]);
        if (command == null) {
            throw new IllegalArgumentException("Command '%s' is not in the plugin.yml!".formatted(commandInfo.commandSyntax()[0]));
        }
        command.setExecutor(commandExecutor);
        command.setTabCompleter(commandExecutor);
    }

    private CommandTabArgumentRecord[] findCommandTapArguments(Object object, CommandInfo commandInfo, String[] commandArgs) {
        List<CommandTabArgumentRecord> commandTabArgumentList = Arrays.stream(object.getClass().getMethods())
                .map(this::toCommandTabArgument)
                .filter(Objects::nonNull)
                .toList();
        CommandTabArgumentRecord[] commandTabArguments = new CommandTabArgumentRecord[commandInfo.commandSyntax().length - 1];
        for (int i = 0; i < commandArgs.length; i++) {
            String commandArg = commandArgs[i];
            commandTabArguments[i] = commandTabArgumentList.stream()
                    .filter(commandArgument -> commandArgument.info.value().equalsIgnoreCase(commandArg))
                    .findFirst()
                    .orElse(null);
        }
        return commandTabArguments;
    }

    @NotNull
    private CommandArgumentRecord[] findCommandArguments(Object object, CommandInfo commandInfo, String[] commandArgs) {
        CommandArgumentRecord[] commandArguments = new CommandArgumentRecord[commandInfo.commandSyntax().length - 1];
        List<CommandArgumentRecord> commandArgumentList = Arrays.stream(object.getClass().getMethods())
                .map(this::toCommandArgument)
                .filter(Objects::nonNull)
                .toList();
        for (int i = 0; i < commandArgs.length; i++) {
            String commandArg = commandArgs[i];
            commandArguments[i] = commandArgumentList.stream()
                    .filter(commandArgument -> commandArgument.info.value().equalsIgnoreCase(commandArg))
                    .findFirst()
                    .orElse(null);
        }
        return commandArguments;
    }

    private CommandArgumentRecord toCommandArgument(Method method) {
        CommandArgument annotation = method.getAnnotation(CommandArgument.class);

        if (annotation == null) {
            return null;
        }

        return new CommandArgumentRecord(annotation, method);
    }

    private CommandTabArgumentRecord toCommandTabArgument(Method method) {
        CommandTabArgument annotation = method.getAnnotation(CommandTabArgument.class);

        if (annotation == null) {
            return null;
        }

        return new CommandTabArgumentRecord(annotation, method);
    }

    private record CommandArgumentRecord(CommandArgument info, Method parseMethod) { }

    private record CommandTabArgumentRecord(CommandTabArgument info, Method parseMethod) { }

}
