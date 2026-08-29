package com.example.transactionstarter.transaction.controller;

import com.example.transactionstarter.transaction.entity.Transaction;
import com.example.transactionstarter.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerTransactionController {

    private final TransactionService service;

    @GetMapping("/{customerId}/transactions")
    public List<Transaction> getCustomerTransactions(
            @PathVariable String customerId) {

        return service.getCustomerTransactions(customerId);
    }
}