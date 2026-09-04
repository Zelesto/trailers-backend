// src/main/java/com/pgsa/trailers/controller/InventoryStockController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.StockOnHandDTO;
import com.pgsa.trailers.dto.StockOnHandFilterDTO;
import com.pgsa.trailers.service.StockOnHandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory Stock", description = "Inventory stock on hand by location/vehicle/driver")
public class InventoryStockController {

    private final StockOnHandService stockOnHandService;

    @GetMapping("/stock-on-hand")
    @Operation(summary = "Get stock on hand by location, vehicle, or driver")
    public ResponseEntity<List<StockOnHandDTO>> getStockOnHand(
            @RequestParam(required = false) String holderType,
            @RequestParam(required = false) Long holderId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean showOnlyOutstanding) {
        
        log.info("📊 Fetching stock on hand with filters - holderType: {}, holderId: {}, category: {}", 
                holderType, holderId, category);
        
        StockOnHandFilterDTO filter = StockOnHandFilterDTO.builder()
                .holderType(holderType != null ? holderType : "ALL")
                .holderId(holderId)
                .itemId(itemId)
                .category(category != null ? category : "ALL")
                .search(search)
                .status(status != null ? status : "ALL")
                .showOnlyOutstanding(showOnlyOutstanding != null ? showOnlyOutstanding : false)
                .build();
        
        List<StockOnHandDTO> results = stockOnHandService.getStockOnHand(filter);
        return ResponseEntity.ok(results);
    }
}
