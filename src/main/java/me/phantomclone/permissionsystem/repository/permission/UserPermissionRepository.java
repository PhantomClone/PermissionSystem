package me.phantomclone.permissionsystem.repository.permission;

import de.chojo.sadu.base.QueryFactory;
import de.chojo.sadu.wrapper.QueryBuilderConfig;
import de.chojo.sadu.wrapper.util.ParamBuilder;
import de.chojo.sadu.wrapper.util.Row;
import de.chojo.sadu.wrapper.util.UpdateResult;
import me.phantomclone.permissionsystem.entity.permission.Permission;
import me.phantomclone.permissionsystem.entity.permission.UserPermission;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class UserPermissionRepository extends QueryFactory {

  private static final String CREATE_TABLE_PERMISSION =
      "CREATE TABLE IF NOT EXISTS pl_user_permission ("
          + "permission_id BIGINT, "
          + "uuid UUID, "
          + "since TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
          + "until TIMESTAMP, "
          + "PRIMARY KEY (permission_id, uuid), "
          + "FOREIGN KEY (permission_id) REFERENCES pl_permission(id) ON DELETE CASCADE)";
  private static final String STORE_USER_PERMISSION_QUERY =
      "INSERT INTO pl_user_permission (permission_id, uuid, until) VALUES (?, ?, ?)";
  private static final String SELECT_QUERY =
      """
                        SELECT
                            p.id,
                            p.permission_string,
                            p.description,
                            up.since,
                            up.until
                        FROM
                            pl_user_permission up
                        JOIN
                            pl_permission p ON up.permission_id = p.id
                        WHERE
                            up.uuid= ?;
            """;
  private static final String DELETE_ONE_USER_PERMISSION_QUERY =
      "DELETE FROM pl_user_permission WHERE uuid = ? AND permission_id = ?";

  private final Supplier<Executor> executorSupplier;

  public UserPermissionRepository(DataSource dataSource, Supplier<Executor> executorSupplier) {
    super(dataSource, QueryBuilderConfig.builder().throwExceptions().build());
    this.executorSupplier = executorSupplier;
  }

  public void initialize() {
    builder().query(CREATE_TABLE_PERMISSION).emptyParams().insert().sendSync();
  }

  public CompletableFuture<Boolean> storeEntity(UserPermission entity) {
    return builder()
        .query(STORE_USER_PERMISSION_QUERY)
        .parameter(paramBuilder -> addOrUpdateParameter(entity, paramBuilder))
        .insert()
        .send(executorSupplier.get())
        .thenApply(UpdateResult::changed);
  }

  public CompletableFuture<List<UserPermission>> getAllUserPermissionsOf(UUID userUUID) {
    return builder(UserPermission.class)
        .query(SELECT_QUERY)
        .parameter(paramBuilder -> paramBuilder.setObject(userUUID))
        .readRow(row -> readUserPermissionOfRow(userUUID, row))
        .all(executorSupplier.get());
  }

  public CompletableFuture<Boolean> removeEntity(UUID uuid, long permissionId) {
    return builder()
        .query(DELETE_ONE_USER_PERMISSION_QUERY)
        .parameter(paramBuilder -> paramBuilder.setObject(uuid).setLong(permissionId))
        .delete()
        .send(executorSupplier.get())
        .thenApply(UpdateResult::changed);
  }

  private static UserPermission readUserPermissionOfRow(UUID userUUID, Row row)
      throws SQLException {
    return new UserPermission(
        userUUID,
        new Permission(
            row.getLong("id"), row.getString("permission_string"), row.getString("description")),
        row.getLocalDateTime("since"),
        row.getLocalDateTime("until"));
  }

  private void addOrUpdateParameter(UserPermission entity, ParamBuilder paramBuilder)
      throws SQLException {
    paramBuilder
        .setLong(entity.permission().id())
        // .setUuidAsBytes(entity.uuid())
        .setObject(entity.uuid())
        .setLocalDateTime(entity.until().orElse(null));
  }
}
