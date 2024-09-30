package me.phantomclone.permissionsystem;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import me.phantomclone.permissionsystem.repository.PermissionRepository;
import me.phantomclone.permissionsystem.repository.RankRepository;
import me.phantomclone.permissionsystem.repository.UserPermissionRepository;
import me.phantomclone.permissionsystem.repository.UserRankRepository;
import me.phantomclone.permissionsystem.service.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executor;
import java.util.logging.Level;

@Getter
public class PermissionSystemPlugin extends JavaPlugin {

  private static final String DATASOURCE_PROPERTY = "datasource.properties";

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

  @Override
  public void onLoad() {
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
        new UserPermissionRankService(userRankService, userPermissionService);
  }

  @Override
  public void onDisable() {
    dataSource.close();
  }

  public Executor getAsyncExecutor() {
    return runnable -> getServer().getAsyncScheduler().runNow(this, task -> runnable.run());
  }
}
