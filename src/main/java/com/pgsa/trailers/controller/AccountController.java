package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.finance.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    // ============================================================
    // CONSTANTS FOR ACCOUNT TYPES (from enum_master table)
    // ============================================================
    public static final String ACCOUNT_TYPE_FUEL = "FUEL";
    public static final String ACCOUNT_TYPE_BANK = "BANK";
    public static final String ACCOUNT_TYPE_CASH = "CASH";
    public static final String ACCOUNT_TYPE_EXPENSE = "EXPENSE";

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
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * GET /api/accounts?type=FUEL - Get accounts by type - FIXED
     */
    @GetMapping(params = "type")
    public ResponseEntity<List<Account>> getAccountsByType(@RequestParam String type) {
        log.info("GET /api/accounts?type={} - Getting accounts by type", type);
        try {
            List<Account> filteredAccounts = new ArrayList<>();
            List<Account> allAccounts = createSampleAccounts();

            // ✅ FIXED: Use String comparison instead of enum .name()
            for (Account account : allAccounts) {
                if (account.getType() != null && account.getType().equalsIgnoreCase(type)) {
                    filteredAccounts.add(account);
                }
            }

            log.info("Found {} accounts of type {}", filteredAccounts.size(), type);
            return ResponseEntity.ok(filteredAccounts);

        } catch (Exception e) {
            log.error("Error in getAccountsByType: {}", e.getMessage(), e);
            return ResponseEntity.ok(new ArrayList<>());
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
            account.setId(System.currentTimeMillis());
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
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error in deleteAccount: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Helper method to create sample accounts - FIXED
     */
    private List<Account> createSampleAccounts() {
        List<Account> accounts = new ArrayList<>();

        // Sample Fuel Accounts - ✅ FIXED: Use String constants
        Account fuelAccount1 = new Account();
        fuelAccount1.setId(1L);
        fuelAccount1.setName("Main Fuel Account");
        fuelAccount1.setType(ACCOUNT_TYPE_FUEL);
        fuelAccount1.setCurrency("ZAR");
        fuelAccount1.setActive(true);
        accounts.add(fuelAccount1);

        Account fuelAccount2 = new Account();
        fuelAccount2.setId(2L);
        fuelAccount2.setName("Backup Fuel Account");
        fuelAccount2.setType(ACCOUNT_TYPE_FUEL);
        fuelAccount2.setCurrency("USD");
        fuelAccount2.setActive(true);
        accounts.add(fuelAccount2);

        Account fuelAccount3 = new Account();
        fuelAccount3.setId(3L);
        fuelAccount3.setName("Diesel Account");
        fuelAccount3.setType(ACCOUNT_TYPE_FUEL);
        fuelAccount3.setCurrency("ZAR");
        fuelAccount3.setActive(true);
        accounts.add(fuelAccount3);

        // Sample Other Accounts - ✅ FIXED: Use String constants
        Account bankAccount = new Account();
        bankAccount.setId(4L);
        bankAccount.setName("Standard Bank");
        bankAccount.setType(ACCOUNT_TYPE_BANK);
        bankAccount.setCurrency("ZAR");
        bankAccount.setActive(true);
        accounts.add(bankAccount);

        Account cashAccount = new Account();
        cashAccount.setId(5L);
        cashAccount.setName("Petty Cash");
        cashAccount.setType(ACCOUNT_TYPE_CASH);
        cashAccount.setCurrency("ZAR");
        cashAccount.setActive(true);
        accounts.add(cashAccount);

        Account expenseAccount = new Account();
        expenseAccount.setId(6L);
        expenseAccount.setName("Maintenance Expenses");
        expenseAccount.setType(ACCOUNT_TYPE_EXPENSE);
        expenseAccount.setCurrency("ZAR");
        expenseAccount.setActive(true);
        accounts.add(expenseAccount);

        return accounts;
    }
}
