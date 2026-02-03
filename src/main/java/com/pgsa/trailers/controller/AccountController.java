package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.finance.Account;
import com.pgsa.trailers.enums.AccountType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    /**
     * GET /api/accounts - Get all accounts
     */
    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        log.info("GET /api/accounts - Getting all accounts");
        try {
            List<Account> accounts = createSampleAccounts();
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            log.error("Error in getAllAccounts: {}", e.getMessage(), e);
            return ResponseEntity.ok(new ArrayList<>()); // Return empty array instead of 500
        }
    }

    /**
     * GET /api/accounts?type=FUEL - Get accounts by type
     */
    @GetMapping(params = "type")
    public ResponseEntity<List<Account>> getAccountsByType(@RequestParam String type) {
        log.info("GET /api/accounts?type={} - Getting accounts by type", type);
        try {
            List<Account> filteredAccounts = new ArrayList<>();
            List<Account> allAccounts = createSampleAccounts();

            // Filter by type
            for (Account account : allAccounts) {
                if (account.getType().name().equalsIgnoreCase(type)) {
                    filteredAccounts.add(account);
                }
            }

            log.info("Found {} accounts of type {}", filteredAccounts.size(), type);
            return ResponseEntity.ok(filteredAccounts);

        } catch (Exception e) {
            log.error("Error in getAccountsByType: {}", e.getMessage(), e);
            return ResponseEntity.ok(new ArrayList<>()); // Return empty array instead of 500
        }
    }

    /**
     * GET /api/accounts/{id} - Get account by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        log.info("GET /api/accounts/{} - Getting account by ID", id);
        try {
            List<Account> allAccounts = createSampleAccounts();

            // Find account by ID
            for (Account account : allAccounts) {
                if (account.getId() != null && account.getId().equals(id)) {
                    return ResponseEntity.ok(account);
                }
            }

            log.warn("Account with ID {} not found", id);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error in getAccountById: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/accounts - Create new account
     */
    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        log.info("POST /api/accounts - Creating new account: {}", account.getName());
        try {
            // In a real application, you would save to database
            // For now, just return the account with a new ID
            account.setId(System.currentTimeMillis()); // Generate a fake ID
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            log.error("Error in createAccount: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/accounts/{id} - Update account
     */
    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @RequestBody Account account) {
        log.info("PUT /api/accounts/{} - Updating account", id);
        try {
            // Set the ID from path variable
            account.setId(id);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            log.error("Error in updateAccount: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DELETE /api/accounts/{id} - Delete account
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        log.info("DELETE /api/accounts/{} - Deleting account", id);
        try {
            // In a real application, you would delete from database
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error in deleteAccount: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Helper method to create sample accounts
     */
    private List<Account> createSampleAccounts() {
        List<Account> accounts = new ArrayList<>();

        // Sample Fuel Accounts
        Account fuelAccount1 = new Account();
        fuelAccount1.setId(1L);
        fuelAccount1.setName("Main Fuel Account");
        fuelAccount1.setType(AccountType.FUEL);
        fuelAccount1.setCurrency("ZAR");
        fuelAccount1.setActive(true);
        accounts.add(fuelAccount1);

        Account fuelAccount2 = new Account();
        fuelAccount2.setId(2L);
        fuelAccount2.setName("Backup Fuel Account");
        fuelAccount2.setType(AccountType.FUEL);
        fuelAccount2.setCurrency("USD");
        fuelAccount2.setActive(true);
        accounts.add(fuelAccount2);

        Account fuelAccount3 = new Account();
        fuelAccount3.setId(3L);
        fuelAccount3.setName("Diesel Account");
        fuelAccount3.setType(AccountType.FUEL);
        fuelAccount3.setCurrency("ZAR");
        fuelAccount3.setActive(true);
        accounts.add(fuelAccount3);

        // Sample Other Accounts
        Account bankAccount = new Account();
        bankAccount.setId(4L);
        bankAccount.setName("Standard Bank");
        bankAccount.setType(AccountType.BANK);
        bankAccount.setCurrency("ZAR");
        bankAccount.setActive(true);
        accounts.add(bankAccount);

        Account cashAccount = new Account();
        cashAccount.setId(5L);
        cashAccount.setName("Petty Cash");
        cashAccount.setType(AccountType.CASH);
        cashAccount.setCurrency("ZAR");
        cashAccount.setActive(true);
        accounts.add(cashAccount);

        Account expenseAccount = new Account();
        expenseAccount.setId(6L);
        expenseAccount.setName("Maintenance Expenses");
        expenseAccount.setType(AccountType.EXPENSE);
        expenseAccount.setCurrency("ZAR");
        expenseAccount.setActive(true);
        accounts.add(expenseAccount);

        return accounts;
    }
}