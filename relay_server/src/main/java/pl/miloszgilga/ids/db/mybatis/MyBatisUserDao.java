package pl.miloszgilga.ids.db.mybatis;

import java.util.Collections;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import pl.miloszgilga.ids.db.DbConnectionPool;
import pl.miloszgilga.ids.db.dao.UserDao;
import pl.miloszgilga.ids.db.dto.UserDetails;
import pl.miloszgilga.ids.db.mapper.UserMapper;

public class MyBatisUserDao extends MyBatisBaseDao implements UserDao {
    public MyBatisUserDao(DbConnectionPool dbConnectionPool) {
        super(dbConnectionPool);
    }

    @Override
    protected void executeInitTable(SqlSession session) {
        session.getMapper(UserMapper.class).initTable();
    }

    @Override
    protected String getTableName() {
        return "_users";
    }

    @Override
    public List<UserDetails> getUsers() {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession()) {
            final List<UserDetails> users = session.getMapper(UserMapper.class).getUsers();
            log.debug("Successfully fetched {} user(s)", users.size());
            return users;
        } catch (Exception ex) {
            log.error("Unable to get all users from table: _users, cause: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public UserDetails getUserDetails(long userId) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession()) {
            final UserDetails user = session.getMapper(UserMapper.class).getUserDetails(userId);
            if (user != null) {
                log.debug("Found user details with id: {}", userId);
            }
            return user;
        } catch (Exception ex) {
            log.error("Unable to get user details with id: {}, cause: {}", userId, ex.getMessage());
            return null;
        }
    }

    @Override
    public String getUserPasswordHash(String username) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession()) {
            final String hash = session.getMapper(UserMapper.class).getUserPasswordHash(username);
            if (hash != null) {
                log.debug("Found password hash for user: {}", username);
            }
            return hash;
        } catch (Exception ex) {
            log.error("Unable to get password hash from user with username: {}, cause: {}", username,
                    ex.getMessage());
            return null;
        }
    }

    @Override
    public Long getUserId(String username) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession()) {
            final Long id = session.getMapper(UserMapper.class).getUserId(username);
            if (id != null) {
                log.debug("Found id for user: {}", username);
            }
            return id;
        } catch (Exception ex) {
            log.error("Unable to get user id from user with username: {}, cause: {}", username,
                    ex.getMessage());
            return null;
        }
    }

    @Override
    public Boolean userExists(String username) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession()) {
            final boolean exists = session.getMapper(UserMapper.class).userExists(username);
            log.debug("User '{}' exists: {}", username, exists);
            return exists;
        } catch (Exception ex) {
            log.error("Error checking if user exists: {}, cause: {}", username, ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean createUser(String username, String hashedDefaultPassword, boolean isActive, long permissionsMask,
            boolean isSystemAccount) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession(true)) {
            final int affectedRows = session.getMapper(UserMapper.class).createUser(username,
                    hashedDefaultPassword, isActive, permissionsMask, isSystemAccount);
            if (affectedRows > 0) {
                log.info("Created user with username: {}", username);
                return true;
            }
        } catch (Exception ex) {
            log.error("Unable to create user with username: {}, cause: {}", username, ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean updateUserPassword(String username, String newHashedPassword, boolean defaultPassword) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession(true)) {
            final int affectedRows = session.getMapper(UserMapper.class).updateUserPassword(username,
                    newHashedPassword, defaultPassword);
            if (affectedRows > 0) {
                log.info("Updated password for user: {}", username);
                return true;
            } else {
                log.warn("Password update failed - user not found: {}", username);
            }
        } catch (Exception ex) {
            log.error("Unable to update password for user with username: {}, cause: {}", username,
                    ex.getMessage());
        }
        return false;
    }

    @Override
    public Boolean userHasDefaultPassword(String username) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession()) {
            final Boolean isDefault = session.getMapper(UserMapper.class).userHasDefaultPassword(username);
            if (isDefault != null) {
                log.debug("User '{}' default password status: {}", username, isDefault);
            }
            return isDefault;
        } catch (Exception ex) {
            log.error("Unable to get default password info from user with username: {}, cause: {}", username,
                    ex.getMessage());
            return null;
        }
    }

    @Override
    public boolean setPermissions(long userId, long permissionsMask) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession(true)) {
            final int affectedRows = session.getMapper(UserMapper.class).setPermissions(userId, permissionsMask);
            if (affectedRows > 0) {
                log.info("Successfully set permissions mask {} from user with id {}", permissionsMask, userId);
                return true;
            } else {
                log.warn("Permissions set failed - user with id {} not found", userId);
            }
        } catch (Exception ex) {
            log.error("Unable to set permissions from user with id: {}, cause: {}", userId, ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean setAccountState(long userId, boolean isActive) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession(true)) {
            final int affectedRows = session.getMapper(UserMapper.class).setAccountState(userId, isActive);
            if (affectedRows > 0) {
                log.info("Successfully set active state {} from user with id {}", isActive, userId);
                return true;
            } else {
                log.warn("Active state set failed - user with id {} not found", userId);
            }
        } catch (Exception ex) {
            log.error("Unable to set active state from user with id: {}, cause: {}", userId, ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean setDoNotCheckPassword(long userId, boolean doNotCheckPassword) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession(true)) {
            final int affectedRows = session.getMapper(UserMapper.class)
                    .setDoNotCheckPassword(userId, doNotCheckPassword);
            if (affectedRows > 0) {
                log.info("Successfully set do not check password {} from user with id {}", doNotCheckPassword,
                        userId);
                return true;
            } else {
                log.warn("Do not check password set failed - user with id {} not found", userId);
            }
        } catch (Exception ex) {
            log.error("Unable to set do not check password from user with id: {}, cause: {}", userId,
                    ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean deleteUser(long userId) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession(true)) {
            final int affectedRows = session.getMapper(UserMapper.class).deleteUser(userId);
            if (affectedRows == 1) {
                log.warn("Deleted user with id {} from table: _users", userId);
                return true;
            }
        } catch (Exception ex) {
            log.error("Unable to delete user with id: from table: _users, cause: {}", userId, ex.getMessage());
        }
        return false;
    }

    @Override
    public void deleteUsers(long permissionsMask) {
        try (final SqlSession session = dbConnectionPool.getSqlSessionFactory().openSession(true)) {
            final int affectedRows = session.getMapper(UserMapper.class).deleteUsers(permissionsMask);
            log.warn("Deleted all users ({}) from table: _users", affectedRows);
        } catch (Exception ex) {
            log.error("Unable to delete all users table: _users, cause: {}", ex.getMessage());
        }
    }
}
