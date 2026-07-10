package com.brayanpv.app.infrastructure.web.contracts;

import com.brayanpv.app.application.dto.response.ImageUploadResponse;
import com.brayanpv.app.application.dto.response.GenericResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.RequestPart;
import reactor.core.publisher.Mono;


public interface IBirdController {

    Mono<ResponseEntity<GenericResponse<ImageUploadResponse>>> detectBird(@RequestPart("image") FilePart image,
                                                                          @RequestPart("userId") String userIdStr);
}
