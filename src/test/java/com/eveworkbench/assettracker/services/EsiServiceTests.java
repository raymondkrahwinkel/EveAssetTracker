package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.models.esi.EsiBaseResponse;
import com.eveworkbench.assettracker.models.esi.OAuthResponse;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.EsiEtagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(locations="classpath:application-test.properties")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // this is needed because we have a base setup for the httpClient and one test that doesn't use httpClient
public class EsiServiceTests {
    private EsiService esiService;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpClientFactory httpClientFactory;

    @Mock
    private HttpResponse<String> httpResponse;

    @Mock
    private CharacterRepository characterRepository;

    @Mock
    private EsiEtagRepository esiEtagRepository;

    @BeforeEach
    public void setup() throws IOException, InterruptedException {
        esiService = new EsiService(characterRepository, httpClientFactory, esiEtagRepository);

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
        assertTrue(response.isEmpty(), "Test 404");

        when(httpResponse.statusCode()).thenReturn(404);
        response = esiService.getOauthInformation("code");
        assertTrue(response.isEmpty(), "Test 404");

        when(httpResponse.statusCode()).thenReturn(500);
        response = esiService.getOauthInformation("code");
        assertTrue(response.isEmpty(), "Test 500");

        when(httpResponse.statusCode()).thenReturn(420); // esi error limit reached
        response = esiService.getOauthInformation("code");
        assertTrue(response.isEmpty(), "Test 500");
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
        assertEquals(response.get().access_token, "test", "Access token value");
        assertEquals(response.get().expires_in, 60, "Expire value");
        assertEquals(response.get().token_type, "Bearer", "Token type value");
        assertEquals(response.get().refresh_token, "token", "Refresh token value");
    }
    // endregion

    // region refreshToken
    @Test
    void refreshToken_emptyCode_Test() {
        Throwable missingCodeException = assertThrows(IllegalArgumentException.class, () -> esiService.refreshToken(""));
        assertEquals(missingCodeException.getMessage(), "refreshToken cannot be empty");
    }

    @Test
    void refreshToken_error_statusCodes_Test() {
        when(httpResponse.statusCode()).thenReturn(401);
        Optional<OAuthResponse> response = esiService.refreshToken("refreshToken");
        assertTrue(response.isEmpty(), "Test 404");

        when(httpResponse.statusCode()).thenReturn(404);
        response = esiService.refreshToken("code");
        assertTrue(response.isEmpty(), "Test 404");

        when(httpResponse.statusCode()).thenReturn(500);
        response = esiService.refreshToken("code");
        assertTrue(response.isEmpty(), "Test 500");

        when(httpResponse.statusCode()).thenReturn(420); // esi error limit reached
        response = esiService.refreshToken("code");
        assertTrue(response.isEmpty(), "Test 500");
    }

    @Test
    void refreshToken_200_invalid_json_Test() {
        // test 200 response with invalid json data
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("");

        Optional<OAuthResponse> response = esiService.refreshToken("code");
        assertTrue(response.isEmpty());
    }

    @Test
    void refreshToken_200_valid_json_Test() {
        // test 200 response with invalid json data
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{ \"access_token\": \"test\", \"expires_in\": 60, \"token_type\": \"Bearer\", \"refresh_token\": \"token\" }");

        Optional<OAuthResponse> response = esiService.refreshToken("code");
        assertFalse(response.isEmpty());

        // validate the json decoded values
        assertEquals(response.get().access_token, "test", "Access token value");
        assertEquals(response.get().expires_in, 60, "Expire value");
        assertEquals(response.get().token_type, "Bearer", "Token type value");
        assertEquals(response.get().refresh_token, "token", "Refresh token value");
    }
    // endregion

    // region interpretEsiResponse (protected)
    Method getInterpretEsiResponseMethod() throws NoSuchMethodException {
        Method interpretEsiResponseMethod = EsiService.class.getDeclaredMethod("interpretEsiResponse", EsiBaseResponse.class, HttpResponse.class);
        interpretEsiResponseMethod.setAccessible(true);
        return interpretEsiResponseMethod;
    }

    @Test
    void interpretEsiResponse_emptyArguments_Test() throws NoSuchMethodException, IllegalAccessException {
        Method interpretEsiResponseMethod = getInterpretEsiResponseMethod();

        try {
            interpretEsiResponseMethod.invoke(esiService, null, null);
        } catch (InvocationTargetException e) {
            assertInstanceOf(IllegalArgumentException.class, e.getCause(), "ESI Response argument cannot be null");
        }

        try {
            interpretEsiResponseMethod.invoke(esiService, new EsiBaseResponse<>() {}, null);
        } catch (InvocationTargetException e) {
            assertInstanceOf(IllegalArgumentException.class, e.getCause(), "Http response object cannot be null");
        }
    }

