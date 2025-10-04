package com.plataformas.hilos.controller;

import com.plataformas.hilos.dto.ApiResponse;
import com.plataformas.hilos.dto.TransactionResponse;
import com.plataformas.hilos.dto.TransferRequest;
import com.plataformas.hilos.entity.TransactionStatus;
import com.plataformas.hilos.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {
    
    private final TransactionService transactionService;
    
    /**
     * POST /accounts/transfer
     * Realizar transferencia entre dos cuentas (procesamiento asíncrono)
     */
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<String>> transfer(@Valid @RequestBody TransferRequest request) {
        try {
            log.info("Iniciando transferencia: {} -> {} (${})", 
                    request.getFromAccountId(), request.getToAccountId(), request.getAmount());
            
            // Procesar transferencia de forma asíncrona
            CompletableFuture<TransactionResponse> future = transactionService.processTransfer(request);
            
            // Retornar inmediatamente con un mensaje de que la transferencia está en proceso
            return ResponseEntity.accepted()
                    .body(ApiResponse.success("Transferencia iniciada y está siendo procesada de forma asíncrona", 
                            "Transacción ID será asignado cuando se complete"));
            
        } catch (Exception e) {
            log.error("Error iniciando transferencia: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Error iniciando transferencia: " + e.getMessage()));
        }
    }
    
    /**
     * POST /transactions/transfer-sync
     * Realizar transferencia entre dos cuentas (procesamiento síncrono para demostración)
     */
    @PostMapping("/transfer-sync")
    public ResponseEntity<ApiResponse<TransactionResponse>> transferSync(@Valid @RequestBody TransferRequest request) {
        try {
            log.info("Iniciando transferencia síncrona: {} -> {} (${})", 
                    request.getFromAccountId(), request.getToAccountId(), request.getAmount());
            
            TransactionResponse transaction = transactionService.executeTransfer(request);
            
            return ResponseEntity.ok(ApiResponse.success("Transferencia completada exitosamente", transaction));
            
        } catch (Exception e) {
            log.error("Error en transferencia síncrona: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Error en transferencia: " + e.getMessage()));
        }
    }
    
    /**
     * GET /transactions
     * Listar todas las transacciones realizadas
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions() {
        try {
            log.info("Consultando todas las transacciones");
            List<TransactionResponse> transactions = transactionService.getAllTransactions();
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (Exception e) {
            log.error("Error consultando transacciones: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error consultando transacciones: " + e.getMessage()));
        }
    }
    
    /**
     * GET /transactions/status/{status}
     * Listar transacciones por estado (PENDING, COMPLETED, FAILED)
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByStatus(@PathVariable TransactionStatus status) {
        try {
            log.info("Consultando transacciones con estado: {}", status);
            List<TransactionResponse> transactions = transactionService.getTransactionsByStatus(status);
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (Exception e) {
            log.error("Error consultando transacciones por estado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error consultando transacciones: " + e.getMessage()));
        }
    }
}
