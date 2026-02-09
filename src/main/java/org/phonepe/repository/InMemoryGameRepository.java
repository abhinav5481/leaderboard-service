package org.phonepe.repository;

import org.phonepe.expiry.ExpiryHandler;
import org.phonepe.model.Campaign;
import org.phonepe.model.CampaignType;
import org.phonepe.model.Game;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryGameRepository implements IGameRepository {

    private final Map<String, Game> gamesById = new ConcurrentHashMap<>();
    private final Map<String, Campaign> campaignsById = new ConcurrentHashMap<>();

    @Override
    public List<String> getSupportedGameIds() {
        return List.copyOf(gamesById.keySet());
    }

    @Override
    public Game getOrCreateGame(String gameId) {
        return getOrCreateGame(gameId, "DEFAULT");
    }

    @Override
    public Game getOrCreateGame(String gameId, String gameName) {
        return gamesById.computeIfAbsent(gameId, k -> new Game(k, gameName));
    }

    @Override
    public Campaign createCampaign(String gameId, String gameName, String campaignName, CampaignType type, long startEpochSeconds, long endEpochSeconds) {
        Game game = getOrCreateGame(gameId, gameName);
        String id = gameId + "::" + startEpochSeconds + "::" + endEpochSeconds;
        Campaign campaign = new Campaign(id, campaignName, type, gameId, startEpochSeconds, endEpochSeconds);
        game.addCampaign(campaign);
        campaignsById.put(id, campaign);
        return campaign;
    }

    @Override
    public Campaign getCampaignById(String leaderboardId) {
        return campaignsById.get(leaderboardId);
    }

    @Override
    public Game getGame(String gameId) {
        return gamesById.get(gameId);
    }

    @Override
    public void runExpiryCheck(long epochSeconds, ExpiryHandler handler) {
        for (Game game : gamesById.values()) {
            game.processExpiredCampaigns(epochSeconds, handler);
        }
    }
}
