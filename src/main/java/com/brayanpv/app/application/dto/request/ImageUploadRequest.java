package com.brayanpv.app.application.dto.request;

import org.springframework.http.codec.multipart.FilePart;

import java.util.UUID;

public record ImageUploadRequest(FilePart image, UUID userId) {
}
