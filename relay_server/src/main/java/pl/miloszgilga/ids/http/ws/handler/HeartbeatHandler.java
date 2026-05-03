package pl.miloszgilga.ids.http.ws.handler;

import com.google.gson.JsonObject;

import pl.miloszgilga.ids.http.ws.WsSessionRegistry;
import pl.miloszgilga.ids.http.ws.op.AppOpCode;
import pl.miloszgilga.ids.http.ws.op.OpCode;

public class HeartbeatHandler implements WsMessageHandler {
    private final WsSessionRegistry wsSessionRegistry;

    public HeartbeatHandler(WsSessionRegistry wsSessionRegistry) {
        this.wsSessionRegistry = wsSessionRegistry;
    }

    @Override
    public OpCode getOpCode() {
        return AppOpCode.HEARTBEAT;
    }

    @Override
    public void handle(String sessionId, JsonObject data) throws Exception {
        wsSessionRegistry.sendTo(sessionId, AppOpCode.HEARTBEAT);
    }
}
