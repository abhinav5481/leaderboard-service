package org.phonepe.model;

import lombok.Getter;

import java.util.Objects;

@Getter
public class CampaignResult {
    private final String campaignId;
    private final String winnerUserId;
    private final int winnerScore;
    private final long expiredAtEpochSeconds;
    private volatile RewardStatus rewardStatus;

    public CampaignResult(String campaignId, String winnerUserId, int winnerScore, long expiredAtEpochSeconds) {
        this.campaignId = Objects.requireNonNull(campaignId);
        this.winnerUserId = winnerUserId;
        this.winnerScore = winnerScore;
        this.expiredAtEpochSeconds = expiredAtEpochSeconds;
        this.rewardStatus = RewardStatus.PENDING;
    }

    public void markDisbursed() { this.rewardStatus = RewardStatus.DISBURSED; }
    public void markFailed() { this.rewardStatus = RewardStatus.FAILED; }
}
