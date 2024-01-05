package com.eveworkbench.assettracker.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.eveworkbench.assettracker.SecurityConstants;
import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.database.SessionDto;
import com.eveworkbench.assettracker.models.esi.OAuthResponse;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuthenticationService {
    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private EsiService esiService;

    // validate the oauth code and return a new JWT token
    public String validateCharacter(String code) throws RuntimeException {
        // execute the oauth validation via the ESI
        Optional<OAuthResponse> oAuthResponse;
        try {
            oAuthResponse = esiService.getOauthInformation(code);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        if(oAuthResponse.isEmpty()) {
            throw new RuntimeException("Failed to get authentication information from the ESI for code: " + code);
        }

        CharacterDto character = getCharacterFromOAuthResponse(oAuthResponse.get());
        return createToken(character);
    }

    // get the character dto from the oauth response
    private CharacterDto getCharacterFromOAuthResponse(OAuthResponse oAuthResponse)
    {
        if(oAuthResponse == null) {
            throw new IllegalArgumentException("OAuthResponse cannot be empty");
        }

        if(oAuthResponse.access_token == null || oAuthResponse.access_token.isEmpty()) {
            throw new RuntimeException("Access token cannot be empty");
        }

        // decode the jwt access_token
        Map<String, Claim> claims = JWT.decode(oAuthResponse.access_token).getClaims();

        // get the character name and id from the access token information
        String characterName = claims.get("name").asString();
        Integer characterId;

        // extract the character id from the JTW subject
        Pattern p = Pattern.compile("^CHARACTER:EVE:([0-9]+)$");
        Matcher m = p.matcher(claims.get("sub").asString());
        if (m.find()) {
            characterId = Integer.parseInt(m.group(1));
        } else {
            characterId = null;
        }

        if(characterId == null) {
            throw new RuntimeException("Failed to get character id from login response");
        }

        Optional<CharacterDto> character = characterRepository.findById(characterId); //.orElseThrow(() -> new EntityNotFoundException("Character not found with id: " + characterId));
        if(character.isPresent()) {
            // update the existing character with the new information
            character.get().setAccessToken(oAuthResponse.access_token);
            character.get().setRefreshToken(oAuthResponse.refresh_token);
            character.get().setTokenExpiresAt(new Date(System.currentTimeMillis() + (oAuthResponse.expires_in * 1000)));
            characterRepository.save(character.get());

            return character.get();
        } else {
            // create new character
            CharacterDto dto = new CharacterDto();
            dto.setId(characterId);
            dto.setName(characterName);
            dto.setAccessToken(oAuthResponse.access_token);
            dto.setRefreshToken(oAuthResponse.refresh_token);
            dto.setTokenExpiresAt(new Date(System.currentTimeMillis() + (oAuthResponse.expires_in * 1000)));
            characterRepository.save(dto);

            return dto;
        }
    }

    // create JWT token for the character dto
    public String createToken(CharacterDto character) {
        return createToken(character, null);
    }

    public String createToken(CharacterDto character, SessionDto session) {
        if(character == null) {
            throw new NullPointerException("Character cannot be null");
        }

        if(session == null) {
            session = new SessionDto();
            session.setCharacter(character);
            session.setToken(UUID.randomUUID().toString());
        }

        session.setExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME)); // set timeout to 2 hours
        sessionRepository.save(session);

        return JWT.create()
                .withSubject(character.getId().toString())
                .withClaim("id", character.getId())
                .withClaim("name", character.getName())
                .withClaim("token", session.getToken())
                .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
                .sign(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()));
    }
}
