package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.factories.HttpClientFactory;
import com.eveworkbench.assettracker.models.database.CharacterAssetDto;
import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.database.EsiEtagDto;
import com.eveworkbench.assettracker.models.esi.AssetResponse;
import com.eveworkbench.assettracker.repositories.CharacterAssetRepository;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.EsiEtagRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class EsiAssetService extends EsiService {
    private final CharacterAssetRepository characterAssetRepository;

    public EsiAssetService(CharacterRepository characterRepository, HttpClientFactory httpClientFactory, EsiEtagRepository esiEtagRepository, CharacterAssetRepository characterAssetRepository) {
        super(characterRepository, httpClientFactory, esiEtagRepository);
        this.characterAssetRepository = characterAssetRepository;
    }

    public Optional<AssetResponse> getAssetsForCharacter(Integer characterId) {
        var character = characterRepository.findById(characterId);
        if(character.isEmpty()) {
            throw new RuntimeException("Cannot get character with id: " + characterId);
        }

        return getAssetsForCharacter(character.get());
    }

    public Optional<AssetResponse> getAssetsForCharacter(CharacterDto character) {
        try {
            AssetResponse response = new AssetResponse();

            // check if the access and refresh token are set and not expired
            if(character.getAccessToken() == null || character.getAccessToken().isEmpty() || character.getRefreshToken() == null || character.getRefreshToken().isEmpty() || character.getTokenExpiresAt() == null || character.getTokenExpiresAt().before(new java.util.Date())) {
                response.hasError = true;
                response.error = "No valid access token available for character";
                return Optional.of(response);
            }

            int maxPages = 1;
            List<String> eTags = new ArrayList<>();
            for(int pageNumber = 1; pageNumber <= maxPages; pageNumber++) {
                HttpRequest request = getBaseCharacterHttpRequestBuilder("https://esi.evetech.net/latest/characters/" + character.getId() + "/assets/?page=" + pageNumber, character.getAccessToken())
                        .GET()
                        .build();

                HttpResponse<String> httpResponse = httpClientFactory
                        .create()
                        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .get();

                if (!interpretEsiResponse(response, httpResponse)) {
                    return Optional.of(response);
                }

                eTags.add(response.etag);

                maxPages = response.pages;
                if(response.value == null) {
                    response.value = new ArrayList<>();
                }

                if(response.contentModified) {
                    // decode the incoming json
                    List<AssetResponse.Asset> items = (new Gson()).fromJson(httpResponse.body(), new TypeToken<List<AssetResponse.Asset>>() {}.getType());

                    // preload all items from the database that match the processed items
                    List<CharacterAssetDto> existingAssets = characterAssetRepository.findByItemIdIn(items.stream().map(a -> a.item_id).toList());

                    // process the response to the database
                    for(AssetResponse.Asset asset : items) {
                        // get the existing database row
                        Optional<CharacterAssetDto> existingDto = existingAssets.stream().filter(a -> a.getItemId().equals(asset.item_id)).findFirst();
                        CharacterAssetDto dto;
                        if(existingDto.isEmpty()) {
                            dto = new CharacterAssetDto();
                            existingAssets.add(dto);
                        } else {
                            dto = existingDto.get();
                        }

                        dto.setCharacter(character);
                        dto.setEsiEtag(esiEtagRepository.findByEtag(response.etag).orElseThrow());
                        asset.toDto(dto);
                    }

                    // save changes to the database
                    characterAssetRepository.saveAll(existingAssets);

                    response.value.addAll(items);
                } else {
                    // pull assets from the database by the etag
                    var dbAssets = characterAssetRepository.findByEsiEtag_Etag(response.etag);
                    response.value.addAll(dbAssets.stream().map(AssetResponse.Asset::fromDto).toList());
                }
            }

            // remove all non-processed assets
            characterAssetRepository.deleteAllByItemIdNotInAndCharacter_Id(response.value.stream().map(a -> a.item_id).toList(), character.getId());

            var r = response;

            // try to get the wallet history line for today
            /*WalletHistoryDto walletHistory = walletHistoryRepository.findByCharacterAndDate(character, Date.valueOf(LocalDate.now())).orElse(new WalletHistoryDto(character, Date.valueOf(LocalDate.now())));
            if(response.contentModified) {
                response.value = Double.parseDouble(httpResponse.body());
                walletHistory.setValue(response.value);
                if (walletHistory.getId() == null) {
                    walletHistory.setStartValue(response.value);
                }
                walletHistoryRepository.save(walletHistory);
            } else if(walletHistory.getValue() == null) {
                // get the previous wallet history
                Optional<WalletHistoryDto> prevWalletHistory = walletHistoryRepository.findFirstByCharacterAndDateBeforeOrderByDateDesc(character, Date.valueOf(LocalDate.now()));
                if(prevWalletHistory.isPresent()) {
                    walletHistory.setStartValue(prevWalletHistory.get().getValue());
                    walletHistory.setValue(prevWalletHistory.get().getValue());

                    response.value = walletHistory.getValue();
                    walletHistoryRepository.save(walletHistory);
                }
            } else {
                // get the wallet value from the database
                response.value = walletHistory.getValue();
            }

            response.difference = (walletHistory.getValue() - walletHistory.getStartValue());*/

            return Optional.of(response);
        } catch (URISyntaxException | InterruptedException | ExecutionException e) {
            logger.error("Error while loading assets for character " + character.getId(), e);
        }

        return Optional.empty();
    }
}
