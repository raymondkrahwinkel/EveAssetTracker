package com.eveworkbench.assettracker;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SpringConfiguration {

    // todo: add background task to cleanup expired sessions
    // region Schedules
//    @Scheduled(fixedDelay = 1000)
//    public void scheduleFixedDelay() {
//        System.out.println("Fixed delay task - " + System.currentTimeMillis() / 1000);
//    }
    // endregion
}
