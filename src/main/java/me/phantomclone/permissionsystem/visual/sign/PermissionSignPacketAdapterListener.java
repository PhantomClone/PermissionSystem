package me.phantomclone.permissionsystem.visual.sign;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import me.phantomclone.permissionsystem.entity.PermissionRankUser;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.entity.rank.UserRank;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.service.UserPermissionRankService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.ListPersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

public class PermissionSignPacketAdapterListener extends PacketAdapter implements Listener {

  private static final String SIGN_ROW_1_IDENTIFIER = "permission_sign_row_1";

  private static final String SIGN_ROW_3_IDENTIFIER = "permission_sign_row_3";
  private static final String SIGN_ROW_4_TIME_PATTERN_IDENTIFIER =
      "permission_sign_row_4_time_pattern";
  private static final String SIGN_ROW_4_INFINITY_IDENTIFIER = "permission_sign_row_4_infinity";

  private final JavaPlugin javaPlugin;
  private final UserPermissionRankService userPermissionRankService;
  private final LanguageService languageService;
  private final Rank defaultRank;
  private final NamespacedKey signNamespacedKey;
  private final ListPersistentDataType<List<Integer>, List<Integer>> sighPersistentDataType;
  private final List<Sign> knownSignBlocks;

  public PermissionSignPacketAdapterListener(
      JavaPlugin javaPlugin,
      UserPermissionRankService userPermissionRankService,
      LanguageService languageService,
      Rank defaultRank) {
    super(javaPlugin, ListenerPriority.NORMAL, PacketType.Play.Server.TILE_ENTITY_DATA);
    this.javaPlugin = javaPlugin;
    this.signNamespacedKey = NamespacedKey.fromString("permission_sign", javaPlugin);
    this.userPermissionRankService = userPermissionRankService;
    this.languageService = languageService;
    this.defaultRank = defaultRank;
    this.sighPersistentDataType =
        PersistentDataType.LIST.listTypeFrom(
            PersistentDataType.LIST.listTypeFrom(PersistentDataType.INTEGER));
    this.knownSignBlocks = new ArrayList<>();

    javaPlugin.getServer().getPluginManager().registerEvents(this, javaPlugin);

    setUpSignUpdateTimer(javaPlugin);

    checkForSignsInLoadedChucks(javaPlugin);
  }

  @Override
  public void onPacketSending(PacketEvent event) {
    BlockPosition blockPosition = event.getPacket().getBlockPositionModifier().read(0);
    Block block =
        event
            .getPlayer()
            .getWorld()
            .getBlockAt(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());

    if (isNoPermissionSignAt(block)) {
      return;
    }

    CompletableFuture<PermissionRankUser> permissionRankUserCompletableFuture =
        userPermissionRankService.getPermissionRankUser(event.getPlayer().getUniqueId());
    if (permissionRankUserCompletableFuture.isDone()) {
      return;
    }
    PermissionRankUser permissionRankUser = permissionRankUserCompletableFuture.join();

    NbtCompound nbtCompound = readAndWriteNewSignLines(event, permissionRankUser);
    event.getPacket().getNbtModifier().write(0, nbtCompound);
  }

  @EventHandler
  public void onChunkLoad(ChunkLoadEvent event) {
    findSignsOfPDCWhichKnownSignThenConsumer(
        event.getChunk().getPersistentDataContainer(), event.getWorld(), knownSignBlocks::add);
  }

  @EventHandler
  public void onChunkUnload(ChunkUnloadEvent event) {
    findSignsOfPDCWhichKnownSignThenConsumer(
        event.getChunk().getPersistentDataContainer(), event.getWorld(), knownSignBlocks::remove);
  }

  void setKeyForPermissionSign(Block block) {
    PersistentDataContainer persistentDataContainer = block.getChunk().getPersistentDataContainer();

    if (persistentDataContainer.has(signNamespacedKey)) {
      List<List<Integer>> cords =
          persistentDataContainer.get(signNamespacedKey, sighPersistentDataType);

      persistentDataContainer.set(
          signNamespacedKey,
          sighPersistentDataType,
          Stream.concat(
                  cords.stream(), Stream.of(List.of(block.getX(), block.getY(), block.getZ())))
              .toList());
    } else {
      persistentDataContainer.set(
          signNamespacedKey,
          sighPersistentDataType,
          List.of(List.of(block.getX(), block.getY(), block.getZ())));
    }
  }

  void addSignToKnownSign(Sign sign) {
    knownSignBlocks.add(sign);
  }

  void removeSignToKnownSign(Sign signToRemove) {
    List<Sign> foundSigns =
        knownSignBlocks.stream()
            .filter(sign -> sign.getLocation().equals(signToRemove.getLocation()))
            .toList();
    if (foundSigns.isEmpty()) {
      return;
    }
    foundSigns.forEach(knownSignBlocks::remove);

    removeSignKey(signToRemove.getBlock());
  }

