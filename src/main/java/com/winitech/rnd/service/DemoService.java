package com.winitech.rnd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class DemoService {

    public Mono<String> getHello() {
        log.debug("Welcome Hello World");

        return Mono.just("Hello World");
    }
}
