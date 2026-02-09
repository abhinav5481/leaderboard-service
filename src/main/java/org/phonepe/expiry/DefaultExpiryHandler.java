package org.phonepe.expiry;

import org.phonepe.model.Campaign;
import org.phonepe.model.CampaignResult;
import org.phonepe.model.ScoreEntry;

import java.util.List;


public class DefaultExpiryHandler implements ExpiryHandler {

    private static final DefaultExpiryHandler INSTANCE = new DefaultExpiryHandler();

    private DefaultExpiryHandler() {}

    public static DefaultExpiryHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void onExpired(Campaign campaign, long expiredAtEpochSeconds) {
        List<ScoreEntry> ranked = campaign.getRankedEntries();
        String winnerUserId = null;
        int winnerScore = 0;
        if (!ranked.isEmpty()) {
            ScoreEntry first = ranked.get(0);
            winnerUserId = first.getUserId();
            winnerScore = first.getScore();
        }
        CampaignResult result = new CampaignResult(
                campaign.getId(),
                winnerUserId,
                winnerScore,
                expiredAtEpochSeconds
        );
        campaign.setResult(result);
    }
}
