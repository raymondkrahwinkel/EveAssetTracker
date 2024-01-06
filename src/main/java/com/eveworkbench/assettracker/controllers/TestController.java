package com.eveworkbench.assettracker.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin // allow requests from the angular frontend
//@CrossOrigin()
public class TestController {
    @GetMapping("/test")
    @CrossOrigin
    public String Test()
    {
        return "Hellow!";
    }
}
