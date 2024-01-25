package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.TestUtils;
import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.models.database.EsiMarketGroupDto;
import com.eveworkbench.assettracker.models.database.EsiTypeCategoryDto;
import com.eveworkbench.assettracker.models.database.EsiTypeDto;
import com.eveworkbench.assettracker.models.database.EsiTypeGroupDto;
import com.eveworkbench.assettracker.repositories.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(locations="classpath:application-test.properties")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // this is needed because we have a base setup for the httpClient and one test that doesn't use httpClient
public class EsiTypeServiceTests {
    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpClientFactory httpClientFactory;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private EsiEtagRepository esiEtagRepository;

    @Mock
    private EsiTypeRepository esiTypeRepository;

    @Mock
    private EsiTypeGroupRepository esiTypeGroupRepository;

    @Mock
    private EsiMarketGroupRepository esiMarketGroupRepository;

    @BeforeEach
    public void setup() {
        when(httpClientFactory.create()).thenReturn(httpClient);
    }

    @Test
    void updateTypes_invalid_Test() {
        EsiTypeService esiTypeService = spy(new EsiTypeService(characterRepository, httpClientFactory, esiEtagRepository, esiTypeRepository, esiTypeGroupRepository, esiMarketGroupRepository));

        doReturn(TestUtils.mockHttpResponse("https://esi.evetech.net/latest/universe/types/?page=1", "[]", 500))
                .when(esiTypeService)
                .requestGet(anyString());

        assertFalse(esiTypeService::updateTypesFromEsi);
        verify(esiTypeService).requestGet("https://esi.evetech.net/latest/universe/types/?page=1");
    }

    @Test
    void updateTypes_valid_Test() {
        List<EsiTypeDto> storedItems = Collections.synchronizedList(new ArrayList<>());

        when(esiTypeGroupRepository.findById(anyInt())).thenAnswer(invocationOnMock -> {
            Integer id = invocationOnMock.getArgument(0, Integer.class);
            EsiTypeGroupDto dtoMock = new EsiTypeGroupDto();
            dtoMock.setId(id);
            return Optional.of(dtoMock);
        });

        when(esiMarketGroupRepository.findById(anyInt())).thenAnswer(invocationOnMock -> {
            Integer id = invocationOnMock.getArgument(0, Integer.class);
            EsiMarketGroupDto dtoMock = new EsiMarketGroupDto();
            dtoMock.setId(id);
            return Optional.of(dtoMock);
        });

        when(esiTypeRepository.save(any())).thenAnswer((invocationOnMock -> {
            var value = invocationOnMock.getArgument(0, EsiTypeDto.class);
            storedItems.add(value);
            return value;
        }));

        EsiTypeService esiTypeService = spy(new EsiTypeService(characterRepository, httpClientFactory, esiEtagRepository, esiTypeRepository, esiTypeGroupRepository, esiMarketGroupRepository));
        doAnswer(invocationOnMock -> {
            String url = invocationOnMock.getArgument(0, String.class);

            return switch (url) {
                case "https://esi.evetech.net/latest/universe/types/?page=1" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/universe/type/list.json").toURI())), 200);
                case "https://esi.evetech.net/latest/universe/types/4" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/universe/type/4.json").toURI())), 200);
                case "https://esi.evetech.net/latest/universe/types/35250" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/universe/type/35250.json").toURI())), 200);
                case "https://esi.evetech.net/latest/universe/types/35251" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/universe/type/35251.json").toURI())), 200);
                default -> TestUtils.mockHttpResponse(url, "[]", 500);
            };

        }).when(esiTypeService).requestGet(anyString());
        assertTrue(esiTypeService::updateTypesFromEsi);
        verify(esiTypeService).requestGet("https://esi.evetech.net/latest/universe/types/?page=1");

        // verify the number of items in the database
        assertEquals(3, storedItems.size(), "Validate number of items stored");

        // check the contents of item 35250
        EsiTypeDto type = storedItems.stream().filter(t -> t.getId().equals(35250)).findFirst().orElseThrow();
        assertEquals("Kronos Police SKIN (30 Days)", type.getName());
    }
}
