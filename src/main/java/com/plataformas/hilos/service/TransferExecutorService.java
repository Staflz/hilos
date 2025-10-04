package com.plataformas.hilos.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plataformas.hilos.dto.TransactionResponse;
import com.plataformas.hilos.dto.TransferRequest;
import com.plataformas.hilos.entity.Account;
import com.plataformas.hilos.entity.Transaction;
import com.plataformas.hilos.entity.TransactionStatus;
import com.plataformas.hilos.exception.InsufficientFundsException;
import com.plataformas.hilos.repository.AccountRepository;
import com.plataformas.hilos.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferExecutorService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(noRollbackFor = InsufficientFundsException.class)
    public TransactionResponse executeTransfer(TransferRequest request) {
        log.info("Ejecutando transferencia: {} -> {} (${})",
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        Transaction transaction = createPendingTransaction(request);

        try {
            Account fromAccount = accountRepository.findByIdWithLock(request.getFromAccountId())
                    .orElseThrow(() -> new RuntimeException("Cuenta origen no encontrada: " + request.getFromAccountId()));

            Account toAccount = accountRepository.findByIdWithLock(request.getToAccountId())
                    .orElseThrow(() -> new RuntimeException("Cuenta destino no encontrada: " + request.getToAccountId()));

            if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                throw new InsufficientFundsException("Saldo insuficiente en la cuenta origen");
            }

            fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
            toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            transaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);

            log.info("Transferencia completada exitosamente. Transacción ID: {}", transaction.getId());

        } catch (Exception e) {
            log.error("Error en transferencia: {}", e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw e;
        }

        return mapToResponse(transaction);
    }

    private Transaction createPendingTransaction(TransferRequest request) {
        Transaction transaction = new Transaction();
        transaction.setFromAccount(accountRepository.getReferenceById(request.getFromAccountId()));
        transaction.setToAccount(accountRepository.getReferenceById(request.getToAccountId()));
        transaction.setAmount(request.getAmount());
        transaction.setStatus(TransactionStatus.PENDING);

        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactions() {
        List<Transaction> transactions = transactionRepository.findAllOrderByCreatedAtDesc();
        return transactions.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByStatus(TransactionStatus status) {
        List<Transaction> transactions = transactionRepository.findByStatus(status);
        return transactions.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromAccount().getId(),
                transaction.getToAccount().getId(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt());
    }
}


