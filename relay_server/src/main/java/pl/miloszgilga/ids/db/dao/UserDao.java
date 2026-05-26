package pl.miloszgilga.ids.db.dao;

import java.util.List;

import pl.miloszgilga.ids.ContentInitializer;
import pl.miloszgilga.ids.db.dto.UserDetails;

public interface UserDao extends ContentInitializer {
    List<UserDetails> getUsers();

    String getUserPasswordHash(String username);

    Integer getUserId(String username);

    Boolean userExists(String username);

    void createUser(String username, String hashedDefaultPassword, long permissionsMask);

    boolean updateUserPassword(String username, String newHashedPassword, boolean defaultPassword);

    Boolean userHasDefaultPassword(String username);

    boolean grantPermission(String username, long permissionBit);

    boolean revokePermission(String username, long permissionBit);

    void deleteUsers(long permissionsMask);
}
