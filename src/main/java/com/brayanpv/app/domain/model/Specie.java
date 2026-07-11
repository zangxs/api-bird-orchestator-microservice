package com.brayanpv.app.domain.model;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
public class Specie implements Serializable {

    private UUID id;
    private String commonName;
    private String scientificName;
    private String commonNameEn;
    private UUID familyId;
    private String imageUrl;
    private String description;
}
