package pl.miloszgilga.ids;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private Utils() {
    }

    static int safetyParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    static long safetyParseLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public static void closeQuietly(ThrowingRunnable throwingRunnable) {
        if (throwingRunnable == null) {
            return;
        }
        try {
            throwingRunnable.run();
        } catch (Exception ex) {
            LOG.error("Unable to close resource, cause: " + ex.getMessage(), ex);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        closeQuietly((ThrowingRunnable) () -> closeable.close());
    }
}
