package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.mappers.EsiTypeGroupMapper;
import com.eveworkbench.assettracker.models.database.EsiEtagDto;
import com.eveworkbench.assettracker.models.database.EsiTypeCategoryDto;
import com.eveworkbench.assettracker.models.database.EsiTypeGroupDto;
import com.eveworkbench.assettracker.models.esi.EsiBaseResponse;
import com.eveworkbench.assettracker.models.esi.types.universe.TypeGroup;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.EsiEtagRepository;
import com.eveworkbench.assettracker.repositories.EsiTypeCategoryRepository;
import com.eveworkbench.assettracker.repositories.EsiTypeGroupRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class EsiTypeGroupService extends EsiService {
    private final EsiTypeGroupRepository esiTypeGroupRepository;
    private final EsiTypeCategoryRepository esiTypeCategoryRepository;

    public EsiTypeGroupService(CharacterRepository characterRepository, HttpClientFactory httpClientFactory, EsiEtagRepository esiEtagRepository, EsiTypeGroupRepository esiTypeGroupRepository, EsiTypeCategoryRepository esiTypeCategoryRepository) {
        super(characterRepository, httpClientFactory, esiEtagRepository);
        this.esiTypeGroupRepository = esiTypeGroupRepository;
        this.esiTypeCategoryRepository = esiTypeCategoryRepository;
    }

    private List<EsiTypeGroupDto> existingDtos = Collections.synchronizedList(new ArrayList<>());

    public Boolean updateGroups() {
        try {
            List<Integer> processedIds = new ArrayList<>();

            int maxPages = 1;
            for(int page = 1; page <= maxPages; page++) {
                System.out.println("ESI Type group: page " + page + "/" + maxPages);

                EsiBaseResponse<List<Integer>> response = new EsiBaseResponse<>() {
                };

                HttpResponse<String> httpResponse = requestGet("https://esi.evetech.net/latest/universe/groups/?page=" + page);
                if (!interpretEsiResponse(response, httpResponse)) {
                    return false;
                }

                maxPages = response.pages;
                if (response.contentModified) {
                    if(existingDtos.isEmpty()) {
                        // preload the existing items from the database
                        esiTypeGroupRepository.findAll().forEach(existingDtos::add);
                    }

                    // decode the incoming json
                    List<Integer> responseTypeIds = (new Gson()).fromJson(httpResponse.body(), new TypeToken<List<Integer>>() {}.getType());

                    // mark all the types as processed
//                    processedTypeIds.addAll(esiTypeRepository.findByEsiListEtag_Etag(response.etag).stream().map(EsiTypeDto::getId).toList());

                    // get the esi etag dto
                    EsiEtagDto listEtagDto = esiEtagRepository.findByEtagAndUrl(response.etag, httpResponse.uri().toString()).orElseThrow();

                    int threadPoolSize = 5;
                    AtomicInteger counter = new AtomicInteger();
                    Collection<List<Integer>> chunks = responseTypeIds.stream()
                            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / threadPoolSize))
                            .values();

                    for(List<Integer> chunk : chunks) {
                        chunk.parallelStream().forEach(id -> {
                            System.out.println("Type group: " + id);
                            try {
                                if(!process(id, listEtagDto)) {
                                    var re = response;
                                    // todo: handle error
                                }
                            } catch (URISyntaxException|ExecutionException|InterruptedException e) {
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
                } else {
                    // check if this is the first page
                    if(page == 1) {
                        // stop processing, nothing changed
                        return true;
                    }

                    // get the type ids based on the etag
                    processedIds.addAll(esiTypeGroupRepository.findByEsiListEtag_Etag(response.etag).stream().map(EsiTypeGroupDto::getId).toList());
                }
            }

            // remove all non-processed assets
            // todo: check local whats needs to be deleted, max number of supported ids to database is 65535
//            esiTypeRepository.deleteAllByIdNotIn(processedTypeIds);

            return true;
        } catch (RuntimeException e) {
            logger.error("Error while loading type group information from the ESI endpoint", e);
        }

        return false;
    }

    private boolean process(Integer id, EsiEtagDto eTag) throws URISyntaxException, ExecutionException, InterruptedException {
        EsiBaseResponse<TypeGroup> response = new EsiBaseResponse<>() {};
        HttpResponse<String> httpResponse = requestGet("https://esi.evetech.net/latest/universe/groups/" + id);
        if (!interpretEsiResponse(response, httpResponse)) {
            return false;
        }

        EsiTypeGroupDto dto = null;
        if(response.contentModified) {
            // get etag
            EsiEtagDto esiEtagDto = esiEtagRepository.findByEtagAndUrl(response.etag, httpResponse.uri().toString()).orElseThrow();

            // deserialize the received information
            TypeGroup decodedResponse = (new Gson()).fromJson(httpResponse.body(), new TypeToken<TypeGroup>() {}.getType());

            // get the type information from the database
            dto = existingDtos.stream().filter(t -> t.getId().equals(decodedResponse.group_id)).findFirst().orElse(new EsiTypeGroupDto());
            dto.setEsiEtag(esiEtagDto);
            dto.setEsiListEtag(eTag);

            // only update the category information when not set or changed
            if(dto.getCategory() == null || !dto.getCategory().getId().equals(decodedResponse.category_id)) {
                // get the category
                EsiTypeCategoryDto category = esiTypeCategoryRepository.findById(decodedResponse.category_id).orElseThrow();
                dto.setCategory(category);
            }

            EsiTypeGroupMapper.INSTANCE.toDto(decodedResponse, dto);
        } else {
            // update the list etag only
            Optional<EsiTypeGroupDto> optionalDto = existingDtos.stream().filter(t -> t.getId().equals(id)).findFirst();
            if(optionalDto.isPresent()) {
                dto = optionalDto.get();
                dto.setEsiListEtag(eTag);
            }
        }

        if(dto != null) {
            esiTypeGroupRepository.save(dto);
        }

        return true;
    }
}
