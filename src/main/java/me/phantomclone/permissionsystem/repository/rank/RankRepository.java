package me.phantomclone.permissionsystem.repository.rank;

import de.chojo.sadu.base.QueryFactory;
import de.chojo.sadu.wrapper.QueryBuilderConfig;
import de.chojo.sadu.wrapper.util.Row;
import de.chojo.sadu.wrapper.util.UpdateResult;
import me.phantomclone.permissionsystem.entity.permission.Permission;
import me.phantomclone.permissionsystem.entity.rank.Rank;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RankRepository extends QueryFactory {

  private static final String CREATE_TABLE_RANK =
      """
            CREATE TABLE IF NOT EXISTS pl_rank (
              id SERIAL PRIMARY KEY,
              priority INT UNIQUE NOT NULL,
              name VARCHAR(50) UNIQUE NOT NULL,
              prefix VARCHAR(20) NOT NULL,
              base_rank_id BIGINT,
              FOREIGN KEY (base_rank_id) REFERENCES pl_rank(id) ON DELETE SET NULL
            )""";

  private static final String CREATE_TABLE_RANK_PERMISSION =
      """
            CREATE TABLE IF NOT EXISTS pl_rank_permission (
            permission_id INT NOT NULL, rank_id INT NOT NULL,
            PRIMARY KEY (permission_id, rank_id),
            FOREIGN KEY (permission_id) REFERENCES pl_permission(id),
            FOREIGN KEY (rank_id) REFERENCES pl_rank(id) ON DELETE CASCADE)
            """;

  private static final String INSERT_INTO_RANK =
      """
            INSERT INTO pl_rank (priority, name, prefix, base_rank_id)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (name)
            DO UPDATE SET
                priority = EXCLUDED.priority,
                prefix = EXCLUDED.prefix,
                base_rank_id = EXCLUDED.base_rank_id""";
  private static final String SELECT_BY_ID_QUERY =
      """
            WITH RECURSIVE RankHierarchy AS (
                SELECT
                    r.id,
                    r.name,
                    r.priority,
                    r.prefix,
                    r.base_rank_id
                FROM
                    pl_rank r
                WHERE
                    r.id = ?

                UNION ALL

                SELECT
                    r.id,
                    r.name,
                    r.priority,
                    r.prefix,
                    r.base_rank_id
                FROM
                    pl_rank r
                INNER JOIN
                    RankHierarchy rh
                ON
                    r.id = rh.base_rank_id
            )

            SELECT
                rh.id AS rank_id,
                rh.name AS rank_name,
                rh.priority AS rank_priority,
                rh.prefix AS rank_prefix,
                rh.base_rank_id,
                p.id AS permission_id,
                p.permission_string AS permission_string,
                p.description AS permission_description
            FROM
                RankHierarchy rh
            LEFT JOIN
                pl_rank_permission rp ON rh.id = rp.rank_id
            LEFT JOIN
                pl_permission p ON rp.permission_id = p.id;
            """;
  private static final String SELECT_BY_NAME_QUERY =
      """
            WITH RECURSIVE RankHierarchy AS (
                SELECT
                    r.id,
                    r.name,
                    r.priority,
                    r.prefix,
                    r.base_rank_id
                FROM
                    pl_rank r
                WHERE
                    r.name = ?

                UNION ALL

                SELECT
                    r.id,
                    r.name,
                    r.priority,
                    r.prefix,
                    r.base_rank_id
                FROM
                    pl_rank r
                INNER JOIN
                    RankHierarchy rh
                ON
                    r.id = rh.base_rank_id
            )

            SELECT
                rh.id AS rank_id,
                rh.name AS rank_name,
                rh.priority AS rank_priority,
                rh.prefix AS rank_prefix,
                rh.base_rank_id,
                p.id AS permission_id,
                p.permission_string AS permission_string,
                p.description AS permission_description
            FROM
                RankHierarchy rh
            LEFT JOIN
                pl_rank_permission rp ON rh.id = rp.rank_id
            LEFT JOIN
                pl_permission p ON rp.permission_id = p.id;
            """;
  private static final String RANK_EXISTS = "SELECT EXISTS(SELECT 1 FROM pl_rank WHERE name=?)";
  private static final String DELETE_RANK = "DELETE FROM pl_rank WHERE id=?";
  private static final String INSERT_INTO_ADD_PERMISSION =
      "INSERT INTO pl_rank_permission (permission_id, rank_id) VALUES (?, ?);";
  private static final String DELETE_RANK_PERMISSION =
      "DELETE FROM pl_rank_permission WHERE permission_id=? AND rank_id=?";
  private final Supplier<Executor> executorSupplier;

  public RankRepository(DataSource dataSource, Supplier<Executor> executorSupplier) {
    super(dataSource, QueryBuilderConfig.builder().throwExceptions().build());
    this.executorSupplier = executorSupplier;
  }

  public void initialize() {
    builder()
        .query(CREATE_TABLE_RANK)
        .emptyParams()
        .append()
        .query(CREATE_TABLE_RANK_PERMISSION)
        .emptyParams()
        .insert()
        .sendSync();
  }

  public CompletableFuture<Rank> createOrUpdateRank(Rank rank) {
    return builder()
        .query(INSERT_INTO_RANK)
        .parameter(
            paramBuilder ->
                paramBuilder
                    .setInt(rank.priority())
                    .setString(rank.name())
                    .setString(rank.prefixString())
                    .setLong(
                        Optional.ofNullable(rank.nullableBaseRank()).map(Rank::id).orElse(null)))
        .insert()
        .key(executorSupplier.get())
        .thenApply(
            optionalId ->
                new Rank(
                    optionalId.orElse(-1L),
                    rank.name(),
                    rank.priority(),
                    rank.prefixString(),
                    List.of(),
                    rank.nullableBaseRank()));
  }

  public CompletableFuture<Boolean> rankExists(String name) {
    return builder(Boolean.class)
            .query(RANK_EXISTS)
            .parameter(paramBuilder -> paramBuilder.setString(name))
            .readRow(row -> row.getBoolean("exists"))
            .first()
            .thenApply(optionalResult -> optionalResult.orElse(false));
  }

  public CompletableFuture<Optional<Rank>> getRank(long id) {
    return builder(SelectRecord.class)
        .query(SELECT_BY_ID_QUERY)
        .parameter(paramBuilder -> paramBuilder.setLong(id))
        .readRow(RankRepository::readSelectRecordOfRow)
        .all(executorSupplier.get())
        .thenApply(RankRepository::buildRankHierarchy);
  }

  public CompletableFuture<Optional<Rank>> getRank(String name) {
    return builder(SelectRecord.class)
        .query(SELECT_BY_NAME_QUERY)
        .parameter(paramBuilder -> paramBuilder.setString(name))
        .readRow(RankRepository::readSelectRecordOfRow)
        .all(executorSupplier.get())
        .thenApply(RankRepository::buildRankHierarchy);
  }

  public CompletableFuture<Optional<Rank>> addRankPermission(long rankId, Permission permission) {
    return builder()
        .query(INSERT_INTO_ADD_PERMISSION)
        .parameter(paramBuilder -> paramBuilder.setLong(permission.id()).setLong(rankId))
        .insert()
        .send(executorSupplier.get())
        .thenCompose(updateResult -> getRank(rankId));
  }

  public CompletableFuture<Optional<Rank>> removeRankPermission(
      long rankId, Permission permission) {
    return builder()
        .query(DELETE_RANK_PERMISSION)
        .parameter(paramBuilder -> paramBuilder.setLong(permission.id()).setLong(rankId))
        .delete()
        .send(executorSupplier.get())
        .thenCompose(updateResult -> getRank(rankId));
  }

  public CompletableFuture<Boolean> deleteRank(long rankId) {
    return builder()
        .query(DELETE_RANK)
        .parameter(paramBuilder -> paramBuilder.setLong(rankId))
        .delete()
        .send(executorSupplier.get())
        .thenApply(UpdateResult::changed);
  }

  private static SelectRecord readSelectRecordOfRow(Row row) throws SQLException {
    int id = row.getInt("rank_id");
    String name = row.getString("rank_name");
    int priority = row.getInt("rank_priority");
    String prefixString = row.getString("rank_prefix");
    Long baseRankId = row.getLong("base_rank_id");
    long permissionId = row.getLong("permission_id");
    String permissionString = row.getString("permission_string");
    String permissionDescription = row.getString("permission_description");

    return new SelectRecord(
        id,
        name,
        priority,
        prefixString,
        baseRankId,
        permissionId,
        permissionString,
        permissionDescription);
  }

  private static Optional<Rank> buildRankHierarchy(List<SelectRecord> records) {
    Map<Long, List<SelectRecord>> groupedByRankId =
        records.stream().collect(Collectors.groupingBy(SelectRecord::id));

    Map<Long, Rank> rankMap = new HashMap<>();

    for (Long rankId : groupedByRankId.keySet()) {
      createRank(rankId, groupedByRankId, rankMap);
    }

    return rankMap.values().stream()
        .filter(
            rank ->
                rankMap.values().stream()
                    .noneMatch(
                        allRanks ->
                            allRanks.baseRank().isPresent()
                                && allRanks.baseRank().get().id() == rank.id()))
        .findFirst();
  }

  private static Rank createRank(
      Long rankId, Map<Long, List<SelectRecord>> groupedByRankId, Map<Long, Rank> rankMap) {
    if (rankMap.containsKey(rankId)) {
      return rankMap.get(rankId);
    }

    if (!groupedByRankId.containsKey(rankId)) {
      return null;
    }

    List<SelectRecord> recordsForRank = groupedByRankId.get(rankId);

    SelectRecord firstRecord = recordsForRank.get(0);

    List<Permission> permissions =
        recordsForRank.stream()
            .filter(selectRecord -> selectRecord.permissionId > 0)
            .map(
                record ->
                    new Permission(
                        record.permissionId(),
                        record.permissionString(),
                        record.permissionDescription()))
            .collect(Collectors.toList());

    Rank baseRank =
        firstRecord.baseRankId() != null
            ? createRank(firstRecord.baseRankId(), groupedByRankId, rankMap)
            : null;

    Rank rank =
        new Rank(
            firstRecord.id(),
            firstRecord.name(),
            firstRecord.priority(),
            firstRecord.prefixString(),
            permissions,
            baseRank);

    rankMap.put(rankId, rank);

    return rank;
  }

    private record SelectRecord(
      long id,
      String name,
      int priority,
      String prefixString,
      Long baseRankId,
      long permissionId,
      String permissionString,
      String permissionDescription) {}
}
