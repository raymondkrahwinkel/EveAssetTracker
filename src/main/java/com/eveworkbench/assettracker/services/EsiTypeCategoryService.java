package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.mappers.EsiTypeCategoryMapper;
import com.eveworkbench.assettracker.models.database.EsiEtagDto;
import com.eveworkbench.assettracker.models.database.EsiTypeCategoryDto;
import com.eveworkbench.assettracker.models.esi.EsiBaseResponse;
import com.eveworkbench.assettracker.models.esi.types.universe.Type;
import com.eveworkbench.assettracker.models.esi.types.universe.TypeCategory;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.EsiEtagRepository;
import com.eveworkbench.assettracker.repositories.EsiTypeCategoryRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class EsiTypeCategoryService extends EsiService {
    private final EsiTypeCategoryRepository esiTypeCategoryRepository;

    public EsiTypeCategoryService(CharacterRepository characterRepository, HttpClientFactory httpClientFactory, EsiEtagRepository esiEtagRepository, EsiTypeCategoryRepository esiTypeCategoryRepository) {
        super(characterRepository, httpClientFactory, esiEtagRepository);
        this.esiTypeCategoryRepository = esiTypeCategoryRepository;
    }

    public Boolean updateCategories() {
        try {
            EsiBaseResponse<List<Integer>> response = new EsiBaseResponse<>() {
            };

            HttpResponse<String> httpResponse = requestGet("https://esi.evetech.net/latest/universe/categories/");
            if (!interpretEsiResponse(response, httpResponse)) {
                return false;
            }

            if (response.contentModified) {
                // decode the incoming json
                List<Integer> responseIds = (new Gson()).fromJson(httpResponse.body(), new TypeToken<List<Integer>>() {}.getType());

                // get the esi etag dto
                EsiEtagDto listEtagDto = esiEtagRepository.findByEtagAndUrl(response.etag, httpResponse.uri().toString()).orElseThrow();

                int threadPoolSize = 5;
                AtomicInteger counter = new AtomicInteger();
                Collection<List<Integer>> chunks = responseIds.stream()
                        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / threadPoolSize))
                        .values();

                for(List<Integer> chunk : chunks) {
                    chunk.parallelStream().forEach(id -> {
                        System.out.println("Type category: " + id);
                        try {
                            if (!process(id, listEtagDto)) {
                                var re = response;
                                // todo: handle error
                            }
                        } catch (URISyntaxException | ExecutionException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        // create 1 second delay
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

                return true;
            } else {
                // stop processing, nothing changed
                return true;
            }
        } catch (RuntimeException e) {
            logger.error("Error while loading type category information from the ESI endpoint", e);
        }

        return false;
    }

    private boolean process(Integer categoryId, EsiEtagDto eTag) throws URISyntaxException, ExecutionException, InterruptedException {
        EsiBaseResponse<Type> response = new EsiBaseResponse<>() {};
        HttpResponse<String> httpResponse = requestGet("https://esi.evetech.net/latest/universe/categories/" + categoryId);
        if (!interpretEsiResponse(response, httpResponse)) {
            return false;
        }

        EsiTypeCategoryDto dto = null;
        Optional<EsiTypeCategoryDto> optionalTypeDto = esiTypeCategoryRepository.findById(categoryId);

        if(response.contentModified) {
            // get etag
            EsiEtagDto esiEtagDto = esiEtagRepository.findByEtagAndUrl(response.etag, httpResponse.uri().toString()).orElseThrow();

            // deserialize the received information
            TypeCategory responseType = (new Gson()).fromJson(httpResponse.body(), new TypeToken<TypeCategory>() {}.getType());

            // get the type information from the database
            dto = optionalTypeDto.orElse(new EsiTypeCategoryDto());
            dto.setEsiEtag(esiEtagDto);
            dto.setEsiListEtag(eTag);
            EsiTypeCategoryMapper.INSTANCE.toDto(responseType, dto);
        } else {
            // update the list etag only
            if(optionalTypeDto.isPresent()) {
                dto = optionalTypeDto.get();
                dto.setEsiListEtag(eTag);
            }
        }

        if(dto != null) {
            esiTypeCategoryRepository.save(dto);
        }

        return true;
    }
}
