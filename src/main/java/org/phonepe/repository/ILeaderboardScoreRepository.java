package org.phonepe.repository;

import org.phonepe.model.ScoreEntry;

import java.util.List;

public interface ILeaderboardScoreRepository {

    void ensureLeaderboard(String leaderboardId);

    void addScore(String leaderboardId, String userId, int score);

    List<ScoreEntry> getRankedEntries(String leaderboardId);

    List<ScoreEntry> getPlayersAbove(String leaderboardId, String userId, int n);

    List<ScoreEntry> getPlayersBelow(String leaderboardId, String userId, int n);
}
