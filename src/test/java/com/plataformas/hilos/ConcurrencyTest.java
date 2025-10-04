package com.plataformas.hilos;

import com.plataformas.hilos.dto.TransferRequest;
import com.plataformas.hilos.entity.Account;
import com.plataformas.hilos.entity.TransactionStatus;
import com.plataformas.hilos.repository.AccountRepository;
import com.plataformas.hilos.repository.TransactionRepository;
import com.plataformas.hilos.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyTest {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private TransactionService transactionService;
    
    @Test
    public void testConcurrentTransfers() throws InterruptedException {
        // Crear cuentas de prueba
        Account fromAccount = new Account();
        fromAccount.setOwner("Test From");
        fromAccount.setBalance(new BigDecimal("1000.00"));
        fromAccount = accountRepository.save(fromAccount);
        
        Account toAccount = new Account();
        toAccount.setOwner("Test To");
        toAccount.setBalance(new BigDecimal("0.00"));
        toAccount = accountRepository.save(toAccount);
        
        final Long fromAccountId = fromAccount.getId();
        final Long toAccountId = toAccount.getId();
        
        // Ejecutar 10 transferencias simultáneas de $50 cada una
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CompletableFuture<Void>[] futures = new CompletableFuture[10];
        
        for (int i = 0; i < 10; i++) {
            final int transferNumber = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    TransferRequest request = new TransferRequest(
                        fromAccountId, 
                        toAccountId, 
                        new BigDecimal("50.00")
                    );
                    transactionService.executeTransfer(request);
                    System.out.println("Transferencia " + transferNumber + " completada");
                } catch (Exception e) {
                    System.err.println("Error en transferencia " + transferNumber + ": " + e.getMessage());
                }
            }, executor);
        }
        
        // Esperar a que todas las transferencias terminen
        CompletableFuture.allOf(futures).join();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        // Verificar resultados
        Account updatedFromAccount = accountRepository.findById(fromAccount.getId()).orElseThrow();
        Account updatedToAccount = accountRepository.findById(toAccount.getId()).orElseThrow();
        
        System.out.println("Saldo cuenta origen: $" + updatedFromAccount.getBalance());
        System.out.println("Saldo cuenta destino: $" + updatedToAccount.getBalance());
        
        // Verificar que no hay saldo negativo
        assertTrue(updatedFromAccount.getBalance().compareTo(BigDecimal.ZERO) >= 0, 
                  "La cuenta origen no debe tener saldo negativo");
        
        // Verificar que el saldo total se mantiene
        BigDecimal totalBalance = updatedFromAccount.getBalance().add(updatedToAccount.getBalance());
        assertEquals(new BigDecimal("1000.00"), totalBalance, 
                    "El saldo total debe mantenerse constante");
        
        // Verificar transacciones completadas
        long completedTransactions = transactionRepository.findByStatus(TransactionStatus.COMPLETED).size();
        System.out.println("Transacciones completadas: " + completedTransactions);
        
        assertTrue(completedTransactions > 0, "Debe haber al menos una transacción completada");
    }
}
