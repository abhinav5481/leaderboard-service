package org.phonepe.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class Campaign {
    private final String id;
    private final String name;
    private final CampaignType type;
    private final String gameId;
    private final long startEpochSeconds;
    private final long endEpochSeconds;
    private final ScoreBoard scoreBoard;
    @Setter
    private volatile CampaignResult result;

    public Campaign(String id, String name, CampaignType type, String gameId, long startEpochSeconds, long endEpochSeconds) {
        if (id == null || gameId == null) throw new IllegalArgumentException("id and gameId required");
        if (startEpochSeconds < 0 || endEpochSeconds < startEpochSeconds)
            throw new IllegalArgumentException("invalid time range");
        if (type == null) throw new IllegalArgumentException("type required");
        this.id = id;
        this.name = name != null ? name : "";
        this.type = type;
        this.gameId = gameId;
        this.startEpochSeconds = startEpochSeconds;
        this.endEpochSeconds = endEpochSeconds;
        this.scoreBoard = new ScoreBoard();
    }

    public boolean isActiveAt(long epochSeconds) {
        return epochSeconds >= startEpochSeconds && epochSeconds <= endEpochSeconds;
    }

    public boolean isExpired(long epochSeconds) {
        return epochSeconds > endEpochSeconds;
    }

    public void submitScore(String userId, int score) {
        scoreBoard.addScore(userId, score);
    }

    public List<ScoreEntry> getRankedEntries() {
        return scoreBoard.getRankedEntries();
    }

    public List<ScoreEntry> getPlayersAbove(String userId, int n) {
        return scoreBoard.getPlayersAbove(userId, n);
    }

    public List<ScoreEntry> getPlayersBelow(String userId, int n) {
        return scoreBoard.getPlayersBelow(userId, n);
    }
}
