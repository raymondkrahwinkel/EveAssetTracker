package com.eveworkbench.assettracker.controllers;

import com.eveworkbench.assettracker.services.EsiAssetService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin // allow requests from the angular frontend
//@CrossOrigin()
public class TestController {
    private final EsiAssetService esiAssetService;

    public TestController(EsiAssetService assetService) {

        this.esiAssetService = assetService;
    }

    @GetMapping("/test")
    @CrossOrigin
    public String Test()
    {
        var response = this.esiAssetService.getAssetsForCharacter(883434905);

        return "Hellow!";
    }
}
