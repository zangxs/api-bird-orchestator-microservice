package com.brayanpv.app.infrastructure.web.implementations;

import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.infrastructure.web.contracts.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Log4j2
public class UserService implements IUserService {



    @Override
    @GetMapping("/userId/birds")
    public Flux<ImageEvent> getImageEvents(@PathVariable UUID userId) {
        return null;
    }
}
