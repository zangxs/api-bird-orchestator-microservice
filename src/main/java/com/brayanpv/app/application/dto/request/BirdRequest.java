package com.brayanpv.app.application.dto.request;

import lombok.Data;

import java.io.File;
import java.io.Serializable;

@Data
public class BirdRequest implements Serializable {

    private File image;
    private String userId;
}
