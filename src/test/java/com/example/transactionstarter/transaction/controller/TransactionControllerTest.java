package com.example.transactionstarter.transaction.controller;

import com.example.transactionstarter.transaction.dto.CreateTransactionRequest;
import com.example.transactionstarter.transaction.entity.Transaction;
import com.example.transactionstarter.transaction.enums.Currency;
import com.example.transactionstarter.transaction.enums.TransactionStatus;
import com.example.transactionstarter.transaction.enums.TransactionType;
import com.example.transactionstarter.transaction.exception.DuplicateTransactionException;
import com.example.transactionstarter.transaction.exception.ResourceNotFoundException;
import com.example.transactionstarter.transaction.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService service;

    @Test
    void shouldCreateTransaction() throws Exception {

        CreateTransactionRequest request =
                new CreateTransactionRequest();

        request.setTransactionId("TXN2001");
        request.setCustomerId("CUST001");
        request.setAmount(new BigDecimal("5000"));
        request.setCurrency(Currency.INR);
        request.setTransactionType(TransactionType.CREDIT);

        Transaction transaction = new Transaction(
                "TXN2001",
                "CUST001",
                new BigDecimal("5000"),
                Currency.INR,
                TransactionType.CREDIT,
                TransactionStatus.PENDING
        );

        when(service.createTransaction(any(CreateTransactionRequest.class)))
                .thenReturn(transaction);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectInvalidTransaction() throws Exception {

        CreateTransactionRequest request =
                new CreateTransactionRequest();

        request.setTransactionId("");
        request.setCustomerId("");
        request.setAmount(BigDecimal.valueOf(-100));
        request.setCurrency(null);
        request.setTransactionType(null);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictForDuplicateTransaction()
            throws Exception {

        CreateTransactionRequest request =
                new CreateTransactionRequest();

        request.setTransactionId("TXN2001");
        request.setCustomerId("CUST001");
        request.setAmount(new BigDecimal("5000"));
        request.setCurrency(Currency.INR);
        request.setTransactionType(TransactionType.CREDIT);

        when(service.createTransaction(any(CreateTransactionRequest.class)))
                .thenThrow(new DuplicateTransactionException(
                        "Transaction ID already exists: TXN2001"));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldGetTransaction() throws Exception {

        Transaction transaction = new Transaction(
                "TXN2001",
                "CUST001",
                new BigDecimal("5000"),
                Currency.INR,
                TransactionType.CREDIT,
                TransactionStatus.PENDING
        );

        when(service.getTransaction("TXN2001"))
                .thenReturn(transaction);

        mockMvc.perform(get("/api/transactions/TXN2001"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundWhenTransactionDoesNotExist()
            throws Exception {

        when(service.getTransaction("TXN9999"))
                .thenThrow(new ResourceNotFoundException(
                        "Transaction not found: TXN9999"));

        mockMvc.perform(get("/api/transactions/TXN9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAcceptLowercaseTransactionType() throws Exception {

        CreateTransactionRequest request =
                new CreateTransactionRequest();

        request.setTransactionId("TXN3001");
        request.setCustomerId("CUST001");
        request.setAmount(new BigDecimal("1000"));
        request.setCurrency(Currency.INR);
        request.setTransactionType(TransactionType.CREDIT);

        Transaction transaction = new Transaction(
                "TXN3001",
                "CUST001",
                new BigDecimal("1000"),
                Currency.INR,
                TransactionType.CREDIT,
                TransactionStatus.PENDING
        );

        when(service.createTransaction(any(CreateTransactionRequest.class)))
                .thenReturn(transaction);

        String json = """
            {
                "transactionId": "TXN3001",
                "customerId": "CUST001",
                "amount": 1000,
                "currency": "inr",
                "transactionType": "credit"
            }
            """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectInvalidTransactionType() throws Exception {

        String json = """
            {
                "transactionId": "TXN3002",
                "customerId": "CUST001",
                "amount": 1000,
                "currency": "INR",
                "transactionType": "INVALID_TYPE"
            }
            """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}