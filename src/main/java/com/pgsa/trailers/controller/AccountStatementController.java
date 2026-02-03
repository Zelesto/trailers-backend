package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.finance.AccountStatement;
import com.pgsa.trailers.service.ComplianceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts/statements")
@PreAuthorize("hasRole('FINANCE')")
public class AccountStatementController {

    private final ComplianceService complianceService;

    public AccountStatementController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    // Create a new account statement
    @PostMapping
    public AccountStatement createStatement(@RequestBody AccountStatement statement) {
        return complianceService.createAccountStatement(statement);
    }

    // Get an account statement by ID
    @GetMapping("/{id}")
    public AccountStatement getStatement(@PathVariable Long id) {
        return complianceService.getAccountStatementById(id);
    }
}