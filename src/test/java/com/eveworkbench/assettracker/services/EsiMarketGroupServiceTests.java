package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.TestUtils;
import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.EsiEtagRepository;
import com.eveworkbench.assettracker.repositories.EsiMarketGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ResourceUtils;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(locations="classpath:application-test.properties")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // this is needed because we have a base setup for the httpClient and one test that doesn't use httpClient
public class EsiMarketGroupServiceTests {
    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpClientFactory httpClientFactory;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private EsiEtagRepository esiEtagRepository;

    @Autowired
    private EsiMarketGroupRepository esiMarketGroupRepository;

    @BeforeEach
    public void setup() {
        when(httpClientFactory.create()).thenReturn(httpClient);
    }

    @Test
    void updateGroups_invalid_Test() {
        EsiMarketGroupService esiMarketGroupService = spy(new EsiMarketGroupService(characterRepository, httpClientFactory, esiEtagRepository, esiMarketGroupRepository));

        doReturn(TestUtils.mockHttpResponse("https://esi.evetech.net/latest/markets/groups/", "[]", 500))
                .when(esiMarketGroupService)
                .requestGet(anyString());

        assertFalse(esiMarketGroupService::updateGroups);
        verify(esiMarketGroupService).requestGet("https://esi.evetech.net/latest/markets/groups/");
    }

    @Test
    void updateGroups_valid_Test() {
        EsiMarketGroupService esiMarketGroupService = spy(new EsiMarketGroupService(characterRepository, httpClientFactory, esiEtagRepository, esiMarketGroupRepository));

        doAnswer(invocationOnMock -> {
            String url = invocationOnMock.getArgument(0, String.class);

            return switch (url) {
                case "https://esi.evetech.net/latest/markets/groups/" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/market/group/list.json").toURI())), 200);
                case "https://esi.evetech.net/latest/markets/groups/2" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/market/group/2.json").toURI())), 200);
                case "https://esi.evetech.net/latest/markets/groups/4" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/market/group/4.json").toURI())), 200);
                case "https://esi.evetech.net/latest/markets/groups/5" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/market/group/5.json").toURI())), 200);
                default -> TestUtils.mockHttpResponse(url, "[]", 500);
            };

        }).when(esiMarketGroupService).requestGet(anyString());
        assertTrue(esiMarketGroupService::updateGroups);
        verify(esiMarketGroupService).requestGet("https://esi.evetech.net/latest/markets/groups/");

        // verify the number of items in the database
        assertEquals(3, esiMarketGroupRepository.count(), "Validate number of items stored");
    }
}
