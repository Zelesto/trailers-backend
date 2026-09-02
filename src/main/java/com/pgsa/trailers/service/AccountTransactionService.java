package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.finance.Account;
import com.pgsa.trailers.entity.finance.AccountTransaction;
import com.pgsa.trailers.repository.AccountRepository;
import com.pgsa.trailers.repository.AccountTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountTransactionService {

    private final AccountTransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public AccountTransaction createTransaction(AccountTransaction transaction) {
        log.info("Creating transaction for account: {}", transaction.getAccount().getId());
        
        // Generate transaction number if not provided
        if (transaction.getTransactionNumber() == null) {
            transaction.setTransactionNumber(generateTransactionNumber());
        }
        
        // Set default payment status
        if (transaction.getPaymentStatus() == null) {
            transaction.setPaymentStatus("PENDING");
        }
        
        // Set transaction date if not provided
        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(LocalDateTime.now());
        }
        
        // Update account balance
        Account account = transaction.getAccount();
        BigDecimal newBalance = account.getBalance();
        
        if ("CREDIT".equals(transaction.getDirection())) {
            newBalance = newBalance.add(transaction.getAmount());
        } else if ("DEBIT".equals(transaction.getDirection())) {
            newBalance = newBalance.subtract(transaction.getAmount());
        }
        
        account.setBalance(newBalance);
        transaction.setBalanceAfter(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        
        accountRepository.save(account);
        
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        
        return transactionRepository.save(transaction);
    }

    @Transactional
    public AccountTransaction reverseTransaction(Long transactionId, String reason) {
        AccountTransaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        // Create reversal transaction
        AccountTransaction reversal = new AccountTransaction();
        reversal.setAccount(original.getAccount());
        reversal.setTransactionNumber(generateTransactionNumber());
        reversal.setTransactionDate(LocalDateTime.now());
        reversal.setPostingDate(LocalDateTime.now().toLocalDate());
        reversal.setAmount(original.getAmount());
        reversal.setDirection("DEBIT".equals(original.getDirection()) ? "CREDIT" : "DEBIT");
        reversal.setBalanceAfter(original.getBalanceAfter());
        reversal.setTransactionType("REVERSAL");
        reversal.setSourceType(original.getSourceType());
        reversal.setSourceId(original.getSourceId());
        reversal.setDescription("Reversal of " + original.getTransactionNumber() + ": " + reason);
        reversal.setPaymentStatus("COMPLETED");
        reversal.setCurrency(original.getCurrency());
        
        return createTransaction(reversal);
    }

    @Transactional(readOnly = true)
    public List<AccountTransaction> getTransactionsByAccount(Long accountId) {
        return transactionRepository.findByAccount_Id(accountId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalanceForAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return account.getBalance();
    }

    private String generateTransactionNumber() {
        return "TXN-" + LocalDateTime.now().getYear() + 
               String.format("%02d", LocalDateTime.now().getMonthValue()) +
               String.format("%02d", LocalDateTime.now().getDayOfMonth()) + "-" +
               UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
