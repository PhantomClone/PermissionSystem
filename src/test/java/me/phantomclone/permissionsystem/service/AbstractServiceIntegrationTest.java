package me.phantomclone.permissionsystem.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.phantomclone.permissionsystem.repository.PermissionRepository;
import me.phantomclone.permissionsystem.repository.RankRepository;
import me.phantomclone.permissionsystem.repository.UserPermissionRepository;
import me.phantomclone.permissionsystem.repository.UserRankRepository;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.*;

class AbstractServiceIntegrationTest {

  static HikariDataSource dataSource;
  static ExecutorService executorService;

  PermissionRepository permissionRepository;
  RankRepository rankRepository;
  UserPermissionRepository userPermissionRepository;
  UserRankRepository userRankRepository;

  PermissionService permissionService;
  RankService rankService;
  UserPermissionRankService userPermissionRankService;
  UserPermissionService userPermissionService;
  UserRankService userRankService;

  @BeforeAll
  static void beforeAll() throws IOException {
    Properties properties = new Properties();

    properties.load(
        AbstractServiceIntegrationTest.class.getResourceAsStream("datasource.properties"));

    executorService = Executors.newFixedThreadPool(4);
    dataSource = new HikariDataSource(new HikariConfig(properties));
  }

  @AfterAll
  static void afterAll() {
    executorService.shutdown();
    executorService.close();
    dataSource.close();
  }

  @BeforeEach
  void beforeEach() {
    this.permissionRepository =
        new PermissionRepository(dataSource, () -> command -> executorService.execute(command));
    this.rankRepository =
        new RankRepository(dataSource, () -> command -> executorService.execute(command));
    this.userPermissionRepository =
        new UserPermissionRepository(dataSource, () -> command -> executorService.execute(command));
    this.rankService = new RankService(rankRepository);
    this.userRankRepository =
        new UserRankRepository(
            dataSource, () -> command -> executorService.execute(command), rankService);

    permissionRepository.initialize();
    rankRepository.initialize();
    userPermissionRepository.initialize();
    userRankRepository.initialize();

    this.permissionService = new PermissionService(permissionRepository);
    this.userPermissionService = new UserPermissionService(userPermissionRepository);
    this.userRankService = new UserRankService(userRankRepository);
    this.userPermissionRankService =
        new UserPermissionRankService(userRankService, userPermissionService);
  }

  @AfterEach
  void afterEach() throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement =
            connection.prepareStatement(
                """
    do $$ declare
    r record;
begin
    for r in (select tablename from pg_tables where schemaname = 'public') loop
        execute 'drop table if exists ' || quote_ident(r.tablename) || ' cascade';
    end loop;
end $$;
""")) {
      preparedStatement.execute();
    }
  }
}
