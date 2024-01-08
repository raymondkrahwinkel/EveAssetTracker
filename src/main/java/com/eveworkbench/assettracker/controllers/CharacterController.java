package com.eveworkbench.assettracker.controllers;

import com.eveworkbench.assettracker.models.api.response.ResponseBaseWithData;
import com.eveworkbench.assettracker.models.database.CharacterDto;
import com.eveworkbench.assettracker.repositories.CharacterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@CrossOrigin
public class CharacterController {
    final CharacterRepository characterRepository;

    public CharacterController(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    @GetMapping("/character/{id}")
    public ResponseEntity<ResponseBaseWithData<CharacterDto>> get(@PathVariable("id") Integer id) {
        Optional<CharacterDto> character = characterRepository.findById(id);
        if(character.isEmpty()) {
            return ResponseEntity.ok(new ResponseBaseWithData<>("Cannot get character with id: " + id, false, null));
        }

        return ResponseEntity.ok(new ResponseBaseWithData<>("", true, character.get()));
    }
}