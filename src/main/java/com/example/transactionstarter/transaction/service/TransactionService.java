package com.example.transactionstarter.transaction.service;

import com.example.transactionstarter.transaction.dto.CreateTransactionRequest;
import com.example.transactionstarter.transaction.dto.UpdateStatusRequest;
import com.example.transactionstarter.transaction.entity.Transaction;
import com.example.transactionstarter.transaction.enums.TransactionStatus;
import com.example.transactionstarter.transaction.exception.DuplicateTransactionException;
import com.example.transactionstarter.transaction.exception.InvalidStatusTransitionException;
import com.example.transactionstarter.transaction.exception.ResourceNotFoundException;
import com.example.transactionstarter.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repository;

    public Transaction createTransaction(CreateTransactionRequest request) {

        if (repository.existsById(request.getTransactionId())) {
            throw new DuplicateTransactionException(
                    "Transaction ID already exists: "
                            + request.getTransactionId());
        }

        Transaction transaction = new Transaction(
                request.getTransactionId(),
                request.getCustomerId(),
                request.getAmount(),
                request.getCurrency(),
                request.getTransactionType(),
                TransactionStatus.PENDING
        );

        return repository.save(transaction);
    }

    public Transaction getTransaction(String transactionId) {

        return repository.findById(transactionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Transaction not found: "
                                        + transactionId));
    }

    public Transaction updateTransactionStatus(
            String transactionId,
            UpdateStatusRequest request) {

        Transaction transaction = repository.findById(transactionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Transaction not found: "
                                        + transactionId));

        TransactionStatus currentStatus =
                transaction.getTransactionStatus();

        TransactionStatus newStatus =
                request.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        transaction.setTransactionStatus(newStatus);

        return repository.save(transaction);
    }

    private void validateStatusTransition(
            TransactionStatus currentStatus,
            TransactionStatus newStatus) {

        if (currentStatus != TransactionStatus.PENDING) {
            throw new InvalidStatusTransitionException(
                    "Transaction cannot be updated from "
                            + currentStatus
                            + " to "
                            + newStatus);
        }

        if (newStatus != TransactionStatus.SUCCESS
                && newStatus != TransactionStatus.FAILED) {

            throw new InvalidStatusTransitionException(
                    "Transaction can only move from PENDING "
                            + "to SUCCESS or FAILED");
        }
    }

    public List<Transaction> getCustomerTransactions(
            String customerId) {

        return repository.findByCustomerId(customerId);
    }
}