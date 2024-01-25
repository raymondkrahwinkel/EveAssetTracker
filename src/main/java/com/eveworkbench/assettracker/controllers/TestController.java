package com.eveworkbench.assettracker.controllers;

import com.eveworkbench.assettracker.repositories.EsiMarketGroupRepository;
import com.eveworkbench.assettracker.services.EsiMarketGroupService;
import com.eveworkbench.assettracker.services.EsiTypeCategoryService;
import com.eveworkbench.assettracker.services.EsiTypeGroupService;
import com.eveworkbench.assettracker.services.EsiTypeService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin // allow requests from the angular frontend
//@CrossOrigin()
public class TestController {
    private final EsiTypeService esiTypeService;
    private final EsiTypeCategoryService esiTypeCategoryService;
    private final EsiTypeGroupService esiTypeGroupService;
    private final EsiMarketGroupService esiMarketGroupService;

    public TestController(EsiTypeService esiTypeService, EsiTypeCategoryService esiTypeCategoryService, EsiTypeGroupService esiTypeGroupService, EsiMarketGroupService esiMarketGroupService) {

        this.esiTypeService = esiTypeService;
        this.esiTypeCategoryService = esiTypeCategoryService;
        this.esiTypeGroupService = esiTypeGroupService;
        this.esiMarketGroupService = esiMarketGroupService;
    }

    @GetMapping("/test")
    @CrossOrigin
    public String Test()
    {
        esiTypeCategoryService.updateCategories();
        esiTypeGroupService.updateGroups();
        esiMarketGroupService.updateGroups();

        esiTypeService.updateTypesFromEsi();
        return "Hellow!";
    }
}
