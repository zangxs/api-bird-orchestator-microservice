package com.brayanpv.app.domain.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
public class ClassificationRequested implements Serializable {
    private UUID imageEventId;
    private String s3Key;
}
