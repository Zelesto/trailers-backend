package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.finance.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByType(String type);

    List<Account> findByActiveTrue();

    List<Account> findByNameContainingIgnoreCase(String name);

    List<Account> findByProvider(String provider);

    Optional<Account> findByAccountNumber(String accountNumber);
}
