package com.brayanpv.app.domain.model.enums;

public enum ImageStatus {
    PROCESSING,
    NOT_A_BIRD,
    BIRD_DETECTED,
    IDENTIFYING,
    DONE,
    FAILED,
    PENDING_REPLACE_CONFIRMATION,
    REPLACED,
    REPLACE_REJECTED,
    EXPIRED;
}
