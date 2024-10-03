package me.phantomclone.permissionsystem.command;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import me.phantomclone.permissionsystem.command.annotation.CommandArgument;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.language.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class CommandExecutor implements TabExecutor {

  private static final String COMMAND_EXCEPTION_MESSAGE_IDENTIFIER = "command_exception_error";

  private final JavaPlugin javaPlugin;
  private final Logger logger;
  private final LanguageService languageService;
  private final Map<CommandInformation, Boolean> commandInformationMap = new ConcurrentHashMap<>();

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    List<CommandInformation> informationList =
        commandInformationMap.keySet().stream()
            .filter(info -> info.commandInfo().commandSyntax()[0].equalsIgnoreCase(label))
            .filter(info -> info.minArgsLength() <= args.length)
            .filter(info -> info.maxArgsLength() >= args.length)
            .filter(
                info ->
                    info.commandInfo().permission().isEmpty()
                        || sender.hasPermission(info.commandInfo().permission()))
            .filter(
                info ->
                    info.commandMethod().getParameterTypes()[0].isAssignableFrom(sender.getClass()))
            .toList();

    if (informationList.isEmpty()) {
      commandInformationMap.keySet().stream()
          .filter(info -> info.commandInfo().commandSyntax()[0].equalsIgnoreCase(label))
          .filter(
              info ->
                  info.commandInfo().permission().isEmpty()
                      || sender.hasPermission(info.commandInfo().permission()))
          .map(CommandInformation::commandInfo)
          .forEach(
              info ->
                  MessageUtil.sendMessage(
                      languageService,
                      sender,
                      info.helpMessageIdentifier(),
                      component -> component));
    } else {
      informationList.forEach(info -> handleCommandInformation(sender, args, info));
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] arguments) {
    return commandInformationMap.keySet().stream()
        .filter(info -> info.commandInfo().commandSyntax()[0].equalsIgnoreCase(label))
        .filter(info -> info.commandInfo().commandSyntax().length - 1 >= arguments.length)
        .filter(
            info ->
                info.commandInfo().permission().isEmpty()
                    || sender.hasPermission(info.commandInfo().permission()))
        .filter(
            info -> {
              for (int i = 0; i < arguments.length - 1; i++) {
                if (info.commandArguments()[i] == null) {
                  if (!arguments[i].equalsIgnoreCase(info.commandInfo().commandSyntax()[i + 1])) {
                    return false;
                  }
                }
              }

              return true;
            })
        .map(info -> invoke(info, sender, arguments))
        .flatMap(Collection::stream)
        .distinct()
        .toList();
  }

  public void addCommand(CommandInformation commandInformation) {
    commandInformationMap.put(commandInformation, true);
  }

  public void removeCommand(Object command) {
    commandInformationMap.keySet().stream()
        .filter(info -> info.commandObject().equals(command))
        .findFirst()
        .ifPresent(commandInformationMap::remove);
  }

  private void handleCommandInformation(
      CommandSender sender, String[] arguments, CommandInformation commandInformation) {
    String[] commandSyntax = commandInformation.commandInfo().commandSyntax();
    List<Integer> integers = checkForArguments(arguments, commandInformation, commandSyntax);
    if (integers == null) {
      return;
    }

    Map<Integer, Object> map =
        toObjectMap(sender, arguments, commandInformation, commandSyntax, integers);
    if (map == null) {
      return;
    }

    Map<Integer, CompletableFuture<?>> completableFutureMap = selectCompletableFutures(map);

    if (completableFutureMap.isEmpty()) {
      execute(sender, commandInformation, arguments, map.values());
    } else {
      javaPlugin
          .getServer()
          .getAsyncScheduler()
          .runNow(
              javaPlugin,
              task ->
                  CompletableFuture.allOf(
                          completableFutureMap.values().toArray(CompletableFuture[]::new))
                      .whenComplete(
                          (unused, throwable) ->
                              executeAsync(
                                  sender,
                                  commandInformation,
                                  arguments,
                                  map,
                                  completableFutureMap,
                                  throwable)));
    }
  }

  private static Map<Integer, CompletableFuture<?>> selectCompletableFutures(
      Map<Integer, Object> map) {
    Map<Integer, CompletableFuture<?>> completableFutureMap = new HashMap<>();
    for (Map.Entry<Integer, Object> integerObjectEntry : map.entrySet()) {
      if (integerObjectEntry.getValue() instanceof CompletableFuture<?> completableFuture) {
        completableFutureMap.put(integerObjectEntry.getKey(), completableFuture);
      }
    }
    return completableFutureMap;
  }

  private Map<Integer, Object> toObjectMap(
      CommandSender sender,
      String[] args,
      CommandInformation commandInformation,
      String[] commandSyntax,
      List<Integer> integers) {
    Map<Integer, Object> map = new HashMap<>();
    map.put(-1, sender);
    for (Integer index : integers) {
      Object object = mapToArgumentObject(commandInformation, args[index], index);
      if (object == null) {
        CommandArgument commandArgument = commandInformation.commandArguments()[index];
        MessageUtil.sendMessage(
            languageService,
            sender,
            commandArgument.parseErrorMessageIdentifier(),
            component -> component);
        return null;
      }
      map.put(index, object);
    }

    if (commandInformation.commandMethod().getParameterCount() != map.size()) {
      if (commandSyntax[commandSyntax.length - 1].startsWith("?")) {
        map.put(commandSyntax.length - 1, null);
      } else {
        return null;
      }
    }
    return map;
  }

  private static List<Integer> checkForArguments(
      String[] args, CommandInformation commandInformation, String[] commandSyntax) {
    List<Integer> integers = new ArrayList<>();
    for (int i = 0; i < args.length; i++) {
      CommandArgument commandArgument = commandInformation.commandArguments()[i];
      if (commandArgument == null) {
        if (!commandSyntax[i + 1].equalsIgnoreCase(args[i])) {
          return null;
        }
      } else {
        integers.add(i);
      }
    }
    return integers;
  }

  private void executeAsync(
      CommandSender sender,
      CommandInformation commandInformation,
      String[] arguments,
      Map<Integer, Object> map,
      Map<Integer, CompletableFuture<?>> completableFutureMap,
      Throwable throwable) {
    if (throwable != null) {
      completableFutureMap.entrySet().stream()
          .filter(
              completableFutureEntry ->
                  completableFutureEntry.getValue().isCompletedExceptionally())
          .map(Map.Entry::getKey)
          .forEach(index -> sendAsyncError(sender, commandInformation, index));
    } else {
      completableFutureMap.forEach(
          (integer, completableFuture) ->
              executeAsync(sender, commandInformation, map, integer, completableFuture));

      for (Map.Entry<Integer, Object> integerObjectEntry : map.entrySet()) {
        if (integerObjectEntry.getKey() != -1
            && integerObjectEntry.getValue() == null
            && !commandInformation
                .commandArguments()[integerObjectEntry.getKey() - 1]
                .value()
                .startsWith("?")) {
          return;
        }
      }
      execute(sender, commandInformation, arguments, map.values());
    }
  }

  private void executeAsync(
      CommandSender sender,
      CommandInformation commandInformation,
      Map<Integer, Object> map,
      Integer index,
      CompletableFuture<?> completableFuture) {
    try {
      Object value = completableFuture.get();
      if (value == null) {
        sendAsyncError(sender, commandInformation, index);
        return;
      }
      map.put(index, value);
    } catch (InterruptedException | ExecutionException ignore) {
      // ERROR IS HANDLED
    }
  }

  private void sendAsyncError(
      CommandSender sender, CommandInformation commandInformation, Integer index) {
    CommandArgument commandArgument = commandInformation.commandArguments()[index];
    MessageUtil.sendMessage(
        languageService,
        sender,
        commandArgument.parseErrorMessageIdentifier(),
        component -> component);
  }

  private void execute(
      CommandSender sender,
      CommandInformation commandInformation,
      String[] arguments,
      Collection<Object> list) {
    try {
      commandInformation.commandMethod().invoke(commandInformation.commandObject(), list.toArray());
    } catch (IllegalAccessException | InvocationTargetException exception) {
      MessageUtil.sendMessage(
          languageService, sender, COMMAND_EXCEPTION_MESSAGE_IDENTIFIER, component -> component);
      logger.log(
          Level.SEVERE,
          "CommandInformation: %s Arguments: [%s]"
              .formatted(commandInformation.toString(), String.join(" ", arguments)),
          exception);
    }
  }

  private Object mapToArgumentObject(
      CommandInformation commandInformation, String argument, int index) {
    try {
      Method method = commandInformation.commandArgumentsMethods()[index];
      return method.invoke(commandInformation.commandObject(), argument);
    } catch (IllegalAccessException | InvocationTargetException e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> invoke(CommandInformation info, CommandSender commandSender, String[] args) {
    Method method = info.commandTabArgumentsMethods()[args.length - 1];
    if (method == null) {
      String syntax = info.commandInfo().commandSyntax()[args.length];
      if (syntax.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
        return List.of(syntax);
      }

      return List.of();
    }

    for (int i = 0; i < args.length; i++) {
      if (info.commandArgumentsMethods()[i] == null
          && !args[i].equalsIgnoreCase(info.commandInfo().commandSyntax()[i + 1])) {
        return List.of();
      }
    }

    try {
      if (method.getParameterCount() == 1) {
        return (List<String>) method.invoke(info.commandObject(), args[args.length - 1]);
      } else {
        return (List<String>)
            method.invoke(info.commandObject(), commandSender, args[args.length - 1]);
      }
    } catch (Exception exception) {
      return List.of();
    }
  }
}
