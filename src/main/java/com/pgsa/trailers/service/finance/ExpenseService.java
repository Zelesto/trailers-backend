package com.pgsa.trailers.service.finance;

import com.pgsa.trailers.entity.finance.Expense;
import com.pgsa.trailers.repository.finance.ExpenseRepository;
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
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Transactional
    public Expense createExpense(Expense expense) {
        if (expense.getExpenseNumber() == null) {
            expense.setExpenseNumber(generateExpenseNumber());
        }
        if (expense.getStatus() == null) {
            expense.setStatus("PENDING");  // ✅ Use String
        }
        if (expense.getApprovalStatus() == null) {
            expense.setApprovalStatus("PENDING");
        }
        if (expense.getTaxRate() == null) {
            expense.setTaxRate(new BigDecimal("15.00"));
        }
        if (expense.getTaxDeductible() == null) {
            expense.setTaxDeductible(true);
        }
        
        // Calculate tax amount
        if (expense.getAmount() != null && expense.getTaxRate() != null) {
            expense.setTaxAmount(expense.getAmount()
                .multiply(expense.getTaxRate())
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP));
        }
        
        expense.setCreatedAt(LocalDateTime.now());
        expense.setUpdatedAt(LocalDateTime.now());
        
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense approveExpense(Long expenseId, Long approvedBy) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        
        expense.setApprovalStatus("APPROVED");
        expense.setApprovedBy(approvedBy);
        expense.setApprovedDate(LocalDateTime.now());
        expense.setUpdatedAt(LocalDateTime.now());
        
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense markAsPaid(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        
        expense.setStatus("PAID");  // ✅ Use String
        expense.setUpdatedAt(LocalDateTime.now());
        
        return expenseRepository.save(expense);
    }

    @Transactional
    public void deleteExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        
        if ("PAID".equals(expense.getStatus())) {  // ✅ Use String
            throw new RuntimeException("Cannot delete a paid expense");
        }
        
        expenseRepository.deleteById(expenseId);
    }

    private String generateExpenseNumber() {
        return "EXP-" + LocalDate.now().getYear() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
