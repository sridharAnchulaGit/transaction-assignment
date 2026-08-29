package com.example.transactionstarter.transaction.service;

import com.example.transactionstarter.transaction.dto.CreateTransactionRequest;
import com.example.transactionstarter.transaction.dto.UpdateStatusRequest;
import com.example.transactionstarter.transaction.entity.Transaction;
import com.example.transactionstarter.transaction.enums.Currency;
import com.example.transactionstarter.transaction.enums.TransactionStatus;
import com.example.transactionstarter.transaction.enums.TransactionType;
import com.example.transactionstarter.transaction.exception.DuplicateTransactionException;
import com.example.transactionstarter.transaction.exception.InvalidStatusTransitionException;
import com.example.transactionstarter.transaction.exception.ResourceNotFoundException;
import com.example.transactionstarter.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(repository);
    }

    @Test
    void shouldCreateTransactionSuccessfully() {

        CreateTransactionRequest request = createRequest();

        Transaction savedTransaction = new Transaction(
                "TXN1001",
                "CUST001",
                new BigDecimal("5000"),
                Currency.INR,
                TransactionType.CREDIT,
                TransactionStatus.PENDING
        );

        when(repository.existsById("TXN1001"))
                .thenReturn(false);

        when(repository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        Transaction result = service.createTransaction(request);

        assertNotNull(result);
        assertEquals("TXN1001", result.getTransactionId());
        assertEquals("CUST001", result.getCustomerId());
        assertEquals(TransactionStatus.PENDING,
                result.getTransactionStatus());

        verify(repository).save(any(Transaction.class));
    }

    @Test
    void shouldRejectDuplicateTransaction() {

        CreateTransactionRequest request = createRequest();

        when(repository.existsById("TXN1001"))
                .thenReturn(true);

        assertThrows(
                DuplicateTransactionException.class,
                () -> service.createTransaction(request)
        );

        verify(repository, never())
                .save(any(Transaction.class));
    }

    @Test
    void shouldGetTransactionSuccessfully() {

        Transaction transaction = createTransaction();

        when(repository.findById("TXN1001"))
                .thenReturn(Optional.of(transaction));

        Transaction result =
                service.getTransaction("TXN1001");

        assertNotNull(result);
        assertEquals("TXN1001",
                result.getTransactionId());

        verify(repository).findById("TXN1001");
    }

    @Test
    void shouldThrowExceptionWhenTransactionDoesNotExist() {

        when(repository.findById("TXN9999"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getTransaction("TXN9999")
        );
    }

    @Test
    void shouldUpdatePendingTransactionToSuccess() {

        Transaction transaction = createTransaction();

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(TransactionStatus.SUCCESS);

        when(repository.findById("TXN1001"))
                .thenReturn(Optional.of(transaction));

        when(repository.save(transaction))
                .thenReturn(transaction);

        Transaction result =
                service.updateTransactionStatus(
                        "TXN1001",
                        request);

        assertEquals(
                TransactionStatus.SUCCESS,
                result.getTransactionStatus()
        );

        verify(repository).save(transaction);
    }

    @Test
    void shouldRejectStatusChangeFromSuccessToFailed() {

        Transaction transaction = createTransaction();

        transaction.setTransactionStatus(
                TransactionStatus.SUCCESS);

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(TransactionStatus.FAILED);

        when(repository.findById("TXN1001"))
                .thenReturn(Optional.of(transaction));

        assertThrows(
                InvalidStatusTransitionException.class,
                () -> service.updateTransactionStatus(
                        "TXN1001",
                        request)
        );

        verify(repository, never())
                .save(any(Transaction.class));
    }

    @Test
    void shouldGetCustomerTransactions() {

        Transaction transaction1 = createTransaction();

        Transaction transaction2 = new Transaction(
                "TXN1002",
                "CUST001",
                new BigDecimal("2000"),
                Currency.INR,
                TransactionType.DEBIT,
                TransactionStatus.PENDING
        );

        when(repository.findByCustomerId("CUST001"))
                .thenReturn(List.of(transaction1, transaction2));

        List<Transaction> result =
                service.getCustomerTransactions("CUST001");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("TXN1001",
                result.get(0).getTransactionId());
        assertEquals("TXN1002",
                result.get(1).getTransactionId());

        verify(repository)
                .findByCustomerId("CUST001");
    }

    private CreateTransactionRequest createRequest() {

        CreateTransactionRequest request =
                new CreateTransactionRequest();

        request.setTransactionId("TXN1001");
        request.setCustomerId("CUST001");
        request.setAmount(new BigDecimal("5000"));
        request.setCurrency(Currency.INR);
        request.setTransactionType(TransactionType.CREDIT);

        return request;
    }

    private Transaction createTransaction() {

        return new Transaction(
                "TXN1001",
                "CUST001",
                new BigDecimal("5000"),
                Currency.INR,
                TransactionType.CREDIT,
                TransactionStatus.PENDING
        );
    }
}