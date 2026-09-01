package com.pgsa.trailers.service.finance;

import com.pgsa.trailers.dto.InvoiceStats;
import com.pgsa.trailers.entity.suppliers.Invoice;
import com.pgsa.trailers.repository.finance.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            log.error("Error fetching invoices: {}", e.getMessage());
            // Return empty page on error
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
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice updateInvoice(Long id, Invoice invoice) {
        Invoice existing = getInvoiceById(id);
        existing.setDescription(invoice.getDescription());
        existing.setDueDate(invoice.getDueDate());
        existing.setStatus(invoice.getStatus());
        existing.setNotes(invoice.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());
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
            java.math.BigDecimal totalPaid = invoiceRepository.sumPaidAmount();
            java.math.BigDecimal totalOutstanding = invoiceRepository.sumOutstandingAmount();

            return InvoiceStats.builder()
                    .totalInvoices(totalInvoices != null ? totalInvoices : 0L)
                    .overdueCount(overdueCount != null ? overdueCount : 0L)
                    .totalPaid(totalPaid != null ? totalPaid : java.math.BigDecimal.ZERO)
                    .totalOutstanding(totalOutstanding != null ? totalOutstanding : java.math.BigDecimal.ZERO)
                    .build();
        } catch (Exception e) {
            log.error("Error calculating invoice stats: {}", e.getMessage());
            return InvoiceStats.builder()
                    .totalInvoices(0L)
                    .overdueCount(0L)
                    .totalPaid(java.math.BigDecimal.ZERO)
                    .totalOutstanding(java.math.BigDecimal.ZERO)
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
