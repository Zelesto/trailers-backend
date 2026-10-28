package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.finance.Account;
import com.pgsa.trailers.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    public static final String ACCOUNT_TYPE_FUEL = "FUEL";
    public static final String ACCOUNT_TYPE_BANK = "BANK";
    public static final String ACCOUNT_TYPE_CASH = "CASH";
    public static final String ACCOUNT_TYPE_EXPENSE = "EXPENSE";

    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        log.info("GET /api/accounts - fetching all accounts");
        try {
            return ResponseEntity.ok(accountService.getAllAccounts());
        } catch (Exception e) {
            log.error("Error fetching all accounts: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(params = "type")
    public ResponseEntity<List<Account>> getAccountsByType(@RequestParam String type) {
        log.info("GET /api/accounts?type={} - fetching accounts by type", type);
        try {
            return ResponseEntity.ok(accountService.getAccountsByType(type));
        } catch (Exception e) {
            log.error("Error fetching accounts by type {}: {}", type, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        log.info("GET /api/accounts/{} - fetching account by id", id);
        try {
            return ResponseEntity.ok(accountService.getAccountById(id));
        } catch (RuntimeException e) {
            log.warn("Account {} not found: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching account {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        log.info("POST /api/accounts - creating new account: {}", account != null ? account.getName() : "null");
        try {
            if (account == null) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(accountService.createAccount(account));
        } catch (Exception e) {
            log.error("Error creating account: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @RequestBody Account account) {
        log.info("PUT /api/accounts/{} - updating account", id);
        try {
            if (account == null) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(accountService.updateAccount(id, account));
        } catch (RuntimeException e) {
            log.warn("Account {} not found during update: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating account {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        log.info("DELETE /api/accounts/{} - deactivating account", id);
        try {
            accountService.deactivateAccount(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.warn("Account {} not found during delete: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting account {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
