package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.finance.Account;
import com.pgsa.trailers.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountControllerTest {

    @Test
    void getAllAccountsDelegatesToService() {
        AccountService accountService = mock(AccountService.class);
        AccountController controller = new AccountController(accountService);

        Account account = new Account();
        account.setId(101L);
        account.setName("Main Bank");
        account.setType("BANK");
        account.setCurrency("ZAR");
        account.setActive(true);

        when(accountService.getAllAccounts()).thenReturn(List.of(account));

        ResponseEntity<List<Account>> response = controller.getAllAccounts();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsExactly(account);
        verify(accountService).getAllAccounts();
    }
}
