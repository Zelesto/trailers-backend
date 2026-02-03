package com.pgsa.trailers.controller.debug;

import com.pgsa.trailers.entity.finance.Account;
import com.pgsa.trailers.enums.AccountType;
import com.pgsa.trailers.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/debug/accounts")
@RequiredArgsConstructor
public class AccountDebugController {

    private final AccountRepository accountRepository;

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testAccountEndpoint() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Test 1: Count all accounts
            long count = accountRepository.count();
            response.put("totalAccounts", count);

            // Test 2: Try to find by type
            List<Account> fuelAccounts = accountRepository.findByType(AccountType.FUEL);
            response.put("fuelAccountsCount", fuelAccounts.size());
            response.put("fuelAccounts", fuelAccounts);

            // Test 3: Try to save a test account
            Account testAccount = new Account();
            testAccount.setName("Test Fuel Account");
            testAccount.setType(AccountType.FUEL);
            testAccount.setCurrency("ZAR");
            testAccount.setActive(true);

            Account saved = accountRepository.save(testAccount);
            response.put("testAccountSaved", true);
            response.put("testAccountId", saved.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getName());
            log.error("Account debug error: ", e);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/cleanup/{id}")
    public ResponseEntity<Map<String, Object>> cleanupTestAccount(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            accountRepository.deleteById(id);
            response.put("deleted", true);
            response.put("id", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}