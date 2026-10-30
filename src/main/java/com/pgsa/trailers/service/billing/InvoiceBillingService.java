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

    private static final BigDecimal VAT_RATE = new BigDecimal("15.00");
    private static final BigDecimal VAT_MULTIPLIER = new BigDecimal("0.15");

    private final LoadBillingRepository loadBillingRepository;
    private final TripBillingRepository tripBillingRepository;
    private final LoadRepository loadRepository;
    private final CustomerRepository customerRepository;
    private final TripRepository tripRepository;
    private final BillingCalculatorService billingCalculatorService;
    private final InvoiceService invoiceService;   // ✅ routed-through for consistent defaults

    // ---------------------------------------------------------------------
    // PUBLIC API
    // ---------------------------------------------------------------------

    /**
     * Generate an invoice from a Load's billing.
     * <p>
     * Totals are taken from the authoritative {@link LoadBilling}; items are
     * constructed so their sum matches those totals (trip line items plus a
     * single load-level residual line if the load carries charges not
     * represented in any {@link TripBilling}).
     */
    @Transactional
    public Invoice generateInvoiceFromLoad(String loadId, Long userId) {
        log.info("📄 Generating invoice for Load: {}", loadId);

        Load load = loadRepository.findByLoadNumber(loadId)
                .orElseThrow(() -> new IllegalArgumentException("Load not found: " + loadId));

        LoadBilling loadBilling = loadBillingRepository.findByLoadId(loadId)
                .orElseGet(() -> billingCalculatorService.recalculateLoadBilling(loadId, userId));

        if (loadBilling.getInvoiceId() != null) {
            throw new IllegalStateException(
                    "Load already has an invoice: " + loadBilling.getInvoiceId());
        }

        // ✅ Resolve customer via fallback chain: LoadBilling → Load
        Long customerId = loadBilling.getCustomerId() != null
                ? loadBilling.getCustomerId()
                : load.getCustomerId();
        if (customerId == null) {
            throw new IllegalStateException(
                    "No customer associated with Load " + loadId
                            + " or its billing record. Please assign a customer first.");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found for id: " + customerId));

        // ✅ Guard against uncomputed billing (avoids a downstream DB NOT NULL crash)
        BigDecimal subtotal = nz(loadBilling.getSubtotal());
        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException(
                    "Cannot generate invoice for Load " + loadId
                            + ": billing subtotal is zero. Recalculate billing first.");
        }

        Invoice invoice = createInvoiceFromLoad(load, customer, loadId);

        List<TripBilling> tripBillings = tripBillingRepository.findByTrip_LoadId(loadId);
        List<InvoiceItem> items = buildTripLineItems(invoice, tripBillings);

        // ✅ Add a residual load-level line only if the load carries charges not
        //    already represented by trip billings (prevents double counting).
        BigDecimal tripSubtotal = tripBillings.stream()
                .map(tb -> nz(tb.getSubtotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tripVat = tripBillings.stream()
                .map(tb -> nz(tb.getVat()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal loadOnlySubtotal = subtotal.subtract(tripSubtotal);
        BigDecimal loadOnlyVat      = nz(loadBilling.getVat()).subtract(tripVat);

        if (loadOnlySubtotal.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("⚠️ Trip subtotals exceed load subtotal for {}. Load={}, Trips={}. "
                            + "Skipping load-level residual line.",
                    loadId, subtotal, tripSubtotal);
        } else if (loadOnlySubtotal.compareTo(BigDecimal.ZERO) > 0
                || loadOnlyVat.compareTo(BigDecimal.ZERO) > 0) {
            items.add(buildLoadLevelChargeItem(
                    invoice, loadId, loadOnlySubtotal, loadOnlyVat, loadBilling));
        }

        invoice.setItems(items);

        // ✅ Totals are authoritative from LoadBilling (not from item sum)
        invoice.setSubtotal(subtotal);
        invoice.setTaxTotal(nz(loadBilling.getVat()));
        invoice.setTotalAmount(nz(loadBilling.getTotal()));

        // ✅ Persist via InvoiceService so audit fields (createdAt/updatedAt/source)
        //    are guaranteed, but skip recalculateTotals() to preserve explicit totals.
        Invoice savedInvoice = invoiceService.createInvoicePreservingTotals(invoice);

        updateLoadAndTripBillings(loadId, loadBilling, tripBillings, savedInvoice);

        log.info("✅ Invoice {} generated for Load {}", savedInvoice.getInvoiceNumber(), loadId);
        return savedInvoice;
    }

    /**
     * Generate an invoice from a single Trip.
     */
    @Transactional
    public Invoice generateInvoiceFromTrip(Long tripId, Long userId) {
        log.info("📄 Generating invoice for Trip: {}", tripId);

        TripBilling tripBilling = tripBillingRepository.findByTripId(tripId);
        if (tripBilling == null) {
            throw new IllegalArgumentException("Trip billing not found for trip id: " + tripId);
        }
        if (tripBilling.getInvoiceId() != null) {
            throw new IllegalStateException(
                    "Trip already has an invoice: " + tripBilling.getInvoiceId());
        }

        Trip trip = tripBilling.getTrip();
        if (trip == null) {
            throw new IllegalStateException(
                    "TripBilling " + tripId + " has no associated Trip record");
        }

        // ✅ Customer fallback: TripBilling → Trip
        Long customerId = tripBilling.getCustomerId() != null
                ? tripBilling.getCustomerId()
                : trip.getCustomerId();
        if (customerId == null) {
            throw new IllegalStateException(
                    "No customer associated with Trip " + tripId);
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found for id: " + customerId));

        BigDecimal subtotal = nz(tripBilling.getSubtotal());
        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException(
                    "Cannot generate invoice for Trip " + tripId
                            + ": billing subtotal is zero.");
        }

        Invoice invoice = createInvoiceFromTrip(trip, customer);
        List<InvoiceItem> items = buildTripDetailLineItems(invoice, tripBilling);
        invoice.setItems(items);
        invoice.setSubtotal(subtotal);
        invoice.setTaxTotal(nz(tripBilling.getVat()));
        invoice.setTotalAmount(nz(tripBilling.getTotal()));

        Invoice savedInvoice = invoiceService.createInvoicePreservingTotals(invoice);

        tripBilling.setInvoiceId(savedInvoice.getId());
        tripBilling.setStatus("INVOICED");
        tripBilling.setInvoicedAt(LocalDateTime.now());
        tripBillingRepository.save(tripBilling);

        log.info("✅ Invoice {} generated for Trip {}", savedInvoice.getInvoiceNumber(), tripId);
        return savedInvoice;
    }

    /**
     * Build a preview summary for a Load's billing (before invoice generation).
     * <p>
     * ✅ NOT read-only: this method may create a {@link LoadBilling} if missing.
     */
    @Transactional
    public LoadBillingSummary getLoadBillingSummary(String loadId) {
        log.info("📊 Getting billing summary for Load: {}", loadId);

        Load load = loadRepository.findByLoadNumber(loadId)
                .orElseThrow(() -> new IllegalArgumentException("Load not found: " + loadId));

        LoadBilling loadBilling = loadBillingRepository.findByLoadId(loadId)
                .orElseGet(() -> {
                    log.info("⚠️ LoadBilling not found, creating from trip data");
                    return createLoadBillingFromTrips(loadId);
                });

        List<TripBilling> tripBillings = tripBillingRepository.findByTrip_LoadId(loadId);

        String customerName = null;
        Long resolvedCustomerId = loadBilling.getCustomerId() != null
                ? loadBilling.getCustomerId()
                : load.getCustomerId();
        if (resolvedCustomerId != null) {
            customerName = customerRepository.findById(resolvedCustomerId)
                    .map(Customer::getName)
                    .orElse(null);
        }

        BigDecimal subtotal    = nz(loadBilling.getSubtotal());
        BigDecimal vat         = nz(loadBilling.getVat());
        BigDecimal totalAmount = nz(loadBilling.getTotal());

        long totalBillable = tripBillings.stream()
                .filter(tb -> "CALCULATED".equals(tb.getStatus()))
                .count();
        long totalInvoiced = tripBillings.stream()
                .filter(tb -> "INVOICED".equals(tb.getStatus()))
                .count();

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
                .status(loadBilling.getStatus() != null ? loadBilling.getStatus() : "DRAFT")
                .canInvoice(loadBilling.getInvoiceId() == null
                        && !tripBillings.isEmpty()
                        && subtotal.compareTo(BigDecimal.ZERO) > 0)
                .build();
    }

    // ---------------------------------------------------------------------
    // PRIVATE HELPERS
    // ---------------------------------------------------------------------

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /**
     * ✅ Create a LoadBilling from trip data. Falls back to the load's customerId
     *    if the trip records don't carry one, so downstream invoicing never NPEs
     *    on a null customer.
     */
    private LoadBilling createLoadBillingFromTrips(String loadId) {
        List<Trip> trips = tripRepository.findByLoadId(loadId);
        if (trips.isEmpty()) {
            throw new IllegalStateException("No trips found for load: " + loadId);
        }

        Long fallbackCustomerId = loadRepository.findByLoadNumber(loadId)
                .map(Load::getCustomerId)
                .orElse(null);
        Long tripCustomerId = trips.get(0).getCustomerId();
        Long resolvedCustomerId = tripCustomerId != null ? tripCustomerId : fallbackCustomerId;
        if (resolvedCustomerId == null) {
            throw new IllegalStateException(
                    "Cannot determine customer for load " + loadId
                            + ": neither trips nor load have a customerId.");
        }

        LoadBilling loadBilling = new LoadBilling();
        loadBilling.setLoadId(loadId);
        loadBilling.setCustomerId(resolvedCustomerId);
        loadBilling.setTotalTrips(trips.size());

        BigDecimal totalDistance       = BigDecimal.ZERO;
        BigDecimal totalTonnage        = BigDecimal.ZERO;
        BigDecimal totalDistanceCharge = BigDecimal.ZERO;
        BigDecimal totalTonnageCharge  = BigDecimal.ZERO;
        BigDecimal totalDailyCharge    = BigDecimal.ZERO;

        for (Trip trip : trips) {
            if (trip.getCalculatedDistanceKm() != null) {
                totalDistance = totalDistance.add(trip.getCalculatedDistanceKm());
            }
            if (trip.getCargoWeight() != null) {
                totalTonnage = totalTonnage.add(trip.getCargoWeight());
            }
            if (trip.getCalculatedDistanceKm() != null) {
                totalDistanceCharge = totalDistanceCharge.add(
                        trip.getCalculatedDistanceKm().multiply(new BigDecimal("1.50")));
            }
            if (trip.getCargoWeight() != null) {
                totalTonnageCharge = totalTonnageCharge.add(
                        trip.getCargoWeight().multiply(new BigDecimal("50.00")));
            }
            totalDailyCharge = totalDailyCharge.add(new BigDecimal("2500.00"));
        }

        loadBilling.setTotalDistanceKm(totalDistance);
        loadBilling.setTotalTonnage(totalTonnage);
        loadBilling.setTotalDistanceCharge(totalDistanceCharge);
        loadBilling.setTotalTonnageCharge(totalTonnageCharge);
        loadBilling.setTotalDailyCharge(totalDailyCharge);

        BigDecimal subtotal = totalDistanceCharge.add(totalTonnageCharge).add(totalDailyCharge);
        loadBilling.setSubtotal(subtotal);
        loadBilling.setVat(subtotal.multiply(VAT_MULTIPLIER));
        loadBilling.setTotal(subtotal.add(loadBilling.getVat()));
        loadBilling.setStatus("CALCULATED");
        loadBilling.setCalculatedAt(LocalDateTime.now());

        return loadBillingRepository.save(loadBilling);
    }

    private Invoice createInvoiceFromLoad(Load load, Customer customer, String loadId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setInvoiceType("RECEIVABLE");
        invoice.setCustomerId(customer.getId());
        invoice.setCustomerName(customer.getName());
        invoice.setCustomerEmail(customer.getEmail());
        invoice.setCustomerAddress(buildCustomerAddress(customer));
        invoice.setLoadId(load.getId());
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setDueDate(LocalDateTime.now().plusDays(30));
        invoice.setCurrency("ZAR");
        invoice.setStatus("DRAFT");
        invoice.setDescription("Transport services for Load: " + loadId);
        invoice.setVatRate(VAT_RATE);
        // ✅ Audit / source fields that InvoiceService normally sets
        invoice.setSource("LOAD_BILLING");
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setUpdatedAt(LocalDateTime.now());
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
        invoice.setTripId(trip.getId());
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setDueDate(LocalDateTime.now().plusDays(30));
        invoice.setCurrency("ZAR");
        invoice.setStatus("DRAFT");
        invoice.setDescription("Transport service - Trip " + trip.getTripNumber());
        invoice.setVatRate(VAT_RATE);
        invoice.setSource("TRIP_BILLING");
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setUpdatedAt(LocalDateTime.now());
        return invoice;
    }

    /**
     * ✅ Line totals now use {@link TripBilling#getSubtotal()} (pre-VAT) so the
     *    sum of line items matches {@code invoice.subtotal}. Using {@code getTotal()}
     *    would double-count VAT.
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

            InvoiceItem item = new InvoiceItem();
            item.setDescription(String.format(
                    "Trip %s: %s → %s (%.0f km, %.1f tons)",
                    trip.getTripNumber(),
                    truncate(trip.getOriginLocation(), 20),
                    truncate(trip.getDestinationLocation(), 20),
                    nz(tb.getDistanceKm()).doubleValue(),
                    nz(tb.getTonnage()).doubleValue()
            ));
            item.setQuantity(BigDecimal.ONE);
            item.setUnitPrice(lineSubtotal);
            item.setTaxRate(VAT_RATE);
            item.setLineTotal(lineSubtotal);
            item.setTaxAmount(lineVat);
            item.setInvoice(invoice);
            item.setTripId(trip.getId());
            item.setLoadId(trip.getLoadId());
            items.add(item);
        }
        return items;
    }

    /**
     * ✅ Load-level residual line: represents charges not accounted for by any
     *    TripBilling. Both subtotal and VAT are passed explicitly so the sum of
     *    invoice items aligns with {@link LoadBilling} totals.
     */
    private InvoiceItem buildLoadLevelChargeItem(Invoice invoice, String loadId,
                                                 BigDecimal subtotal, BigDecimal vat,
                                                 LoadBilling loadBilling) {
        boolean noTrips = loadBilling.getTotalTrips() == null
                || loadBilling.getTotalTrips() == 0;

        InvoiceItem item = new InvoiceItem();
        item.setDescription(String.format(
                "Load %s: %s (%.0f km total)",
                loadId,
                noTrips ? "total transport charges" : "additional load-level charges",
                nz(loadBilling.getTotalDistanceKm()).doubleValue()
        ));
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(subtotal);
        item.setTaxRate(VAT_RATE);
        item.setLineTotal(subtotal);
        item.setTaxAmount(vat);
        item.setInvoice(invoice);
        item.setLoadId(loadId);
        return item;
    }

    private List<InvoiceItem> buildTripDetailLineItems(Invoice invoice, TripBilling tripBilling) {
        List<InvoiceItem> items = new ArrayList<>();
        Trip trip = tripBilling.getTrip();
        Long tripId   = trip != null ? trip.getId() : null;
        String tripLoad = trip != null ? trip.getLoadId() : null;

        if (isPositive(tripBilling.getDistanceCharge())) {
            InvoiceItem distItem = new InvoiceItem();
            distItem.setDescription(String.format(
                    "Distance charge: %.0f km @ R%.2f/km",
                    nz(tripBilling.getDistanceKm()).doubleValue(),
                    nz(tripBilling.getBaseRate()).doubleValue()));
            distItem.setQuantity(BigDecimal.ONE);
            distItem.setUnitPrice(tripBilling.getDistanceCharge());
            distItem.setTaxRate(VAT_RATE);
            distItem.setLineTotal(tripBilling.getDistanceCharge());
            distItem.setTaxAmount(tripBilling.getDistanceCharge().multiply(VAT_MULTIPLIER));
            distItem.setInvoice(invoice);
            distItem.setTripId(tripId);
            distItem.setLoadId(tripLoad);
            items.add(distItem);
        }

        if (isPositive(tripBilling.getTonnageCharge())) {
            InvoiceItem tonItem = new InvoiceItem();
            tonItem.setDescription(String.format(
                    "Tonnage charge: %.1f tons",
                    nz(tripBilling.getTonnage()).doubleValue()));
            tonItem.setQuantity(BigDecimal.ONE);
            tonItem.setUnitPrice(tripBilling.getTonnageCharge());
            tonItem.setTaxRate(VAT_RATE);
            tonItem.setLineTotal(tripBilling.getTonnageCharge());
            tonItem.setTaxAmount(tripBilling.getTonnageCharge().multiply(VAT_MULTIPLIER));
            tonItem.setInvoice(invoice);
            tonItem.setTripId(tripId);
            tonItem.setLoadId(tripLoad);
            items.add(tonItem);
        }

        if (isPositive(tripBilling.getDailyRateCharge())) {
            InvoiceItem dailyItem = new InvoiceItem();
            dailyItem.setDescription(String.format(
                    "Daily rate: %d day(s)",
                    tripBilling.getDays() != null ? tripBilling.getDays() : 1));
            dailyItem.setQuantity(BigDecimal.ONE);
            dailyItem.setUnitPrice(tripBilling.getDailyRateCharge());
            dailyItem.setTaxRate(VAT_RATE);
            dailyItem.setLineTotal(tripBilling.getDailyRateCharge());
            dailyItem.setTaxAmount(tripBilling.getDailyRateCharge().multiply(VAT_MULTIPLIER));
            dailyItem.setInvoice(invoice);
            dailyItem.setTripId(tripId);
            dailyItem.setLoadId(tripLoad);
            items.add(dailyItem);
        }

        if (isPositive(tripBilling.getLabourCharge())) {
            InvoiceItem labourItem = new InvoiceItem();
            labourItem.setDescription(String.format(
                    "Labour: %.1f hours",
                    nz(tripBilling.getLabourHours()).doubleValue()));
            labourItem.setQuantity(BigDecimal.ONE);
            labourItem.setUnitPrice(tripBilling.getLabourCharge());
            labourItem.setTaxRate(VAT_RATE);
            labourItem.setLineTotal(tripBilling.getLabourCharge());
            labourItem.setTaxAmount(tripBilling.getLabourCharge().multiply(VAT_MULTIPLIER));
            labourItem.setInvoice(invoice);
            labourItem.setTripId(tripId);
            labourItem.setLoadId(tripLoad);
            items.add(labourItem);
        }

        if (isPositive(tripBilling.getFixedSurcharge())) {
            InvoiceItem fixedItem = new InvoiceItem();
            fixedItem.setDescription("Fixed surcharge");
            fixedItem.setQuantity(BigDecimal.ONE);
            fixedItem.setUnitPrice(tripBilling.getFixedSurcharge());
            fixedItem.setTaxRate(VAT_RATE);
            fixedItem.setLineTotal(tripBilling.getFixedSurcharge());
            fixedItem.setTaxAmount(tripBilling.getFixedSurcharge().multiply(VAT_MULTIPLIER));
            fixedItem.setInvoice(invoice);
            fixedItem.setTripId(tripId);
            fixedItem.setLoadId(tripLoad);
            items.add(fixedItem);
        }

        return items;
    }

    private void updateLoadAndTripBillings(String loadId, LoadBilling loadBilling,
                                           List<TripBilling> tripBillings, Invoice savedInvoice) {
        loadBilling.setInvoiceId(savedInvoice.getId());
        loadBilling.setStatus("INVOICED");
        loadBilling.setInvoicedAt(LocalDateTime.now());
        loadBillingRepository.save(loadBilling);

        if (!tripBillings.isEmpty()) {
            for (TripBilling tb : tripBillings) {
                tb.setInvoiceId(savedInvoice.getId());
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

    private String buildCustomerAddress(Customer customer) {
        StringBuilder sb = new StringBuilder();
        if (customer.getAddressLine1() != null) sb.append(customer.getAddressLine1());
        if (customer.getAddressLine2() != null) sb.append(", ").append(customer.getAddressLine2());
        if (customer.getCity()         != null) sb.append(", ").append(customer.getCity());
        if (customer.getProvince()     != null) sb.append(", ").append(customer.getProvince());
        if (customer.getPostalCode()   != null) sb.append(" ").append(customer.getPostalCode());
        if (customer.getCountry()      != null) sb.append(", ").append(customer.getCountry());
        return sb.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "N/A";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private static boolean isPositive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }
}
