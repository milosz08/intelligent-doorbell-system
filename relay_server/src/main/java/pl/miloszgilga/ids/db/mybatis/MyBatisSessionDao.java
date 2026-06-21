package pl.miloszgilga.ids.db.mybatis;

import java.time.Instant;

import org.apache.ibatis.session.SqlSession;
import pl.miloszgilga.ids.db.DbConnectionPool;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.db.mapper.SessionMapper;

public class MyBatisSessionDao extends MyBatisBaseDao implements SessionDao {
    public MyBatisSessionDao(DbConnectionPool dbConnectionPool) {
        super(dbConnectionPool);
    }

    @Override
    protected void executeInitTable(SqlSession session) {
        session.getMapper(SessionMapper.class).initTable();
    }

    @Override
    protected String getTableName() {
        return "_sessions";
    }

    @Override
    public boolean createSession(String sessionId, long userId, Instant expiresAt) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession(true)) {
            final int affectedRows = session.getMapper(SessionMapper.class)
                    .createSession(sessionId, expiresAt.toEpochMilli(), userId);
            if (affectedRows > 0) {
                log.info("Created session successfully for user ID: {}", userId);
                return true;
            }
        } catch (Exception ex) {
            log.error("Unable to create session for user: {}, cause: {}", userId, ex.getMessage());
        }
        return false;
    }

    @Override
    public UserDetails getSession(String sessionId) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession()) {
            final UserDetails user = session.getMapper(SessionMapper.class)
                    .getSession(sessionId, Instant.now().toEpochMilli());
            if (user != null) {
                log.debug("Session is VALID, user: '{}', permissions mask: {}", user.username(),
                        user.permissionsMask());
                return user;
            }
        } catch (Exception ex) {
            log.error("Unable to get session with id: {}, cause: {}", sessionId, ex.getMessage());
        }
        return null;
    }

    @Override
    public boolean updateSessionTime(String sessionId, Instant newExpiresAt) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession(true)) {
            final int affectedRows = session.getMapper(SessionMapper.class)
                    .updateSessionTime(sessionId, newExpiresAt.toEpochMilli());
            if (affectedRows > 0) {
                log.debug("Successfully extended session time for ID: {}", sessionId);
                return true;
            } else {
                log.debug("Failed to extend session time, session ID: {} might not exist", sessionId);
            }
        } catch (Exception ex) {
            log.error("Unable to update session time for session with id: {}, cause: {}", sessionId,
                    ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean destroySession(String sessionId) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession(true)) {
            final int affectedRows = session.getMapper(SessionMapper.class).destroySession(sessionId);
            if (affectedRows > 0) {
                log.info("Destroyed session with ID: {}", sessionId);
            } else {
                log.debug("Session destroy skipped - session ID: {} was not found.", sessionId);
            }
            return true;
        } catch (Exception ex) {
            log.error("Unable to destroy session with session id: {}, cause: {}", sessionId, ex.getMessage());
        }
        return false;
    }

    @Override
    public void removeExpired() {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession(true)) {
            final int affectedRows = session.getMapper(SessionMapper.class)
                    .removeExpired(Instant.now().toEpochMilli());
            if (affectedRows > 0) {
                log.info("Cleanup completed, removed {} expired session(s)", affectedRows);
            } else {
                log.debug("Cleanup completed, no expired sessions found.");
            }
        } catch (Exception ex) {
            log.error("Failed to execute cleanup for expired sessions, cause: {}", ex.getMessage());
        }
    }
}
