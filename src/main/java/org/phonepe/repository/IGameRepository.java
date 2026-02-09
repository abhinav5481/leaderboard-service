package org.phonepe.repository;

import org.phonepe.expiry.ExpiryHandler;
import org.phonepe.model.Campaign;
import org.phonepe.model.CampaignType;
import org.phonepe.model.Game;

import java.util.List;

public interface IGameRepository {

    List<String> getSupportedGameIds();

    Game getOrCreateGame(String gameId);

    Game getOrCreateGame(String gameId, String gameName);

    Campaign createCampaign(String gameId, String gameName, String campaignName, CampaignType type, long startEpochSeconds, long endEpochSeconds);

    Campaign getCampaignById(String leaderboardId);

    Game getGame(String gameId);

    /** Run expiry for all games: move expired campaigns and invoke handler (winner, reward status). */
    void runExpiryCheck(long epochSeconds, ExpiryHandler handler);
}
