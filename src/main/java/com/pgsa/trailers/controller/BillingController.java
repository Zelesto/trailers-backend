package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.billing.LoadBillingSummary;
import com.pgsa.trailers.entity.billing.LoadBilling;
import com.pgsa.trailers.entity.billing.TripBilling;
import com.pgsa.trailers.entity.finance.Invoice;
import com.pgsa.trailers.service.billing.BillingCalculatorService;
import com.pgsa.trailers.service.billing.InvoiceBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Slf4j
public class BillingController {

    private final InvoiceBillingService invoiceBillingService;
    private final BillingCalculatorService billingCalculatorService;

    // ========== LOAD BILLING ==========

    @GetMapping("/load/{loadId}/summary")
    public ResponseEntity<LoadBillingSummary> getLoadBillingSummary(@PathVariable String loadId) {
        return ResponseEntity.ok(invoiceBillingService.getLoadBillingSummary(loadId));
    }

    @GetMapping("/load/{loadId}")
    public ResponseEntity<LoadBilling> getLoadBilling(@PathVariable String loadId) {
        return ResponseEntity.ok(billingCalculatorService.getLoadBilling(loadId));
    }

    @GetMapping("/loads/billable")
    public ResponseEntity<List<LoadBilling>> getBillableLoads() {
        log.info("GET /api/billing/loads/billable");
        try {
            List<LoadBilling> billableLoads = billingCalculatorService.getBillableLoads();
            return ResponseEntity.ok(billableLoads != null ? billableLoads : new ArrayList<>());
        } catch (Exception e) {
            log.error("Error fetching billable loads: {}", e.getMessage(), e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PostMapping("/load/{loadId}/calculate")
    public ResponseEntity<LoadBilling> calculateLoadBilling(
            @PathVariable String loadId, 
            @RequestParam Long userId) {
        return ResponseEntity.ok(billingCalculatorService.updateLoadBilling(loadId, userId));
    }

   @PostMapping("/load/{loadId}/recalculate")
    public ResponseEntity<LoadBilling> recalculateLoadBilling(
            @PathVariable String loadId,
            @RequestParam Long userId) {
        log.info("🔄 Recalculating billing for load: {}", loadId);
        LoadBilling billing = billingCalculatorService.recalculateLoadBilling(loadId, userId);
        return ResponseEntity.ok(billing);
    }

    // ========== TRIP BILLING ==========

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<TripBilling> getTripBilling(@PathVariable Long tripId) {
        TripBilling billing = billingCalculatorService.getTripBilling(tripId);
        return ResponseEntity.ok(billing);
    }

    @PostMapping("/trip/{tripId}/calculate")
    public ResponseEntity<TripBilling> calculateTripBilling(
            @PathVariable Long tripId, 
            @RequestParam Long userId) {
        return ResponseEntity.ok(billingCalculatorService.calculateTripBilling(tripId, userId));
    }

    // ========== INVOICE GENERATION ==========

    @PostMapping("/load/{loadId}/invoice")
    public ResponseEntity<Invoice> generateInvoiceFromLoad(
            @PathVariable String loadId, 
            @RequestParam Long userId) {
        return ResponseEntity.ok(invoiceBillingService.generateInvoiceFromLoad(loadId, userId));
    }

    @PostMapping("/trip/{tripId}/invoice")
    public ResponseEntity<Invoice> generateInvoiceFromTrip(
            @PathVariable Long tripId, 
            @RequestParam Long userId) {
        return ResponseEntity.ok(invoiceBillingService.generateInvoiceFromTrip(tripId, userId));
    }

    // ========== BULK OPERATIONS ==========

    @PostMapping("/load/bulk-invoice")
    public ResponseEntity<List<Invoice>> bulkGenerateInvoices(
            @RequestBody List<String> loadIds, 
            @RequestParam Long userId) {
        List<Invoice> invoices = new ArrayList<>();
        for (String loadId : loadIds) {
            try {
                invoices.add(invoiceBillingService.generateInvoiceFromLoad(loadId, userId));
            } catch (Exception e) {
                log.error("Failed to generate invoice for load {}: {}", loadId, e.getMessage());
            }
        }
        return ResponseEntity.ok(invoices);
    }
}
