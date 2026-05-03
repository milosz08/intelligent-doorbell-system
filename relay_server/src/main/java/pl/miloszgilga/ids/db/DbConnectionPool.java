package pl.miloszgilga.ids.db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import pl.miloszgilga.ids.ComponentLifecycle;

public class DbConnectionPool implements ComponentLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(DbConnectionPool.class);

    private final HikariConfig config;

    private HikariDataSource dataSource;

    private DbConnectionPool(Builder builder) {
        createMissingDir(builder.dbName);
        config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + builder.dbName);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(builder.maximumPoolSize);
    }

    @Override
    public void init() {
        dataSource = new HikariDataSource(config);
    }

    public static Builder builder() {
        return new Builder();
    }

    private void createMissingDir(String dbName) {
        final File dbFile = new File(dbName);
        final File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (parentDir.mkdirs()) {
                LOG.info("Created missing directory structure: {}", parentDir.getPath());
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() throws IOException {
        dataSource.close();
    }

    public static class Builder {
        private String dbName;
        private int maximumPoolSize;

        private Builder() {
        }

        public Builder dbName(String dbName) {
            this.dbName = dbName;
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public DbConnectionPool build() {
            return new DbConnectionPool(this);
        }
    }
}
