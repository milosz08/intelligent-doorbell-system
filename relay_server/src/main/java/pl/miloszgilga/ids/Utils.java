package pl.miloszgilga.ids;

import java.io.Closeable;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    private static final String ALL_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static final String REGEX_USERNAME = "^[a-z0-9]{3,20}$";
    public static final String REGEX_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,40}$";

    private Utils() {
    }

    public static String generateSecurePassword(int length) {
        final StringBuilder password = new StringBuilder(length);
        final SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < length; i++) {
            final int randomIndex = secureRandom.nextInt(ALL_CHARACTERS.length());
            password.append(ALL_CHARACTERS.charAt(randomIndex));
        }
        return password.toString();
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

    public static boolean isNullOrBlank(String text) {
        return text == null || text.isBlank();
    }
}
