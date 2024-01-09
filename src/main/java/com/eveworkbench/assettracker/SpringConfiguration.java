package com.eveworkbench.assettracker;

import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@EnableScheduling
public class SpringConfiguration {
    @Autowired
    CharacterRepository characterRepository;

    // todo: add background task to cleanup expired sessions
    // todo: add schedule to keep the access token up2date
    // region Schedules
    @Scheduled(fixedDelay = (60 * 1000))
    public void scheduleFixedDelay() {
        // get all characters with access and refresh token set
        List<CharacterDto> characters = characterRepository.findByAccessTokenIsNotNullAndRefreshTokenIsNotNull();
        for(CharacterDto character : characters) {
            System.out.println(character.getName());
        }

        System.out.println("Fixed delay task - " + System.currentTimeMillis() / 1000);
    }
    // endregion
}
