package pl.miloszgilga.ids.db.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.miloszgilga.ids.db.DbConnectionPool;
import pl.miloszgilga.ids.db.dao.UserDao;
import pl.miloszgilga.ids.db.dto.UserDetails;

public class JdbcUserDao implements UserDao {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcUserDao.class);
    public static final String TABLE_NAME = "_users";

    private final DbConnectionPool dbConnectionPool;

    public JdbcUserDao(DbConnectionPool dbConnectionPool) {
        this.dbConnectionPool = dbConnectionPool;
    }

    @Override
    public void init() {
        final String sql = String.format("""
                CREATE TABLE IF NOT EXISTS `%s` (
                    id INTEGER PRIMARY KEY,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    defaultPassword INTEGER NOT NULL DEFAULT 1,
                    doNotCheckPassword INTEGER NOT NULL DEFAULT 1,
                    permissionsMask BIGINT NOT NULL DEFAULT 0,
                    isSystemAccount INTEGER NOT NULL DEFAULT 0
                );
                """, TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final Statement statement = conn.createStatement()) {
            statement.execute(sql);
            LOG.info("Init table (or skip): {}", TABLE_NAME);
        } catch (SQLException ex) {
            LOG.error("Unable to create table: {}, cause: {}", TABLE_NAME, ex.getMessage());
        }
    }

    @Override
    public List<UserDetails> getUsers() {
        final List<UserDetails> users = new ArrayList<>();
        final String sql = String.format("""
                SELECT id, username, isActive, permissionsMask, isSystemAccount FROM `%s`;
                """, TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql);
                final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                final UserDetails user = new UserDetails(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getBoolean("isActive"),
                        rs.getLong("permissionsMask"),
                        rs.getBoolean("isSystemAccount"));
                users.add(user);
            }
            LOG.debug("Successfully fetched {} user(s)", users.size());
        } catch (SQLException ex) {
            LOG.error("Unable to get all users from table: {}, cause: {}", TABLE_NAME, ex.getMessage());
        }
        return users;
    }

    @Override
    public UserDetails getUserDetails(long userId) {
        final String sql = String.format("""
                SELECT id, username, isActive, permissionsMask, isSystemAccount FROM `%s` WHERE id = ?;
                """, TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LOG.debug("Found user details with id: {}", userId);
                    return new UserDetails(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getBoolean("isActive"),
                            rs.getLong("permissionsMask"),
                            rs.getBoolean("isSystemAccount"));
                }
            }
        } catch (SQLException ex) {
            LOG.error("Unable to get user details with id: {}, cause: {}", userId, ex.getMessage());
        }
        return null;
    }

    @Override
    public String getUserPasswordHash(String username) {
        final String sql = String.format("SELECT password FROM `%s` WHERE username = ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LOG.debug("Found password hash for user: {}", username);
                    return rs.getString(1);
                }
            }
        } catch (SQLException ex) {
            LOG.error("Unable to get password hash from user with username: {}, cause: {}", username,
                    ex.getMessage());
        }
        return null;
    }

    @Override
    public Long getUserId(String username) {
        final String sql = String.format("SELECT id FROM `%s` WHERE username = ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LOG.debug("Found id for user: {}", username);
                    return rs.getLong(1);
                }
            }
        } catch (SQLException ex) {
            LOG.error("Unable to get user id from user with username: {}, cause: {}", username, ex.getMessage());
        }
        return null;
    }

    @Override
    public Boolean userExists(String username) {
        final String sql = String.format("SELECT COUNT(*) > 0 FROM `%s` WHERE username = ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final boolean exists = rs.getBoolean(1);
                    LOG.debug("User '{}' exists: {}", username, exists);
                    return exists;
                }
            }
        } catch (SQLException ex) {
            LOG.error("Error checking if user exists: {}, cause: {}", username, ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean createUser(String username, String hashedDefaultPassword, boolean isActive, long permissionsMask,
            boolean isSystemAccount) {
        final String sql = String.format("""
                  INSERT INTO `%s` (username, password, isActive, permissionsMask, isSystemAccount) VALUES (?,?,?,?,?);
                """, TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashedDefaultPassword);
            ps.setBoolean(3, isActive);
            ps.setLong(4, permissionsMask);
            ps.setBoolean(5, isSystemAccount);
            final int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                LOG.info("Created user with username: {}", username);
                return true;
            }
        } catch (SQLException ex) {
            LOG.error("Unable to create user with username: {}, cause: {}", username, ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean updateUserPassword(String username, String newHashedPassword, boolean defaultPassword) {
        final String sql = String.format(
                "UPDATE `%s` SET password = ?, defaultPassword = ? WHERE username = ?;",
                TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHashedPassword);
            ps.setBoolean(2, defaultPassword);
            ps.setString(3, username);
            final int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                LOG.info("Updated password for user: {}", username);
                return true;
            } else {
                LOG.warn("Password update failed - user not found: {}", username);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to update password for user with username: {}, cause: {}", username,
                    ex.getMessage());
        }
        return false;
    }

    @Override
    public Boolean userHasDefaultPassword(String username) {
        final String sql = String.format("""
                SELECT defaultPassword FROM `%s` WHERE username = ?;
                """, TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final boolean isDefault = rs.getBoolean(1);
                    LOG.debug("User '{}' default password status: {}", username, isDefault);
                    return isDefault;
                }
            }
        } catch (SQLException ex) {
            LOG.error("Unable to get default password info from user with username: {}, cause: {}", username,
                    ex.getMessage());
        }
        return null;
    }

    @Override
    public boolean setPermissions(long userId, long permissionsMask) {
        final String sql = String.format("UPDATE `%s` SET permissionsMask = ? WHERE id = ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, permissionsMask);
            ps.setLong(2, userId);
            final int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                LOG.info("Successfully set permissions mask {} from user with id {}", permissionsMask, userId);
                return true;
            } else {
                LOG.warn("Permissions set failed - user with id {} not found", userId);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to set permissions from user with id: {}, cause: {}", userId, ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean setAccountState(long userId, boolean isActive) {
        final String sql = String.format("UPDATE `%s` SET isActive = ? WHERE id = ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isActive);
            ps.setLong(2, userId);
            final int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                LOG.info("Successfully set active state {} from user with id {}", isActive, userId);
                return true;
            } else {
                LOG.warn("Active state set failed - user with id {} not found", userId);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to set active state from user with id: {}, cause: {}", userId, ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean setDoNotCheckPassword(long userId, boolean doNotCheckPassword) {
        final String sql = String.format("UPDATE `%s` SET doNotCheckPassword = ? WHERE id = ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, doNotCheckPassword);
            ps.setLong(2, userId);
            final int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                LOG.info("Successfully set do not check password {} from user with id {}", doNotCheckPassword,
                        userId);
                return true;
            } else {
                LOG.warn("Do not check password set failed - user with id {} not found", userId);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to set do not check password from user with id: {}, cause: {}", userId,
                    ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean deleteUser(long userId) {
        final String sql = String.format("DELETE FROM `%s` WHERE id = ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            final int affectedRows = ps.executeUpdate();
            if (affectedRows == 1) {
                LOG.warn("Deleted user with id {} from table: {}", userId, TABLE_NAME);
                return true;
            }
        } catch (SQLException ex) {
            LOG.error("Unable to delete user with id: from table: {}, cause: {}", userId, TABLE_NAME,
                    ex.getMessage());
        }
        return false;
    }

    @Override
    public void deleteUsers(long permissionsMask) {
        final String sql = String.format("DELETE FROM `%s` WHERE permissionsMask = ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, permissionsMask);
            final int affectedRows = ps.executeUpdate();
            LOG.warn("Deleted all users ({}) from table: {}", affectedRows, TABLE_NAME);
        } catch (SQLException ex) {
            LOG.error("Unable to delete all users table: {}, cause: {}", TABLE_NAME, ex.getMessage());
        }
    }
}
