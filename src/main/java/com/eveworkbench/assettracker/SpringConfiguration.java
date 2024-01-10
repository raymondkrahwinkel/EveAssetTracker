package com.eveworkbench.assettracker;

import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.LoginStateRepository;
import com.eveworkbench.assettracker.repositories.SessionRepository;
import com.eveworkbench.assettracker.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.*;
import java.util.Date;
import java.util.List;

@Configuration
@EnableScheduling
public class SpringConfiguration {
    @Autowired
    CharacterRepository characterRepository;

    @Autowired
    LoginStateRepository loginStateRepository;


    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    AuthenticationService authenticationService;

    // todo: add background task to cleanup expired sessions
    // todo: add schedule to keep the access token up2date
    // region Schedules
    @Scheduled(fixedDelay = (60 * 1000))
    public void esiTokenRefresh() {
        // get all characters with access and refresh token set
        List<CharacterDto> characters = characterRepository.findByAccessTokenIsNotNullAndRefreshTokenIsNotNull();
        for(CharacterDto character : characters) {
            // check if we need to update the character access token when it is less than 5 minutes valid
            if(character.getTokenExpiresAt().before(Date.from(Instant.now().plus(Duration.ofMinutes(5))))) {
                boolean tokenUpdated = authenticationService.characterRefreshAccessToken(character.getId());
                System.out.println(character.getName() + " token refresh: " + tokenUpdated);
            } else {
                System.out.println(character.getName() + " no update needed");
            }
        }

        System.out.println(LocalDateTime.now() + " run token refresh schedule");
    }

    // cleanup tasks
    @Scheduled(fixedDelay = (60 * 1000))
    public void cleanup() {
        System.out.println(LocalDateTime.now() + " run cleanup");

        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(5);
        loginStateRepository.removeByCreatedAtBefore(expireTime);
        sessionRepository.removeByExpiresAtBefore(Date.from(expireTime.toInstant(ZoneOffset.systemDefault().getRules().getOffset(expireTime))));
    }
    // endregion
}
