package com.eveworkbench.assettracker;

import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.LoginStateRepository;
import com.eveworkbench.assettracker.repositories.SessionRepository;
import com.eveworkbench.assettracker.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.*;
import java.util.Date;
import java.util.List;

@Configuration
@EnableScheduling
public class SpringScheduler {
    final
    CharacterRepository characterRepository;

    final
    LoginStateRepository loginStateRepository;

    final
    SessionRepository sessionRepository;

    final
    AuthenticationService authenticationService;

    final
    EsiWalletService esiWalletService;
    private final EsiTypeService esiTypeService;
    private final EsiTypeCategoryService esiTypeCategoryService;
    private final EsiTypeGroupService esiTypeGroupService;
    private final EsiMarketGroupService esiMarketGroupService;
    protected final Logger logger = LoggerFactory.getLogger(EsiService.class);

    public SpringScheduler(CharacterRepository characterRepository, LoginStateRepository loginStateRepository, SessionRepository sessionRepository, AuthenticationService authenticationService, EsiWalletService esiWalletService, EsiTypeService esiTypeService, EsiTypeCategoryService esiTypeCategoryService, EsiTypeGroupService esiTypeGroupService, EsiMarketGroupService esiMarketGroupService) {
        this.characterRepository = characterRepository;
        this.loginStateRepository = loginStateRepository;
        this.sessionRepository = sessionRepository;
        this.authenticationService = authenticationService;
        this.esiWalletService = esiWalletService;
        this.esiTypeService = esiTypeService;
        this.esiTypeCategoryService = esiTypeCategoryService;
        this.esiTypeGroupService = esiTypeGroupService;
        this.esiMarketGroupService = esiMarketGroupService;
    }

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

    // region type update
    @Scheduled(cron = "0 0 12 * * ?", zone = "UTC") // every day 12.00 utc
    public void updateTypeData() {
        System.out.println(LocalDateTime.now() + " run type update");

        if(!esiTypeCategoryService.updateCategories()) {
            logger.error("Failed to update type category information");
            return;
        }

        if(!esiTypeGroupService.updateGroups()) {
            logger.error("Failed to update type group information");
            return;
        }

        if(!esiMarketGroupService.updateGroups()) {
            logger.error("Failed to update market group information");
            return;
        }

        if(!esiTypeService.updateTypesFromEsi()) {
            logger.error("Failed to update type information");
            return;
        }
    }
    // endregion
}
