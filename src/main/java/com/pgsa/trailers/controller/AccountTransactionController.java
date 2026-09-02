package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.finance.AccountTransaction;
import com.pgsa.trailers.service.AccountTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account-transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AccountTransactionController {

    private final AccountTransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<AccountTransaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAll());
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<AccountTransaction>> getTransactionsByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getByAccount(accountId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountTransaction> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getById(id));
    }

    @PostMapping
    public ResponseEntity<AccountTransaction> createTransaction(@RequestBody AccountTransaction transaction) {
        return ResponseEntity.ok(transactionService.create(transaction));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<AccountTransaction> reverseTransaction(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(transactionService.reverseTransaction(id, reason));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
