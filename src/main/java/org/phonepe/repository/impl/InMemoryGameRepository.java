package org.phonepe.repository.impl;

import org.phonepe.expiry.ExpiryHandler;
import org.phonepe.model.Campaign;
import org.phonepe.model.CampaignType;
import org.phonepe.model.Game;
import org.phonepe.repository.IGameRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryGameRepository implements IGameRepository {

    private static final InMemoryGameRepository INSTANCE = new InMemoryGameRepository();

    private final Map<String, Game> gamesById = new ConcurrentHashMap<>();
    private final Map<String, Campaign> campaignsById = new ConcurrentHashMap<>();
    private final Map<String, List<Campaign>> activeByGameId = new ConcurrentHashMap<>();
    private final Map<String, List<Campaign>> expiredByGameId = new ConcurrentHashMap<>();
    private final Map<String, Object> locksByGameId = new ConcurrentHashMap<>();

    private InMemoryGameRepository() {}

    public static InMemoryGameRepository getInstance() {
        return INSTANCE;
    }

    @Override
    public List<String> getSupportedGameIds() {
        return List.copyOf(gamesById.keySet());
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
        activeByGameId.computeIfAbsent(gameId, k -> new CopyOnWriteArrayList<>()).add(campaign);
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
    public List<Campaign> getActiveCampaigns(String gameId, long epochSeconds) {
        List<Campaign> active = activeByGameId.get(gameId);
        if (active == null) return List.of();
        List<Campaign> currentActive = new ArrayList<>();
        for (Campaign c : active) {
            if (c.isActiveAt(epochSeconds)) currentActive.add(c);
        }
        return currentActive;
    }

    @Override
    public void runExpiryCheck(long epochSeconds, ExpiryHandler handler) {
        for (String gameId : activeByGameId.keySet()) {
            Object lock = locksByGameId.computeIfAbsent(gameId, k -> new Object());
            List<Campaign> toExpire;
            synchronized (lock) {
                List<Campaign> active = activeByGameId.get(gameId);
                if (active == null) continue;
                toExpire = new ArrayList<>();
                for (Campaign c : active) {
                    if (c.isExpired(epochSeconds)) toExpire.add(c);
                }
                if (toExpire.isEmpty()) continue;
                active.removeAll(toExpire);
                expiredByGameId.computeIfAbsent(gameId, k -> new CopyOnWriteArrayList<>()).addAll(toExpire);
            }
            for (Campaign c : toExpire) {
                handler.onExpired(c, epochSeconds);
            }
        }
    }
}
