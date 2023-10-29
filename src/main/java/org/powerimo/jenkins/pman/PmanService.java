package org.powerimo.jenkins.pman;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PmanService {

    @PostConstruct
    public void afterInit() {
        log.info("Started");
    }

    public String getValue() {
        log.info("getValue()");
        return "test value";
    }

}
