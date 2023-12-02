package com.eveworkbench.assettracker.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.eveworkbench.assettracker.SecurityConstants;
import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.esi.OAuthResponse;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuthenticationService {
    @Autowired
    private CharacterRepository characterRepository;

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
        // decode the jwt access_token
        Map<String, Claim> claims = JWT.decode(oAuthResponse.access_token).getClaims();

        // get the character name and id from the access token information
        String characterName = claims.get("name").asString();
        Integer characterId;

        // extract the character id from the JTW subject
        Pattern p = Pattern.compile("^CHARACTER:EVE:([0-9]+)$");
        Matcher m = p.matcher(claims.get("sub").asString());
        if(m.find()) {
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

    // create JWT token for the character id
    public String createToken(int characterId) throws Exception {
        Optional<CharacterDto> character = characterRepository.findById(characterId);
        if(character.isEmpty()) {
            throw new Exception("Failed to get character with requested id: " + characterId);
        }

        return createToken(character.get());
    }

    // create JWT token for the character dto
    public String createToken(CharacterDto character) {
        return JWT.create()
                .withSubject(character.getId().toString())
                .withClaim("id", character.getId())
                .withClaim("name", character.getName())
                .withClaim("access_token", character.getAccessToken())
                .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
                .sign(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()));
    }
}