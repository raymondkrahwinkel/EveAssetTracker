package com.eveworkbench.assettracker.services;

import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.database.WalletHistoryDto;
import com.eveworkbench.assettracker.models.esi.WalletResponse;
import com.eveworkbench.assettracker.repositories.WalletHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

// todo: add tests
@Service
public class EsiWalletService extends EsiService {
    @Autowired
    WalletHistoryRepository walletHistoryRepository;

    public EsiWalletService() {
        super();
    }

    public Optional<WalletResponse> getWalletBalance(Integer characterId) {
        var character = characterRepository.findById(characterId);
        if(character.isEmpty()) {
            throw new RuntimeException("Cannot get character with id: " + characterId);
        }

        return getWalletBalance(character.get());
    }

    public Optional<WalletResponse> getWalletBalance(CharacterDto character) {
        try {
            WalletResponse response = new WalletResponse();

            HttpRequest request = getBaseCharacterHttpRequestBuilder("https://esi.evetech.net/latest/characters/" + character.getId() + "/wallet/", character.getAccessToken())
                    .GET()
                    .build();

            HttpResponse<String> httpResponse = httpClientFactory
                    .create()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
//                    .thenApply(HttpResponse::body)
//                    .thenApply(Double::parseDouble)
                    .get();

            if(!interpretEsiResponse(response, httpResponse)) {
                return Optional.of(response);
            }

            // try to get the wallet history line for today
            WalletHistoryDto walletHistory = walletHistoryRepository.findByCharacterAndDate(character, Date.valueOf(LocalDate.now())).orElse(new WalletHistoryDto(character, Date.valueOf(LocalDate.now())));
            if(response.contentModified) {
                response.value = Double.parseDouble(httpResponse.body());
                walletHistory.setValue(response.value);
                if(walletHistory.getId() == null) {
                    walletHistory.setStartValue(response.value);
                }
                walletHistoryRepository.save(walletHistory);
            } else {
                // get the wallet value from the database
                response.value = walletHistory.getValue();
            }

            response.difference = (walletHistory.getValue() - walletHistory.getStartValue());

            return Optional.of(response);
        } catch (URISyntaxException | InterruptedException | ExecutionException e) {
            logger.error("Error while loading wallet balance for " + character.getId(), e);
        }

        return Optional.empty();
    }
}