    @Test
    void interpretEsiResponse_nullHttpHeaders_Test() throws NoSuchMethodException, IllegalAccessException {
        Method interpretEsiResponseMethod = getInterpretEsiResponseMethod();
        HttpResponse fakeHttpResponse = new HttpResponse() {
            @Override
            public int statusCode() {
                return 0;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            public Object body() {
                return null;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public HttpClient.Version version() {
                return null;
            }
        };

        try {
            interpretEsiResponseMethod.invoke(esiService, new EsiBaseResponse<>() {}, fakeHttpResponse);
        } catch (InvocationTargetException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), "Http response has no headers");
        }
    }

    @Test
    void interpretEsiResponse_emptyHttpHeaders_Test() throws NoSuchMethodException, IllegalAccessException {
        Method interpretEsiResponseMethod = getInterpretEsiResponseMethod();
        HttpResponse fakeHttpResponse = mock(HttpResponse.class);
        EsiBaseResponse fakeEsiResponse = mock(EsiBaseResponse.class);

        when(fakeHttpResponse.headers()).thenReturn(mock(HttpHeaders.class));

        try {
            Object success = interpretEsiResponseMethod.invoke(esiService, fakeEsiResponse, fakeHttpResponse);
            assertFalse((Boolean) success);
        } catch(InvocationTargetException|RuntimeException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), "Http response has no headers");
        }

        assertTrue(fakeEsiResponse.hasError, "Has error reported");
        assertEquals(fakeEsiResponse.error, "Error status code received from ESI");
    }

    @Test
    void interpretEsiResponse_errorStatusCodes_Test() throws NoSuchMethodException, IllegalAccessException {
        Method interpretEsiResponseMethod = getInterpretEsiResponseMethod();
        HttpResponse fakeHttpResponse = mock(HttpResponse.class);
        EsiBaseResponse fakeEsiResponse = mock(EsiBaseResponse.class);

        when(fakeHttpResponse.headers()).thenReturn(mock(HttpHeaders.class));
        when(fakeHttpResponse.statusCode()).thenReturn(400);

        try {
            Object success = interpretEsiResponseMethod.invoke(esiService, fakeEsiResponse, fakeHttpResponse);
            assertFalse((Boolean) success);
        } catch(InvocationTargetException|RuntimeException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), "Http response has no headers");
        }

        assertTrue(fakeEsiResponse.hasError, "Has error reported");
        assertEquals("Error status code received from ESI", fakeEsiResponse.error);
        assertEquals(400, fakeEsiResponse.statusCode);

        when(fakeHttpResponse.headers()).thenReturn(mock(HttpHeaders.class));
        when(fakeHttpResponse.statusCode()).thenReturn(401);

        try {
            Object success = interpretEsiResponseMethod.invoke(esiService, fakeEsiResponse, fakeHttpResponse);
            assertFalse((Boolean) success);
        } catch(InvocationTargetException|RuntimeException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), "Http response has no headers");
        }

        assertTrue(fakeEsiResponse.hasError, "Has error reported");
        assertEquals("Error status code received from ESI", fakeEsiResponse.error);
        assertEquals(401, fakeEsiResponse.statusCode);

        when(fakeHttpResponse.headers()).thenReturn(mock(HttpHeaders.class));
        when(fakeHttpResponse.statusCode()).thenReturn(403);

        try {
            Object success = interpretEsiResponseMethod.invoke(esiService, fakeEsiResponse, fakeHttpResponse);
            assertFalse((Boolean) success);
        } catch(InvocationTargetException|RuntimeException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), "Http response has no headers");
        }

        assertTrue(fakeEsiResponse.hasError, "Has error reported");
        assertEquals("Error status code received from ESI", fakeEsiResponse.error);
        assertEquals(403, fakeEsiResponse.statusCode);

        when(fakeHttpResponse.headers()).thenReturn(mock(HttpHeaders.class));
        when(fakeHttpResponse.statusCode()).thenReturn(404);

        try {
            Object success = interpretEsiResponseMethod.invoke(esiService, fakeEsiResponse, fakeHttpResponse);
            assertFalse((Boolean) success);
        } catch(InvocationTargetException|RuntimeException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), "Http response has no headers");
        }

        assertTrue(fakeEsiResponse.hasError, "Has error reported");
        assertEquals("Error status code received from ESI", fakeEsiResponse.error);
        assertEquals(404, fakeEsiResponse.statusCode);

        when(fakeHttpResponse.headers()).thenReturn(mock(HttpHeaders.class));
        when(fakeHttpResponse.statusCode()).thenReturn(500);

        try {
            Object success = interpretEsiResponseMethod.invoke(esiService, fakeEsiResponse, fakeHttpResponse);
            assertFalse((Boolean) success);
        } catch(InvocationTargetException|RuntimeException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), "Http response has no headers");
        }

        assertTrue(fakeEsiResponse.hasError, "Has error reported");
        assertEquals("Error status code received from ESI", fakeEsiResponse.error);
        assertEquals(500, fakeEsiResponse.statusCode);

        when(fakeHttpResponse.headers()).thenReturn(mock(HttpHeaders.class));
        when(fakeHttpResponse.statusCode()).thenReturn(503);

        try {
            Object success = interpretEsiResponseMethod.invoke(esiService, fakeEsiResponse, fakeHttpResponse);
            assertFalse((Boolean) success);
        } catch(InvocationTargetException|RuntimeException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), "Http response has no headers");
        }

        assertTrue(fakeEsiResponse.hasError, "Has error reported");
        assertEquals("Error status code received from ESI", fakeEsiResponse.error);
        assertEquals(503, fakeEsiResponse.statusCode);

        when(fakeHttpResponse.headers()).thenReturn(mock(HttpHeaders.class));
        when(fakeHttpResponse.statusCode()).thenReturn(504);

        try {
            Object success = interpretEsiResponseMethod.invoke(esiService, fakeEsiResponse, fakeHttpResponse);
            assertFalse((Boolean) success);
        } catch(InvocationTargetException|RuntimeException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), "Http response has no headers");
        }

        assertTrue(fakeEsiResponse.hasError, "Has error reported");
        assertEquals("Error status code received from ESI", fakeEsiResponse.error);
        assertEquals(504, fakeEsiResponse.statusCode);
    }

    @Test
    void interpretEsiResponse_errorLimiter_Test() throws NoSuchMethodException, IllegalAccessException {
        Method interpretEsiResponseMethod = getInterpretEsiResponseMethod();
        HttpResponse fakeHttpResponse = mock(HttpResponse.class);
        EsiBaseResponse fakeEsiResponse = mock(EsiBaseResponse.class);

        when(fakeHttpResponse.headers()).thenReturn(mock(HttpHeaders.class));
        when(fakeHttpResponse.headers().firstValue("X-Esi-Error-Limit-Reset")).thenReturn(Optional.of("30"));
        when(fakeHttpResponse.headers().firstValue("X-Esi-Error-Limit-Remain")).thenReturn(Optional.of("1"));
        when(fakeHttpResponse.statusCode()).thenReturn(420);

        try {
            Object success = interpretEsiResponseMethod.invoke(esiService, fakeEsiResponse, fakeHttpResponse);
            assertFalse((Boolean) success);
        } catch(InvocationTargetException|RuntimeException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), "Http response has no headers");
        }

        assertTrue(fakeEsiResponse.hasError, "Has error reported");
        assertEquals("ESI error limit reached", fakeEsiResponse.error);
        assertEquals(420, fakeEsiResponse.statusCode);
        assertEquals(30, fakeEsiResponse.esiErrorLimitReset);
        assertEquals(1, fakeEsiResponse.esiErrorLimitRemain);
    }

    @Test
    void interpretEsiResponse_contentNotModified_Test() throws NoSuchMethodException, IllegalAccessException {
        Method interpretEsiResponseMethod = getInterpretEsiResponseMethod();
        HttpResponse fakeHttpResponse = mock(HttpResponse.class);
        EsiBaseResponse fakeEsiResponse = new EsiBaseResponse() {};

        when(fakeHttpResponse.headers()).thenReturn(mock(HttpHeaders.class));
        when(fakeHttpResponse.statusCode()).thenReturn(304);

        try {
            Object success = interpretEsiResponseMethod.invoke(esiService, fakeEsiResponse, fakeHttpResponse);
            assertTrue((Boolean) success);
        } catch(InvocationTargetException|RuntimeException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), "Http response has no headers");
        }

        assertFalse(fakeEsiResponse.hasError, "Has error reported");
        assertEquals(304, fakeEsiResponse.statusCode);
        assertFalse(fakeEsiResponse.contentModified);
    }

    @Test
    void interpretEsiResponse_ok_Test() throws NoSuchMethodException, IllegalAccessException {
        Method interpretEsiResponseMethod = getInterpretEsiResponseMethod();
        HttpResponse fakeHttpResponse = mock(HttpResponse.class);
        EsiBaseResponse fakeEsiResponse = new EsiBaseResponse() {};

        when(fakeHttpResponse.headers()).thenReturn(mock(HttpHeaders.class));
        when(fakeHttpResponse.statusCode()).thenReturn(200);

        try {
            Object success = interpretEsiResponseMethod.invoke(esiService, fakeEsiResponse, fakeHttpResponse);
            assertTrue((Boolean) success);
        } catch(InvocationTargetException|RuntimeException e) {
            assertInstanceOf(RuntimeException.class, e.getCause(), "Http response has no headers");
        }

        assertFalse(fakeEsiResponse.hasError, "Has error reported");
        assertEquals(200, fakeEsiResponse.statusCode);
        assertTrue(fakeEsiResponse.contentModified);
    }
    // endregion
}
