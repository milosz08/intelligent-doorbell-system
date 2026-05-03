package pl.miloszgilga.ids.db.dao;

import java.util.List;

import pl.miloszgilga.ids.ContentInitializer;
import pl.miloszgilga.ids.db.Role;
import pl.miloszgilga.ids.db.dto.UserDetails;

public interface UserDao extends ContentInitializer {
    List<UserDetails> getUsers();

    String getUserPasswordHash(String username);

    Integer getUserId(String username);

    Boolean userExists(String username);

    void createUser(String username, String hashedDefaultPassword, Role role);

    boolean updateUserPassword(String username, String newHashedPassword, boolean defaultPassword);

    Boolean userHasDefaultPassword(String username);

    void deleteUsers(Role role);
}
