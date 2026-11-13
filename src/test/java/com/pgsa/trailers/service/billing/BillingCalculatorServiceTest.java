package com.pgsa.trailers.service.billing;

import com.pgsa.trailers.entity.billing.LoadBilling;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.billing.LoadBillingRepository;
import com.pgsa.trailers.repository.billing.TripBillingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingCalculatorServiceTest {

    @Mock
    private RateResolverService rateResolverService;

    @Mock
    private TripBillingRepository tripBillingRepository;

    @Mock
    private LoadBillingRepository loadBillingRepository;

    @Mock
    private LoadRepository loadRepository;

    @Mock
    private TripRepository tripRepository;

    @Spy
    @InjectMocks
    private BillingCalculatorService billingCalculatorService;

    @Test
    void getBillableLoadsBackfillsFromLoadTableWhenBillingRowsAreMissing() {
        Load load = new Load();
        load.setId(99L);
        load.setLoadNumber("LD-100");

        Trip trip = new Trip();
        trip.setId(11L);
        trip.setLoadId("LD-100");

        LoadBilling generated = new LoadBilling();
        generated.setLoadId("LD-100");
        generated.setStatus("CALCULATED");
        generated.setInvoiceId(null);

        when(loadBillingRepository.findBillableLoads()).thenReturn(new ArrayList<>());
        when(loadRepository.findAll()).thenReturn(List.of(load));
        when(tripRepository.findByLoadId("LD-100")).thenReturn(List.of(trip));
        when(loadBillingRepository.findByLoadId("LD-100")).thenReturn(Optional.empty());
        doReturn(generated).when(billingCalculatorService).recalculateLoadBilling("LD-100", 0L);

        List<LoadBilling> result = billingCalculatorService.getBillableLoads();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLoadId()).isEqualTo("LD-100");
        assertThat(result.get(0).getStatus()).isEqualTo("CALCULATED");
    }
}
