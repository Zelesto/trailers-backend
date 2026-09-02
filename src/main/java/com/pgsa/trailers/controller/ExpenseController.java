package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.finance.Expense;
import com.pgsa.trailers.repository.finance.ExpenseRepository;
import com.pgsa.trailers.service.SequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final SequenceService sequenceService;

    private static final String TABLE_NAME = "expenses";
    private static final String PREFIX = "EXP";
    private static final int PADDING = 6;

    // ========== GET METHODS ==========

    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        log.info("GET /api/expenses - Fetching all expenses");
        try {
            return ResponseEntity.ok(expenseRepository.findAll());
        } catch (Exception e) {
            log.error("Error fetching expenses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpenseById(@PathVariable Long id) {
        log.info("GET /api/expenses/{}", id);
        return expenseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Expense>> getExpensesByCategory(@PathVariable String category) {
        log.info("GET /api/expenses/category/{}", category);
        return ResponseEntity.ok(expenseRepository.findByCategory(category));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Expense>> getExpensesByStatus(@PathVariable String status) {
        log.info("GET /api/expenses/status/{}", status);
        return ResponseEntity.ok(expenseRepository.findByStatus(status));
    }

    // ========== POST METHODS ==========

    @PostMapping
    public ResponseEntity<?> createExpense(@RequestBody Expense expense) {
        log.info("POST /api/expenses - Creating expense");
        try {
            // ✅ Use SequenceService to generate expense number
            if (expense.getExpenseNumber() == null || expense.getExpenseNumber().isEmpty()) {
                String year = String.valueOf(Year.now().getValue());
                String expenseNumber = sequenceService.generateFormattedSequence(
                    TABLE_NAME,
                    PREFIX,
                    year,
                    PADDING
                );
                expense.setExpenseNumber(expenseNumber);
                log.info("Generated expense number from sequence: {}", expenseNumber);
            }

            // Set defaults
            if (expense.getStatus() == null) {
                expense.setStatus("PENDING");
            }
            if (expense.getApprovalStatus() == null) {
                expense.setApprovalStatus("PENDING");
            }
            if (expense.getTaxRate() == null) {
                expense.setTaxRate(new BigDecimal("15.00"));
            }
            if (expense.getCurrency() == null) {
                expense.setCurrency("ZAR");
            }
            if (expense.getTaxDeductible() == null) {
                expense.setTaxDeductible(true);
            }
            if (expense.getReimbursementEligible() == null) {
                expense.setReimbursementEligible(false);
            }

            // Calculate tax amount
            if (expense.getAmount() != null && expense.getTaxRate() != null) {
                expense.setTaxAmount(expense.getAmount()
                    .multiply(expense.getTaxRate())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            }

            expense.setCreatedAt(LocalDateTime.now());
            expense.setUpdatedAt(LocalDateTime.now());

            Expense saved = expenseRepository.save(expense);
            log.info("✅ Expense created with ID: {}, Number: {}", saved.getId(), saved.getExpenseNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            log.error("Error creating expense: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== PUT METHODS ==========

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable Long id, @RequestBody Expense expense) {
        log.info("PUT /api/expenses/{}", id);
        try {
            if (!expenseRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            expense.setId(id);
            expense.setUpdatedAt(LocalDateTime.now());

            // Recalculate tax amount
            if (expense.getAmount() != null && expense.getTaxRate() != null) {
                expense.setTaxAmount(expense.getAmount()
                    .multiply(expense.getTaxRate())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            }

            Expense updated = expenseRepository.save(expense);
            log.info("✅ Expense updated: {}", id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating expense: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== PATCH METHODS ==========

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateExpenseStatus(@PathVariable Long id, @RequestParam String status) {
        log.info("PATCH /api/expenses/{}/status -> {}", id, status);
        try {
            Expense expense = expenseRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Expense not found: " + id));

            expense.setStatus(status);
            expense.setUpdatedAt(LocalDateTime.now());

            Expense updated = expenseRepository.save(expense);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating expense status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveExpense(@PathVariable Long id, @RequestParam Long approvedBy) {
        log.info("PATCH /api/expenses/{}/approve by {}", id, approvedBy);
        try {
            Expense expense = expenseRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Expense not found: " + id));

            expense.setApprovalStatus("APPROVED");
            expense.setApprovedBy(approvedBy);
            expense.setApprovedDate(LocalDateTime.now());
            expense.setUpdatedAt(LocalDateTime.now());

            Expense updated = expenseRepository.save(expense);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error approving expense: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectExpense(@PathVariable Long id, @RequestParam Long rejectedBy) {
        log.info("PATCH /api/expenses/{}/reject by {}", id, rejectedBy);
        try {
            Expense expense = expenseRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Expense not found: " + id));

            expense.setApprovalStatus("REJECTED");
            expense.setApprovedBy(rejectedBy);
            expense.setApprovedDate(LocalDateTime.now());
            expense.setUpdatedAt(LocalDateTime.now());

            Expense updated = expenseRepository.save(expense);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error rejecting expense: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== DELETE METHODS ==========

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        log.info("DELETE /api/expenses/{}", id);
        try {
            if (!expenseRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            expenseRepository.deleteById(id);
            log.info("✅ Expense deleted: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting expense: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
