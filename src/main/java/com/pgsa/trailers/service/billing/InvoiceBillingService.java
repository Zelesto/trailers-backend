package com.pgsa.trailers.service.billing;

import com.pgsa.trailers.entity.billing.LoadBilling;
import com.pgsa.trailers.entity.billing.TripBilling;
import com.pgsa.trailers.dto.billing.LoadBillingSummary;
import com.pgsa.trailers.entity.finance.Invoice;
import com.pgsa.trailers.entity.finance.InvoiceItem;
import com.pgsa.trailers.entity.ops.Customer;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.billing.LoadBillingRepository;
import com.pgsa.trailers.repository.billing.TripBillingRepository;
import com.pgsa.trailers.repository.finance.InvoiceRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceBillingService {

    private final LoadBillingRepository loadBillingRepository;
    private final TripBillingRepository tripBillingRepository;
    private final InvoiceRepository invoiceRepository;
    private final LoadRepository loadRepository;
    private final CustomerRepository customerRepository;

    /**
     * Generate an invoice from a Load's billing
     */
    @Transactional
    public Invoice generateInvoiceFromLoad(String loadId, Long userId) {
        log.info("📄 Generating invoice for Load: {}", loadId);

        Load load = loadRepository.findByLoadNumber(loadId)
                .orElseThrow(() -> new RuntimeException("Load not found: " + loadId));

        LoadBilling loadBilling = loadBillingRepository.findByLoadId(loadId)
                .orElseThrow(() -> new RuntimeException("Load billing not found for: " + loadId));

        Customer customer = customerRepository.findById(loadBilling.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (loadBilling.getInvoiceId() != null) {
            throw new RuntimeException("Load already has an invoice: " + loadBilling.getInvoiceId());
        }

        Invoice invoice = createInvoiceFromLoad(load, customer, loadId);
        List<TripBilling> tripBillings = tripBillingRepository.findByTrip_LoadId(loadId);
        List<InvoiceItem> items = buildTripLineItems(invoice, tripBillings);
        items.add(buildSummaryLineItem(invoice, loadId, tripBillings, loadBilling));

        invoice.setItems(items);
        invoice.setSubtotal(loadBilling.getSubtotal());
        invoice.setTaxTotal(loadBilling.getVat());
        invoice.setTotalAmount(loadBilling.getTotal());

        Invoice savedInvoice = invoiceRepository.save(invoice);

        updateLoadAndTripBillings(loadId, loadBilling, tripBillings, savedInvoice);

        log.info("✅ Invoice {} generated for Load {}", savedInvoice.getInvoiceNumber(), loadId);
        return savedInvoice;
    }

    /**
     * Generate an invoice from a single Trip
     */
    @Transactional
    public Invoice generateInvoiceFromTrip(Long tripId, Long userId) {
        log.info("📄 Generating invoice for Trip: {}", tripId);

        TripBilling tripBilling = tripBillingRepository.findByTripId(tripId);
        if (tripBilling == null) {
            throw new RuntimeException("Trip billing not found for: " + tripId);
        }

        if (tripBilling.getInvoiceId() != null) {
            throw new RuntimeException("Trip already has an invoice: " + tripBilling.getInvoiceId());
        }

        Trip trip = tripBilling.getTrip();
        Customer customer = customerRepository.findById(tripBilling.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Invoice invoice = createInvoiceFromTrip(trip, customer);
        List<InvoiceItem> items = buildTripDetailLineItems(invoice, tripBilling);
        invoice.setItems(items);
        invoice.setSubtotal(tripBilling.getSubtotal());
        invoice.setTaxTotal(tripBilling.getVat());
        invoice.setTotalAmount(tripBilling.getTotal());

        Invoice savedInvoice = invoiceRepository.save(invoice);

        tripBilling.setInvoiceId(savedInvoice.getId());
        tripBilling.setStatus("INVOICED");
        tripBilling.setInvoicedAt(LocalDateTime.now());
        tripBillingRepository.save(tripBilling);

        log.info("✅ Invoice {} generated for Trip {}", savedInvoice.getInvoiceNumber(), tripId);
        return savedInvoice;
    }

    /**
     * Get billing summary for a load (preview before generating invoice)
     */
    @Transactional(readOnly = true)
    public LoadBillingSummary getLoadBillingSummary(String loadId) {
        log.info("📊 Getting billing summary for Load: {}", loadId);

        Load load = loadRepository.findByLoadNumber(loadId)
                .orElseThrow(() -> new RuntimeException("Load not found: " + loadId));

        LoadBilling loadBilling = loadBillingRepository.findByLoadId(loadId)
                .orElse(null);

        List<TripBilling> tripBillings = tripBillingRepository.findByTrip_LoadId(loadId);

        // Get customer name safely
        String customerName = null;
        if (load.getCustomerId() != null) {
            Customer customer = customerRepository.findById(load.getCustomerId()).orElse(null);
            if (customer != null) {
                customerName = customer.getName();
            }
        }

        // Calculate subtotal and vat
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (loadBilling != null) {
            subtotal = loadBilling.getSubtotal() != null ? loadBilling.getSubtotal() : BigDecimal.ZERO;
            vat = loadBilling.getVat() != null ? loadBilling.getVat() : BigDecimal.ZERO;
            totalAmount = loadBilling.getTotal() != null ? loadBilling.getTotal() : BigDecimal.ZERO;
        } else {
            // Calculate from trip billings using a simple loop (avoids lambda issues)
            for (TripBilling tb : tripBillings) {
                if (tb.getTotal() != null) {
                    totalAmount = totalAmount.add(tb.getTotal());
                }
            }
        }

        // Count totals using simple loops
        long totalBillable = 0;
        long totalInvoiced = 0;
        for (TripBilling tb : tripBillings) {
            if ("CALCULATED".equals(tb.getStatus())) {
                totalBillable++;
            }
            if ("INVOICED".equals(tb.getStatus())) {
                totalInvoiced++;
            }
        }

        return LoadBillingSummary.builder()
                .loadId(loadId)
                .loadDescription(load.getDescription())
                .customerId(load.getCustomerId())
                .customerName(customerName)
                .totalTrips(tripBillings.size())
                .totalBillable(totalBillable)
                .totalInvoiced(totalInvoiced)
                .totalAmount(totalAmount)
                .subtotal(subtotal)
                .vat(vat)
                .trips(tripBillings)
                .status(loadBilling != null ? loadBilling.getStatus() : "DRAFT")
                .canInvoice(loadBilling != null &&
                        loadBilling.getInvoiceId() == null &&
                        !tripBillings.isEmpty())
                .build();
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Invoice createInvoiceFromLoad(Load load, Customer customer, String loadId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setInvoiceType("RECEIVABLE");
        invoice.setCustomerId(customer.getId());
        invoice.setCustomerName(customer.getName());
        invoice.setCustomerEmail(customer.getEmail());
        invoice.setCustomerAddress(buildCustomerAddress(customer));
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setDueDate(LocalDateTime.now().plusDays(30));
        invoice.setCurrency("ZAR");
        invoice.setStatus("DRAFT");
        invoice.setDescription("Transport services for Load: " + loadId);
        invoice.setVatRate(new BigDecimal("15.00"));
        return invoice;
    }

    private Invoice createInvoiceFromTrip(Trip trip, Customer customer) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setInvoiceType("RECEIVABLE");
        invoice.setCustomerId(customer.getId());
        invoice.setCustomerName(customer.getName());
        invoice.setCustomerEmail(customer.getEmail());
        invoice.setCustomerAddress(buildCustomerAddress(customer));
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setDueDate(LocalDateTime.now().plusDays(30));
        invoice.setCurrency("ZAR");
        invoice.setStatus("DRAFT");
        invoice.setDescription("Transport service - Trip " + trip.getTripNumber());
        invoice.setVatRate(new BigDecimal("15.00"));
        return invoice;
    }

    private List<InvoiceItem> buildTripLineItems(Invoice invoice, List<TripBilling> tripBillings) {
        List<InvoiceItem> items = new ArrayList<>();
        for (TripBilling tb : tripBillings) {
            Trip trip = tb.getTrip();
            InvoiceItem item = new InvoiceItem();
            item.setDescription(String.format(
                    "Trip %s: %s → %s (%.0f km, %.1f tons)",
                    trip.getTripNumber(),
                    trip.getOriginLocation() != null ? truncate(trip.getOriginLocation(), 20) : "N/A",
                    trip.getDestinationLocation() != null ? truncate(trip.getDestinationLocation(), 20) : "N/A",
                    tb.getDistanceKm() != null ? tb.getDistanceKm() : BigDecimal.ZERO,
                    tb.getTonnage() != null ? tb.getTonnage() : BigDecimal.ZERO
            ));
            item.setQuantity(BigDecimal.ONE);
            item.setUnitPrice(tb.getTotal());
            item.setTaxRate(new BigDecimal("15.00"));
            item.setLineTotal(tb.getTotal());
            item.setTaxAmount(tb.getVat());
            item.setInvoice(invoice);
            item.setTripId(trip.getId());
            items.add(item);
        }
        return items;
    }

    private InvoiceItem buildSummaryLineItem(Invoice invoice, String loadId, List<TripBilling> tripBillings, LoadBilling loadBilling) {
        InvoiceItem summaryItem = new InvoiceItem();
        summaryItem.setDescription(String.format(
                "Load %s: %d trips, %.0f km total",
                loadId,
                tripBillings.size(),
                loadBilling.getTotalDistanceKm() != null ? loadBilling.getTotalDistanceKm() : BigDecimal.ZERO
        ));
        summaryItem.setQuantity(BigDecimal.ONE);
        summaryItem.setUnitPrice(loadBilling.getSubtotal());
        summaryItem.setTaxRate(new BigDecimal("15.00"));
        summaryItem.setLineTotal(loadBilling.getSubtotal());
        summaryItem.setTaxAmount(loadBilling.getVat());
        summaryItem.setInvoice(invoice);
        return summaryItem;
    }

    private List<InvoiceItem> buildTripDetailLineItems(Invoice invoice, TripBilling tripBilling) {
        List<InvoiceItem> items = new ArrayList<>();

        // Distance charge
        if (tripBilling.getDistanceCharge() != null && tripBilling.getDistanceCharge().compareTo(BigDecimal.ZERO) > 0) {
            InvoiceItem distItem = new InvoiceItem();
            distItem.setDescription(String.format(
                    "Distance charge: %.0f km @ R%.2f/km",
                    tripBilling.getDistanceKm() != null ? tripBilling.getDistanceKm() : BigDecimal.ZERO,
                    tripBilling.getBaseRate() != null ? tripBilling.getBaseRate() : BigDecimal.ZERO
            ));
            distItem.setQuantity(BigDecimal.ONE);
            distItem.setUnitPrice(tripBilling.getDistanceCharge());
            distItem.setTaxRate(new BigDecimal("15.00"));
            distItem.setLineTotal(tripBilling.getDistanceCharge());
            distItem.setTaxAmount(tripBilling.getDistanceCharge().multiply(new BigDecimal("0.15")));
            distItem.setInvoice(invoice);
            items.add(distItem);
        }

        // Tonnage charge
        if (tripBilling.getTonnageCharge() != null && tripBilling.getTonnageCharge().compareTo(BigDecimal.ZERO) > 0) {
            InvoiceItem tonItem = new InvoiceItem();
            tonItem.setDescription(String.format(
                    "Tonnage charge: %.1f tons",
                    tripBilling.getTonnage() != null ? tripBilling.getTonnage() : BigDecimal.ZERO
            ));
            tonItem.setQuantity(BigDecimal.ONE);
            tonItem.setUnitPrice(tripBilling.getTonnageCharge());
            tonItem.setTaxRate(new BigDecimal("15.00"));
            tonItem.setLineTotal(tripBilling.getTonnageCharge());
            tonItem.setTaxAmount(tripBilling.getTonnageCharge().multiply(new BigDecimal("0.15")));
            tonItem.setInvoice(invoice);
            items.add(tonItem);
        }

        // Daily rate charge
        if (tripBilling.getDailyRateCharge() != null && tripBilling.getDailyRateCharge().compareTo(BigDecimal.ZERO) > 0) {
            InvoiceItem dailyItem = new InvoiceItem();
            dailyItem.setDescription(String.format(
                    "Daily rate: %d day(s)",
                    tripBilling.getDays() != null ? tripBilling.getDays() : 1
            ));
            dailyItem.setQuantity(BigDecimal.ONE);
            dailyItem.setUnitPrice(tripBilling.getDailyRateCharge());
            dailyItem.setTaxRate(new BigDecimal("15.00"));
            dailyItem.setLineTotal(tripBilling.getDailyRateCharge());
            dailyItem.setTaxAmount(tripBilling.getDailyRateCharge().multiply(new BigDecimal("0.15")));
            dailyItem.setInvoice(invoice);
            items.add(dailyItem);
        }

        // Labour charge
        if (tripBilling.getLabourCharge() != null && tripBilling.getLabourCharge().compareTo(BigDecimal.ZERO) > 0) {
            InvoiceItem labourItem = new InvoiceItem();
            labourItem.setDescription(String.format(
                    "Labour: %.1f hours",
                    tripBilling.getLabourHours() != null ? tripBilling.getLabourHours() : BigDecimal.ZERO
            ));
            labourItem.setQuantity(BigDecimal.ONE);
            labourItem.setUnitPrice(tripBilling.getLabourCharge());
            labourItem.setTaxRate(new BigDecimal("15.00"));
            labourItem.setLineTotal(tripBilling.getLabourCharge());
            labourItem.setTaxAmount(tripBilling.getLabourCharge().multiply(new BigDecimal("0.15")));
            labourItem.setInvoice(invoice);
            items.add(labourItem);
        }

        // Fixed surcharge
        if (tripBilling.getFixedSurcharge() != null && tripBilling.getFixedSurcharge().compareTo(BigDecimal.ZERO) > 0) {
            InvoiceItem fixedItem = new InvoiceItem();
            fixedItem.setDescription("Fixed surcharge");
            fixedItem.setQuantity(BigDecimal.ONE);
            fixedItem.setUnitPrice(tripBilling.getFixedSurcharge());
            fixedItem.setTaxRate(new BigDecimal("15.00"));
            fixedItem.setLineTotal(tripBilling.getFixedSurcharge());
            fixedItem.setTaxAmount(tripBilling.getFixedSurcharge().multiply(new BigDecimal("0.15")));
            fixedItem.setInvoice(invoice);
            items.add(fixedItem);
        }

        return items;
    }

    private void updateLoadAndTripBillings(String loadId, LoadBilling loadBilling, List<TripBilling> tripBillings, Invoice savedInvoice) {
        loadBilling.setInvoiceId(savedInvoice.getId());
        loadBilling.setStatus("INVOICED");
        loadBilling.setInvoicedAt(LocalDateTime.now());
        loadBillingRepository.save(loadBilling);

        for (TripBilling tb : tripBillings) {
            tb.setInvoiceId(savedInvoice.getId());
            tb.setStatus("INVOICED");
            tb.setInvoicedAt(LocalDateTime.now());
        }
        tripBillingRepository.saveAll(tripBillings);
    }

    private String generateInvoiceNumber() {
        String year = String.valueOf(java.time.Year.now().getValue());
        String prefix = "INV-" + year + "-";
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + random;
    }

    private String buildCustomerAddress(Customer customer) {
        StringBuilder sb = new StringBuilder();
        if (customer.getAddressLine1() != null) sb.append(customer.getAddressLine1());
        if (customer.getAddressLine2() != null) sb.append(", ").append(customer.getAddressLine2());
        if (customer.getCity() != null) sb.append(", ").append(customer.getCity());
        if (customer.getProvince() != null) sb.append(", ").append(customer.getProvince());
        if (customer.getPostalCode() != null) sb.append(" ").append(customer.getPostalCode());
        if (customer.getCountry() != null) sb.append(", ").append(customer.getCountry());
        return sb.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
