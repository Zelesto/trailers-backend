package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.finance.Account;
import com.pgsa.trailers.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // Use AccountType enum
    List<Account> findByType(AccountType type);

    // Find active accounts
    List<Account> findByActiveTrue();

    // Find by name containing (case-insensitive)
    List<Account> findByNameContainingIgnoreCase(String name);

    // Find by provider
    List<Account> findByProvider(String provider);

    // REMOVE or COMMENT OUT this line if Account entity doesn't have accountNumber:
    // Optional<Account> findByAccountNumber(String accountNumber);
}