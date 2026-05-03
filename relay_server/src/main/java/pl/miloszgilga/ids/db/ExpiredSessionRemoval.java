package pl.miloszgilga.ids.db;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.miloszgilga.ids.ComponentLifecycle;
import pl.miloszgilga.ids.db.dao.SessionDao;

public class ExpiredSessionRemoval implements ComponentLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(ExpiredSessionRemoval.class);

    private final SessionDao sessionDao;
    private final int intervalSec;
    private final ScheduledExecutorService scheduler;

    private ExpiredSessionRemoval(Builder builder) {
        sessionDao = builder.sessionDao;
        intervalSec = builder.intervalSec;
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void init() {
        scheduler.scheduleAtFixedRate(sessionDao::removeExpired, 0, intervalSec, TimeUnit.SECONDS);
        LOG.info("Started expired session removal executor service");
    }

    @Override
    public void close() throws IOException {
        scheduler.shutdown();
    }

    public static class Builder {
        private SessionDao sessionDao;
        private int intervalSec;

        private Builder() {
        }

        public Builder sessionDao(SessionDao sessionDao) {
            this.sessionDao = sessionDao;
            return this;
        }

        public Builder intervalSec(int intervalSec) {
            this.intervalSec = intervalSec;
            return this;
        }

        public ExpiredSessionRemoval build() {
            return new ExpiredSessionRemoval(this);
        }
    }
}
