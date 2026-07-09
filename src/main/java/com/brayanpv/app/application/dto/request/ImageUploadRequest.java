package com.brayanpv.app.application.dto.request;

import org.springframework.http.codec.multipart.FilePart;

public record ImageUploadRequest(FilePart image, String userId) {
}
