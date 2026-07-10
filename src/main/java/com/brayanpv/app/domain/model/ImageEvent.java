package com.brayanpv.app.domain.model;

import com.brayanpv.app.domain.model.enums.ImageStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
public class ImageEvent implements Serializable {

    private UUID id;
    private UUID userId;
    private String s3Key;
    private ImageStatus status;
}
