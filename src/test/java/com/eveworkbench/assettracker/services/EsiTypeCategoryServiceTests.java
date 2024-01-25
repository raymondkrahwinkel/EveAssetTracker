package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.TestUtils;
import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.models.database.EsiTypeCategoryDto;
import com.eveworkbench.assettracker.models.database.EsiTypeGroupDto;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.EsiEtagRepository;
import com.eveworkbench.assettracker.repositories.EsiTypeCategoryRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(locations="classpath:application-test.properties")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // this is needed because we have a base setup for the httpClient and one test that doesn't use httpClient
public class EsiTypeCategoryServiceTests {
    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpClientFactory httpClientFactory;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private EsiEtagRepository esiEtagRepository;

    @Mock
    private EsiTypeCategoryRepository esiTypeCategoryRepository;

    @BeforeEach
    public void setup() {
        when(httpClientFactory.create()).thenReturn(httpClient);
    }

    @Test
    void updateCategories_invalid_Test() {
        EsiTypeCategoryService esiTypeCategoryService = spy(new EsiTypeCategoryService(characterRepository, httpClientFactory, esiEtagRepository, esiTypeCategoryRepository));

        doReturn(TestUtils.mockHttpResponse("https://esi.evetech.net/latest/universe/categories/", "[]", 500))
                .when(esiTypeCategoryService)
                .requestGet(anyString());

        assertFalse(esiTypeCategoryService::updateCategories);
        verify(esiTypeCategoryService).requestGet("https://esi.evetech.net/latest/universe/categories/");
    }

    @Test
    void updateCategories_valid_Test() {
        List<EsiTypeCategoryDto> storedItems = new ArrayList<>();
        when(esiTypeCategoryRepository.save(any())).thenAnswer((invocationOnMock -> {
            var value = invocationOnMock.getArgument(0, EsiTypeCategoryDto.class);
            storedItems.add(value);
            return value;
        }));

        EsiTypeCategoryService esiTypeCategoryService = spy(new EsiTypeCategoryService(characterRepository, httpClientFactory, esiEtagRepository, esiTypeCategoryRepository));

        doAnswer(invocationOnMock -> {
            String url = invocationOnMock.getArgument(0, String.class);

            return switch (url) {
                case "https://esi.evetech.net/latest/universe/categories/" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/universe/category/list.json").toURI())), 200);
                case "https://esi.evetech.net/latest/universe/categories/0" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/universe/category/0.json").toURI())), 200);
                case "https://esi.evetech.net/latest/universe/categories/23" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/universe/category/23.json").toURI())), 200);
                case "https://esi.evetech.net/latest/universe/categories/46" ->
                        TestUtils.mockHttpResponse(url, Files.readString(Paths.get(ResourceUtils.getFile("classpath:esi/universe/category/46.json").toURI())), 200);
                default -> TestUtils.mockHttpResponse(url, "[]", 500);
            };

        }).when(esiTypeCategoryService).requestGet(anyString());
        assertTrue(esiTypeCategoryService::updateCategories);
        verify(esiTypeCategoryService).requestGet("https://esi.evetech.net/latest/universe/categories/");

        // verify the number of items in the database
        assertEquals(3, storedItems.size(), "Validate number of items stored");
    }
}
