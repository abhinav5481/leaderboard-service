package org.phonepe.service;

import org.phonepe.model.CampaignResult;
import org.phonepe.model.CampaignType;
import org.phonepe.model.ScoreEntry;

import java.util.List;

public interface ILeaderboardService {

    List<String> getSupportedGames();

    String createLeaderboard(String gameId, String gameName, String campaignName, CampaignType type, long startEpochSeconds, long endEpochSeconds);

    List<ScoreEntry> getLeaderboard(String leaderboardId);

    void submitScore(String gameId, String userId, int score, long submittedAtEpochSeconds);

    List<ScoreEntry> listPlayersPrev(String gameId, String leaderboardId, String userId, int nPlayers);

    List<ScoreEntry> listPlayersNext(String gameId, String leaderboardId, String userId, int nPlayers);

    CampaignResult getCampaignResult(String leaderboardId);

    void markRewardDisbursed(String leaderboardId);

    void markRewardFailed(String leaderboardId);
}
