package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.finance.AccountTransaction;
import com.pgsa.trailers.repository.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AccountTransactionService {

    private final AccountTransactionRepository transactionRepo;

    public AccountTransactionService(AccountTransactionRepository transactionRepo) {
        this.transactionRepo = transactionRepo;
    }

    public AccountTransaction create(AccountTransaction tx) {
        return transactionRepo.save(tx);
    }

    public List<AccountTransaction> getAll() {
        return transactionRepo.findAll();
    }

    public AccountTransaction getById(Long id) {
        return transactionRepo.findById(id).orElse(null);
    }

    public List<AccountTransaction> getByAccount(Long accountId) {
        return transactionRepo.findByAccount_Id(accountId);
    }

    public List<AccountTransaction> getPendingForReconciliation(Long accountId) {
        return transactionRepo.findByAccountIdAndReconciledFalse(accountId);
    }



    public AccountTransaction update(AccountTransaction tx) {
        return transactionRepo.save(tx);
    }

    public void delete(Long id) {
        transactionRepo.deleteById(id);
    }
}
