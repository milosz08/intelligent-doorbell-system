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
import pl.miloszgilga.ids.db.Role;
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
                    defaultPassword INTEGER NOT NULL DEFAULT 1,
                    role TEXT NOT NULL
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
        final String sql = String.format("SELECT id, username, role FROM `%s`;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql);
                final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                final UserDetails user = new UserDetails(
                        rs.getInt("id"),
                        rs.getString("username"),
                        Role.valueOf(rs.getString("role")));
                users.add(user);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to get all users from table: {}, cause: {}", TABLE_NAME, ex.getMessage());
        }
        return users;
    }

    @Override
    public String getUserPasswordHash(String username) {
        final String sql = String.format("SELECT password FROM `%s` WHERE username = ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
    public Integer getUserId(String username) {
        final String sql = String.format("SELECT id FROM `%s` WHERE username = ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            LOG.error("Unable to get user id from user with username: {}, cause: {}", username,
                    ex.getMessage());
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
                    return rs.getBoolean(1);
                }
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    @Override
    public void createUser(String username, String hashedDefaultPassword, Role role) {
        final String sql = String.format("""
                  INSERT INTO `%s` (username, password, role) VALUES (?,?,?);
                """, TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashedDefaultPassword);
            ps.setString(3, role.name());
            final int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                LOG.info("Created user with username: {}", username);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to create user with username: {}, cause: {}", username, ex.getMessage());
        }
    }

    @Override
    public boolean updateUserPassword(String username, String newHashedPassword, boolean defaultPassword) {
        final String sql = String.format(
                "UPDATE `%s` SET password = ?, defaultPassword = ? WHERE username = ?;",
                TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHashedPassword);
            ps.setInt(2, defaultPassword ? 1 : 0);
            ps.setString(3, username);
            final int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                LOG.info("Updated password for user with username: {}", username);
            }
            return true;
        } catch (SQLException ex) {
            LOG.error("Unable to update password for user with username: {}. Cause: {}",
                    username, ex.getMessage());
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
                return rs.next() ? rs.getBoolean(1) : null;
            }
        } catch (SQLException ex) {
            LOG.error("Unable to get default password info from user with username: {}, cause: {}",
                    username, ex.getMessage());
        }
        return null;
    }

    @Override
    public void deleteUsers(Role role) {
        final String sql = String.format("DELETE FROM `%s` WHERE role = ?;", TABLE_NAME);
        try (final Connection conn = dbConnectionPool.getConnection();
                final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            final int affectedRows = ps.executeUpdate();
            LOG.warn("Deleted all users ({}) from table: {}", affectedRows, TABLE_NAME);
        } catch (SQLException ex) {
            LOG.error("Unable to delete all users table: {}, cause: {}", TABLE_NAME, ex.getMessage());
        }
    }
}
