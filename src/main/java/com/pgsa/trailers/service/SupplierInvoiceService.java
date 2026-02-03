package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.suppliers.Invoice;
import com.pgsa.trailers.repository.SupplierInvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * SupplierInvoiceService
 *
 * Responsibilities:
 * - Retrieve supplier invoices
 * - Create or update invoices
 *
 * Rules respected:
 * - No getAll()
 * - ID-scoped access only
 * - Transactional writes
 */
@Service
public class SupplierInvoiceService {

    private final SupplierInvoiceRepository supplierRepo;

    public SupplierInvoiceService(SupplierInvoiceRepository supplierRepo) {
        this.supplierRepo = supplierRepo;
    }

    /**
     * Retrieve an invoice by ID.
     * Bounded query – no mass reads.
     */
    public Optional<Invoice> getInvoice(Long invoiceId) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("Invoice ID cannot be null");
        }
        return supplierRepo.findById(invoiceId);
    }

    /**
     * Create or update a supplier invoice.
     *
     * Transactional to guarantee persistence consistency.
     */
    @Transactional
    public Invoice createInvoice(Invoice invoice) {
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice cannot be null");
        }
        return supplierRepo.save(invoice);
    }
}
