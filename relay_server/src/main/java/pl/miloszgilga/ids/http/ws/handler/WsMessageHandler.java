package pl.miloszgilga.ids.http.ws.handler;

import com.google.gson.JsonObject;

import pl.miloszgilga.ids.http.ws.op.OpCode;

public interface WsMessageHandler {
    OpCode getOpCode();

    void handle(String sessionId, JsonObject data) throws Exception;
}
