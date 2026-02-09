package org.phonepe.expiry;

import org.phonepe.model.Campaign;
import org.phonepe.model.CampaignResult;
import org.phonepe.model.ScoreEntry;

import java.util.List;

public interface ExpiryHandler {

    void onExpired(Campaign campaign, long expiredAtEpochSeconds);
}
