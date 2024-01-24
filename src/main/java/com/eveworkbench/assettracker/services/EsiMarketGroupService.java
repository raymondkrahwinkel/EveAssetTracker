package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.mappers.EsiMarketGroupMapper;
import com.eveworkbench.assettracker.models.database.EsiEtagDto;
import com.eveworkbench.assettracker.models.database.EsiMarketGroupDto;
import com.eveworkbench.assettracker.models.esi.EsiBaseResponse;
import com.eveworkbench.assettracker.models.esi.types.market.MarketGroup;
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
public class EsiMarketGroupService extends EsiService {
    private final EsiMarketGroupRepository esiMarketGroupRepository;

    public EsiMarketGroupService(CharacterRepository characterRepository, HttpClientFactory httpClientFactory, EsiEtagRepository esiEtagRepository, EsiMarketGroupRepository esiMarketGroupRepository) {
        super(characterRepository, httpClientFactory, esiEtagRepository);
        this.esiMarketGroupRepository = esiMarketGroupRepository;
    }

    private List<EsiMarketGroupDto> existingDtos = Collections.synchronizedList(new ArrayList<>());

    public Boolean updateGroups() {
        System.out.println("Starting updateGroups");
        try {
            EsiBaseResponse<List<Integer>> response = new EsiBaseResponse<>() {
            };

            HttpResponse<String> httpResponse = requestGet("https://esi.evetech.net/latest/markets/groups/");
            if (!interpretEsiResponse(response, httpResponse)) {
                return false;
            }

            if (response.contentModified) {
                if(existingDtos.isEmpty()) {
                    // preload the existing items from the database
                    esiMarketGroupRepository.findAll().forEach(existingDtos::add);
                }

                // decode the incoming json
                List<Integer> responseIds = (new Gson()).fromJson(httpResponse.body(), new TypeToken<List<Integer>>() {}.getType());

                // get the esi etag dto
                EsiEtagDto listEtagDto = esiEtagRepository.findByEtagAndUrl(response.etag, httpResponse.uri().toString()).orElseThrow();

                int threadPoolSize = 1;
                AtomicInteger counter = new AtomicInteger();
                Collection<List<Integer>> chunks = responseIds.stream()
                        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / threadPoolSize))
                        .values();

                for(List<Integer> chunk : chunks) {
                    chunk.parallelStream().forEach(id -> {
                        System.out.println("Market group: " + id);
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
                // stop processing, nothing changed
                return true;
            }

            // remove all non-processed assets
            // todo: check local whats needs to be deleted, max number of supported ids to database is 65535
//            esiTypeRepository.deleteAllByIdNotIn(processedTypeIds);

            return true;
        } catch (RuntimeException e) {
            logger.error("Error while loading market group information from the ESI endpoint", e);
        }

        return false;
    }

    private boolean process(Integer id, EsiEtagDto eTag) throws URISyntaxException, ExecutionException, InterruptedException {
        EsiBaseResponse<MarketGroup> response = new EsiBaseResponse<>() {};
        HttpResponse<String> httpResponse = requestGet("https://esi.evetech.net/latest/markets/groups/" + id);
        if (!interpretEsiResponse(response, httpResponse)) {
            return false;
        }

        EsiMarketGroupDto dto = null;
        if(response.contentModified) {
            // get etag
            EsiEtagDto esiEtagDto = esiEtagRepository.findByEtagAndUrl(response.etag, httpResponse.uri().toString()).orElseThrow();

            // deserialize the received information
            MarketGroup decodedResponse = (new Gson()).fromJson(httpResponse.body(), new TypeToken<MarketGroup>() {}.getType());

            // get the type information from the database
            dto = existingDtos.stream().filter(t -> t.getId().equals(decodedResponse.market_group_id)).findFirst().orElse(new EsiMarketGroupDto());
            dto.setEsiEtag(esiEtagDto);
            dto.setEsiListEtag(eTag);

            if(decodedResponse.parent_group_id != null && decodedResponse.parent_group_id > 0) {
                Optional<EsiMarketGroupDto> parentDto = esiMarketGroupRepository.findById(decodedResponse.parent_group_id);
                if(parentDto.isEmpty()) {
                    if(process(decodedResponse.parent_group_id, eTag)) {
                        parentDto = esiMarketGroupRepository.findById(decodedResponse.parent_group_id);
                        parentDto.ifPresent(existingDtos::add);
                    }
                }

                parentDto.ifPresent(dto::setParent);
            }

            EsiMarketGroupMapper.INSTANCE.toDto(decodedResponse, dto);
        } else {
            // update the list etag only
            Optional<EsiMarketGroupDto> optionalDto = existingDtos.stream().filter(t -> t.getId().equals(id)).findFirst();
            if(optionalDto.isPresent()) {
                dto = optionalDto.get();
                dto.setEsiListEtag(eTag);
            }
        }

        if(dto != null) {
            esiMarketGroupRepository.save(dto);
        }

        return true;
    }
}
