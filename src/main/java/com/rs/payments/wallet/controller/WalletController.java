package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.exception.InvalidAmountException;
import com.rs.payments.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/wallets")
@Tag(name = "Wallet Management", description = "APIs for managing user wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(
            summary = "Create a new wallet for a user",
            description = "Creates a new wallet for the specified user ID with a zero balance.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Wallet created successfully",
                            content = @Content(schema = @Schema(implementation = Wallet.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found"
                    )
            }
    )
    @PostMapping
    public ResponseEntity<Wallet> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWalletForUser(request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<Wallet> deposit(@PathVariable UUID id,
                                          @RequestBody Map<String, BigDecimal> request) {

        BigDecimal amount = request.get("amount");

        Wallet wallet = walletService.deposit(id, amount);

        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Wallet> withdraw(@PathVariable UUID id,
                                           @RequestBody Map<String, BigDecimal> request) {

        BigDecimal amount = request.get("amount");

        if (amount == null) {
            throw new InvalidAmountException("Amount cannot be null");
        }

        Wallet wallet = walletService.withdraw(id, amount);

        return ResponseEntity.ok(wallet);
    }
}