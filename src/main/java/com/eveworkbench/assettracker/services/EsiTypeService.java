package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.mappers.EsiTypeMapper;
import com.eveworkbench.assettracker.models.database.EsiEtagDto;
import com.eveworkbench.assettracker.models.database.EsiMarketGroupDto;
import com.eveworkbench.assettracker.models.database.EsiTypeDto;
import com.eveworkbench.assettracker.models.database.EsiTypeGroupDto;
import com.eveworkbench.assettracker.models.esi.EsiBaseResponse;
import com.eveworkbench.assettracker.models.esi.types.universe.Type;
import com.eveworkbench.assettracker.repositories.*;
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
public class EsiTypeService extends EsiService {
    private final EsiTypeRepository esiTypeRepository;
    private final EsiTypeGroupRepository esiTypeGroupRepository;
    private final EsiMarketGroupRepository esiMarketGroupRepository;

    public EsiTypeService(CharacterRepository characterRepository, HttpClientFactory httpClientFactory, EsiEtagRepository esiEtagRepository, EsiTypeRepository esiTypeRepository, EsiTypeGroupRepository esiTypeGroupRepository, EsiMarketGroupRepository esiMarketGroupRepository) {
        super(characterRepository, httpClientFactory, esiEtagRepository);
        this.esiTypeRepository = esiTypeRepository;
        this.esiTypeGroupRepository = esiTypeGroupRepository;
        this.esiMarketGroupRepository = esiMarketGroupRepository;
    }

    private List<EsiTypeDto> existingTypes = Collections.synchronizedList(new ArrayList<>());

    public Boolean updateTypesFromEsi() {
        try {
            List<Integer> processedTypeIds = new ArrayList<>();

            int maxPages = 1;
            for(int page = 1; page <= maxPages; page++) {
                System.out.println("ESI Types: page " + page + "/" + maxPages);

                EsiBaseResponse<List<Integer>> response = new EsiBaseResponse<>() {
                };

                HttpResponse<String> httpResponse = requestGet("https://esi.evetech.net/latest/universe/types/?page=" + page);
                if (!interpretEsiResponse(response, httpResponse)) {
                    return false;
                }

                maxPages = response.pages;
                if (response.contentModified) {
                    if(existingTypes.isEmpty()) {
                        // preload the existing items from the database
                        esiTypeRepository.findAll().forEach(existingTypes::add);
                    }

                    // decode the incoming json
                    List<Integer> responseTypeIds = (new Gson()).fromJson(httpResponse.body(), new TypeToken<List<Integer>>() {}.getType());

                    // mark all the types as processed
//                    processedTypeIds.addAll(esiTypeRepository.findByEsiListEtag_Etag(response.etag).stream().map(EsiTypeDto::getId).toList());

                    // get the esi etag dto
                    EsiEtagDto listEtagDto = esiEtagRepository.findTopByEtagAndUrl(response.etag, httpResponse.uri().toString()).orElseThrow();

                    int threadPoolSize = 5;
                    AtomicInteger counter = new AtomicInteger();
                    Collection<List<Integer>> chunks = responseTypeIds.stream()
                            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / threadPoolSize))
                            .values();

                    for(List<Integer> chunk : chunks) {
                        chunk.parallelStream().forEach(typeId -> {
                            System.out.println("Type: " + typeId);
                            try {
                                if(!processType(typeId, listEtagDto)) {
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
                    processedTypeIds.addAll(esiTypeRepository.findByEsiListEtag_Etag(response.etag).stream().map(EsiTypeDto::getId).toList());
                }
            }

            // remove all non-processed assets
            // todo: check local whats needs to be deleted, max number of supported ids to database is 65535
//            esiTypeRepository.deleteAllByIdNotIn(processedTypeIds);

            return true;
        } catch (RuntimeException e) {
            logger.error("Error while loading type information from the ESI endpoint", e);
        }

        return false;
    }

    private boolean processType(Integer typeId, EsiEtagDto eTag) throws URISyntaxException, ExecutionException, InterruptedException {
        EsiBaseResponse<Type> response = new EsiBaseResponse<>() {};
        HttpResponse<String> httpResponse = requestGet("https://esi.evetech.net/latest/universe/types/" + typeId);
        if (!interpretEsiResponse(response, httpResponse)) {
            return false;
        }

        EsiTypeDto typeDto = null;
        if(response.contentModified) {
            // get etag
            EsiEtagDto esiEtagDto = esiEtagRepository.findTopByEtagAndUrl(response.etag, httpResponse.uri().toString()).orElseThrow();

            // deserialize the received information
            Type responseType = (new Gson()).fromJson(httpResponse.body(), new TypeToken<Type>() {}.getType());

            // get the type information from the database
            typeDto = existingTypes.stream().filter(t -> t.getId().equals(responseType.type_id)).findFirst().orElse(new EsiTypeDto());
            typeDto.setEsiEtag(esiEtagDto);
            typeDto.setEsiListEtag(eTag);

            // only update the group information when not set or changed
            if(typeDto.getGroup() == null || !typeDto.getGroup().getId().equals(responseType.group_id)) {
                // get the group
                EsiTypeGroupDto group = esiTypeGroupRepository.findById(responseType.group_id).orElseThrow();
                typeDto.setGroup(group);
            }

            // only update the group information when not set or changed
            if(responseType.market_group_id > 0 && (typeDto.getMarketGroup() == null || !typeDto.getMarketGroup().getId().equals(responseType.market_group_id))) {
                if(esiMarketGroupRepository.findById(responseType.market_group_id).isEmpty()) {
                    var tt = responseType.market_group_id;
                }

                // get the market group
                EsiMarketGroupDto marketGroup = esiMarketGroupRepository.findById(responseType.market_group_id).orElseThrow();
                typeDto.setMarketGroup(marketGroup);
            }

            EsiTypeMapper.INSTANCE.toDto(responseType, typeDto);
        } else {
            // update the list etag only
            Optional<EsiTypeDto> optionalTypeDto = existingTypes.stream().filter(t -> t.getId().equals(typeId)).findFirst();
            if(optionalTypeDto.isPresent()) {
                typeDto = optionalTypeDto.get();
                typeDto.setEsiListEtag(eTag);
            }
        }

        if(typeDto != null) {
            esiTypeRepository.save(typeDto);
        }

        return true;
    }
}
