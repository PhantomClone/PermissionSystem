package me.phantomclone.permissionsystem;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.stream.Stream;
import lombok.Getter;
import me.phantomclone.permissionsystem.cache.PlayerPermissionRankUserCacheListener;
import me.phantomclone.permissionsystem.command.CommandExecutor;
import me.phantomclone.permissionsystem.command.CommandRegistry;
import me.phantomclone.permissionsystem.command.permission.*;
import me.phantomclone.permissionsystem.command.visual.sign.AddSignCommand;
import me.phantomclone.permissionsystem.command.visual.sign.RemoveSignCommand;
import me.phantomclone.permissionsystem.entity.rank.Rank;
import me.phantomclone.permissionsystem.language.LanguageService;
import me.phantomclone.permissionsystem.language.LanguageUserService;
import me.phantomclone.permissionsystem.listener.chat.AsyncChatEventListener;
import me.phantomclone.permissionsystem.listener.login.PlayerLoginListener;
import me.phantomclone.permissionsystem.repository.permission.PermissionRepository;
import me.phantomclone.permissionsystem.repository.permission.UserPermissionRepository;
import me.phantomclone.permissionsystem.repository.rank.RankRepository;
import me.phantomclone.permissionsystem.repository.rank.UserRankRepository;
import me.phantomclone.permissionsystem.service.*;
import me.phantomclone.permissionsystem.service.permission.PermissionService;
import me.phantomclone.permissionsystem.service.permission.UserPermissionService;
import me.phantomclone.permissionsystem.service.rank.RankService;
import me.phantomclone.permissionsystem.service.rank.UserRankService;
import me.phantomclone.permissionsystem.visual.sidebar.SidebarService;
import me.phantomclone.permissionsystem.visual.sidebar.listener.PlayerJoinEventListener;
import me.phantomclone.permissionsystem.visual.sign.PermissionSignPacketAdapterListener;
import me.phantomclone.permissionsystem.visual.tablist.TabListService;
import org.apache.commons.lang3.LocaleUtils;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class PermissionSystemPlugin extends JavaPlugin {

  private static final String DATASOURCE_PROPERTY = "datasource.properties";

  private ProtocolManager protocolManager;

  private HikariDataSource dataSource;

  private PermissionRepository permissionRepository;
  private RankRepository rankRepository;
  private UserPermissionRepository userPermissionRepository;
  private UserRankRepository userRankRepository;

  private PermissionService permissionService;
  private RankService rankService;
  private UserPermissionRankService userPermissionRankService;
  private UserPermissionService userPermissionService;
  private UserRankService userRankService;

  private LanguageService languageService;
  private LanguageUserService languageUserService;

  private PermissionSignPacketAdapterListener packetListener;

  private CommandRegistry commandRegistry;
  private CommandExecutor commandExecutor;

  private SidebarService sidebarService;
  private TabListService tabListService;

  private PlayerPermissionRankUserCacheListener playerPermissionRankUserCacheListener;

  @Override
  public void onLoad() {
    protocolManager = ProtocolLibrary.getProtocolManager();

    saveResource(DATASOURCE_PROPERTY, false);

    dataSource =
        new HikariDataSource(
            new HikariConfig(String.format("%s/%s", getDataFolder(), DATASOURCE_PROPERTY)));

    this.permissionRepository = new PermissionRepository(dataSource, this::getAsyncExecutor);
    this.rankRepository = new RankRepository(dataSource, this::getAsyncExecutor);
    this.userPermissionRepository =
        new UserPermissionRepository(dataSource, this::getAsyncExecutor);

    this.rankService = new RankService(rankRepository);
    this.userRankRepository =
        new UserRankRepository(dataSource, this::getAsyncExecutor, rankService);

    getLogger().log(Level.INFO, "Initialize tables sync...");
    permissionRepository.initialize();
    rankRepository.initialize();
    userPermissionRepository.initialize();
    userRankRepository.initialize();
    getLogger().log(Level.INFO, "Initialize tables sync done.");

    this.permissionService = new PermissionService(permissionRepository);
    this.userPermissionService = new UserPermissionService(userPermissionRepository);
    this.userRankService = new UserRankService(userRankRepository);
    this.userPermissionRankService =
        new UserPermissionRankService(this, userRankService, userPermissionService);

    this.languageUserService = new LanguageUserService(this);
    this.languageService = new LanguageService(languageUserService);

    this.commandExecutor = new CommandExecutor(this, getLogger(), languageService);
    this.commandRegistry = new CommandRegistry(this, commandExecutor);

    this.sidebarService = new SidebarService();

    copyIfNotExistsAndLoadLanguageFiles();
  }

  @Override
  public void onEnable() {
    Rank defaultRank =
        rankService
            .getRank("default")
            .join()
            .orElse(rankService.createOrUpdateRank("default", 0, "<gray>User", null).join());

    this.tabListService = new TabListService(defaultRank);

    registerEvents(defaultRank);

    registerCommand();
  }

  private void registerEvents(Rank defaultRank) {
    this.playerPermissionRankUserCacheListener =
        new PlayerPermissionRankUserCacheListener(userPermissionRankService);
    languageUserService.registerListener(this, commandRegistry, languageService);

    getServer()
        .getPluginManager()
        .registerEvents(new AsyncChatEventListener(playerPermissionRankUserCacheListener), this);
    getServer().getPluginManager().registerEvents(playerPermissionRankUserCacheListener, this);
    getServer()
        .getPluginManager()
        .registerEvents(
            new PlayerJoinEventListener(
                this,
                sidebarService,
                languageService,
                playerPermissionRankUserCacheListener,
                getLogger(),
                defaultRank),
            this);
    getServer()
        .getPluginManager()
        .registerEvents(
            new me.phantomclone.permissionsystem.visual.tablist.listener.PlayerJoinEventListener(
                this, tabListService, playerPermissionRankUserCacheListener, getLogger()),
            this);

    new PlayerLoginListener(
            this,
            getLogger(),
            playerPermissionRankUserCacheListener,
            userRankService,
            userPermissionRankService,
            defaultRank,
            languageService)
        .register();

    packetListener =
        new PermissionSignPacketAdapterListener(
            this, userPermissionRankService, languageService, defaultRank);
    protocolManager.addPacketListener(packetListener);
  }

  @Override
  public void onDisable() {
    dataSource.close();
  }

  private void registerCommand() {
    commandRegistry.registerCommand(new AddSignCommand(this, languageService, packetListener));
    commandRegistry.registerCommand(new RemoveSignCommand(this, languageService, packetListener));

    commandRegistry.registerCommand(new CreateRankCommand(rankService, languageService));
    commandRegistry.registerCommand(
        new AddPermissionToRankCommand(
            rankService,
            permissionService,
            playerPermissionRankUserCacheListener,
            userPermissionRankService,
            languageService));
    commandRegistry.registerCommand(
        new RemovePermissionFromRankCommand(
            rankService,
            permissionService,
            playerPermissionRankUserCacheListener,
            userPermissionRankService,
            languageService));
    commandRegistry.registerCommand(
        new AddPermissionToPlayerCommand(
            this,
            permissionService,
            userPermissionService,
            userPermissionRankService,
            languageService));
    commandRegistry.registerCommand(
        new RemovePermissionFromPlayerCommand(
            this,
            permissionService,
            userPermissionService,
            userPermissionRankService,
            languageService));
    commandRegistry.registerCommand(
        new AddRankToPlayerCommand(
            this, rankService, userRankService, userPermissionRankService, languageService));
    commandRegistry.registerCommand(
        new RemoveRankFromPlayerCommand(
            this, rankService, userRankService, userPermissionRankService, languageService));
  }

  private Executor getAsyncExecutor() {
    return runnable -> getServer().getAsyncScheduler().runNow(this, task -> runnable.run());
  }

  private void copyIfNotExistsAndLoadLanguageFiles() {
    File languageFolder = new File(getDataFolder(), "languages");
    if (!new File(languageFolder, "de_DE.json").exists()) {
      saveResource("languages/de_DE.json", true);
    }
    if (!new File(languageFolder, "en.json").exists()) {
      saveResource("languages/en_US.json", true);
    }

    try (Stream<Path> langugesFilesStream = Files.list(Paths.get(languageFolder.getPath()))) {
      langugesFilesStream
          .filter(
              path ->
                  path.getFileName().toString().matches("^[a-zA-Z]{2,3}(_[a-zA-Z]{2,3})?\\.json$"))
          .map(Path::toFile)
          .peek(file -> getLogger().log(Level.INFO, "Add language file:{0}", file.getName()))
          .forEach(this::registerLanguageFile);
    } catch (IOException ioException) {
      getLogger().log(Level.SEVERE, "Error in loading language folder", ioException);
    }
  }

  private void registerLanguageFile(File file) {
    try {
      languageService.registerMessages(LocaleUtils.toLocale(removeJsonEnding(file)), file);
    } catch (Exception exception) {
      getLogger()
          .log(
              Level.SEVERE,
              String.format("Error in loading language file: %s", file.getName()),
              exception);
    }
  }

  private String removeJsonEnding(File file) {
    String name = file.getName();

    return name.substring(0, name.length() - ".json".length());
  }
}
