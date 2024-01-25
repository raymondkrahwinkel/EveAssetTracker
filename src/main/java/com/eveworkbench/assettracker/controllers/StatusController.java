package com.eveworkbench.assettracker.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {
    @GetMapping("/health/ready")
    public String ready()
    {
        return "Ok";
    }

    @GetMapping("/health/live")
    public String live()
    {
        return "Ok";
    }
}