  private NbtCompound readAndWriteNewSignLines(
      PacketEvent event, PermissionRankUser permissionRankUser) {
    NbtCompound nbtCompound = (NbtCompound) event.getPacket().getNbtModifier().read(0);
    NbtBase<Map<String, NbtList<String>>> frontText = nbtCompound.getValue("front_text");
    Map<String, NbtList<String>> value = frontText.getValue();

    Rank rank = permissionRankUser.highestRank().orElse(defaultRank);
    value.put(
        "messages",
        NbtFactory.ofList(
            "messages",
            JSONComponentSerializer.json()
                .serialize(
                    languageService
                        .getMessageComponent(SIGN_ROW_1_IDENTIFIER, event.getPlayer())
                        .orElse(Component.text("Rank:"))),
            JSONComponentSerializer.json().serialize(rank.prefix()),
            JSONComponentSerializer.json()
                .serialize(
                    languageService
                        .getMessageComponent(SIGN_ROW_3_IDENTIFIER, event.getPlayer())
                        .orElse(Component.text("Duration:"))),
            JSONComponentSerializer.json()
                .serialize(
                    permissionRankUser.ranks().stream()
                        .filter(userRank -> userRank.rank().id() == rank.id())
                        .map(UserRank::until)
                        .findFirst()
                        .orElse(Optional.empty())
                        .map(localDateTime -> untilToString(event.getPlayer(), localDateTime))
                        .orElseGet(
                            () ->
                                languageService
                                    .getMessageComponent(
                                        SIGN_ROW_4_INFINITY_IDENTIFIER, event.getPlayer())
                                    .orElse(Component.text("INFINITY"))))));

    nbtCompound.put("front_text", frontText);
    return nbtCompound;
  }

  private Component untilToString(Player player, LocalDateTime localDateTime) {
    Duration duration = Duration.between(LocalDateTime.now(), localDateTime);

    long days = duration.toDays();
    long hours = duration.toHours() % 24;
    long minutes = duration.toMinutes() % 60;
    long seconds = duration.getSeconds() % 60;

    return languageService
        .getMessageComponent(SIGN_ROW_4_TIME_PATTERN_IDENTIFIER, player)
        .orElse(
            Component.text("{days} days, {hours} hours, {minutes} minutes and {seconds} seconds"))
        .replaceText(builder -> builder.match("\\{days\\}").replacement(Long.toString(days)))
        .replaceText(builder -> builder.match("\\{hours\\}").replacement(Long.toString(hours)))
        .replaceText(builder -> builder.match("\\{minutes\\}").replacement(Long.toString(minutes)))
        .replaceText(builder -> builder.match("\\{seconds\\}").replacement(Long.toString(seconds)));
  }

  private void checkForSignsInLoadedChucks(JavaPlugin javaPlugin) {
    javaPlugin.getServer().getWorlds().stream()
        .map(World::getLoadedChunks)
        .flatMap(Arrays::stream)
        .forEach(
            chunk ->
                findSignsOfPDCWhichKnownSignThenConsumer(
                    chunk.getPersistentDataContainer(), chunk.getWorld(), knownSignBlocks::add));
  }

  private void setUpSignUpdateTimer(JavaPlugin javaPlugin) {
    javaPlugin
        .getServer()
        .getAsyncScheduler()
        .runAtFixedRate(
            javaPlugin,
            task -> knownSignBlocks.forEach(BlockState::update),
            5,
            5,
            TimeUnit.SECONDS);
  }

  private void findSignsOfPDCWhichKnownSignThenConsumer(
      PersistentDataContainer persistentDataContainer, World world, Consumer<Sign> signConsumer) {
    getValueOf(persistentDataContainer)
        .ifPresent(
            lists ->
                lists.stream()
                    .filter(integers -> integers.size() == 3)
                    .map(cords -> world.getBlockAt(cords.get(0), cords.get(1), cords.get(2)))
                    .map(Block::getState)
                    .filter(blockState -> Sign.class.isAssignableFrom(blockState.getClass()))
                    .map(blockState -> (Sign) blockState)
                    .filter(
                        sign ->
                            knownSignBlocks.stream()
                                .noneMatch(
                                    knownSign ->
                                        knownSign.getLocation().equals(sign.getLocation())))
                    .forEach(signConsumer));
  }

  private void removeSignKey(Block block) {
    PersistentDataContainer persistentDataContainer = block.getChunk().getPersistentDataContainer();
    getValueOf(persistentDataContainer)
        .ifPresent(
            lists ->
                persistentDataContainer.set(
                    signNamespacedKey,
                    sighPersistentDataType,
                    lists.stream()
                        .filter(cords -> cords.size() == 3)
                        .filter(cords -> cords.get(0) == block.getX())
                        .filter(cords -> cords.get(1) == block.getY())
                        .filter(cords -> cords.get(2) == block.getZ())
                        .toList()));
  }

  private boolean isNoPermissionSignAt(Block block) {
    PersistentDataContainer persistentDataContainer = block.getChunk().getPersistentDataContainer();
    if (!persistentDataContainer.has(signNamespacedKey)) {
      return true;
    }

    return persistentDataContainer.get(signNamespacedKey, sighPersistentDataType).stream()
        .filter(cords -> cords.size() == 3)
        .filter(cords -> cords.get(0) == block.getX())
        .filter(cords -> cords.get(1) == block.getY())
        .filter(cords -> cords.get(2) == block.getZ())
        .findAny()
        .isEmpty();
  }

  private Optional<List<List<Integer>>> getValueOf(
      PersistentDataContainer persistentDataContainer) {
    if (!persistentDataContainer.has(signNamespacedKey)) {
      return Optional.empty();
    }

    return Optional.of(persistentDataContainer.get(signNamespacedKey, sighPersistentDataType));
  }
}
