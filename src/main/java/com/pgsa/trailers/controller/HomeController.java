package com.pgsa.trailers.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Welcome to PGSA Management System API! powered by SCB";
    }

    @GetMapping("/api/status")
    public String status() {
        return "API is running!";
    }
}