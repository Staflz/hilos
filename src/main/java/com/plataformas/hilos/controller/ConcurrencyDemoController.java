package com.plataformas.hilos.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plataformas.hilos.dto.ApiResponse;
import com.plataformas.hilos.dto.TransferRequest;
import com.plataformas.hilos.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
@Slf4j
public class ConcurrencyDemoController {
    
    private final TransactionService transactionService;
    @Qualifier("transferExecutor")
    private final ThreadPoolTaskExecutor transferExecutor;
    
    /**
     * POST /demo/concurrent-transfers
     * Demostrar el problema de concurrencia con múltiples transferencias simultáneas
     */
    @PostMapping("/concurrent-transfers")
    public ResponseEntity<ApiResponse<String>> demonstrateConcurrency(
            @RequestParam Long fromAccountId,
            @RequestParam Long toAccountId,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "5") int numberOfTransfers) {
        
        try {
            log.info("Iniciando demostración de concurrencia con {} transferencias solicitadas", numberOfTransfers);
            
            // Usar el límite del AsyncConfig para alinear la demo con la config global
            int configuredMax = transferExecutor.getMaxPoolSize();
            int poolSize = Math.min(configuredMax, Math.max(4, numberOfTransfers));
            ExecutorService executor = Executors.newFixedThreadPool(poolSize);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            
            // Crear múltiples transferencias simultáneas
            for (int i = 0; i < numberOfTransfers; i++) {
                final int transferNumber = i + 1;
                TransferRequest request = new TransferRequest(fromAccountId, toAccountId, amount);
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        log.info("Ejecutando transferencia {} de {}: {} -> {} (${})", 
                                transferNumber, numberOfTransfers, fromAccountId, toAccountId, amount);
                        transactionService.executeTransfer(request);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Error en transferencia {}: {}", transferNumber, e.getMessage());
                        failureCount.incrementAndGet();
                    }
                }, executor);
                
                futures.add(future);
            }
            
            // Esperar a que todas las transferencias terminen
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executor.shutdown();
            
            log.info("Resumen demo concurrencia -> éxitos: {}, fallos: {} (total: {}) | poolSize: {}",
                    successCount.get(), failureCount.get(), numberOfTransfers, poolSize);

            return ResponseEntity.ok(ApiResponse.success(
                    "Demostración de concurrencia completada. Se lanzaron " + numberOfTransfers + 
                    " transferencias. Revisa los logs para ver el comportamiento."));
            
        } catch (Exception e) {
            log.error("Error en demostración de concurrencia: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error en demostración: " + e.getMessage()));
        }
    }
    
    /**
     * GET /demo/race-condition
     * Demostrar condición de carrera sin sincronización
     */
    @GetMapping("/race-condition")
    public ResponseEntity<ApiResponse<String>> demonstrateRaceCondition() {
        return ResponseEntity.ok(ApiResponse.success(
                "Para demostrar condiciones de carrera, usa el endpoint POST /demo/concurrent-transfers " +
                "con múltiples transferencias desde la misma cuenta. " +
                "La sincronización implementada previene saldos negativos."));
    }
}
