package com.rs.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to transfer funds between wallets")
public class TransferRequest {

    @NotNull
    @Schema(description = "Sender wallet ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID fromWalletId;

    @NotNull
    @Schema(description = "Receiver wallet ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID toWalletId;

    @NotNull
    @Schema(description = "Amount to transfer", example = "500")
    private BigDecimal amount;
}