package com.plataformas.hilos.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.plataformas.hilos.dto.TransactionResponse;
import com.plataformas.hilos.dto.TransferRequest;
import com.plataformas.hilos.entity.TransactionStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransferExecutorService transferExecutorService;
    
    /**
     * Inicia una transferencia de forma asíncrona
     */
    @Async("transferExecutor")
    public CompletableFuture<TransactionResponse> processTransfer(TransferRequest request) {
        log.info("Iniciando transferencia asíncrona: {} -> {} (${})", 
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        try {
            TransactionResponse response = transferExecutorService.executeTransfer(request);
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("Error en transferencia asíncrona: {}", e.getMessage());
            CompletableFuture<TransactionResponse> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }
    
    // Compatibilidad para llamadas existentes (p.ej., ConcurrencyDemoController)
    public TransactionResponse executeTransfer(TransferRequest request) {
        return transferExecutorService.executeTransfer(request);
    }

    /**
     * Obtiene todas las transacciones
     */
    public List<TransactionResponse> getAllTransactions() {
        log.info("Consultando todas las transacciones");
        return transferExecutorService.getAllTransactions();
    }
    
    /**
     * Obtiene transacciones por estado
     */
    public List<TransactionResponse> getTransactionsByStatus(TransactionStatus status) {
        log.info("Consultando transacciones con estado: {}", status);
        return transferExecutorService.getTransactionsByStatus(status);
    }
}
