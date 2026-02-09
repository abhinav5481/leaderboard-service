package org.phonepe.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    INVALID_GAME_ID("LB_001", "gameId is required", 400),
    INVALID_USER_ID("LB_002", "userId is required", 400),
    INVALID_LEADERBOARD_ID("LB_003", "leaderboardId is required", 400),
    INVALID_SCORE("LB_004", "score must be between 0 and 1000000000", 400),
    INVALID_TIMESTAMP("LB_005", "submittedAtEpochSeconds must be >= 0", 400),
    LEADERBOARD_NOT_FOUND("LB_006", "unknown leaderboard", 404),
    GAME_NOT_FOUND("LB_007", "game not found", 404),
    CAMPAIGN_NOT_EXPIRED("LB_008", "campaign not yet expired", 400),
    INVALID_CAMPAIGN_TYPE("LB_009", "type is required", 400),
    INTERNAL_ERROR("LB_999", "internal error", 500);

    private final String code;
    private final String message;
    private final int statusCode;

    ErrorCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
