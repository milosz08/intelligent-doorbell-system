package pl.miloszgilga.ids.db.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import pl.miloszgilga.ids.ContentInitializer;
import pl.miloszgilga.ids.db.dto.UserDetails;

public interface SessionMapper extends ContentInitializer {
    @Update("""
            CREATE TABLE IF NOT EXISTS `_sessions` (
                sessionId TEXT PRIMARY KEY NOT NULL,
                expiredAtUtc BIGINT NOT NULL,
                userId BIGINT NOT NULL,
                FOREIGN KEY(userId) REFERENCES `_users`(id) ON DELETE CASCADE
            );
            """)
    void initTable();

    @Insert("""
            INSERT INTO `_sessions` (sessionId, expiredAtUtc, userId)
            VALUES (#{sessionId}, #{expiredAtUtc}, #{userId})
            """)
    int createSession(@Param("sessionId") String sessionId, @Param("expiredAtUtc") long expiredAtUtc,
            @Param("userId") long userId);

    @Select("""
            SELECT u.id, u.username, u.isActive, u.permissionsMask, u.isSystemAccount
            FROM `_sessions` s
            INNER JOIN `_users` u ON s.userId = u.id
            WHERE s.sessionId = #{sessionId} AND s.expiredAtUtc >= #{now}
            """)
    UserDetails getSession(@Param("sessionId") String sessionId, @Param("now") long now);

    @Update("UPDATE `_sessions` SET expiredAtUtc = #{newExpiresAt} WHERE sessionId = #{sessionId}")
    int updateSessionTime(@Param("sessionId") String sessionId, @Param("newExpiresAt") long newExpiresAt);

    @Delete("DELETE FROM `_sessions` WHERE sessionId = #{sessionId}")
    int destroySession(@Param("sessionId") String sessionId);

    @Delete("DELETE FROM `_sessions` WHERE expiredAtUtc < #{now}")
    int removeExpired(@Param("now") long now);
}
