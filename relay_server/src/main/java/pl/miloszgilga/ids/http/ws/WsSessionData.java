package pl.miloszgilga.ids.http.ws;

import org.eclipse.jetty.websocket.api.Session;

import pl.miloszgilga.ids.db.dto.UserDetails;

public record WsSessionData(String sessionId, Session session, UserDetails user) {
}
