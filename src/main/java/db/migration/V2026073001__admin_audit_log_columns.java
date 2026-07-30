package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class V2026073001__admin_audit_log_columns extends BaseJavaMigration {
    private static final String TABLE_NAME = "system_log";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        addColumnIfMissing(connection, "actor_id");
        addColumnIfMissing(connection, "actor_type");
        addColumnIfMissing(connection, "action_group");
        createIndexIfMissing(connection, "idx_system_log_actor_type_group_created",
                "actor_type, action_group, created_at");
        createIndexIfMissing(connection, "idx_system_log_actor_id_created",
                "actor_id, created_at");
    }

    private void addColumnIfMissing(Connection connection, String columnName) throws SQLException {
        if (columnExists(connection, columnName)) {
            return;
        }
        execute(connection, "alter table " + TABLE_NAME + " add column " + columnName + " varchar(255) null");
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

    private void createIndexIfMissing(Connection connection, String indexName, String columns) throws SQLException {
        if (indexExists(connection, indexName)) {
            return;
        }
        execute(connection, "create index " + indexName + " on " + TABLE_NAME + " (" + columns + ")");
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
