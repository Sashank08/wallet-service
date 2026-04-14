package com.rs.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing transfer details")
public class TransferResponse {

    @Schema(description = "Sender wallet ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID fromWalletId;

    @Schema(description = "Receiver wallet ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID toWalletId;

    @Schema(description = "Transferred amount", example = "500")
    private BigDecimal amount;
}