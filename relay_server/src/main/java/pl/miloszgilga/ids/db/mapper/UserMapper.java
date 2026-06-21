package pl.miloszgilga.ids.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import pl.miloszgilga.ids.ContentInitializer;
import pl.miloszgilga.ids.db.dto.UserDetails;

public interface UserMapper extends ContentInitializer {
    @Update("""
            CREATE TABLE IF NOT EXISTS `_users` (
                id INTEGER PRIMARY KEY,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 1,
                defaultPassword INTEGER NOT NULL DEFAULT 1,
                doNotCheckPassword INTEGER NOT NULL DEFAULT 1,
                permissionsMask BIGINT NOT NULL DEFAULT 0,
                isSystemAccount INTEGER NOT NULL DEFAULT 0
            );
            """)
    void initTable();

    @Select("SELECT id, username, isActive, permissionsMask, isSystemAccount FROM `_users`")
    List<UserDetails> getUsers();

    @Select("SELECT id, username, isActive, permissionsMask, isSystemAccount FROM `_users` WHERE id = #{userId}")
    UserDetails getUserDetails(@Param("userId") long userId);

    @Select("SELECT password FROM `_users` WHERE username = #{username}")
    String getUserPasswordHash(@Param("username") String username);

    // possibly null -> user not found
    @Select("SELECT id FROM `_users` WHERE username = #{username}")
    Long getUserId(@Param("username") String username);

    @Select("SELECT COUNT(*) > 0 FROM `_users` WHERE username = #{username}")
    boolean userExists(@Param("username") String username);

    @Insert("""
            INSERT INTO `_users` (username, password, isActive, permissionsMask, isSystemAccount)
            VALUES (#{username}, #{password}, #{isActive}, #{permissionsMask}, #{isSystemAccount})
            """)
    int createUser(@Param("username") String username, @Param("password") String password,
            @Param("isActive") boolean isActive, @Param("permissionsMask") long permissionsMask,
            @Param("isSystemAccount") boolean isSystemAccount);

    @Update("""
            UPDATE `_users` SET password = #{newPassword}, defaultPassword = #{defaultPassword}
            WHERE username = #{username}
            """)
    int updateUserPassword(@Param("username") String username, @Param("newPassword") String newPassword,
            @Param("defaultPassword") boolean defaultPassword);

    // possibly null -> some error
    @Select("SELECT defaultPassword FROM `_users` WHERE username = #{username}")
    Boolean userHasDefaultPassword(@Param("username") String username);

    @Update("UPDATE `_users` SET permissionsMask = #{permissionsMask} WHERE id = #{userId}")
    int setPermissions(@Param("userId") long userId, @Param("permissionsMask") long permissionsMask);

    @Update("UPDATE `_users` SET isActive = #{isActive} WHERE id = #{userId}")
    int setAccountState(@Param("userId") long userId, @Param("isActive") boolean isActive);

    @Update("UPDATE `_users` SET doNotCheckPassword = #{doNotCheckPassword} WHERE id = #{userId}")
    int setDoNotCheckPassword(@Param("userId") long userId, @Param("doNotCheckPassword") boolean doNotCheckPassword);

    @Delete("DELETE FROM `_users` WHERE id = #{userId}")
    int deleteUser(@Param("userId") long userId);

    @Delete("DELETE FROM `_users` WHERE permissionsMask = #{permissionsMask}")
    int deleteUsers(@Param("permissionsMask") long permissionsMask);
}
