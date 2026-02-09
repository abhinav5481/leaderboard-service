package org.phonepe.exception;

import lombok.Getter;

@Getter
public class LeaderboardException extends RuntimeException {

    private final ErrorCode errorCode;
    private final int statusCode;

    public LeaderboardException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.statusCode = errorCode.getStatusCode();
    }

    public LeaderboardException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = errorCode.getStatusCode();
    }

    public LeaderboardException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.statusCode = errorCode.getStatusCode();
    }
}
