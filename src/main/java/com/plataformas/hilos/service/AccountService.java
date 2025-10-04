package com.plataformas.hilos.service;

import com.plataformas.hilos.dto.AccountResponse;
import com.plataformas.hilos.dto.CreateAccountRequest;
import com.plataformas.hilos.entity.Account;
import com.plataformas.hilos.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    
    private final AccountRepository accountRepository;
    
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creando cuenta para: {}", request.getOwner());
        
        Account account = new Account();
        account.setOwner(request.getOwner());
        account.setBalance(request.getInitialBalance());
        
        Account savedAccount = accountRepository.save(account);
        log.info("Cuenta creada con ID: {}", savedAccount.getId());
        
        return mapToResponse(savedAccount);
    }
    
    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        log.info("Consultando cuenta con ID: {}", id);
        
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada con ID: " + id));
        
        return mapToResponse(account);
    }
    
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        log.info("Consultando todas las cuentas");
        
        List<Account> accounts = accountRepository.findAll();
        return accounts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwner(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}
