package org.phonepe.model;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Getter(AccessLevel.NONE)
public class ScoreBoard {
    private static final int MAX_SCORE = 1_000_000_000;
    private static final Comparator<ScoreEntry> SCORE_DESC_USER_ASC =
            (a, b) -> {
                int c = Integer.compare(b.getScore(), a.getScore());
                return c != 0 ? c : a.getUserId().compareTo(b.getUserId());
            };

    private final Map<String, Integer> userToScore = new HashMap<>();
    private final TreeSet<ScoreEntry> ranked = new TreeSet<>(SCORE_DESC_USER_ASC);
    private final ReentrantLock lock = new ReentrantLock();

    public void addScore(String userId, int score) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId required");
        if (score < 0 || score > MAX_SCORE) throw new IllegalArgumentException("score 0 to " + MAX_SCORE);
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

    public List<ScoreEntry> getRankedEntries() {
        lock.lock();
        try {
            return new ArrayList<>(ranked);
        } finally {
            lock.unlock();
        }
    }

    public List<ScoreEntry> getPlayersAbove(String userId, int n) {
        if (n <= 0) return List.of();
        List<ScoreEntry> list = getRankedEntries();
        int idx = indexOf(list, userId);
        if (idx <= 0) return List.of();
        int from = Math.max(0, idx - n);
        return list.subList(from, idx);
    }

    public List<ScoreEntry> getPlayersBelow(String userId, int n) {
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
