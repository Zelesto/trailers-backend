package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.finance.AccountTransaction;
import com.pgsa.trailers.service.AccountTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/account-transactions")
public class AccountTransactionController {

    private final AccountTransactionService transactionService;

    public AccountTransactionController(AccountTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public AccountTransaction create(@RequestBody AccountTransaction tx) {
        return transactionService.create(tx);
    }

    @GetMapping
    public List<AccountTransaction> getAll() {
        return transactionService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountTransaction> getById(@PathVariable Long id) {
        AccountTransaction tx = transactionService.getById(id);
        return tx != null ? ResponseEntity.ok(tx) : ResponseEntity.notFound().build();
    }

    @GetMapping("/account/{accountId}")
    public List<AccountTransaction> getByAccount(@PathVariable Long accountId) {
        return transactionService.getByAccount(accountId);
    }

    @GetMapping("/account/{accountId}/pending")
    public List<AccountTransaction> getPending(@PathVariable Long accountId) {
        return transactionService.getPendingForReconciliation(accountId);
    }


    @PutMapping("/{id}")
    public ResponseEntity<AccountTransaction> update(@PathVariable Long id, @RequestBody AccountTransaction tx) {
        tx.setId(id);
        AccountTransaction updated = transactionService.update(tx);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        transactionService.delete(id);
    }
}
