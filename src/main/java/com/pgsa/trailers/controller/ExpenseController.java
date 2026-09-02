package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.finance.Expense;
import com.pgsa.trailers.repository.finance.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;

    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        log.info("GET /api/expenses - Fetching all expenses");
        return ResponseEntity.ok(expenseRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpenseById(@PathVariable Long id) {
        return expenseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Expense> createExpense(@RequestBody Expense expense) {
        log.info("POST /api/expenses - Creating expense");
        return ResponseEntity.ok(expenseRepository.save(expense));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(@PathVariable Long id, @RequestBody Expense expense) {
        if (!expenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        expense.setId(id);
        return ResponseEntity.ok(expenseRepository.save(expense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Expense>> getExpensesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(expenseRepository.findByCategory(category));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Expense>> getExpensesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(expenseRepository.findByStatus(status));
    }
}
