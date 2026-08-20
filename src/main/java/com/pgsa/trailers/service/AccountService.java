package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.finance.Account;
import com.pgsa.trailers.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    // ============================================================
    // CONSTANTS FOR ACCOUNT TYPES (from enum_master table)
    // ============================================================
    public static final String ACCOUNT_TYPE_ASSET = "ASSET";
    public static final String ACCOUNT_TYPE_LIABILITY = "LIABILITY";
    public static final String ACCOUNT_TYPE_EQUITY = "EQUITY";
    public static final String ACCOUNT_TYPE_REVENUE = "REVENUE";
    public static final String ACCOUNT_TYPE_EXPENSE = "EXPENSE";
    public static final String ACCOUNT_TYPE_FUEL = "FUEL";

    // Valid account types for validation
    private static final List<String> VALID_ACCOUNT_TYPES = List.of(
        ACCOUNT_TYPE_ASSET, ACCOUNT_TYPE_LIABILITY, ACCOUNT_TYPE_EQUITY,
        ACCOUNT_TYPE_REVENUE, ACCOUNT_TYPE_EXPENSE, ACCOUNT_TYPE_FUEL
    );

    private final AccountRepository accountRepository;

    /**
     * Get all accounts
     */
    public List<Account> getAllAccounts() {
        log.debug("Fetching all accounts");
        return accountRepository.findAll();
    }

    /**
     * Get accounts by type (using String)
     */
    public List<Account> getAccountsByType(String type) {
        log.debug("Fetching accounts by type: {}", type);
        
        // Validate the type
        if (type == null || type.trim().isEmpty()) {
            log.warn("Account type is null or empty");
            return List.of();
        }
        
        String upperType = type.toUpperCase();
        if (!VALID_ACCOUNT_TYPES.contains(upperType)) {
            log.warn("Invalid account type: {}, using default validation", type);
            // Still try to find by type, but log warning
        }
        
        return accountRepository.findByType(upperType);
    }

    /**
     * Get accounts by type with case-insensitive matching
     */
    public List<Account> getAccountsByTypeIgnoreCase(String type) {
        if (type == null || type.trim().isEmpty()) {
            return List.of();
        }
        return getAccountsByType(type.trim().toUpperCase());
    }

    /**
     * Get account by ID
     */
    public Account getAccountById(Long id) {
        log.debug("Fetching account by ID: {}", id);
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + id));
    }

    /**
     * Get active accounts
     */
    public List<Account> getActiveAccounts() {
        log.debug("Fetching active accounts");
        return accountRepository.findByActiveTrue();
    }

    /**
     * Get accounts by provider
     */
    public List<Account> getAccountsByProvider(String provider) {
        log.debug("Fetching accounts by provider: {}", provider);
        if (provider == null || provider.trim().isEmpty()) {
            return List.of();
        }
        return accountRepository.findByProvider(provider);
    }

    /**
     * Search accounts by name (case-insensitive)
     */
    public List<Account> searchAccountsByName(String name) {
        log.debug("Searching accounts by name: {}", name);
        if (name == null || name.trim().isEmpty()) {
            return List.of();
        }
        return accountRepository.findByNameContainingIgnoreCase(name.trim());
    }

    /**
     * Create a new account
     */
    public Account createAccount(Account account) {
        log.info("Creating new account: {}", account.getName());
        
        // Set defaults if not provided
        if (account.getActive() == null) {
            account.setActive(true);
        }
        
        if (account.getCurrency() == null || account.getCurrency().isEmpty()) {
            account.setCurrency("ZAR");
        }
        
        account.setCreatedAt(java.time.LocalDateTime.now());
        account.setUpdatedAt(java.time.LocalDateTime.now());
        
        return accountRepository.save(account);
    }

    /**
     * Update an existing account
     */
    public Account updateAccount(Long id, Account account) {
        log.info("Updating account: {}", id);
        
        Account existing = getAccountById(id);
        
        if (account.getName() != null) {
            existing.setName(account.getName());
        }
        if (account.getType() != null) {
            existing.setType(account.getType());
        }
        if (account.getProvider() != null) {
            existing.setProvider(account.getProvider());
        }
        if (account.getCurrency() != null) {
            existing.setCurrency(account.getCurrency());
        }
        if (account.getActive() != null) {
            existing.setActive(account.getActive());
        }
        if (account.getAccountNumber() != null) {
            existing.setAccountNumber(account.getAccountNumber());
        }
        
        existing.setUpdatedAt(java.time.LocalDateTime.now());
        
        return accountRepository.save(existing);
    }

    /**
     * Delete an account (soft delete by setting active to false)
     */
    public void deactivateAccount(Long id) {
        log.info("Deactivating account: {}", id);
        Account account = getAccountById(id);
        account.setActive(false);
        account.setUpdatedAt(java.time.LocalDateTime.now());
        accountRepository.save(account);
    }

    /**
     * Activate an account
     */
    public void activateAccount(Long id) {
        log.info("Activating account: {}", id);
        Account account = getAccountById(id);
        account.setActive(true);
        account.setUpdatedAt(java.time.LocalDateTime.now());
        accountRepository.save(account);
    }
}
