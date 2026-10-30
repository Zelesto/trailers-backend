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

    @Transactional   // NOT read-only — may create a LoadBilling
    public LoadBillingSummary getLoadBillingSummary(String loadId) {
        log.info("📊 Getting billing summary for Load: {}", loadId);

        Load load = loadRepository.findByLoadNumber(loadId)
                .orElseThrow(() -> new IllegalArgumentException("Load not found: " + loadId));

        LoadBilling lb = loadBillingRepository.findByLoadId(loadId)
                .orElseGet(() -> createLoadBillingFromTrips(loadId));

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

    // ---------------------------------------------------------------------

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private LoadBilling createLoadBillingFromTrips(String loadId) {
        List<Trip> trips = tripRepository.findByLoadId(loadId);
        if (trips.isEmpty()) throw new IllegalStateException("No trips found for load: " + loadId);

        Long fallback = loadRepository.findByLoadNumber(loadId)
                .map(Load::getCustomerId).orElse(null);
        Long tripCustomer = trips.get(0).getCustomerId();
        Long resolved = tripCustomer != null ? tripCustomer : fallback;
        if (resolved == null) {
            throw new IllegalStateException(
                    "Cannot determine customer for load " + loadId);
        }

        LoadBilling lb = new LoadBilling();
        lb.setLoadId(loadId);
        lb.setCustomerId(resolved);
        lb.setTotalTrips(trips.size());

        BigDecimal dist = BigDecimal.ZERO, ton = BigDecimal.ZERO;
        BigDecimal distCharge = BigDecimal.ZERO, tonCharge = BigDecimal.ZERO;
        BigDecimal dailyCharge = BigDecimal.ZERO;
        for (Trip t : trips) {
            if (t.getCalculatedDistanceKm() != null) {
                dist = dist.add(t.getCalculatedDistanceKm());
                distCharge = distCharge.add(t.getCalculatedDistanceKm()
                        .multiply(new BigDecimal("18.50")));
            }
            if (t.getCargoWeight() != null) {
                ton = ton.add(t.getCargoWeight());
                tonCharge = tonCharge.add(t.getCargoWeight()
                        .multiply(new BigDecimal("50.00")));
            }
            dailyCharge = dailyCharge.add(new BigDecimal("2500.00"));
        }
        lb.setTotalDistanceKm(dist);
        lb.setTotalTonnage(ton);
        lb.setTotalDistanceCharge(distCharge);
        lb.setTotalTonnageCharge(tonCharge);
        lb.setTotalDailyCharge(dailyCharge);

        BigDecimal subtotal = distCharge.add(tonCharge).add(dailyCharge);
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

    private List<InvoiceItem> buildTripLineItems(Invoice invoice, List<TripBilling> tripBillings) {
        List<InvoiceItem> items = new ArrayList<>();
        for (TripBilling tb : tripBillings) {
            Trip t = tb.getTrip();
            if (t == null) continue;

            InvoiceItem item = new InvoiceItem();
            item.setDescription(String.format(
                    "Trip %s: %s → %s (%.0f km, %.1f tons)",
                    t.getTripNumber(),
                    truncate(t.getOriginLocation(), 20),
                    truncate(t.getDestinationLocation(), 20),
                    nz(tb.getDistanceKm()).doubleValue(),
                    nz(tb.getTonnage()).doubleValue()));
            item.setQuantity(BigDecimal.ONE);
            item.setUnitPrice(nz(tb.getSubtotal()));
            item.setTaxRate(VAT_RATE);
            item.setLineTotal(nz(tb.getSubtotal()));
            item.setTaxAmount(nz(tb.getVat()));
            item.setInvoice(invoice);
            item.setTripId(t.getId());
            item.setLoadId(t.getLoadId());
            items.add(item);
        }
        return items;
    }

    private InvoiceItem buildLoadLevelChargeItem(Invoice invoice, String loadId,
                                                 BigDecimal subtotal, BigDecimal vat,
                                                 LoadBilling lb) {
        boolean noTrips = lb.getTotalTrips() == null || lb.getTotalTrips() == 0;
        InvoiceItem item = new InvoiceItem();
        item.setDescription(String.format(
                "Load %s: %s (%.0f km total)",
                loadId,
                noTrips ? "total transport charges" : "additional load-level charges",
                nz(lb.getTotalDistanceKm()).doubleValue()));
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(subtotal);
        item.setTaxRate(VAT_RATE);
        item.setLineTotal(subtotal);
        item.setTaxAmount(vat);
        item.setInvoice(invoice);
        item.setLoadId(loadId);
        return item;
    }

    private List<InvoiceItem> buildTripDetailLineItems(Invoice invoice, TripBilling tb) {
        List<InvoiceItem> items = new ArrayList<>();
        Trip t = tb.getTrip();
        Long tripId = t != null ? t.getId() : null;
        String loadId = t != null ? t.getLoadId() : null;

        if (positive(tb.getDistanceCharge())) {
            items.add(perTripItem(invoice, tripId, loadId,
                    String.format("Distance charge: %.0f km @ R%.2f/km",
                            nz(tb.getDistanceKm()).doubleValue(),
                            nz(tb.getBaseRate()).doubleValue()),
                    tb.getDistanceCharge()));
        }
        if (positive(tb.getTonnageCharge())) {
            items.add(perTripItem(invoice, tripId, loadId,
                    String.format("Tonnage charge: %.1f tons",
                            nz(tb.getTonnage()).doubleValue()),
                    tb.getTonnageCharge()));
        }
        if (positive(tb.getDailyRateCharge())) {
            items.add(perTripItem(invoice, tripId, loadId,
                    String.format("Daily rate: %d day(s)",
                            tb.getDays() != null ? tb.getDays() : 1),
                    tb.getDailyRateCharge()));
        }
        if (positive(tb.getLabourCharge())) {
            items.add(perTripItem(invoice, tripId, loadId,
                    String.format("Labour: %.1f hours",
                            nz(tb.getLabourHours()).doubleValue()),
                    tb.getLabourCharge()));
        }
        if (positive(tb.getFixedSurcharge())) {
            items.add(perTripItem(invoice, tripId, loadId,
                    "Fixed surcharge", tb.getFixedSurcharge()));
        }
        return items;
    }

    private InvoiceItem perTripItem(Invoice invoice, Long tripId, String loadId,
                                    String description, BigDecimal amount) {
        InvoiceItem item = new InvoiceItem();
        item.setDescription(description);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(amount);
        item.setTaxRate(VAT_RATE);
        item.setLineTotal(amount);
        item.setTaxAmount(amount.multiply(VAT_MULTIPLIER));
        item.setInvoice(invoice);
        item.setTripId(tripId);
        item.setLoadId(loadId);
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

    private String truncate(String t, int max) {
        if (t == null) return "N/A";
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }

    private static boolean positive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }
}
