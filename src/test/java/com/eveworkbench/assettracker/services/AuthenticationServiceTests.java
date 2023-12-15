package com.eveworkbench.assettracker.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.eveworkbench.assettracker.SecurityConstants;
import com.eveworkbench.assettracker.models.esi.OAuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(locations="classpath:application-test.properties")
@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTests {
    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // todo: tests for validateCharacter

    @Test
    void getCharacterFromOAuthResponse_null_input_Test() throws NoSuchMethodException, IllegalAccessException {
        Method getCharacterFromOAuthResponseMethod = AuthenticationService.class.getDeclaredMethod("getCharacterFromOAuthResponse", OAuthResponse.class);
        getCharacterFromOAuthResponseMethod.setAccessible(true);

        try {
            getCharacterFromOAuthResponseMethod.invoke(authenticationService, (Object) null);
        } catch (InvocationTargetException e) {
            assertInstanceOf(IllegalArgumentException.class, e.getCause(), () -> "Exception type is correct");
            assertEquals(e.getCause().getMessage(), "OAuthResponse cannot be empty", () -> "Exception message");
        }

    }

    @Test
    void getCharacterFromOAuthResponse_invalid_oauthData_Test() throws NoSuchMethodException, IllegalAccessException {
        Method getCharacterFromOAuthResponseMethod = AuthenticationService.class.getDeclaredMethod("getCharacterFromOAuthResponse", OAuthResponse.class);
        getCharacterFromOAuthResponseMethod.setAccessible(true);

        // test empty object
        try {
            getCharacterFromOAuthResponseMethod.invoke(authenticationService, new OAuthResponse());
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
            getCharacterFromOAuthResponseMethod.invoke(authenticationService, fakeResponse);
        } catch (InvocationTargetException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), () -> "Exception type is correct");
            assertEquals(e.getCause().getMessage(), "Access token cannot be empty", () -> "Exception message");
        }

        //getCharacterFromOAuthResponseMethod.invoke(authenticationService, new OAuthResponse());
    }
    // void getCharacterFromOAuthResponse_valid_oauthData_Test

    // void createToken_null_input_Test
    // void createToken_valid_input_Test
}
