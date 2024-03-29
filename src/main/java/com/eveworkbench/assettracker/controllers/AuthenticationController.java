package com.eveworkbench.assettracker.controllers;

import com.eveworkbench.assettracker.models.api.request.RequestSwitch;
import com.eveworkbench.assettracker.models.api.response.ResponsePing;
import com.eveworkbench.assettracker.models.api.response.ResponseSwitch;
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
    public String getLoginUrl(@RequestParam UUID state, @RequestParam boolean ra, @RequestParam boolean ac, @RequestParam(required = false) Integer pc, @RequestParam(required = false) UUID session) throws URISyntaxException {
        if(clientId == null) {
            throw new RuntimeException("Missing esi client id");
        } else if(callbackUrl == null) {
            throw new RuntimeException("Missing esi callback url");
        }

        // register the login state
        LoginStateDto loginState = new LoginStateDto(state);
        loginState.setReAuthenticate(ra);
        loginState.setAddCharacter(ac);
        if(session != null) {
            loginState.setSession(session);
        }

        if(pc != null && pc > 0) {
            // get the character information
            Optional<CharacterDto> parentCharacter = characterRepository.findById(pc);
            if(parentCharacter.isEmpty()) {
                throw new IllegalArgumentException("Failed to get parent character with id: " + pc);
            }

            // check if this character has a parent
            if(parentCharacter.get().getParent() != null) {
                loginState.setParentCharacter(parentCharacter.get().getParent());
            } else {
                loginState.setParentCharacter(parentCharacter.get());
            }
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

            String token = authenticationService.validateCharacter(code, parentCharacter.orElse(null), !loginState.get().isAddCharacter());
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

    @PostMapping("/auth/switch")
    public ResponseEntity<ResponseSwitch> switchCharacter(@RequestBody RequestSwitch request) {
        // get the current logged-in user information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Integer characterId = Integer.parseInt(auth.getPrincipal().toString());
        String token = auth.getCredentials().toString();

        // get the session information
        Optional<SessionDto> session = sessionRepository.findByCharacterIdAndToken(characterId, token);
        if(session.isEmpty()) {
            return ResponseEntity.ok(new ResponseSwitch("Failed to get session information", false));
        }

        // get the character information
        if(!session.get().getCharacter().getId().equals(characterId)) {
            return ResponseEntity.ok(new ResponseSwitch("Failed to get character information", false));
        }

        CharacterDto parentCharacter = session.get().getCharacter();
        if(session.get().getCharacter().getParent() != null) {
            parentCharacter = session.get().getCharacter().getParent();
        }

        // check if we are authorized
        if(!
                (
                        parentCharacter.getId().equals(request.id) || // main
                        (parentCharacter.getParent() != null && parentCharacter.getParent().getId().equals(request.id)) || // parent
                        parentCharacter.getChildren().stream().anyMatch(child -> child.getId().equals(request.id)) // child
                )
        ) {
            return ResponseEntity.ok(new ResponseSwitch("You are not authorized to access this character", false));
        }

        // get the target character
        var targetCharacter = characterRepository.findById(request.id);
        if(targetCharacter.isEmpty()) {
            return ResponseEntity.ok(new ResponseSwitch("Failed to get target character information", false));
        }

        // change the session to the target character
        session.get().setCharacter(targetCharacter.get());
        sessionRepository.save(session.get());

        String jwtToken = authenticationService.createToken(session.get().getCharacter(), session.get());
        return ResponseEntity.ok(new ResponseSwitch("", true, jwtToken));
    }
}
