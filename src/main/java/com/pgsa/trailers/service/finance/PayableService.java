package com.pgsa.trailers.service.finance;

import com.pgsa.trailers.entity.finance.Payable;
import com.pgsa.trailers.repository.finance.PayableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayableService {

    private final PayableRepository payableRepository;

    @Transactional
    public Payable createPayable(Payable payable) {
        if (payable.getPayableNumber() == null) {
            payable.setPayableNumber(generatePayableNumber());
        }
        if (payable.getStatus() == null) {
            payable.setStatus("PENDING");  // ✅ Use String
        }
        if (payable.getBalanceDue() == null) {
            payable.setBalanceDue(payable.getOriginalAmount());
        }
        if (payable.getApprovalStatus() == null) {
            payable.setApprovalStatus("PENDING");
        }
        payable.setCreatedAt(LocalDateTime.now());
        payable.setUpdatedAt(LocalDateTime.now());
        
        return payableRepository.save(payable);
    }

    @Transactional
    public Payable recordPayment(Long payableId, BigDecimal paymentAmount) {
        Payable payable = payableRepository.findById(payableId)
                .orElseThrow(() -> new RuntimeException("Payable not found"));

        BigDecimal newPaidAmount = payable.getAmountPaid().add(paymentAmount);
        payable.setAmountPaid(newPaidAmount);
        
        BigDecimal newBalance = payable.getOriginalAmount().subtract(newPaidAmount);
        payable.setBalanceDue(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            payable.setStatus("PAID");  // ✅ Use String
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            payable.setStatus("PARTIAL");  // ✅ Use String
        }

        payable.setUpdatedAt(LocalDateTime.now());
        return payableRepository.save(payable);
    }

    @Transactional
    public Payable approvePayable(Long payableId, Long approvedBy) {
        Payable payable = payableRepository.findById(payableId)
                .orElseThrow(() -> new RuntimeException("Payable not found"));
        
        payable.setApprovalStatus("APPROVED");
        payable.setApprovedBy(approvedBy);
        payable.setApprovedDate(LocalDateTime.now());
        payable.setUpdatedAt(LocalDateTime.now());
        
        return payableRepository.save(payable);
    }

    @Transactional
    public void markOverduePayables() {
        List<Payable> payables = payableRepository.findOverduePayables(LocalDate.now());
        for (Payable payable : payables) {
            if (!"PAID".equals(payable.getStatus())) {  // ✅ Use String
                payable.setStatus("OVERDUE");  // ✅ Use String
                payable.setUpdatedAt(LocalDateTime.now());
            }
        }
        payableRepository.saveAll(payables);
        log.info("Marked {} payables as overdue", payables.size());
    }

    private String generatePayableNumber() {
        return "PAY-" + LocalDate.now().getYear() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
