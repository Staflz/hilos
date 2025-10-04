package com.plataformas.hilos.controller;

import com.plataformas.hilos.dto.AccountResponse;
import com.plataformas.hilos.dto.ApiResponse;
import com.plataformas.hilos.dto.CreateAccountRequest;
import com.plataformas.hilos.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {
    
    private final AccountService accountService;
    
    /**
     * POST /accounts
     * Crear una nueva cuenta con saldo inicial
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        try {
            log.info("Creando cuenta para: {}", request.getOwner());
            AccountResponse account = accountService.createAccount(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Cuenta creada exitosamente", account));
        } catch (Exception e) {
            log.error("Error creando cuenta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Error creando cuenta: " + e.getMessage()));
        }
    }
    
    /**
     * GET /accounts/{id}
     * Consultar saldo de una cuenta
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable Long id) {
        try {
            log.info("Consultando cuenta con ID: {}", id);
            AccountResponse account = accountService.getAccount(id);
            return ResponseEntity.ok(ApiResponse.success(account));
        } catch (Exception e) {
            log.error("Error consultando cuenta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Error consultando cuenta: " + e.getMessage()));
        }
    }
    
    /**
     * GET /accounts
     * Listar todas las cuentas
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts() {
        try {
            log.info("Consultando todas las cuentas");
            List<AccountResponse> accounts = accountService.getAllAccounts();
            return ResponseEntity.ok(ApiResponse.success(accounts));
        } catch (Exception e) {
            log.error("Error consultando cuentas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error consultando cuentas: " + e.getMessage()));
        }
    }
}
