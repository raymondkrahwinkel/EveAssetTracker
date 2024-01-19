package com.eveworkbench.assettracker.controllers;

import com.eveworkbench.assettracker.models.api.response.ResponseBaseWithData;
import com.eveworkbench.assettracker.models.api.response.ResponseCharacterWallet;
import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.models.database.SessionDto;
import com.eveworkbench.assettracker.models.database.WalletHistoryDto;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import com.eveworkbench.assettracker.repositories.SessionRepository;
import com.eveworkbench.assettracker.repositories.WalletHistoryRepository;
import com.eveworkbench.assettracker.services.EsiWalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
public class CharacterController {
    final CharacterRepository characterRepository;

    final SessionRepository sessionRepository;
    private final WalletHistoryRepository walletHistoryRepository;
    private final EsiWalletService esiWalletService;

    public CharacterController(CharacterRepository characterRepository, SessionRepository sessionRepository, WalletHistoryRepository walletHistoryRepository, EsiWalletService esiWalletService) {
        this.characterRepository = characterRepository;
        this.sessionRepository = sessionRepository;
        this.walletHistoryRepository = walletHistoryRepository;
        this.esiWalletService = esiWalletService;
    }

    @GetMapping("/character")
    public ResponseEntity<ResponseBaseWithData<CharacterDto>> get() {
        // get the current logged-in user information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Integer characterId = Integer.parseInt(auth.getPrincipal().toString());
        String token = auth.getCredentials().toString();

        // get the session information
        Optional<SessionDto> session = sessionRepository.findByCharacterIdAndToken(characterId, token);
        if(session.isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseBaseWithData<>("failed to get session", false, null));
        }

        return ResponseEntity.ok(new ResponseBaseWithData<>("", true, session.get().getCharacter()));
    }

    @GetMapping("/character/children")
    public ResponseEntity<ResponseBaseWithData<List<CharacterDto>>> getChildren() {
        // get the current logged-in user information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Integer characterId = Integer.parseInt(auth.getPrincipal().toString());
        String token = auth.getCredentials().toString();

        // get the session information
        Optional<SessionDto> session = sessionRepository.findByCharacterIdAndToken(characterId, token);
        if(session.isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseBaseWithData<>("failed to get session", false, null));
        }

        CharacterDto parent = session.get().getCharacter();
        if(session.get().getCharacter().getParent() != null) {
            parent = session.get().getCharacter().getParent();
        }

        List<CharacterDto> characters = characterRepository.findByIdOrParentOrderByName(parent.getId(), parent);
        return ResponseEntity.ok(new ResponseBaseWithData<>("", true, characters.stream().filter(c -> !c.getId().equals(characterId)).distinct().toList()));
    }

    @GetMapping("/wallet/balance/{id}")
    public ResponseEntity<ResponseCharacterWallet> walletBalance(@PathVariable("id") Integer id) {
        // get the current logged-in user information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Integer characterId = Integer.parseInt(auth.getPrincipal().toString());
        String token = auth.getCredentials().toString();

        // get the session information
        Optional<SessionDto> session = sessionRepository.findByCharacterIdAndToken(characterId, token);
        if(session.isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseCharacterWallet("failed to get session", false, null));
        }

        // check if the requested character is the main or linked
         if(!
                (
                        session.get().getCharacter().getId().equals(id) || // main
                        (session.get().getCharacter().getParent() != null && session.get().getCharacter().getParent().getId().equals(id)) || // parent
                        session.get().getCharacter().getChildren().stream().anyMatch(child -> child.getId().equals(id)) // child
                )
        ) {
             return ResponseEntity.badRequest().body(new ResponseCharacterWallet("failed to get wallet information", false, null));
        }

        var walletResponse = esiWalletService.getWalletBalance(id);
        if(walletResponse.isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseCharacterWallet("failed to get wallet information from ESI", false, null));
        }

        return ResponseEntity.ok(new ResponseCharacterWallet("", true, walletResponse.get().value, walletResponse.get().difference));
    }

    @GetMapping("/wallet/history")
    public ResponseEntity<ResponseBaseWithData<List<WalletHistoryDto>>> getWalletHistory() {
        // get the current logged-in user information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Integer characterId = Integer.parseInt(auth.getPrincipal().toString());
        String token = auth.getCredentials().toString();

        // get the session information
        Optional<SessionDto> session = sessionRepository.findByCharacterIdAndToken(characterId, token);
        if(session.isEmpty()) {
            return ResponseEntity.ok(new ResponseBaseWithData<List<WalletHistoryDto>>("Failed to get character from session", false, null));
        }

        return ResponseEntity.ok(new ResponseBaseWithData<>("", true, walletHistoryRepository.findAllByCharacterOrderByDateDesc(session.get().getCharacter())));
    }
}