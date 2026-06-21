package pl.miloszgilga.ids.db.mybatis;

import java.sql.SQLException;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.miloszgilga.ids.ContentInitializer;
import pl.miloszgilga.ids.db.DbConnectionPool;

abstract class MyBatisBaseDao implements ContentInitializer {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final DbConnectionPool dbConnectionPool;

    protected MyBatisBaseDao(DbConnectionPool dbConnectionPool) {
        this.dbConnectionPool = dbConnectionPool;
    }

    @Override
    public final void init() {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession(true)) {
            executeInitTable(session);
            log.info("Init table (or skip): {}", getTableName());
        } catch (SQLException ex) {
            log.error("Failed to retrieve SqlSessionFactory: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to execute init script for table '{}': {}", getTableName(), ex.getMessage());
        }
    }

    protected abstract void executeInitTable(SqlSession session);

    protected abstract String getTableName();
}
