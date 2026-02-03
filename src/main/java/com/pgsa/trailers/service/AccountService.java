package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.finance.Account;
import com.pgsa.trailers.enums.AccountType;
import com.pgsa.trailers.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    /**
     * Get all accounts
     */
    public List<Account> getAllAccounts() {
        log.debug("Fetching all accounts");
        return accountRepository.findAll();
    }

    /**
     * Get accounts by type (using enum)
     */
    public List<Account> getAccountsByType(AccountType type) {
        log.debug("Fetching accounts by type: {}", type);
        return accountRepository.findByType(type);
    }

    /**
     * Get accounts by type string (converts string to enum)
     */
    public List<Account> getAccountsByType(String typeString) {
        try {
            AccountType type = AccountType.valueOf(typeString.toUpperCase());
            return getAccountsByType(type);
        } catch (IllegalArgumentException e) {
            log.error("Invalid account type: {}", typeString);
            throw new IllegalArgumentException("Invalid account type: " + typeString);
        }
    }

    /**
     * Get account by ID
     */
    public Account getAccountById(Long id) {
        log.debug("Fetching account by ID: {}", id);
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + id));
    }
}