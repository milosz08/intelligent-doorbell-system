package pl.miloszgilga.ids.db;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import pl.miloszgilga.ids.ComponentLifecycle;

public class DbConnectionPool implements ComponentLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(DbConnectionPool.class);

    private final HikariConfig config;
    private final Function<HikariDataSource, Configuration> myBatisConfig;

    private HikariDataSource dataSource;
    private SqlSessionFactory sqlSessionFactory;

    private DbConnectionPool(Builder builder) {
        LOG.debug("Configuring database connection pool for DB: {}", builder.dbName);
        createMissingDir(builder.dbName);
        config = initHikariDataSource(builder);
        myBatisConfig = initMyBatisMapper(builder);
        LOG.info("Database connection pool configuration phase completed");
    }

    private HikariConfig initHikariDataSource(Builder builder) {
        LOG.trace("Initializing HikariConfig properties");
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + builder.dbName);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.setMaximumPoolSize(builder.maximumPoolSize);
        LOG.debug("HikariConfig initialized, max pool size: {}, URL: {}", builder.maximumPoolSize,
                config.getJdbcUrl());
        return config;
    }

    private Function<HikariDataSource, Configuration> initMyBatisMapper(Builder builder) {
        return (dataSource) -> {
            LOG.debug("Executing MyBatis configuration (injecting Hikari DataSource)");
            final Environment environment = new Environment(
                    "production",
                    new JdbcTransactionFactory(),
                    dataSource);
            final Configuration myBatisConfig = new Configuration(environment);
            myBatisConfig.setMapUnderscoreToCamelCase(true);
            LOG.trace("Registering {} MyBatis mappers", builder.myBatisMappers.size());
            for (final Class<?> mapperClass : builder.myBatisMappers) {
                myBatisConfig.addMapper(mapperClass);
                LOG.trace("Successfully registered mapper: {}", mapperClass.getSimpleName());
            }
            LOG.debug("MyBatis configuration successfully built");
            return myBatisConfig;
        };
    }

    @Override
    public void init() {
        dataSource = new HikariDataSource(config);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(myBatisConfig.apply(dataSource));
    }

    public static Builder builder() {
        return new Builder();
    }

    private void createMissingDir(String dbName) {
        final File dbFile = new File(dbName);
        final File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            LOG.debug("Directory {} does not exist, attempting to create...", parentDir.getPath());
            if (parentDir.mkdirs()) {
                LOG.info("Created missing directory structure: {}", parentDir.getPath());
            } else {
                LOG.error("Failed to create directory structure: {}", parentDir.getPath());
            }
        } else if (parentDir != null) {
            LOG.debug("Directory {} already exists", parentDir.getPath());
        }
    }

    public SqlSessionFactory getSqlSessionFactory() throws SQLException {
        return sqlSessionFactory;
    }

    @Override
    public void close() throws IOException {
        dataSource.close();
    }

    public static class Builder {
        private String dbName;
        private int maximumPoolSize;
        private List<Class<?>> myBatisMappers = new ArrayList<>();

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

        public Builder addMyBatisMapperClass(Class<?> mapperClass) {
            myBatisMappers.add(mapperClass);
            return this;
        }

        public DbConnectionPool build() {
            return new DbConnectionPool(this);
        }
    }
}
