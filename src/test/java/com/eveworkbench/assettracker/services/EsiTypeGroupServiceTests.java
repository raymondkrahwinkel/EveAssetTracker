package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.TestUtils;
import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.models.database.EsiTypeCategoryDto;
import com.eveworkbench.assettracker.models.database.EsiTypeGroupDto;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.EsiEtagRepository;
import com.eveworkbench.assettracker.repositories.EsiTypeCategoryRepository;
import com.eveworkbench.assettracker.repositories.EsiTypeGroupRepository;
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
public class EsiTypeGroupServiceTests {
    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpClientFactory httpClientFactory;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private EsiEtagRepository esiEtagRepository;

    @Mock
    private EsiTypeGroupRepository esiTypeGroupRepository;

    @Mock
    private EsiTypeCategoryRepository esiTypeCategoryRepository;

    @BeforeEach
    public void setup() {
        when(httpClientFactory.create()).thenReturn(httpClient);
    }

    @Test
    void updateGroups_invalid_Test() {
        EsiTypeGroupService esiTypeGroupService = spy(new EsiTypeGroupService(characterRepository, httpClientFactory, esiEtagRepository, esiTypeGroupRepository, esiTypeCategoryRepository));

        doReturn(TestUtils.mockHttpResponse("https://esi.evetech.net/latest/universe/groups/?page=1", "[]", 500))
                .when(esiTypeGroupService)
                .requestGet(anyString());

        assertFalse(esiTypeGroupService::updateGroups);
        verify(esiTypeGroupService).requestGet("https://esi.evetech.net/latest/universe/groups/?page=1");
    }

    @Test
    void updateGroups_valid_Test() {
        List<EsiTypeGroupDto> storedItems = Collections.synchronizedList(new ArrayList<>());

        when(esiTypeCategoryRepository.findById(anyInt())).thenAnswer(invocationOnMock -> {
            Integer id = invocationOnMock.getArgument(0, Integer.class);
            EsiTypeCategoryDto dtoMock = new EsiTypeCategoryDto();
            dtoMock.setId(id);
            return Optional.of(dtoMock);
        });

        when(esiTypeGroupRepository.save(any())).thenAnswer((invocationOnMock -> {
            var value = invocationOnMock.getArgument(0, EsiTypeGroupDto.class);
            storedItems.add(value);
            return value;
        }));

        EsiTypeGroupService esiTypeGroupService = spy(new EsiTypeGroupService(characterRepository, httpClientFactory, esiEtagRepository, esiTypeGroupRepository, esiTypeCategoryRepository));
        doAnswer(invocationOnMock -> {
            String url = invocationOnMock.getArgument(0, String.class);

            return switch (url) {
                case "https://esi.evetech.net/latest/universe/groups/?page=1" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/universe/group/list.json").toURI())), 200);
                case "https://esi.evetech.net/latest/universe/groups/0" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/universe/group/0.json").toURI())), 200);
                case "https://esi.evetech.net/latest/universe/groups/1" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/universe/group/1.json").toURI())), 200);
                case "https://esi.evetech.net/latest/universe/groups/2" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/universe/group/2.json").toURI())), 200);
                default -> TestUtils.mockHttpResponse(url, "[]", 500);
            };

        }).when(esiTypeGroupService).requestGet(anyString());
        assertTrue(esiTypeGroupService::updateGroups);
        verify(esiTypeGroupService).requestGet("https://esi.evetech.net/latest/universe/groups/?page=1");

        // verify the number of items in the database
        assertEquals(3, storedItems.size(), "Validate number of items stored");
    }
}
