package org.phonepe.service;

import org.phonepe.exception.ErrorCode;
import org.phonepe.exception.LeaderboardException;
import org.phonepe.model.Campaign;
import org.phonepe.model.CampaignResult;
import org.phonepe.model.CampaignType;
import org.phonepe.model.Game;
import org.phonepe.model.ScoreEntry;
import org.phonepe.repository.IGameRepository;
import org.phonepe.repository.ILeaderboardScoreRepository;
import org.phonepe.repository.impl.InMemoryGameRepository;
import org.phonepe.repository.impl.InMemoryLeaderboardScoreRepository;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LeaderboardService implements ILeaderboardService {

    private static final Logger LOG = Logger.getLogger(LeaderboardService.class.getName());

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
        try {
            return repository.getSupportedGameIds();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "getSupportedGames failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String createLeaderboard(String gameId, String gameName, String campaignName, CampaignType type, long startEpochSeconds, long endEpochSeconds) {
        try {
            if (gameId == null || gameId.isBlank()) throw new LeaderboardException(ErrorCode.INVALID_GAME_ID);
            if (type == null) throw new LeaderboardException(ErrorCode.INVALID_CAMPAIGN_TYPE);
            Campaign campaign = repository.createCampaign(gameId, gameName, campaignName, type, startEpochSeconds, endEpochSeconds);
            scoreRepository.ensureLeaderboard(campaign.getId());
            return campaign.getId();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "createLeaderboard failed", e);
            return null;
        }
    }

    @Override
    public List<ScoreEntry> getLeaderboard(String leaderboardId) {
        try {
            if (leaderboardId == null || leaderboardId.isBlank()) throw new LeaderboardException(ErrorCode.INVALID_LEADERBOARD_ID);
            getCampaignOrThrow(leaderboardId);
            return scoreRepository.getRankedEntries(leaderboardId);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "getLeaderboard failed", e);
            return Collections.emptyList();
        }
    }

    private static final int MAX_SCORE = 1_000_000_000;

    @Override
    public void submitScore(String gameId, String userId, int score, long submittedAtEpochSeconds) {
        try {
            if (gameId == null || gameId.isBlank()) throw new LeaderboardException(ErrorCode.INVALID_GAME_ID);
            if (userId == null || userId.isBlank()) throw new LeaderboardException(ErrorCode.INVALID_USER_ID);
            if (score < 0 || score > MAX_SCORE) throw new LeaderboardException(ErrorCode.INVALID_SCORE);
            if (submittedAtEpochSeconds < 0) throw new LeaderboardException(ErrorCode.INVALID_TIMESTAMP);
            Game game = repository.getGame(gameId);
            if (game == null) throw new LeaderboardException(ErrorCode.GAME_NOT_FOUND);
            List<Campaign> active = repository.getActiveCampaigns(gameId, submittedAtEpochSeconds);
            for (Campaign campaign : active) {
                scoreRepository.addScore(campaign.getId(), userId, score);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "submitScore failed", e);
        }
    }

    @Override
    public List<ScoreEntry> listPlayersPrev(String gameId, String leaderboardId, String userId, int nPlayers) {
        try {
            getCampaignOrThrow(leaderboardId);
            return scoreRepository.getPlayersAbove(leaderboardId, userId, nPlayers);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "listPlayersPrev failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<ScoreEntry> listPlayersNext(String gameId, String leaderboardId, String userId, int nPlayers) {
        try {
            getCampaignOrThrow(leaderboardId);
            return scoreRepository.getPlayersBelow(leaderboardId, userId, nPlayers);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "listPlayersNext failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    public CampaignResult getCampaignResult(String leaderboardId) {
        try {
            Campaign campaign = getCampaignOrThrow(leaderboardId);
            return campaign.getResult();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "getCampaignResult failed", e);
            return null;
        }
    }

    @Override
    public void markRewardDisbursed(String leaderboardId) {
        try {
            Campaign campaign = getCampaignOrThrow(leaderboardId);
            CampaignResult result = campaign.getResult();
            if (result == null) throw new LeaderboardException(ErrorCode.CAMPAIGN_NOT_EXPIRED);
            result.markDisbursed();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "markRewardDisbursed failed", e);
        }
    }

    @Override
    public void markRewardFailed(String leaderboardId) {
        try {
            Campaign campaign = getCampaignOrThrow(leaderboardId);
            CampaignResult result = campaign.getResult();
            if (result == null) throw new LeaderboardException(ErrorCode.CAMPAIGN_NOT_EXPIRED);
            result.markFailed();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "markRewardFailed failed", e);
        }
    }

    private Campaign getCampaignOrThrow(String leaderboardId) {
        Campaign campaign = repository.getCampaignById(leaderboardId);
        if (campaign == null) throw new LeaderboardException(ErrorCode.LEADERBOARD_NOT_FOUND, "unknown leaderboard: " + leaderboardId);
        return campaign;
    }
}
