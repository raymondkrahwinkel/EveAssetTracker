package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.models.esi.OAuthResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidParameterException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(locations="classpath:application-test.properties")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // this is needed because we have a base setup for the httpClient and one test that doesn't use httpClient
public class EsiServiceTests {
    @InjectMocks
    private EsiService esiService;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpClientFactory httpClientFactory;

    @Mock
    private HttpResponse<String> httpResponse;

    @SneakyThrows
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        when(httpClientFactory.create()).thenReturn(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(httpResponse);

        ReflectionTestUtils.setField(esiService, "clientId", "test");
        ReflectionTestUtils.setField(esiService, "clientSecret", "test");
    }

    // region getOauthInformation
    @Test
    void getOauthInformation_emptyCode_Test() {
        // test empty code exception
        Throwable missingCodeException = assertThrows(IllegalArgumentException.class, () -> esiService.getOauthInformation(""));
        assertEquals(missingCodeException.getMessage(), "code cannot be empty");
    }

    @Test
    void getOauthInformation_error_statusCodes_Test() throws URISyntaxException {
        when(httpResponse.statusCode()).thenReturn(401);
        Optional<OAuthResponse> response = esiService.getOauthInformation("code");
        assertTrue(response.isEmpty(), () -> "Test 404");

        when(httpResponse.statusCode()).thenReturn(404);
        response = esiService.getOauthInformation("code");
        assertTrue(response.isEmpty(), () -> "Test 404");

        when(httpResponse.statusCode()).thenReturn(500);
        response = esiService.getOauthInformation("code");
        assertTrue(response.isEmpty(), () -> "Test 500");

        when(httpResponse.statusCode()).thenReturn(420); // esi error limit reached
        response = esiService.getOauthInformation("code");
        assertTrue(response.isEmpty(), () -> "Test 500");
    }

    @Test
    void getOauthInformation_200_invalid_json_Test() throws URISyntaxException {
        // test 200 response with invalid json data
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("");

        Optional<OAuthResponse> response = esiService.getOauthInformation("code");
        assertTrue(response.isEmpty());
    }

    @Test
    void getOauthInformation_200_valid_json_Test() throws URISyntaxException {
        // test 200 response with invalid json data
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{ \"access_token\": \"test\", \"expires_in\": 60, \"token_type\": \"Bearer\", \"refresh_token\": \"token\" }");

        Optional<OAuthResponse> response = esiService.getOauthInformation("code");
        assertFalse(response.isEmpty());

        // validate the json decoded values
        assertEquals(response.get().access_token, "test", () -> "Access token value");
        assertEquals(response.get().expires_in, 60, () -> "Expire value");
        assertEquals(response.get().token_type, "Bearer", () -> "Token type value");
        assertEquals(response.get().refresh_token, "token", () -> "Refresh token value");
    }
    // endregion
}
