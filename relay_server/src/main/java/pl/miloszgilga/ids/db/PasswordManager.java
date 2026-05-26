package pl.miloszgilga.ids.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.favre.lib.crypto.bcrypt.BCrypt;
import pl.miloszgilga.ids.ContentInitializer;
import pl.miloszgilga.ids.Utils;
import pl.miloszgilga.ids.db.dao.UserDao;
import pl.miloszgilga.ids.security.Permission;

public class PasswordManager implements ContentInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordManager.class);

    private final UserDao userDao;
    private final String username;
    private final int passwordLength;
    private final int hashStrength;

    private PasswordManager(Builder builder) {
        userDao = builder.userDao;
        username = builder.username;
        passwordLength = builder.passwordLength;
        hashStrength = builder.hashStrength;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void init() {
        final Boolean userExists = userDao.userExists(username);
        if (userExists == null) {
            LOG.error("Failed to check if admin user exists (returned null)");
            return;
        }
        if (!userExists) {
            LOG.debug("Admin user '{}' does not exist, creating default setup", username);
            userDao.deleteUsers(Permission.ADMIN.getBit());

            final String password = Utils.generateSecurePassword(passwordLength);
            final String passwordHash = hash(password);

            userDao.createUser(username, passwordHash, Permission.ADMIN.getBit());
            printLoginDetails(password);
            return;
        }
        LOG.debug("Admin user '{}' exists, checking if default password is in use", username);
        final Boolean hasDefaultPassword = userDao.userHasDefaultPassword(username);
        if (hasDefaultPassword == null) {
            LOG.error("Failed to check default password status (returned null)");
            return;
        }
        if (!hasDefaultPassword) {
            LOG.debug("Admin user is NOT using a default password, setup skipped");
            return;
        }
        LOG.debug("Admin user is using default password, regenerating");
        final String password = Utils.generateSecurePassword(passwordLength);
        userDao.updateUserPassword(username, hash(password), true);
        printLoginDetails(password);
    }

    public String hash(String rawPassword) {
        return BCrypt.withDefaults().hashToString(hashStrength, rawPassword.toCharArray());
    }

    public boolean verify(String username, String incomingPassword) {
        if (!Objects.equals(this.username, username)) {
            return false;
        }
        final String passwordHash = userDao.getUserPasswordHash(username);
        if (passwordHash == null) {
            LOG.debug("Verification failed: password hash not found in DB for user '{}'", username);
            return false;
        }
        final BCrypt.Result result = BCrypt.verifyer().verify(incomingPassword.toCharArray(),
                passwordHash.toCharArray());
        LOG.debug("Verification result for user '{}': {}", username, result.verified);
        return result.verified;
    }

    private void printLoginDetails(String rawPassword) {
        System.out.println("\n\tadmin username: " + username);
        System.out.println("\tadmin password: " + rawPassword + "\n");
    }

    public static class Builder {
        private UserDao userDao;
        private String username;
        private int passwordLength;
        private int hashStrength;

        private Builder() {
        }

        public Builder userDao(UserDao userDao) {
            this.userDao = userDao;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder passwordLength(int passwordLength) {
            this.passwordLength = passwordLength;
            return this;
        }

        public Builder hashStrength(int hashStrength) {
            this.hashStrength = hashStrength;
            return this;
        }

        public PasswordManager build() {
            return new PasswordManager(this);
        }
    }
}
