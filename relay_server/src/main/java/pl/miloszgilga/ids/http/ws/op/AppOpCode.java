package pl.miloszgilga.ids.http.ws.op;

public enum AppOpCode implements OpCode {
    HEARTBEAT(1, 1), // 65537
    ENV_STATUS(2, 1), // 131073
    DOORBELL_RING(2, 2), // 131074
    DOORBELL_MODE_SET(2, 2), // 131076
    DOORBELL_MANUALLY_RING(2, 4), // 131075
    ;

    private final int code;

    static {
        OpCode.registerAll(values());
    }

    AppOpCode(int category, int action) {
        code = OpCode.combine(category, action);
    }

    @Override
    public int getCode() {
        return code;
    }
}
