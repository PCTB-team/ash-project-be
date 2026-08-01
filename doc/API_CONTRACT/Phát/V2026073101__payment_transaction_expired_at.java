package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Locale;

public class V2026073101__payment_transaction_expired_at extends BaseJavaMigration {
    private static final String TABLE_NAME = "payment_transaction";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!columnExists(connection, "expired_at")) {
            execute(connection, "alter table " + TABLE_NAME + " add column expired_at datetime(6) null");
        }
        backfillPendingExpiredAt(connection);
        if (!indexExists(connection, "idx_payment_transaction_status_expired_at")) {
            execute(connection, "create index idx_payment_transaction_status_expired_at on "
                    + TABLE_NAME + " (status, expired_at)");
        }
    }

    private void backfillPendingExpiredAt(Connection connection) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("""
                select id, created_at
                from payment_transaction
                where expired_at is null
                  and status = 'PENDING'
                  and created_at is not null
                """);
             ResultSet rows = select.executeQuery();
             PreparedStatement update = connection.prepareStatement("""
                     update payment_transaction
                     set expired_at = ?
                     where id = ?
                     """)) {
            while (rows.next()) {
                Timestamp createdAt = rows.getTimestamp("created_at");
                if (createdAt == null) {
                    continue;
                }
                LocalDateTime expiredAt = createdAt.toLocalDateTime().plusSeconds(30);
                update.setTimestamp(1, Timestamp.valueOf(expiredAt));
                update.setString(2, rows.getString("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private boolean columnExists(Connection connection, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, null, null)) {
            while (columns.next()) {
                if (equalsIgnoreCase(TABLE_NAME, columns.getString("TABLE_NAME"))
                        && equalsIgnoreCase(columnName, columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean indexExists(Connection connection, String indexName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), null, TABLE_NAME, false, false)) {
            while (indexes.next()) {
                if (equalsIgnoreCase(indexName, indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private boolean equalsIgnoreCase(String expected, String actual) {
        return actual != null && expected.toUpperCase(Locale.ROOT).equals(actual.toUpperCase(Locale.ROOT));
    }
}
