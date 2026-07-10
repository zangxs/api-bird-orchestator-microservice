package com.brayanpv.app.domain.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
public class BirdObserved implements Serializable {
    private UUID userId;
    private String s3Key;
    private String status;
}
