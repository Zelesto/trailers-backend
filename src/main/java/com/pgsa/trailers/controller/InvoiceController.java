package com.pgsa.trailers.controller;

// IMPORTANT: Use the existing Invoice from suppliers package
import com.pgsa.trailers.entity.suppliers.Invoice;
import com.pgsa.trailers.dto.InvoiceStats;
import com.pgsa.trailers.service.finance.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<Page<Invoice>> getAllInvoices(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("GET /api/invoices - Fetching all invoices");
        try {
            return ResponseEntity.ok(invoiceService.getAllInvoices(pageable));
        } catch (Exception e) {
            log.error("Error fetching invoices: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long id) {
        log.info("GET /api/invoices/{}", id);
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@RequestBody Invoice invoice) {
        log.info("POST /api/invoices - Creating invoice");
        return ResponseEntity.ok(invoiceService.createInvoice(invoice));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Invoice> updateInvoice(
            @PathVariable Long id, 
            @RequestBody Invoice invoice) {
        log.info("PUT /api/invoices/{}", id);
        return ResponseEntity.ok(invoiceService.updateInvoice(id, invoice));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        log.info("DELETE /api/invoices/{}", id);
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/mark-as-paid")
    public ResponseEntity<Invoice> markAsPaid(@PathVariable Long id) {
        log.info("POST /api/invoices/{}/mark-as-paid", id);
        return ResponseEntity.ok(invoiceService.markAsPaid(id));
    }

    @PostMapping("/{id}/send-email")
    public ResponseEntity<Void> sendInvoiceEmail(@PathVariable Long id) {
        log.info("POST /api/invoices/{}/send-email", id);
        invoiceService.sendInvoiceEmail(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<InvoiceStats> getInvoiceStats() {
        log.info("GET /api/invoices/stats");
        return ResponseEntity.ok(invoiceService.getInvoiceStats());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Invoice>> getInvoicesByCustomer(@PathVariable Long customerId) {
        log.info("GET /api/invoices/customer/{}", customerId);
        return ResponseEntity.ok(invoiceService.getInvoicesByCustomer(customerId));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Invoice>> getOverdueInvoices() {
        log.info("GET /api/invoices/overdue");
        return ResponseEntity.ok(invoiceService.getOverdueInvoices());
    }
}
