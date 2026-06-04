package pl.miloszgilga.ids.db.dao;

import java.time.Instant;

import pl.miloszgilga.ids.ContentInitializer;
import pl.miloszgilga.ids.db.dto.UserDetails;

public interface SessionDao extends ContentInitializer {
    boolean createSession(String sessionId, long userId, Instant expiresAt);

    UserDetails getSession(String sessionId);

    boolean updateSessionTime(String sessionId, Instant newExpiresAt);

    boolean destroySession(String sessionId);

    void removeExpired();
}
