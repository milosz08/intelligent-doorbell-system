package pl.miloszgilga.ids.http.ws.op;

import java.util.HashMap;
import java.util.Map;

public interface OpCode {
    static Map<Integer, OpCode> LOOKUP = new HashMap<>();

    int getCode();

    static void registerAll(OpCode[] codes) {
        for (final OpCode op : codes) {
            LOOKUP.put(op.getCode(), op);
        }
    }

    // category: (0xXXXX0000), action: (0x0000XXXX)
    static int combine(int category, int action) {
        return (category << 16) | (action & 0xFFFF);
    }

    static OpCode fromInt(int code, OpCode fallback) {
        return LOOKUP.getOrDefault(code, fallback);
    }

    default int getCategory() {
        return (getCode() >> 16) & 0xFFFF;
    }

    default int getActionCode() {
        return getCode() & 0xFFFF;
    }
}
