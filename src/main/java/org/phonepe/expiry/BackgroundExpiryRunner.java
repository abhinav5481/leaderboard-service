package org.phonepe.expiry;

import org.phonepe.repository.impl.InMemoryGameRepository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundExpiryRunner {

    private static final long DEFAULT_INTERVAL_SECONDS = 60;
    private static volatile BackgroundExpiryRunner INSTANCE;

    private final InMemoryGameRepository repository = InMemoryGameRepository.getInstance();
    private final ExpiryHandler handler = DefaultExpiryHandler.getInstance();
    private final long intervalSeconds;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "leaderboard-expiry");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);

    private BackgroundExpiryRunner(long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public static BackgroundExpiryRunner getInstance() {
        return getInstance(DEFAULT_INTERVAL_SECONDS);
    }

    public static BackgroundExpiryRunner getInstance(long intervalSeconds) {
        if (INSTANCE == null) {
            synchronized (BackgroundExpiryRunner.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BackgroundExpiryRunner(intervalSeconds);
                }
            }
        }
        return INSTANCE;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        scheduler.scheduleWithFixedDelay(
                this::runOnce,
                0,
                intervalSeconds,
                TimeUnit.SECONDS
        );
    }

    public void runOnce() {
        long now = System.currentTimeMillis() / 1000;
        repository.runExpiryCheck(now, handler);
    }
}
