package pl.miloszgilga.ids.db.dao;

import java.util.List;

import pl.miloszgilga.ids.ContentInitializer;
import pl.miloszgilga.ids.db.dto.UserDetails;

public interface UserDao extends ContentInitializer {
    List<UserDetails> getUsers();

    UserDetails getUserDetails(long userId);

    String getUserPasswordHash(String username);

    // possibly null -> user not found
    Long getUserId(String username);

    Boolean userExists(String username);

    boolean createUser(String username, String hashedDefaultPassword, boolean isActive, long permissionsMask,
            boolean isSystemAccount);

    boolean updateUserPassword(String username, String newHashedPassword, boolean defaultPassword);

    // possibly null -> some error
    Boolean userHasDefaultPassword(String username);

    boolean setPermissions(long userId, long permissionsMask);

    boolean setAccountState(long userId, boolean isActive);

    boolean setDoNotCheckPassword(long userId, boolean doNotCheckPassword);

    boolean deleteUser(long userId);

    void deleteUsers(long permissionsMask);
}
