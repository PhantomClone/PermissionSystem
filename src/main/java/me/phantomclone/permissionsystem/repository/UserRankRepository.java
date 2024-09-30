package me.phantomclone.permissionsystem.repository;

import de.chojo.sadu.base.QueryFactory;
import de.chojo.sadu.wrapper.QueryBuilderConfig;
import de.chojo.sadu.wrapper.util.ParamBuilder;
import de.chojo.sadu.wrapper.util.Row;
import de.chojo.sadu.wrapper.util.UpdateResult;
import me.phantomclone.permissionsystem.entity.rank.UserRank;
import me.phantomclone.permissionsystem.service.RankService;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class UserRankRepository extends QueryFactory {

  private static final String CREATE_TABLE_USER_RANK =
      "CREATE TABLE IF NOT EXISTS pl_user_rank ("
          + "rank_id BIGINT, "
          + "uuid UUID, "
          + "since TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
          + "until TIMESTAMP, "
          + "PRIMARY KEY (rank_id, uuid), "
          + "FOREIGN KEY (rank_id) REFERENCES pl_rank(id) ON DELETE CASCADE)";
  private static final String STORE_USER_PERMISSION_QUERY =
      "INSERT INTO pl_user_rank (rank_id, uuid, until) VALUES (?, ?, ?)";
  private static final String SELECT_QUERY =
      "SELECT rank_id, since, until FROM pl_user_rank WHERE uuid = ?";
  private static final String DELETE_ONE_USER_PERMISSION_QUERY =
      "DELETE FROM pl_user_rank WHERE uuid = ? AND rank_id = ?";

  private final Supplier<Executor> executorSupplier;
  private final RankService rankService;

  public UserRankRepository(
      DataSource dataSource, Supplier<Executor> executorSupplier, RankService rankService) {
    super(dataSource, QueryBuilderConfig.builder().throwExceptions().build());
    this.executorSupplier = executorSupplier;
    this.rankService = rankService;
  }

  public void initialize() {
    builder().query(CREATE_TABLE_USER_RANK).emptyParams().insert().sendSync();
  }

  public CompletableFuture<Boolean> storeEntity(UserRank userRank) {
    return builder()
        .query(STORE_USER_PERMISSION_QUERY)
        .parameter(paramBuilder -> addOrUpdateParameter(userRank, paramBuilder))
        .insert()
        .send(executorSupplier.get())
        .thenApply(UpdateResult::changed);
  }

  public CompletableFuture<List<UserRank>> getAllUserRankOf(UUID uuid) {
    return builder(RawUserRank.class)
        .query(SELECT_QUERY)
        .parameter(paramBuilder -> paramBuilder.setObject(uuid))
        .readRow(row -> toRawUserPermission(row, uuid))
        .all(executorSupplier.get())
        .thenCompose(rawUserRanks -> fetchRankFromRankRepository(uuid, rawUserRanks));
  }

  public CompletableFuture<Boolean> deleteUserRank(UUID uuid, long rankId) {
    return builder()
        .query(DELETE_ONE_USER_PERMISSION_QUERY)
        .parameter(paramBuilder -> paramBuilder.setObject(uuid).setLong(rankId))
        .delete()
        .send(executorSupplier.get())
        .thenApply(UpdateResult::changed);
  }

  private CompletableFuture<List<UserRank>> fetchRankFromRankRepository(
      UUID uuid, List<RawUserRank> rawUserRanks) {
    List<CompletableFuture<UserRank>> list =
        rawUserRanks.stream()
            .map(
                rawUserRank ->
                    rankService
                        .getRank(rawUserRank.rankId)
                        .thenApply(
                            optionalRank ->
                                optionalRank
                                    .map(
                                        rank ->
                                            new UserRank(
                                                uuid,
                                                rank,
                                                rawUserRank.since(),
                                                rawUserRank.until()))
                                    .orElse(null)))
            .toList();

    return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new))
        .thenApply(
            unused -> list.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList());
  }

  private void addOrUpdateParameter(UserRank userRank, ParamBuilder paramBuilder)
      throws SQLException {
    paramBuilder
        .setLong(userRank.rank().id())
        .setObject(userRank.uuid())
        .setLocalDateTime(userRank.until().orElse(null));
  }

  private record RawUserRank(long rankId, UUID uuid, LocalDateTime since, LocalDateTime until) {}

  private RawUserRank toRawUserPermission(Row row, UUID uuid) throws SQLException {
    return new RawUserRank(
        row.getLong("rank_id"), uuid, row.getLocalDateTime("since"), row.getLocalDateTime("until"));
  }
}
