package com.plataformas.hilos.repository;

import com.plataformas.hilos.entity.Transaction;
import com.plataformas.hilos.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Busca todas las transacciones por estado
     */
    List<Transaction> findByStatus(TransactionStatus status);
    
    /**
     * Busca todas las transacciones de una cuenta específica (como emisor o receptor)
     */
    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId")
    List<Transaction> findByAccountId(@Param("accountId") Long accountId);
    
    /**
     * Busca todas las transacciones ordenadas por fecha de creación
     */
    @Query("SELECT t FROM Transaction t ORDER BY t.createdAt DESC")
    List<Transaction> findAllOrderByCreatedAtDesc();
}
