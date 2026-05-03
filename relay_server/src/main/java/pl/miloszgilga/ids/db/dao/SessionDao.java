package pl.miloszgilga.ids.db.dao;

import java.time.Instant;

import pl.miloszgilga.ids.ContentInitializer;
import pl.miloszgilga.ids.db.dto.UserDetails;

public interface SessionDao extends ContentInitializer {
    void createSession(String sessionId, Integer userId, Instant expiresAt);

    UserDetails getSession(String sessionId);

    boolean updateSessionTime(String sessionId, Instant newExpiresAt);

    void destroySession(String sessionId);

    void removeExpired();
}
