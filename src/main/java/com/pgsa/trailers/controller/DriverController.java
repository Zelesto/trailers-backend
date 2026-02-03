package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.DriverDTO;
import com.pgsa.trailers.dto.DriverRequest;
import com.pgsa.trailers.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    //@PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public DriverDTO createDriver(@RequestBody DriverRequest request) {
        return driverService.createDriver(request);
    }


    @GetMapping("/{id}")
    //@PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<DriverDTO> getDriverById(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @GetMapping
    //@PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<DriverDTO>> getAllDrivers() {
        List<DriverDTO> drivers = driverService.getAllDrivers();
        return ResponseEntity.ok(drivers); // ok even if empty
    }


    @DeleteMapping("/{id}")
    //@PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }
}
