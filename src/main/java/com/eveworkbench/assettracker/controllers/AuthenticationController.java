package com.eveworkbench.assettracker.controllers;

import com.eveworkbench.assettracker.models.api.response.ResponsePing;
import com.eveworkbench.assettracker.models.api.response.ResponseValidate;
import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.database.LoginStateDto;
import com.eveworkbench.assettracker.models.database.SessionDto;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.LoginStateRepository;
import com.eveworkbench.assettracker.repositories.SessionRepository;
import com.eveworkbench.assettracker.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin
public class AuthenticationController {
    @Value("${esi.clientid:#{null}}")
    private String clientId;

    @Value("${esi.scopes:#{\"\"}}")
    private String esiScopes;

    @Value("${esi.callbackUrl:#{null}}")
    private String callbackUrl;

    private final AuthenticationService authenticationService;
    private final SessionRepository sessionRepository;
    private final LoginStateRepository loginStateRepository;
    private final CharacterRepository characterRepository;

    public AuthenticationController(AuthenticationService authenticationService, SessionRepository sessionRepository, LoginStateRepository loginStateRepository,
                                    CharacterRepository characterRepository) {
        this.authenticationService = authenticationService;
        this.sessionRepository = sessionRepository;
        this.loginStateRepository = loginStateRepository;
        this.characterRepository = characterRepository;
    }

    @GetMapping("/auth/login/url")
    public String getLoginUrl(@RequestParam UUID state, @RequestParam boolean ra, @RequestParam boolean ac, @RequestParam Optional<Integer> pc, @RequestParam Optional<UUID> session) throws URISyntaxException {
        if(clientId == null) {
            throw new RuntimeException("Missing esi client id");
        } else if(callbackUrl == null) {
            throw new RuntimeException("Missing esi callback url");
        }

        // register the login state
        LoginStateDto loginState = new LoginStateDto(state);
        loginState.setReAuthenticate(ra);
        loginState.setAddCharacter(ac);
        session.ifPresent(loginState::setSession);

        if(pc.isPresent()) {
            // get the character information
            Optional<CharacterDto> parentCharacter = characterRepository.findById(pc.get());
            if(parentCharacter.isEmpty()) {
                throw new IllegalArgumentException("Failed to get parent character with id: " + pc.get());
            }

            loginState.setParentCharacter(parentCharacter.get());
        }

        loginStateRepository.save(loginState);

        String url = String.format("https://login.eveonline.com/v2/oauth/authorize/?response_type=code&redirect_uri=%s&client_id=%s&scope=%s&state=%s", URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8), clientId, URLEncoder.encode(esiScopes, StandardCharsets.UTF_8), state);
        URI uri = new URI(url);
        return uri.toString();
    }

    @GetMapping("/auth/validate")
    public ResponseEntity<ResponseValidate> getValidate(String code, UUID state)
    {
        Optional<CharacterDto> parentCharacter = Optional.empty();
        Optional<LoginStateDto> loginState = loginStateRepository.findByState(state);
        if(loginState.isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseValidate("Login state is missing", false, null));
        }

        try {
            if(loginState.get().isAddCharacter()) {
                if(loginState.get().getParentCharacter() == null) {
                    return ResponseEntity.badRequest().body(new ResponseValidate("Cannot add new character without parent defined", false, null));
                }

                parentCharacter = Optional.ofNullable(loginState.get().getParentCharacter());
            }

            String token = authenticationService.validateCharacter(code, parentCharacter, !loginState.get().isAddCharacter());
            var response = new ResponseValidate("", true, token);
            if(loginState.get().isAddCharacter() && parentCharacter.isPresent() && token == null) {
                // we need no new token, only the data update
                response.setChildCharacterValidation(true);
            }

            return ResponseEntity.ok(response);
        }
        catch(RuntimeException e)
        {
            return ResponseEntity.badRequest().body(new ResponseValidate(e.getMessage(), false, null));
        }
        finally
        {
            loginStateRepository.delete(loginState.get());
        }
    }

    @PostMapping("/auth/logout")
    public Boolean logout() {
        // get the current logged-in user information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Integer characterId = Integer.parseInt(auth.getPrincipal().toString());
        String token = auth.getCredentials().toString();

        // get the session information
        Optional<SessionDto> session = sessionRepository.findByCharacterIdAndToken(characterId, token);
        if(session.isEmpty()) {
            return false;
        }

        // remove the sessions
        sessionRepository.delete(session.get());
        return true;
    }

    @PostMapping("/auth/ping")
    public ResponseEntity<ResponsePing> ping() {
        // get the current logged-in user information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Integer characterId = Integer.parseInt(auth.getPrincipal().toString());
        String token = auth.getCredentials().toString();

        // get the session information
        Optional<SessionDto> session = sessionRepository.findByCharacterIdAndToken(characterId, token);
        if(session.isEmpty()) {
            return ResponseEntity.ok(new ResponsePing("Failed to get session information", false));
        }

        // get the character information
        if(!session.get().getCharacter().getId().equals(characterId)) {
            return ResponseEntity.ok(new ResponsePing("Failed to get character information", false));
        }

        // check if we need to update the character access token when it is less than 5 minutes valid
        if(session.get().getCharacter().getTokenExpiresAt().before(Date.from(Instant.now().plus(Duration.ofMinutes(5))))) {
            if(!authenticationService.characterRefreshAccessToken(characterId)) {
                return ResponseEntity.ok(new ResponsePing("Failed to refresh access token for character", false));
            }
        }

        String jwtToken = authenticationService.createToken(session.get().getCharacter(), session.get());
        return ResponseEntity.ok(new ResponsePing("", true, jwtToken));
    }
}
