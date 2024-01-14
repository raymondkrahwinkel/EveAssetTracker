package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.esi.WalletResponse;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.EsiEtagRepository;
import com.eveworkbench.assettracker.repositories.WalletHistoryRepository;
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

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(locations="classpath:application-test.properties")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // this is needed because we have a base setup for the httpClient and one test that doesn't use httpClient
public class EsiWalletServiceTests {
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

    @Mock
    private WalletHistoryRepository walletHistoryRepository;

    private EsiWalletService esiWalletService;

    @BeforeEach
    public void setup() {
        esiWalletService = new EsiWalletService(characterRepository, httpClientFactory, esiEtagRepository, walletHistoryRepository);

        when(httpClientFactory.create()).thenReturn(httpClient);

        ReflectionTestUtils.setField(esiWalletService, "clientId", "test");
        ReflectionTestUtils.setField(esiWalletService, "clientSecret", "test");
    }

    @Test
    void getWallet_characterNotFound_Test() {
        // test not found character based on id
        Throwable characterNotFoundException = assertThrows(RuntimeException.class, () -> esiWalletService.getWalletBalance(1));
        assertEquals(characterNotFoundException.getMessage(), "Cannot get character with id: 1");
    }

    @Test
    void getWallet_esiTokenInvalid_Test() {
        // create fake character
        CharacterDto characterDto = new CharacterDto();
        characterDto.setId(1);

        // test invalid esi token data
        Optional<WalletResponse> esiTokenInvalidResponse = esiWalletService.getWalletBalance(characterDto);
        assertFalse(esiTokenInvalidResponse.isEmpty(), "Received response");
        assertTrue(esiTokenInvalidResponse.get().hasError, "Error flag has been set");
        assertEquals("No valid access token available for character", esiTokenInvalidResponse.get().error);

        // test expired token
        characterDto.setAccessToken("testAccessToken");
        characterDto.setRefreshToken("testRefreshToken");
        characterDto.setTokenExpiresAt(Date.from(Instant.parse("2024-01-01T00:00:00Z")));

        esiTokenInvalidResponse = esiWalletService.getWalletBalance(characterDto);
        assertFalse(esiTokenInvalidResponse.isEmpty(), "Received response");
        assertTrue(esiTokenInvalidResponse.get().hasError, "Error flag has been set");
        assertEquals("No valid access token available for character", esiTokenInvalidResponse.get().error);
    }

    @Test
    void getWallet_valid_Test() {
        // todo: test esi response codes and handling (interpretEsiResponse)

        // create fake character
        CharacterDto characterDto = new CharacterDto();
        characterDto.setId(1);
        characterDto.setAccessToken("testAccessToken");
        characterDto.setRefreshToken("testRefreshToken");
        characterDto.setTokenExpiresAt(Date.from(Instant.now().plusSeconds(120)));

        // Create a mock response
        HttpResponse<String> mockResponse = new HttpResponse<>() {
            @Override
            public int statusCode() {
                return 200;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return mock(HttpHeaders.class);
            }

            @Override
            public String body() {
                return "20000";
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

        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        Optional<WalletResponse> walletResponse = esiWalletService.getWalletBalance(characterDto);
        assertFalse(walletResponse.isEmpty());
        assertFalse(walletResponse.get().hasError);
        assertEquals(20000.0, walletResponse.get().value);
        assertEquals(0,walletResponse.get().difference);
    }
}
