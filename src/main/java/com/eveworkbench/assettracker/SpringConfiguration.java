package com.eveworkbench.assettracker;

import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.LoginStateRepository;
import com.eveworkbench.assettracker.repositories.SessionRepository;
import com.eveworkbench.assettracker.services.AuthenticationService;
import com.eveworkbench.assettracker.services.EsiWalletService;
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

    @Autowired
    EsiWalletService esiWalletService;

    // region esi token refresh
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
    // endregion

    // region cleanup tasks
    @Scheduled(fixedDelay = (60 * 1000))
    public void cleanup() {
        System.out.println(LocalDateTime.now() + " run cleanup");

        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(5);
        loginStateRepository.removeByCreatedAtBefore(expireTime);
        sessionRepository.removeByExpiresAtBefore(Date.from(expireTime.toInstant(ZoneOffset.systemDefault().getRules().getOffset(expireTime))));
    }
    // endregion

    // region wallet update
    @Scheduled(fixedDelay = (30 * 1000))
    public void updateWallets() {
        List<CharacterDto> characters = characterRepository.findByAccessTokenIsNotNullAndRefreshTokenIsNotNull();
        for(CharacterDto character : characters) {
            esiWalletService.getWalletBalance(character);
        }

        System.out.println(LocalDateTime.now() + " run wallet update");
    }
    // endregion
}
