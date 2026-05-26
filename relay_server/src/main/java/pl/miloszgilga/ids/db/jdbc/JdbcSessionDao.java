package pl.miloszgilga.ids.db.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.miloszgilga.ids.db.DbConnectionPool;
import pl.miloszgilga.ids.db.dao.SessionDao;
import pl.miloszgilga.ids.db.dto.UserDetails;

public class JdbcSessionDao implements SessionDao {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcSessionDao.class);
    private static final String TABLE_NAME = "_sessions";

    private final DbConnectionPool dbConnectionPool;

    public JdbcSessionDao(DbConnectionPool dbConnectionPool) {
        this.dbConnectionPool = dbConnectionPool;
    }

    @Override
    public void init() {
        final String sql = String.format("""
                  CREATE TABLE IF NOT EXISTS `%s` (
                    sessionId TEXT PRIMARY KEY NOT NULL,
                    expiredAtUtc DATETIME NOT NULL,
                    userId INTEGER NOT NULL,
                    FOREIGN KEY(userId) REFERENCES %s(id) ON DELETE CASCADE
                  );
                """, TABLE_NAME, JdbcUserDao.TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final Statement statement = conn.createStatement()) {
            statement.execute(sql);
            LOG.info("Init table (or skip): {}", TABLE_NAME);
        } catch (SQLException ex) {
            LOG.error("Unable to create table: {}, cause: {}", TABLE_NAME, ex.getMessage());
        }
    }

    @Override
    public void createSession(String sessionId, Integer userId, Instant expiresAt) {
        final String sql = String.format("""
                  INSERT INTO `%s` (sessionId, expiredAtUtc, userId) VALUES (?,?,?);
                """, TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setTimestamp(2, Timestamp.from(expiresAt));
            ps.setInt(3, userId);
            final int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                LOG.info("Created session successfully for user ID: {}", userId);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to create session for user: {}, cause: {}", userId, ex.getMessage());
        }
    }

    @Override
    public UserDetails getSession(String sessionId) {
        final Instant now = Instant.now();
        final String sql = String.format("""
                SELECT userId, username, permissionsMask FROM `%s` s
                INNER JOIN `%s` u ON s.userId = u.id WHERE sessionId = ? AND expiredAtUtc >= ?;
                """, TABLE_NAME, JdbcUserDao.TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setTimestamp(2, Timestamp.from(now));
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final UserDetails user = new UserDetails(rs.getInt(1), rs.getString(2), rs.getLong(3));
                    LOG.debug("Session is VALID, user: '{}', permissions mask: {}", user.username(),
                            user.permissionsMask());
                    return user;
                }
            }
        } catch (SQLException ex) {
            LOG.error("Unable to get session with id: {}, cause: {}", sessionId, ex.getMessage());
        }
        return null;
    }

    @Override
    public boolean updateSessionTime(String sessionId, Instant newExpiresAt) {
        final String sql = String.format(
                "UPDATE `%s` SET expiredAtUtc = ? WHERE sessionId = ?;",
                TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(newExpiresAt));
            ps.setString(2, sessionId);
            final int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                LOG.debug("Successfully extended session time for ID: {}", sessionId);
                return true;
            } else {
                LOG.debug("Failed to extend session time, session ID: {} might not exist", sessionId);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to update session time for session with id: {}, cause: {}", sessionId,
                    ex.getMessage());
        }
        return false;
    }

    @Override
    public void destroySession(String sessionId) {
        final String sql = String.format("DELETE FROM `%s` WHERE sessionId = ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            final int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                LOG.info("Destroyed session with ID: {}", sessionId);
            } else {
                LOG.debug("Session destroy skipped - session ID: {} was not found.", sessionId);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to destroy session with session id: {}, cause: {}", sessionId, ex.getMessage());
        }
    }

    @Override
    public void removeExpired() {
        final String sql = String.format("DELETE FROM `%s` WHERE expiredAtUtc < ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            final int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                LOG.info("Cleanup completed, removed {} expired session(s)", affectedRows);
            } else {
                LOG.debug("Cleanup completed, no expired sessions found.");
            }
        } catch (SQLException ex) {
            LOG.error("Failed to execute cleanup for expired sessions, cause: {}", ex.getMessage());
        }
    }
}
