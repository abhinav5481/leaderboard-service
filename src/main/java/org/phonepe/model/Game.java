package org.phonepe.model;

import lombok.Getter;
import org.phonepe.expiry.ExpiryHandler;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Game {
    private final String id;
    private final String name;
    private final List<Campaign> activeCampaigns = new ArrayList<>();
    private final List<Campaign> expiredCampaigns = new ArrayList<>();

    public Game(String id, String name) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("game id required");
        this.id = id;
        this.name = (name != null && !name.isBlank()) ? name : "";
    }

    public synchronized void addCampaign(Campaign campaign) {
        if (campaign == null || !campaign.getGameId().equals(id))
            throw new IllegalArgumentException("campaign must belong to this game");
        activeCampaigns.add(campaign);
    }

    /**
     * Returns campaigns currently active at the given time. No mutation; efficient for submit path.
     */
    public synchronized List<Campaign> getActiveCampaigns(long epochSeconds) {
        List<Campaign> currentActive = new ArrayList<>();
        for (Campaign c : activeCampaigns) {
            if (c.isActiveAt(epochSeconds)) currentActive.add(c);
        }
        return currentActive;
    }

    /**
     * Called by background expiry only: move expired campaigns to expired list and run handler
     * (winner calculation, result storage, reward status PENDING).
     */
    public synchronized void processExpiredCampaigns(long epochSeconds, ExpiryHandler handler) {
        List<Campaign> toExpire = new ArrayList<>();
        for (Campaign c : activeCampaigns) {
            if (c.isExpired(epochSeconds)) toExpire.add(c);
        }
        if (toExpire.isEmpty()) return;
        activeCampaigns.removeAll(toExpire);
        expiredCampaigns.addAll(toExpire);
        for (Campaign c : toExpire) {
            handler.onExpired(c, epochSeconds);
        }
    }

    public synchronized List<Campaign> getExpiredCampaigns() {
        return new ArrayList<>(expiredCampaigns);
    }
}
