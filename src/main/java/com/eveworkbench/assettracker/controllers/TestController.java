package com.eveworkbench.assettracker.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("http://localhost:4200") // allow requests from the angular frontend
public class TestController {
    @GetMapping("/test")
    public String Test()
    {
        return "Hellow!";
    }
}
