package com.pgsa.trailers.service.finance;

import com.pgsa.trailers.dto.InvoiceStats;
import com.pgsa.trailers.entity.finance.Invoice;
import com.pgsa.trailers.entity.finance.InvoiceItem;
import com.pgsa.trailers.repository.finance.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public Page<Invoice> getAllInvoices(Pageable pageable) {
        try {
            return invoiceRepository.findAll(pageable);
        } catch (Exception e) {
            log.error("Error fetching invoices: {}", e.getMessage(), e);
            return Page.empty(pageable);
        }
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));
    }

    @Transactional
    public Invoice createInvoice(Invoice invoice) {
        log.info("Creating invoice: {}", invoice.getInvoiceNumber());
        
        // Set defaults
        if (invoice.getCreatedAt() == null) {
            invoice.setCreatedAt(LocalDateTime.now());
        }
        if (invoice.getUpdatedAt() == null) {
            invoice.setUpdatedAt(LocalDateTime.now());
        }
        if (invoice.getStatus() == null) {
            invoice.setStatus("DRAFT");
        }
        if (invoice.getSource() == null) {
            invoice.setSource("MANUAL");
        }
        if (invoice.getVatRate() == null) {
            invoice.setVatRate(new BigDecimal("15.00"));
        }
        
        // Recalculate totals
        invoice.recalculateTotals();
        
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice updateInvoice(Long id, Invoice invoice) {
        Invoice existing = getInvoiceById(id);
        
        // Update fields
        if (invoice.getCustomerId() != null) {
            existing.setCustomerId(invoice.getCustomerId());
        }
        if (invoice.getCustomerName() != null) {
            existing.setCustomerName(invoice.getCustomerName());
        }
        if (invoice.getCustomerEmail() != null) {
            existing.setCustomerEmail(invoice.getCustomerEmail());
        }
        if (invoice.getCustomerAddress() != null) {
            existing.setCustomerAddress(invoice.getCustomerAddress());
        }
        if (invoice.getDescription() != null) {
            existing.setDescription(invoice.getDescription());
        }
        if (invoice.getDueDate() != null) {
            existing.setDueDate(invoice.getDueDate());
        }
        if (invoice.getStatus() != null) {
            existing.setStatus(invoice.getStatus());
        }
        if (invoice.getNotes() != null) {
            existing.setNotes(invoice.getNotes());
        }
        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            // Clear existing items and add new ones
            existing.getItems().clear();
            for (InvoiceItem item : invoice.getItems()) {
                item.setInvoice(existing);
                existing.getItems().add(item);
            }
        }
        
        existing.setUpdatedAt(LocalDateTime.now());
        existing.recalculateTotals();
        
        return invoiceRepository.save(existing);
    }

    @Transactional
    public void deleteInvoice(Long id) {
        Invoice invoice = getInvoiceById(id);
        if ("PAID".equals(invoice.getStatus())) {
            throw new RuntimeException("Cannot delete a paid invoice");
        }
        invoiceRepository.deleteById(id);
    }

    @Transactional
    public Invoice markAsPaid(Long id) {
        Invoice invoice = getInvoiceById(id);
        invoice.setStatus("PAID");
        invoice.setPaidAmount(invoice.getTotalAmount());
        invoice.setUpdatedAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void sendInvoiceEmail(Long id) {
        Invoice invoice = getInvoiceById(id);
        log.info("Sending invoice {} to {}", invoice.getInvoiceNumber(), invoice.getCustomerEmail());
        // TODO: Implement actual email sending
    }

    @Transactional(readOnly = true)
    public InvoiceStats getInvoiceStats() {
        try {
            Long totalInvoices = invoiceRepository.count();
            Long overdueCount = invoiceRepository.countOverdue();
            BigDecimal totalPaid = invoiceRepository.sumPaidAmount();
            BigDecimal totalOutstanding = invoiceRepository.sumOutstandingAmount();

            return InvoiceStats.builder()
                    .totalInvoices(totalInvoices != null ? totalInvoices : 0L)
                    .overdueCount(overdueCount != null ? overdueCount : 0L)
                    .totalPaid(totalPaid != null ? totalPaid : BigDecimal.ZERO)
                    .totalOutstanding(totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO)
                    .build();
        } catch (Exception e) {
            log.error("Error calculating invoice stats: {}", e.getMessage(), e);
            return InvoiceStats.builder()
                    .totalInvoices(0L)
                    .overdueCount(0L)
                    .totalPaid(BigDecimal.ZERO)
                    .totalOutstanding(BigDecimal.ZERO)
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByCustomer(Long customerId) {
        return invoiceRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices(LocalDateTime.now());
    }
}
