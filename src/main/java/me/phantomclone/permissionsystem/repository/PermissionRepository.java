package me.phantomclone.permissionsystem.repository;

import de.chojo.sadu.base.QueryFactory;
import de.chojo.sadu.wrapper.QueryBuilderConfig;
import de.chojo.sadu.wrapper.util.ParamBuilder;
import de.chojo.sadu.wrapper.util.Row;
import de.chojo.sadu.wrapper.util.UpdateResult;
import me.phantomclone.permissionsystem.entity.permission.Permission;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class PermissionRepository extends QueryFactory {

  private static final String CREATE_TABLE_PERMISSION =
      "CREATE TABLE IF NOT EXISTS pl_permission (id SERIAL PRIMARY KEY, permission_string VARCHAR(255) UNIQUE NOT NULL, description TEXT NOT NULL)";
  private static final String STORE_PERMISSION_QUERY =
      "INSERT INTO pl_permission(permission_string, description) VALUES(?, ?) ON CONFLICT (permission_string) DO UPDATE SET description = EXCLUDED.description";
  private static final String SELECT_ONE_QUERY =
      "SELECT id, permission_string, description FROM pl_permission WHERE permission_string=?";
  private static final String DELETE_ONE_PERMISSION_QUERY = "DELETE FROM pl_permission WHERE id=?";
  private final Supplier<Executor> executorSupplier;

  public PermissionRepository(DataSource dataSource, Supplier<Executor> executorSupplier) {
    super(dataSource, QueryBuilderConfig.builder().throwExceptions().build());
    this.executorSupplier = executorSupplier;
  }

  public void initialize() {
    builder().query(CREATE_TABLE_PERMISSION).emptyParams().insert().sendSync();
  }

  public CompletableFuture<Permission> storeEntity(Permission entity) {
    return builder()
        .query(STORE_PERMISSION_QUERY)
        .parameter(paramBuilder -> addOrUpdateParameter(entity, paramBuilder))
        .insert()
        .key(executorSupplier.get())
        .thenApply(
            id -> new Permission(id.orElse(-1L), entity.permission(), entity.descriptionString()));
  }

  public CompletableFuture<Optional<Permission>> getEntityBy(String permission) {
    return builder(Permission.class)
        .query(SELECT_ONE_QUERY)
        .parameter(paramBuilder -> paramBuilder.setString(permission))
        .readRow(PermissionRepository::toPermissionOf)
        .first(executorSupplier.get());
  }

  public CompletableFuture<Boolean> removeEntity(long id) {
    return builder()
        .query(DELETE_ONE_PERMISSION_QUERY)
        .parameter(paramBuilder -> paramBuilder.setLong(id))
        .delete()
        .send(executorSupplier.get())
        .thenApply(UpdateResult::changed);
  }

  private void addOrUpdateParameter(Permission entity, ParamBuilder paramBuilder)
      throws SQLException {
    String descriptionString = miniMessage().serialize(entity.description());

    paramBuilder.setString(entity.permission()).setString(descriptionString);
  }

  private static Permission toPermissionOf(Row row) throws SQLException {
    return new Permission(
        row.getLong("id"), row.getString("permission_string"), row.getString("description"));
  }
}
