package org.phonepe.service;

import org.phonepe.model.Campaign;
import org.phonepe.model.CampaignResult;
import org.phonepe.model.CampaignType;
import org.phonepe.model.Game;
import org.phonepe.model.ScoreEntry;
import org.phonepe.repository.IGameRepository;
import org.phonepe.repository.ILeaderboardScoreRepository;
import org.phonepe.repository.impl.InMemoryGameRepository;
import org.phonepe.repository.impl.InMemoryLeaderboardScoreRepository;

import java.util.List;

public class LeaderboardService implements ILeaderboardService {

    private static final LeaderboardService INSTANCE = new LeaderboardService(
            InMemoryGameRepository.getInstance(),
            InMemoryLeaderboardScoreRepository.getInstance()
    );

    private final IGameRepository repository;
    private final ILeaderboardScoreRepository scoreRepository;

    private LeaderboardService(IGameRepository repository, ILeaderboardScoreRepository scoreRepository) {
        this.repository = repository;
        this.scoreRepository = scoreRepository;
    }

    public static LeaderboardService getInstance() {
        return INSTANCE;
    }

    @Override
    public List<String> getSupportedGames() {
        return repository.getSupportedGameIds();
    }

    @Override
    public String createLeaderboard(String gameId, String gameName, String campaignName, CampaignType type, long startEpochSeconds, long endEpochSeconds) {
        if (gameId == null || gameId.isBlank()) throw new IllegalArgumentException("gameId required");
        if (type == null) throw new IllegalArgumentException("type required");
        Campaign campaign = repository.createCampaign(gameId, gameName, campaignName, type, startEpochSeconds, endEpochSeconds);
        scoreRepository.ensureLeaderboard(campaign.getId());
        return campaign.getId();
    }

    @Override
    public List<ScoreEntry> getLeaderboard(String leaderboardId) {
        if (leaderboardId == null || leaderboardId.isBlank()) throw new IllegalArgumentException("leaderboardId required");
        if (repository.getCampaignById(leaderboardId) == null) throw new IllegalArgumentException("unknown leaderboard: " + leaderboardId);
        return scoreRepository.getRankedEntries(leaderboardId);
    }

    @Override
    public void submitScore(String gameId, String userId, int score, long submittedAtEpochSeconds) {
        if (gameId == null || userId == null) throw new IllegalArgumentException("gameId and userId required");
        if (submittedAtEpochSeconds < 0) throw new IllegalArgumentException("submittedAtEpochSeconds must be >= 0");
        Game game = repository.getGame(gameId);
        if (game == null){
            System.out.println("No Game Found with gameId: "+gameId);
            return;
        }
        List<Campaign> active = game.getActiveCampaigns(submittedAtEpochSeconds);
        for (Campaign campaign : active) {
            scoreRepository.addScore(campaign.getId(), userId, score);
        }
    }

    @Override
    public List<ScoreEntry> listPlayersPrev(String gameId, String leaderboardId, String userId, int nPlayers) {
        getCampaignOrThrow(leaderboardId);
        return scoreRepository.getPlayersAbove(leaderboardId, userId, nPlayers);
    }

    @Override
    public List<ScoreEntry> listPlayersNext(String gameId, String leaderboardId, String userId, int nPlayers) {
        getCampaignOrThrow(leaderboardId);
        return scoreRepository.getPlayersBelow(leaderboardId, userId, nPlayers);
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
