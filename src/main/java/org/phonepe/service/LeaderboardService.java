package org.phonepe.service;

import lombok.RequiredArgsConstructor;
import org.phonepe.model.Campaign;
import org.phonepe.model.CampaignResult;
import org.phonepe.model.CampaignType;
import org.phonepe.model.Game;
import org.phonepe.model.ScoreEntry;
import org.phonepe.repository.IGameRepository;

import java.util.List;

@RequiredArgsConstructor
public class LeaderboardService implements ILeaderboardService {

    private final IGameRepository repository;

    @Override
    public List<String> getSupportedGames() {
        return repository.getSupportedGameIds();
    }

    @Override
    public String createLeaderboard(String gameId, String gameName, String campaignName, CampaignType type, long startEpochSeconds, long endEpochSeconds) {
        if (gameId == null || gameId.isBlank()) throw new IllegalArgumentException("gameId required");
        if (type == null) throw new IllegalArgumentException("type required");
        Campaign campaign = repository.createCampaign(gameId, gameName, campaignName, type, startEpochSeconds, endEpochSeconds);
        return campaign.getId();
    }

    @Override
    public List<ScoreEntry> getLeaderboard(String leaderboardId) {
        if (leaderboardId == null || leaderboardId.isBlank()) throw new IllegalArgumentException("leaderboardId required");
        Campaign campaign = repository.getCampaignById(leaderboardId);
        if (campaign == null) throw new IllegalArgumentException("unknown leaderboard: " + leaderboardId);
        return campaign.getRankedEntries();
    }

    @Override
    public void submitScore(String gameId, String userId, int score, long submittedAtEpochSeconds) {
        if (gameId == null || userId == null) throw new IllegalArgumentException("gameId and userId required");
        if (submittedAtEpochSeconds < 0) throw new IllegalArgumentException("submittedAtEpochSeconds must be >= 0");
        Game game = repository.getOrCreateGame(gameId);
        List<Campaign> active = game.getActiveCampaigns(submittedAtEpochSeconds);
        for (Campaign campaign : active) {
            campaign.submitScore(userId, score);
        }
    }

    @Override
    public List<ScoreEntry> listPlayersPrev(String gameId, String leaderboardId, String userId, int nPlayers) {
        Campaign campaign = getCampaignOrThrow(leaderboardId);
        return campaign.getPlayersAbove(userId, nPlayers);
    }

    @Override
    public List<ScoreEntry> listPlayersNext(String gameId, String leaderboardId, String userId, int nPlayers) {
        Campaign campaign = getCampaignOrThrow(leaderboardId);
        return campaign.getPlayersBelow(userId, nPlayers);
    }

    @Override
    public CampaignResult getCampaignResult(String leaderboardId) {
        Campaign campaign = getCampaignOrThrow(leaderboardId);
        return campaign.getResult();
    }

    @Override
    public void markRewardDisbursed(String leaderboardId) {
        Campaign campaign = getCampaignOrThrow(leaderboardId);
        CampaignResult result = campaign.getResult();
        if (result == null) throw new IllegalStateException("campaign not yet expired: " + leaderboardId);
        result.markDisbursed();
    }

    @Override
    public void markRewardFailed(String leaderboardId) {
        Campaign campaign = getCampaignOrThrow(leaderboardId);
        CampaignResult result = campaign.getResult();
        if (result == null) throw new IllegalStateException("campaign not yet expired: " + leaderboardId);
        result.markFailed();
    }

    private Campaign getCampaignOrThrow(String leaderboardId) {
        Campaign campaign = repository.getCampaignById(leaderboardId);
        if (campaign == null) throw new IllegalArgumentException("unknown leaderboard: " + leaderboardId);
        return campaign;
    }
}
