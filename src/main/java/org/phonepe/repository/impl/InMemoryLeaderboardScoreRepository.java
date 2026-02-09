package org.phonepe.repository.impl;

import org.phonepe.model.ScoreEntry;
import org.phonepe.repository.ILeaderboardScoreRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReentrantLock;

public class InMemoryLeaderboardScoreRepository implements ILeaderboardScoreRepository {

    private static final InMemoryLeaderboardScoreRepository INSTANCE = new InMemoryLeaderboardScoreRepository();

    private final Map<String, LeaderboardScoreStore> storeByLeaderboardId = new ConcurrentHashMap<>();

    private InMemoryLeaderboardScoreRepository() {}

    public static InMemoryLeaderboardScoreRepository getInstance() {
        return INSTANCE;
    }

    @Override
    public void ensureLeaderboard(String leaderboardId) {
        storeByLeaderboardId.putIfAbsent(leaderboardId, new LeaderboardScoreStore());
    }

    @Override
    public void addScore(String leaderboardId, String userId, int score) {
        LeaderboardScoreStore store = storeByLeaderboardId.get(leaderboardId);
        if (store == null) return;
        store.addScore(userId, score);
    }

    @Override
    public List<ScoreEntry> getRankedEntries(String leaderboardId) {
        LeaderboardScoreStore store = storeByLeaderboardId.get(leaderboardId);
        return store == null ? List.of() : store.getRankedEntries();
    }

    @Override
    public List<ScoreEntry> getPlayersAbove(String leaderboardId, String userId, int n) {
        LeaderboardScoreStore store = storeByLeaderboardId.get(leaderboardId);
        return store == null ? List.of() : store.getPlayersAbove(userId, n);
    }

    @Override
    public List<ScoreEntry> getPlayersBelow(String leaderboardId, String userId, int n) {
        LeaderboardScoreStore store = storeByLeaderboardId.get(leaderboardId);
        return store == null ? List.of() : store.getPlayersBelow(userId, n);
    }

    private static final class LeaderboardScoreStore {
        private static final int MAX_SCORE = 1_000_000_000;
        private static final int STRIPE_COUNT = 64;
        private static final Comparator<ScoreEntry> SCORE_DESC_USER_ASC =
                (a, b) -> {
                    int c = Integer.compare(b.getScore(), a.getScore());
                    return c != 0 ? c : a.getUserId().compareTo(b.getUserId());
                };

        private final Map<String, Integer> userToScore = new ConcurrentHashMap<>();
        private final ConcurrentSkipListSet<ScoreEntry> ranked = new ConcurrentSkipListSet<>(SCORE_DESC_USER_ASC);
        private final ReentrantLock[] stripes = new ReentrantLock[STRIPE_COUNT];

        LeaderboardScoreStore() {
            for (int i = 0; i < STRIPE_COUNT; i++) {
                stripes[i] = new ReentrantLock();
            }
        }

        private static int stripeIndex(String userId) {
            return Math.abs(userId.hashCode() % STRIPE_COUNT);
        }

        void addScore(String userId, int score) {
            if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId required");
            if (score < 0 || score > MAX_SCORE) throw new IllegalArgumentException("score 0 to " + MAX_SCORE);
            ReentrantLock lock = stripes[stripeIndex(userId)];
            lock.lock();
            try {
                int current = userToScore.getOrDefault(userId, -1);
                if (score <= current) return;
                if (current >= 0) ranked.remove(new ScoreEntry(userId, current));
                userToScore.put(userId, score);
                ranked.add(new ScoreEntry(userId, score));
            } finally {
                lock.unlock();
            }
        }

        List<ScoreEntry> getRankedEntries() {
            return new ArrayList<>(ranked);
        }

        List<ScoreEntry> getPlayersAbove(String userId, int n) {
            if (n <= 0) return List.of();
            List<ScoreEntry> list = getRankedEntries();
            int idx = indexOf(list, userId);
            if (idx <= 0) return List.of();
            int from = Math.max(0, idx - n);
            return list.subList(from, idx);
        }

        List<ScoreEntry> getPlayersBelow(String userId, int n) {
            if (n <= 0) return List.of();
            List<ScoreEntry> list = getRankedEntries();
            int idx = indexOf(list, userId);
            if (idx < 0 || idx >= list.size() - 1) return List.of();
            int to = Math.min(list.size(), idx + 1 + n);
            return list.subList(idx + 1, to);
        }

        private static int indexOf(List<ScoreEntry> list, String userId) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getUserId().equals(userId)) return i;
            }
            return -1;
        }
    }
}
