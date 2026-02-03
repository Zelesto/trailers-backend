package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.ops.Pod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pods")
public class PodController {

    @PostMapping
    public ResponseEntity<?> createPod(@RequestBody Pod pod) {
        // Implementation
        return null;
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<?> getPodsByTrip(@PathVariable Long tripId) {
        // Implementation
        return null;
    }
}