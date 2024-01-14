package com.eveworkbench.assettracker.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.eveworkbench.assettracker.SecurityConstants;
import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.esi.OAuthResponse;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.SessionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(locations="classpath:application-test.properties")
@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTests {
    @Mock
    EsiService esiService;

    @Mock
    CharacterRepository characterRepository;

    @Mock
    SessionRepository sessionRepository;

    AuthenticationService authenticationService;

    @BeforeEach
    public void setup() {
        authenticationService = new AuthenticationService(characterRepository, sessionRepository, esiService);
    }

    // todo: tests for validateCharacter

    @Test
    void getCharacterFromOAuthResponse_null_input_Test() throws NoSuchMethodException, IllegalAccessException {
        Method getCharacterFromOAuthResponseMethod = AuthenticationService.class.getDeclaredMethod("getCharacterFromOAuthResponse", OAuthResponse.class, Optional.class);
        getCharacterFromOAuthResponseMethod.setAccessible(true);

        try {
            getCharacterFromOAuthResponseMethod.invoke(authenticationService, (Object) null, Optional.empty());
        } catch (InvocationTargetException e) {
            assertInstanceOf(IllegalArgumentException.class, e.getCause(), () -> "Exception type is correct");
            assertEquals(e.getCause().getMessage(), "OAuthResponse cannot be empty", () -> "Exception message");
        }

    }

    @Test
    void getCharacterFromOAuthResponse_invalid_oauthData_Test() throws NoSuchMethodException, IllegalAccessException {
        Method getCharacterFromOAuthResponseMethod = AuthenticationService.class.getDeclaredMethod("getCharacterFromOAuthResponse", OAuthResponse.class, Optional.class);
        getCharacterFromOAuthResponseMethod.setAccessible(true);

        // test empty object
        try {
            getCharacterFromOAuthResponseMethod.invoke(authenticationService, new OAuthResponse(), Optional.empty());
        } catch (InvocationTargetException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), () -> "Exception type is correct");
            assertEquals(e.getCause().getMessage(), "Access token cannot be empty", () -> "Exception message");
        }

        // test invalid subject information
        String jwtToken = JWT.create()
                .withSubject("InvalidInput")
                .withClaim("name", "tester")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 5000L))
                .withJWTId(UUID.randomUUID().toString())
                .withNotBefore(new Date(System.currentTimeMillis() + 1000L))
                .sign(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()));

        OAuthResponse fakeResponse = new OAuthResponse();
        fakeResponse.access_token = jwtToken;

        try {
            getCharacterFromOAuthResponseMethod.invoke(authenticationService, fakeResponse, Optional.empty());
        } catch (InvocationTargetException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), () -> "Exception type is correct");
            assertEquals(e.getCause().getMessage(), "Failed to get character id from login response", () -> "Exception message");
        }

        //getCharacterFromOAuthResponseMethod.invoke(authenticationService, new OAuthResponse());
    }

    @Test
    void getCharacterFromOAuthResponse_valid_oauthData_Test() throws NoSuchMethodException, IllegalAccessException {
        Method getCharacterFromOAuthResponseMethod = AuthenticationService.class.getDeclaredMethod("getCharacterFromOAuthResponse", OAuthResponse.class, Optional.class);
        getCharacterFromOAuthResponseMethod.setAccessible(true);

        // test valid subject information
        String jwtToken = JWT.create()
                .withSubject("CHARACTER:EVE:0000000001")
                .withClaim("name", "tester")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 5000L))
                .withJWTId(UUID.randomUUID().toString())
                .withNotBefore(new Date(System.currentTimeMillis() + 1000L))
                .sign(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()));

        OAuthResponse fakeResponse = new OAuthResponse();
        fakeResponse.access_token = jwtToken;
        fakeResponse.refresh_token = UUID.randomUUID().toString();
        fakeResponse.expires_in = 1000;
        fakeResponse.token_type = "Bearer";

        try {
            var response = (CharacterDto)getCharacterFromOAuthResponseMethod.invoke(authenticationService, fakeResponse, Optional.empty());

            assertEquals(response.getId(), 1);
            assertEquals(response.getName(), "tester");
            assertEquals(response.getAccessToken(), fakeResponse.access_token);
            assertEquals(response.getRefreshToken(), fakeResponse.refresh_token);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createToken_null_input_Test()
    {
        Throwable exception = assertThrows(NullPointerException.class, () -> authenticationService.createToken(null));
        assertEquals(exception.getMessage(), "Character cannot be null");
    }

    @Test
    void createToken_valid_input_Test()
    {
        var character = new CharacterDto();
        character.setId(1);
        character.setName("tester");
        String token;

        // validate the JWT token
        try {
            token = authenticationService.createToken(character, null);

            Algorithm algorithm = Algorithm.HMAC512(SecurityConstants.SECRET.getBytes());
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(token);
        } catch (JWTVerificationException exception){
            // Invalid signature/claims
            fail("JWT Token invalid");
        }
    }

    @Test
    void characterRefreshAccessToken_missingCharacter_Test() {
        // character id null
        Throwable throwable = assertThrows(IllegalArgumentException.class, () -> authenticationService.characterRefreshAccessToken(null));
        assertEquals(throwable.getMessage(), "Character id cannot be null");

        // missing character dto
        throwable = assertThrows(EntityNotFoundException.class, () -> authenticationService.characterRefreshAccessToken(1));
        assertEquals(throwable.getMessage(), "Character with id: 1 could not be found");
    }

    @Test
    void characterRefreshAccessToken_missingAccessRefreshToken_Test() {
        // missing access / refresh token
        CharacterDto characterDto = new CharacterDto();
        characterDto.setId(1);
        when(characterRepository.findById(1)).thenReturn(Optional.of(characterDto));

        Throwable throwable = assertThrows(RuntimeException.class, () -> authenticationService.characterRefreshAccessToken(1));
        assertEquals(throwable.getMessage(), "Character access and/or refresh token is empty");
    }

    @Test
    void characterRefreshAccessToken_invalidEsiResponse_Test() {
        // create fake character
        CharacterDto characterDto = new CharacterDto();
        characterDto.setId(1);
        characterDto.setAccessToken("accessTokenValue");
        characterDto.setRefreshToken("refreshTokenValue");

        when(characterRepository.findById(1)).thenReturn(Optional.of(characterDto));
        when(esiService.refreshToken("refreshTokenValue")).thenReturn(Optional.empty());
        assertFalse(authenticationService.characterRefreshAccessToken(1));
    }

    @Test
    void characterRefreshAccessToken_validEsiResponse_Test() {
        // create fake character
        CharacterDto characterDto = new CharacterDto();
        characterDto.setId(1);
        characterDto.setAccessToken("accessTokenValue");
        characterDto.setRefreshToken("refreshTokenValue");

        // create fake response object
        OAuthResponse fakeResponse = new OAuthResponse();
        fakeResponse.access_token = "accessTokenValue";
        fakeResponse.refresh_token = UUID.randomUUID().toString();
        fakeResponse.expires_in = 1000;
        fakeResponse.token_type = "Bearer";

        when(characterRepository.findById(1)).thenReturn(Optional.of(characterDto));
        when(esiService.refreshToken("refreshTokenValue")).thenReturn(Optional.of(fakeResponse));
        assertTrue(authenticationService.characterRefreshAccessToken(1));
    }
}
