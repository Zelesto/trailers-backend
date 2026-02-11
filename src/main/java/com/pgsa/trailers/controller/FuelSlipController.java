package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.FuelSlipDTO;
import com.pgsa.trailers.dto.FuelSlipRequest;
import com.pgsa.trailers.entity.ops.FuelSlip;
import com.pgsa.trailers.service.FuelSlipService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fuel/slips")
@RequiredArgsConstructor
public class FuelSlipController {

    private final FuelSlipService fuelSlipService;

    // ----------------------
    // CREATE
    // ----------------------
    @PostMapping
    public ResponseEntity<FuelSlipDTO> create(@RequestBody FuelSlipRequest request) {
        FuelSlipDTO created = fuelSlipService.createFuelSlip(request);
        return ResponseEntity.ok(created);
    }

    // ----------------------
    // Verify
    // ----------------------
    @PostMapping("/{id}/verify")
    public ResponseEntity<Void> verifySlip(@PathVariable Long id, @RequestParam String verifiedBy) {
        fuelSlipService.verifyFuelSlip(id, verifiedBy);
        return ResponseEntity.ok().build();
    }

    // ----------------------
    // READ
    // ----------------------

    // 1️⃣ Get a single slip by ID
    @GetMapping("/{id}")
    public ResponseEntity<FuelSlipDTO> getById(@PathVariable Long id) {
        FuelSlipDTO slip = fuelSlipService.getFuelSlipById(id);
        return ResponseEntity.ok(slip);
    }

    // 2️⃣ Get all slips (with optional filters) - ✅ FIXED
    @GetMapping
    public ResponseEntity<List<FuelSlipDTO>> getAll(
            @RequestParam(required = false) Long tripId,      // 🔥 ADDED
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long vehicleId
    ) {
        List<FuelSlipDTO> slips;

        // 🔥 FIX: Handle tripId filter FIRST
        if (tripId != null) {
            slips = fuelSlipService.getFuelSlipsByTripId(tripId);
        } else if (driverId != null && vehicleId != null) {
            slips = fuelSlipService.getFuelSlipsByDriverAndVehicle(driverId, vehicleId);
        } else if (driverId != null) {
            slips = fuelSlipService.getFuelSlipsByDriver(driverId);
        } else if (vehicleId != null) {
            slips = fuelSlipService.getFuelSlipsByVehicle(vehicleId);
        } else {
            slips = fuelSlipService.getAllFuelSlips();
        }

        return ResponseEntity.ok(slips);
    }

    // 3️⃣ Get slips for a period
    @GetMapping("/period")
    public ResponseEntity<List<FuelSlipDTO>> getForPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<FuelSlipDTO> slips = fuelSlipService.getFuelSlipsForPeriod(start, end);
        return ResponseEntity.ok(slips);
    }

    // 4️⃣ Get slips by driver
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<FuelSlipDTO>> getByDriver(@PathVariable Long driverId) {
        List<FuelSlipDTO> slips = fuelSlipService.getFuelSlipsByDriver(driverId);
        return ResponseEntity.ok(slips);
    }

    // 5️⃣ Get slips by vehicle
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<FuelSlipDTO>> getByVehicle(@PathVariable Long vehicleId) {
        List<FuelSlipDTO> slips = fuelSlipService.getFuelSlipsByVehicle(vehicleId);
        return ResponseEntity.ok(slips);
    }

    // ----------------------
    // UPDATE
    // ----------------------

    // Update a slip (if not finalized)
    @PutMapping("/{id}")
    public ResponseEntity<FuelSlipDTO> update(@PathVariable Long id, @RequestBody FuelSlipRequest request) {
        FuelSlipDTO updated = fuelSlipService.updateFuelSlip(id, request);
        return ResponseEntity.ok(updated);
    }

    // ----------------------
    // DELETE
    // ----------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fuelSlipService.deleteFuelSlip(id);
        return ResponseEntity.noContent().build();
    }

    // ----------------------
    // FINALIZE
    // ----------------------
    @PostMapping("/{id}/finalize")
    public ResponseEntity<Void> finalizeSlip(@PathVariable Long id) {
        fuelSlipService.finalizeFuelSlip(id);
        return ResponseEntity.ok().build();
    }
}
