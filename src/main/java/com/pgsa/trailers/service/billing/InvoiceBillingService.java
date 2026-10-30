package com.pgsa.trailers.service.billing;

import com.pgsa.trailers.dto.billing.LoadBillingSummary;
import com.pgsa.trailers.entity.billing.LoadBilling;
import com.pgsa.trailers.entity.billing.TripBilling;
import com.pgsa.trailers.entity.finance.Invoice;
import com.pgsa.trailers.entity.finance.InvoiceItem;
import com.pgsa.trailers.entity.ops.Customer;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.CustomerRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.billing.LoadBillingRepository;
import com.pgsa.trailers.repository.billing.TripBillingRepository;
import com.pgsa.trailers.service.finance.InvoiceService;
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

    private static final BigDecimal VAT_RATE       = new BigDecimal("15.00");
    private static final BigDecimal VAT_MULTIPLIER = new BigDecimal("0.15");

    private final LoadBillingRepository loadBillingRepository;
    private final TripBillingRepository tripBillingRepository;
    private final LoadRepository loadRepository;
    private final CustomerRepository customerRepository;
    private final TripRepository tripRepository;
    private final BillingCalculatorService billingCalculatorService;
    private final InvoiceService invoiceService;

    // =====================================================================
    // PUBLIC API
    // =====================================================================

    /**
     * Generate an invoice from a Load's billing.
     * <p>
     * Line items map onto the real {@code invoice_item} schema:
     * <ul>
     *   <li>{@code amount}        ← pre-VAT line total</li>
     *   <li>{@code tax_amount}    ← VAT portion</li>
     *   <li>{@code total_amount}  ← pre-VAT + VAT</li>
     *   <li>{@code reference_type} / {@code reference_id} ← polymorphic link</li>
     * </ul>
     */
    @Transactional
    public Invoice generateInvoiceFromLoad(String loadId, Long userId) {
        log.info("📄 Generating invoice for Load: {}", loadId);

        Load load = loadRepository.findByLoadNumber(loadId)
                .orElseThrow(() -> new IllegalArgumentException("Load not found: " + loadId));

        LoadBilling lb = loadBillingRepository.findByLoadId(loadId)
                .orElseGet(() -> billingCalculatorService.recalculateLoadBilling(loadId, userId));

        if (lb.getInvoiceId() != null) {
            throw new IllegalStateException("Load already has an invoice: " + lb.getInvoiceId());
        }

        Long customerId = lb.getCustomerId() != null ? lb.getCustomerId() : load.getCustomerId();
        if (customerId == null) {
            throw new IllegalStateException(
                    "No customer associated with Load " + loadId + ". Assign a customer first.");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        BigDecimal subtotal = nz(lb.getSubtotal());
        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException(
                    "Cannot generate invoice for Load " + loadId
                            + ": billing subtotal is zero. Recalculate billing first.");
        }

        Invoice invoice = createInvoiceFromLoad(load, customer, loadId);
        List<TripBilling> tripBillings = tripBillingRepository.findByTrip_LoadId(loadId);
        List<InvoiceItem> items = buildTripLineItems(invoice, tripBillings);

        BigDecimal tripSubtotal = tripBillings.stream()
                .map(tb -> nz(tb.getSubtotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tripVat = tripBillings.stream()
                .map(tb -> nz(tb.getVat())).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal residualSubtotal = subtotal.subtract(tripSubtotal);
        BigDecimal residualVat      = nz(lb.getVat()).subtract(tripVat);
        if (residualSubtotal.compareTo(BigDecimal.ZERO) > 0
                || residualVat.compareTo(BigDecimal.ZERO) > 0) {
            items.add(buildLoadLevelChargeItem(
                    invoice, loadId, residualSubtotal, residualVat, lb));
        }

        invoice.setItems(items);
        invoice.setSubtotal(subtotal);
        invoice.setTaxTotal(nz(lb.getVat()));
        invoice.setTotalAmount(nz(lb.getTotal()));

        Invoice saved = invoiceService.createInvoicePreservingTotals(invoice);
        updateLoadAndTripBillings(loadId, lb, tripBillings, saved);
        log.info("✅ Invoice {} generated for Load {}", saved.getInvoiceNumber(), loadId);
        return saved;
    }

    /**
     * Generate an invoice from a single Trip.
     */
    @Transactional
    public Invoice generateInvoiceFromTrip(Long tripId, Long userId) {
        log.info("📄 Generating invoice for Trip: {}", tripId);

        TripBilling tb = tripBillingRepository.findByTripId(tripId);
        if (tb == null) throw new IllegalArgumentException("Trip billing not found: " + tripId);
        if (tb.getInvoiceId() != null) {
            throw new IllegalStateException("Trip already has an invoice: " + tb.getInvoiceId());
        }
        Trip trip = tb.getTrip();
        if (trip == null) {
            throw new IllegalStateException("TripBilling " + tripId + " has no Trip record");
        }

        Long customerId = tb.getCustomerId() != null ? tb.getCustomerId() : trip.getCustomerId();
        if (customerId == null) {
            throw new IllegalStateException("No customer associated with Trip " + tripId);
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        BigDecimal subtotal = nz(tb.getSubtotal());
        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException(
                    "Cannot generate invoice for Trip " + tripId + ": billing subtotal is zero.");
        }

        Invoice invoice = createInvoiceFromTrip(trip, customer);
        invoice.setItems(buildTripDetailLineItems(invoice, tb));
        invoice.setSubtotal(subtotal);
        invoice.setTaxTotal(nz(tb.getVat()));
        invoice.setTotalAmount(nz(tb.getTotal()));

        Invoice saved = invoiceService.createInvoicePreservingTotals(invoice);

        tb.setInvoiceId(saved.getId());
        tb.setStatus("INVOICED");
        tb.setInvoicedAt(LocalDateTime.now());
        tripBillingRepository.save(tb);

        log.info("✅ Invoice {} generated for Trip {}", saved.getInvoiceNumber(), tripId);
        return saved;
    }

    /**
     * Build a preview summary for a Load's billing.
     * ✅ NOT read-only — this method may create a {@link LoadBilling}.
     */
    @Transactional
    public LoadBillingSummary getLoadBillingSummary(String loadId) {
        log.info("📊 Getting billing summary for Load: {}", loadId);

        Load load = loadRepository.findByLoadNumber(loadId)
                .orElseThrow(() -> new IllegalArgumentException("Load not found: " + loadId));

        LoadBilling lb = loadBillingRepository.findByLoadId(loadId)
                .orElseGet(() -> {
                    log.info("⚠️ LoadBilling not found, creating from trip data");
                    return createLoadBillingFromTrips(loadId);
                });

        List<TripBilling> tripBillings = tripBillingRepository.findByTrip_LoadId(loadId);

        String customerName = null;
        Long resolvedCustomerId = lb.getCustomerId() != null
                ? lb.getCustomerId() : load.getCustomerId();
        if (resolvedCustomerId != null) {
            customerName = customerRepository.findById(resolvedCustomerId)
                    .map(Customer::getName).orElse(null);
        }

        BigDecimal subtotal    = nz(lb.getSubtotal());
        BigDecimal vat         = nz(lb.getVat());
        BigDecimal totalAmount = nz(lb.getTotal());

        long totalBillable = tripBillings.stream()
                .filter(t -> "CALCULATED".equals(t.getStatus())).count();
        long totalInvoiced = tripBillings.stream()
                .filter(t -> "INVOICED".equals(t.getStatus())).count();

        return LoadBillingSummary.builder()
                .loadId(loadId)
                .loadDescription(load.getDescription())
                .customerId(resolvedCustomerId)
                .customerName(customerName)
                .totalTrips(tripBillings.size())
                .totalBillable(totalBillable)
                .totalInvoiced(totalInvoiced)
                .totalAmount(totalAmount)
                .subtotal(subtotal)
                .vat(vat)
                .trips(tripBillings)
                .status(lb.getStatus() != null ? lb.getStatus() : "DRAFT")
                .canInvoice(lb.getInvoiceId() == null
                        && !tripBillings.isEmpty()
                        && subtotal.compareTo(BigDecimal.ZERO) > 0)
                .build();
    }

    // =====================================================================
    // PRIVATE HELPERS
    // =====================================================================

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /**
     * Create a LoadBilling from trip data if one doesn't exist.
     * Falls back to {@code load.customerId} if the trips don't carry one.
     */
    private LoadBilling createLoadBillingFromTrips(String loadId) {
        List<Trip> trips = tripRepository.findByLoadId(loadId);
        if (trips.isEmpty()) {
            throw new IllegalStateException("No trips found for load: " + loadId);
        }

        Long fallbackCustomerId = loadRepository.findByLoadNumber(loadId)
                .map(Load::getCustomerId).orElse(null);
        Long tripCustomerId = trips.get(0).getCustomerId();
        Long resolvedCustomerId = tripCustomerId != null ? tripCustomerId : fallbackCustomerId;
        if (resolvedCustomerId == null) {
            throw new IllegalStateException(
                    "Cannot determine customer for load " + loadId
                            + ": neither trips nor load have a customerId.");
        }

        LoadBilling lb = new LoadBilling();
        lb.setLoadId(loadId);
        lb.setCustomerId(resolvedCustomerId);
        lb.setTotalTrips(trips.size());

        BigDecimal totalDistance = BigDecimal.ZERO, totalTonnage = BigDecimal.ZERO;
        BigDecimal totalDistanceCharge = BigDecimal.ZERO, totalTonnageCharge = BigDecimal.ZERO;
        BigDecimal totalDailyCharge = BigDecimal.ZERO;

        for (Trip trip : trips) {
            if (trip.getCalculatedDistanceKm() != null) {
                totalDistance = totalDistance.add(trip.getCalculatedDistanceKm());
                totalDistanceCharge = totalDistanceCharge.add(
                        trip.getCalculatedDistanceKm().multiply(new BigDecimal("18.50")));
            }
            if (trip.getCargoWeight() != null) {
                totalTonnage = totalTonnage.add(trip.getCargoWeight());
                totalTonnageCharge = totalTonnageCharge.add(
                        trip.getCargoWeight().multiply(new BigDecimal("50.00")));
            }
            totalDailyCharge = totalDailyCharge.add(new BigDecimal("2500.00"));
        }

        lb.setTotalDistanceKm(totalDistance);
        lb.setTotalTonnage(totalTonnage);
        lb.setTotalDistanceCharge(totalDistanceCharge);
        lb.setTotalTonnageCharge(totalTonnageCharge);
        lb.setTotalDailyCharge(totalDailyCharge);

        BigDecimal subtotal = totalDistanceCharge.add(totalTonnageCharge).add(totalDailyCharge);
        lb.setSubtotal(subtotal);
        lb.setVat(subtotal.multiply(VAT_MULTIPLIER));
        lb.setTotal(subtotal.add(lb.getVat()));
        lb.setStatus("CALCULATED");
        lb.setCalculatedAt(LocalDateTime.now());
        return loadBillingRepository.save(lb);
    }

    private Invoice createInvoiceFromLoad(Load load, Customer customer, String loadId) {
        Invoice i = new Invoice();
        i.setInvoiceNumber(generateInvoiceNumber());
        i.setInvoiceType("RECEIVABLE");
        i.setCustomerId(customer.getId());
        i.setCustomerName(customer.getName());
        i.setCustomerEmail(customer.getEmail());
        i.setCustomerAddress(buildCustomerAddress(customer));
        i.setLoadId(load.getId());
        i.setInvoiceDate(LocalDateTime.now());
        i.setDueDate(LocalDateTime.now().plusDays(30));
        i.setCurrency("ZAR");
        i.setStatus("DRAFT");
        i.setDescription("Transport services for Load: " + loadId);
        i.setVatRate(VAT_RATE);
        i.setSource("LOAD_BILLING");
        i.setCreatedAt(LocalDateTime.now());
        i.setUpdatedAt(LocalDateTime.now());
        return i;
    }

    private Invoice createInvoiceFromTrip(Trip trip, Customer customer) {
        Invoice i = new Invoice();
        i.setInvoiceNumber(generateInvoiceNumber());
        i.setInvoiceType("RECEIVABLE");
        i.setCustomerId(customer.getId());
        i.setCustomerName(customer.getName());
        i.setCustomerEmail(customer.getEmail());
        i.setCustomerAddress(buildCustomerAddress(customer));
        i.setTripId(trip.getId());
        i.setInvoiceDate(LocalDateTime.now());
        i.setDueDate(LocalDateTime.now().plusDays(30));
        i.setCurrency("ZAR");
        i.setStatus("DRAFT");
        i.setDescription("Transport service - Trip " + trip.getTripNumber());
        i.setVatRate(VAT_RATE);
        i.setSource("TRIP_BILLING");
        i.setCreatedAt(LocalDateTime.now());
        i.setUpdatedAt(LocalDateTime.now());
        return i;
    }

    /**
     * One line item per {@link TripBilling}. Each item's pre-VAT amount
     * goes into {@code amount}, VAT into {@code tax_amount}, and the
     * combined figure into {@code total_amount}. Reference is polymorphic:
     * {@code reference_type='TRIP'}, {@code reference_id=trip.id}.
     */
    private List<InvoiceItem> buildTripLineItems(Invoice invoice, List<TripBilling> tripBillings) {
        List<InvoiceItem> items = new ArrayList<>();
        for (TripBilling tb : tripBillings) {
            Trip trip = tb.getTrip();
            if (trip == null) {
                log.warn("⚠️ TripBilling {} has no Trip; skipping line item", tb.getId());
                continue;
            }

            BigDecimal lineSubtotal = nz(tb.getSubtotal());
            BigDecimal lineVat      = nz(tb.getVat());
            BigDecimal lineTotal    = lineSubtotal.add(lineVat);

            InvoiceItem item = new InvoiceItem();
            item.setDescription(String.format(
                    "Trip %s: %s → %s (%.0f km, %.1f tons)",
                    trip.getTripNumber(),
                    truncate(trip.getOriginLocation(), 20),
                    truncate(trip.getDestinationLocation(), 20),
                    nz(tb.getDistanceKm()).doubleValue(),
                    nz(tb.getTonnage()).doubleValue()));
            item.setQuantity(BigDecimal.ONE);
            item.setUnitPrice(lineSubtotal);
            item.setLineTotal(lineSubtotal);          // → amount
            item.setTaxRate(VAT_RATE);
            item.setTaxAmount(lineVat);               // → tax_amount
            item.setTotalAmount(lineTotal);           // → total_amount  ✅
            item.setInvoice(invoice);
            item.setReferenceType("TRIP");            // ✅
            item.setReferenceId(trip.getId());        // ✅
            items.add(item);
        }
        return items;
    }

    /**
     * Load-level residual line (only when {@code LoadBilling.subtotal}
     * exceeds the sum of trip subtotals).
     */
    private InvoiceItem buildLoadLevelChargeItem(Invoice invoice, String loadId,
                                                 BigDecimal subtotal, BigDecimal vat,
                                                 LoadBilling lb) {
        boolean noTrips = lb.getTotalTrips() == null || lb.getTotalTrips() == 0;
        BigDecimal lineTotal = subtotal.add(vat);

        InvoiceItem item = new InvoiceItem();
        item.setDescription(String.format(
                "Load %s: %s (%.0f km total)",
                loadId,
                noTrips ? "total transport charges" : "additional load-level charges",
                nz(lb.getTotalDistanceKm()).doubleValue()));
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(subtotal);
        item.setLineTotal(subtotal);                  // → amount
        item.setTaxRate(VAT_RATE);
        item.setTaxAmount(vat);                       // → tax_amount
        item.setTotalAmount(lineTotal);               // → total_amount  ✅
        item.setInvoice(invoice);
        item.setReferenceType("LOAD");                // ✅
        item.setReferenceId(null);                    // load link lives on the Invoice row
        return item;
    }

    /**
     * Itemised charges for a single-trip invoice: distance, tonnage, daily,
     * labour, fixed surcharge — one line per non-zero component.
     */
    private List<InvoiceItem> buildTripDetailLineItems(Invoice invoice, TripBilling tb) {
        List<InvoiceItem> items = new ArrayList<>();
        Trip trip = tb.getTrip();
        Long tripId = trip != null ? trip.getId() : null;

        if (positive(tb.getDistanceCharge())) {
            items.add(perTripItem(invoice, tripId,
                    String.format("Distance charge: %.0f km @ R%.2f/km",
                            nz(tb.getDistanceKm()).doubleValue(),
                            nz(tb.getBaseRate()).doubleValue()),
                    tb.getDistanceCharge()));
        }
        if (positive(tb.getTonnageCharge())) {
            items.add(perTripItem(invoice, tripId,
                    String.format("Tonnage charge: %.1f tons",
                            nz(tb.getTonnage()).doubleValue()),
                    tb.getTonnageCharge()));
        }
        if (positive(tb.getDailyRateCharge())) {
            items.add(perTripItem(invoice, tripId,
                    String.format("Daily rate: %d day(s)",
                            tb.getDays() != null ? tb.getDays() : 1),
                    tb.getDailyRateCharge()));
        }
        if (positive(tb.getLabourCharge())) {
            items.add(perTripItem(invoice, tripId,
                    String.format("Labour: %.1f hours",
                            nz(tb.getLabourHours()).doubleValue()),
                    tb.getLabourCharge()));
        }
        if (positive(tb.getFixedSurcharge())) {
            items.add(perTripItem(invoice, tripId,
                    "Fixed surcharge", tb.getFixedSurcharge()));
        }
        return items;
    }

    /**
     * Build a single trip-linked invoice item with all four money fields
     * populated (pre-VAT, VAT, combined) so none of the NOT NULL columns
     * in {@code invoice_item} are empty.
     */
    private InvoiceItem perTripItem(Invoice invoice, Long tripId,
                                    String description, BigDecimal amount) {
        BigDecimal tax = amount.multiply(VAT_MULTIPLIER);
        BigDecimal total = amount.add(tax);

        InvoiceItem item = new InvoiceItem();
        item.setDescription(description);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(amount);
        item.setTaxRate(VAT_RATE);
        item.setLineTotal(amount);                    // → amount
        item.setTaxAmount(tax);                       // → tax_amount
        item.setTotalAmount(total);                   // → total_amount  ✅
        item.setInvoice(invoice);
        item.setReferenceType("TRIP");                // ✅
        item.setReferenceId(tripId);                  // ✅
        return item;
    }

    private void updateLoadAndTripBillings(String loadId, LoadBilling lb,
                                           List<TripBilling> tripBillings, Invoice saved) {
        lb.setInvoiceId(saved.getId());
        lb.setStatus("INVOICED");
        lb.setInvoicedAt(LocalDateTime.now());
        loadBillingRepository.save(lb);
        if (!tripBillings.isEmpty()) {
            for (TripBilling tb : tripBillings) {
                tb.setInvoiceId(saved.getId());
                tb.setStatus("INVOICED");
                tb.setInvoicedAt(LocalDateTime.now());
            }
            tripBillingRepository.saveAll(tripBillings);
        }
    }

    private String generateInvoiceNumber() {
        String year = String.valueOf(java.time.Year.now().getValue());
        return "INV-" + year + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String buildCustomerAddress(Customer c) {
        StringBuilder sb = new StringBuilder();
        if (c.getAddressLine1() != null) sb.append(c.getAddressLine1());
        if (c.getAddressLine2() != null) sb.append(", ").append(c.getAddressLine2());
        if (c.getCity()         != null) sb.append(", ").append(c.getCity());
        if (c.getProvince()     != null) sb.append(", ").append(c.getProvince());
        if (c.getPostalCode()   != null) sb.append(" ").append(c.getPostalCode());
        if (c.getCountry()      != null) sb.append(", ").append(c.getCountry());
        return sb.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "N/A";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private static boolean positive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }
}
