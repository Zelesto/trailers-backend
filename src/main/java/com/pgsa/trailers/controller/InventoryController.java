package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.InventoryVarianceDTO;
import com.pgsa.trailers.entity.inventory.StockMovement;
import com.pgsa.trailers.service.StockCountService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
public class InventoryController {

    private final StockCountService stockCountService;

    public InventoryController(StockCountService stockCountService) {
        this.stockCountService = stockCountService;
    }

    @PostMapping("/recordMovement")
    public void recordMovement(@RequestBody StockMovement movement) {
        stockCountService.recordStockMovement(movement); // void is fine
    }

    @GetMapping("/shrinkage/{id}")
    public InventoryVarianceDTO getShrinkage(@PathVariable Long id) {
        return stockCountService.getShrinkageReport(id); // must return DTO
    }
}