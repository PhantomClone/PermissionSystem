package me.phantomclone.permissionsystem.inject;

import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.entity.permission.UserPermission;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.entity.rank.UserRank;
import me.phantomclone.permissionsystem.event.PermissionRankUserUpdateEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PermissibleBaseListener extends PermissibleBase implements Listener {

  private final PermissionTree permissionTree;
  private PermissionRankUser permissionRankUser;

  public PermissibleBaseListener(ServerOperator serverOperator, PermissionRankUser permissionRankUser) {
    super(serverOperator);
    this.permissionTree = new PermissionTree();
    this.permissionRankUser = permissionRankUser;
  }

  private void clearAndFillPermissionTree() {
    permissionTree.clear();

    permissionRankUser.userPermissions().stream()
        .filter(
            userPermission ->
                userPermission
                    .until()
                    .filter(localDateTime -> LocalDateTime.now().isAfter(localDateTime))
                    .isEmpty())
        .forEach(this::addIntoPermissionTree);
    permissionRankUser.ranks().stream()
        .filter(
            userRank ->
                userRank
                    .until()
                    .filter(localDateTime -> LocalDateTime.now().isAfter(localDateTime))
                    .isEmpty())
        .forEach(this::addIntoPermissionTree);
  }

  @EventHandler
  public void onPermissionRankUserUpdateEvent(PermissionRankUserUpdateEvent event) {
    if (!event.getPermissionRankUser().uuid().equals(permissionRankUser.uuid())) {
      return;
    }

    this.permissionRankUser = event.getPermissionRankUser();
    clearAndFillPermissionTree();
  }

  @EventHandler
  public void onPlayerQuitEvent(PlayerQuitEvent event) {
    if (!event.getPlayer().getUniqueId().equals(permissionRankUser.uuid())) {
      return;
    }

    HandlerList.unregisterAll(this);
  }

  @Override
  public boolean isPermissionSet(String name) {
    return this.permissionTree.containsPermission(
        splitPermission(name.toLowerCase(Locale.ENGLISH)));
  }

  @Override
  public boolean isPermissionSet(Permission perm) {
    return this.isPermissionSet(perm.getName());
  }

  @Override
  public boolean hasPermission(@NotNull String inName) {
    return isOp() || permissionTree.getValue(splitPermission(inName.toLowerCase(Locale.ENGLISH)));
  }

  @Override
  public boolean hasPermission(Permission perm) {
    return hasPermission(perm.getName());
  }

  @Override
  public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
    return new PermissionAttachment(plugin, this);
  }

  @Override
  public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
    return addAttachment(plugin, "", false);
  }

  @Override
  public void removeAttachment(@NotNull PermissionAttachment attachment) {}

  @Override
  public void recalculatePermissions() {}

  @Override
  public void clearPermissions() {}

  @Override
  public @Nullable PermissionAttachment addAttachment(
          @NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
    return addAttachment(plugin);
  }

  @Override
  public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
    return addAttachment(plugin);
  }

  @Override
  public synchronized @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
    return Stream.concat(
            permissionRankUser.userPermissions().stream()
                .filter(
                    userPermission ->
                        userPermission
                            .until()
                            .filter(localDateTime -> LocalDateTime.now().isAfter(localDateTime))
                            .isEmpty())
                .map(UserPermission::permission),
            permissionRankUser.ranks().stream()
                .filter(
                    userRank ->
                        userRank
                            .until()
                            .filter(localDateTime -> LocalDateTime.now().isAfter(localDateTime))
                            .isEmpty())
                .map(UserRank::rank)
                .map(Rank::getAllPermissionList)
                .flatMap(Collection::stream))
        .map(me.phantomclone.permissionsystem.entity.permission.Permission::permission)
        .map(
            permission ->
                new PermissionAttachmentInfo(this, permission, null, !permission.startsWith("-")))
        .collect(Collectors.toSet());
  }

  public static void inject(
      JavaPlugin javaPlugin, Player player, PermissionRankUser permissionRankUser)
      throws NoSuchFieldException, IllegalAccessException {

    PermissibleBaseListener permissibleBase = new PermissibleBaseListener(player, permissionRankUser);

    Field perm =
        Arrays.stream(player.getClass().getSuperclass().getDeclaredFields())
            .filter(field -> field.getType() == PermissibleBase.class)
            .findFirst()
            .orElseThrow(
                () -> new NoSuchFieldException("No field with type PermissibleBase found."));
    perm.setAccessible(true);
    perm.set(player, permissibleBase);
    perm.setAccessible(false);

    permissibleBase.clearAndFillPermissionTree();

    javaPlugin.getServer().getPluginManager().registerEvents(permissibleBase, javaPlugin);
  }

  private void addIntoPermissionTree(UserRank userRank) {
    LocalDateTime nullableUntil = userRank.nullableUntil();

    userRank.rank().getAllPermissionList().stream()
        .map(me.phantomclone.permissionsystem.entity.permission.Permission::permission)
        .map(name -> name.toLowerCase(Locale.ENGLISH))
        .map(PermissibleBaseListener::splitPermission)
        .forEach(
            permissionArray ->
                permissionTree.add(
                    permissionArray, !permissionArray[0].startsWith("-"), nullableUntil));
  }

  private void addIntoPermissionTree(UserPermission userPermission) {
    String[] permissionArray =
        splitPermission(userPermission.permission().permission().toLowerCase(Locale.ENGLISH));
    permissionTree.add(
        permissionArray, !permissionArray[0].startsWith("-"), userPermission.nullableUntil());
  }

  private static String[] splitPermission(String permString) {
    return permString.split("\\.");
  }
}
