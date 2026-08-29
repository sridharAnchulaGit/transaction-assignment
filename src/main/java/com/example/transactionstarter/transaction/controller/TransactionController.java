package com.example.transactionstarter.transaction.controller;

import com.example.transactionstarter.transaction.dto.CreateTransactionRequest;
import com.example.transactionstarter.transaction.dto.UpdateStatusRequest;
import com.example.transactionstarter.transaction.entity.Transaction;
import com.example.transactionstarter.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {

        return service.createTransaction(request);
    }

    @GetMapping("/{transactionId}")
    public Transaction getTransaction(
            @PathVariable String transactionId) {

        return service.getTransaction(transactionId);
    }

    @PatchMapping("/{transactionId}/status")
    public Transaction updateTransactionStatus(
            @PathVariable String transactionId,
            @Valid @RequestBody UpdateStatusRequest request) {

        return service.updateTransactionStatus(
                transactionId,
                request);
    }
}