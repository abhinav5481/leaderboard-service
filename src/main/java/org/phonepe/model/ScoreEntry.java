package org.phonepe.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ScoreEntry {
    private final String userId;
    private final int score;

    public ScoreEntry(String userId, int score) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId required");
        if (score < 0 || score > 1_000_000_000) throw new IllegalArgumentException("score must be 0 to 1e9");
        this.userId = userId;
        this.score = score;
    }
}
