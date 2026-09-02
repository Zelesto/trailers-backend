package com.pgsa.trailers.service.finance;

import com.pgsa.trailers.entity.finance.Receivable;
import com.pgsa.trailers.repository.finance.ReceivableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableService {

    private final ReceivableRepository receivableRepository;

    @Transactional
    public Receivable createReceivable(Receivable receivable) {
        if (receivable.getReceivableNumber() == null) {
            receivable.setReceivableNumber(generateReceivableNumber());
        }
        if (receivable.getStatus() == null) {
            receivable.setStatus("PENDING");  // ✅ Use String
        }
        if (receivable.getBalanceDue() == null) {
            receivable.setBalanceDue(receivable.getOriginalAmount());
        }
        receivable.setCreatedAt(LocalDateTime.now());
        receivable.setUpdatedAt(LocalDateTime.now());
        
        return receivableRepository.save(receivable);
    }

    @Transactional
    public Receivable recordPayment(Long receivableId, BigDecimal paymentAmount) {
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new RuntimeException("Receivable not found"));

        BigDecimal newPaidAmount = receivable.getAmountPaid().add(paymentAmount);
        receivable.setAmountPaid(newPaidAmount);
        
        BigDecimal newBalance = receivable.getOriginalAmount().subtract(newPaidAmount);
        receivable.setBalanceDue(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            receivable.setStatus("PAID");  // ✅ Use String
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            receivable.setStatus("PARTIAL");  // ✅ Use String
        }

        receivable.setUpdatedAt(LocalDateTime.now());
        return receivableRepository.save(receivable);
    }

    @Transactional
    public void markAsOverdue(Long receivableId) {
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new RuntimeException("Receivable not found"));
        
        if (receivable.getDueDate().isBefore(LocalDate.now()) && 
            !"PAID".equals(receivable.getStatus())) {  // ✅ Use String
            receivable.setStatus("OVERDUE");  // ✅ Use String
            receivable.setUpdatedAt(LocalDateTime.now());
            receivableRepository.save(receivable);
        }
    }

    private String generateReceivableNumber() {
        return "REC-" + LocalDate.now().getYear() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
