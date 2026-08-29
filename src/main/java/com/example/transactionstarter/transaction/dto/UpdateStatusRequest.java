package com.example.transactionstarter.transaction.dto;

import com.example.transactionstarter.transaction.enums.TransactionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private TransactionStatus status;
}