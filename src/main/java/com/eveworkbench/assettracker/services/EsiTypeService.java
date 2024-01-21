package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.mappers.EsiTypeMapper;
import com.eveworkbench.assettracker.models.database.EsiEtagDto;
import com.eveworkbench.assettracker.models.database.EsiTypeDto;
import com.eveworkbench.assettracker.models.esi.EsiBaseResponse;
import com.eveworkbench.assettracker.models.esi.types.universe.Type;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.EsiEtagRepository;
import com.eveworkbench.assettracker.repositories.EsiTypeRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class EsiTypeService extends EsiService {
    private final EsiTypeRepository esiTypeRepository;

    public EsiTypeService(CharacterRepository characterRepository, HttpClientFactory httpClientFactory, EsiEtagRepository esiEtagRepository, EsiTypeRepository esiTypeRepository) {
        super(characterRepository, httpClientFactory, esiEtagRepository);
        this.esiTypeRepository = esiTypeRepository;
    }

    public Boolean updateTypesFromEsi() {
        try {
            List<Integer> processedTypeIds = new ArrayList<>();
            int maxPages = 1;
            for(int page = 1; page <= maxPages; page++) {
                EsiBaseResponse<List<Integer>> response = new EsiBaseResponse<>() {
                };
                HttpRequest request = getAnonymousHttpRequestBuilder("https://esi.evetech.net/latest/universe/types/?page=" + page)
                        .GET()
                        .build();

                HttpResponse<String> httpResponse = httpClientFactory
                        .create()
                        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .get();

                if (!interpretEsiResponse(response, httpResponse)) {
                    return false;
                }

                maxPages = response.pages;
                if (response.contentModified) {
                    // decode the incoming json
                    List<Integer> responseTypeIds = (new Gson()).fromJson(httpResponse.body(), new TypeToken<List<Integer>>() {}.getType());

                    // mark all the types as processed
                    processedTypeIds.addAll(esiTypeRepository.findByEsiEtag_Etag(response.etag).stream().map(EsiTypeDto::getId).toList());

                    // get the esi etag dto
                    EsiEtagDto listEtagDto = esiEtagRepository.findByEtagAndUrl(response.etag, request.uri().toString()).orElseThrow();
                    for(Integer typeId : responseTypeIds) {
                        if(!processType(typeId, listEtagDto)) {
                            // todo: handle error
                        }
                    }
                } else {
                    // get the type ids based on the etag
                    processedTypeIds.addAll(esiTypeRepository.findByEsiListEtag_Etag(response.etag).stream().map(EsiTypeDto::getId).toList());
                }
            }

            // remove all non-processed assets
            esiTypeRepository.deleteAllByIdNotIn(processedTypeIds);

            return true;
        } catch (URISyntaxException | InterruptedException | ExecutionException e) {
            logger.error("Error while loading type information from the ESI endpoint", e);
        }

        return false;
    }

    private boolean processType(Integer typeId, EsiEtagDto eTag) throws URISyntaxException, ExecutionException, InterruptedException {
        EsiBaseResponse<Type> response = new EsiBaseResponse<>() {};

        HttpRequest request = getAnonymousHttpRequestBuilder("https://esi.evetech.net/latest/universe/types/" + typeId)
                .GET()
                .build();

        HttpResponse<String> httpResponse = httpClientFactory
                .create()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .get();

        if (!interpretEsiResponse(response, httpResponse)) {
            return false;
        }

        if(response.contentModified) {
            // get etag
            EsiEtagDto esiEtagDto = esiEtagRepository.findByEtagAndUrl(response.etag, request.uri().toString()).orElseThrow();

            // deserialize the received information
            Type responseType = (new Gson()).fromJson(httpResponse.body(), new TypeToken<Type>() {}.getType());

            // get the type information from the database
            EsiTypeDto typeDto = esiTypeRepository.findById(typeId).orElse(new EsiTypeDto());
            typeDto.setEsiEtag(esiEtagDto);
            typeDto.setEsiListEtag(eTag);
            EsiTypeMapper.INSTANCE.toDto(responseType, typeDto);
            esiTypeRepository.save(typeDto);
        }

        return true;
    }
}
